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

            if (preAlarmTimeMillis <= now) {
                Log.d("AlarmSchedulerImpl", "Pre-alarm time is in the past, skipping")
                return
            }

            val delayMillis = preAlarmTimeMillis - now
            val workData = Data.Builder()
                .putLong("alarmId", alarmId)
                .putString("alarmTitle", alarm.title)
                .build()

            val preAlarmWork = OneTimeWorkRequestBuilder<PreAlarmWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(workData)
                .addTag("pre_alarm_$alarmId")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "pre_alarm_$alarmId",
                ExistingWorkPolicy.REPLACE,
                preAlarmWork
            )

            Log.d("AlarmSchedulerImpl", "Pre-alarm scheduled for alarm $alarmId in ${delayMillis / 1000} seconds")
        } catch (e: Exception) {
            Log.e("AlarmSchedulerImpl", "Error scheduling pre-alarm for $alarmId", e)
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
