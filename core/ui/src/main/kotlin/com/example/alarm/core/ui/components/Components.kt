package com.example.alarm.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.scale
import com.example.alarm.core.ui.theme.Corners
import com.example.alarm.core.ui.theme.Elevations
import com.example.alarm.core.ui.theme.Spacing

// Alarm Card Component - for list items
@Composable
fun AlarmCard(
    title: String,
    time: String,
    nextTriggerTime: String = "",
    isActive: Boolean = true,
    repeatInfo: String = "",
    onToggle: (Boolean) -> Unit = {},
    onDelete: () -> Unit = {},
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Corners.cornerLarge),
        elevation = CardDefaults.cardElevation(
            defaultElevation = Elevations.elevationSmall
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.spacing4)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = Spacing.spacing3)
                ) {
                    Text(
                        text = time,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = Spacing.spacing1)
                    )
                    if (repeatInfo.isNotEmpty()) {
                        Text(
                            text = repeatInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Spacing.spacing1)
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.spacing1),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = isActive,
                        onCheckedChange = onToggle,
                        modifier = Modifier.scale(0.8f)
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            if (nextTriggerTime.isNotEmpty()) {
                Divider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(vertical = Spacing.spacing2)
                )
                Text(
                    text = "Next ring: $nextTriggerTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Time Display Component - for timer/stopwatch
@Composable
fun TimeDisplay(
    time: String,
    modifier: Modifier = Modifier,
    isLarge: Boolean = true
) {
    Text(
        text = time,
        style = if (isLarge) MaterialTheme.typography.displayLarge else MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
        maxLines = 1,
        softWrap = false
    )
}

// Empty State Component
@Composable
fun EmptyState(
    icon: ImageVector? = null,
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.spacing6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(bottom = Spacing.spacing4)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(Corners.cornerXLarge)
                    )
                    .padding(Spacing.spacing4),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = Spacing.spacing2)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.spacing4)
        )
    }
}

// Settings Item Component
@Composable
fun SettingsItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Corners.cornerMedium),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevations.elevationSmall),
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
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.spacing1)
                    )
                }
            }
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = Spacing.spacing3)
                )
            }
        }
    }
}

// Stat Card Component
@Composable
fun StatCard(
    title: String,
    stats: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Corners.cornerLarge),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevations.elevationSmall),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.spacing4)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = Spacing.spacing3)
            )
            stats.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.spacing2),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// Repeat Selection Component
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RepeatSelectionDialog(
    selectedRepeatType: String,
    selectedCustomDays: Set<Int>,
    onRepeatTypeSelected: (String) -> Unit,
    onCustomDaysSelected: (Set<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    val repeatOptions = listOf(
        "once" to "Once",
        "daily" to "Daily",
        "weekdays" to "Mon - Fri",
        "custom" to "Custom"
    )

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Repeat",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.spacing2)
            ) {
                repeatOptions.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onRepeatTypeSelected(value)
                                if (value != "custom") {
                                    onDismiss()
                                }
                            }
                            .padding(vertical = Spacing.spacing3),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = selectedRepeatType == value,
                            onClick = {
                                onRepeatTypeSelected(value)
                                if (value != "custom") {
                                    onDismiss()
                                }
                            }
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = Spacing.spacing3)
                        )
                    }
                }

                if (selectedRepeatType == "custom") {
                    Divider(modifier = Modifier.padding(vertical = Spacing.spacing3))
                    Text(
                        "Select Days",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = Spacing.spacing2)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.spacing2),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.spacing1)
                    ) {
                        listOf(
                            "S" to 1,
                            "M" to 2,
                            "T" to 3,
                            "W" to 4,
                            "T" to 5,
                            "F" to 6,
                            "S" to 7
                        ).forEach { (label, day) ->
                            FilterChip(
                                selected = selectedCustomDays.contains(day),
                                onClick = {
                                    onCustomDaysSelected(
                                        if (selectedCustomDays.contains(day)) {
                                            selectedCustomDays - day
                                        } else {
                                            selectedCustomDays + day
                                        }
                                    )
                                },
                                label = {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

// Helper function to convert repeat type and days to display text
fun getRepeatDisplayText(repeatType: String, customDays: Set<Int>): String {
    return when (repeatType) {
        "once" -> "Once"
        "daily" -> "Daily"
        "weekdays" -> "Mon - Fri"
        "custom" -> {
            if (customDays.isEmpty()) "Never" else {
                val dayLabels = mapOf(
                    1 to "S", 2 to "M", 3 to "T", 4 to "W",
                    5 to "T", 6 to "F", 7 to "S"
                )
                customDays.sorted().map { dayLabels[it] ?: "" }.joinToString(", ")
            }
        }
        else -> "Once"
    }
}
