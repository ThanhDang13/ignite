package com.example.alarm.feature.stats

import com.example.alarm.data.db.entity.AlarmEventEntity
import java.util.Calendar

data class AlarmStats(
    val totalAlarmsFired: Int,
    val totalSnoozed: Int,
    val totalDismissed: Int,
    val averageSnoozeCount: Float,
    val noSnoozeStreak: Int,
    val wakeConsistencyScore: Float
)

data class WeeklyReport(
    val weekStartMillis: Long,
    val weekEndMillis: Long,
    val stats: AlarmStats,
    val insights: String,
    val generatedAt: Long = System.currentTimeMillis()
)

object StatsCalculator {

    fun calculateStats(events: List<AlarmEventEntity>): AlarmStats {
        val firedEvents = events.filter { it.eventType == "AlarmFired" }
        val snoozedEvents = events.filter { it.eventType == "AlarmSnoozed" }
        val dismissedEvents = events.filter { it.eventType == "AlarmDismissed" }

        val totalFired = firedEvents.size
        val totalSnoozed = snoozedEvents.size
        val totalDismissed = dismissedEvents.size

        val averageSnooze = if (totalFired > 0) {
            totalSnoozed.toFloat() / totalFired
        } else 0f

        val noSnoozeStreak = calculateNoSnoozeStreak(events)
        val consistencyScore = calculateConsistencyScore(firedEvents)

        return AlarmStats(
            totalAlarmsFired = totalFired,
            totalSnoozed = totalSnoozed,
            totalDismissed = totalDismissed,
            averageSnoozeCount = averageSnooze,
            noSnoozeStreak = noSnoozeStreak,
            wakeConsistencyScore = consistencyScore
        )
    }

    fun generateWeeklyReport(events: List<AlarmEventEntity>): WeeklyReport {
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

        val weekEvents = events.filter { it.timestamp in weekStart until weekEnd }
        val stats = calculateStats(weekEvents)
        val insights = generateInsights(stats)

        return WeeklyReport(
            weekStartMillis = weekStart,
            weekEndMillis = weekEnd,
            stats = stats,
            insights = insights
        )
    }

    private fun calculateNoSnoozeStreak(events: List<AlarmEventEntity>): Int {
        val sortedEvents = events.sortedByDescending { it.timestamp }
        var streak = 0

        val groupedByAlarm = sortedEvents.groupBy { it.alarmId }

        for ((_, alarmEvents) in groupedByAlarm) {
            val hasFired = alarmEvents.any { it.eventType == "AlarmFired" }
            val hasSnoozed = alarmEvents.any { it.eventType == "AlarmSnoozed" }

            if (hasFired && !hasSnoozed) {
                streak++
            } else if (hasSnoozed) {
                break
            }
        }

        return streak
    }

    private fun calculateConsistencyScore(firedEvents: List<AlarmEventEntity>): Float {
        // Need at least 3 data points for meaningful consistency calculation
        if (firedEvents.size < 3) return 0f

        // Extract wake times in minutes since midnight
        val times = firedEvents.map { event ->
            Calendar.getInstance().apply {
                timeInMillis = event.timestamp
            }.let {
                it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
            }
        }

        // Calculate average wake time
        val avgTime = times.average()

        // Calculate average absolute deviation from mean
        val avgDeviation = times.map { kotlin.math.abs(it - avgTime) }.average()

        // Define consistency thresholds (in minutes)
        // <= 5 min deviation = near perfect (95-100%)
        // 15 min deviation = good (80%)
        // 30 min deviation = moderate (60%)
        // 60 min deviation = poor (30%)
        // >= 120 min deviation = very poor (0%)

        val maxDeviation = 120.0 // 2 hours

        // Convert deviation to score (inverse relationship)
        // Lower deviation = higher score
        val normalizedDeviation = (avgDeviation / maxDeviation).coerceIn(0.0, 1.0)
        val score = (100.0 * (1.0 - normalizedDeviation)).toFloat()

        return score.coerceIn(0f, 100f)
    }

    private fun generateInsights(stats: AlarmStats): String {
        return buildString {
            if (stats.totalAlarmsFired == 0) {
                append("No alarms this week.")
                return@buildString
            }

            append("This week you woke up to ${stats.totalAlarmsFired} alarms. ")

            when {
                stats.averageSnoozeCount == 0f -> {
                    append("Great job! You didn't snooze once. ")
                }
                stats.averageSnoozeCount < 1f -> {
                    append("You snoozed occasionally. ")
                }
                stats.averageSnoozeCount < 2f -> {
                    append("You snoozed frequently. Consider adjusting your sleep schedule. ")
                }
                else -> {
                    append("You snoozed heavily. Time to rethink your wake-up routine. ")
                }
            }

            if (stats.noSnoozeStreak > 0) {
                append("Current no-snooze streak: ${stats.noSnoozeStreak} days. ")
            }

            when {
                stats.wakeConsistencyScore >= 90f -> append("Your wake-up times are very consistent!")
                stats.wakeConsistencyScore >= 70f -> append("Your wake-up times are fairly consistent.")
                else -> append("Your wake-up times vary significantly.")
            }
        }
    }
}
