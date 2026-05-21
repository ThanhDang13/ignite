package com.example.alarm.data.scheduler

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PreAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmScheduler: AlarmSchedulerImpl

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra("alarmId", -1L)
        if (alarmId == -1L) return

        Log.d("PreAlarmReceiver", "Pre-alarm action received for alarm $alarmId: ${intent.action}")

        when (intent.action) {
            PreAlarmWorker.ACTION_DISMISS_PRE_ALARM -> handleDismiss(context, alarmId)
            PreAlarmWorker.ACTION_SNOOZE_PRE_ALARM -> handleSnooze(context, alarmId)
        }

        // Dismiss the pre-alarm notification
        NotificationManagerCompat.from(context).cancel(com.example.alarm.core.notification.NotificationManager.NOTIFICATION_ID_PRE_ALARM)
    }

    private fun handleDismiss(context: Context, alarmId: Long) {
        Log.d("PreAlarmReceiver", "Dismissing pre-alarm for alarm $alarmId")
        // Cancel the main alarm
        val cancelIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.alarm.ALARM_TRIGGER"
            putExtra("alarmId", alarmId)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
        alarmManager.cancel(cancelPendingIntent)
    }

    private fun handleSnooze(context: Context, alarmId: Long) {
        Log.d("PreAlarmReceiver", "Snoozing pre-alarm for alarm $alarmId")
        // Cancel the pre-alarm work
        WorkManager.getInstance(context).cancelAllWorkByTag("pre_alarm_$alarmId")
    }
}
