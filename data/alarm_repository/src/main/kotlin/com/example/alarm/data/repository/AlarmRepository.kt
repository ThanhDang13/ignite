package com.example.alarm.data.repository

import com.example.alarm.data.db.AlarmDatabase
import com.example.alarm.data.db.entity.AlarmEntity
import com.example.alarm.data.db.entity.AlarmEventEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

@Singleton
class AlarmRepository @Inject constructor(
    private val database: AlarmDatabase
) : IAlarmRepository {

    private val alarmDao = database.alarmDao()
    private val alarmEventDao = database.alarmEventDao()

    override suspend fun create(alarm: Alarm): Long {
        val entity = alarm.toEntity()
        return alarmDao.insert(entity)
    }

    override suspend fun update(alarm: Alarm) {
        val entity = alarm.toEntity()
        alarmDao.update(entity)
    }

    override suspend fun delete(id: Long) {
        alarmDao.deleteById(id)
    }

    override suspend fun getById(id: Long): Alarm? {
        return alarmDao.getById(id)?.toDomain()
    }

    fun getByIdFlow(id: Long): Flow<Alarm?> {
        return alarmDao.getByIdFlow(id).map { it?.toDomain() }
    }

    override fun getAllFlow(): Flow<List<Alarm>> {
        return alarmDao.getAllFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAll(): List<Alarm> {
        return alarmDao.getAll().map { it.toDomain() }
    }

    override suspend fun getEnabled(): List<Alarm> {
        return alarmDao.getEnabled().map { it.toDomain() }
    }

    override suspend fun logAlarmEvent(alarmId: Long, eventType: String) {
        val event = AlarmEventEntity(
            alarmId = alarmId,
            eventType = eventType,
            timestamp = System.currentTimeMillis()
        )
        alarmEventDao.insert(event)
    }

    suspend fun getEventsForAlarm(alarmId: Long): Flow<List<AlarmEventEntity>> {
        return alarmEventDao.getEventsForAlarm(alarmId)
    }

    suspend fun getEventsBetween(startTime: Long, endTime: Long): List<AlarmEventEntity> {
        return alarmEventDao.getEventsBetween(startTime, endTime)
    }

    suspend fun getSnoozeCount(alarmId: Long, startTime: Long): Int {
        return alarmEventDao.getSnoozeCount(alarmId, startTime)
    }

    private fun Alarm.toEntity(): AlarmEntity {
        return AlarmEntity(
            id = id,
            title = title,
            timeMillis = timeMillis,
            isEnabled = isEnabled,
            repeatDays = JSONArray(repeatDays.toList()).toString(),
            isCountdown = isCountdown,
            countdownDurationMillis = countdownDurationMillis,
            soundId = soundId,
            snoozeMinutes = snoozeMinutes,
            preAlarmEnabled = preAlarmEnabled
        )
    }

    private fun AlarmEntity.toDomain(): Alarm {
        val repeatSet = try {
            JSONArray(repeatDays).let { json ->
                (0 until json.length()).map { json.getInt(it) }.toSet()
            }
        } catch (e: Exception) {
            emptySet()
        }

        return Alarm(
            id = id,
            title = title,
            timeMillis = timeMillis,
            isEnabled = isEnabled,
            repeatDays = repeatSet,
            isCountdown = isCountdown,
            countdownDurationMillis = countdownDurationMillis,
            soundId = soundId,
            snoozeMinutes = snoozeMinutes,
            preAlarmEnabled = preAlarmEnabled
        )
    }
}
