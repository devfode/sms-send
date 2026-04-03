package com.smsrelay.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.smsrelay.core.store.ConfigStore
import com.smsrelay.databinding.ActivityMainBinding
import com.smsrelay.mail.SmtpDefaults
import com.smsrelay.service.RelayService

/**
 * Main activity that handles permission requests on first launch
 * and starts the background service.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var configStore: ConfigStore

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            Log.d(TAG, "All SMS permissions granted")
            Toast.makeText(this, "SMS permissions granted", Toast.LENGTH_SHORT).show()
            startRelayService()
        } else {
            Log.w(TAG, "Some SMS permissions were denied")
            Toast.makeText(this, "SMS permission is required for forwarding", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configStore = ConfigStore(this)

        // Ensure hardcoded defaults are saved to ConfigStore if not already configured
        if (configStore.getSmtpConfig() == null) {
            configStore.saveSmtpConfig(SmtpDefaults.getDefaultConfig())
            Log.d(TAG, "Default SMTP config saved to ConfigStore")
        }

        setupUI()
        requestPermissions()
    }

    private fun setupUI() {
        updateStatus()

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun updateStatus() {
        val smtpConfig = configStore.getSmtpConfig() ?: SmtpDefaults.getDefaultConfig()

        binding.textStatus.text = if (smtpConfig.isValid()) {
            "SMS Forwarding Service - Running"
        } else {
            "SMS Forwarding Service - SMTP configuration required"
        }
    }

    private fun requestPermissions() {
        // 1. Request SMS permissions
        val smsPermissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            smsPermissions.add(Manifest.permission.RECEIVE_SMS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            smsPermissions.add(Manifest.permission.READ_SMS)
        }

        if (smsPermissions.isNotEmpty()) {
            smsPermissionLauncher.launch(smsPermissions.toTypedArray())
        } else {
            // Permissions already granted, start service
            startRelayService()
        }

        // 2. Request battery optimization exclusion
        requestBatteryOptimizationExclusion()
    }

    /**
     * Requests the system to exclude this app from battery optimizations.
     * This is critical for the background service to run persistently.
     */
    private fun requestBatteryOptimizationExclusion() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager

        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            Log.d(TAG, "Requesting battery optimization exclusion")
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request battery optimization exclusion", e)
            }
        } else {
            Log.d(TAG, "Battery optimization already excluded")
        }
    }

    private fun startRelayService() {
        val serviceIntent = Intent(this, RelayService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        Log.d(TAG, "RelayService started from MainActivity")
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }
}