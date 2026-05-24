package com.example.alarm.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.alarm.data.db.entity.SleepSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepSessionDao {
    @Insert
    suspend fun insert(session: SleepSessionEntity): Long

    @Update
    suspend fun update(session: SleepSessionEntity)

    @Query("SELECT * FROM sleep_sessions WHERE alarmId = :alarmId AND sessionEndMillis IS NULL ORDER BY sessionStartMillis DESC LIMIT 1")
    suspend fun getPendingSessionForAlarm(alarmId: Long): SleepSessionEntity?

    @Query("SELECT * FROM sleep_sessions ORDER BY sessionStartMillis DESC LIMIT 1")
    suspend fun getLatestSession(): SleepSessionEntity?

    @Query("SELECT * FROM sleep_sessions WHERE sessionStartMillis >= :startTime AND sessionStartMillis <= :endTime ORDER BY sessionStartMillis DESC")
    suspend fun getSessionsBetween(startTime: Long, endTime: Long): List<SleepSessionEntity>

    @Query("SELECT AVG(durationMillis) FROM sleep_sessions WHERE sessionStartMillis >= :startTime AND sessionEndMillis IS NOT NULL")
    suspend fun getAverageSleepDuration(startTime: Long): Long?

    @Query("SELECT * FROM sleep_sessions WHERE sessionEndMillis IS NOT NULL ORDER BY sessionStartMillis DESC LIMIT :limit")
    fun getRecentSessionsFlow(limit: Int): Flow<List<SleepSessionEntity>>

    @Query("DELETE FROM sleep_sessions WHERE sessionStartMillis < :cutoffTime")
    suspend fun deleteOldSessions(cutoffTime: Long)

    @Query("DELETE FROM sleep_sessions WHERE alarmId = :alarmId AND sessionEndMillis IS NULL")
    suspend fun cancelPendingSessionsForAlarm(alarmId: Long)
}
