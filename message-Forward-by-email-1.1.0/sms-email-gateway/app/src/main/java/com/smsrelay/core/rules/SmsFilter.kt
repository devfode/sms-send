package com.smsrelay.core.rules

import com.smsrelay.core.model.SmsEvent

/**
 * SMS filtering rules for determining which messages should be forwarded.
 * Includes whitelist/blacklist logic and OTP detection.
 */
object SmsFilter {

    // No default whitelist: forward all messages by default
    private val defaultWhitelist = emptySet<String>()


    // Default blacklist: common spam keywords
    private val defaultBlacklist = setOf(
        "unsubscribe", "reply STOP", "reply TD", "advertisement", "promo", "marketing"
    )

    fun shouldForward(
        event: SmsEvent,
        whitelistEnabled: Boolean = true,
        customWhitelist: Set<String> = emptySet(),
        blacklistEnabled: Boolean = true,
        customBlacklist: Set<String> = emptySet()
    ): Boolean {

        val whitelist = if (customWhitelist.isNotEmpty()) customWhitelist else defaultWhitelist
        val blacklist = if (customBlacklist.isNotEmpty()) customBlacklist else defaultBlacklist

        // Check blacklist first
        if (blacklistEnabled) {
            blacklist.forEach { blocked ->
                if (event.from.contains(blocked, ignoreCase = true) ||
                    event.body.contains(blocked, ignoreCase = true)) {
                    return false
                }
            }
        }

        // Then check whitelist
        if (whitelistEnabled) {
            val isInWhitelist = whitelist.any { allowed ->
                event.from.startsWith(allowed) || event.from.contains(allowed)
            }
            if (!isInWhitelist) {
                return false
            }
        }

        return true
    }

    fun extractOtpFromMessage(body: String, minLength: Int = 4, maxLength: Int = 8): String? {
        val otpRegex = Regex("(?<!\\d)\\d{$minLength,$maxLength}(?!\\d)")
        return otpRegex.find(body)?.value
    }

    fun isOtpMessage(body: String): Boolean {
        val otpKeywords = listOf(
            "verification", "code", "otp", "pin",
            "verify", "confirm", "security code", "auth"
        )

        return otpKeywords.any { keyword ->
            body.contains(keyword, ignoreCase = true)
        } && extractOtpFromMessage(body) != null
    }

    fun formatOtpOnlyMessage(body: String): String? {
        val otp = extractOtpFromMessage(body) ?: return null
        return "Verification Code: $otp"
    }
}