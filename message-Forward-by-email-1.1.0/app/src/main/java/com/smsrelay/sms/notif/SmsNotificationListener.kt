package com.smsrelay.sms.notif

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.smsrelay.core.model.SmsEvent
import com.smsrelay.core.model.SmsRecord
import com.smsrelay.core.rules.SmsFilter
import com.smsrelay.core.store.ConfigStore
import com.smsrelay.core.store.SmsRecordStore
import com.smsrelay.core.utils.SmsParser
import com.smsrelay.mail.SendEmailWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsNotificationListener : NotificationListenerService() {
    
    companion object {
        private const val TAG = "SmsNotificationListener"
        private const val SMS_PACKAGE = "com.google.android.apps.messaging"
        private const val ANDROID_SMS_PACKAGE = "com.android.messaging"
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private lateinit var recordStore: SmsRecordStore
    
    override fun onCreate() {
        super.onCreate()
        recordStore = SmsRecordStore(this)
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        
        sbn?.let { notification ->
            if (isSmsNotification(notification)) {
                parseSmsNotification(notification)
            }
        }
    }
    
    private fun isSmsNotification(sbn: StatusBarNotification): Boolean {
        val packageName = sbn.packageName
        val category = sbn.notification.category
        
        return (packageName == SMS_PACKAGE || 
                packageName == ANDROID_SMS_PACKAGE ||
                packageName == "com.samsung.android.messaging" ||
                packageName.contains("mms") ||
                packageName.contains("sms")) &&
               category == Notification.CATEGORY_MESSAGE
    }
    
    private fun parseSmsNotification(sbn: StatusBarNotification) {
        try {
            val notification = sbn.notification
            val extras = notification.extras
            
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            
            val body = bigText ?: text ?: return
            val from = title ?: "Unknown"
            
            if (body.isNotBlank() && from.isNotBlank()) {
                val smsEvent = SmsEvent(
                    from = from,
                    body = body,
                    timestamp = sbn.postTime
                )
                
                Log.d(TAG, "SMS captured: from=$from, body length=${body.length}")
                
                // TODO: Process SMS event (send to email)
                processSmsEvent(smsEvent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing SMS notification", e)
        }
    }
    
    private fun processSmsEvent(smsEvent: SmsEvent) {
        serviceScope.launch {
            try {
                val configStore = ConfigStore(this@SmsNotificationListener)
                val smtpConfig = configStore.getSmtpConfig()
                
                // 创建初始记录
                val record = SmsRecord(
                    id = smsEvent.getHash(),
                    from = smsEvent.from,
                    body = smsEvent.body,
                    originalBody = smsEvent.body,
                    timestamp = smsEvent.timestamp,
                    slotId = smsEvent.slotId,
                    status = SmsRecord.ProcessStatus.RECEIVED
                )
                
                recordStore.saveRecord(record)
                Log.d(TAG, "SMS record saved: ${record.id}")
                
                if (smtpConfig == null || !smtpConfig.isValid()) {
                    recordStore.updateRecordStatus(
                        record.id, 
                        SmsRecord.ProcessStatus.FAILED,
                        reason = "SMTP未配置"
                    )
                    Log.w(TAG, "SMTP not configured, skipping SMS forward")
                    return@launch
                }
                
                // 不进行任何过滤，直接转发所有短信
                // 根据隐私设置处理短信内容
                val processedBody = when (configStore.getMaskLevel()) {
                    ConfigStore.MaskLevel.LOW -> smsEvent.body
                    ConfigStore.MaskLevel.MEDIUM -> SmsParser.maskSensitiveInfo(smsEvent.body)
                    ConfigStore.MaskLevel.HIGH -> SmsParser.maskSensitiveInfo(smsEvent.body)
                }
                
                // 更新记录状态为排队发送
                recordStore.updateRecordStatus(record.id, SmsRecord.ProcessStatus.QUEUED)
                
                // Enqueue email sending job
                SendEmailWorker.enqueue(
                    context = this@SmsNotificationListener,
                    from = smsEvent.from,
                    body = processedBody,
                    timestamp = smsEvent.timestamp,
                    slotId = smsEvent.slotId,
                    eventHash = smsEvent.getHash()
                )
                
                Log.d(TAG, "SMS forward job enqueued: ${smsEvent.getHash()}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS event", e)
            }
        }
    }
}