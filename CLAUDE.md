# OpenWolf

@.wolf/OPENWOLF.md

This project uses OpenWolf for context management. Read and follow .wolf/OPENWOLF.md every session. Check .wolf/cerebrum.md before generating code. Check .wolf/anatomy.md before reading files.


# CLAUDE.md — Alarm App Agent Instructions

## Project Summary

Android Kotlin alarm app. Feature-based modular monolith. CLI-only workflow (no Android Studio).

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
| `core/scheduler/` | AlarmScheduler interface only — no implementation |
| `data/alarm_scheduler_impl/` | AlarmManager + WorkManager wiring |
| `data/alarm_db/` | Room entities, DAOs, migrations |
| `data/alarm_repository/` | Single source of truth for alarm state |
| `feature_ring/` | ForegroundService + full-screen UI |
| `feature_alarm/` | Create / edit / list UI only |
| `feature_stats/` | Read-only analytics queries + UI |

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