# 短信转发云

**Android短信转邮箱直发工具** - 无需第三方中继，本地加密存储，支持白名单过滤与隐私掩码。

## 快速开始

### 1. 本地测试环境 (Mailpit)

#### macOS 设置
```bash
# 启动 Mailpit 测试服务器
docker run --rm -p 8025:8025 -p 1025:1025 axllent/mailpit

# 打开浏览器查看邮件
open http://localhost:8025
```

#### Android 模拟器配置
1. 安装 APK 到模拟器
2. 打开应用 → 设置 → 点击"Mailpit"预设
3. 目标邮箱填写: `test@example.com`
4. 保存配置

#### 发送测试短信
```bash
# 模拟收到短信
adb emu sms send +8613800138000 "【招商银行】您的验证码为 123456，请在5分钟内使用"

# 查看是否到达
open http://localhost:8025
```

### 2. 生产环境配置

#### Gmail 配置
1. 开启两步验证
2. 生成应用专用密码
3. 设置中选择"Gmail"预设:
   - 主机: `smtp.gmail.com`
   - 端口: `587`
   - 安全: `STARTTLS`
   - 账号: 你的Gmail地址
   - 密码: 应用专用密码

#### QQ邮箱配置
1. 邮箱设置 → 开启SMTP服务 → 获取授权码
2. 设置中选择"QQ邮箱"预设:
   - 主机: `smtp.qq.com`
   - 端口: `465`
   - 安全: `SSL`
   - 账号: 你的QQ邮箱
   - 密码: 授权码

## 权限设置

### 通知访问权限
1. 设置 → 应用 → 特殊访问权限 → 通知使用权
2. 找到"短信转发云"并开启

### 各厂商后台保活
- **MIUI**: 设置 → 应用管理 → 权限 → 自启动管理 → 开启
- **鸿蒙**: 设置 → 电池 → 应用启动管理 → 手动管理 → 全部开启
- **ColorOS**: 设置 → 电池 → 应用耗电管理 → 不限制

## 功能特性

- ✅ NotificationListener 短信捕获
- ✅ SMTP 直发邮箱 (SSL/STARTTLS)
- ✅ 加密存储配置
- ✅ 隐私掩码 (手机/卡号/邮箱)
- ✅ 仅转OTP模式
- ⏳ 前台服务保活
- ⏳ 白/黑名单规则
- ⏳ 双卡支持

## 安全说明

- 配置信息使用 EncryptedSharedPreferences 加密存储
- 短信正文不会明文保存，仅保留发送状态哈希
- 支持多级隐私掩码，保护敏感信息
- 仅转OTP模式下只发送验证码，过滤其他内容

## 开发调试

```bash
# 构建项目
./gradlew assembleDebug

# 安装到设备
adb install app/build/outputs/apk/debug/app-debug.apk

# 查看日志
adb logcat | grep SmsRelay
```

## 许可证

Apache-2.0 License