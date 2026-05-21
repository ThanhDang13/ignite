package com.example.alarm.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_sounds")
data class CustomSoundEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val uri: String,
    val createdAt: Long = System.currentTimeMillis()
)
