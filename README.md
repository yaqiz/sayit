# SayIt

SayIt is an Android task management app designed around voice-first interaction.

The app opens on today's task list, supports a monthly calendar view, and allows users to create, complete, review, and delete tasks using short spoken commands in Chinese.

## Why This Project Exists

Many lightweight task apps still assume that users want to type, tap through multiple screens, and manually organize daily items. SayIt explores a different interaction model:

- capture tasks as quickly as a spoken thought
- keep today's work immediately visible
- make calendar navigation simple and visual
- reduce friction for repetitive daily task management

This repository currently contains a working Android prototype built with Kotlin, Jetpack Compose, and Room.

## Core Features

- Voice-first task input and task updates
- Today's task list as the default entry screen
- Monthly calendar view for browsing tasks by date
- Tap any calendar cell to open that day's task list
- Visual task status by date
  - Blue: completed
  - Light red: overdue and unfinished
  - White: pending and not overdue
- Double-tap blank space on the main screen to start listening
- Collapsible quick-command hint card
- Custom app icon and in-app brand treatment

## Supported Voice Commands

Current examples include:

- `Record a task for today`
- `Mark a task as completed`
- `Tell me today's unfinished tasks`
- `Tell me today's completed tasks`
- `Delete all tasks for today`

In the current build, the actual recognition phrases are implemented in Chinese because the product flow is focused on Chinese-speaking usage.

## Interaction Model

- App launch opens today's task list
- Back from task list opens the monthly calendar
- Back from calendar returns to today's task list
- Tapping a date cell opens that day's tasks
- The primary voice button shows explicit listening and processing states
- The header also surfaces system feedback such as listening, processing, and recognition results

## Tech Stack

- Kotlin
- Android Jetpack Compose
- Room
- Android TextToSpeech
- Android system speech recognition via `RecognizerIntent`

## Current Speech Recognition Limitation

The current prototype uses Android system speech recognition through `RecognizerIntent`.

That makes the prototype fast to build, but it also means recognition quality, latency, and availability depend on the speech service installed on the device. In mainland China, Google-backed recognition may be slow, unavailable, or inconsistent.

The current implementation already includes:

- listening state feedback
- processing state feedback
- an offline preference hint via `RecognizerIntent.EXTRA_PREFER_OFFLINE`

For production use in China, the recommended next step is to replace the current recognition layer with either:

- a domestic speech SDK such as Baidu or Aliyun, or
- a fully offline speech recognition engine such as `sherpa-onnx`

## Requirements

- Android Studio
- Android SDK 34
- JDK 17

## Open In Android Studio

1. Open Android Studio
2. Choose `Open`
3. Select this project folder
4. Wait for Gradle sync to finish

If Gradle JDK is not configured, use Android Studio's embedded JDK:

- `File` -> `Settings`
- `Build, Execution, Deployment` -> `Build Tools` -> `Gradle`
- Set `Gradle JDK` to `jbr`

## Run The App

1. Connect an Android device or start an emulator
2. Click `Run`
3. Grant microphone permission when prompted

## Build APK

In Android Studio:

1. Click `Build`
2. Click `Build APK(s)`

The debug APK is usually generated at:

`app/build/outputs/apk/debug/app-debug.apk`

From the command line on Windows:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="C:\Program Files\Android\Android Studio\jbr\bin;$env:Path"
.\gradlew.bat assembleDebug
```

## Project Structure

```text
app/src/main/java/com/example/sayit/
  MainActivity.kt
  data/
  ui/
  voice/
```

## Permissions

- `RECORD_AUDIO`

## Visual Assets

Source icon assets are stored in:

- `img/`

## Build Status

This repository builds successfully with:

```powershell
.\gradlew.bat assembleDebug
```

## Case Study

If you want to present this project as a portfolio piece or LinkedIn case study, see:

- [`docs/case-study.md`](docs/case-study.md)

That document includes:

- product framing
- problem statement
- user needs
- functional requirements
- design decisions
- technical tradeoffs
- slide-ready outline

## Roadmap

- Replace system speech recognition with a China-friendly solution
- Add stronger task editing and deletion controls in UI
- Add signed release packaging
- Add screenshots, release notes, and APK distribution
