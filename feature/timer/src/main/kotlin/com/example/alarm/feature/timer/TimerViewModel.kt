package com.example.alarm.feature.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TimerState(
    val totalMillis: Long = 0,
    val remainingMillis: Long = 0,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false
)

@HiltViewModel
class TimerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val timerTickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                TimerService.BROADCAST_TIMER_TICK -> {
                    val remaining = intent.getLongExtra(TimerService.EXTRA_REMAINING_MILLIS, 0L)
                    val paused = intent.getBooleanExtra(TimerService.EXTRA_IS_PAUSED, false)
                    val current = _timerState.value
                    _timerState.value = current.copy(
                        remainingMillis = remaining,
                        isPaused = paused
                    )
                }
                TimerService.BROADCAST_TIMER_RESET -> {
                    _timerState.value = TimerState()
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(TimerService.BROADCAST_TIMER_TICK)
            addAction(TimerService.BROADCAST_TIMER_RESET)
        }
        context.registerReceiver(timerTickReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onCleared() {
        super.onCleared()
        try {
            context.unregisterReceiver(timerTickReceiver)
        } catch (e: Exception) {
            android.util.Log.e("TimerViewModel", "Error unregistering receiver", e)
        }
    }

    fun setDuration(hours: Int, minutes: Int, seconds: Int) {
        // Validate inputs
        if (hours < 0 || minutes < 0 || seconds < 0) {
            android.util.Log.e("TimerViewModel", "Invalid negative duration values")
            return
        }
        if (hours > 23 || minutes > 59 || seconds > 59) {
            android.util.Log.e("TimerViewModel", "Duration values out of range")
            return
        }

        val millis = (hours * 3600000L) + (minutes * 60000L) + (seconds * 1000L)

        if (millis <= 0) {
            android.util.Log.e("TimerViewModel", "Total duration is zero or negative")
            return
        }
        if (millis > 86400000L) { // Max 24 hours
            android.util.Log.e("TimerViewModel", "Duration exceeds 24 hours")
            return
        }

        _timerState.value = TimerState(
            totalMillis = millis,
            remainingMillis = millis,
            isRunning = false,
            isPaused = false
        )
    }

    fun start() {
        val current = _timerState.value
        if (current.isRunning || current.remainingMillis <= 0) return

        _timerState.value = current.copy(isRunning = true, isPaused = false)

        try {
            val intent = Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_START
                putExtra(TimerService.EXTRA_DURATION_MILLIS, current.remainingMillis)
            }
            context.startForegroundService(intent)
        } catch (e: Exception) {
            android.util.Log.e("TimerViewModel", "Failed to start timer service", e)
            _timerState.value = current.copy(isRunning = false, isPaused = false)
        }
    }

    fun pause() {
        val current = _timerState.value
        if (!current.isRunning || current.isPaused) return

        _timerState.value = current.copy(isPaused = true)

        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_PAUSE
        }
        context.startService(intent)
    }

    fun resume() {
        val current = _timerState.value
        if (!current.isPaused) return

        _timerState.value = current.copy(isPaused = false)

        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_RESUME
        }
        context.startService(intent)
    }

    fun cancel() {
        _timerState.value = TimerState()

        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_CANCEL
        }
        context.startService(intent)
    }

    fun reset() {
        val current = _timerState.value
        _timerState.value = current.copy(
            remainingMillis = current.totalMillis,
            isRunning = false,
            isPaused = false
        )

        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_CANCEL
        }
        context.startService(intent)
    }

    fun clear() {
        _timerState.value = TimerState()

        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_CANCEL
        }
        context.startService(intent)
    }
}
