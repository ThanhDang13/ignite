package com.example.alarm.data.scheduler

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class PreAlarmWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val alarmId = inputData.getLong("alarmId", -1L)
        if (alarmId == -1L) return Result.failure()

        // TODO: Show pre-alarm notification with actions (dismiss/snooze)

        return Result.success()
    }
}
