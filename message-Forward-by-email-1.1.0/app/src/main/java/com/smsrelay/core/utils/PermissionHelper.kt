package com.smsrelay.core.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AlertDialog

object PermissionHelper {
    
    fun checkNotificationListenerPermission(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        
        val packageName = context.packageName
        return enabledListeners != null && enabledListeners.contains(packageName)
    }
    
    fun checkBatteryOptimizationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return powerManager.isIgnoringBatteryOptimizations(context.packageName)
        }
        return true
    }
    
    fun requestNotificationListenerPermission(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle("需要通知访问权限")
            .setMessage("为了监听短信通知，需要开启通知访问权限：\n\n" +
                    "1. 点击确定进入设置页面\n" +
                    "2. 找到\"短信转发云\"并开启\n" +
                    "3. 返回应用即可正常使用")
            .setPositiveButton("前往设置") { _, _ ->
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                activity.startActivity(intent)
            }
            .setNegativeButton("稍后设置", null)
            .setCancelable(false)
            .show()
    }
    
    fun requestBatteryOptimizationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AlertDialog.Builder(activity)
                .setTitle("需要电池优化白名单")
                .setMessage("为了确保应用在后台正常运行，建议将应用加入电池优化白名单：\n\n" +
                        "1. 点击确定进入设置页面\n" +
                        "2. 选择\"允许\"或\"不优化\"\n" +
                        "3. 返回应用即可")
                .setPositiveButton("前往设置") { _, _ ->
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    try {
                        activity.startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback to battery optimization settings
                        val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        activity.startActivity(fallbackIntent)
                    }
                }
                .setNegativeButton("稍后设置", null)
                .show()
        }
    }
    
    fun requestAutoStartPermission(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle("需要自启动权限")
            .setMessage("为了确保应用开机自启和后台保活，请手动设置自启动权限：\n\n" +
                    "• MIUI: 设置 → 应用管理 → 权限 → 自启动管理\n" +
                    "• 鸿蒙: 设置 → 电池 → 应用启动管理\n" +
                    "• ColorOS: 设置 → 电池 → 应用耗电管理\n" +
                    "• EMUI: 设置 → 应用启动管理")
            .setPositiveButton("知道了", null)
            .show()
    }
    
    fun checkAllRequiredPermissions(context: Context): Boolean {
        return checkNotificationListenerPermission(context) && 
               checkBatteryOptimizationPermission(context)
    }
    
    fun requestAllRequiredPermissions(activity: Activity, onComplete: (() -> Unit)? = null) {
        when {
            !checkNotificationListenerPermission(activity) -> {
                requestNotificationListenerPermission(activity)
            }
            !checkBatteryOptimizationPermission(activity) -> {
                requestBatteryOptimizationPermission(activity)
            }
            else -> {
                // Show auto-start permission reminder
                requestAutoStartPermission(activity)
                onComplete?.invoke()
            }
        }
    }
}