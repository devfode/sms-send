package com.smsrelay.core.utils

object SmsParser {
    
    private val OTP_REGEX = Regex("(?<!\\d)\\d{4,8}(?!\\d)")
    private val PHONE_REGEX = Regex("1[3-9]\\d{9}")
    private val CARD_REGEX = Regex("\\d{4}\\*{4,}\\d{4}")
    
    fun extractOtp(text: String): String? {
        return OTP_REGEX.find(text)?.value
    }
    
    fun extractPhoneNumbers(text: String): List<String> {
        return PHONE_REGEX.findAll(text).map { it.value }.toList()
    }
    
    fun extractCardNumbers(text: String): List<String> {
        return CARD_REGEX.findAll(text).map { it.value }.toList()
    }
    
    fun maskSensitiveInfo(text: String): String {
        var masked = text
        
        // Mask phone numbers (keep first 3 and last 4 digits)
        masked = masked.replace(PHONE_REGEX) { match ->
            val phone = match.value
            "${phone.take(3)}****${phone.takeLast(4)}"
        }
        
        // Mask long numbers (likely card numbers, ID cards etc)
        masked = masked.replace(Regex("\\d{8,}")) { match ->
            val number = match.value
            when (number.length) {
                in 8..10 -> "${number.take(2)}****${number.takeLast(2)}"
                in 11..16 -> "${number.take(4)}****${number.takeLast(4)}"
                else -> "${number.take(6)}****${number.takeLast(4)}"
            }
        }
        
        // Mask email addresses
        masked = masked.replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) { match ->
            val email = match.value
            val parts = email.split("@")
            val localPart = parts[0]
            val domain = parts[1]
            
            val maskedLocal = if (localPart.length <= 2) {
                "*".repeat(localPart.length)
            } else {
                "${localPart.first()}${"*".repeat(localPart.length - 2)}${localPart.last()}"
            }
            
            "$maskedLocal@$domain"
        }
        
        return masked
    }
}