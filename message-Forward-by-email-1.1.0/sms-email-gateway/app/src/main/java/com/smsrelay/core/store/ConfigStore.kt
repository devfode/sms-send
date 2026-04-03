package com.smsrelay.core.store

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.smsrelay.core.model.SmtpConfig

class ConfigStore(private val context: Context) {
    
    companion object {
        private const val PREFS_FILE = "encrypted_prefs_config"
        
        private const val KEY_SMTP_HOST = "smtp_host"
        private const val KEY_SMTP_PORT = "smtp_port"
        private const val KEY_SMTP_SECURITY = "smtp_security"
        private const val KEY_SMTP_USERNAME = "smtp_username"
        private const val KEY_SMTP_PASSWORD = "smtp_password"
        private const val KEY_TO_EMAIL = "to_email"
        private const val KEY_FROM_NAME = "from_name"
        
        private const val KEY_ONLY_OTP = "only_otp"
        private const val KEY_MASK_LEVEL = "mask_level"
    }
    
    private val sharedPreferences: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    fun saveSmtpConfig(config: SmtpConfig) {
        sharedPreferences.edit()
            .putString(KEY_SMTP_HOST, config.host)
            .putInt(KEY_SMTP_PORT, config.port)
            .putString(KEY_SMTP_SECURITY, config.security.name)
            .putString(KEY_SMTP_USERNAME, config.username)
            .putString(KEY_SMTP_PASSWORD, config.password)
            .putString(KEY_TO_EMAIL, config.toEmail)
            .putString(KEY_FROM_NAME, config.fromName)
            .apply()
    }
    
    fun getSmtpConfig(): SmtpConfig? {
        return try {
            val host = sharedPreferences.getString(KEY_SMTP_HOST, null) ?: return null
            val port = sharedPreferences.getInt(KEY_SMTP_PORT, 0)
            val securityName = sharedPreferences.getString(KEY_SMTP_SECURITY, null) ?: return null
            val username = sharedPreferences.getString(KEY_SMTP_USERNAME, null) ?: return null
            val password = sharedPreferences.getString(KEY_SMTP_PASSWORD, null) ?: return null
            val toEmail = sharedPreferences.getString(KEY_TO_EMAIL, null) ?: return null
            val fromName = sharedPreferences.getString(KEY_FROM_NAME, "SMS Relay") ?: "SMS Relay"
            
            val security = SmtpConfig.SecurityType.valueOf(securityName)
            
            SmtpConfig(
                host = host,
                port = port,
                security = security,
                username = username,
                password = password,
                toEmail = toEmail,
                fromName = fromName
            )
        } catch (e: Exception) {
            null
        }
    }
    
    fun setOnlyOtpMode(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_ONLY_OTP, enabled)
            .apply()
    }
    
    fun isOnlyOtpMode(): Boolean {
        return sharedPreferences.getBoolean(KEY_ONLY_OTP, false)
    }
    
    fun setMaskLevel(level: MaskLevel) {
        sharedPreferences.edit()
            .putString(KEY_MASK_LEVEL, level.name)
            .apply()
    }
    
    fun getMaskLevel(): MaskLevel {
        val levelName = sharedPreferences.getString(KEY_MASK_LEVEL, MaskLevel.MEDIUM.name)
        return try {
            MaskLevel.valueOf(levelName!!)
        } catch (e: Exception) {
            MaskLevel.MEDIUM
        }
    }
    
    fun clear() {
        sharedPreferences.edit().clear().apply()
    }
    
    enum class MaskLevel {
        LOW,    // Only mask card numbers and IDs
        MEDIUM, // Mask phones, cards, IDs
        HIGH    // Mask everything including partial text
    }
}