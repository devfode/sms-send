package com.smsrelay.test

import com.smsrelay.core.model.SmtpConfig

/**
 * SMTP配置功能测试
 * 用于验证SMTP配置的创建、验证和格式化功能
 */
object SmtpConfigTest {
    
    fun runTests(): TestResult {
        val results = mutableListOf<TestCase>()
        
        // 测试1: 有效的Gmail配置
        results.add(testValidGmailConfig())
        
        // 测试2: 有效的QQ邮箱配置
        results.add(testValidQQConfig())
        
        // 测试3: Mailpit本地测试配置
        results.add(testMailpitConfig())
        
        // 测试4: 无效配置检测
        results.add(testInvalidConfigs())
        
        // 测试5: 安全类型测试
        results.add(testSecurityTypes())
        
        val passed = results.count { it.passed }
        val total = results.size
        
        return TestResult("SMTP配置测试", passed, total, results)
    }
    
    private fun testValidGmailConfig(): TestCase {
        return try {
            val config = SmtpConfig(
                host = "smtp.gmail.com",
                port = 587,
                security = SmtpConfig.SecurityType.STARTTLS,
                username = "test@gmail.com",
                password = "app_password",
                toEmail = "recipient@example.com",
                fromName = "SMS Relay"
            )
            
            val isValid = config.isValid()
            TestCase("Gmail配置有效性", isValid, "Gmail配置应该通过验证")
        } catch (e: Exception) {
            TestCase("Gmail配置有效性", false, "配置创建失败: ${e.message}")
        }
    }
    
    private fun testValidQQConfig(): TestCase {
        return try {
            val config = SmtpConfig(
                host = "smtp.qq.com",
                port = 465,
                security = SmtpConfig.SecurityType.SSL,
                username = "test@qq.com",
                password = "auth_code",
                toEmail = "recipient@example.com"
            )
            
            val isValid = config.isValid()
            TestCase("QQ邮箱配置有效性", isValid, "QQ邮箱配置应该通过验证")
        } catch (e: Exception) {
            TestCase("QQ邮箱配置有效性", false, "配置创建失败: ${e.message}")
        }
    }
    
    private fun testMailpitConfig(): TestCase {
        return try {
            val config = SmtpConfig(
                host = "10.0.2.2",
                port = 1025,
                security = SmtpConfig.SecurityType.NONE,
                username = "test",
                password = "test",
                toEmail = "test@example.com"
            )
            
            val isValid = config.isValid()
            TestCase("Mailpit配置有效性", isValid, "Mailpit配置应该通过验证")
        } catch (e: Exception) {
            TestCase("Mailpit配置有效性", false, "配置创建失败: ${e.message}")
        }
    }
    
    private fun testInvalidConfigs(): TestCase {
        val invalidConfigs = listOf(
            // 空主机
            SmtpConfig("", 587, SmtpConfig.SecurityType.STARTTLS, "user", "pass", "to@example.com"),
            // 无效端口
            SmtpConfig("smtp.gmail.com", 0, SmtpConfig.SecurityType.STARTTLS, "user", "pass", "to@example.com"),
            // 空用户名
            SmtpConfig("smtp.gmail.com", 587, SmtpConfig.SecurityType.STARTTLS, "", "pass", "to@example.com"),
            // 空密码
            SmtpConfig("smtp.gmail.com", 587, SmtpConfig.SecurityType.STARTTLS, "user", "", "to@example.com"),
            // 空目标邮箱
            SmtpConfig("smtp.gmail.com", 587, SmtpConfig.SecurityType.STARTTLS, "user", "pass", "")
        )
        
        val allInvalid = invalidConfigs.all { !it.isValid() }
        return TestCase("无效配置检测", allInvalid, "无效配置应该被正确识别")
    }
    
    private fun testSecurityTypes(): TestCase {
        return try {
            val sslConfig = SmtpConfig("smtp.qq.com", 465, SmtpConfig.SecurityType.SSL, "user", "pass", "to@example.com")
            val starttlsConfig = SmtpConfig("smtp.gmail.com", 587, SmtpConfig.SecurityType.STARTTLS, "user", "pass", "to@example.com")
            val noneConfig = SmtpConfig("localhost", 25, SmtpConfig.SecurityType.NONE, "user", "pass", "to@example.com")
            
            val allValid = sslConfig.isValid() && starttlsConfig.isValid() && noneConfig.isValid()
            TestCase("安全类型配置", allValid, "所有安全类型应该被正确支持")
        } catch (e: Exception) {
            TestCase("安全类型配置", false, "安全类型配置失败: ${e.message}")
        }
    }
}

data class TestCase(val name: String, val passed: Boolean, val message: String)
data class TestResult(val suiteName: String, val passed: Int, val total: Int, val cases: List<TestCase>)