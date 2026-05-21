package com.example.alarm.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weekly_reports")
data class WeeklyReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val weekStartMillis: Long,
    val weekEndMillis: Long,
    val totalAlarmsFired: Int,
    val totalSnoozed: Int,
    val totalDismissed: Int,
    val averageSnoozeCount: Float,
    val noSnoozeStreak: Int,
    val wakeConsistencyScore: Float,
    val insights: String,
    val generatedAt: Long = System.currentTimeMillis()
)
