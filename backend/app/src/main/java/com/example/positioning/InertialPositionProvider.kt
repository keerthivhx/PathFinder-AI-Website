package com.example.positioning

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.cos
import kotlin.math.sin

class InertialPositionProvider(
    private val context: Context? = null,
    var averageStepLengthMeters: Float = 0.75f // Configurable step length (default ~75cm)
) : SensorEventListener {

    private val sensorManager: SensorManager? by lazy {
        context?.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }

    private var stepDetectorSensor: Sensor? = null
    private var rotationVectorSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var magnetometerSensor: Sensor? = null

    private val _stepCount = MutableStateFlow(0)
    val stepCount: StateFlow<Int> = _stepCount.asStateFlow()

    private val _headingDegrees = MutableStateFlow(0f)
    val headingDegrees: StateFlow<Float> = _headingDegrees.asStateFlow()

    private val _isSensorActive = MutableStateFlow(false)
    val isSensorActive: StateFlow<Boolean> = _isSensorActive.asStateFlow()

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private val lastAccelerometer = FloatArray(3)
    private val lastMagnetometer = FloatArray(3)
    private var lastAccelerometerSet = false
    private var lastMagnetometerSet = false

    private var onStepDetectedCallback: ((deltaX: Float, deltaY: Float, headingDeg: Float) -> Unit)? = null

    init {
        stepDetectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    fun startListening(onStepDetected: (deltaX: Float, deltaY: Float, headingDeg: Float) -> Unit) {
        onStepDetectedCallback = onStepDetected
        if (sensorManager == null) {
            Log.w("InertialPositionProvider", "SensorManager unavailable.")
            return
        }

        try {
            stepDetectorSensor?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
            if (rotationVectorSensor != null) {
                sensorManager?.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
            } else {
                sensorManager?.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI)
                sensorManager?.registerListener(this, magnetometerSensor, SensorManager.SENSOR_DELAY_UI)
            }
            _isSensorActive.value = true
        } catch (e: Exception) {
            Log.w("InertialPositionProvider", "Error registering sensor listeners: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            sensorManager?.unregisterListener(this)
        } catch (e: Exception) {
            Log.w("InertialPositionProvider", "Error unregistering sensor listeners: ${e.message}")
        } finally {
            _isSensorActive.value = false
            onStepDetectedCallback = null
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> {
                _stepCount.value += 1
                val heading = _headingDegrees.value
                val rad = Math.toRadians(heading.toDouble())
                // In canvas space (0..1000 across typical ~100m building), 1 meter ~ 10 units
                val canvasScaleUnitsPerMeter = 10f
                val stepCanvasUnits = averageStepLengthMeters * canvasScaleUnitsPerMeter
                val dx = (sin(rad) * stepCanvasUnits).toFloat()
                val dy = (-cos(rad) * stepCanvasUnits).toFloat()
                onStepDetectedCallback?.invoke(dx, dy, heading)
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val azimuthRad = orientationAngles[0]
                var azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
                if (azimuthDeg < 0) azimuthDeg += 360f
                _headingDegrees.value = azimuthDeg
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.size)
                lastAccelerometerSet = true
                updateOrientationFallback()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.size)
                lastMagnetometerSet = true
                updateOrientationFallback()
            }
        }
    }

    private fun updateOrientationFallback() {
        if (lastAccelerometerSet && lastMagnetometerSet) {
            if (SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer)) {
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val azimuthRad = orientationAngles[0]
                var azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
                if (azimuthDeg < 0) azimuthDeg += 360f
                _headingDegrees.value = azimuthDeg
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /**
     * Manual / simulated step trigger for demo mode.
     */
    fun triggerSimulatedStep(headingDegreesOverride: Float? = null) {
        _stepCount.value += 1
        val heading = headingDegreesOverride ?: _headingDegrees.value
        val rad = Math.toRadians(heading.toDouble())
        val canvasScaleUnitsPerMeter = 10f
        val stepCanvasUnits = averageStepLengthMeters * canvasScaleUnitsPerMeter
        val dx = (sin(rad) * stepCanvasUnits).toFloat()
        val dy = (-cos(rad) * stepCanvasUnits).toFloat()
        onStepDetectedCallback?.invoke(dx, dy, heading)
    }

    fun setHeadingOverride(deg: Float) {
        _headingDegrees.value = (deg % 360f + 360f) % 360f
    }
}
