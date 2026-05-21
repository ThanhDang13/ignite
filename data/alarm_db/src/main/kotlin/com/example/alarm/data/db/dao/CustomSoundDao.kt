package com.example.alarm.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.alarm.data.db.entity.CustomSoundEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomSoundDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sound: CustomSoundEntity)

    @Delete
    suspend fun delete(sound: CustomSoundEntity)

    @Query("SELECT * FROM custom_sounds ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<CustomSoundEntity>>

    @Query("SELECT * FROM custom_sounds ORDER BY createdAt DESC")
    suspend fun getAll(): List<CustomSoundEntity>

    @Query("DELETE FROM custom_sounds WHERE id = :soundId")
    suspend fun deleteById(soundId: String)
}
