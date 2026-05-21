package com.example.alarm.feature.stopwatch

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@Composable
fun StopwatchScreen(
    viewModel: StopwatchViewModel = hiltViewModel()
) {
    val stopwatchState by viewModel.stopwatchState.collectAsStateWithLifecycle()
    var prevElapsedMillis by remember { mutableStateOf(0L) }
    val scale = remember { androidx.compose.animation.core.Animatable(1f) }

    LaunchedEffect(stopwatchState.elapsedMillis) {
        if (stopwatchState.elapsedMillis != prevElapsedMillis && stopwatchState.isRunning) {
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
            prevElapsedMillis = stopwatchState.elapsedMillis
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(Spacing.spacing4),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Stopwatch",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = Spacing.spacing7)
        )

        val hours = stopwatchState.elapsedMillis / 3600000
        val minutes = (stopwatchState.elapsedMillis % 3600000) / 60000
        val seconds = (stopwatchState.elapsedMillis % 60000) / 1000
        val millis = (stopwatchState.elapsedMillis % 1000) / 10

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
                    time = String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, millis),
                    isLarge = true
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.spacing6),
            horizontalArrangement = Arrangement.spacedBy(Spacing.spacing3)
        ) {
            if (stopwatchState.isRunning) {
                Button(
                    onClick = { viewModel.stop() },
                    modifier = Modifier
                        .weight(1f)
                        .height(Spacing.spacing8),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Stop", style = MaterialTheme.typography.labelLarge)
                }
            } else {
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
                onClick = { viewModel.recordLap() },
                enabled = stopwatchState.elapsedMillis > 0,
                modifier = Modifier
                    .weight(1f)
                    .height(Spacing.spacing8),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text("Lap", style = MaterialTheme.typography.labelLarge)
            }
        }

        Button(
            onClick = { viewModel.reset() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors()
        ) {
            Text("Reset", style = MaterialTheme.typography.labelLarge)
        }

        if (stopwatchState.laps.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.spacing6))
            Text(
                text = "Laps",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = Spacing.spacing3)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.spacing2)
            ) {
                items(stopwatchState.laps.withIndex().toList()) { (index, lapTime) ->
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

                    val lapHours = lapTime / 3600000
                    val lapMinutes = (lapTime % 3600000) / 60000
                    val lapSeconds = (lapTime % 60000) / 1000
                    val lapMillis = (lapTime % 1000) / 10

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer(alpha = alpha.value),
                        shape = RoundedCornerShape(Corners.cornerMedium),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.spacing3),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Lap ${index + 1}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                String.format("%02d:%02d:%02d.%02d", lapHours, lapMinutes, lapSeconds, lapMillis),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
