package com.example.alarm

import androidx.lifecycle.ViewModel
import com.example.alarm.core.sound.AudioPlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val audioPlaybackManager: AudioPlaybackManager
) : ViewModel()
