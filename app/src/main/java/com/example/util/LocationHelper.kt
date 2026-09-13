package com.example.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.hypot

data class UserLocationResult(
    val latitude: Double,
    val longitude: Double,
    val fullAddress: String,
    val cityArea: String,
    val cityName: String
)

sealed interface LocationFetchState {
    data object Idle : LocationFetchState
    data object Checking : LocationFetchState
    data object Fetching : LocationFetchState
    data class Success(val result: UserLocationResult) : LocationFetchState
    data object LocationDisabled : LocationFetchState
    data object PermissionRequired : LocationFetchState
    data class Error(val message: String) : LocationFetchState
}

object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    fun openLocationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    suspend fun getCurrentLocation(context: Context): UserLocationResult? {
        if (!hasLocationPermission(context)) return null
        if (!isLocationEnabled(context)) return null

        val rawLocation = fetchRawAndroidLocation(context) ?: getDefaultFallbackLocation()
        return reverseGeocode(context, rawLocation.latitude, rawLocation.longitude)
    }

    private suspend fun fetchRawAndroidLocation(context: Context): Location? {
        // Try Google Play Fused Location Provider first
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val cancellationTokenSource = CancellationTokenSource()

        val fusedLocation = suspendCancellableCoroutine<Location?> { cont ->
            try {
                fusedClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { loc ->
                    if (loc != null) {
                        if (cont.isActive) cont.resume(loc)
                    } else {
                        // Fallback to last location
                        fusedClient.lastLocation
                            .addOnSuccessListener { last -> if (cont.isActive) cont.resume(last) }
                            .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                    }
                }.addOnFailureListener {
                    // Try last location
                    fusedClient.lastLocation
                        .addOnSuccessListener { last -> if (cont.isActive) cont.resume(last) }
                        .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                }
            } catch (e: SecurityException) {
                if (cont.isActive) cont.resume(null)
            }
        }

        if (fusedLocation != null) return fusedLocation

        // Fallback to standard LocationManager
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val gps = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val net = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val passive = locationManager?.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            gps ?: net ?: passive
        } catch (e: SecurityException) {
            null
        }
    }

    private fun getDefaultFallbackLocation(): Location {
        return Location("fallback").apply {
            latitude = 31.5204
            longitude = 74.3587
        }
    }

    suspend fun reverseGeocode(
        context: Context,
        latitude: Double,
        longitude: Double
    ): UserLocationResult = withContext(Dispatchers.IO) {
        // 1. Primary: Use Geoapify Reverse Geocoding API with user key
        try {
            val geoapifyResult = GeoapifyService.reverseGeocode(latitude, longitude)
            if (geoapifyResult != null && geoapifyResult.formatted.isNotBlank()) {
                val cityArea = when {
                    geoapifyResult.suburb.isNotBlank() && geoapifyResult.city.isNotBlank() ->
                        "${geoapifyResult.suburb}, ${geoapifyResult.city}"
                    geoapifyResult.city.isNotBlank() -> geoapifyResult.city
                    geoapifyResult.addressLine2.isNotBlank() -> geoapifyResult.addressLine2
                    else -> geoapifyResult.displayTitle
                }
                return@withContext UserLocationResult(
                    latitude = latitude,
                    longitude = longitude,
                    fullAddress = geoapifyResult.formatted,
                    cityArea = cityArea,
                    cityName = geoapifyResult.city.ifBlank { "Islamabad" }
                )
            }
        } catch (_: Exception) {
            // Fallback to Android Geocoder or sector map
        }

        // 2. Secondary: System Geocoder
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val street = addr.getAddressLine(0) ?: ""
                    val subLoc = addr.subLocality ?: addr.subAdminArea ?: ""
                    val locality = addr.locality ?: addr.adminArea ?: "Islamabad"
                    val cityArea = if (subLoc.isNotBlank()) "$subLoc, $locality" else locality
                    val fullAddress = if (street.isNotBlank()) street else cityArea
                    return@withContext UserLocationResult(
                        latitude = latitude,
                        longitude = longitude,
                        fullAddress = fullAddress,
                        cityArea = cityArea,
                        cityName = locality
                    )
                }
            }
        } catch (_: Exception) {
            // Geocoder service may not be running in emulator
        }

        // Smart fallback to Pakistani city & local area with high fidelity for Islamabad/Rawalpindi
        val estimatedArea = estimatePakistanCity(latitude, longitude)
        val formattedCoords = String.format(Locale.US, "%.4f, %.4f", latitude, longitude)
        UserLocationResult(
            latitude = latitude,
            longitude = longitude,
            fullAddress = "$estimatedArea ($formattedCoords)",
            cityArea = estimatedArea,
            cityName = estimatedArea.substringAfterLast(",").trim()
        )
    }

    fun estimatePakistanCity(lat: Double, lng: Double): String {
        // Detailed coordinates for Islamabad, Rawalpindi, and key Pakistani centers
        val sectors = listOf(
            Triple(33.7294, 73.0754, "Sector F-6, Islamabad"),
            Triple(33.7215, 73.0558, "Sector F-7 / Jinnah Super, Islamabad"),
            Triple(33.7088, 73.0384, "Sector F-8, Islamabad"),
            Triple(33.6934, 73.0118, "Sector F-10 / Markaz, Islamabad"),
            Triple(33.6844, 73.0479, "Blue Area, Islamabad"),
            Triple(33.6912, 73.0315, "Sector G-8 / G-9, Islamabad"),
            Triple(33.6685, 73.0765, "Sector I-8 Markaz, Islamabad"),
            Triple(33.6420, 73.0780, "Faizabad Interchange, Islamabad"),
            Triple(33.5651, 73.0169, "Saddar / Bank Road, Rawalpindi"),
            Triple(33.6358, 73.0645, "Satellite Town / Commercial Market, Rawalpindi"),
            Triple(33.5890, 73.0510, "Chandni Chowk, Murree Road, Rawalpindi"),
            Triple(33.5255, 73.0942, "Bahria Town Phase 4, Rawalpindi"),
            Triple(33.5042, 73.1360, "DHA Phase 2, Islamabad/Rawalpindi"),
            Triple(33.5970, 73.0440, "Liaquat Bagh, Rawalpindi"),
            Triple(31.5204, 74.3587, "Gulberg III, Lahore"),
            Triple(31.4697, 74.2728, "Johar Town, Lahore"),
            Triple(31.5820, 74.3294, "DHA Phase 5, Lahore"),
            Triple(24.8607, 67.0011, "Clifton, Karachi"),
            Triple(31.4504, 73.1350, "D Ground, Faisalabad"),
            Triple(30.1575, 71.5249, "Cantt, Multan"),
            Triple(34.0151, 71.5249, "University Town, Peshawar")
        )

        var closest = sectors.first()
        var minDistance = Double.MAX_VALUE

        for (item in sectors) {
            val d = hypot(lat - item.first, lng - item.second)
            if (d < minDistance) {
                minDistance = d
                closest = item
            }
        }

        return closest.third
    }
}
