package com.smsrelay.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.smsrelay.R
import com.smsrelay.core.model.SmtpConfig
import com.smsrelay.core.store.ConfigStore
import com.smsrelay.databinding.ActivitySettingsBinding
import com.smsrelay.mail.SmtpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var configStore: ConfigStore
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        configStore = ConfigStore(this)
        
        setupUI()
        loadConfig()
    }
    
    private fun setupUI() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "设置"
        
        // Security type spinner
        val securityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listOf("无加密", "SSL", "STARTTLS")
        )
        securityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSecurity.adapter = securityAdapter
        
        // Save button
        binding.btnSave.setOnClickListener {
            saveConfig()
        }
        
        // Test connection button
        binding.btnTestConnection.setOnClickListener {
            testConnection()
        }
        
        // Set common SMTP presets
        binding.btnGmailPreset.setOnClickListener {
            setGmailPreset()
        }
        
        binding.btnQqPreset.setOnClickListener {
            setQqPreset()
        }
        
        binding.btnMailpitPreset.setOnClickListener {
            setMailpitPreset()
        }
    }
    
    private fun loadConfig() {
        configStore.getSmtpConfig()?.let { config ->
            binding.etHost.setText(config.host)
            binding.etPort.setText(config.port.toString())
            binding.etUsername.setText(config.username)
            binding.etPassword.setText(config.password)
            binding.etToEmail.setText(config.toEmail)
            binding.etFromName.setText(config.fromName)
            
            val securityIndex = when (config.security) {
                SmtpConfig.SecurityType.NONE -> 0
                SmtpConfig.SecurityType.SSL -> 1
                SmtpConfig.SecurityType.STARTTLS -> 2
            }
            binding.spinnerSecurity.setSelection(securityIndex)
        }
        
        binding.switchOnlyOtp.isChecked = configStore.isOnlyOtpMode()
    }
    
    private fun saveConfig() {
        val host = binding.etHost.text.toString().trim()
        val port = binding.etPort.text.toString().toIntOrNull() ?: 0
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val toEmail = binding.etToEmail.text.toString().trim()
        val fromName = binding.etFromName.text.toString().trim().ifEmpty { "SMS Relay" }
        
        val security = when (binding.spinnerSecurity.selectedItemPosition) {
            0 -> SmtpConfig.SecurityType.NONE
            1 -> SmtpConfig.SecurityType.SSL
            2 -> SmtpConfig.SecurityType.STARTTLS
            else -> SmtpConfig.SecurityType.STARTTLS
        }
        
        val config = SmtpConfig(
            host = host,
            port = port,
            security = security,
            username = username,
            password = password,
            toEmail = toEmail,
            fromName = fromName
        )
        
        if (!config.isValid()) {
            Toast.makeText(this, "请填写完整的SMTP配置", Toast.LENGTH_SHORT).show()
            return
        }
        
        configStore.saveSmtpConfig(config)
        configStore.setOnlyOtpMode(binding.switchOnlyOtp.isChecked)
        
        Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show()
    }
    
    private fun testConnection() {
        val host = binding.etHost.text.toString().trim()
        val port = binding.etPort.text.toString().toIntOrNull() ?: 0
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString()
        
        val security = when (binding.spinnerSecurity.selectedItemPosition) {
            0 -> SmtpConfig.SecurityType.NONE
            1 -> SmtpConfig.SecurityType.SSL
            2 -> SmtpConfig.SecurityType.STARTTLS
            else -> SmtpConfig.SecurityType.STARTTLS
        }
        
        val config = SmtpConfig(
            host = host,
            port = port,
            security = security,
            username = username,
            password = password,
            toEmail = "test@example.com",
            fromName = "Test"
        )
        
        binding.btnTestConnection.isEnabled = false
        binding.btnTestConnection.text = "测试中..."
        
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                SmtpClient().testConnection(config)
            }
            
            binding.btnTestConnection.isEnabled = true
            binding.btnTestConnection.text = "测试连接"
            
            if (result.isSuccess) {
                Toast.makeText(this@SettingsActivity, "连接成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@SettingsActivity, "连接失败: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun setGmailPreset() {
        binding.etHost.setText("smtp.gmail.com")
        binding.etPort.setText("587")
        binding.spinnerSecurity.setSelection(2) // STARTTLS
    }
    
    private fun setQqPreset() {
        binding.etHost.setText("smtp.qq.com")
        binding.etPort.setText("465")
        binding.spinnerSecurity.setSelection(1) // SSL
    }
    
    private fun setMailpitPreset() {
        binding.etHost.setText("10.0.2.2")
        binding.etPort.setText("1025")
        binding.spinnerSecurity.setSelection(0) // None
        binding.etUsername.setText("test")
        binding.etPassword.setText("test")
        binding.etToEmail.setText("test@example.com")
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}