package com.example.alarm.data.repository

import com.example.alarm.data.db.dao.PreferencesDao
import com.example.alarm.data.db.entity.PreferencesEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    private val preferencesDao: PreferencesDao
) {
    fun getPreferencesFlow(): Flow<Preferences> =
        preferencesDao.getPreferencesFlow().map { entity ->
            entity?.toPreferences() ?: Preferences()
        }

    suspend fun getPreferences(): Preferences {
        val entity = preferencesDao.getPreferences()
        return entity?.toPreferences() ?: Preferences()
    }

    suspend fun updateSelectedSound(soundId: String) {
        val current = getPreferences()
        preferencesDao.updatePreferences(
            PreferencesEntity(
                selectedSoundId = soundId,
                themeMode = current.themeMode,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateThemeMode(themeMode: String) {
        val current = getPreferences()
        preferencesDao.updatePreferences(
            PreferencesEntity(
                selectedSoundId = current.selectedSoundId,
                themeMode = themeMode,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private fun PreferencesEntity.toPreferences() = Preferences(
        selectedSoundId = selectedSoundId,
        themeMode = themeMode
    )
}

data class Preferences(
    val selectedSoundId: String = "default",
    val themeMode: String = "system" // "light", "dark", "system"
)
