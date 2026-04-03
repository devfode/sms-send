package com.smsrelay.test

/**
 * 测试运行器
 * 执行所有测试并生成报告
 */
object TestRunner {
    
    fun runAllTests(): TestSummary {
        println("🧪 开始执行短信转发云功能测试")
        println("=" * 50)
        
        val testSuites = listOf(
            SmtpConfigTest.runTests(),
            SmsParserTest.runTests(),
            SmsFilterTest.runTests()
        )
        
        // 输出详细结果
        testSuites.forEach { suite ->
            printTestSuite(suite)
        }
        
        // 生成总结
        val totalPassed = testSuites.sumOf { it.passed }
        val totalTests = testSuites.sumOf { it.total }
        val allSuitesCount = testSuites.size
        val passedSuitesCount = testSuites.count { it.passed == it.total }
        
        val summary = TestSummary(
            totalTests = totalTests,
            totalPassed = totalPassed,
            totalFailed = totalTests - totalPassed,
            suiteResults = testSuites
        )
        
        printSummary(summary)
        return summary
    }
    
    private fun printTestSuite(suite: TestResult) {
        val status = if (suite.passed == suite.total) "✅ PASS" else "❌ FAIL"
        println("\n$status ${suite.suiteName} (${suite.passed}/${suite.total})")
        println("-" * 30)
        
        suite.cases.forEach { case ->
            val caseStatus = if (case.passed) "  ✓" else "  ✗"
            println("$caseStatus ${case.name}")
            if (!case.passed) {
                println("    理由: ${case.message}")
            }
        }
    }
    
    private fun printSummary(summary: TestSummary) {
        println("\n" + "=" * 50)
        println("📋 测试总结")
        println("=" * 50)
        
        val overallStatus = if (summary.totalFailed == 0) "✅ 全部通过" else "❌ 存在失败"
        
        println("总体状态: $overallStatus")
        println("测试用例: ${summary.totalPassed}/${summary.totalTests} 通过")
        println("测试套件: ${summary.suiteResults.count { it.passed == it.total }}/${summary.suiteResults.size} 通过")
        
        if (summary.totalFailed > 0) {
            println("\n❌ 失败的测试:")
            summary.suiteResults.forEach { suite ->
                suite.cases.filter { !it.passed }.forEach { case ->
                    println("  • ${suite.suiteName} -> ${case.name}: ${case.message}")
                }
            }
        }
        
        println("\n🎯 下一步建议:")
        if (summary.totalFailed == 0) {
            println("  • 所有核心功能测试通过，可以进行实际设备测试")
            println("  • 建议启动 Mailpit 进行端到端测试")
            println("  • 可以编译APK到Android设备进行完整测试")
        } else {
            println("  • 修复失败的测试用例")
            println("  • 重新运行测试确保修复有效")
            println("  • 检查相关代码逻辑")
        }
    }
}

data class TestSummary(
    val totalTests: Int,
    val totalPassed: Int,
    val totalFailed: Int,
    val suiteResults: List<TestResult>
)

// 扩展函数用于字符串重复
private operator fun String.times(n: Int): String = repeat(n)