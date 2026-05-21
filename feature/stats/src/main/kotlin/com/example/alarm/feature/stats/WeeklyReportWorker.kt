package com.example.alarm.feature.stats

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WeeklyReportWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val statsRepository: StatsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            statsRepository.generateWeeklyReport()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
