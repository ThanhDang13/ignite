package com.example.alarm.core.scheduler

data class ScheduleRequest(
    val alarmId: Long,
    val triggerTimeMillis: Long,
    val isExact: Boolean = true,
    val allowWhileIdle: Boolean = true
)

interface AlarmScheduler {
    suspend fun schedule(request: ScheduleRequest)
    suspend fun cancel(alarmId: Long)
    suspend fun rescheduleAll()
}
