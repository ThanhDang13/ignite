package com.example.alarm.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.data.db.entity.SleepSessionEntity
import com.example.alarm.data.db.entity.WeeklyReportEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val _dailyStats = MutableStateFlow<AlarmStats?>(null)
    val dailyStats: StateFlow<AlarmStats?> = _dailyStats.asStateFlow()

    private val _weeklyStats = MutableStateFlow<AlarmStats?>(null)
    val weeklyStats: StateFlow<AlarmStats?> = _weeklyStats.asStateFlow()

    private val _latestReport = MutableStateFlow<WeeklyReportEntity?>(null)
    val latestReport: StateFlow<WeeklyReportEntity?> = _latestReport.asStateFlow()

    private val _recentSleepSessions = MutableStateFlow<List<SleepSessionEntity>>(emptyList())
    val recentSleepSessions: StateFlow<List<SleepSessionEntity>> = _recentSleepSessions.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _dailyStats.value = statsRepository.getDailyStats()
            _weeklyStats.value = statsRepository.getWeeklyStats()
            _latestReport.value = statsRepository.getLatestWeeklyReport()
        }

        viewModelScope.launch {
            statsRepository.getRecentSleepSessions(7).collect { sessions ->
                _recentSleepSessions.value = sessions
            }
        }
    }

    fun generateWeeklyReport() {
        viewModelScope.launch {
            val report = statsRepository.generateWeeklyReport()
            _latestReport.value = report
        }
    }

    fun recordManualSleep(bedtimeMillis: Long, wakeTimeMillis: Long) {
        viewModelScope.launch {
            statsRepository.recordSleepSession(bedtimeMillis, wakeTimeMillis, wasManual = true)
            loadStats()
        }
    }
}
