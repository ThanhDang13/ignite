package com.example.alarm.feature.alarm

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alarm.core.sound.AudioPlaybackManager
import com.example.alarm.core.ui.theme.Corners
import com.example.alarm.core.ui.theme.Spacing
import com.example.alarm.core.ui.components.TimeWheelPicker
import com.example.alarm.core.ui.components.SoundSelectionDialog
import com.example.alarm.data.repository.Alarm
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditAlarmScreen(
    alarmId: Long? = null,
    viewModel: AlarmViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    var title by remember { mutableStateOf("Alarm") }
    var selectedHour by remember { mutableStateOf(7) }
    var selectedMinute by remember { mutableStateOf(0) }
    var repeatDays by remember { mutableStateOf(setOf<Int>()) }
    var repeatType by remember { mutableStateOf("once") }
    var isCountdown by remember { mutableStateOf(false) }
    var countdownMinutes by remember { mutableStateOf(30) }
    var preAlarmEnabled by remember { mutableStateOf(true) }
    var snoozeMinutes by remember { mutableStateOf(10) }
    var selectedSoundId by remember { mutableStateOf("default") }
    var vibrateEnabled by remember { mutableStateOf(true) }
    var showSoundDialog by remember { mutableStateOf(false) }
    var showRepeatDialog by remember { mutableStateOf(false) }
    var appDefaultSoundId by remember { mutableStateOf("default") }

    val scope = rememberCoroutineScope()

    // Get the injected AudioPlaybackManager singleton from the ViewModel's scope
    val audioPlaybackManager = remember {
        // Access the singleton through the app's DI container
        viewModel.audioPlaybackManager
    }

    val availableSounds by remember {
        derivedStateOf {
            audioPlaybackManager.getAvailableSounds().toList()
        }
    }

    // Load app default sound preference
    LaunchedEffect(Unit) {
        scope.launch {
            val prefs = viewModel.preferencesRepository.getPreferences()
            appDefaultSoundId = prefs.selectedSoundId
        }
    }

    // Load alarm data when editing
    LaunchedEffect(alarmId) {
        if (alarmId != null) {
            viewModel.getAlarmById(alarmId).collect { alarm ->
                if (alarm != null) {
                    title = alarm.title
                    val calendar = Calendar.getInstance().apply { timeInMillis = alarm.timeMillis }
                    selectedHour = calendar.get(Calendar.HOUR_OF_DAY)
                    selectedMinute = calendar.get(Calendar.MINUTE)
                    repeatDays = alarm.repeatDays
                    // Determine repeat type from repeatDays
                    repeatType = when {
                        repeatDays.isEmpty() -> "once"
                        repeatDays == setOf(2, 3, 4, 5, 6) -> "weekdays"
                        repeatDays.size == 7 -> "daily"
                        else -> "custom"
                    }
                    isCountdown = alarm.isCountdown
                    countdownMinutes = if (alarm.countdownDurationMillis != null) {
                        (alarm.countdownDurationMillis!! / 60 / 1000).toInt()
                    } else {
                        30
                    }
                    preAlarmEnabled = alarm.preAlarmEnabled
                    snoozeMinutes = alarm.snoozeMinutes
                    selectedSoundId = alarm.soundId
                    vibrateEnabled = alarm.vibrateEnabled
                }
            }
        }
    }

    // Stop preview sound when dialog closes
    DisposableEffect(showSoundDialog) {
        onDispose {
            if (!showSoundDialog) {
                scope.launch {
                    audioPlaybackManager.stop()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (alarmId == null) "Create Alarm" else "Edit Alarm") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.spacing4)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.spacing6),
                shape = RoundedCornerShape(Corners.cornerLarge),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.spacing5)
                ) {
                    Text(
                        "Alarm Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = Spacing.spacing3)
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Alarm Title") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge
                    )
                }
            }

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
                        .padding(Spacing.spacing5),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(bottom = Spacing.spacing4)
                    )

                    TimeWheelPicker(
                        selectedHour = selectedHour,
                        selectedMinute = selectedMinute,
                        selectedSecond = null,
                        onTimeChanged = { h, m, _ ->
                            selectedHour = h
                            selectedMinute = m
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.spacing3)
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
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.spacing6),
                shape = RoundedCornerShape(Corners.cornerLarge),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.spacing5)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRepeatDialog = true }
                            .padding(Spacing.spacing2),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Repeat",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                com.example.alarm.core.ui.components.getRepeatDisplayText(repeatType, repeatDays),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = Spacing.spacing1)
                            )
                        }
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowForward,
                            contentDescription = "Edit repeat",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (showRepeatDialog) {
                com.example.alarm.core.ui.components.RepeatSelectionDialog(
                    selectedRepeatType = repeatType,
                    selectedCustomDays = repeatDays,
                    onRepeatTypeSelected = { newType ->
                        repeatType = newType
                        // Update repeatDays based on selected type
                        repeatDays = when (newType) {
                            "once" -> setOf()
                            "daily" -> (1..7).toSet()
                            "weekdays" -> setOf(2, 3, 4, 5, 6)
                            "custom" -> repeatDays
                            else -> setOf()
                        }
                    },
                    onCustomDaysSelected = { days ->
                        repeatDays = days
                    },
                    onDismiss = { showRepeatDialog = false }
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.spacing6),
                shape = RoundedCornerShape(Corners.cornerLarge),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.spacing5)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Countdown Mode",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = isCountdown,
                            onCheckedChange = { isCountdown = it }
                        )
                    }

                    if (isCountdown) {
                        Spacer(modifier = Modifier.height(Spacing.spacing3))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Duration: $countdownMinutes min",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.spacing2)
                            ) {
                                Button(
                                    onClick = { if (countdownMinutes > 1) countdownMinutes-- },
                                    modifier = Modifier.size(40.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiary
                                    )
                                ) {
                                    Text("−", style = MaterialTheme.typography.headlineSmall)
                                }
                                Button(
                                    onClick = { countdownMinutes++ },
                                    modifier = Modifier.size(40.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiary
                                    )
                                ) {
                                    Text("+", style = MaterialTheme.typography.headlineSmall)
                                }
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.spacing6),
                shape = RoundedCornerShape(Corners.cornerLarge),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.spacing5)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Pre-Alarm",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "15 minutes before",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preAlarmEnabled,
                            onCheckedChange = { preAlarmEnabled = it }
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.spacing6),
                shape = RoundedCornerShape(Corners.cornerLarge),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.spacing5)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Vibrate",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = vibrateEnabled,
                            onCheckedChange = { vibrateEnabled = it }
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.spacing6),
                shape = RoundedCornerShape(Corners.cornerLarge),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.spacing5)
                ) {
                    Text(
                        "Snooze Duration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = Spacing.spacing4)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.spacing3),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { if (snoozeMinutes > 1) snoozeMinutes-- },
                            modifier = Modifier.size(56.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("−", style = MaterialTheme.typography.headlineSmall)
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = Spacing.spacing4),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "$snoozeMinutes",
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "minutes",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { snoozeMinutes++ },
                            modifier = Modifier.size(56.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("+", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.spacing6),
                shape = RoundedCornerShape(Corners.cornerLarge),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.spacing5)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Alarm Sound",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                availableSounds.find { it.id == selectedSoundId }?.name ?: "Default",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = Spacing.spacing1)
                            )
                        }
                        Button(
                            onClick = { showSoundDialog = true },
                            modifier = Modifier.padding(start = Spacing.spacing2)
                        ) {
                            Text("Select", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            if (showSoundDialog) {
                SoundSelectionDialog(
                    sounds = availableSounds,
                    selectedSoundId = selectedSoundId,
                    onSoundSelected = { soundId ->
                        selectedSoundId = soundId
                    },
                    onPreviewSound = { soundId ->
                        scope.launch {
                            audioPlaybackManager.preview(soundId)
                        }
                    },
                    onAddCustomSound = { uri, fileName ->
                        val sound = audioPlaybackManager.addCustomSound(fileName, uri)
                        selectedSoundId = sound.id
                    },
                    onDeleteCustomSound = { soundId ->
                        audioPlaybackManager.removeCustomSound(soundId)
                    },
                    onDismiss = {
                        showSoundDialog = false
                        // Stop preview sound when dialog closes
                        scope.launch {
                            audioPlaybackManager.stop()
                        }
                    },
                    showAppDefault = true,
                    appDefaultSoundId = appDefaultSoundId
                )
            }

            Button(
                onClick = {
                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, selectedHour)
                        set(Calendar.MINUTE, selectedMinute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    if (alarmId == null) {
                        viewModel.createAlarm(
                            title = title,
                            timeMillis = calendar.timeInMillis,
                            repeatDays = repeatDays,
                            isCountdown = isCountdown,
                            countdownDurationMillis = if (isCountdown) countdownMinutes * 60 * 1000L else null,
                            snoozeMinutes = snoozeMinutes,
                            preAlarmEnabled = preAlarmEnabled,
                            soundId = selectedSoundId,
                            vibrateEnabled = vibrateEnabled
                        )
                    } else {
                        viewModel.updateAlarm(
                            Alarm(
                                id = alarmId,
                                title = title,
                                timeMillis = calendar.timeInMillis,
                                isEnabled = true,
                                repeatDays = repeatDays,
                                isCountdown = isCountdown,
                                countdownDurationMillis = if (isCountdown) countdownMinutes * 60 * 1000L else null,
                                soundId = selectedSoundId,
                                snoozeMinutes = snoozeMinutes,
                                preAlarmEnabled = preAlarmEnabled,
                                vibrateEnabled = vibrateEnabled
                            )
                        )
                    }
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Spacing.spacing8),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Save Alarm", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(Spacing.spacing4))
        }
    }
}
