package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class HomEaseFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "HomEaseFCM"
        const val CHANNEL_JOB_PINGS = "homease_job_pings"
        const val CHANNEL_OFFERS = "homease_offers"
        const val CHANNEL_STATUS = "homease_status"
        const val CHANNEL_CHAT = "homease_chat"

        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return

                val jobPingChannel = NotificationChannel(
                    CHANNEL_JOB_PINGS,
                    "Job Alerts (New Requests)",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Urgent alerts when a customer posts a job matching your skills"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 300, 200, 300)
                }

                val offersChannel = NotificationChannel(
                    CHANNEL_OFFERS,
                    "Job Offers & Counter-Offers",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications when service providers send quotes or counter-offers"
                    enableVibration(true)
                }

                val statusChannel = NotificationChannel(
                    CHANNEL_STATUS,
                    "Job Status Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Updates on provider arrival, job start, and completion"
                }

                val chatChannel = NotificationChannel(
                    CHANNEL_CHAT,
                    "In-App Messages",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Direct chat messages between customer and service provider"
                }

                notificationManager.createNotificationChannels(
                    listOf(jobPingChannel, offersChannel, statusChannel, chatChannel)
                )
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token generated: $token")
        val prefs = getSharedPreferences("homease_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")

        createNotificationChannels(this)

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val type = data["type"] ?: "generic"
        val jobId = data["job_id"] ?: ""
        val title = notification?.title ?: data["title"] ?: when (type) {
            "job_ping" -> "New Service Request Nearby!"
            "offer_received" -> "New Offer Received"
            "offer_accepted" -> "Your Offer Was Accepted!"
            "status_update" -> "Job Status Update"
            "new_message" -> "New Message"
            else -> "HomEase Alert"
        }

        val body = notification?.body ?: data["body"] ?: when (type) {
            "job_ping" -> data["service_title"] ?: "A customer needs assistance in your area."
            "offer_received" -> "Provider quoted Rs. ${data["price_rs"] ?: ""}"
            "offer_accepted" -> "Customer accepted your quote. Tap to view job details."
            "status_update" -> data["status_text"] ?: "Job status has been updated."
            "new_message" -> data["message"] ?: "You received a new message."
            else -> "Tap to open HomEase."
        }

        val channelId = when (type) {
            "job_ping" -> CHANNEL_JOB_PINGS
            "offer_received" -> CHANNEL_OFFERS
            "offer_accepted", "status_update" -> CHANNEL_STATUS
            "new_message" -> CHANNEL_CHAT
            else -> CHANNEL_STATUS
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("job_id", jobId)
            putExtra("notification_type", type)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            jobId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSound)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifId = if (jobId.isNotEmpty()) jobId.hashCode() else System.currentTimeMillis().toInt()
        notificationManager.notify(notifId, builder.build())
    }
}
