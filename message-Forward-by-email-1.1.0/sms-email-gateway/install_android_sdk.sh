#!/bin/bash

echo "🤖 Android SDK 安装和配置向导"
echo "================================"
echo "项目: 短信转发云 (SMS Email Gateway)"
echo "时间: $(date)"
echo ""

# 检查当前SDK状态
echo "📋 当前SDK状态检查"
echo "------------------"
if [ -n "$ANDROID_HOME" ]; then
    echo "✅ ANDROID_HOME: $ANDROID_HOME"
else
    echo "❌ ANDROID_HOME 未设置"
fi

if command -v adb >/dev/null 2>&1; then
    echo "✅ ADB 可用: $(which adb)"
else
    echo "❌ ADB 未安装"
fi

echo ""

# 提供安装选择
echo "🛠️  安装选项"
echo "============="
echo ""
echo "方案 A（推荐）：使用 Android Studio"
echo "--------------------------------"
echo "1. 下载并安装 Android Studio:"
echo "   https://developer.android.com/studio"
echo ""
echo "2. 启动 Android Studio"
echo "3. 通过 SDK Manager 安装:"
echo "   • Android SDK Platform-Tools"
echo "   • Android SDK Build-Tools 35.0.0"  
echo "   • Android 14 (API 34) Platform"
echo "4. 接受所有许可协议"
echo ""
echo "优势: 图形界面，自动管理依赖，完整的开发环境"
echo ""

echo "方案 B：仅安装 SDK Command Line Tools (当前脚本将执行)"
echo "======================================================"
echo "1. 下载 Android SDK Command Line Tools"
echo "2. 解压到 ~/Library/Android/sdk"
echo "3. 使用 sdkmanager 安装必需组件"
echo "4. 配置环境变量"
echo ""
echo "优势: 轻量级，仅包含构建所需组件"
echo ""

# 询问用户选择
read -p "选择安装方案 [A/B] (默认B): " choice
choice=${choice:-B}

case $choice in
    [Aa])
        echo ""
        echo "📱 请按以下步骤使用 Android Studio:"
        echo "1. 访问 https://developer.android.com/studio 下载"
        echo "2. 安装完成后运行此脚本进行环境配置"
        echo ""
        echo "⏳ 安装完成后请重新运行: ./install_android_sdk.sh"
        exit 0
        ;;
    [Bb])
        echo ""
        echo "🚀 开始自动安装 SDK Command Line Tools..."
        ;;
    *)
        echo "❌ 无效选择，退出"
        exit 1
        ;;
esac

# 创建SDK目录
SDK_DIR="$HOME/Library/Android/sdk"
mkdir -p "$SDK_DIR"
cd "$SDK_DIR"

echo ""
echo "📁 SDK 安装目录: $SDK_DIR"

# 下载Command Line Tools
echo ""
echo "📥 下载 Android SDK Command Line Tools..."
CMDTOOLS_URL="https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip"
CMDTOOLS_ZIP="commandlinetools.zip"

if command -v curl >/dev/null 2>&1; then
    curl -L -o "$CMDTOOLS_ZIP" "$CMDTOOLS_URL"
elif command -v wget >/dev/null 2>&1; then
    wget -O "$CMDTOOLS_ZIP" "$CMDTOOLS_URL"
else
    echo "❌ 错误: 需要 curl 或 wget 来下载文件"
    exit 1
fi

# 解压
echo "📦 解压 Command Line Tools..."
unzip -q "$CMDTOOLS_ZIP"
rm "$CMDTOOLS_ZIP"

# 重命名目录结构 (新版本需要)
if [ -d "cmdline-tools" ]; then
    mkdir -p cmdline-tools/latest
    mv cmdline-tools/bin cmdline-tools/lib cmdline-tools/NOTICE.txt cmdline-tools/source.properties cmdline-tools/latest/ 2>/dev/null
fi

# 设置临时环境变量
export ANDROID_HOME="$SDK_DIR"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"

echo ""
echo "📋 安装必需的SDK组件..."

# 接受许可
echo "✅ 接受SDK许可协议..."
yes | "$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null 2>&1

# 安装必需组件
echo "📱 安装 Platform-Tools..."
"$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" "platform-tools"

echo "🔨 安装 Build-Tools 34.0.0..."  
"$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" "build-tools;34.0.0"

echo "🎯 安装 Android 14 Platform..."
"$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" "platforms;android-34"

echo ""
echo "⚙️  配置环境变量..."

# 配置shell profile
SHELL_PROFILE=""
if [ -f "$HOME/.zshrc" ]; then
    SHELL_PROFILE="$HOME/.zshrc"
elif [ -f "$HOME/.bash_profile" ]; then
    SHELL_PROFILE="$HOME/.bash_profile"
elif [ -f "$HOME/.bashrc" ]; then
    SHELL_PROFILE="$HOME/.bashrc"
fi

if [ -n "$SHELL_PROFILE" ]; then
    echo "" >> "$SHELL_PROFILE"
    echo "# Android SDK Environment" >> "$SHELL_PROFILE"
    echo "export ANDROID_HOME=\"$SDK_DIR\"" >> "$SHELL_PROFILE"
    echo "export PATH=\"\$PATH:\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools\"" >> "$SHELL_PROFILE"
    echo ""
    echo "✅ 环境变量已添加到 $SHELL_PROFILE"
    echo "💡 请运行: source $SHELL_PROFILE  或重新打开终端"
else
    echo "⚠️  未找到 shell 配置文件，请手动设置环境变量:"
    echo "export ANDROID_HOME=\"$SDK_DIR\""
    echo "export PATH=\"\$PATH:\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools\""
fi

# 创建local.properties
echo ""
echo "📝 创建 local.properties 文件..."
cat > ~/Projects/sms-email-gateway/local.properties << EOF
# Android SDK location  
sdk.dir=$SDK_DIR
EOF

echo "✅ local.properties 已创建"

echo ""
echo "🎉 Android SDK 安装完成!"
echo "========================"
echo ""
echo "📊 安装总结:"
echo "• SDK 路径: $SDK_DIR"
echo "• Platform-Tools: ✅"
echo "• Build-Tools: ✅"  
echo "• Android 14 Platform: ✅"
echo "• local.properties: ✅"
echo ""
echo "🚀 下一步:"
echo "1. 重新加载环境变量: source $SHELL_PROFILE"
echo "2. 验证安装: adb --version"
echo "3. 测试构建: cd ~/Projects/sms-email-gateway && ./gradlew assembleDebug"
echo ""