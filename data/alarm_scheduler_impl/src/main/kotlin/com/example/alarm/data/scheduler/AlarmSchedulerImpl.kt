package com.example.alarm.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.AlarmManagerCompat
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.alarm.core.common.AlarmTimeCalculator
import com.example.alarm.core.scheduler.AlarmScheduler
import com.example.alarm.core.scheduler.ScheduleRequest
import com.example.alarm.data.repository.AlarmRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AlarmSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmRepository: AlarmRepository
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override suspend fun schedule(request: ScheduleRequest): Unit = withContext(Dispatchers.Default) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.alarm.ALARM_TRIGGER"
            putExtra("alarmId", request.alarmId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            request.alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val triggerDate = dateFormat.format(Date(request.triggerTimeMillis))
        val now = System.currentTimeMillis()
        val delaySeconds = (request.triggerTimeMillis - now) / 1000

        Log.d("AlarmSchedulerImpl", "Scheduling alarm ${request.alarmId}")
        Log.d("AlarmSchedulerImpl", "  Current time: ${dateFormat.format(Date(now))}")
        Log.d("AlarmSchedulerImpl", "  Trigger time: $triggerDate")
        Log.d("AlarmSchedulerImpl", "  Delay: $delaySeconds seconds")
        Log.d("AlarmSchedulerImpl", "  Action: ${intent.action}")
        Log.d("AlarmSchedulerImpl", "  Request code: ${request.alarmId.toInt()}")

        try {
            if (request.isExact) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            request.triggerTimeMillis,
                            pendingIntent
                        )
                        Log.d("AlarmSchedulerImpl", "  Method: setExactAndAllowWhileIdle")
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            request.triggerTimeMillis,
                            pendingIntent
                        )
                        Log.w("AlarmSchedulerImpl", "  Method: setAndAllowWhileIdle (exact alarm permission not granted)")
                    }
                } else {
                    AlarmManagerCompat.setExactAndAllowWhileIdle(
                        alarmManager,
                        AlarmManager.RTC_WAKEUP,
                        request.triggerTimeMillis,
                        pendingIntent
                    )
                    Log.d("AlarmSchedulerImpl", "  Method: setExactAndAllowWhileIdle (compat)")
                }
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    request.triggerTimeMillis,
                    pendingIntent
                )
                Log.d("AlarmSchedulerImpl", "  Method: setAndAllowWhileIdle")
            }
            Log.d("AlarmSchedulerImpl", "Alarm ${request.alarmId} scheduled successfully")

            // Schedule pre-alarm notification 15 minutes before
            schedulePreAlarm(request.alarmId, request.triggerTimeMillis)
        } catch (e: SecurityException) {
            Log.e("AlarmSchedulerImpl", "SecurityException scheduling alarm ${request.alarmId}, falling back", e)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                request.triggerTimeMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e("AlarmSchedulerImpl", "Error scheduling alarm ${request.alarmId}", e)
        }
    }

    private suspend fun schedulePreAlarm(alarmId: Long, triggerTimeMillis: Long) {
        try {
            val alarm = alarmRepository.getById(alarmId) ?: return
            if (!alarm.preAlarmEnabled) return

            val preAlarmTimeMillis = triggerTimeMillis - (15 * 60 * 1000) // 15 minutes before
            val now = System.currentTimeMillis()

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            Log.d("AlarmSchedulerImpl", "Scheduling pre-alarm for alarm $alarmId")
            Log.d("AlarmSchedulerImpl", "  Current time: ${dateFormat.format(Date(now))}")
            Log.d("AlarmSchedulerImpl", "  Pre-alarm time: ${dateFormat.format(Date(preAlarmTimeMillis))}")
            Log.d("AlarmSchedulerImpl", "  Main alarm time: ${dateFormat.format(Date(triggerTimeMillis))}")

            if (preAlarmTimeMillis <= now) {
                Log.d("AlarmSchedulerImpl", "Pre-alarm time is in the past or very close, showing immediate notification")
                // Pre-alarm window already passed or very close - show notification immediately
                showImmediatePreAlarmNotification(alarmId, alarm.title, triggerTimeMillis)
                return
            }

            val intent = Intent(context, PreAlarmReceiver::class.java).apply {
                action = PreAlarmReceiver.ACTION_PRE_ALARM_TRIGGER
                putExtra(PreAlarmReceiver.EXTRA_ALARM_ID, alarmId)
                putExtra(PreAlarmReceiver.EXTRA_ALARM_TITLE, alarm.title)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                (alarmId.toInt() * 1000) + PreAlarmReceiver.PRE_ALARM_REQUEST_CODE_OFFSET,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            preAlarmTimeMillis,
                            pendingIntent
                        )
                        Log.d("AlarmSchedulerImpl", "  Method: setExactAndAllowWhileIdle")
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            preAlarmTimeMillis,
                            pendingIntent
                        )
                        Log.w("AlarmSchedulerImpl", "  Method: setAndAllowWhileIdle (exact alarm permission not granted)")
                    }
                } else {
                    AlarmManagerCompat.setExactAndAllowWhileIdle(
                        alarmManager,
                        AlarmManager.RTC_WAKEUP,
                        preAlarmTimeMillis,
                        pendingIntent
                    )
                    Log.d("AlarmSchedulerImpl", "  Method: setExactAndAllowWhileIdle (compat)")
                }
                Log.d("AlarmSchedulerImpl", "Pre-alarm scheduled successfully for alarm $alarmId")
            } catch (e: SecurityException) {
                Log.e("AlarmSchedulerImpl", "SecurityException scheduling pre-alarm", e)
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    preAlarmTimeMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.e("AlarmSchedulerImpl", "Error scheduling pre-alarm for $alarmId", e)
        }
    }

    private fun showImmediatePreAlarmNotification(alarmId: Long, alarmTitle: String, mainAlarmTimeMillis: Long) {
        try {
            val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val timeString = dateFormat.format(Date(mainAlarmTimeMillis))

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

            // Build the pre-alarm notification using the same builder as PreAlarmReceiver
            val dismissIntent = Intent(context, PreAlarmReceiver::class.java).apply {
                action = PreAlarmReceiver.ACTION_DISMISS_PRE_ALARM
                putExtra(PreAlarmReceiver.EXTRA_ALARM_ID, alarmId)
            }
            val dismissPi = PendingIntent.getBroadcast(
                context,
                (alarmId.toInt() * 1000) + 1,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val skipIntent = Intent(context, PreAlarmReceiver::class.java).apply {
                action = PreAlarmReceiver.ACTION_SKIP_ALARM
                putExtra(PreAlarmReceiver.EXTRA_ALARM_ID, alarmId)
            }
            val skipPi = PendingIntent.getBroadcast(
                context,
                (alarmId.toInt() * 1000) + 2,
                skipIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val snoozeIntent = Intent(context, PreAlarmReceiver::class.java).apply {
                action = PreAlarmReceiver.ACTION_SNOOZE_PRE_ALARM
                putExtra(PreAlarmReceiver.EXTRA_ALARM_ID, alarmId)
            }
            val snoozePi = PendingIntent.getBroadcast(
                context,
                (alarmId.toInt() * 1000) + 3,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val remainingMinutes = ((mainAlarmTimeMillis - System.currentTimeMillis()) / 60000).toInt()
            val remainingText = if (remainingMinutes > 0) "in $remainingMinutes minutes" else "very soon"

            val notification = androidx.core.app.NotificationCompat.Builder(context, "pre_alarm_channel")
                .setContentTitle("$alarmTitle $remainingText")
                .setContentText("Alarm scheduled for $timeString")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
                .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPi)
                .addAction(android.R.drawable.ic_delete, "Skip Alarm", skipPi)
                .addAction(android.R.drawable.ic_menu_revert, "Remind Later", snoozePi)
                .build()

            notificationManager.notify(1002, notification) // NOTIFICATION_ID_PRE_ALARM
            Log.d("AlarmSchedulerImpl", "Immediate pre-alarm notification shown for alarm $alarmId")
        } catch (e: Exception) {
            Log.e("AlarmSchedulerImpl", "Error showing immediate pre-alarm notification", e)
        }
    }

    override suspend fun cancel(alarmId: Long): Unit = withContext(Dispatchers.Default) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.alarm.ALARM_TRIGGER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("AlarmSchedulerImpl", "Cancelled alarm $alarmId")

        // Also cancel pre-alarm
        val preAlarmIntent = Intent(context, PreAlarmReceiver::class.java).apply {
            action = PreAlarmReceiver.ACTION_PRE_ALARM_TRIGGER
        }
        val preAlarmPendingIntent = PendingIntent.getBroadcast(
            context,
            (alarmId.toInt() * 1000) + PreAlarmReceiver.PRE_ALARM_REQUEST_CODE_OFFSET,
            preAlarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(preAlarmPendingIntent)
        Log.d("AlarmSchedulerImpl", "Cancelled pre-alarm for alarm $alarmId")
    }

    override suspend fun rescheduleAll(): Unit = withContext(Dispatchers.Default) {
        val alarms = alarmRepository.getEnabled()
        val now = System.currentTimeMillis()
        Log.d("AlarmSchedulerImpl", "Rescheduling ${alarms.size} enabled alarms")
        alarms.forEach { alarm ->
            if (alarm.isEnabled) {
                val nextTrigger = AlarmTimeCalculator.resolveNextAlarmTime(
                    timeMillis = alarm.timeMillis,
                    repeatDays = alarm.repeatDays,
                    isCountdown = alarm.isCountdown,
                    countdownDurationMillis = alarm.countdownDurationMillis,
                    now = now
                )

                schedule(
                    ScheduleRequest(
                        alarmId = alarm.id,
                        triggerTimeMillis = nextTrigger,
                        isExact = true,
                        allowWhileIdle = true
                    )
                )
            }
        }
        Log.d("AlarmSchedulerImpl", "Rescheduling complete")
    }
}
