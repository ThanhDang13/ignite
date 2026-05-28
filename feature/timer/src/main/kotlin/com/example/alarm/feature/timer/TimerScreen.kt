package com.example.alarm.feature.timer

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alarm.core.ui.theme.Corners
import com.example.alarm.core.ui.theme.Spacing
import com.example.alarm.core.ui.theme.AnimationDurations
import com.example.alarm.core.ui.components.TimeDisplay
import com.example.alarm.core.ui.components.TimeWheelPicker

@Composable
fun TimerScreen(
    viewModel: TimerViewModel = hiltViewModel()
) {
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    var selectedHours by remember { mutableStateOf(0) }
    var selectedMinutes by remember { mutableStateOf(1) }
    var selectedSeconds by remember { mutableStateOf(0) }
    var prevRemainingMillis by remember { mutableStateOf(0L) }
    val scale = remember { androidx.compose.animation.core.Animatable(1f) }

    // Watch for timer reset and update input state
    LaunchedEffect(timerState.totalMillis) {
        if (timerState.totalMillis == 0L) {
            selectedHours = 0
            selectedMinutes = 1
            selectedSeconds = 0
            prevRemainingMillis = 0L
        }
    }

    LaunchedEffect(timerState.remainingMillis) {
        if (timerState.remainingMillis != prevRemainingMillis && timerState.isRunning) {
            scale.animateTo(
                targetValue = 0.95f,
                animationSpec = tween(
                    durationMillis = AnimationDurations.durationShort.inWholeMilliseconds.toInt(),
                    easing = EaseInOutCubic
                )
            )
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = AnimationDurations.durationShort.inWholeMilliseconds.toInt(),
                    easing = EaseInOutCubic
                )
            )
            prevRemainingMillis = timerState.remainingMillis
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(Spacing.spacing4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Timer",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = Spacing.spacing7)
        )

        if (timerState.totalMillis == 0L) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.spacing6),
                shape = RoundedCornerShape(Corners.cornerLarge),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.spacing6),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Set Duration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(bottom = Spacing.spacing4)
                    )

                    TimeWheelPicker(
                        selectedHour = selectedHours,
                        selectedMinute = selectedMinutes,
                        selectedSecond = selectedSeconds,
                        onTimeChanged = { h, m, s ->
                            selectedHours = h
                            selectedMinutes = m
                            selectedSeconds = s ?: 0
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.spacing4)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.spacing2)
                    ) {
                        Text(
                            text = "Hours",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.width(Spacing.spacing4))
                        Text(
                            text = "Minutes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.width(Spacing.spacing4))
                        Text(
                            text = "Seconds",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.spacing4))

                    Button(
                        onClick = {
                            if (selectedHours > 0 || selectedMinutes > 0 || selectedSeconds > 0) {
                                viewModel.setDuration(selectedHours, selectedMinutes, selectedSeconds)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Spacing.spacing8),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        enabled = selectedHours > 0 || selectedMinutes > 0 || selectedSeconds > 0
                    ) {
                        Text(
                            "Start Timer",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        } else {
            val hours = timerState.remainingMillis / 3600000
            val minutes = (timerState.remainingMillis % 3600000) / 60000
            val seconds = (timerState.remainingMillis % 60000) / 1000

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.spacing7)
                    .graphicsLayer(scaleX = scale.value, scaleY = scale.value),
                shape = RoundedCornerShape(Corners.cornerXLarge),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.spacing6)
                        .padding(vertical = Spacing.spacing7),
                    contentAlignment = Alignment.Center
                ) {
                    TimeDisplay(
                        time = String.format("%02d:%02d:%02d", hours, minutes, seconds),
                        isLarge = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.spacing6))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.spacing6),
                horizontalArrangement = Arrangement.spacedBy(Spacing.spacing3)
            ) {
                if (timerState.isRunning && !timerState.isPaused) {
                    Button(
                        onClick = { viewModel.pause() },
                        modifier = Modifier
                            .weight(1f)
                            .height(Spacing.spacing8),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Pause", style = MaterialTheme.typography.labelLarge)
                    }
                } else if (timerState.isPaused) {
                    Button(
                        onClick = { viewModel.resume() },
                        modifier = Modifier
                            .weight(1f)
                            .height(Spacing.spacing8),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Resume", style = MaterialTheme.typography.labelLarge)
                    }
                } else if (timerState.remainingMillis > 0) {
                    Button(
                        onClick = { viewModel.start() },
                        modifier = Modifier
                            .weight(1f)
                            .height(Spacing.spacing8),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Start", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Button(
                    onClick = {
                        if (timerState.isRunning || timerState.isPaused) {
                            viewModel.cancel()
                        } else {
                            viewModel.reset()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(Spacing.spacing8),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text(
                        if (timerState.isRunning || timerState.isPaused) "Cancel" else "Reset",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    viewModel.clear()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Set New Timer", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
