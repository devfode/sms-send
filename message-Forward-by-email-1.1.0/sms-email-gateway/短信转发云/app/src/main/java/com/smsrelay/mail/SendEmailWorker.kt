package com.smsrelay.mail

import android.content.Context
import android.util.Log
import androidx.work.*
import com.smsrelay.core.model.SmtpConfig
import com.smsrelay.core.store.ConfigStore
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class SendEmailWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val TAG = "SendEmailWorker"
        
        const val KEY_FROM = "from"
        const val KEY_BODY = "body" 
        const val KEY_TIMESTAMP = "timestamp"
        const val KEY_SLOT_ID = "slot_id"
        const val KEY_EVENT_HASH = "event_hash"
        
        fun enqueue(
            context: Context,
            from: String,
            body: String,
            timestamp: Long,
            slotId: Int? = null,
            eventHash: String
        ) {
            val inputData = Data.Builder()
                .putString(KEY_FROM, from)
                .putString(KEY_BODY, body)
                .putLong(KEY_TIMESTAMP, timestamp)
                .putString(KEY_EVENT_HASH, eventHash)
                .apply {
                    slotId?.let { putInt(KEY_SLOT_ID, it) }
                }
                .build()
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val workRequest = OneTimeWorkRequestBuilder<SendEmailWorker>()
                .setInputData(inputData)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15, TimeUnit.SECONDS
                )
                .build()
            
            WorkManager.getInstance(context).enqueue(workRequest)
            Log.d(TAG, "Email job enqueued for hash: $eventHash")
        }
    }
    
    override suspend fun doWork(): Result {
        val from = inputData.getString(KEY_FROM) ?: return Result.failure()
        val body = inputData.getString(KEY_BODY) ?: return Result.failure()
        val timestamp = inputData.getLong(KEY_TIMESTAMP, 0L)
        val slotId = if (inputData.keyValueMap.containsKey(KEY_SLOT_ID)) {
            inputData.getInt(KEY_SLOT_ID, -1)
        } else null
        val eventHash = inputData.getString(KEY_EVENT_HASH) ?: return Result.failure()
        
        return try {
            val configStore = ConfigStore(applicationContext)
            val smtpConfig = configStore.getSmtpConfig()
            
            if (smtpConfig == null || !smtpConfig.isValid()) {
                Log.e(TAG, "Invalid SMTP configuration")
                return Result.failure()
            }
            
            val subject = formatSubject(from, timestamp)
            val emailBody = formatEmailBody(from, body, timestamp, slotId)
            
            val smtpClient = SmtpClient()
            val result = smtpClient.sendEmail(smtpConfig, subject, emailBody)
            
            if (result.isSuccess) {
                Log.d(TAG, "Email sent successfully for hash: $eventHash")
                Result.success()
            } else {
                Log.e(TAG, "Failed to send email for hash: $eventHash", result.exceptionOrNull())
                Result.retry()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in SendEmailWorker", e)
            Result.retry()
        }
    }
    
    private fun formatSubject(from: String, timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(timestamp))
        return "SMS | $from | $formattedDate"
    }
    
    private fun formatEmailBody(from: String, body: String, timestamp: Long, slotId: Int?): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(timestamp))
        val deviceInfo = android.os.Build.MODEL
        
        val slotInfo = slotId?.let { " (slot:$it)" } ?: ""
        
        return """
发件人: $from
时间: $formattedDate
设备: $deviceInfo$slotInfo

短信内容:
$body
        """.trimIndent()
    }
}