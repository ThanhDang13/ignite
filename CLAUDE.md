# OpenWolf

@.wolf/OPENWOLF.md

This project uses OpenWolf for context management. Read and follow .wolf/OPENWOLF.md every session. Check .wolf/cerebrum.md before generating code. Check .wolf/anatomy.md before reading files.


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

### Sound System
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