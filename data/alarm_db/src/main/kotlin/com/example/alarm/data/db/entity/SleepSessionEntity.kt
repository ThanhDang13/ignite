package com.example.alarm.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_sessions")
data class SleepSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bedtimeMillis: Long,
    val wakeTimeMillis: Long,
    val durationMillis: Long,
    val wasManual: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
