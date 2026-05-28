package com.example.alarm.feature.timer

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.alarm.core.notification.NotificationManager as AlarmNotificationManager
import com.example.alarm.core.sound.AudioPlaybackManager
import com.example.alarm.data.repository.PreferencesRepository
import com.example.alarm.feature.ring.RingActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Foreground service that runs a countdown timer with live notification updates.
 * Uses SystemClock.elapsedRealtime() for drift-free timing.
 */
@AndroidEntryPoint
class TimerService : Service() {

    @Inject lateinit var notificationManager: AlarmNotificationManager
    @Inject lateinit var audioPlaybackManager: AudioPlaybackManager
    @Inject lateinit var preferencesRepository: PreferencesRepository

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    private var wakeLock: PowerManager.WakeLock? = null
    private var vibrator: Vibrator? = null

    // Timer state: elapsedRealtime-based for accuracy
    private var targetEndTimeRealtime: Long = 0L
    private var pausedRemainingMillis: Long = 0L
    private var isPaused = false
    private var isCompleted = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand action=$action")

        if (intent == null) {
            Log.e(TAG, "Received null intent")
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            when (action) {
                ACTION_START -> {
                    val durationMillis = intent.getLongExtra(EXTRA_DURATION_MILLIS, 0L)
                    if (durationMillis > 0) {
                        startTimer(durationMillis)
                    } else {
                        Log.e(TAG, "Invalid duration in ACTION_START: $durationMillis")
                        stopSelf()
                    }
                }
                ACTION_PAUSE -> pauseTimer()
                ACTION_RESUME -> resumeTimer()
                ACTION_CANCEL -> cancelTimer()
                ACTION_DISMISS_ALERT -> dismissAlert()
                else -> {
                    Log.w(TAG, "Unknown action: $action")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStartCommand", e)
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    private fun startTimer(durationMillis: Long) {
        Log.d(TAG, "startTimer duration=$durationMillis")

        if (durationMillis <= 0) {
            Log.e(TAG, "Invalid duration: $durationMillis")
            stopSelf()
            return
        }

        isPaused = false
        isCompleted = false
        targetEndTimeRealtime = SystemClock.elapsedRealtime() + durationMillis
        pausedRemainingMillis = 0L

        try {
            acquireWakeLock()
            startForegroundWithNotification(durationMillis)
            startUpdateLoop()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting timer", e)
            stopSelf()
        }
    }

    private fun pauseTimer() {
        Log.d(TAG, "pauseTimer")
        if (isPaused || isCompleted) return
        isPaused = true
        pausedRemainingMillis = (targetEndTimeRealtime - SystemClock.elapsedRealtime()).coerceAtLeast(0)
        stopUpdateLoop()
        updateNotification(pausedRemainingMillis, paused = true)
        broadcastTimerState(pausedRemainingMillis, paused = true)
    }

    private fun resumeTimer() {
        Log.d(TAG, "resumeTimer")
        if (!isPaused || isCompleted) return
        isPaused = false
        targetEndTimeRealtime = SystemClock.elapsedRealtime() + pausedRemainingMillis
        pausedRemainingMillis = 0L
        startUpdateLoop()
    }

    private fun broadcastTimerState(remainingMillis: Long, paused: Boolean) {
        val intent = Intent(BROADCAST_TIMER_TICK).apply {
            putExtra(EXTRA_REMAINING_MILLIS, remainingMillis)
            putExtra(EXTRA_IS_PAUSED, paused)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun broadcastTimerReset() {
        val intent = Intent(BROADCAST_TIMER_RESET).apply {
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun cancelTimer() {
        Log.d(TAG, "cancelTimer")
        stopUpdateLoop()
        releaseWakeLock()
        try {
            NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID_TIMER)
        } catch (_: Throwable) {}
        broadcastTimerReset()
        stopSelf()
    }

    private fun dismissAlert() {
        Log.d(TAG, "dismissAlert")
        audioPlaybackManager.stopBlocking()
        stopVibration()
        stopUpdateLoop()
        releaseWakeLock()
        try {
            NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID_TIMER)
        } catch (_: Throwable) {}
        broadcastTimerReset()
        stopSelf()
    }

    private fun startUpdateLoop() {
        stopUpdateLoop()
        updateRunnable = object : Runnable {
            override fun run() {
                if (isPaused || isCompleted) return

                val now = SystemClock.elapsedRealtime()
                val remaining = (targetEndTimeRealtime - now).coerceAtLeast(0)

                if (remaining <= 0) {
                    onTimerComplete()
                } else {
                    updateNotification(remaining, paused = false)
                    broadcastTimerState(remaining, paused = false)
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.post(updateRunnable!!)
    }

    private fun stopUpdateLoop() {
        updateRunnable?.let { handler.removeCallbacks(it) }
        updateRunnable = null
    }

    private fun onTimerComplete() {
        Log.d(TAG, "onTimerComplete")
        isCompleted = true
        stopUpdateLoop()

        serviceScope.launch {
            try {
                val preferences = preferencesRepository.getPreferences()
                audioPlaybackManager.play(preferences.selectedSoundId, loop = true)
                audioPlaybackManager.rampVolume(durationMs = 3000, targetVolume = 1f)
                startVibration()
            } catch (e: Exception) {
                Log.e(TAG, "Error playing timer alert", e)
            }
        }

        showTimeUpNotification()
    }

    private fun startVibration() {
        try {
            val v = getSystemService(VIBRATOR_SERVICE) as? Vibrator ?: return
            if (!v.hasVibrator()) return
            val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(pattern, 0)
            }
            vibrator = v
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

    private fun startForegroundWithNotification(durationMillis: Long) {
        val notification = buildTimerNotification(durationMillis, paused = false)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID_TIMER,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID_TIMER, notification)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "startForeground failed", t)
        }
    }

    private fun updateNotification(remainingMillis: Long, paused: Boolean) {
        val notification = buildTimerNotification(remainingMillis, paused)
        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_TIMER, notification)
        } catch (t: Throwable) {
            Log.e(TAG, "updateNotification failed", t)
        }
    }

    private fun buildTimerNotification(remainingMillis: Long, paused: Boolean): android.app.Notification {
        val hours = (remainingMillis / 3600000).toInt()
        val minutes = ((remainingMillis % 3600000) / 60000).toInt()
        val seconds = ((remainingMillis % 60000) / 1000).toInt()
        val timeText = if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }

        val contentIntent = try {
            val intent = Intent(this, Class.forName("com.example.alarm.MainActivity")).apply {
                putExtra("navigate_to_tab", "timer")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create content intent", e)
            null
        }

        val builder = notificationManager.buildTimerNotification(
            title = if (paused) "Timer Paused" else "Timer Running",
            message = "$timeText remaining",
            contentIntent = contentIntent
        )

        if (paused) {
            val resumeIntent = Intent(this, TimerService::class.java).apply {
                action = ACTION_RESUME
            }
            val resumePi = PendingIntent.getService(
                this, 1, resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_play, "Resume", resumePi)
        } else {
            val pauseIntent = Intent(this, TimerService::class.java).apply {
                action = ACTION_PAUSE
            }
            val pausePi = PendingIntent.getService(
                this, 2, pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePi)
        }

        val cancelIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_CANCEL
        }
        val cancelPi = PendingIntent.getService(
            this, 3, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPi)

        return builder.build()
    }

    private fun showTimeUpNotification() {
        val dismissIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_DISMISS_ALERT
        }
        val dismissPi = PendingIntent.getService(
            this, 4, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, RingActivity::class.java).apply {
                putExtra("alarmId", -1L)
                putExtra("title", "Timer")
                putExtra("timeMillis", System.currentTimeMillis())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = notificationManager.buildTimerNotification(
            title = "Time's Up!",
            message = "Timer finished",
            contentIntent = contentIntent
        )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPi)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_TIMER, notification)
        } catch (t: Throwable) {
            Log.e(TAG, "showTimeUpNotification failed", t)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AlarmApp:TimerServiceWakeLock"
        ).apply {
            setReferenceCounted(false)
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

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        stopUpdateLoop()
        try { audioPlaybackManager.stopBlocking() } catch (_: Throwable) {}
        stopVibration()
        releaseWakeLock()
        try {
            NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID_TIMER)
        } catch (_: Throwable) {}
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "TimerService"
        const val NOTIFICATION_ID_TIMER = 2001

        const val ACTION_START = "com.example.alarm.timer.ACTION_START"
        const val ACTION_PAUSE = "com.example.alarm.timer.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.alarm.timer.ACTION_RESUME"
        const val ACTION_CANCEL = "com.example.alarm.timer.ACTION_CANCEL"
        const val ACTION_DISMISS_ALERT = "com.example.alarm.timer.ACTION_DISMISS_ALERT"

        const val EXTRA_DURATION_MILLIS = "durationMillis"

        const val BROADCAST_TIMER_TICK = "com.example.alarm.timer.TIMER_TICK"
        const val BROADCAST_TIMER_RESET = "com.example.alarm.timer.TIMER_RESET"
        const val EXTRA_REMAINING_MILLIS = "remainingMillis"
        const val EXTRA_IS_PAUSED = "isPaused"
    }
}
