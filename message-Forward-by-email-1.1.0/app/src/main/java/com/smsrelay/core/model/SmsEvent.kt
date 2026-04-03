package com.smsrelay.core.model

data class SmsEvent(
    val from: String,
    val body: String,
    val timestamp: Long,
    val slotId: Int? = null
) {
    fun getHash(): String {
        return "${from}_${timestamp}_${body.hashCode()}".hashCode().toString()
    }
}