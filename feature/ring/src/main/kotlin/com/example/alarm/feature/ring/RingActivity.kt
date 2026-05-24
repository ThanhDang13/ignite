package com.example.alarm.feature.ring

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.example.alarm.core.sound.AudioPlaybackManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RingActivity : ComponentActivity() {

    @Inject
    lateinit var audioPlaybackManager: AudioPlaybackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        val alarmId = intent.getLongExtra("alarmId", -1L)
        val title = intent.getStringExtra("title") ?: "Alarm"
        val timeMillis = intent.getLongExtra("timeMillis", System.currentTimeMillis())
        val isTimer = title == "Timer"

        setContent {
            MaterialTheme {
                RingScreen(
                    alarmTitle = title,
                    alarmTime = timeMillis,
                    showSnooze = !isTimer,
                    onDismiss = {
                        // Don't stop audio here directly. Let RingService own
                        // the lifecycle so we don't have two paths racing to
                        // release the same MediaPlayer.
                        sendActionToService(RingService.ACTION_DISMISS, alarmId)
                        finish()
                    },
                    onSnooze = {
                        sendActionToService(RingService.ACTION_SNOOZE, alarmId)
                        finish()
                    }
                )
            }
        }
    }

    private fun sendActionToService(action: String, alarmId: Long) {
        val intent = Intent(this, RingService::class.java).apply {
            this.action = action
            putExtra("alarmId", alarmId)
        }
        // Use startForegroundService so the system permits the service to
        // start even when the screen is off / device is in Doze.
        startForegroundService(intent)
    }
}
