#!/bin/bash

echo "🔧 测试 Gradle 构建环境"
echo "========================"

cd ~/Projects/sms-email-gateway

echo "📁 当前工作目录: $(pwd)"
echo "📂 项目结构:"
ls -la

echo -e "\n📋 Gradle Wrapper 文件:"
ls -la gradle/wrapper/
ls -la gradlew

echo -e "\n🔍 settings.gradle.kts 配置:"
head -20 settings.gradle.kts

echo -e "\n⚡ 测试 Gradle Wrapper (非阻塞):"
echo "运行命令: ./gradlew --version"

# 尝试运行gradle version，但设置较短超时
if command -v timeout >/dev/null 2>&1; then
    timeout 30s ./gradlew --version
elif command -v gtimeout >/dev/null 2>&1; then
    gtimeout 30s ./gradlew --version  
else
    echo "正在运行 ./gradlew --version (可能需要下载 Gradle)..."
    # 在后台运行，避免阻塞
    ./gradlew --version &
    GRADLE_PID=$!
    
    # 等待 30 秒
    for i in {1..30}; do
        if ! kill -0 $GRADLE_PID 2>/dev/null; then
            echo "Gradle 命令完成!"
            wait $GRADLE_PID
            break
        fi
        echo "等待中... ($i/30)"
        sleep 1
    done
    
    # 如果还在运行，终止进程
    if kill -0 $GRADLE_PID 2>/dev/null; then
        echo "命令超时，终止 Gradle 进程..."
        kill $GRADLE_PID 2>/dev/null
        echo "Gradle 可能还在下载中，这是正常的。"
    fi
fi

echo -e "\n✅ 环境检查完成"
echo "💡 如果 Gradle 还在下载，请稍后手动运行："
echo "   cd ~/Projects/sms-email-gateway"  
echo "   ./gradlew assembleDebug"