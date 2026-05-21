package com.example.alarm

import android.app.Application
import com.example.alarm.feature.stats.WeeklyReportScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AlarmApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        WeeklyReportScheduler.scheduleWeeklyReport(this)
    }
}
