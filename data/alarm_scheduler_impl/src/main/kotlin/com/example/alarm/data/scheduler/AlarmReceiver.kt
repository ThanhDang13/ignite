package com.example.alarm.data.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.alarm.core.common.AlarmTimeCalculator
import com.example.alarm.core.scheduler.ScheduleRequest
import com.example.alarm.data.repository.AlarmRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmRepository: AlarmRepository

    @Inject
    lateinit var alarmScheduler: AlarmSchedulerImpl

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "========================================")
        Log.d("AlarmReceiver", "onReceive called!")
        Log.d("AlarmReceiver", "  Action: ${intent.action}")
        Log.d("AlarmReceiver", "  Extras: ${intent.extras?.keySet()?.joinToString()}")
        Log.d("AlarmReceiver", "  Thread: ${Thread.currentThread().name}")
        Log.d("AlarmReceiver", "========================================")

        when (intent.action) {
            "android.intent.action.BOOT_COMPLETED" -> {
                Log.d("AlarmReceiver", "Boot completed, rescheduling alarms")
                CoroutineScope(Dispatchers.Default).launch {
                    alarmScheduler.rescheduleAll()
                }
            }
            "com.example.alarm.ALARM_TRIGGER" -> {
                val alarmId = intent.getLongExtra("alarmId", -1L)
                Log.d("AlarmReceiver", "Alarm trigger received for ID: $alarmId")
                if (alarmId != -1L) {
                    CoroutineScope(Dispatchers.Default).launch {
                        handleAlarmTrigger(context, alarmId)
                    }
                } else {
                    Log.e("AlarmReceiver", "Invalid alarm ID: $alarmId")
                }
            }
            else -> {
                Log.w("AlarmReceiver", "Unknown action: ${intent.action}")
            }
        }
    }

    private suspend fun handleAlarmTrigger(context: Context, alarmId: Long) {
        Log.d("AlarmReceiver", "handleAlarmTrigger started for alarm $alarmId")

        val alarm = alarmRepository.getById(alarmId)
        if (alarm == null) {
            Log.e("AlarmReceiver", "Alarm $alarmId not found in database!")
            return
        }

        Log.d("AlarmReceiver", "Alarm found: ${alarm.title}, enabled=${alarm.isEnabled}, repeatDays=${alarm.repeatDays}")

        val now = System.currentTimeMillis()

        alarmRepository.logAlarmEvent(alarmId, "AlarmFired")
        Log.d("AlarmReceiver", "Logged AlarmFired event")

        // If this is a repeating alarm, schedule the next occurrence
        if (alarm.repeatDays.isNotEmpty()) {
            Log.d("AlarmReceiver", "Repeating alarm detected, scheduling next occurrence")
            val nextTrigger = AlarmTimeCalculator.resolveNextAlarmTime(
                timeMillis = alarm.timeMillis,
                repeatDays = alarm.repeatDays,
                isCountdown = alarm.isCountdown,
                countdownDurationMillis = alarm.countdownDurationMillis,
                now = now
            )

            alarmScheduler.schedule(
                ScheduleRequest(
                    alarmId = alarmId,
                    triggerTimeMillis = nextTrigger,
                    isExact = true,
                    allowWhileIdle = true
                )
            )
            Log.d("AlarmReceiver", "Rescheduled repeating alarm $alarmId for $nextTrigger")
        } else {
            // One-time alarm: auto-disable after firing
            Log.d("AlarmReceiver", "One-time alarm detected, auto-disabling")
            val disabledAlarm = alarm.copy(isEnabled = false)
            alarmRepository.update(disabledAlarm)
            Log.d("AlarmReceiver", "Auto-disabled one-time alarm $alarmId")
        }

        Log.d("AlarmReceiver", "Starting RingService for alarm $alarmId")
        val ringIntent = Intent(context, Class.forName("com.example.alarm.feature.ring.RingService")).apply {
            putExtra("alarmId", alarmId)
            putExtra("title", alarm.title)
            putExtra("soundId", alarm.soundId)
            putExtra("vibrateEnabled", alarm.vibrateEnabled)
        }
        context.startForegroundService(ringIntent)
        Log.d("AlarmReceiver", "RingService started successfully")
    }
}


