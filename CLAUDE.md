# OpenWolf

@.wolf/OPENWOLF.md

This project uses OpenWolf for context management. Read and follow .wolf/OPENWOLF.md every session. Check .wolf/cerebrum.md before generating code. Check .wolf/anatomy.md before reading files.

# CocoIndex

@mcp/cocoindex

This project uses CocoIndex MCP for repository discovery.

Use CocoIndex before recursive file reads.

Use CocoIndex for:
- symbol discovery
- cross-module references
- architecture navigation
- semantic code search

Read source files before making edits.

Source files are authoritative if indexed context differs from repository state.


# CLAUDE.md — Ignite: Multi-Feature Time Management App

## Project Summary

Android Kotlin time management app (alarms, timer, stopwatch). Feature-based modular monolith with Material Design 3 UI. CLI-only workflow (no Android Studio). Emphasis on AlarmManager correctness, persistent state, and analytics.

## Core Rules

- AlarmManager correctness is top priority — UI polish is secondary
- Every alarm state change must be persisted to Room **immediately**, before returning from any handler
- Keep all scheduling logic centralized in `data/alarm_scheduler_impl/`
- No DDD, no excessive abstraction layers
- Validate all scheduling via ADB after changes (see CLI commands below)

---

## Data Model

```kotlin
data class Alarm(
    val id: Long,
    val title: String,
    val timeMillis: Long,
    val isEnabled: Boolean,
    val repeatDays: Set<Int>,           // DayOfWeek ints, empty = one-time
    val isCountdown: Boolean,
    val countdownDurationMillis: Long?, // null if not countdown type
    val soundId: String,
    val snoozeMinutes: Int,             // default: 10
    val preAlarmEnabled: Boolean        // show notification at T-15min
)
```

---

## Lifecycle Flows

### Alarm Fire

```
AlarmManager trigger
  → BroadcastReceiver
  → ForegroundService (acquire WakeLock, start ring engine)
  → Full-screen UI displayed
  → User action:
      ├── Dismiss → stop sound → log AlarmDismissed → persist to DB
      └── Snooze  → stop sound → reschedule (now + snoozeMinutes) → increment snooze count → log AlarmSnoozed → persist to DB
```

### Pre-Alarm (T − 15 min)

```
WorkManager fires at T − 15 min
  → Show notification
  → User action:
      ├── Dismiss → cancel main AlarmManager alarm → log event
      └── Ignore  → no-op, alarm continues as scheduled
```

### Snooze

```
User taps Snooze
  → Stop alarm sound
  → AlarmManager.set(now + snoozeMinutes)
  → Increment snooze count in Room
  → Log AlarmSnoozed event
```

---

## Analytics Events

Log these to Room on every occurrence:

| Event | Trigger |
|---|---|
| `AlarmFired` | BroadcastReceiver receives alarm intent |
| `AlarmDismissed` | User taps Dismiss |
| `AlarmSnoozed` | User taps Snooze |

Aggregated into: daily stats, weekly report (Sunday), no-snooze streak.

---

## Android-Specific Requirements

- Declare `SCHEDULE_EXACT_ALARM` permission — check and request at runtime on Android 12+
- ForegroundService must acquire a **WakeLock** to prevent CPU sleep during alarm ring
- All alarms must survive process death — reschedule on `BOOT_COMPLETED` broadcast
- Handle Doze mode: use `setExactAndAllowWhileIdle()` for alarm triggers
- Pre-alarm WorkManager job must use `setExpedited()` to avoid deferral

---

## Module Responsibilities

| Module | Responsibility |
|---|---|
| `core/common/` | Utilities: AlarmTimeCalculator, time helpers |
| `core/notification/` | NotificationManager: channels, alarm + pre-alarm notifications |
| `core/scheduler/` | AlarmScheduler interface only — no implementation |
| `core/sound/` | AudioPlaybackManager singleton, SoundEngine, sound playback |
| `core/ui/` | Shared Compose components, Material 3 theme, animations, tokens |
| `data/alarm_db/` | Room entities, DAOs, migrations |
| `data/alarm_repository/` | Single source of truth for alarm state, Alarm domain model |
| `data/alarm_scheduler_impl/` | AlarmManager + WorkManager wiring, AlarmReceiver, PreAlarmWorker |
| `feature/alarm/` | Create / edit / list alarms UI |
| `feature/ring/` | ForegroundService + full-screen ring UI + snooze/dismiss |
| `feature/stats/` | Analytics queries, weekly report generation, stats UI |
| `feature/widget/` | Home screen widget provider + layout |
| `feature/timer/` | Timer feature with countdown |
| `feature/stopwatch/` | Stopwatch feature with lap tracking |
| `feature/sleep/` | Sleep tracking (placeholder) |

---

## Implementation Notes

