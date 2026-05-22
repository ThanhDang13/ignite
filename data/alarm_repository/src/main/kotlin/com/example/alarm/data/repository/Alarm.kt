package com.example.alarm.data.repository

data class Alarm(
    val id: Long,
    val title: String,
    val timeMillis: Long,
    val isEnabled: Boolean,
    val repeatDays: Set<Int>,
    val isCountdown: Boolean,
    val countdownDurationMillis: Long?,
    val soundId: String,
    val snoozeMinutes: Int,
    val preAlarmEnabled: Boolean,
    val vibrateEnabled: Boolean = true
)
