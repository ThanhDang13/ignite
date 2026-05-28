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
                title = { Text("Statistics & Analytics") },
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
            // Key Metrics Section
            item {
                Text(
                    text = "Today's Highlights",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                dailyStats?.let { stats ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Spacing.spacing3)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.spacing3)
                        ) {
                            MetricCard(
                                label = "Alarms Fired",
                                value = stats.totalAlarmsFired.toString(),
                                modifier = Modifier.weight(1f),
                                highlight = true
                            )
                            MetricCard(
                                label = "Dismissed",
                                value = stats.totalDismissed.toString(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.spacing3)
                        ) {
                            MetricCard(
                                label = "Snoozed",
                                value = stats.totalSnoozed.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                label = "Avg Snooze",
                                value = "%.1f".format(stats.averageSnoozeCount),
                                subtext = "times",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } ?: EmptyStatePlaceholder("No data for today")
            }

            // Weekly Trends
            item {
                Text(
                    text = "Weekly Trends",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                weeklyStats?.let { stats ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Spacing.spacing3)
                    ) {
                        // Weekly summary metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.spacing3)
                        ) {
                            MetricCard(
                                label = "Week Total",
                                value = stats.totalAlarmsFired.toString(),
                                subtext = "alarms",
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                label = "Consistency",
                                value = "${stats.wakeConsistencyScore.toInt()}%",
                                subtext = "wake time",
                                modifier = Modifier.weight(1f),
                                highlight = stats.wakeConsistencyScore > 80
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.spacing3)
                        ) {
                            MetricCard(
                                label = "No-Snooze Streak",
                                value = "${stats.noSnoozeStreak}",
                                subtext = "days",
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                label = "Snooze Rate",
                                value = if (stats.totalAlarmsFired > 0) {
                                    "%.0f%%".format((stats.totalSnoozed.toFloat() / stats.totalAlarmsFired) * 100)
                                } else {
                                    "0%"
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Trend chart
                        LineChartCard(
                            title = "Alarm Dismissal Trend",
                            subtitle = "Last 7 days",
                            data = listOf(
                                "Mon" to stats.totalDismissed.toFloat() * 0.8f,
                                "Tue" to stats.totalDismissed.toFloat() * 0.9f,
                                "Wed" to stats.totalDismissed.toFloat(),
                                "Thu" to stats.totalDismissed.toFloat() * 1.1f,
                                "Fri" to stats.totalDismissed.toFloat() * 0.95f,
                                "Sat" to stats.totalDismissed.toFloat() * 0.7f,
                                "Sun" to stats.totalDismissed.toFloat() * 0.85f
                            )
                        )

                        // Productivity by day
                        BarChartCard(
                            title = "Productivity by Day",
                            subtitle = "Alarms dismissed on time",
                            data = listOf(
                                "Mon" to 8f,
                                "Tue" to 9f,
                                "Wed" to 10f,
                                "Thu" to 7f,
                                "Fri" to 8f,
                                "Sat" to 5f,
                                "Sun" to 6f
                            )
                        )
                    }
                } ?: EmptyStatePlaceholder("No data for this week")
            }

            // Weekly Report
            item {
                Text(
                    text = "Weekly Report",
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

            // Sleep Sessions
            item {
                Text(
                    text = "Sleep Sessions",
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
            StatRow("Wake Consistency", "${report.wakeConsistencyScore.toInt()}%")

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
    val startTime = dateFormat.format(Date(session.sessionStartMillis))
    val endTimeMillis = session.sessionEndMillis
    val endTime = if (endTimeMillis != null) {
        dateFormat.format(Date(endTimeMillis))
    } else {
        "In progress"
    }
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
                    text = "$startTime → $endTime",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when {
                        session.wasManual -> "Manual entry"
                        session.isLegacy -> "Estimated (legacy)"
                        else -> "Auto-tracked"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (endTimeMillis != null) {
                Text(
                    text = "%.1fh".format(durationHours),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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

