package com.smsrelay.core.rules

import com.smsrelay.core.model.SmsEvent

object SmsFilter {
    
    // 默认白名单：银行、运营商、常见服务号段
    private val defaultWhitelist = setOf(
        // 银行
        "95588", "95533", "95555", "95599", "95595", "95568", "95566", "95580",
        "95558", "95577", "95559", "95561", "95562", "95563", "95564", "95565",
        
        // 运营商
        "10086", "10010", "10000", "10001",
        
        // 常见服务
        "106", "95", "10690", "10691", "10692", "10693", "10694", "10695",
        "10696", "10697", "10698", "10699"
    )
    
    // 默认黑名单：垃圾短信常见前缀
    private val defaultBlacklist = setOf(
        "退订回", "回复TD", "回复T", "广告", "推广", "营销"
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
        
        // 首先检查黑名单
        if (blacklistEnabled) {
            blacklist.forEach { blocked ->
                if (event.from.contains(blocked, ignoreCase = true) ||
                    event.body.contains(blocked, ignoreCase = true)) {
                    return false
                }
            }
        }
        
        // 然后检查白名单
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
            "验证码", "校验码", "动态码", "安全码", "确认码",
            "verification", "code", "otp", "pin"
        )
        
        return otpKeywords.any { keyword ->
            body.contains(keyword, ignoreCase = true)
        } && extractOtpFromMessage(body) != null
    }
    
    fun formatOtpOnlyMessage(body: String): String? {
        val otp = extractOtpFromMessage(body) ?: return null
        return "验证码: $otp"
    }
}