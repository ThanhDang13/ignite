package com.example.alarm.core.common

import java.util.Calendar

object AlarmTimeCalculator {

    /**
     * Unified function to resolve the next alarm trigger time.
     * Always returns the closest valid future timestamp based on current time.
     * Never returns past timestamps.
     *
     * @param timeMillis The alarm's configured time (hour + minute as milliseconds)
     * @param repeatDays Set of DayOfWeek integers (1=Sunday, 2=Monday, ..., 7=Saturday), empty = one-time
     * @param isCountdown Whether this is a countdown alarm
     * @param countdownDurationMillis Duration for countdown alarms
     * @param now Current time in milliseconds (defaults to System.currentTimeMillis())
     * @return Next trigger time in milliseconds, always in the future
     */
    fun resolveNextAlarmTime(
        timeMillis: Long,
        repeatDays: Set<Int>,
        isCountdown: Boolean,
        countdownDurationMillis: Long?,
        now: Long = System.currentTimeMillis()
    ): Long {
        // Countdown alarms: trigger at now + duration
        if (isCountdown && countdownDurationMillis != null) {
            return now + countdownDurationMillis
        }

        // Extract time-of-day from the alarm's configured timeMillis
        val alarmCal = Calendar.getInstance().apply {
            timeInMillis = timeMillis
        }
        val hourOfDay = alarmCal.get(Calendar.HOUR_OF_DAY)
        val minute = alarmCal.get(Calendar.MINUTE)

        // One-time alarm: compute from current time
        if (repeatDays.isEmpty()) {
            return resolveOneTimeAlarmTime(hourOfDay, minute, now)
        }

        // Recurring alarm: find next matching day
        return resolveRecurringAlarmTime(hourOfDay, minute, repeatDays, now)
    }

    /**
     * Resolve next trigger for a one-time alarm.
     * If target time today has passed, schedule for tomorrow.
     */
    private fun resolveOneTimeAlarmTime(hourOfDay: Int, minute: Int, now: Long): Long {
        val todayCal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If today's time hasn't passed, use it
        if (todayCal.timeInMillis > now) {
            return todayCal.timeInMillis
        }

        // Otherwise, schedule for tomorrow at the same time
        todayCal.add(Calendar.DAY_OF_YEAR, 1)
        return todayCal.timeInMillis
    }

    /**
     * Resolve next trigger for a recurring alarm.
     * Finds the nearest valid future occurrence from current time.
     */
    private fun resolveRecurringAlarmTime(
        hourOfDay: Int,
        minute: Int,
        repeatDays: Set<Int>,
        now: Long
    ): Long {
        // Try today first
        val todayCal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val todayDayOfWeek = todayCal.get(Calendar.DAY_OF_WEEK)

        // If today is a repeat day and time hasn't passed, use today
        if (repeatDays.contains(todayDayOfWeek) && todayCal.timeInMillis > now) {
            return todayCal.timeInMillis
        }

        // Search next 7 days for matching day
        for (daysAhead in 1..7) {
            val candidateCal = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, daysAhead)
                set(Calendar.HOUR_OF_DAY, hourOfDay)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val candidateDayOfWeek = candidateCal.get(Calendar.DAY_OF_WEEK)
            if (repeatDays.contains(candidateDayOfWeek)) {
                return candidateCal.timeInMillis
            }
        }

        // Fallback: should never reach here if repeatDays is non-empty
        // Schedule for next week on the first matching day
        val nextWeekCal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 7)
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return nextWeekCal.timeInMillis
    }

    /**
     * Calculate snooze trigger time.
     */
    fun calculateSnoozeTrigger(snoozeMinutes: Int): Long {
        return System.currentTimeMillis() + (snoozeMinutes * 60 * 1000)
    }

    /**
     * Calculate pre-alarm trigger time (15 minutes before main alarm).
     */
    fun calculatePreAlarmTrigger(mainAlarmTimeMillis: Long): Long {
        return mainAlarmTimeMillis - (15 * 60 * 1000)
    }

    /**
     * @deprecated Use resolveNextAlarmTime with individual parameters instead
     */
    @Deprecated("Use resolveNextAlarmTime with individual parameters instead")
    fun calculateNextTrigger(
        baseTimeMillis: Long,
        repeatDays: Set<Int>,
        isCountdown: Boolean,
        countdownDurationMillis: Long?
    ): Long {
        return resolveNextAlarmTime(baseTimeMillis, repeatDays, isCountdown, countdownDurationMillis)
    }
}
