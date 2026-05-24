package com.example.alarm.data.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.example.alarm.core.common.AlarmTimeCalculator
import com.example.alarm.core.scheduler.ScheduleRequest
import com.example.alarm.data.repository.AlarmRepository
import com.example.alarm.data.repository.SleepSessionRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmRepository: AlarmRepository

    @Inject
    lateinit var alarmScheduler: AlarmSchedulerImpl

    @Inject
    lateinit var sleepSessionRepository: SleepSessionRepository

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "========================================")
        Log.d("AlarmReceiver", "onReceive called!")
        Log.d("AlarmReceiver", "  Action: ${intent.action}")
        Log.d("AlarmReceiver", "  Extras: ${intent.extras?.keySet()?.joinToString()}")
        Log.d("AlarmReceiver", "  Thread: ${Thread.currentThread().name}")
        Log.d("AlarmReceiver", "========================================")

        // Acquire a wake lock so the device stays awake while we hand work off
        // to the foreground service. Without this, the receiver can return and
        // the device can return to Doze before the service even starts.
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AlarmApp:AlarmReceiverWakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire(WAKE_LOCK_TIMEOUT_MS)
        }

        // goAsync() keeps the BroadcastReceiver alive past onReceive() returning.
        // Without this, work launched on a CoroutineScope is racing process death
        // and gets deferred until the app is foregrounded again — which is exactly
        // the "alarm fires when I open the app" symptom.
        val pendingResult = goAsync()

        when (intent.action) {
            "android.intent.action.BOOT_COMPLETED" -> {
                Log.d("AlarmReceiver", "Boot completed, rescheduling alarms")
                CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
                    try {
                        alarmScheduler.rescheduleAll()
                    } catch (t: Throwable) {
                        Log.e("AlarmReceiver", "Error rescheduling on boot", t)
                    } finally {
                        if (wakeLock.isHeld) wakeLock.release()
                        pendingResult.finish()
                    }
                }
            }
            "com.example.alarm.ALARM_TRIGGER" -> {
                val alarmId = intent.getLongExtra("alarmId", -1L)
                Log.d("AlarmReceiver", "Alarm trigger received for ID: $alarmId")
                if (alarmId == -1L) {
                    Log.e("AlarmReceiver", "Invalid alarm ID: $alarmId")
                    if (wakeLock.isHeld) wakeLock.release()
                    pendingResult.finish()
                    return
                }
                CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
                    try {
                        handleAlarmTrigger(context, alarmId)
                    } catch (t: Throwable) {
                        Log.e("AlarmReceiver", "Error handling alarm trigger", t)
                    } finally {
                        if (wakeLock.isHeld) wakeLock.release()
                        pendingResult.finish()
                    }
                }
            }
            else -> {
                Log.w("AlarmReceiver", "Unknown action: ${intent.action}")
                if (wakeLock.isHeld) wakeLock.release()
                pendingResult.finish()
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

        // Clear any pre-alarm notification since the main alarm is now firing
        try {
            androidx.core.app.NotificationManagerCompat.from(context)
                .cancel(com.example.alarm.core.notification.NotificationManager.NOTIFICATION_ID_PRE_ALARM)
            Log.d("AlarmReceiver", "Cleared pre-alarm notification for alarm $alarmId")
        } catch (t: Throwable) {
            Log.w("AlarmReceiver", "Failed to clear pre-alarm notification", t)
        }

        val now = System.currentTimeMillis()

        alarmRepository.logAlarmEvent(alarmId, "AlarmFired")
        Log.d("AlarmReceiver", "Logged AlarmFired event")

        // Start RingService FIRST so the alarm rings even if rescheduling
        // is slow or fails. Audio is the user-visible contract.
        Log.d("AlarmReceiver", "Starting RingService for alarm $alarmId")
        val ringIntent = Intent(context, Class.forName("com.example.alarm.feature.ring.RingService")).apply {
            putExtra("alarmId", alarmId)
            putExtra("title", alarm.title)
            putExtra("soundId", alarm.soundId)
            putExtra("vibrateEnabled", alarm.vibrateEnabled)
        }
        try {
            context.startForegroundService(ringIntent)
            Log.d("AlarmReceiver", "RingService started successfully")
        } catch (t: Throwable) {
            Log.e("AlarmReceiver", "Failed to start RingService", t)
        }

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

            // Start a new sleep session for the next occurrence
            sleepSessionRepository.startSleepSession(alarmId)
            Log.d("AlarmReceiver", "Started new sleep session for recurring alarm $alarmId")
        } else {
            Log.d("AlarmReceiver", "One-time alarm detected, auto-disabling")
            val disabledAlarm = alarm.copy(isEnabled = false)
            alarmRepository.update(disabledAlarm)
            Log.d("AlarmReceiver", "Auto-disabled one-time alarm $alarmId")
        }
    }

    companion object {
        // Match BroadcastReceiver's 10-second hard limit with a small buffer.
        private const val WAKE_LOCK_TIMEOUT_MS = 30_000L
    }
}
