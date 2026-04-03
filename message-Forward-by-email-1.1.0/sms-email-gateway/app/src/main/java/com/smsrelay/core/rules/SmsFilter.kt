package com.smsrelay.core.rules

import com.smsrelay.core.model.SmsEvent

/**
 * SMS filtering rules for determining which messages should be forwarded.
 * Includes whitelist/blacklist logic and OTP detection.
 */
object SmsFilter {

    // Default whitelist: banks, carriers, common service short codes
    private val defaultWhitelist = setOf(
        // Banks
        "95588", "95533", "95555", "95599", "95595", "95568", "95566", "95580",
        "95558", "95577", "95559", "95561", "95562", "95563", "95564", "95565",

        // Carriers
        "10086", "10010", "10000", "10001",

        // Common service prefixes
        "106", "95", "10690", "10691", "10692", "10693", "10694", "10695",
        "10696", "10697", "10698", "10699"
    )

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