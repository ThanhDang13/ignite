package com.example.alarm.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.alarm.data.db.entity.WeeklyReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyReportDao {
    @Insert
    suspend fun insert(report: WeeklyReportEntity): Long

    @Query("SELECT * FROM weekly_reports ORDER BY weekStartMillis DESC LIMIT 1")
    suspend fun getLatestReport(): WeeklyReportEntity?

    @Query("SELECT * FROM weekly_reports ORDER BY weekStartMillis DESC")
    fun getAllReportsFlow(): Flow<List<WeeklyReportEntity>>

    @Query("SELECT * FROM weekly_reports WHERE weekStartMillis >= :startTime ORDER BY weekStartMillis DESC")
    suspend fun getReportsSince(startTime: Long): List<WeeklyReportEntity>

    @Query("DELETE FROM weekly_reports WHERE weekStartMillis < :cutoffTime")
    suspend fun deleteOldReports(cutoffTime: Long)
}
