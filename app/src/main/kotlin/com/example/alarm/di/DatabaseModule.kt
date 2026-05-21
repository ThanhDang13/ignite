package com.example.alarm.di

import android.content.Context
import androidx.room.Room
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
