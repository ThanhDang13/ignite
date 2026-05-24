package com.example.alarm.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_sessions")
data class SleepSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val alarmId: Long,
    val sessionStartMillis: Long,
    val sessionEndMillis: Long?,
    val durationMillis: Long,
    val wasManual: Boolean = false,
    val isLegacy: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    // Legacy fields for backward compatibility
    @Deprecated("Use sessionStartMillis instead")
    val bedtimeMillis: Long = sessionStartMillis,
    @Deprecated("Use sessionEndMillis instead")
    val wakeTimeMillis: Long = sessionEndMillis ?: sessionStartMillis
)
