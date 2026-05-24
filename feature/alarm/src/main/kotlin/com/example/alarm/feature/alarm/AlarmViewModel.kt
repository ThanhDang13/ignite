package com.example.alarm.feature.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.core.common.AlarmTimeCalculator
import com.example.alarm.core.scheduler.ScheduleRequest
import com.example.alarm.core.sound.AudioPlaybackManager
import com.example.alarm.data.repository.Alarm
import com.example.alarm.data.repository.AlarmRepository
import com.example.alarm.data.repository.PreferencesRepository
import com.example.alarm.data.repository.SleepSessionRepository
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
    private val sleepSessionRepository: SleepSessionRepository,
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
        preAlarmEnabled: Boolean = true,
        vibrateEnabled: Boolean = true
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
                preAlarmEnabled = preAlarmEnabled,
                vibrateEnabled = vibrateEnabled
            )

            val alarmId = alarmRepository.create(alarm)
            val nextTrigger = AlarmTimeCalculator.resolveNextAlarmTime(
                timeMillis = timeMillis,
                repeatDays = repeatDays,
                isCountdown = isCountdown,
                countdownDurationMillis = countdownDurationMillis
            )

            scheduleAlarm(alarmId, nextTrigger)
            sleepSessionRepository.startSleepSession(alarmId)
        }
    }

    fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val oldAlarm = alarmRepository.getById(alarm.id)
            alarmRepository.update(alarm)

            if (alarm.isEnabled) {
                val nextTrigger = AlarmTimeCalculator.resolveNextAlarmTime(
                    timeMillis = alarm.timeMillis,
                    repeatDays = alarm.repeatDays,
                    isCountdown = alarm.isCountdown,
                    countdownDurationMillis = alarm.countdownDurationMillis
                )
                scheduleAlarm(alarm.id, nextTrigger)

                // Start new sleep session if alarm was previously disabled
                if (oldAlarm?.isEnabled == false) {
                    sleepSessionRepository.startSleepSession(alarm.id)
                }
            } else {
                alarmScheduler.cancel(alarm.id)
                // Cancel pending sleep session if alarm is disabled
                sleepSessionRepository.cancelSleepSession(alarm.id)
            }
        }
    }

    fun deleteAlarm(id: Long) {
        viewModelScope.launch {
            alarmRepository.delete(id)
            alarmScheduler.cancel(id)
            sleepSessionRepository.cancelSleepSession(id)
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
                sleepSessionRepository.startSleepSession(id)
            } else {
                alarmScheduler.cancel(id)
                sleepSessionRepository.cancelSleepSession(id)
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

