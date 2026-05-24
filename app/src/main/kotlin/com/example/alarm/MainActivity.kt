package com.example.alarm

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alarm.core.common.AlarmPermissionChecker
import com.example.alarm.core.sound.AudioPlaybackManager
import com.example.alarm.feature.alarm.AlarmListScreen
import com.example.alarm.feature.alarm.CreateEditAlarmScreen
import com.example.alarm.feature.stats.StatsScreen
import com.example.alarm.feature.timer.TimerScreen
import com.example.alarm.feature.stopwatch.StopwatchScreen
import com.example.alarm.core.ui.theme.AlarmAppTheme
import com.example.alarm.core.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle(initialValue = "system")

            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }

            AlarmAppTheme(darkTheme = isDarkTheme, context = this) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AlarmApp(themeViewModel)
                }
            }
        }
    }
}

enum class BottomTab(val label: String, val icon: ImageVector) {
    Alarms("Alarms", Icons.Default.Home),
    Timer("Timer", Icons.Default.Favorite),
    Stopwatch("Stopwatch", Icons.Default.Info),
    Stats("Stats", Icons.Default.Settings),
    Settings("Settings", Icons.Default.Settings)
}

@Composable
fun AlarmApp(themeViewModel: ThemeViewModel) {
    var currentTab by remember { mutableStateOf(BottomTab.Alarms) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.AlarmList) }
    var editAlarmId by remember { mutableStateOf<Long?>(null) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Track permission/restriction status. Recompute on resume because the
    // user may flip system settings while the app is backgrounded.
    var permissionStatus by remember {
        mutableStateOf(AlarmPermissionChecker.status(context))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionStatus = AlarmPermissionChecker.status(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        permissionStatus = AlarmPermissionChecker.status(context)
    }

    // First-launch: prompt for POST_NOTIFICATIONS on Android 13+. Without
    // this, the user has no way to know alarms will fail silently.
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !permissionStatus.hasNotificationPermission
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomTab.values().forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        selected = currentTab == tab,
                        onClick = {
                            currentTab = tab
                            currentScreen = Screen.AlarmList
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!permissionStatus.allGranted) {
                PermissionWarningBanner(
                    status = permissionStatus,
                    onRequestNotifications = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onOpenExactAlarmSettings = {
                        AlarmPermissionChecker.exactAlarmSettingsIntent(context)?.let {
                            context.startActivity(it)
                        }
                    },
                    onOpenBatterySettings = {
                        context.startActivity(
                            AlarmPermissionChecker.ignoreBatteryOptimizationIntent(context)
                        )
                    },
                    onOpenNotificationSettings = {
                        context.startActivity(
                            AlarmPermissionChecker.appNotificationSettingsIntent(context)
                        )
                    }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (currentTab) {
                    BottomTab.Alarms -> {
                        when (currentScreen) {
                            Screen.AlarmList -> {
                                AlarmListScreen(
                                    onNavigateToCreate = {
                                        editAlarmId = null
                                        currentScreen = Screen.CreateEdit
                                    },
                                    onNavigateToEdit = { alarmId ->
                                        editAlarmId = alarmId
                                        currentScreen = Screen.CreateEdit
                                    }
                                )
                            }
                            Screen.CreateEdit -> {
                                CreateEditAlarmScreen(
                                    alarmId = editAlarmId,
                                    onNavigateBack = {
                                        currentScreen = Screen.AlarmList
                                    }
                                )
                            }
                            Screen.Stats -> {}
                        }
                    }
                    BottomTab.Timer -> TimerScreen()
                    BottomTab.Stopwatch -> StopwatchScreen()
                    BottomTab.Stats -> StatsScreen(
                        onNavigateBack = { currentTab = BottomTab.Alarms }
                    )
                    BottomTab.Settings -> SettingsPlaceholder(
                        themeViewModel = themeViewModel,
                        permissionStatus = permissionStatus,
                        onRequestNotifications = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                context.startActivity(
                                    AlarmPermissionChecker.appNotificationSettingsIntent(context)
                                )
                            }
                        },
                        onOpenExactAlarmSettings = {
                            AlarmPermissionChecker.exactAlarmSettingsIntent(context)?.let {
                                context.startActivity(it)
                            }
                        },
                        onOpenBatterySettings = {
                            context.startActivity(
                                AlarmPermissionChecker.ignoreBatteryOptimizationIntent(context)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionWarningBanner(
    status: AlarmPermissionChecker.Status,
    onRequestNotifications: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    "Alarms may not work reliably",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            if (!status.hasNotificationPermission) {
                Text(
                    "Notifications are blocked. Without them, alarm rings will not appear when the app is closed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                TextButton(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        onRequestNotifications()
                    } else {
                        onOpenNotificationSettings()
                    }
                }) { Text("Enable notifications") }
            }
            if (!status.canScheduleExactAlarms) {
                Text(
                    "Exact alarms are disabled. Alarms will fire late or not at all.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                TextButton(onClick = onOpenExactAlarmSettings) {
                    Text("Allow exact alarms")
                }
            }
            if (!status.isIgnoringBatteryOptimizations) {
                Text(
                    "Battery optimization is on. Android may pause this app and miss alarms after long idle periods.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                TextButton(onClick = onOpenBatterySettings) {
                    Text("Disable battery optimization")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPlaceholder(
    themeViewModel: ThemeViewModel,
    permissionStatus: AlarmPermissionChecker.Status,
    onRequestNotifications: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val audioPlaybackManager = settingsViewModel.audioPlaybackManager
    var showSoundDialog by remember { mutableStateOf(false) }
    var soundsRefreshTrigger by remember { mutableStateOf(0) }
    val currentThemeMode by themeViewModel.themeMode.collectAsStateWithLifecycle(initialValue = "system")
    var selectedSound by remember { mutableStateOf("default") }
    val scope = rememberCoroutineScope()
    val preferencesRepository = themeViewModel.preferencesRepository

    val availableSounds by remember(soundsRefreshTrigger) {
        derivedStateOf {
            audioPlaybackManager.getAvailableSounds().toList()
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            val prefs = preferencesRepository.getPreferences()
            selectedSound = prefs.selectedSoundId
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = "Alarm reliability",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        PermissionRow(
            label = "Notifications",
            granted = permissionStatus.hasNotificationPermission,
            actionLabel = "Enable",
            onAction = onRequestNotifications
        )
        PermissionRow(
            label = "Exact alarms",
            granted = permissionStatus.canScheduleExactAlarms,
            actionLabel = "Open settings",
            onAction = onOpenExactAlarmSettings,
            visible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        )
        PermissionRow(
            label = "Ignore battery optimization",
            granted = permissionStatus.isIgnoringBatteryOptimizations,
            actionLabel = "Disable",
            onAction = onOpenBatterySettings
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Light", "Dark", "System").forEach { theme ->
                        FilterChip(
                            selected = currentThemeMode == theme.lowercase(),
                            onClick = {
                                scope.launch {
                                    themeViewModel.setThemeMode(theme.lowercase())
                                }
                            },
                            label = { Text(theme, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }

        Text(
            text = "Sound",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Alarm Sound",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = availableSounds.find { it.id == selectedSound }?.name ?: "Default",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Button(
                    onClick = { showSoundDialog = true },
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Text("Select", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Text(
            text = "About",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "App Version",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "1.0.0",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showSoundDialog) {
        com.example.alarm.core.ui.components.SoundSelectionDialog(
            sounds = availableSounds,
            selectedSoundId = selectedSound,
            onSoundSelected = { soundId ->
                selectedSound = soundId
                scope.launch {
                    themeViewModel.preferencesRepository.updateSelectedSound(soundId)
                }
            },
            onPreviewSound = { soundId ->
                scope.launch {
                    audioPlaybackManager.preview(soundId)
                }
            },
            onAddCustomSound = { uri, fileName ->
                val sound = audioPlaybackManager.addCustomSound(fileName, uri)
                selectedSound = sound.id
                soundsRefreshTrigger++
                scope.launch {
                    themeViewModel.preferencesRepository.updateSelectedSound(sound.id)
                }
            },
            onDeleteCustomSound = { soundId ->
                audioPlaybackManager.removeCustomSound(soundId)
                soundsRefreshTrigger++
            },
            onDismiss = {
                showSoundDialog = false
                scope.launch {
                    audioPlaybackManager.stop()
                }
            }
        )
    }
}

@Composable
private fun PermissionRow(
    label: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    visible: Boolean = true
) {
    if (!visible) return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (granted) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (granted) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = if (granted) "Granted" else "Not granted",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (granted) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onErrorContainer
                )
            }
            if (!granted) {
                TextButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

sealed class Screen {
    object AlarmList : Screen()
    object CreateEdit : Screen()
    object Stats : Screen()
}
