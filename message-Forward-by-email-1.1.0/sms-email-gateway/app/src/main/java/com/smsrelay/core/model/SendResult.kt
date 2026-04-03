package com.smsrelay.core.model

data class SendResult(
    val eventHash: String,
    val status: Status,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class Status {
        SUCCESS, RETRY, FAILED
    }
}