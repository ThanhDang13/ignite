package com.example.alarm.feature.stopwatch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Timer
import kotlin.concurrent.timer

data class StopwatchState(
    val elapsedMillis: Long = 0,
    val isRunning: Boolean = false,
    val laps: List<Long> = emptyList()
)

@HiltViewModel
class StopwatchViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _stopwatchState = MutableStateFlow(StopwatchState())
    val stopwatchState: StateFlow<StopwatchState> = _stopwatchState.asStateFlow()

    private var timerTask: Timer? = null

    fun start() {
        if (_stopwatchState.value.isRunning) return

        _stopwatchState.value = _stopwatchState.value.copy(isRunning = true)

        timerTask = timer(initialDelay = 100, period = 100) {
            val current = _stopwatchState.value
            _stopwatchState.value = current.copy(elapsedMillis = current.elapsedMillis + 100)
        }
    }

    fun stop() {
        timerTask?.cancel()
        _stopwatchState.value = _stopwatchState.value.copy(isRunning = false)
    }

    fun reset() {
        timerTask?.cancel()
        _stopwatchState.value = StopwatchState()
    }

    fun recordLap() {
        val current = _stopwatchState.value
        val newLaps = current.laps + current.elapsedMillis
        _stopwatchState.value = current.copy(laps = newLaps)
    }

    override fun onCleared() {
        super.onCleared()
        timerTask?.cancel()
    }
}
