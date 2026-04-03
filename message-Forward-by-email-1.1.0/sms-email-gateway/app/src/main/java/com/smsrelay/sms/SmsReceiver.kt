package com.smsrelay.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.smsrelay.core.model.SmsEvent
import com.smsrelay.mail.SendEmailWorker

/**
 * BroadcastReceiver that intercepts incoming SMS messages in real-time.
 * Uses Telephony.Sms.Intents.getMessagesFromIntent() for robust PDU extraction,
 * compatible with Android 14+.
 *
 * Registered in AndroidManifest with priority 999 on SMS_RECEIVED action.
 */
class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        Log.d(TAG, "SMS_RECEIVED broadcast intercepted")

        try {
            // Use the robust Telephony API for PDU extraction (Android 14 compatible)
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            if (messages.isNullOrEmpty()) {
                Log.w(TAG, "No messages extracted from intent")
                return
            }

            // Group messages by sender (multi-part SMS handling)
            val grouped = messages.groupBy { it.originatingAddress ?: "Unknown" }

            for ((sender, parts) in grouped) {
                val fullBody = parts.joinToString("") { it.messageBody ?: "" }
                val timestamp = parts.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()

                if (fullBody.isBlank()) {
                    Log.w(TAG, "Empty message body from sender: $sender, skipping")
                    continue
                }

                Log.d(TAG, "SMS received from: $sender, body length: ${fullBody.length}")

                val smsEvent = SmsEvent(
                    from = sender,
                    body = fullBody,
                    timestamp = timestamp
                )

                // Immediately enqueue email forwarding
                SendEmailWorker.enqueue(
                    context = context,
                    from = smsEvent.from,
                    body = smsEvent.body,
                    timestamp = smsEvent.timestamp,
                    slotId = smsEvent.slotId,
                    eventHash = smsEvent.getHash()
                )

                Log.d(TAG, "Email forwarding enqueued for SMS from: $sender, hash: ${smsEvent.getHash()}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing incoming SMS", e)
        }
    }
}
