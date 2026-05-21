package com.example.alarm

import androidx.lifecycle.ViewModel
import com.example.alarm.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val themeMode = preferencesRepository.getPreferencesFlow().map { it.themeMode }

    suspend fun setThemeMode(mode: String) {
        preferencesRepository.updateThemeMode(mode)
    }
}
