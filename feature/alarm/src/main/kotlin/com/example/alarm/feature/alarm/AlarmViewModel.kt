package com.example.alarm.feature.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.core.common.AlarmTimeCalculator
import com.example.alarm.core.scheduler.ScheduleRequest
import com.example.alarm.core.sound.AudioPlaybackManager
import com.example.alarm.data.repository.Alarm
import com.example.alarm.data.repository.AlarmRepository
import com.example.alarm.data.repository.PreferencesRepository
import com.example.alarm.data.scheduler.AlarmSchedulerImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmSchedulerImpl,
    val audioPlaybackManager: AudioPlaybackManager,
    val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val alarms: StateFlow<List<Alarm>> = alarmRepository.getAllFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun getAlarmById(id: Long) = alarmRepository.getByIdFlow(id)

    fun createAlarm(
        title: String,
        timeMillis: Long,
        repeatDays: Set<Int> = emptySet(),
        isCountdown: Boolean = false,
        countdownDurationMillis: Long? = null,
        soundId: String = "default",
        snoozeMinutes: Int = 10,
        preAlarmEnabled: Boolean = true
    ) {
        viewModelScope.launch {
            val alarm = Alarm(
                id = 0,
                title = title,
                timeMillis = timeMillis,
                isEnabled = true,
                repeatDays = repeatDays,
                isCountdown = isCountdown,
                countdownDurationMillis = countdownDurationMillis,
                soundId = soundId,
                snoozeMinutes = snoozeMinutes,
                preAlarmEnabled = preAlarmEnabled
            )

            val alarmId = alarmRepository.create(alarm)
            val nextTrigger = AlarmTimeCalculator.resolveNextAlarmTime(
                timeMillis = timeMillis,
                repeatDays = repeatDays,
                isCountdown = isCountdown,
                countdownDurationMillis = countdownDurationMillis
            )

            scheduleAlarm(alarmId, nextTrigger)
        }
    }

    fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            alarmRepository.update(alarm)
            if (alarm.isEnabled) {
                val nextTrigger = AlarmTimeCalculator.resolveNextAlarmTime(
                    timeMillis = alarm.timeMillis,
                    repeatDays = alarm.repeatDays,
                    isCountdown = alarm.isCountdown,
                    countdownDurationMillis = alarm.countdownDurationMillis
                )
                scheduleAlarm(alarm.id, nextTrigger)
            } else {
                alarmScheduler.cancel(alarm.id)
            }
        }
    }

    fun deleteAlarm(id: Long) {
        viewModelScope.launch {
            alarmRepository.delete(id)
            alarmScheduler.cancel(id)
        }
    }

    fun toggleAlarm(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            val alarm = alarmRepository.getById(id) ?: return@launch
            val updated = alarm.copy(isEnabled = enabled)
            alarmRepository.update(updated)

            if (enabled) {
                val nextTrigger = AlarmTimeCalculator.resolveNextAlarmTime(
                    timeMillis = updated.timeMillis,
                    repeatDays = updated.repeatDays,
                    isCountdown = updated.isCountdown,
                    countdownDurationMillis = updated.countdownDurationMillis
                )
                scheduleAlarm(id, nextTrigger)
            } else {
                alarmScheduler.cancel(id)
            }
        }
    }

    private suspend fun scheduleAlarm(alarmId: Long, timeMillis: Long) {
        alarmScheduler.schedule(
            ScheduleRequest(
                alarmId = alarmId,
                triggerTimeMillis = timeMillis,
                isExact = true,
                allowWhileIdle = true
            )
        )
    }
}

