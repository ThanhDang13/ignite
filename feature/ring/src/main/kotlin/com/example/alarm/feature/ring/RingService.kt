package com.example.alarm.feature.ring

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Vibrator
import android.util.Log
import com.example.alarm.core.common.AlarmTimeCalculator
import com.example.alarm.core.notification.NotificationManager
import com.example.alarm.core.scheduler.ScheduleRequest
import com.example.alarm.core.sound.AudioPlaybackManager
import com.example.alarm.data.repository.AlarmRepository
import com.example.alarm.data.repository.PreferencesRepository
import com.example.alarm.data.scheduler.AlarmSchedulerImpl
import com.example.alarm.feature.stats.StatsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RingService : Service() {

    @Inject
    lateinit var audioPlaybackManager: AudioPlaybackManager

    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var alarmRepository: AlarmRepository

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var alarmScheduler: AlarmSchedulerImpl

    @Inject
    lateinit var statsRepository: StatsRepository

    private var serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var alarmId: Long = -1L
    private var title: String = "Alarm"
    private var soundId: String = "default"
    private var vibrateEnabled: Boolean = true

    private var vibrator: Vibrator? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("RingService", "Service started with action: ${intent?.action}")

        when (intent?.action) {
            ACTION_DISMISS -> {
                val alarmId = intent.getLongExtra("alarmId", -1L)
                if (alarmId != -1L) {
                    this.alarmId = alarmId
                    dismiss()
                }
                return START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                val alarmId = intent.getLongExtra("alarmId", -1L)
                if (alarmId != -1L) {
                    this.alarmId = alarmId
                    snooze()
                }
                return START_NOT_STICKY
            }
            else -> {
                alarmId = intent?.getLongExtra("alarmId", -1L) ?: -1L
                title = intent?.getStringExtra("title") ?: "Alarm"
                soundId = intent?.getStringExtra("soundId") ?: "default"
                vibrateEnabled = intent?.getBooleanExtra("vibrateEnabled", true) ?: true

                vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator

                startForeground()
                playAlarm()

                return START_STICKY
            }
        }
    }

    private fun startForeground() {
        val notification = notificationManager.buildAlarmNotification(
            title,
            "Tap to dismiss"
        ).build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationManager.NOTIFICATION_ID_ALARM,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NotificationManager.NOTIFICATION_ID_ALARM, notification)
        }

        val ringIntent = Intent(this, RingActivity::class.java).apply {
            putExtra("alarmId", alarmId)
            putExtra("title", title)
            putExtra("timeMillis", System.currentTimeMillis())
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
        startActivity(ringIntent)
    }

    private fun playAlarm() {
        serviceScope.launch {
            try {
                val preferences = preferencesRepository.getPreferences()
                val soundToPlay = when (soundId) {
                    "app_default" -> preferences.selectedSoundId
                    "default" -> preferences.selectedSoundId
                    else -> soundId
                }
                audioPlaybackManager.play(soundToPlay, loop = true)
                audioPlaybackManager.rampVolume(durationMs = 3000, targetVolume = 1f)

                if (vibrateEnabled) {
                    startVibration()
                }
            } catch (e: Exception) {
                Log.e("RingService", "Error playing alarm", e)
            }
        }
    }

    private fun startVibration() {
        try {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
            if (vibrator?.hasVibrator() == true) {
                val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, 0)
                }
            }
        } catch (e: Exception) {
            Log.e("RingService", "Error starting vibration", e)
        }
    }

    fun dismiss() {
        serviceScope.launch {
            try {
                stopVibration()
                audioPlaybackManager.stop()
                alarmRepository.logAlarmEvent(alarmId, "AlarmDismissed")

                // Infer sleep session when alarm is dismissed
                val wakeTimeMillis = System.currentTimeMillis()
                statsRepository.inferSleepSession(wakeTimeMillis)
                Log.d("RingService", "Sleep session inferred for wake time: $wakeTimeMillis")

                stopSelf()
            } catch (e: Exception) {
                Log.e("RingService", "Error dismissing alarm", e)
            }
        }
    }

    fun snooze() {
        serviceScope.launch {
            try {
                stopVibration()
                audioPlaybackManager.stop()

                val alarm = alarmRepository.getById(alarmId)
                if (alarm != null) {
                    val snoozeMinutes = alarm.snoozeMinutes
                    val snoozeTrigger = AlarmTimeCalculator.calculateSnoozeTrigger(snoozeMinutes)

                    alarmScheduler.schedule(
                        ScheduleRequest(
                            alarmId = alarmId,
                            triggerTimeMillis = snoozeTrigger,
                            isExact = true,
                            allowWhileIdle = true
                        )
                    )

                    alarmRepository.logAlarmEvent(alarmId, "AlarmSnoozed")
                    Log.d("RingService", "Alarm $alarmId snoozed for $snoozeMinutes minutes")
                }

                stopSelf()
            } catch (e: Exception) {
                Log.e("RingService", "Error snoozing alarm", e)
            }
        }
    }

    private fun stopVibration() {
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e("RingService", "Error stopping vibration", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        serviceScope.launch {
            audioPlaybackManager.stop()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_DISMISS = "com.example.alarm.ACTION_DISMISS"
        const val ACTION_SNOOZE = "com.example.alarm.ACTION_SNOOZE"
    }
}
