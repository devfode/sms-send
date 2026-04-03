package com.smsrelay.core.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SmsRecord(
    val id: String,
    val from: String,
    val body: String,
    val originalBody: String, // 原始短信内容（用于显示）
    val timestamp: Long,
    val slotId: Int? = null,
    val status: ProcessStatus,
    val reason: String? = null, // 过滤或失败原因
    val emailSent: Boolean = false,
    val emailError: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    enum class ProcessStatus {
        RECEIVED,    // 收到短信
        QUEUED,      // 排队发送
        SENT,        // 邮件发送成功
        FAILED       // 邮件发送失败
    }
    
    fun getFormattedTime(): String {
        val formatter = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }
    
    fun getStatusText(): String {
        return when (status) {
            ProcessStatus.RECEIVED -> "已接收"
            ProcessStatus.QUEUED -> "发送中"
            ProcessStatus.SENT -> "已转发"
            ProcessStatus.FAILED -> "发送失败"
        }
    }
    
    fun getStatusColor(): Int {
        return when (status) {
            ProcessStatus.RECEIVED -> android.graphics.Color.GRAY
            ProcessStatus.QUEUED -> android.graphics.Color.BLUE
            ProcessStatus.SENT -> android.graphics.Color.GREEN
            ProcessStatus.FAILED -> android.graphics.Color.RED
        }
    }
}