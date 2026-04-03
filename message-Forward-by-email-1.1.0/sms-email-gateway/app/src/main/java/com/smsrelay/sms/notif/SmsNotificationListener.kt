package com.smsrelay.sms.notif

import android.app.Notification
import android.content.SharedPreferences
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.smsrelay.core.model.SmsEvent
import com.smsrelay.core.rules.SmsFilter
import com.smsrelay.core.store.ConfigStore
import com.smsrelay.core.utils.SmsParser
import com.smsrelay.mail.SendEmailWorker
import com.smsrelay.mail.SmtpDefaults
import java.text.SimpleDateFormat
import java.util.*

/**
 * NotificationListenerService that captures SMS notifications from messaging apps.
 * This is a secondary interception method alongside SmsReceiver.
 */
class SmsNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "SmsNotificationListener"
        private const val SMS_PACKAGE = "com.google.android.apps.messaging"
        private const val ANDROID_SMS_PACKAGE = "com.android.messaging"
        private const val PREF_NAME = "sms_debug_logs"
        private const val KEY_TOTAL_NOTIFICATIONS = "total_notifications"
        private const val KEY_SMS_NOTIFICATIONS = "sms_notifications"
        private const val KEY_LAST_SMS_TIME = "last_sms_time"
        private const val KEY_LAST_SMS_FROM = "last_sms_from"
        private const val KEY_LAST_SMS_BODY = "last_sms_body"
        private const val KEY_DEBUG_LOG = "debug_log"
    }

    private lateinit var debugPrefs: SharedPreferences
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onCreate() {
        super.onCreate()
        debugPrefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        addDebugLog("SmsNotificationListener service started")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        // Track all notifications
        incrementTotalNotifications()

        sbn?.let { notification ->
            val packageName = notification.packageName
            val category = notification.notification.category

            addDebugLog("Notification received: package=$packageName, category=$category")

            if (isSmsNotification(notification)) {
                addDebugLog("Identified as SMS notification, parsing...")
                incrementSmsNotifications()
                parseSmsNotification(notification)
            } else {
                addDebugLog("Not an SMS notification, skipping")
            }
        } ?: addDebugLog("Received null notification object")
    }

    private fun isSmsNotification(sbn: StatusBarNotification): Boolean {
        val packageName = sbn.packageName
        val category = sbn.notification.category

        val isSms = (packageName == SMS_PACKAGE ||
                packageName == ANDROID_SMS_PACKAGE ||
                packageName == "com.samsung.android.messaging" ||
                packageName == "com.android.mms.service" ||
                packageName.contains("mms") ||
                packageName.contains("sms") ||
                packageName.contains("messaging")) &&
                category == Notification.CATEGORY_MESSAGE

        addDebugLog("SMS detection: package=$packageName, category=$category, isSms=$isSms")

        return isSms
    }

    private fun parseSmsNotification(sbn: StatusBarNotification) {
        try {
            val notification = sbn.notification
            val extras = notification.extras

            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

            addDebugLog("Notification content: title=$title, text=$text, bigText=$bigText")

            val body = bigText ?: text ?: ""
            val from = title ?: "Unknown"

            // Save latest SMS info
            saveLastSmsInfo(from, body)

            if (body.isNotBlank() && from.isNotBlank()) {
                val smsEvent = SmsEvent(
                    from = from,
                    body = body,
                    timestamp = sbn.postTime
                )

                addDebugLog("SMS parsed successfully: from=$from, bodyLength=${body.length}")
                Log.d(TAG, "SMS captured: from=$from, body length=${body.length}")

                processSmsEvent(smsEvent)
            } else {
                addDebugLog("SMS content is empty, skipping")
            }
        } catch (e: Exception) {
            val error = "Error parsing SMS notification: ${e.message}"
            addDebugLog("Parse error: $error")
            Log.e(TAG, error, e)
        }
    }

    private fun processSmsEvent(smsEvent: SmsEvent) {
        try {
            addDebugLog("Processing SMS event...")

            val configStore = ConfigStore(this)
            val smtpConfig = configStore.getSmtpConfig() ?: SmtpDefaults.getDefaultConfig()

            if (!smtpConfig.isValid()) {
                addDebugLog("SMTP config is invalid, skipping forwarding")
                Log.w(TAG, "SMTP not configured, skipping SMS forward")
                return
            }

            addDebugLog("SMTP config is valid, continuing...")

            // Apply filtering rules
            if (!SmsFilter.shouldForward(smsEvent)) {
                addDebugLog("SMS filtered out by rules: ${smsEvent.from}")
                Log.d(TAG, "SMS filtered out by rules: ${smsEvent.from}")
                return
            }

            addDebugLog("SMS passed filter rules")

            // Apply privacy filters
            val processedBody = if (configStore.isOnlyOtpMode()) {
                if (SmsFilter.isOtpMessage(smsEvent.body)) {
                    addDebugLog("OTP message detected, forwarding OTP only")
                    SmsFilter.formatOtpOnlyMessage(smsEvent.body) ?: return
                } else {
                    addDebugLog("OTP-only mode: no OTP found, skipping")
                    return // Skip if no OTP found in OTP-only mode
                }
            } else {
                when (configStore.getMaskLevel()) {
                    ConfigStore.MaskLevel.LOW -> {
                        addDebugLog("Privacy level: LOW, no masking applied")
                        smsEvent.body
                    }
                    ConfigStore.MaskLevel.MEDIUM -> {
                        addDebugLog("Privacy level: MEDIUM, applying data masking")
                        SmsParser.maskSensitiveInfo(smsEvent.body)
                    }
                    ConfigStore.MaskLevel.HIGH -> {
                        addDebugLog("Privacy level: HIGH, applying data masking")
                        SmsParser.maskSensitiveInfo(smsEvent.body)
                    }
                }
            }

            // Enqueue email sending job
            SendEmailWorker.enqueue(
                context = this,
                from = smsEvent.from,
                body = processedBody,
                timestamp = smsEvent.timestamp,
                slotId = smsEvent.slotId,
                eventHash = smsEvent.getHash()
            )

            addDebugLog("Email job enqueued: ${smsEvent.getHash()}")
            Log.d(TAG, "SMS forward job enqueued: ${smsEvent.getHash()}")

        } catch (e: Exception) {
            val error = "Error processing SMS event: ${e.message}"
            addDebugLog("Processing error: $error")
            Log.e(TAG, error, e)
        }
    }

    // Debug log recording methods
    private fun addDebugLog(message: String) {
        val timestamp = dateFormat.format(Date())
        val logEntry = "[$timestamp] $message"

        Log.d(TAG, message)

        // Save to SharedPreferences
        val currentLog = debugPrefs.getString(KEY_DEBUG_LOG, "") ?: ""
        val newLog = "$logEntry\n$currentLog".take(5000) // Limit log size

        debugPrefs.edit()
            .putString(KEY_DEBUG_LOG, newLog)
            .apply()
    }

    private fun incrementTotalNotifications() {
        val current = debugPrefs.getInt(KEY_TOTAL_NOTIFICATIONS, 0)
        debugPrefs.edit()
            .putInt(KEY_TOTAL_NOTIFICATIONS, current + 1)
            .apply()
    }

    private fun incrementSmsNotifications() {
        val current = debugPrefs.getInt(KEY_SMS_NOTIFICATIONS, 0)
        debugPrefs.edit()
            .putInt(KEY_SMS_NOTIFICATIONS, current + 1)
            .apply()
    }

    private fun saveLastSmsInfo(from: String, body: String) {
        val timestamp = dateFormat.format(Date())
        debugPrefs.edit()
            .putString(KEY_LAST_SMS_TIME, timestamp)
            .putString(KEY_LAST_SMS_FROM, from)
            .putString(KEY_LAST_SMS_BODY, body.take(200)) // Limit length
            .apply()
    }
}