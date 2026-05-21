package com.example.alarm.data.scheduler

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.alarm.core.notification.NotificationManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class PreAlarmWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NotificationManagerEntryPoint {
        fun notificationManager(): NotificationManager
    }

    override suspend fun doWork(): Result {
        val alarmId = inputData.getLong("alarmId", -1L)
        val alarmTitle = inputData.getString("alarmTitle") ?: "Alarm"
        if (alarmId == -1L) return Result.failure()

        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                NotificationManagerEntryPoint::class.java
            )
            val notificationManager = entryPoint.notificationManager()

            val dismissIntent = Intent(applicationContext, PreAlarmReceiver::class.java).apply {
                action = ACTION_DISMISS_PRE_ALARM
                putExtra("alarmId", alarmId)
            }
            val dismissPendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                alarmId.toInt() * 1000 + 1,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val snoozeIntent = Intent(applicationContext, PreAlarmReceiver::class.java).apply {
                action = ACTION_SNOOZE_PRE_ALARM
                putExtra("alarmId", alarmId)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                alarmId.toInt() * 1000 + 2,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = notificationManager.buildPreAlarmNotification(alarmTitle)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
                .addAction(android.R.drawable.ic_menu_view, "Snooze", snoozePendingIntent)
                .build()

            NotificationManagerCompat.from(applicationContext).notify(
                NotificationManager.NOTIFICATION_ID_PRE_ALARM,
                notification
            )

            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    companion object {
        const val ACTION_DISMISS_PRE_ALARM = "com.example.alarm.DISMISS_PRE_ALARM"
        const val ACTION_SNOOZE_PRE_ALARM = "com.example.alarm.SNOOZE_PRE_ALARM"
    }
}
