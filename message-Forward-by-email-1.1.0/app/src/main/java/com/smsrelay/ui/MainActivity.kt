package com.smsrelay.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.smsrelay.core.store.ConfigStore
import com.smsrelay.core.utils.PermissionHelper
import com.smsrelay.databinding.ActivityMainBinding
import com.smsrelay.service.RelayService

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var configStore: ConfigStore
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            configStore = ConfigStore(this)
            setupUI()
            checkPermissionsAndStartService()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in onCreate", e)
            // 如果出现严重错误，显示简单的错误信息而不是崩溃
            try {
                android.widget.Toast.makeText(this, "应用启动出错，请重新安装", android.widget.Toast.LENGTH_LONG).show()
            } catch (ignored: Exception) {
                // Ignore toast errors
            }
            finish()
        }
    }
    
    private fun setupUI() {
        updateStatus()
        
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        binding.btnRecords.setOnClickListener {
            startActivity(Intent(this, RecordsActivity::class.java))
        }
        
        binding.btnCheckPermissions?.setOnClickListener {
            PermissionHelper.requestAllRequiredPermissions(this) {
                updateStatus()
                startServiceIfConfigured()
            }
        }
    }
    
    private fun updateStatus() {
        val smtpConfig = configStore.getSmtpConfig()
        val hasPermissions = PermissionHelper.checkAllRequiredPermissions(this)
        
        binding.textStatus.text = when {
            !hasPermissions -> "短信转发服务 - 需要权限"
            smtpConfig == null || !smtpConfig.isValid() -> "短信转发服务 - 需要配置SMTP"
            else -> "短信转发服务 - 运行中"
        }
        
        // Show/hide permissions button based on permission status
        binding.btnCheckPermissions?.visibility = if (hasPermissions) {
            android.view.View.GONE
        } else {
            android.view.View.VISIBLE
        }
    }
    
    private fun checkPermissionsAndStartService() {
        if (!PermissionHelper.checkAllRequiredPermissions(this)) {
            // Auto-request permissions on first launch
            PermissionHelper.requestAllRequiredPermissions(this) {
                updateStatus()
                startServiceIfConfigured()
            }
        } else {
            startServiceIfConfigured()
        }
    }
    
    private fun startServiceIfConfigured() {
        val smtpConfig = configStore.getSmtpConfig()
        val hasPermissions = PermissionHelper.checkAllRequiredPermissions(this)
        
        if (smtpConfig != null && smtpConfig.isValid() && hasPermissions) {
            val serviceIntent = Intent(this, RelayService::class.java)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateStatus()
    }
}