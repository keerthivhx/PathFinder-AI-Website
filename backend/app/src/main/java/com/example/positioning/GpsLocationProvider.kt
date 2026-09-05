package com.example.positioning

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import com.example.data.model.Building
import com.example.data.model.BuildingEntrance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

class GpsLocationProvider(
    private val context: Context? = null
) : LocationListener {

    private val locationManager: LocationManager? by lazy {
        context?.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    }

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _isGpsActive = MutableStateFlow(false)
    val isGpsActive: StateFlow<Boolean> = _isGpsActive.asStateFlow()

    fun startListening() {
        if (locationManager == null) {
            Log.w("GpsLocationProvider", "LocationManager is null.")
            return
        }

        try {
            val hasGps = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
            val hasNetwork = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

            if (hasGps) {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L,
                    2f,
                    this
                )
                _isGpsActive.value = true
            } else if (hasNetwork) {
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    3000L,
                    5f,
                    this
                )
                _isGpsActive.value = true
            } else {
                Log.w("GpsLocationProvider", "Neither GPS nor Network location provider is enabled.")
            }
        } catch (e: SecurityException) {
            Log.w("GpsLocationProvider", "Missing ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION: ${e.message}")
        } catch (e: Exception) {
            Log.e("GpsLocationProvider", "Error registering location listener: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            locationManager?.removeUpdates(this)
        } catch (e: Exception) {
            Log.w("GpsLocationProvider", "Error removing location updates: ${e.message}")
        } finally {
            _isGpsActive.value = false
        }
    }

    override fun onLocationChanged(location: Location) {
        _currentLocation.value = location
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    /**
     * Injects a simulated GPS coordinate (e.g. For Demo Mode / Testing).
     */
    fun setSimulatedLocation(lat: Double, lng: Double, accuracyMeters: Float = 5f) {
        val loc = Location("simulation").apply {
            latitude = lat
            longitude = lng
            accuracy = accuracyMeters
            time = System.currentTimeMillis()
        }
        _currentLocation.value = loc
    }

    fun simulateGpsLocation(latitude: Double, longitude: Double) {
        setSimulatedLocation(latitude, longitude)
    }

    /**
     * Calculates distance between user and building in meters using Haversine formula.
     */
    fun calculateDistanceToBuilding(building: Building): Float? {
        val loc = _currentLocation.value ?: return null
        return calculateHaversineDistance(loc.latitude, loc.longitude, building.latitude, building.longitude)
    }

    /**
     * Finds the nearest entrance to user GPS location.
     */
    fun findNearestEntrance(building: Building, wheelchairOnly: Boolean = false): Pair<BuildingEntrance, Float>? {
        val loc = _currentLocation.value ?: return null
        val candidates = if (wheelchairOnly) {
            building.entrances.filter { it.isWheelchairAccessible }
        } else {
            building.entrances
        }.ifEmpty { building.entrances }

        if (candidates.isEmpty()) return null

        var bestEntrance = candidates.first()
        var minDistance = Float.MAX_VALUE

        for (ent in candidates) {
            val dist = calculateHaversineDistance(loc.latitude, loc.longitude, ent.latitude, ent.longitude)
            if (dist < minDistance) {
                minDistance = dist
                bestEntrance = ent
            }
        }

        return Pair(bestEntrance, minDistance)
    }

    companion object {
        /**
         * Calculates spherical distance in meters using Haversine formula.
         */
        fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
            val r = 6371000.0 // Earth radius in meters
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return (r * c).toFloat()
        }
    }
}
