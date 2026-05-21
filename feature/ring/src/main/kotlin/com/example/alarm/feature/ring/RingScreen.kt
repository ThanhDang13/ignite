package com.example.alarm.feature.ring

import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.text.style.TextAlign
import com.example.alarm.core.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RingScreen(
    alarmTitle: String,
    alarmTime: Long,
    showSnooze: Boolean = true,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val currentTime = timeFormat.format(Date(alarmTime))

    val scale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        while (true) {
            scale.animateTo(
                targetValue = 1.05f,
                animationSpec = tween(durationMillis = 600, easing = EaseInOutCubic)
            )
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600, easing = EaseInOutCubic)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.spacing6)
        ) {
            Spacer(modifier = Modifier.height(Spacing.spacing6))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer(scaleX = scale.value, scaleY = scale.value)
            ) {
                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(Spacing.spacing5))

                Text(
                    text = alarmTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.spacing4)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Spacing.spacing8),
                    shape = RoundedCornerShape(Spacing.spacing3),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        "DISMISS",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (showSnooze) {
                    OutlinedButton(
                        onClick = onSnooze,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Spacing.spacing8),
                        shape = RoundedCornerShape(Spacing.spacing3),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            "SNOOZE",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.spacing4))
        }
    }
}
