package com.example.alarm.feature.stats

import android.content.Context
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

object WeeklyReportScheduler {

    private const val WEEKLY_REPORT_WORK = "weekly_report_generation"

    fun scheduleWeeklyReport(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        // Calculate delay until next Sunday at midnight
        val now = Calendar.getInstance()
        val nextSunday = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If today is Sunday and time has passed, schedule for next week
            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        val delayMillis = nextSunday.timeInMillis - now.timeInMillis

        val weeklyReportRequest = PeriodicWorkRequestBuilder<WeeklyReportWorker>(
            7, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WEEKLY_REPORT_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            weeklyReportRequest
        )
    }

    fun cancelWeeklyReport(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WEEKLY_REPORT_WORK)
    }
}
