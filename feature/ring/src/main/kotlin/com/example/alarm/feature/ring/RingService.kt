package com.example.alarm.feature.ring

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.alarm.core.common.AlarmTimeCalculator
import com.example.alarm.core.notification.NotificationManager
import com.example.alarm.core.scheduler.ScheduleRequest
import com.example.alarm.core.sound.AudioPlaybackManager
import com.example.alarm.data.repository.AlarmRepository
import com.example.alarm.data.repository.PreferencesRepository
import com.example.alarm.data.repository.SleepSessionRepository
import com.example.alarm.data.scheduler.AlarmSchedulerImpl
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class RingService : Service() {

    @Inject lateinit var audioPlaybackManager: AudioPlaybackManager
    @Inject lateinit var notificationManager: NotificationManager
    @Inject lateinit var alarmRepository: AlarmRepository
    @Inject lateinit var preferencesRepository: PreferencesRepository
    @Inject lateinit var alarmScheduler: AlarmSchedulerImpl
    @Inject lateinit var sleepSessionRepository: SleepSessionRepository

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var alarmId: Long = -1L
    private var title: String = "Alarm"
    private var soundId: String = "default"
    private var vibrateEnabled: Boolean = true

    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var hasStarted = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand action=$action hasStarted=$hasStarted")

        // The system can re-deliver intents after the process is killed; if we
        // get a Dismiss/Snooze before we ever started ringing (e.g. notification
        // tapped after the process died), we still need to enter the foreground
        // briefly to stop properly without crashing on Android 12+.
        ensureForeground(intent)

        return when (action) {
            ACTION_DISMISS -> {
                val incomingId = intent.getLongExtra("alarmId", -1L)
                if (incomingId != -1L) alarmId = incomingId
                dismiss()
                START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                val incomingId = intent.getLongExtra("alarmId", -1L)
                if (incomingId != -1L) alarmId = incomingId
                snooze()
                START_NOT_STICKY
            }
            else -> {
                if (intent != null) {
                    alarmId = intent.getLongExtra("alarmId", -1L)
                    title = intent.getStringExtra("title") ?: "Alarm"
                    soundId = intent.getStringExtra("soundId") ?: "default"
                    vibrateEnabled = intent.getBooleanExtra("vibrateEnabled", true)
                }

                acquireWakeLockIfNeeded()
                vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
                openRingActivity()
                playAlarm()
                hasStarted = true
                START_STICKY
            }
        }
    }

    /**
     * On Android 12+, calling startForegroundService() obligates us to call
     * startForeground() within ~5s on every entry, even if the action is just
     * dismiss/snooze. Build a stable notification up front.
     */
    private fun ensureForeground(intent: Intent?) {
        val incomingId = intent?.getLongExtra("alarmId", -1L) ?: -1L
        val effectiveId = if (incomingId != -1L) incomingId else alarmId
        val effectiveTitle = intent?.getStringExtra("title") ?: title

        val contentIntent = PendingIntent.getActivity(
            this,
            effectiveId.toInt(),
            Intent(this, RingActivity::class.java).apply {
                putExtra("alarmId", effectiveId)
                putExtra("title", effectiveTitle)
                putExtra("timeMillis", System.currentTimeMillis())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = notificationManager.buildAlarmNotification(
            title = effectiveTitle,
            message = "Tap to open",
            alarmId = effectiveId,
            contentIntent = contentIntent
        ).build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NotificationManager.NOTIFICATION_ID_ALARM,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NotificationManager.NOTIFICATION_ID_ALARM, notification)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "startForeground failed", t)
        }
    }

    private fun acquireWakeLockIfNeeded() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AlarmApp:RingServiceWakeLock"
        ).apply {
            setReferenceCounted(false)
            // Cap at 10 minutes — well past any reasonable ring-time, but
            // ensures we don't hold the CPU forever if something goes wrong.
            acquire(10 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (t: Throwable) {
            Log.w(TAG, "wakeLock release failed", t)
        }
        wakeLock = null
    }

    private fun openRingActivity() {
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
        try {
            startActivity(ringIntent)
        } catch (t: Throwable) {
            Log.e(TAG, "openRingActivity failed", t)
        }
    }

    private fun playAlarm() {
        serviceScope.launch {
            try {
                val preferences = preferencesRepository.getPreferences()
                val soundToPlay = when (soundId) {
                    "app_default", "default" -> preferences.selectedSoundId
                    else -> soundId
                }
                audioPlaybackManager.play(soundToPlay, loop = true)
                audioPlaybackManager.rampVolume(durationMs = 3000, targetVolume = 1f)

                if (vibrateEnabled) {
                    startVibration()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing alarm", e)
            }
        }
    }

    private fun startVibration() {
        try {
            val v = vibrator ?: getSystemService(VIBRATOR_SERVICE) as? Vibrator ?: return
            if (!v.hasVibrator()) return
            val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting vibration", e)
        }
    }

    private fun stopVibration() {
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping vibration", e)
        }
    }

    private fun dismiss() {
        Log.d(TAG, "dismiss() alarmId=$alarmId")
        // Stop audio and vibration immediately on the calling thread so the
        // user gets silence the moment they tap Dismiss — don't wait for the
        // coroutine to be scheduled.
        stopVibration()
        audioPlaybackManager.stopBlocking()

        serviceScope.launch {
            try {
                if (alarmId != -1L) {
                    alarmRepository.logAlarmEvent(alarmId, "AlarmDismissed")
                    sleepSessionRepository.completeSleepSession(alarmId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in dismiss persistence", e)
            } finally {
                fullStop()
            }
        }
    }

    private fun snooze() {
        Log.d(TAG, "snooze() alarmId=$alarmId")
        stopVibration()
        audioPlaybackManager.stopBlocking()

        serviceScope.launch {
            try {
                if (alarmId != -1L) {
                    val alarm = alarmRepository.getById(alarmId)
                    if (alarm != null) {
                        val snoozeTrigger = AlarmTimeCalculator.calculateSnoozeTrigger(alarm.snoozeMinutes)
                        alarmScheduler.schedule(
                            ScheduleRequest(
                                alarmId = alarmId,
                                triggerTimeMillis = snoozeTrigger,
                                isExact = true,
                                allowWhileIdle = true
                            )
                        )
                        alarmRepository.logAlarmEvent(alarmId, "AlarmSnoozed")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in snooze", e)
            } finally {
                fullStop()
            }
        }
    }

    private fun fullStop() {
        try {
            NotificationManagerCompat.from(this).cancel(NotificationManager.NOTIFICATION_ID_ALARM)
        } catch (_: Throwable) {}
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Throwable) {}
        releaseWakeLock()
        stopSelf()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "onTaskRemoved — releasing audio")
        // User swiped the app away. Make sure we don't leave a MediaPlayer
        // ringing in a dying process.
        runBlocking {
            try { audioPlaybackManager.stop() } catch (_: Throwable) {}
        }
        stopVibration()
        fullStop()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        // Belt-and-braces cleanup: every code path eventually lands here.
        try { audioPlaybackManager.stopBlocking() } catch (_: Throwable) {}
        stopVibration()
        releaseWakeLock()
        try {
            NotificationManagerCompat.from(this).cancel(NotificationManager.NOTIFICATION_ID_ALARM)
        } catch (_: Throwable) {}
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "RingService"
        const val ACTION_DISMISS = "com.example.alarm.ACTION_DISMISS"
        const val ACTION_SNOOZE = "com.example.alarm.ACTION_SNOOZE"
    }
}
