package com.example.alarm.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.alarm.data.db.dao.AlarmDao
import com.example.alarm.data.db.dao.AlarmEventDao
import com.example.alarm.data.db.dao.CustomSoundDao
import com.example.alarm.data.db.dao.PreferencesDao
import com.example.alarm.data.db.dao.SleepSessionDao
import com.example.alarm.data.db.dao.WeeklyReportDao
import com.example.alarm.data.db.entity.AlarmEntity
import com.example.alarm.data.db.entity.AlarmEventEntity
import com.example.alarm.data.db.entity.CustomSoundEntity
import com.example.alarm.data.db.entity.PreferencesEntity
import com.example.alarm.data.db.entity.SleepSessionEntity
import com.example.alarm.data.db.entity.WeeklyReportEntity

@Database(
    entities = [
        AlarmEntity::class,
        AlarmEventEntity::class,
        SleepSessionEntity::class,
        WeeklyReportEntity::class,
        PreferencesEntity::class,
        CustomSoundEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun alarmEventDao(): AlarmEventDao
    abstract fun sleepSessionDao(): SleepSessionDao
    abstract fun weeklyReportDao(): WeeklyReportDao
    abstract fun preferencesDao(): PreferencesDao
    abstract fun customSoundDao(): CustomSoundDao
}
