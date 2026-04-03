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
import com.smsrelay.mail.SmtpDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Settings screen for configuring SMTP parameters.
 * Fields are pre-filled with hardcoded defaults from SmtpDefaults.
 */
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
        supportActionBar?.title = "Settings"

        // Security type spinner
        val securityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listOf("None", "SSL", "STARTTLS")
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
        // Load saved config, or use hardcoded defaults
        val config = configStore.getSmtpConfig() ?: SmtpDefaults.getDefaultConfig()

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

        binding.switchOnlyOtp.isChecked = configStore.isOnlyOtpMode()
    }

    private fun saveConfig() {
        val host = binding.etHost.text.toString().trim()
        val port = binding.etPort.text.toString().toIntOrNull() ?: 0
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val toEmail = binding.etToEmail.text.toString().trim()
        val fromName = binding.etFromName.text.toString().trim().ifEmpty { "auto-update" }

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
            Toast.makeText(this, "Please fill in all SMTP fields", Toast.LENGTH_SHORT).show()
            return
        }

        configStore.saveSmtpConfig(config)
        configStore.setOnlyOtpMode(binding.switchOnlyOtp.isChecked)

        Toast.makeText(this, "Configuration saved", Toast.LENGTH_SHORT).show()
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
        binding.btnTestConnection.text = "Testing..."

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                SmtpClient().testConnection(config)
            }

            binding.btnTestConnection.isEnabled = true
            binding.btnTestConnection.text = "Test Connection"

            if (result.isSuccess) {
                Toast.makeText(this@SettingsActivity, "Connection successful", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@SettingsActivity, "Connection failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setGmailPreset() {
        binding.etHost.setText(SmtpDefaults.EMAIL_HOST)
        binding.etPort.setText(SmtpDefaults.EMAIL_PORT.toString())
        binding.spinnerSecurity.setSelection(2) // STARTTLS
        binding.etUsername.setText(SmtpDefaults.EMAIL_HOST_USER)
        binding.etPassword.setText(SmtpDefaults.EMAIL_HOST_PASSWORD)
        binding.etToEmail.setText(SmtpDefaults.DEFAULT_FROM_EMAIL)
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