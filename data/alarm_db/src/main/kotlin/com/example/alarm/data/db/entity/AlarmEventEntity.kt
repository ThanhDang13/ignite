package com.example.alarm.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarm_events")
data class AlarmEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val alarmId: Long,
    val eventType: String, // AlarmFired, AlarmDismissed, AlarmSnoozed
    val timestamp: Long = System.currentTimeMillis(),
    val snoozeCount: Int = 0
)
