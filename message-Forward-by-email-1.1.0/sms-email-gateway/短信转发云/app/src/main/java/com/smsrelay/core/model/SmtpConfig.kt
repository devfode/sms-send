package com.smsrelay.core.model

data class SmtpConfig(
    val host: String,
    val port: Int,
    val security: SecurityType,
    val username: String,
    val password: String,
    val toEmail: String,
    val fromName: String = "SMS Relay"
) {
    enum class SecurityType {
        NONE, SSL, STARTTLS
    }
    
    fun isValid(): Boolean {
        return host.isNotBlank() && 
               port > 0 && 
               username.isNotBlank() && 
               password.isNotBlank() && 
               toEmail.isNotBlank()
    }
}