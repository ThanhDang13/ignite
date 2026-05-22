package com.example.alarm.feature.alarm

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alarm.core.common.AlarmTimeCalculator
import com.example.alarm.data.repository.Alarm
import com.example.alarm.core.ui.components.AlarmCard
import com.example.alarm.core.ui.components.EmptyState
import com.example.alarm.core.ui.theme.Spacing
import com.example.alarm.core.ui.theme.AnimationDurations
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    viewModel: AlarmViewModel = hiltViewModel(),
    onNavigateToCreate: () -> Unit = {},
    onNavigateToEdit: (Long) -> Unit = {}
) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Alarm")
            }
        }
    ) { padding ->
        if (alarms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = "No Alarms",
                    message = "Create your first alarm to get started"
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Text(
                    text = "Alarms",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(Spacing.spacing4)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Spacing.spacing4),
                    verticalArrangement = Arrangement.spacedBy(Spacing.spacing3)
                ) {
                    items(alarms) { alarm ->
                        var isVisible by remember { mutableStateOf(false) }
                        val alpha = remember { androidx.compose.animation.core.Animatable(0f) }

                        LaunchedEffect(Unit) {
                            isVisible = true
                            alpha.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = AnimationDurations.durationMedium.inWholeMilliseconds.toInt(),
                                    easing = EaseInOutCubic
                                )
                            )
                        }

                        AlarmListItem(
                            alarm = alarm,
                            onToggle = { enabled ->
                                viewModel.toggleAlarm(alarm.id, enabled)
                            },
                            onDelete = {
                                viewModel.deleteAlarm(alarm.id)
                            },
                            onClick = {
                                onNavigateToEdit(alarm.id)
                            },
                            modifier = Modifier.graphicsLayer(alpha = alpha.value)
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(Spacing.spacing6))
                    }
                }
            }
        }
    }
}

@Composable
fun AlarmListItem(
    alarm: Alarm,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val alarmTime = timeFormat.format(Date(alarm.timeMillis))
    val nextTriggerTime = if (alarm.isEnabled) {
        calculateNextTriggerTime(alarm)
    } else {
        "" // Don't show trigger time when alarm is disabled
    }

    AlarmCard(
        title = alarm.title,
        time = alarmTime,
        nextTriggerTime = nextTriggerTime,
        isActive = alarm.isEnabled,
        repeatInfo = formatRepeatDays(alarm.repeatDays),
        onToggle = onToggle,
        onDelete = onDelete,
        onClick = onClick,
        modifier = modifier
    )
}

private fun calculateNextTriggerTime(alarm: Alarm): String {
    val now = System.currentTimeMillis()

    if (alarm.repeatDays.isEmpty()) {
        // One-time alarm - use unified function to get actual scheduled time
        val nextTrigger = AlarmTimeCalculator.resolveNextAlarmTime(
            timeMillis = alarm.timeMillis,
            repeatDays = alarm.repeatDays,
            isCountdown = alarm.isCountdown,
            countdownDurationMillis = alarm.countdownDurationMillis,
            now = now
        )
        val dateFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
        return dateFormat.format(Date(nextTrigger))
    }

    // Recurring alarm - use unified function to find next occurrence
    val nextTrigger = AlarmTimeCalculator.resolveNextAlarmTime(
        timeMillis = alarm.timeMillis,
        repeatDays = alarm.repeatDays,
        isCountdown = alarm.isCountdown,
        countdownDurationMillis = alarm.countdownDurationMillis,
        now = now
    )
    val dayNames = mapOf(
        1 to "Sunday", 2 to "Monday", 3 to "Tuesday", 4 to "Wednesday",
        5 to "Thursday", 6 to "Friday", 7 to "Saturday"
    )

    val nextCal = Calendar.getInstance().apply {
        timeInMillis = nextTrigger
    }
    val dayOfWeek = nextCal.get(Calendar.DAY_OF_WEEK)
    val dayName = dayNames[dayOfWeek] ?: "Unknown"
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeStr = timeFormat.format(Date(nextTrigger))

    return "$dayName $timeStr"
}

private fun formatRepeatDays(repeatDays: Set<Int>): String {
    if (repeatDays.isEmpty()) return "One time"

    val dayNames = mapOf(
        1 to "Sun", 2 to "Mon", 3 to "Tue", 4 to "Wed",
        5 to "Thu", 6 to "Fri", 7 to "Sat"
    )

    return repeatDays.sorted().joinToString(", ") { dayNames[it] ?: "" }
}
