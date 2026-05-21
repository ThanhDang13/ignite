package com.example.alarm.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.alarm.data.db.entity.PreferencesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PreferencesDao {
    @Query("SELECT * FROM preferences WHERE id = 0")
    fun getPreferencesFlow(): Flow<PreferencesEntity?>

    @Query("SELECT * FROM preferences WHERE id = 0")
    suspend fun getPreferences(): PreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePreferences(preferences: PreferencesEntity)
}
