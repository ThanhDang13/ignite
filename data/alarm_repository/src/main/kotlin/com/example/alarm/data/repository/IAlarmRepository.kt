package com.example.alarm.data.repository

import kotlinx.coroutines.flow.Flow

interface IAlarmRepository {
    suspend fun create(alarm: Alarm): Long
    suspend fun update(alarm: Alarm)
    suspend fun delete(id: Long)
    suspend fun getById(id: Long): Alarm?
    fun getAllFlow(): Flow<List<Alarm>>
    suspend fun getAll(): List<Alarm>
    suspend fun getEnabled(): List<Alarm>
    suspend fun logAlarmEvent(alarmId: Long, eventType: String)
}
