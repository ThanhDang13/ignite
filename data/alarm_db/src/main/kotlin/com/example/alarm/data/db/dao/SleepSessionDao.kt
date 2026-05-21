package com.example.alarm.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.alarm.data.db.entity.SleepSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepSessionDao {
    @Insert
    suspend fun insert(session: SleepSessionEntity): Long

    @Query("SELECT * FROM sleep_sessions ORDER BY bedtimeMillis DESC LIMIT 1")
    suspend fun getLatestSession(): SleepSessionEntity?

    @Query("SELECT * FROM sleep_sessions WHERE bedtimeMillis >= :startTime AND bedtimeMillis <= :endTime ORDER BY bedtimeMillis DESC")
    suspend fun getSessionsBetween(startTime: Long, endTime: Long): List<SleepSessionEntity>

    @Query("SELECT AVG(durationMillis) FROM sleep_sessions WHERE bedtimeMillis >= :startTime")
    suspend fun getAverageSleepDuration(startTime: Long): Long?

    @Query("SELECT * FROM sleep_sessions ORDER BY bedtimeMillis DESC LIMIT :limit")
    fun getRecentSessionsFlow(limit: Int): Flow<List<SleepSessionEntity>>

    @Query("DELETE FROM sleep_sessions WHERE bedtimeMillis < :cutoffTime")
    suspend fun deleteOldSessions(cutoffTime: Long)
}
