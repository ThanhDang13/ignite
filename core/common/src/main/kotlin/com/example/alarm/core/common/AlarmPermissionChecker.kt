package com.example.alarm.core.common

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Centralized permission and platform-restriction checks. Alarm reliability
 * depends on three system-level conditions: notifications, exact alarms, and
 * battery-optimization exemption. These helpers let the UI surface those
 * states clearly to the user and request fixes.
 */
object AlarmPermissionChecker {

    /** True if the app can post notifications (always true pre-Android 13). */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /** True if the app may schedule exact alarms (always true pre-Android 12). */
    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * True if the app is exempt from battery optimization (Doze restrictions).
     * Without this, "pause app activity if unused" can stop alarms from firing
     * after the device has been idle for hours.
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Intent that opens the system settings page for exact alarm permission. */
    fun exactAlarmSettingsIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            null
        }
    }

    /**
     * Intent that asks the user to whitelist this app from battery optimization.
     * Note: this is a sensitive permission and apps that abuse it can be
     * rejected from Play Store, but for an alarm clock it's a legitimate use.
     */
    @Suppress("BatteryLife")
    fun ignoreBatteryOptimizationIntent(context: Context): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /** Intent that opens app-level notification settings. */
    fun appNotificationSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    data class Status(
        val hasNotificationPermission: Boolean,
        val canScheduleExactAlarms: Boolean,
        val isIgnoringBatteryOptimizations: Boolean
    ) {
        val allGranted: Boolean
            get() = hasNotificationPermission && canScheduleExactAlarms && isIgnoringBatteryOptimizations
    }

    fun status(context: Context): Status = Status(
        hasNotificationPermission = hasNotificationPermission(context),
        canScheduleExactAlarms = canScheduleExactAlarms(context),
        isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(context)
    )
}
