package com.example.alarm.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.alarm.core.notification.NotificationManager
import com.example.alarm.data.repository.AlarmRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Receives pre-alarm events 15 minutes before the main alarm.
 * Shows a notification with dismiss/skip/snooze actions.
 */
@AndroidEntryPoint
class PreAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmRepository: AlarmRepository

    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var alarmScheduler: AlarmSchedulerImpl

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "========================================")
        Log.d(TAG, "onReceive called!")
        Log.d(TAG, "  Action: ${intent.action}")
        Log.d(TAG, "  Extras: ${intent.extras?.keySet()?.joinToString()}")
        Log.d(TAG, "========================================")

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AlarmApp:PreAlarmReceiverWakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire(30_000L)
        }

        val pendingResult = goAsync()

        when (intent.action) {
            ACTION_PRE_ALARM_TRIGGER -> {
                val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
                if (alarmId == -1L) {
                    Log.e(TAG, "Invalid alarm ID")
                    if (wakeLock.isHeld) wakeLock.release()
                    pendingResult.finish()
                    return
                }
                CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
                    try {
                        handlePreAlarmTrigger(context, alarmId)
                    } catch (t: Throwable) {
                        Log.e(TAG, "Error handling pre-alarm trigger", t)
                    } finally {
                        if (wakeLock.isHeld) wakeLock.release()
                        pendingResult.finish()
                    }
                }
            }
            ACTION_DISMISS_PRE_ALARM -> {
                val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
                if (alarmId != -1L) {
                    handleDismiss(context, alarmId)
                }
                if (wakeLock.isHeld) wakeLock.release()
                pendingResult.finish()
            }
            ACTION_SKIP_ALARM -> {
                val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
                if (alarmId != -1L) {
                    CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
                        try {
                            handleSkipAlarm(context, alarmId)
                        } catch (t: Throwable) {
                            Log.e(TAG, "Error skipping alarm", t)
                        } finally {
                            if (wakeLock.isHeld) wakeLock.release()
                            pendingResult.finish()
                        }
                    }
                } else {
                    if (wakeLock.isHeld) wakeLock.release()
                    pendingResult.finish()
                }
            }
            ACTION_SNOOZE_PRE_ALARM -> {
                val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
                if (alarmId != -1L) {
                    CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
                        try {
                            handleSnoozeReminder(context, alarmId)
                        } catch (t: Throwable) {
                            Log.e(TAG, "Error snoozing pre-alarm", t)
                        } finally {
                            if (wakeLock.isHeld) wakeLock.release()
                            pendingResult.finish()
                        }
                    }
                } else {
                    if (wakeLock.isHeld) wakeLock.release()
                    pendingResult.finish()
                }
            }
            else -> {
                Log.w(TAG, "Unknown action: ${intent.action}")
                if (wakeLock.isHeld) wakeLock.release()
                pendingResult.finish()
            }
        }
    }

    private suspend fun handlePreAlarmTrigger(context: Context, alarmId: Long) {
        Log.d(TAG, "handlePreAlarmTrigger for alarm $alarmId")

        val alarm = alarmRepository.getById(alarmId)
        if (alarm == null) {
            Log.e(TAG, "Alarm $alarmId not found")
            return
        }

        if (!alarm.isEnabled) {
            Log.d(TAG, "Alarm $alarmId is disabled, skipping pre-alarm")
            return
        }

        val mainAlarmTime = alarm.timeMillis
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeString = dateFormat.format(Date(mainAlarmTime))

        val notification = notificationManager.buildPreAlarmNotification(
            title = alarm.title,
            alarmTime = timeString,
            alarmId = alarmId
        ).build()

        try {
            NotificationManagerCompat.from(context).notify(
                NotificationManager.NOTIFICATION_ID_PRE_ALARM,
                notification
            )
            Log.d(TAG, "Pre-alarm notification shown for alarm $alarmId")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to show pre-alarm notification", t)
        }
    }

    private fun handleDismiss(context: Context, alarmId: Long) {
        Log.d(TAG, "Dismissing pre-alarm notification for alarm $alarmId")
        try {
            NotificationManagerCompat.from(context).cancel(NotificationManager.NOTIFICATION_ID_PRE_ALARM)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to cancel notification", t)
        }
    }

    private suspend fun handleSkipAlarm(context: Context, alarmId: Long) {
        Log.d(TAG, "Skipping alarm $alarmId")
        try {
            // Cancel the main alarm
            alarmScheduler.cancel(alarmId)

            // Disable the alarm in the database
            val alarm = alarmRepository.getById(alarmId)
            if (alarm != null) {
                alarmRepository.update(alarm.copy(isEnabled = false))
            }

            // Remove the pre-alarm notification
            NotificationManagerCompat.from(context).cancel(NotificationManager.NOTIFICATION_ID_PRE_ALARM)

            Log.d(TAG, "Alarm $alarmId skipped successfully")
        } catch (t: Throwable) {
            Log.e(TAG, "Error skipping alarm $alarmId", t)
        }
    }

    private suspend fun handleSnoozeReminder(context: Context, alarmId: Long) {
        Log.d(TAG, "Snoozing pre-alarm reminder for alarm $alarmId")
        try {
            // Remove current notification
            NotificationManagerCompat.from(context).cancel(NotificationManager.NOTIFICATION_ID_PRE_ALARM)

            // Reschedule pre-alarm for 5 minutes from now
            val snoozeDelayMillis = 5 * 60 * 1000L
            val newPreAlarmTime = System.currentTimeMillis() + snoozeDelayMillis

            val alarm = alarmRepository.getById(alarmId)
            if (alarm != null) {
                schedulePreAlarmReminder(context, alarmId, alarm.title, newPreAlarmTime)
                Log.d(TAG, "Pre-alarm reminder snoozed for 5 minutes")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error snoozing pre-alarm reminder", t)
        }
    }

    private fun schedulePreAlarmReminder(
        context: Context,
        alarmId: Long,
        alarmTitle: String,
        triggerTimeMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PreAlarmReceiver::class.java).apply {
            action = ACTION_PRE_ALARM_TRIGGER
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_TITLE, alarmTitle)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (alarmId.toInt() * 1000) + PRE_ALARM_REQUEST_CODE_OFFSET,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Pre-alarm reminder scheduled for ${Date(triggerTimeMillis)}")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling pre-alarm reminder", e)
        }
    }

    companion object {
        private const val TAG = "PreAlarmReceiver"
        const val ACTION_PRE_ALARM_TRIGGER = "com.example.alarm.PRE_ALARM_TRIGGER"
        const val ACTION_DISMISS_PRE_ALARM = "com.example.alarm.DISMISS_PRE_ALARM"
        const val ACTION_SKIP_ALARM = "com.example.alarm.SKIP_ALARM"
        const val ACTION_SNOOZE_PRE_ALARM = "com.example.alarm.SNOOZE_PRE_ALARM"
        const val EXTRA_ALARM_ID = "alarmId"
        const val EXTRA_ALARM_TITLE = "alarmTitle"
        const val PRE_ALARM_REQUEST_CODE_OFFSET = 10000
    }
}
