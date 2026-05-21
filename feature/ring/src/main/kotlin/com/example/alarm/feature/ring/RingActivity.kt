package com.example.alarm.feature.ring

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.example.alarm.core.sound.AudioPlaybackManager
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RingActivity : ComponentActivity() {

    @Inject
    lateinit var audioPlaybackManager: AudioPlaybackManager

    private val activityScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lock screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

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
                        activityScope.launch {
                            audioPlaybackManager.stop()
                        }
                        sendActionToService(ACTION_DISMISS, alarmId)
                        finish()
                    },
                    onSnooze = {
                        activityScope.launch {
                            audioPlaybackManager.stop()
                        }
                        sendActionToService(ACTION_SNOOZE, alarmId)
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
        startService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.launch {
            audioPlaybackManager.stop()
        }
    }

    companion object {
        const val ACTION_DISMISS = "com.example.alarm.ACTION_DISMISS"
        const val ACTION_SNOOZE = "com.example.alarm.ACTION_SNOOZE"
    }
}

