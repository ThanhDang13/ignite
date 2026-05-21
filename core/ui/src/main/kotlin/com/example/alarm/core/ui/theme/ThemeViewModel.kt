package com.example.alarm.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import com.example.alarm.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val themeMode = preferencesRepository.getPreferencesFlow().map { it.themeMode }

    suspend fun setThemeMode(mode: String) {
        preferencesRepository.updateThemeMode(mode)
    }
}
