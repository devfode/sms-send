package com.smsrelay.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.smsrelay.core.store.ConfigStore

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
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
        try {
            // Check if SMTP is configured before starting service
            val configStore = ConfigStore(context)
            val smtpConfig = configStore.getSmtpConfig()
            
            if (smtpConfig != null && smtpConfig.isValid()) {
                val serviceIntent = Intent(context, RelayService::class.java)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                
                Log.d(TAG, "RelayService started from boot receiver")
            } else {
                Log.d(TAG, "SMTP not configured, skipping service start")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start service from boot receiver", e)
        }
    }
}