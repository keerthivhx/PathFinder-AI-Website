package com.example.positioning

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.model.Beacon
import com.example.data.model.IndoorPosition
import com.example.data.model.PositionConfidence
import com.example.data.model.PositionSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

data class BeaconReading(
    val beaconId: String,
    val rawRssi: Int,
    val smoothedRssi: Float,
    val estimatedDistanceMeters: Float,
    val timestamp: Long = System.currentTimeMillis()
)

class BlePositionEngine(
    private val context: Context? = null
) {
    companion object {
        const val RSSI_SMOOTHING_FACTOR = 0.35f // Alpha for Exponential Moving Average
        const val RSSI_SAMPLE_COUNT = 5
        const val BEACON_TIMEOUT_MS = 6000L // 6 seconds before stale beacon is discarded
        const val PATH_LOSS_EXPONENT_N = 2.4 // Path loss exponent indoors (corridors & rooms)
    }

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _recentReadings = ConcurrentHashMap<String, MutableList<Int>>()
    private val _smoothedRssiMap = ConcurrentHashMap<String, Float>()
    private val _lastSeenMap = ConcurrentHashMap<String, Long>()

    private val bluetoothManager: BluetoothManager? by lazy {
        context?.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager?.adapter
    }
    private var bleScanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null

    /**
     * Estimates distance in meters from RSSI using standard Log-Distance Path Loss Model:
     * Distance = 10 ^ ((TxPower - RSSI) / (10 * n))
     */
    fun estimateDistance(rssi: Float, txPower: Int = -59): Float {
        if (rssi == 0f) return -1f
        val ratio = (txPower - rssi) / (10.0 * PATH_LOSS_EXPONENT_N)
        val dist = 10.0.pow(ratio).toFloat()
        return dist.coerceIn(0.2f, 50.0f)
    }

    /**
     * Smooths an incoming RSSI measurement with Exponential Moving Average (EMA) and sample window.
     */
    fun updateRssiReading(beaconId: String, newRssi: Int): Float {
        val history = _recentReadings.computeIfAbsent(beaconId) { mutableListOf() }
        synchronized(history) {
            history.add(newRssi)
            if (history.size > RSSI_SAMPLE_COUNT) {
                history.removeAt(0)
            }
        }

        val previousSmoothed = _smoothedRssiMap[beaconId]
        val smoothed = if (previousSmoothed != null) {
            (RSSI_SMOOTHING_FACTOR * newRssi) + ((1f - RSSI_SMOOTHING_FACTOR) * previousSmoothed)
        } else {
            newRssi.toFloat()
        }

        _smoothedRssiMap[beaconId] = smoothed
        _lastSeenMap[beaconId] = System.currentTimeMillis()
        return smoothed
    }

    /**
     * Injects a simulated beacon reading for testing and hardware-free simulation.
     */
    fun simulateBeaconReading(beaconId: String, rssi: Int): Float {
        return updateRssiReading(beaconId, rssi)
    }

    /**
     * Calculates 2D Indoor Position $(x, y)$ and floor across registered active beacons using
     * Distance-Weighted Trilateration / Centroid algorithm.
     */
    fun calculateIndoorPosition(
        registeredBeacons: List<Beacon>,
        targetBuildingId: String,
        currentFloorHint: Int? = null
    ): IndoorPosition? {
        val now = System.currentTimeMillis()
        // Filter active beacons with fresh readings
        val activeBeaconsWithData = registeredBeacons.filter { beacon ->
            beacon.activeStatus &&
            beacon.buildingId == targetBuildingId &&
            (_lastSeenMap[beacon.beaconId]?.let { now - it < BEACON_TIMEOUT_MS } == true)
        }

        if (activeBeaconsWithData.isEmpty()) {
            return null
        }

        // Determine most likely floor based on strongest RSSI cluster
        val floorVotes = activeBeaconsWithData.groupBy { it.floorId }
        val dominantFloor = floorVotes.maxByOrNull { entry ->
            entry.value.sumOf { b ->
                val rssi = _smoothedRssiMap[b.beaconId] ?: -100f
                (100f + rssi).coerceAtLeast(1f).toDouble()
            }
        }?.key ?: (currentFloorHint ?: 0)

        // Beacons on the selected dominant floor
        val floorBeacons = activeBeaconsWithData.filter { it.floorId == dominantFloor }
        if (floorBeacons.isEmpty()) {
            val single = activeBeaconsWithData.maxByOrNull { _smoothedRssiMap[it.beaconId] ?: -100f } ?: return null
            return IndoorPosition(
                buildingId = targetBuildingId,
                floorId = single.floorId,
                x = single.x,
                y = single.y,
                nearestNodeId = single.nodeId,
                confidence = PositionConfidence.LOW,
                confidenceScore = 0.35f,
                source = PositionSource.BLE_BEACON,
                activeBeaconsCount = 1
            )
        }

        if (floorBeacons.size == 1) {
            val b = floorBeacons.first()
            val rssi = _smoothedRssiMap[b.beaconId] ?: -75f
            val dist = estimateDistance(rssi, b.txPower)
            return IndoorPosition(
                buildingId = targetBuildingId,
                floorId = b.floorId,
                x = b.x,
                y = b.y,
                nearestNodeId = b.nodeId,
                confidence = if (dist < 3.0f) PositionConfidence.MEDIUM else PositionConfidence.LOW,
                confidenceScore = if (dist < 3.0f) 0.65f else 0.40f,
                source = PositionSource.BLE_BEACON,
                activeBeaconsCount = 1
            )
        }

        // Multi-beacon inverse-distance weighted centroid positioning:
        // Weight w_i = 1 / (d_i ^ 2)
        var totalWeight = 0.0
        var weightedX = 0.0
        var weightedY = 0.0
        var minDistance = Float.MAX_VALUE

        for (b in floorBeacons) {
            val rssi = _smoothedRssiMap[b.beaconId] ?: -80f
            val distMeters = estimateDistance(rssi, b.txPower).coerceAtLeast(0.5f)
            if (distMeters < minDistance) minDistance = distMeters

            // Weight calculation
            val w = 1.0 / (distMeters.toDouble() * distMeters.toDouble())
            totalWeight += w
            weightedX += b.x * w
            weightedY += b.y * w
        }

        val finalX = if (totalWeight > 0.0) (weightedX / totalWeight).toFloat() else floorBeacons.first().x
        val finalY = if (totalWeight > 0.0) (weightedY / totalWeight).toFloat() else floorBeacons.first().y

        // Determine Confidence
        val confidence = when {
            floorBeacons.size >= 3 && minDistance < 3.5f -> PositionConfidence.HIGH
            floorBeacons.size >= 2 -> PositionConfidence.MEDIUM
            else -> PositionConfidence.LOW
        }
        val confidenceScore = when (confidence) {
            PositionConfidence.HIGH -> 0.90f
            PositionConfidence.MEDIUM -> 0.70f
            PositionConfidence.LOW -> 0.45f
        }

        val nearestBeacon = floorBeacons.minByOrNull { hypot(it.x - finalX, it.y - finalY) }

        return IndoorPosition(
            buildingId = targetBuildingId,
            floorId = dominantFloor,
            x = finalX,
            y = finalY,
            nearestNodeId = nearestBeacon?.nodeId,
            confidence = confidence,
            confidenceScore = confidenceScore,
            source = PositionSource.BLE_BEACON,
            activeBeaconsCount = floorBeacons.size
        )
    }

    /**
     * Injects a simulated beacon reading (Demo & Simulation Mode).
     */
    fun injectSimulatedReading(beaconId: String, rssi: Int) {
        updateRssiReading(beaconId, rssi)
    }

    /**
     * Resets internal beacon buffers.
     */
    fun clear() {
        _recentReadings.clear()
        _smoothedRssiMap.clear()
        _lastSeenMap.clear()
    }

    /**
     * Starts real hardware BLE scan if permissions and Bluetooth adapter are available.
     */
    fun startBleScanning(onBeaconDiscovered: (String, Int) -> Unit): Boolean {
        if (bluetoothAdapter == null || bluetoothAdapter?.isEnabled != true) {
            Log.w("BlePositionEngine", "Bluetooth is disabled or not supported on this device.")
            return false
        }

        try {
            bleScanner = bluetoothAdapter?.bluetoothLeScanner
            if (bleScanner == null) return false

            scanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult?) {
                    result?.let { res ->
                        val device = res.device
                        val rssi = res.rssi
                        val name = try { device.name } catch (e: SecurityException) { null } ?: device.address
                        
                        // Parse beacon ID from name or service UUID or address
                        val beaconId = if (name.startsWith("BEACON", ignoreCase = true)) {
                            name
                        } else {
                            "BEACON_${device.address.replace(":", "").takeLast(4).uppercase()}"
                        }
                        
                        updateRssiReading(beaconId, rssi)
                        onBeaconDiscovered(beaconId, rssi)
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    Log.e("BlePositionEngine", "BLE Scan failed with error code: $errorCode")
                    _isScanning.value = false
                }
            }

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            bleScanner?.startScan(null, settings, scanCallback)
            _isScanning.value = true
            return true
        } catch (e: SecurityException) {
            Log.w("BlePositionEngine", "Missing Bluetooth runtime permissions: ${e.message}")
            return false
        } catch (e: Exception) {
            Log.e("BlePositionEngine", "Error starting BLE scan: ${e.message}")
            return false
        }
    }

    fun stopBleScanning() {
        try {
            if (_isScanning.value && bleScanner != null && scanCallback != null) {
                bleScanner?.stopScan(scanCallback)
            }
        } catch (e: Exception) {
            Log.w("BlePositionEngine", "Error stopping BLE scan: ${e.message}")
        } finally {
            _isScanning.value = false
            scanCallback = null
        }
    }
}
