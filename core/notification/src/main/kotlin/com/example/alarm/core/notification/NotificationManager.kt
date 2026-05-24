package com.example.alarm.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
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
                // The channel itself must not play sound — RingService owns playback.
                // If the channel had a sound, it would double up over the MediaPlayer.
                setSound(null, null)
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val preAlarmChannel = NotificationChannel(
                CHANNEL_PRE_ALARM,
                "Pre-Alarm Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications 15 minutes before alarm"
            }

            val timerChannel = NotificationChannel(
                CHANNEL_TIMER,
                "Timer Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Live countdown timer notifications"
                setSound(null, null)
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(alarmChannel)
            notificationManager.createNotificationChannel(preAlarmChannel)
            notificationManager.createNotificationChannel(timerChannel)
        }
    }

    /**
     * Build the foreground-service notification for an active alarm.
     * Includes Dismiss + Snooze actions that can be triggered from the
     * notification shade or lockscreen even when the app is backgrounded.
     */
    fun buildAlarmNotification(
        title: String,
        message: String,
        alarmId: Long = -1L,
        contentIntent: PendingIntent? = null
    ): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(context, CHANNEL_ALARM)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)

        if (contentIntent != null) {
            builder.setContentIntent(contentIntent)
            builder.setFullScreenIntent(contentIntent, true)
        }

        if (alarmId != -1L) {
            val dismissIntent = Intent(ACTION_NOTIFICATION_DISMISS).apply {
                setPackage(context.packageName)
                putExtra(EXTRA_ALARM_ID, alarmId)
            }
            val dismissPi = PendingIntent.getBroadcast(
                context,
                (alarmId.toInt() * 31) + 1,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val snoozeIntent = Intent(ACTION_NOTIFICATION_SNOOZE).apply {
                setPackage(context.packageName)
                putExtra(EXTRA_ALARM_ID, alarmId)
            }
            val snoozePi = PendingIntent.getBroadcast(
                context,
                (alarmId.toInt() * 31) + 2,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Dismiss",
                dismissPi
            )
            builder.addAction(
                android.R.drawable.ic_menu_revert,
                "Snooze",
                snoozePi
            )

            // If the notification is swiped away, treat that as Dismiss so we
            // don't end up with a foreground service that leaks audio.
            builder.setDeleteIntent(dismissPi)
        }

        return builder
    }

    fun buildPreAlarmNotification(
        title: String,
        alarmTime: String,
        alarmId: Long
    ): NotificationCompat.Builder {
        val dismissIntent = Intent("com.example.alarm.DISMISS_PRE_ALARM").apply {
            setPackage(context.packageName)
            putExtra("alarmId", alarmId)
        }
        val dismissPi = PendingIntent.getBroadcast(
            context,
            (alarmId.toInt() * 1000) + 1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val skipIntent = Intent("com.example.alarm.SKIP_ALARM").apply {
            setPackage(context.packageName)
            putExtra("alarmId", alarmId)
        }
        val skipPi = PendingIntent.getBroadcast(
            context,
            (alarmId.toInt() * 1000) + 2,
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent("com.example.alarm.SNOOZE_PRE_ALARM").apply {
            setPackage(context.packageName)
            putExtra("alarmId", alarmId)
        }
        val snoozePi = PendingIntent.getBroadcast(
            context,
            (alarmId.toInt() * 1000) + 3,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_PRE_ALARM)
            .setContentTitle("$title in 15 minutes")
            .setContentText("Alarm scheduled for $alarmTime")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPi)
            .addAction(android.R.drawable.ic_delete, "Skip Alarm", skipPi)
            .addAction(android.R.drawable.ic_menu_revert, "Remind Later", snoozePi)
    }

    fun buildTimerNotification(
        title: String,
        message: String,
        contentIntent: PendingIntent? = null
    ): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)

        if (contentIntent != null) {
            builder.setContentIntent(contentIntent)
        }

        return builder
    }

    companion object {
        const val CHANNEL_ALARM = "alarm_channel"
        const val CHANNEL_PRE_ALARM = "pre_alarm_channel"
        const val CHANNEL_TIMER = "timer_channel"
        const val NOTIFICATION_ID_ALARM = 1001
        const val NOTIFICATION_ID_PRE_ALARM = 1002

        const val ACTION_NOTIFICATION_DISMISS = "com.example.alarm.NOTIFICATION_DISMISS"
        const val ACTION_NOTIFICATION_SNOOZE = "com.example.alarm.NOTIFICATION_SNOOZE"
        const val EXTRA_ALARM_ID = "alarmId"
    }
}
