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
alarm-app/
├── app/                        # Entry point — MainActivity, navigation, DI setup
│
├── core/                       # Shared system-level components
│   ├── common/                 # Utilities, time helpers
│   ├── notification/           # Notification system
│   ├── scheduler/              # AlarmScheduler abstraction
│   ├── sound/                  # MediaPlayer + volume control engine
│   └── utils/
│
├── data/                       # Persistence + implementation
│   ├── alarm_db/               # Room DB, entities, DAOs
│   ├── alarm_repository/       # Repository layer
│   └── alarm_scheduler_impl/   # AlarmManager + WorkManager integration
│
├── feature_alarm/              # Create / edit / list alarms
├── feature_ring/               # Full-screen alarm UI + snooze/dismiss
├── feature_widget/             # Home screen widget (daily overview)
├── feature_stats/              # Analytics + reports
└── feature_sleep/              # Bedtime reminders + sleep tracking
```

---

## Features

### Alarm Scheduling
- Exact-time, repeat (day-of-week), date-specific, and countdown alarms
- Pre-alarm notification at T − 15 min via WorkManager
- Snooze with configurable duration (default 10 min)

### Sound System
- Multiple selectable alarm sounds
- Gradual volume ramp (configurable curve)

### Sleep Tracking
- Bedtime reminder
- Sleep session estimation
- Wake-up time tracking

### Statistics
- Snooze count, wake-up delay, dismissal behavior
- Wake consistency score + no-snooze streak
- Weekly report generated every Sunday

### Home Screen Widget
- View upcoming alarms
- Quick enable/disable and cancel

### UI
- Light / Dark mode, follows system theme

---

## MVP Roadmap

### Phase 1 — Core Alarm Engine
- [ ] Create / edit / delete alarm
- [ ] Schedule via AlarmManager
- [ ] BroadcastReceiver + ForegroundService ring engine
- [ ] Full-screen ring UI
- [ ] Snooze and dismiss
- [ ] Sound playback

### Phase 2 — Scheduling Features
- [ ] Repeat rules (weekly day-of-week)
- [ ] Countdown alarm type
- [ ] Pre-alarm notification (T − 15 min)

### Phase 3 — UX Layer
- [ ] Light / Dark theme
- [ ] Home screen widget
- [ ] Sound selection UI

### Phase 4 — Intelligence Layer
- [ ] Statistics event logging
- [ ] Daily + weekly aggregation
- [ ] Weekly report
- [ ] No-snooze streak tracking

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