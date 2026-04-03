package com.smsrelay.service

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.smsrelay.ui.MainActivity

class RelayService : Service() {
    
    companion object {
        private const val TAG = "RelayService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "sms_relay_service"
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
        
        startForeground(NOTIFICATION_ID, createNotification())
        
        return START_STICKY // 服务被杀死后会重新启动
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null // 不需要绑定
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "短信转发服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于保持短信转发服务运行"
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
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
            errorCount > 0 -> "错误: $errorCount"
            forwardedCount > 0 -> "已转发: $forwardedCount"
            else -> "短信转发服务正在运行"
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("短信转发云")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
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