### Pre-Alarm System
- **WorkManager Job:** Scheduled 15 minutes before main alarm via PreAlarmWorker
- **Notification:** Shows alarm title with "Alarm in 15 minutes" header
- **Actions:** Dismiss (cancels main alarm) and Snooze (cancels pre-alarm, allows main alarm)
- **Conditional:** Only scheduled if `preAlarmEnabled` is true on alarm
- **Safety:** Skips scheduling if pre-alarm time is already in the past
- Custom sounds are persisted to Room database via `CustomSoundEntity`
- `AudioPlaybackManager` is a singleton injected via Hilt across all screens
- Sound selection UI in `core/ui/components/SoundSelection.kt`
- Volume ramping handled by `SoundEngine` with configurable curves

### UI Architecture
- All UI components centralized in `core/ui/` for consistency
- Material Design 3 theming with light/dark mode support
- Animations defined in `core/ui/animations/Animations.kt`
- Spacing system uses 8dp grid via `Tokens.kt`
- Wheel picker components for time selection in alarm creation

### Statistics & Analytics
- Events logged to `AlarmEventEntity` on every alarm state change
- `StatsRepository` aggregates daily and weekly stats
- Weekly report generated via `WeeklyReportWorker` (WorkManager)
- `StatsCalculator` computes derived metrics (streak, consistency score)

### Timer & Stopwatch
- Separate feature modules with independent ViewModels
- Timer: countdown with notification on completion
- Stopwatch: lap tracking with formatted display

---

## Implementation Status (as of 2026-05-24)

### ✅ Complete Features
- **Alarm Engine:** Create, edit, delete, schedule with AlarmManager
- **Scheduling:** One-time, recurring (day-of-week), countdown alarms
- **Snooze System:** Configurable snooze duration (default 10 min), persisted with alarm
- **Pre-Alarm:** WorkManager notification at T−15 min with dismiss/snooze actions
- **Ring Engine:** ForegroundService with WakeLock, full-screen UI, snooze/dismiss
- **Sound System:** 
  - Built-in sounds (default, notification, ringtone) from RingtoneManager
  - Custom sound file picker and persistence to Room database
  - AudioPlaybackManager singleton for centralized playback
  - Sound selection in both alarm creation and global settings
  - "App Default" option that resolves to global default at alarm time
- **Theme System:** Light/Dark/System modes, persistent preference, dynamic switching
- **Statistics:** Event logging (AlarmFired, AlarmDismissed, AlarmSnoozed), daily/weekly aggregation, no-snooze streak
- **Sleep Tracking:** Actual sleep session tracking from alarm enable to dismiss (replaces fake 8-hour estimation)
- **Widget:** Home screen widget showing next alarm
- **Timer:** Countdown with MM:SS display, sound on completion, wheel picker input
- **Stopwatch:** Count-up with HH:MM:SS.MS display, lap tracking
- **UI/UX:** Material Design 3, animations, consistent spacing (8dp grid), dark mode support

### 🚧 Placeholder (Not Implemented)
- **Sleep Feature:** Bedtime reminders, sleep session tracking (placeholder only)

---

## Key Implementation Details

### Sleep Session Tracking
- **Session Start:** Automatically created when alarm is enabled or created
- **Session End:** Completed when alarm is dismissed or snoozed
- **Recurring Alarms:** Each occurrence creates a new independent sleep session
- **Session Cancellation:** Pending sessions cancelled when alarm is disabled or deleted before ringing
- **Legacy Support:** Old estimated sessions (bedtime - 8 hours) marked as `isLegacy = true`
- **SleepSessionRepository:** Centralized repository in data layer for sleep session management
- **Statistics Display:** Shows actual tracked duration, distinguishes between auto-tracked, manual, and legacy sessions

### Sound Persistence Architecture
- **CustomSoundEntity:** Room entity storing custom sounds (id, name, uri, createdAt)
- **CustomSoundDao:** Database access with Flow support
- **AudioPlaybackManager:** 
  - Loads custom sounds from DB on init via runBlocking
  - Persists/deletes via database operations
  - Injected as @Singleton into AlarmViewModel and SettingsViewModel
  - Ensures all screens access same singleton instance
- **Sound Selection:** File picker via ActivityResultContracts.GetContent(), preview stops on dialog dismiss

### Theme & Preferences System
- **PreferencesEntity:** Room entity storing selectedSoundId and themeMode
- **PreferencesRepository:** Singleton providing Flow-based reactive access
- **ThemeViewModel:** Manages theme state, injected into MainActivity
- **Dynamic Switching:** No app restart required, recomposition on preference change

### Alarm Time Calculation
- **resolveNextAlarmTime():** Unified function in AlarmTimeCalculator
  - Accepts individual params (timeMillis, repeatDays, isCountdown, countdownDurationMillis, now)
  - Avoids circular dependencies with data layer
  - One-time alarms: auto-disable after firing, recalculate from current time if re-enabled
  - Recurring alarms: always compute from current time + repeatDays set
  - Never returns past timestamps

