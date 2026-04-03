package com.smsrelay.test

import com.smsrelay.core.model.SmsEvent
import com.smsrelay.core.rules.SmsFilter

/**
 * 规则引擎过滤功能测试
 * 验证白名单/黑名单过滤、OTP识别、消息分类等功能
 */
object SmsFilterTest {
    
    fun runTests(): TestResult {
        val results = mutableListOf<TestCase>()
        
        // 测试1: 白名单过滤
        results.add(testWhitelistFiltering())
        
        // 测试2: 黑名单过滤
        results.add(testBlacklistFiltering())
        
        // 测试3: OTP消息识别
        results.add(testOtpMessageDetection())
        
        // 测试4: OTP格式化
        results.add(testOtpFormatting())
        
        // 测试5: 银行短信白名单
        results.add(testBankWhitelist())
        
        // 测试6: 垃圾短信黑名单
        results.add(testSpamBlacklist())
        
        // 测试7: 自定义规则
        results.add(testCustomRules())
        
        val passed = results.count { it.passed }
        val total = results.size
        
        return TestResult("规则引擎测试", passed, total, results)
    }
    
    private fun testWhitelistFiltering(): TestCase {
        val bankSms = SmsEvent("95588", "【工商银行】您的账户余额为1000元", System.currentTimeMillis())
        val carrierSms = SmsEvent("10086", "【中国移动】话费余额提醒", System.currentTimeMillis())
        val unknownSms = SmsEvent("12345", "普通消息", System.currentTimeMillis())
        
        val bankAllowed = SmsFilter.shouldForward(bankSms, whitelistEnabled = true)
        val carrierAllowed = SmsFilter.shouldForward(carrierSms, whitelistEnabled = true)
        val unknownBlocked = !SmsFilter.shouldForward(unknownSms, whitelistEnabled = true)
        
        val result = bankAllowed && carrierAllowed && unknownBlocked
        return TestCase("白名单过滤", result, "银行和运营商短信应通过，未知号码应被拒绝")
    }
    
    private fun testBlacklistFiltering(): TestCase {
        val normalSms = SmsEvent("95588", "【工商银行】余额提醒", System.currentTimeMillis())
        val adSms1 = SmsEvent("106123", "广告推广信息，回复TD退订", System.currentTimeMillis())
        val adSms2 = SmsEvent("95555", "营销活动通知，退订回T", System.currentTimeMillis())
        
        val normalAllowed = SmsFilter.shouldForward(normalSms, blacklistEnabled = true)
        val ad1Blocked = !SmsFilter.shouldForward(adSms1, blacklistEnabled = true)
        val ad2Blocked = !SmsFilter.shouldForward(adSms2, blacklistEnabled = true)
        
        val result = normalAllowed && ad1Blocked && ad2Blocked
        return TestCase("黑名单过滤", result, "正常短信应通过，广告短信应被拒绝")
    }
    
    private fun testOtpMessageDetection(): TestCase {
        val otpMessages = listOf(
            "【招商银行】您的验证码为 123456，请在5分钟内使用",
            "您的动态密码是 8888，请妥善保管",
            "Verification code: 654321",
            "登录OTP: 9999"
        )
        
        val normalMessages = listOf(
            "【银行通知】您的账户余额为1000元",
            "会议将在下午3点开始",
            "天气预报：明天晴天"
        )
        
        val otpDetected = otpMessages.all { SmsFilter.isOtpMessage(it) }
        val normalNotDetected = normalMessages.all { !SmsFilter.isOtpMessage(it) }
        
        val result = otpDetected && normalNotDetected
        return TestCase("OTP消息识别", result, "OTP消息应被正确识别，普通消息应被排除")
    }
    
    private fun testOtpFormatting(): TestCase {
        val testCases = mapOf(
            "【招商银行】您的验证码为 123456，请在5分钟内使用" to "验证码: 123456",
            "动态密码8888，有效期3分钟" to "验证码: 8888",
            "普通消息，没有验证码" to null
        )
        
        val allCorrect = testCases.all { (message, expected) ->
            val formatted = SmsFilter.formatOtpOnlyMessage(message)
            formatted == expected
        }
        
        return TestCase("OTP格式化", allCorrect, "OTP消息应被正确格式化")
    }
    
    private fun testBankWhitelist(): TestCase {
        val bankNumbers = listOf(
            "95588", "95533", "95555", "95599", "95595", "95568", "95566", "95580"
        )
        
        val allAllowed = bankNumbers.all { bankNumber ->
            val sms = SmsEvent(bankNumber, "银行通知消息", System.currentTimeMillis())
            SmsFilter.shouldForward(sms, whitelistEnabled = true)
        }
        
        return TestCase("银行白名单", allAllowed, "所有银行号码应在白名单中")
    }
    
    private fun testSpamBlacklist(): TestCase {
        val spamMessages = listOf(
            SmsEvent("106123", "广告推广消息", System.currentTimeMillis()),
            SmsEvent("95555", "营销活动，回复TD退订", System.currentTimeMillis()),
            SmsEvent("10690", "推广信息，退订回T", System.currentTimeMillis())
        )
        
        val allBlocked = spamMessages.all { sms ->
            !SmsFilter.shouldForward(sms, blacklistEnabled = true)
        }
        
        return TestCase("垃圾短信黑名单", allBlocked, "包含垃圾短信关键词的消息应被拒绝")
    }
    
    private fun testCustomRules(): TestCase {
        val customWhitelist = setOf("12345", "67890")
        val customBlacklist = setOf("测试", "禁止")
        
        val whitelistSms = SmsEvent("12345", "来自白名单的消息", System.currentTimeMillis())
        val blacklistSms = SmsEvent("95588", "包含测试关键词的消息", System.currentTimeMillis())
        
        val whitelistAllowed = SmsFilter.shouldForward(
            whitelistSms, 
            whitelistEnabled = true, 
            customWhitelist = customWhitelist
        )
        
        val blacklistBlocked = !SmsFilter.shouldForward(
            blacklistSms,
            blacklistEnabled = true,
            customBlacklist = customBlacklist
        )
        
        val result = whitelistAllowed && blacklistBlocked
        return TestCase("自定义规则", result, "自定义白名单和黑名单应正确工作")
    }
    
    private fun testComplexScenarios(): TestCase {
        // 复杂场景：银行号码发送的广告消息
        val bankAdSms = SmsEvent("95588", "银行理财产品推广，退订回T", System.currentTimeMillis())
        
        // 应该被黑名单拒绝，即使是银行号码
        val blocked = !SmsFilter.shouldForward(bankAdSms, whitelistEnabled = true, blacklistEnabled = true)
        
        return TestCase("复杂场景过滤", blocked, "黑名单优先级应高于白名单")
    }
}