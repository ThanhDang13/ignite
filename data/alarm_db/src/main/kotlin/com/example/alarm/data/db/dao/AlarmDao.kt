package com.example.alarm.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.alarm.data.db.entity.AlarmEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Insert
    suspend fun insert(alarm: AlarmEntity): Long

    @Update
    suspend fun update(alarm: AlarmEntity)

    @Delete
    suspend fun delete(alarm: AlarmEntity)

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getById(id: Long): AlarmEntity?

    @Query("SELECT * FROM alarms WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<AlarmEntity?>

    @Query("SELECT * FROM alarms ORDER BY timeMillis ASC")
    fun getAllFlow(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms ORDER BY timeMillis ASC")
    suspend fun getAll(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE isEnabled = 1 ORDER BY timeMillis ASC")
    suspend fun getEnabled(): List<AlarmEntity>

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteById(id: Long)
}