### Snooze Implementation
- **Snooze Duration:** Configurable per alarm via +/− buttons in CreateEditAlarmScreen
- **UI Design:** Large 56dp buttons, centered layout, prominent display with "minutes" label
- **Snooze Flow:** Stop sound → reschedule (now + snoozeMinutes) → increment count → log event → persist to DB

### Custom Sound File Picker
- **Integration:** ActivityResultContracts.GetContent() with rememberLauncherForActivityResult
- **Callback:** Receives Uri, passed to AudioPlaybackManager.addCustomSound()
- **State Refresh:** Use soundsRefreshTrigger to force recomposition after add/delete (derivedStateOf alone insufficient for mutable list changes)
- **Cleanup:** Stop audio in onDismiss callback to prevent preview from continuing

### App Default Sound Resolution
- **Storage:** Save "app_default" as soundId in alarm
- **Resolution:** At alarm fire time (RingService.playAlarm), resolve "app_default" to current global default from PreferencesRepository
- **Benefit:** Alarms dynamically use whatever sound is set globally

---

## Database Schema (Current Version: 7)

### Key Entities
- **AlarmEntity:** id, title, timeMillis, isEnabled, repeatDays (JSON), isCountdown, countdownDurationMillis, soundId, snoozeMinutes, preAlarmEnabled
- **AlarmEventEntity:** id, alarmId, eventType, timestamp
- **CustomSoundEntity:** id, name, uri, createdAt
- **PreferencesEntity:** id, selectedSoundId, themeMode, updatedAt
- **SleepSessionEntity:** id, alarmId, sessionStartMillis, sessionEndMillis, durationMillis, wasManual, isLegacy, createdAt, bedtimeMillis (deprecated), wakeTimeMillis (deprecated)
- **WeeklyReportEntity:** id, weekStartDate, totalAlarms, dismissedCount, snoozedCount, avgSnoozeCount, consistency, generatedAt

### Migration 6 → 7
- Added `alarmId`, `sessionStartMillis`, `sessionEndMillis`, `isLegacy` columns to `sleep_sessions`
- Migrated existing data: `bedtimeMillis` → `sessionStartMillis`, `wakeTimeMillis` → `sessionEndMillis`
- Legacy records marked with `isLegacy = true` for backward compatibility

---

## CLI Debug Commands

```bash
# Build & install
./gradlew assembleDebug
./gradlew installDebug

# Verify alarm is scheduled
adb shell dumpsys alarm | grep com.example.alarm

# Stream logs (filter by tag)
adb logcat -s AlarmReceiver:D RingService:D

# Launch app directly
adb shell am start -n com.example.alarm/.MainActivity

# Simulate boot (test BOOT_COMPLETED receiver)
adb shell am broadcast -a android.intent.action.BOOT_COMPLETED -p com.example.alarm
```

---

## Known Constraints & Patterns

### Do Not
- Use `@ApplicationScoped` for repositories — use `@Singleton` from javax.inject
- Use suspend functions for Compose state loading — use Flow-based queries with LaunchedEffect
- Use `collectAsState()` in Compose — use `collectAsStateWithLifecycle()` for lifecycle safety
- Rely on `derivedStateOf` alone for list updates from mutable singletons — manually refresh state after mutations
- Forget to stop audio playback when dialog closes — use onDismiss callback or DisposableEffect

### Do
- Persist all alarm state changes to Room immediately before returning from handlers
- Use Flow-based getByIdFlow() for Compose state management
- Inject AudioPlaybackManager into ViewModels to ensure singleton access across all screens
- Load custom sounds from DB on AudioPlaybackManager init via runBlocking
- Use LaunchedEffect(alarmId) to load all alarm properties when editing
- Resolve "app_default" soundId at alarm fire time, not at creation time

---

## Architecture Decisions

1. **Feature-based Modular Monolith:** Scalability and independent testing without DDD overhead
2. **Centralized AudioPlaybackManager:** Single source of truth for all audio playback
3. **Room Database for Preferences:** Persistent storage survives app restarts and process death
4. **Flow-based Reactive Updates:** UI responds immediately to preference changes
5. **Manual Navigation:** State-based tab switching (no Compose Navigation) — simpler for 5 tabs
6. **Wheel Picker UX:** Vertical scroll wheel for time selection — more intuitive than manual input
7. **Immediate DB Persistence:** All state changes persisted before returning from handlers (AlarmManager correctness priority)

---

## Next Steps for Future Sessions

1. **Sleep Feature:** Implement bedtime reminders and sleep session tracking (currently placeholder)
2. **Testing:** Manual device testing of sound persistence, theme switching, alarm scheduling
3. **Performance:** Profile and optimize if needed (currently builds in ~5s)
4. **Additional Polish:** UI refinements, accessibility improvements
5. **Documentation:** Add inline code comments for complex logic