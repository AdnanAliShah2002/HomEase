package com.example.util

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Data model for a Geoapify geocoded place / autocomplete result.
 */
data class GeoapifyPlace(
    val formatted: String,
    val name: String,
    val addressLine1: String,
    val addressLine2: String,
    val city: String,
    val suburb: String,
    val lat: Double,
    val lng: Double
) {
    val displayTitle: String
        get() = when {
            name.isNotBlank() && name != formatted -> name
            addressLine1.isNotBlank() -> addressLine1
            suburb.isNotBlank() -> suburb
            else -> formatted.substringBefore(",")
        }

    val displaySubtitle: String
        get() = when {
            addressLine2.isNotBlank() -> addressLine2
            city.isNotBlank() && suburb.isNotBlank() && !formatted.startsWith(suburb) -> "$suburb, $city"
            city.isNotBlank() -> city
            else -> formatted
        }
}

/**
 * Result from Geoapify Driving Routing API.
 */
data class GeoapifyRoute(
    val distanceMeters: Double,
    val durationSeconds: Double,
    val distanceKm: Double = distanceMeters / 1000.0,
    val etaMinutes: Int = Math.max(1, Math.round(durationSeconds / 60.0).toInt()),
    val waypoints: List<Pair<Double, Double>> = emptyList(),
    val instructions: List<String> = emptyList()
)

/**
 * Marker descriptor for Geoapify Static Maps.
 */
data class GeoapifyMapMarker(
    val lat: Double,
    val lng: Double,
    val colorHex: String = "#4F46E5",
    val icon: String = "circle", // circle, home, motorcycle, car, crosshairs
    val size: String = "medium"  // small, medium, large
)

/**
 * Production-ready client for Geoapify APIs:
 * - Autocomplete Address Search
 * - Forward & Reverse Geocoding
 * - Turn-by-turn Driving Route & Real Driving Distance/ETA
 * - High-Resolution Static Map & Live Tracking Views
 */
object GeoapifyService {
    private const val TAG = "GeoapifyService"

    // User-configured API Key
    const val API_KEY: String = "6aeb38ece1144d409b4e3a84261139a8"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Search places and addresses with Geoapify Autocomplete API.
     * Biased towards Pakistan (filter=countrycode:pk) and Islamabad/Rawalpindi proximity.
     */
    suspend fun searchAutocomplete(
        query: String,
        proximityLat: Double = 33.6844,
        proximityLng: Double = 73.0479,
        limit: Int = 8
    ): List<GeoapifyPlace> = withContext(Dispatchers.IO) {
        if (query.trim().length < 2) return@withContext emptyList()

        try {
            val encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.toString())
            val url = "https://api.geoapify.com/v1/geocode/autocomplete" +
                    "?text=$encodedQuery" +
                    "&filter=countrycode:pk" +
                    "&bias=proximity:$proximityLng,$proximityLat" +
                    "&limit=$limit" +
                    "&apiKey=$API_KEY"

            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Autocomplete HTTP error: ${response.code}")
                    return@withContext emptyList()
                }

                val bodyString = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(bodyString)
                val features = json.optJSONArray("features") ?: return@withContext emptyList()

