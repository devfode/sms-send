package com.smsrelay.mail

import android.util.Log
import com.smsrelay.core.model.SmtpConfig
import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.*

class SmtpClient {
    
    companion object {
        private const val TAG = "SmtpClient"
    }
    
    fun sendEmail(
        config: SmtpConfig,
        subject: String,
        body: String
    ): Result<Unit> {
        return try {
            val props = Properties().apply {
                put("mail.smtp.host", config.host)
                put("mail.smtp.port", config.port.toString())
                put("mail.smtp.auth", "true")
                
                when (config.security) {
                    SmtpConfig.SecurityType.SSL -> {
                        put("mail.smtp.ssl.enable", "true")
                        put("mail.smtp.ssl.protocols", "TLSv1.2")
                    }
                    SmtpConfig.SecurityType.STARTTLS -> {
                        put("mail.smtp.starttls.enable", "true")
                        put("mail.smtp.starttls.required", "true")
                        put("mail.smtp.ssl.protocols", "TLSv1.2")
                    }
                    SmtpConfig.SecurityType.NONE -> {
                        // No encryption
                    }
                }
                
                // Connection timeout
                put("mail.smtp.connectiontimeout", "10000")
                put("mail.smtp.timeout", "10000")
                put("mail.smtp.writetimeout", "10000")
            }
            
            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(config.username, config.password)
                }
            })
            
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(config.username, config.fromName))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(config.toEmail))
                setSubject(subject, "UTF-8")
                setText(body, "UTF-8")
                sentDate = Date()
            }
            
            Transport.send(message)
            
            Log.d(TAG, "Email sent successfully to ${config.toEmail}")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send email", e)
            Result.failure(e)
        }
    }
    
    fun testConnection(config: SmtpConfig): Result<String> {
        return try {
            val props = Properties().apply {
                put("mail.smtp.host", config.host)
                put("mail.smtp.port", config.port.toString())
                put("mail.smtp.auth", "true")
                put("mail.smtp.connectiontimeout", "5000")
                
                when (config.security) {
                    SmtpConfig.SecurityType.SSL -> {
                        put("mail.smtp.ssl.enable", "true")
                    }
                    SmtpConfig.SecurityType.STARTTLS -> {
                        put("mail.smtp.starttls.enable", "true")
                        put("mail.smtp.starttls.required", "true")
                    }
                    SmtpConfig.SecurityType.NONE -> {
                        // No encryption
                    }
                }
            }
            
            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(config.username, config.password)
                }
            })
            
            val transport = session.getTransport("smtp")
            transport.connect()
            transport.close()
            
            Result.success("Connection successful")
            
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed", e)
            Result.failure(e)
        }
    }
}