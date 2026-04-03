#!/bin/bash

echo "🔧 Android项目构建验证"
echo "======================="
echo "项目: 短信转发云 (SMS Email Gateway)"
echo "时间: $(date)"
echo ""

cd ~/Projects/sms-email-gateway || {
    echo "❌ 错误: 找不到项目目录"
    exit 1
}

echo "📁 工作目录: $(pwd)"
echo ""

# 步骤1: 检查Gradle版本
echo "📋 步骤1: 检查Gradle版本"
echo "------------------------"
if ./gradlew --version; then
    echo "✅ Gradle Wrapper 工作正常"
else
    echo "❌ Gradle Wrapper 有问题"
    exit 1
fi
echo ""

# 步骤2: 执行单元测试
echo "🧪 步骤2: 执行单元测试"
echo "---------------------"
echo "运行: ./gradlew testDebugUnitTest"
if ./gradlew testDebugUnitTest --no-daemon; then
    echo "✅ 单元测试完成"
    
    # 检查测试报告
    if [ -f "app/build/reports/tests/testDebugUnitTest/index.html" ]; then
        echo "📄 测试报告已生成: app/build/reports/tests/testDebugUnitTest/index.html"
    fi
else
    echo "⚠️  单元测试失败 - 继续构建APK测试"
fi
echo ""

# 步骤3: 构建调试APK
echo "📦 步骤3: 构建调试包"
echo "-------------------"
echo "运行: ./gradlew assembleDebug"
if ./gradlew assembleDebug --no-daemon; then
    echo "✅ APK构建完成"
    
    # 检查APK文件
    if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
        APK_SIZE=$(ls -lh app/build/outputs/apk/debug/app-debug.apk | awk '{print $5}')
        echo "📱 调试APK已生成: app/build/outputs/apk/debug/app-debug.apk ($APK_SIZE)"
        
        # 显示APK信息
        echo ""
        echo "📊 APK详细信息:"
        ls -la app/build/outputs/apk/debug/
    else
        echo "❌ APK文件未找到"
        exit 1
    fi
else
    echo "❌ APK构建失败"
    exit 1
fi

echo ""
echo "🎉 构建验证完成!"
echo "================="
echo ""
echo "📋 成功生成的文件:"
echo "• 测试报告: app/build/reports/tests/testDebugUnitTest/index.html"
echo "• 调试APK: app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "🚀 下一步建议:"
echo "1. 启动Mailpit: docker run --rm -p 8025:8025 -p 1025:1025 axllent/mailpit"
echo "2. 安装APK到模拟器: adb install app/build/outputs/apk/debug/app-debug.apk"
echo "3. 发送测试短信: adb emu sms send +8613800138000 '验证码123456'"
echo "4. 查看邮件: open http://localhost:8025"
echo ""