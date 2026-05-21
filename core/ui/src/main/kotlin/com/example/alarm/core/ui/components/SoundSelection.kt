package com.example.alarm.core.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.alarm.core.sound.Sound
import com.example.alarm.core.ui.theme.Corners
import com.example.alarm.core.ui.theme.Spacing

@Composable
fun SoundSelectionDialog(
    sounds: List<Sound>,
    selectedSoundId: String,
    onSoundSelected: (String) -> Unit,
    onPreviewSound: (String) -> Unit,
    onAddCustomSound: ((Uri, String) -> Unit)? = null,
    onDeleteCustomSound: ((String) -> Unit)? = null,
    onDismiss: () -> Unit,
    showAppDefault: Boolean = false,
    appDefaultSoundId: String? = null
) {
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = "Custom Sound ${System.currentTimeMillis()}"
            onAddCustomSound?.invoke(it, fileName)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Select Alarm Sound",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = Spacing.spacing9 + Spacing.spacing9)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.spacing2)
                ) {
                    // Add "App Default" option if enabled
                    if (showAppDefault && appDefaultSoundId != null) {
                        item {
                            val appDefaultSound = sounds.find { it.id == appDefaultSoundId }
                            SoundSelectionItem(
                                sound = Sound(
                                    id = "app_default",
                                    name = "App Default (${appDefaultSound?.name ?: "Default"})",
                                    uri = appDefaultSound?.uri ?: Uri.EMPTY,
                                    isCustom = false
                                ),
                                isSelected = selectedSoundId == "app_default",
                                onSelect = { onSoundSelected("app_default") },
                                onPreview = { onPreviewSound(appDefaultSoundId) },
                                onDelete = null
                            )
                        }
                    }

                    items(sounds.size) { index ->
                        val sound = sounds[index]
                        SoundSelectionItem(
                            sound = sound,
                            isSelected = sound.id == selectedSoundId,
                            onSelect = { onSoundSelected(sound.id) },
                            onPreview = { onPreviewSound(sound.id) },
                            onDelete = if (sound.isCustom) {
                                { onDeleteCustomSound?.invoke(sound.id) }
                            } else null
                        )
                    }
                }

                if (onAddCustomSound != null) {
                    Spacer(modifier = Modifier.height(Spacing.spacing3))
                    Button(
                        onClick = { filePickerLauncher.launch("audio/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Spacing.spacing7),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add custom sound",
                            modifier = Modifier
                                .size(Spacing.spacing5)
                                .padding(end = Spacing.spacing1)
                        )
                        Text("Add Custom Sound", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun SoundSelectionItem(
    sound: Sound,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(Corners.cornerMedium),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.spacing3),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = Spacing.spacing2)
            ) {
                Text(
                    text = sound.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                if (sound.isCustom) {
                    Text(
                        text = "Custom",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = Spacing.spacing1)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.spacing1),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreview,
                    modifier = Modifier.size(Spacing.spacing6)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Preview sound",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Spacing.spacing5)
                    )
                }

                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(Spacing.spacing6)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete sound",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(Spacing.spacing5)
                        )
                    }
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Spacing.spacing5)
                    )
                }
            }
        }
    }
}

