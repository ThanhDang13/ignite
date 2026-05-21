package com.example.alarm.feature.timer

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.alarm.core.sound.AudioPlaybackManager
import com.example.alarm.data.repository.PreferencesRepository
import com.example.alarm.feature.ring.RingActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Timer
import kotlin.concurrent.timer

data class TimerState(
    val totalMillis: Long = 0,
    val remainingMillis: Long = 0,
    val isRunning: Boolean = false
)

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val audioPlaybackManager: AudioPlaybackManager,
    private val preferencesRepository: PreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var timerTask: Timer? = null
    private val viewModelScope = CoroutineScope(Dispatchers.Main + Job())

    fun setDuration(hours: Int, minutes: Int, seconds: Int) {
        val millis = (hours * 3600000L) + (minutes * 60000L) + (seconds * 1000L)
        _timerState.value = TimerState(
            totalMillis = millis,
            remainingMillis = millis,
            isRunning = false
        )
    }

    fun start() {
        if (_timerState.value.isRunning || _timerState.value.remainingMillis <= 0) return

        _timerState.value = _timerState.value.copy(isRunning = true)

        timerTask = timer(initialDelay = 100, period = 100) {
            val current = _timerState.value
            val newRemaining = (current.remainingMillis - 100).coerceAtLeast(0)

            _timerState.value = current.copy(remainingMillis = newRemaining)

            if (newRemaining <= 0) {
                timerTask?.cancel()
                _timerState.value = current.copy(isRunning = false)
                onTimerComplete()
            }
        }
    }

    private fun onTimerComplete() {
        viewModelScope.launch {
            try {
                val preferences = preferencesRepository.getPreferences()

                // Show full-screen ring UI like alarms
                val ringIntent = Intent(context, RingActivity::class.java).apply {
                    putExtra("alarmId", -1L) // Timer ID
                    putExtra("title", "Timer")
                    putExtra("soundId", preferences.selectedSoundId)
                    putExtra("timeMillis", System.currentTimeMillis())
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                context.startActivity(ringIntent)

                // Play sound
                audioPlaybackManager.play(preferences.selectedSoundId, loop = true)
                audioPlaybackManager.rampVolume(durationMs = 3000, targetVolume = 1f)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun pause() {
        timerTask?.cancel()
        _timerState.value = _timerState.value.copy(isRunning = false)
    }

    fun reset() {
        timerTask?.cancel()
        _timerState.value = _timerState.value.copy(
            remainingMillis = _timerState.value.totalMillis,
            isRunning = false
        )
    }

    fun clear() {
        timerTask?.cancel()
        _timerState.value = TimerState()
    }

    fun stopSound() {
        viewModelScope.launch {
            audioPlaybackManager.stop()
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerTask?.cancel()
        viewModelScope.launch {
            audioPlaybackManager.stop()
        }
    }
}
