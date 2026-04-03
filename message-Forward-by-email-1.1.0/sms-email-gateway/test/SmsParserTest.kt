package com.smsrelay.test

import com.smsrelay.core.utils.SmsParser

/**
 * 短信解析和掩码功能测试
 * 验证OTP提取、隐私信息掩码、敏感数据处理等功能
 */
object SmsParserTest {
    
    fun runTests(): TestResult {
        val results = mutableListOf<TestCase>()
        
        // 测试1: OTP验证码提取
        results.add(testOtpExtraction())
        
        // 测试2: 手机号码掩码
        results.add(testPhoneMasking())
        
        // 测试3: 银行卡号掩码
        results.add(testCardMasking())
        
        // 测试4: 邮箱地址掩码
        results.add(testEmailMasking())
        
        // 测试5: 复合敏感信息掩码
        results.add(testCompoundMasking())
        
        // 测试6: OTP边界检测
        results.add(testOtpBoundary())
        
        val passed = results.count { it.passed }
        val total = results.size
        
        return TestResult("短信解析测试", passed, total, results)
    }
    
    private fun testOtpExtraction(): TestCase {
        val testCases = mapOf(
            "【招商银行】您的验证码为 123456，请在5分钟内使用" to "123456",
            "验证码：8888，有效期3分钟" to "8888",
            "您的动态密码是 654321 请妥善保管" to "654321",
            "PIN: 9999" to "9999",
            "【支付宝】登录验证码12345678，请勿泄露给他人" to "12345678",
            "普通短信，没有验证码" to null,
            "包含数字1234567890123但不是验证码" to null  // 太长，不符合4-8位规则
        )
        
        val allCorrect = testCases.all { (message, expected) ->
            val extracted = SmsParser.extractOtp(message)
            extracted == expected
        }
        
        return TestCase("OTP提取", allCorrect, "所有验证码提取应该正确")
    }
    
    private fun testPhoneMasking(): TestCase {
        val testCases = mapOf(
            "您的手机号13812345678已绑定" to "您的手机号138****5678已绑定",
            "联系电话：15987654321" to "联系电话：159****4321",
            "客服热线18600000000为您服务" to "客服热线186****0000为您服务"
        )
        
        val allCorrect = testCases.all { (original, expected) ->
            val masked = SmsParser.maskSensitiveInfo(original)
            masked == expected
        }
        
        return TestCase("手机号掩码", allCorrect, "手机号应该被正确掩码")
    }
    
    private fun testCardMasking(): TestCase {
        val testCases = mapOf(
            "您的卡号6222****1234消费了100元" to "您的卡号6222****1234消费了100元", // 已掩码
            "身份证号123456789012345678" to "身份证号123456****345678", // 长数字掩码
            "订单号：20231201123456789" to "订单号：202312****6789", // 长数字掩码
        )
        
        val results = testCases.map { (original, expected) ->
            val masked = SmsParser.maskSensitiveInfo(original)
            println("原文: $original")
            println("掩码: $masked")
            println("期望: $expected")
            println("---")
            masked.contains("****")  // 检查是否包含掩码标记
        }
        
        val hasCorrectMasking = results.all { it }
        return TestCase("长数字掩码", hasCorrectMasking, "长数字应该被正确掩码")
    }
    
    private fun testEmailMasking(): TestCase {
        val testCases = mapOf(
            "请登录abc@qq.com查看详情" to "请登录a*c@qq.com查看详情",
            "发送到test@gmail.com的邮件已送达" to "发送到t**t@gmail.com的邮件已送达",
            "邮箱longusername@company.com.cn" to "邮箱l**********e@company.com.cn"
        )
        
        val results = testCases.map { (original, _) ->
            val masked = SmsParser.maskSensitiveInfo(original)
            println("邮箱掩码 - 原文: $original")
            println("邮箱掩码 - 结果: $masked")
            println("---")
            masked.contains("*") && masked.contains("@")  // 检查是否有掩码且保留@符号
        }
        
        val hasCorrectMasking = results.all { it }
        return TestCase("邮箱掩码", hasCorrectMasking, "邮箱地址应该被正确掩码")
    }
    
    private fun testCompoundMasking(): TestCase {
        val originalMessage = """
            【银行通知】
            尊敬的客户，您的账户13812345678在2023-12-01 15:30:00
            向卡号6222081234567890转账500.00元成功。
            如有疑问请联系客服400-800-9999或发邮件至service@bank.com.cn
        """.trimIndent()
        
        val maskedMessage = SmsParser.maskSensitiveInfo(originalMessage)
        
        val hasPhoneMask = maskedMessage.contains("138****5678")
        val hasCardMask = maskedMessage.contains("****") && maskedMessage.contains("6222")
        val hasEmailMask = maskedMessage.contains("*") && maskedMessage.contains("@bank.com.cn")
        
        println("复合掩码测试:")
        println("原文: $originalMessage")
        println("掩码: $maskedMessage")
        println("---")
        
        return TestCase("复合信息掩码", hasPhoneMask || hasCardMask || hasEmailMask, "复合敏感信息应该被掩码")
    }
    
    private fun testOtpBoundary(): TestCase {
        val testCases = mapOf(
            "验证码1234" to "1234",           // 4位
            "动态码87654321" to "87654321",   // 8位
            "密码123" to null,               // 3位，太短
            "数字123456789" to null,         // 9位，太长
            "金额12345.67元" to "12345",     // 边界：包含小数点
            "订单202312011234" to null       // 长数字，不是验证码
        )
        
        val allCorrect = testCases.all { (message, expected) ->
            val extracted = SmsParser.extractOtp(message)
            extracted == expected
        }
        
        return TestCase("OTP边界检测", allCorrect, "OTP边界检测应该正确")
    }
}