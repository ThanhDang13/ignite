package com.example.alarm.feature.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alarm.core.ui.theme.Corners
import com.example.alarm.core.ui.theme.Spacing
import com.example.alarm.data.db.entity.SleepSessionEntity
import com.example.alarm.data.db.entity.WeeklyReportEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: StatsViewModel = hiltViewModel()
) {
    val dailyStats by viewModel.dailyStats.collectAsStateWithLifecycle()
    val weeklyStats by viewModel.weeklyStats.collectAsStateWithLifecycle()
    val latestReport by viewModel.latestReport.collectAsStateWithLifecycle()
    val recentSleepSessions by viewModel.recentSleepSessions.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(Spacing.spacing4),
            verticalArrangement = Arrangement.spacedBy(Spacing.spacing5)
        ) {
            item {
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                dailyStats?.let { stats ->
                    StatsCard(stats = stats)
                } ?: EmptyStatePlaceholder("No data for today")
            }

            item {
                Text(
                    text = "This Week",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                weeklyStats?.let { stats ->
                    StatsCard(stats = stats)
                } ?: EmptyStatePlaceholder("No data for this week")
            }

            item {
                Text(
                    text = "Latest Weekly Report",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                latestReport?.let { report ->
                    WeeklyReportCard(report = report)
                } ?: EmptyStatePlaceholder("No weekly report generated yet")
            }

            item {
                Button(
                    onClick = { viewModel.generateWeeklyReport() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Spacing.spacing7),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Generate Weekly Report", style = MaterialTheme.typography.labelLarge)
                }
            }

            item {
                Text(
                    text = "Recent Sleep Sessions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(recentSleepSessions) { session ->
                SleepSessionCard(session = session)
            }

            item {
                Spacer(modifier = Modifier.height(Spacing.spacing4))
            }
        }
    }
}

@Composable
fun EmptyStatePlaceholder(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Corners.cornerLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.spacing6),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatsCard(stats: AlarmStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Corners.cornerLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.spacing5),
            verticalArrangement = Arrangement.spacedBy(Spacing.spacing3)
        ) {
            StatRow("Alarms Fired", stats.totalAlarmsFired.toString())
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            StatRow("Snoozed", stats.totalSnoozed.toString())
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            StatRow("Dismissed", stats.totalDismissed.toString())
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            StatRow("Avg Snooze Count", "%.1f".format(stats.averageSnoozeCount))
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            StatRow("No-Snooze Streak", "${stats.noSnoozeStreak} days")
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            StatRow("Wake Consistency", "${(stats.wakeConsistencyScore * 100).toInt()}%")
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun WeeklyReportCard(report: WeeklyReportEntity) {
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    val weekStart = dateFormat.format(Date(report.weekStartMillis))
    val weekEnd = dateFormat.format(Date(report.weekEndMillis))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Corners.cornerLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.spacing5),
            verticalArrangement = Arrangement.spacedBy(Spacing.spacing3)
        ) {
            Text(
                text = "$weekStart - $weekEnd",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            StatRow("Alarms Fired", report.totalAlarmsFired.toString())
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            StatRow("Snoozed", report.totalSnoozed.toString())
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            StatRow("Dismissed", report.totalDismissed.toString())
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            StatRow("Avg Snooze Count", "%.1f".format(report.averageSnoozeCount))
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            StatRow("No-Snooze Streak", "${report.noSnoozeStreak} days")
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            StatRow("Wake Consistency", "${(report.wakeConsistencyScore * 100).toInt()}%")

            if (report.insights.isNotBlank()) {
                Spacer(modifier = Modifier.height(Spacing.spacing2))
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(Spacing.spacing3))
                Text(
                    text = "Insights",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = report.insights,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SleepSessionCard(session: SleepSessionEntity) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val bedtime = dateFormat.format(Date(session.bedtimeMillis))
    val wakeTime = dateFormat.format(Date(session.wakeTimeMillis))
    val durationHours = session.durationMillis / (1000 * 60 * 60).toFloat()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Corners.cornerLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.spacing4),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.spacing1)
            ) {
                Text(
                    text = "$bedtime → $wakeTime",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (session.wasManual) "Manual entry" else "Auto-tracked",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "%.1fh".format(durationHours),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
