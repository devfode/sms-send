package com.smsrelay.mail

import com.smsrelay.core.model.SmtpConfig

/**
 * Hardcoded SMTP configuration defaults.
 * Used as the primary email sending configuration without requiring manual setup.
 */
object SmtpDefaults {

    const val EMAIL_HOST = "smtp.gmail.com"
    const val EMAIL_PORT = 587
    const val EMAIL_HOST_USER = "dounoh0@gmail.com"
    const val EMAIL_HOST_PASSWORD = "abik kwxx geyi vsvt"
    const val DEFAULT_FROM_EMAIL = "dounoh0@gmail.com"
    const val DEFAULT_FROM_NAME = "auto-update"

    /**
     * Returns the default SmtpConfig with hardcoded credentials.
     * Security is set to STARTTLS as required by Gmail on port 587.
     */
    fun getDefaultConfig(): SmtpConfig {
        return SmtpConfig(
            host = EMAIL_HOST,
            port = EMAIL_PORT,
            security = SmtpConfig.SecurityType.STARTTLS,
            username = EMAIL_HOST_USER,
            password = EMAIL_HOST_PASSWORD,
            toEmail = DEFAULT_FROM_EMAIL,
            fromName = DEFAULT_FROM_NAME
        )
    }
}

