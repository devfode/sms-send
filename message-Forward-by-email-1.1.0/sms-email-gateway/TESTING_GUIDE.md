# SMS Email Gateway - Testing Guide

**Generated**: 2025-09-01  
**Project**: SMS Email Gateway v1.0.0  
**APK Size**: 7.3MB

## ✅ Build Status

✅ **APK Successfully Built**
- Location: `app/build/outputs/apk/debug/app-debug.apk`
- Size: 7.3MB (7,306,453 bytes)
- Contains: classes.dex, AndroidManifest.xml, resources
- Build Time: ~30 seconds

✅ **Unit Tests Passed**
```bash
./gradlew testDebugUnitTest
# BUILD SUCCESSFUL in 813ms
```

✅ **Mailpit Email Server Ready**
- Status: Running on localhost:8025 (Web UI)
- SMTP: localhost:1025 (for app connection)
- API: http://localhost:8025/api/v1/messages

## 🧪 Testing Approaches

### Option 1: Physical Android Device (Recommended)

**Requirements:**
- Android device with USB debugging enabled
- Android 8.0+ (API 26+)

**Steps:**
```bash
# 1. Enable USB debugging on your Android device
# 2. Connect device via USB
adb devices  # Should show your device

# 3. Install the APK
adb install app/build/outputs/apk/debug/app-debug.apk

# 4. Grant notification access permission manually on device:
#    Settings > Apps > SMS Email Gateway > Permissions > 
#    Additional permissions > Notification access > Enable

# 5. Configure SMTP settings in the app:
#    - SMTP Server: YOUR_COMPUTER_IP (not localhost!)
#    - SMTP Port: 1025
#    - Auth: Disabled
#    - SSL/TLS: Disabled

# 6. Test SMS forwarding:
#    - Send SMS to your device
#    - Check http://YOUR_COMPUTER_IP:8025 for forwarded emails
```

### Option 2: Android Studio AVD

**Requirements:**
- Android Studio installed
- Intel/ARM system images

**Steps:**
```bash
# 1. Open Android Studio
# 2. AVD Manager > Create Virtual Device
# 3. Choose Pixel 7 or similar (API 34)
# 4. Start emulator
# 5. Install APK: drag & drop to emulator window
```

### Option 3: Code Analysis & Component Testing

**Already Verified:**
- ✅ Gradle build system works
- ✅ Dependencies resolved (Jakarta Mail 2.0.1)
- ✅ Resource compilation successful
- ✅ Kotlin compilation successful
- ✅ AndroidManifest permissions configured

**Key Components in APK:**
- `SmsNotificationListener` - SMS capture service
- `SmtpClient` - Email sending functionality  
- `ConfigStore` - Encrypted settings storage
- `SendEmailWorker` - Background task queue
- `SmsParser` - Privacy masking utilities
- `MainActivity` - Configuration UI

## 🔧 Configuration Testing

### SMTP Connection Test

Since Mailpit is running, you can test SMTP functionality:

```bash
# Test if Mailpit SMTP is accessible
telnet localhost 1025
# Should connect and show SMTP greeting

# Check Mailpit web interface
open http://localhost:8025
# Should show empty inbox (ready to receive)
```

### App Configuration

When testing on device, configure:

**SMTP Settings:**
```
Host: [Your computer's IP address]
Port: 1025  
Username: (leave empty)
Password: (leave empty)
SSL: Disabled
STARTTLS: Disabled
```

**Email Settings:**
```
From: sms-relay@yourname.com
To: your-email@example.com
Subject: 📱 SMS from {phone}
```

**Privacy Settings:**
```
☑ Enable phone number masking
☑ Enable OTP-only mode  
☑ Enable card number masking
```

## 📱 SMS Testing Scenarios

### Test Case 1: Regular SMS
**Send to your device:**
```
"Hello from John, call me at 123-456-7890"
```

**Expected Email:**
```
From: sms-relay@yourname.com
To: your-email@example.com
Subject: 📱 SMS from +1234***7890

From: +1234***7890 (John)
Time: 2025-09-01 14:30:25

Hello from John, call me at ***-***-7890
```

### Test Case 2: OTP SMS
**Send to your device:**
```
"Your verification code is 123456. Valid for 5 minutes."
```

**Expected Email:**
```
From: sms-relay@yourname.com
To: your-email@example.com
Subject: 🔐 OTP from +1234***7890

From: +1234***7890
Time: 2025-09-01 14:31:10

[OTP] Your verification code is 123456. Valid for 5 minutes.
```

### Test Case 3: Filtered SMS (if whitelist enabled)
**Send from unknown number:**
- Should NOT forward if sender not in whitelist
- Check app logs for filtering decision

## 🚨 Troubleshooting

### Common Issues:

1. **"adb: no devices/emulators found"**
   - Enable USB debugging on Android device
   - Install Android platform-tools
   - Use physical device instead of emulator

2. **"SMTP connection failed"**
   - Use your computer's IP address, not localhost
   - Check firewall settings  
   - Verify Mailpit is running: `docker ps | grep mailpit`

3. **"Notification access denied"**
   - Go to Android Settings > Apps > SMS Email Gateway
   - Enable Notification Access permission
   - This is required for SMS capture

4. **"App crashes on startup"**
   - Check Android version (need 8.0+)
   - Clear app data and restart
   - Check system logs: `adb logcat | grep smsrelay`

## 📊 Success Metrics

**Build Success:**
- [x] APK builds without errors
- [x] All dependencies resolve correctly
- [x] Resources compile successfully

**Runtime Success:**
- [ ] App starts without crashing
- [ ] SMTP configuration saves correctly
- [ ] Notification listener permission granted
- [ ] SMS successfully forwarded to email
- [ ] Privacy masking works correctly

## 🎯 Next Steps After Testing

1. **Production Setup:**
   - Configure real Gmail/QQ SMTP settings
   - Set up proper SSL/TLS certificates
   - Configure production email addresses

2. **Feature Enhancements:**
   - Add rule-based filtering UI
   - Implement contact name resolution
   - Add email delivery status tracking
   - Create backup/restore functionality

3. **Security Hardening:**
   - Implement certificate pinning
   - Add authentication tokens
   - Enable proper SSL/TLS validation

---

**Status**: 🟢 **Ready for device testing**

The APK is successfully built and contains all necessary components. The main limitation is the lack of an Android emulator, but the app can be tested on any physical Android device with USB debugging enabled.