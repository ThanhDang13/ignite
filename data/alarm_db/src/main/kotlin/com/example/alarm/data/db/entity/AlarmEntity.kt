package com.example.alarm.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val timeMillis: Long,
    val isEnabled: Boolean,
    val repeatDays: String, // JSON-encoded Set<Int> of DayOfWeek
    val isCountdown: Boolean,
    val countdownDurationMillis: Long?,
    val soundId: String,
    val snoozeMinutes: Int,
    val preAlarmEnabled: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
