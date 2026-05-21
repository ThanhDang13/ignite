package com.example.alarm.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.alarm.data.db.entity.AlarmEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmEventDao {
    @Insert
    suspend fun insert(event: AlarmEventEntity)

    @Query("SELECT * FROM alarm_events WHERE alarmId = :alarmId ORDER BY timestamp DESC")
    fun getEventsForAlarm(alarmId: Long): Flow<List<AlarmEventEntity>>

    @Query("SELECT * FROM alarm_events WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getEventsBetween(startTime: Long, endTime: Long): List<AlarmEventEntity>

    @Query("SELECT * FROM alarm_events WHERE eventType = :eventType AND timestamp >= :startTime ORDER BY timestamp DESC")
    suspend fun getEventsByType(eventType: String, startTime: Long): List<AlarmEventEntity>

    @Query("SELECT COUNT(*) FROM alarm_events WHERE alarmId = :alarmId AND eventType = 'AlarmSnoozed' AND timestamp >= :startTime")
    suspend fun getSnoozeCount(alarmId: Long, startTime: Long): Int

    @Query("DELETE FROM alarm_events WHERE timestamp < :cutoffTime")
    suspend fun deleteOldEvents(cutoffTime: Long)
}
