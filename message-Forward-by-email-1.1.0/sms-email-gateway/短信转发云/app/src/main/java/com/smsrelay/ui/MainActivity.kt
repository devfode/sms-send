package com.smsrelay.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.smsrelay.core.store.ConfigStore
import com.smsrelay.databinding.ActivityMainBinding
import com.smsrelay.service.RelayService

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var configStore: ConfigStore
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        configStore = ConfigStore(this)
        setupUI()
        startServiceIfConfigured()
    }
    
    private fun setupUI() {
        updateStatus()
        
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
    
    private fun updateStatus() {
        val smtpConfig = configStore.getSmtpConfig()
        
        binding.textStatus.text = if (smtpConfig != null && smtpConfig.isValid()) {
            "短信转发服务 - 运行中"
        } else {
            "短信转发服务 - 需要配置SMTP"
        }
    }
    
    private fun startServiceIfConfigured() {
        val smtpConfig = configStore.getSmtpConfig()
        
        if (smtpConfig != null && smtpConfig.isValid()) {
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