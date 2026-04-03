package com.smsrelay.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log

/**
 * BroadcastReceiver that starts RelayService on device boot or app update.
 * Uses a temporary WakeLock to ensure the service starts before the CPU sleeps.
 * Always starts the service (hardcoded SMTP defaults guarantee valid config).
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
        private const val WAKELOCK_TAG = "smsrelay:boot_wakelock"
        private const val WAKELOCK_TIMEOUT_MS = 10_000L // 10 seconds
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Boot receiver triggered: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                startRelayService(context)
            }
        }
    }

    private fun startRelayService(context: Context) {
        // Acquire a temporary WakeLock to prevent CPU from sleeping
        // before the foreground service has a chance to start
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKELOCK_TAG
        )

        try {
            wakeLock.acquire(WAKELOCK_TIMEOUT_MS)
            Log.d(TAG, "WakeLock acquired for service startup")

            val serviceIntent = Intent(context, RelayService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }

            Log.d(TAG, "RelayService started from boot receiver")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start service from boot receiver", e)
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
                Log.d(TAG, "WakeLock released after service startup")
            }
        }
    }
}