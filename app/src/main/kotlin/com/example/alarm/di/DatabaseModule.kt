package com.example.alarm.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.alarm.core.scheduler.AlarmScheduler
import com.example.alarm.core.sound.AudioPlaybackManager
import com.example.alarm.data.db.AlarmDatabase
import com.example.alarm.data.repository.AlarmRepository
import com.example.alarm.data.repository.PreferencesRepository
import com.example.alarm.data.scheduler.AlarmSchedulerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add new columns to sleep_sessions table
            database.execSQL("ALTER TABLE sleep_sessions ADD COLUMN alarmId INTEGER NOT NULL DEFAULT -1")
            database.execSQL("ALTER TABLE sleep_sessions ADD COLUMN sessionStartMillis INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE sleep_sessions ADD COLUMN sessionEndMillis INTEGER")
            database.execSQL("ALTER TABLE sleep_sessions ADD COLUMN isLegacy INTEGER NOT NULL DEFAULT 1")

            // Migrate existing data: copy bedtimeMillis to sessionStartMillis, wakeTimeMillis to sessionEndMillis
            database.execSQL("UPDATE sleep_sessions SET sessionStartMillis = bedtimeMillis, sessionEndMillis = wakeTimeMillis WHERE sessionStartMillis = 0")
        }
    }

    @Singleton
    @Provides
    fun provideAlarmDatabase(
        @ApplicationContext context: Context
    ): AlarmDatabase {
        return Room.databaseBuilder(
            context,
            AlarmDatabase::class.java,
            "alarm_database"
        )
        .addMigrations(MIGRATION_6_7)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Singleton
    @Provides
    fun provideAlarmRepository(database: AlarmDatabase): AlarmRepository {
        return AlarmRepository(database)
    }

    @Singleton
    @Provides
    fun providePreferencesRepository(database: AlarmDatabase): PreferencesRepository {
        return PreferencesRepository(database.preferencesDao())
    }

    @Singleton
    @Provides
    fun provideAlarmScheduler(
        @ApplicationContext context: Context,
        repository: AlarmRepository
    ): AlarmScheduler {
        return AlarmSchedulerImpl(context, repository)
    }

    @Singleton
    @Provides
    fun provideAudioPlaybackManager(
        @ApplicationContext context: Context,
        database: AlarmDatabase
    ): AudioPlaybackManager {
        return AudioPlaybackManager(context, database)
    }
}