                val results = mutableListOf<GeoapifyPlace>()
                for (i in 0 until features.length()) {
                    val feature = features.getJSONObject(i)
                    val properties = feature.optJSONObject("properties") ?: continue

                    val formatted = properties.optString("formatted", "")
                    val name = properties.optString("name", "")
                    val addressLine1 = properties.optString("address_line1", "")
                    val addressLine2 = properties.optString("address_line2", "")
                    val city = properties.optString("city", "")
                    val suburb = properties.optString("suburb", properties.optString("district", ""))
                    val lon = properties.optDouble("lon", 0.0)
                    val lat = properties.optDouble("lat", 0.0)

                    if (lat != 0.0 && lon != 0.0) {
                        results.add(
                            GeoapifyPlace(
                                formatted = formatted.ifBlank { name },
                                name = name,
                                addressLine1 = addressLine1,
                                addressLine2 = addressLine2,
                                city = city,
                                suburb = suburb,
                                lat = lat,
                                lng = lon
                            )
                        )
                    }
                }
                return@withContext results
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in Geoapify autocomplete", e)
            emptyList()
        }
    }

    /**
     * Reverse geocodes latitude and longitude into a structured Pakistani address.
     */
    suspend fun reverseGeocode(lat: Double, lng: Double): GeoapifyPlace? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.geoapify.com/v1/geocode/reverse" +
                    "?lat=$lat" +
                    "&lon=$lng" +
                    "&apiKey=$API_KEY"

            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Reverse geocode HTTP error: ${response.code}")
                    return@withContext null
                }

                val bodyString = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyString)
                val features = json.optJSONArray("features") ?: return@withContext null
                if (features.length() == 0) return@withContext null

                val feature = features.getJSONObject(0)
                val properties = feature.optJSONObject("properties") ?: return@withContext null

                val formatted = properties.optString("formatted", "")
                val name = properties.optString("name", "")
                val addressLine1 = properties.optString("address_line1", "")
                val addressLine2 = properties.optString("address_line2", "")
                val city = properties.optString("city", "")
                val suburb = properties.optString("suburb", properties.optString("district", ""))
                val lon = properties.optDouble("lon", lng)
                val resolvedLat = properties.optDouble("lat", lat)

                return@withContext GeoapifyPlace(
                    formatted = formatted.ifBlank { name },
                    name = name,
                    addressLine1 = addressLine1,
                    addressLine2 = addressLine2,
                    city = city,
                    suburb = suburb,
                    lat = resolvedLat,
                    lng = lon
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in Geoapify reverseGeocode", e)
            null
        }
    }

    /**
     * Fetches driving route, real driving distance in meters, and travel time from Geoapify Routing API.
     */
    suspend fun getDrivingRoute(
        startLat: Double,
        startLng: Double,
        destLat: Double,
        destLng: Double
    ): GeoapifyRoute? = withContext(Dispatchers.IO) {
        try {
            val waypointsParam = String.format(Locale.US, "%.5f,%.5f|%.5f,%.5f", startLat, startLng, destLat, destLng)
            val url = "https://api.geoapify.com/v1/routing" +
                    "?waypoints=$waypointsParam" +
                    "&mode=drive" +
                    "&apiKey=$API_KEY"

            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Routing HTTP error: ${response.code}")
                    return@withContext null
                }

                val bodyString = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyString)
                val features = json.optJSONArray("features") ?: return@withContext null
                if (features.length() == 0) return@withContext null

                val routeFeature = features.getJSONObject(0)
                val properties = routeFeature.optJSONObject("properties") ?: return@withContext null
                val distance = properties.optDouble("distance", 0.0)
                val time = properties.optDouble("time", 0.0)

                // Extract polyline waypoints if present
                val geometry = routeFeature.optJSONObject("geometry")
                val coordsList = mutableListOf<Pair<Double, Double>>()
                if (geometry != null) {
                    val coordsArray = geometry.optJSONArray("coordinates")
                    if (coordsArray != null) {
                        // GeoJSON coordinates can be LineString [ [lon, lat], ... ] or MultiLineString [ [ [lon, lat] ] ]
                        if (coordsArray.length() > 0 && coordsArray.optJSONArray(0)?.optJSONArray(0) != null) {
                            val multiLine = coordsArray.getJSONArray(0)
                            for (j in 0 until multiLine.length()) {
                                val pt = multiLine.getJSONArray(j)
                                coordsList.add(Pair(pt.getDouble(1), pt.getDouble(0))) // (lat, lng)
                            }
                        } else {
                            for (j in 0 until coordsArray.length()) {
                                val pt = coordsArray.optJSONArray(j)
                                if (pt != null && pt.length() >= 2) {
                                    coordsList.add(Pair(pt.getDouble(1), pt.getDouble(0))) // (lat, lng)
                                }
                            }
                        }
                    }
                }

                // Extract step instructions
                val legs = properties.optJSONArray("legs")
                val instructions = mutableListOf<String>()
                if (legs != null && legs.length() > 0) {
                    val steps = legs.getJSONObject(0).optJSONArray("steps")
                    if (steps != null) {
                        for (s in 0 until steps.length()) {
                            val step = steps.getJSONObject(s)
                            val instr = step.optJSONObject("instruction")?.optString("text")
                            if (!instr.isNullOrBlank()) {
                                instructions.add(instr)
                            }
                        }
                    }
                }

                return@withContext GeoapifyRoute(
                    distanceMeters = distance,
                    durationSeconds = time,
                    waypoints = coordsList,
                    instructions = instructions
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in Geoapify getDrivingRoute", e)
            null
        }
    }

    /**
     * Builds a URL for Geoapify Static Maps API.
     * Displays clean, high-resolution OpenStreetMap Bright styling with custom markers.
     */
    fun buildStaticMapUrl(
        centerLat: Double? = null,
        centerLng: Double? = null,
        zoom: Int = 14,
        width: Int = 800,
        height: Int = 600,
        style: String = "osm-bright",
        markers: List<GeoapifyMapMarker> = emptyList()
    ): String {
        val sb = StringBuilder("https://maps.geoapify.com/v1/staticmap?style=$style&width=$width&height=$height")

        if (centerLat != null && centerLng != null) {
            val formattedLonLat = String.format(Locale.US, "%.5f,%.5f", centerLng, centerLat)
            sb.append("&center=lonlat:$formattedLonLat&zoom=$zoom")
        }

        if (markers.isNotEmpty()) {
            val markerStrings = markers.map { m ->
                val colorEncoded = URLEncoder.encode(m.colorHex, StandardCharsets.UTF_8.toString())
                val lonLat = String.format(Locale.US, "%.5f,%.5f", m.lng, m.lat)
                "lonlat:$lonLat;type:awesome;color:$colorEncoded;size:${m.size};icon:${m.icon}"
            }
            sb.append("&marker=").append(markerStrings.joinToString("|"))
        }

        sb.append("&apiKey=$API_KEY")
        return sb.toString()
    }

    /**
     * Builds a URL for Geoapify raster map tiles (Standard 256x256 Web Mercator tiles).
     * e.g. https://maps.geoapify.com/v1/tile/osm-bright/{z}/{x}/{y}.png?apiKey={apiKey}
     */
    fun buildTileUrl(zoom: Int, x: Int, y: Int, style: String = "osm-bright"): String {
        return "https://maps.geoapify.com/v1/tile/$style/$zoom/$x/$y.png?apiKey=$API_KEY"
    }

    /**
     * Builds a dedicated live tracking map URL that automatically frames both
     * the customer delivery destination and provider location.
     */
    fun buildLiveTrackingMapUrl(
        providerLat: Double,
        providerLng: Double,
        customerLat: Double,
        customerLng: Double,
        width: Int = 800,
        height: Int = 600
    ): String {
        val customerMarker = GeoapifyMapMarker(
            lat = customerLat,
            lng = customerLng,
            colorHex = "#4F46E5", // Deep Indigo
            icon = "home",
            size = "medium"
        )
        val providerMarker = GeoapifyMapMarker(
            lat = providerLat,
            lng = providerLng,
            colorHex = "#059669", // Emerald Green
            icon = "motorcycle",
            size = "medium"
        )
        return buildStaticMapUrl(
            width = width,
            height = height,
            style = "osm-bright",
            markers = listOf(customerMarker, providerMarker)
        )
    }
}
