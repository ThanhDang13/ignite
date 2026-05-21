package com.example.alarm.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM,
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for alarm events"
                enableVibration(true)
            }

            val preAlarmChannel = NotificationChannel(
                CHANNEL_PRE_ALARM,
                "Pre-Alarm Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications 15 minutes before alarm"
            }

            notificationManager.createNotificationChannel(alarmChannel)
            notificationManager.createNotificationChannel(preAlarmChannel)
        }
    }

    fun buildAlarmNotification(title: String, message: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ALARM)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
    }

    fun buildPreAlarmNotification(title: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_PRE_ALARM)
            .setContentTitle("Alarm in 15 minutes")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    }

    companion object {
        const val CHANNEL_ALARM = "alarm_channel"
        const val CHANNEL_PRE_ALARM = "pre_alarm_channel"
        const val NOTIFICATION_ID_ALARM = 1001
        const val NOTIFICATION_ID_PRE_ALARM = 1002
    }
}
