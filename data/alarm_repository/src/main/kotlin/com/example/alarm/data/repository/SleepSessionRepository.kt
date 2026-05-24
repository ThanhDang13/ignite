package com.example.alarm.data.repository

import com.example.alarm.data.db.AlarmDatabase
import com.example.alarm.data.db.entity.SleepSessionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepSessionRepository @Inject constructor(
    private val database: AlarmDatabase
) {

    private val sleepSessionDao = database.sleepSessionDao()

    /**
     * Start a new sleep session when an alarm is created or enabled.
     * This marks the beginning of the sleep tracking period.
     */
    suspend fun startSleepSession(alarmId: Long) {
        // Cancel any existing pending sessions for this alarm
        sleepSessionDao.cancelPendingSessionsForAlarm(alarmId)

        val session = SleepSessionEntity(
            alarmId = alarmId,
            sessionStartMillis = System.currentTimeMillis(),
            sessionEndMillis = null,
            durationMillis = 0,
            wasManual = false,
            isLegacy = false
        )
        sleepSessionDao.insert(session)
    }

    /**
     * Complete a sleep session when the alarm is dismissed or completed.
     * This marks the end of the sleep tracking period.
     */
    suspend fun completeSleepSession(alarmId: Long) {
        val pendingSession = sleepSessionDao.getPendingSessionForAlarm(alarmId)
        if (pendingSession != null) {
            val endTime = System.currentTimeMillis()
            val duration = endTime - pendingSession.sessionStartMillis

            val completedSession = pendingSession.copy(
                sessionEndMillis = endTime,
                durationMillis = duration
            )
            sleepSessionDao.update(completedSession)
        }
    }

    /**
     * Cancel a pending sleep session when an alarm is disabled or deleted
     * before it rings.
     */
    suspend fun cancelSleepSession(alarmId: Long) {
        sleepSessionDao.cancelPendingSessionsForAlarm(alarmId)
    }

    /**
     * Record a manual sleep session (for backward compatibility or manual entry).
     */
    suspend fun recordSleepSession(bedtimeMillis: Long, wakeTimeMillis: Long, wasManual: Boolean = false) {
        val duration = wakeTimeMillis - bedtimeMillis
        val session = SleepSessionEntity(
            alarmId = -1L,
            sessionStartMillis = bedtimeMillis,
            sessionEndMillis = wakeTimeMillis,
            durationMillis = duration,
            wasManual = wasManual,
            isLegacy = true
        )
        sleepSessionDao.insert(session)
    }

    suspend fun getAverageSleepDuration(daysBack: Int): Long? {
        val startTime = System.currentTimeMillis() - (daysBack * 24 * 60 * 60 * 1000L)
        return sleepSessionDao.getAverageSleepDuration(startTime)
    }

    fun getRecentSleepSessions(limit: Int): Flow<List<SleepSessionEntity>> {
        return sleepSessionDao.getRecentSessionsFlow(limit)
    }

    suspend fun cleanupOldSessions() {
        val cutoffTime = System.currentTimeMillis() - (90 * 24 * 60 * 60 * 1000L) // 90 days
        sleepSessionDao.deleteOldSessions(cutoffTime)
    }
}
