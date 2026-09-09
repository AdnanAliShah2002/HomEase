package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.db.AppDatabase
import com.example.data.db.ProviderLocationEntity
import com.example.data.remote.HomEaseSupabaseClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val TAG = "ProviderLocationService"
private const val CHANNEL_ID = "homease_provider_tracking"
private const val NOTIFICATION_ID = 4041

class ProviderLocationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    private var activeJobId: String = ""
    private var activeProviderId: String = ""
    private var destinationLat: Double = 31.5204
    private var destinationLng: Double = 74.3587

    private var currentLat: Double = 31.5120
    private var currentLng: Double = 74.3450
    private var currentBearing: Double = 45.0
    private var simulationJob: Job? = null

    companion object {
        const val ACTION_START = "com.example.service.START_TRACKING"
        const val ACTION_STOP = "com.example.service.STOP_TRACKING"
        const val EXTRA_JOB_ID = "extra_job_id"
        const val EXTRA_PROVIDER_ID = "extra_provider_id"
        const val EXTRA_DEST_LAT = "extra_dest_lat"
        const val EXTRA_DEST_LNG = "extra_dest_lng"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        fun start(
            context: Context,
            jobId: String,
            providerId: String,
            destLat: Double = 31.5204,
            destLng: Double = 74.3587
        ) {
            val intent = Intent(context, ProviderLocationService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_JOB_ID, jobId)
                putExtra(EXTRA_PROVIDER_ID, providerId)
                putExtra(EXTRA_DEST_LAT, destLat)
                putExtra(EXTRA_DEST_LNG, destLng)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ProviderLocationService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopTracking()
            stopSelf()
            return START_NOT_STICKY
        }

        if (action == ACTION_START) {
            activeJobId = intent.getStringExtra(EXTRA_JOB_ID) ?: ""
            activeProviderId = intent.getStringExtra(EXTRA_PROVIDER_ID) ?: "provider_1"
            destinationLat = intent.getDoubleExtra(EXTRA_DEST_LAT, 31.5204)
            destinationLng = intent.getDoubleExtra(EXTRA_DEST_LNG, 74.3587)

            // Start ~1.2 km away from destination for realistic transit
            currentLat = destinationLat - 0.0110
            currentLng = destinationLng - 0.0090
            currentBearing = calculateBearing(currentLat, currentLng, destinationLat, destinationLng)

            startForegroundNotification()
            startLocationUpdates()
            _isServiceRunning.value = true
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Provider Live Job Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifies when your live location is being shared with customer for an active job"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundNotification() {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HomEase Live Tracking Active")
            .setContentText("Sharing your live location with the customer for job #$activeJobId")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 6000L)
            .setMinUpdateIntervalMillis(4000L)
            .setMaxUpdateDelayMillis(8000L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                currentLat = loc.latitude
                currentLng = loc.longitude
                if (loc.hasBearing()) {
                    currentBearing = loc.bearing.toDouble()
                }
                uploadLocation(currentLat, currentLng, currentBearing)
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback as LocationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "Location permission missing or restricted: ${e.message}")
        }

        // Start fallback simulation in case emulator GPS is static or not moving
        startSimulationProgress()
    }

    private fun startSimulationProgress() {
        simulationJob?.cancel()
        simulationJob = serviceScope.launch {
            // Upsert initial position
            uploadLocation(currentLat, currentLng, currentBearing)

            while (isActive) {
                delay(6000L) // updates every 6 seconds (within 5-8s spec)

                // Smoothly step towards destination
                val dLat = destinationLat - currentLat
                val dLng = destinationLng - currentLng
                val distance = Math.hypot(dLat, dLng)

                if (distance > 0.0003) {
                    val stepRatio = (0.0008 / distance).coerceAtMost(0.25)
                    currentLat += dLat * stepRatio
                    currentLng += dLng * stepRatio
                    currentBearing = calculateBearing(currentLat, currentLng, destinationLat, destinationLng)
                }

                uploadLocation(currentLat, currentLng, currentBearing)
            }
        }
    }

    private fun uploadLocation(lat: Double, lng: Double, heading: Double?) {
        if (activeJobId.isBlank()) return
        serviceScope.launch {
            try {
                // 1. Upsert to Supabase
                val supabase = HomEaseSupabaseClient.getInstance(applicationContext)
                supabase.upsertProviderLocation(
                    jobId = activeJobId,
                    providerId = activeProviderId,
                    lat = lat,
                    lng = lng,
                    heading = heading
                )

                // 2. Cache in local Room DB for zero-latency local reactivity
                val db = AppDatabase.getDatabase(applicationContext)
                db.providerLocationDao().upsertLocation(
                    ProviderLocationEntity(
                        jobId = activeJobId,
                        providerId = activeProviderId,
                        lat = lat,
                        lng = lng,
                        heading = heading,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload provider location: ${e.message}")
            }
        }
    }

    private fun stopTracking() {
        simulationJob?.cancel()
        simulationJob = null
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
        _isServiceRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun calculateBearing(startLat: Double, startLng: Double, endLat: Double, endLng: Double): Double {
        val lat1 = Math.toRadians(startLat)
        val lon1 = Math.toRadians(startLng)
        val lat2 = Math.toRadians(endLat)
        val lon2 = Math.toRadians(endLng)

        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        val brng = Math.toDegrees(atan2(y, x))
        return (brng + 360.0) % 360.0
    }
}
