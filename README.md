# Alarm App — Android Kotlin

> Smart alarm system for Android written in Kotlin.  
> CLI-first workflow (Gradle + ADB). No Android Studio required.

---

## Overview

Feature-rich smart alarm clock built on Android system-level primitives. Goes beyond basic scheduling to include sleep tracking, usage analytics, and home-screen widget integration.

Architecture: **feature-based modular monolith** (not DDD), optimized for Android background execution constraints.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin (JVM + Android) |
| Build | Gradle Kotlin DSL |
| SDK | Android SDK (CLI tools only) |
| Debug | ADB |
| UI | Jetpack Compose |
| Storage | Room Database |
| Background | AlarmManager (exact alarms) + WorkManager |
| DI | Hilt (recommended) or manual DI |

---

## Project Structure

```
ignite/
├── app/                        # Entry point — MainActivity, navigation, DI setup
│
├── core/                       # Shared system-level components
│   ├── common/                 # Utilities, time helpers, AlarmTimeCalculator
│   ├── notification/           # Notification system
│   ├── scheduler/              # AlarmScheduler abstraction
│   ├── sound/                  # AudioPlaybackManager, SoundEngine
│   └── ui/                     # Shared Compose components, theme, animations
│
├── data/                       # Persistence + implementation
│   ├── alarm_db/               # Room DB, entities, DAOs
│   ├── alarm_repository/       # Repository layer, Alarm domain model
│   └── alarm_scheduler_impl/   # AlarmManager + WorkManager integration
│
├── feature/                    # Feature modules
│   ├── alarm/                  # Create / edit / list alarms
│   ├── ring/                   # Full-screen alarm UI + snooze/dismiss + ForegroundService
│   ├── widget/                 # Home screen widget (daily overview)
│   ├── stats/                  # Analytics + reports + weekly report generation
│   ├── sleep/                  # Bedtime reminders + sleep tracking (placeholder)
│   ├── timer/                  # Timer feature
│   └── stopwatch/              # Stopwatch feature
```

---

## Features

### Alarm Scheduling
- Exact-time, repeat (day-of-week), date-specific, and countdown alarms
- Pre-alarm notification at T − 15 min via WorkManager
- Snooze with configurable duration (default 10 min)
- Persistent scheduling via AlarmManager + WorkManager

### Sound System
- Multiple selectable alarm sounds (built-in + custom)
- Custom sound persistence to database
- Gradual volume ramp (configurable curve)
- AudioPlaybackManager singleton for centralized playback

### Sleep Tracking
- Bedtime reminder
- Sleep session estimation
- Wake-up time tracking

### Statistics & Analytics
- Event logging: AlarmFired, AlarmDismissed, AlarmSnoozed
- Snooze count, wake-up delay, dismissal behavior
- Wake consistency score + no-snooze streak
- Weekly report generated every Sunday
- Daily + weekly aggregation

### Home Screen Widget
- View upcoming alarms
- Quick enable/disable and cancel

### Timer & Stopwatch
- Countdown timer with notifications
- Stopwatch with lap tracking

### UI & Theme
- Material Design 3 system
- Light / Dark mode, follows system theme
- Jetpack Compose throughout
- Smooth animations and transitions

---

## Implementation Status

### ✅ Complete
- Core alarm engine (create, edit, delete, schedule)
- AlarmManager + WorkManager integration
- BroadcastReceiver + ForegroundService ring engine
- Full-screen ring UI with snooze/dismiss
- Sound playback with custom sound persistence
- Repeat rules (weekly day-of-week)
- Countdown alarm type
- Pre-alarm notification (T − 15 min)
- Material Design 3 theme with light/dark mode
- Home screen widget
- Sound selection UI
- Statistics event logging + aggregation
- Weekly report generation
- No-snooze streak tracking
- Timer and stopwatch features

### 🚧 In Progress / Placeholder
- Sleep tracking (bedtime reminders, session estimation)

---

## Quick Start

```bash
# Build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# View logs
adb logcat

# Inspect scheduled alarms
adb shell dumpsys alarm

# Launch app
adb shell am start -n com.example.app/.MainActivity
```