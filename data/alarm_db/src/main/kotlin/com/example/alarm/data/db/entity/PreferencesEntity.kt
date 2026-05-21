package com.example.alarm.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "preferences")
data class PreferencesEntity(
    @PrimaryKey
    val id: Int = 0,
    val selectedSoundId: String = "default",
    val themeMode: String = "system", // "light", "dark", "system"
    val updatedAt: Long = System.currentTimeMillis()
)
