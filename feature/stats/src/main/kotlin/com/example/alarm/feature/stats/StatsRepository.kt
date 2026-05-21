package com.example.alarm.feature.stats

import com.example.alarm.data.db.AlarmDatabase
import com.example.alarm.data.db.entity.SleepSessionEntity
import com.example.alarm.data.db.entity.WeeklyReportEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsRepository @Inject constructor(
    private val database: AlarmDatabase
) {

    private val alarmEventDao = database.alarmEventDao()
    private val sleepSessionDao = database.sleepSessionDao()
    private val weeklyReportDao = database.weeklyReportDao()

    suspend fun getDailyStats(): AlarmStats {
        val now = System.currentTimeMillis()
        val startOfDay = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val events = alarmEventDao.getEventsBetween(startOfDay, now)
        return StatsCalculator.calculateStats(events)
    }

    suspend fun getWeeklyStats(): AlarmStats {
        val now = System.currentTimeMillis()
        val startOfWeek = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val events = alarmEventDao.getEventsBetween(startOfWeek, now)
        return StatsCalculator.calculateStats(events)
    }

    suspend fun generateWeeklyReport(): WeeklyReportEntity {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val weekStart = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 7)
        val weekEnd = cal.timeInMillis

        val events = alarmEventDao.getEventsBetween(weekStart, weekEnd)
        val report = StatsCalculator.generateWeeklyReport(events)

        val entity = WeeklyReportEntity(
            weekStartMillis = report.weekStartMillis,
            weekEndMillis = report.weekEndMillis,
            totalAlarmsFired = report.stats.totalAlarmsFired,
            totalSnoozed = report.stats.totalSnoozed,
            totalDismissed = report.stats.totalDismissed,
            averageSnoozeCount = report.stats.averageSnoozeCount,
            noSnoozeStreak = report.stats.noSnoozeStreak,
            wakeConsistencyScore = report.stats.wakeConsistencyScore,
            insights = report.insights
        )

        weeklyReportDao.insert(entity)
        return entity
    }

    suspend fun getLatestWeeklyReport(): WeeklyReportEntity? {
        return weeklyReportDao.getLatestReport()
    }

    fun getAllWeeklyReports(): Flow<List<WeeklyReportEntity>> {
        return weeklyReportDao.getAllReportsFlow()
    }

    suspend fun recordSleepSession(bedtimeMillis: Long, wakeTimeMillis: Long, wasManual: Boolean = false) {
        val duration = wakeTimeMillis - bedtimeMillis
        val session = SleepSessionEntity(
            bedtimeMillis = bedtimeMillis,
            wakeTimeMillis = wakeTimeMillis,
            durationMillis = duration,
            wasManual = wasManual
        )
        sleepSessionDao.insert(session)
    }

    suspend fun inferSleepSession(wakeTimeMillis: Long) {
        // Infer bedtime as 8 hours before wake time (simple heuristic)
        val estimatedBedtime = wakeTimeMillis - (8 * 60 * 60 * 1000)
        recordSleepSession(estimatedBedtime, wakeTimeMillis, wasManual = false)
    }

    suspend fun getAverageSleepDuration(daysBack: Int): Long? {
        val startTime = System.currentTimeMillis() - (daysBack * 24 * 60 * 60 * 1000L)
        return sleepSessionDao.getAverageSleepDuration(startTime)
    }

    fun getRecentSleepSessions(limit: Int): Flow<List<SleepSessionEntity>> {
        return sleepSessionDao.getRecentSessionsFlow(limit)
    }

    suspend fun cleanupOldData() {
        val cutoffTime = System.currentTimeMillis() - (90 * 24 * 60 * 60 * 1000L) // 90 days
        alarmEventDao.deleteOldEvents(cutoffTime)
        sleepSessionDao.deleteOldSessions(cutoffTime)
        weeklyReportDao.deleteOldReports(cutoffTime)
    }
}
