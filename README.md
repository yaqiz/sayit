# SayIt

SayIt is an Android app for managing daily tasks with voice commands.

It opens on today's task list, supports a monthly calendar view, and lets you create, complete, query, and delete tasks by speaking in Chinese.

## Features

- Voice-first daily task management
- Today's task list as the default home screen
- Monthly calendar view with date-based task browsing
- Task status colors
  - Blue: completed
  - Light red: overdue and unfinished
  - White: pending and not overdue
- Double-tap blank space on the main page to start listening
- Built-in voice feedback with Android TextToSpeech
- Custom app icon and in-app branding

## Voice Commands

Examples currently supported:

- `记录我今天完成小红书文案`
- `小红书文案完成`
- `告诉我今天未完成的任务`
- `告诉我今天完成的任务`
- `删除今天的所有任务`

## Main Interaction

- App launch opens today's task list
- Press back from task list to open the monthly calendar
- Press back from calendar to return to today's task list
- Tap a calendar cell to open that day's task list
- The "可以直接说" card on the home page can be collapsed

## Tech Stack

- Kotlin
- Android Jetpack Compose
- Room
- Android TextToSpeech
- Android speech recognition via `RecognizerIntent`

## Current Speech Recognition Note

The current implementation uses Android system speech recognition through `RecognizerIntent`.

This is fast to integrate, but availability depends on the speech service installed on the device. For users in mainland China, Google-backed recognition may be slow or unavailable. The app currently includes:

- listening state UI
- processing state UI
- `EXTRA_PREFER_OFFLINE` hint for the system recognizer

If you need a production-ready China-friendly solution, the next recommended step is replacing this layer with:

- a domestic speech SDK such as Baidu or Aliyun, or
- a fully offline engine such as `sherpa-onnx`

## Requirements

- Android Studio
- Android SDK 34
- JDK 17

## Open In Android Studio

1. Open Android Studio
2. Select `Open`
3. Choose this project folder
4. Wait for Gradle sync to finish

If Gradle JDK is not configured, use Android Studio's embedded JDK:

- `File` -> `Settings` -> `Build, Execution, Deployment` -> `Build Tools` -> `Gradle`
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

## Assets

App icon source files are stored in:

- `img/`

## Status

This repository contains a working Android prototype and currently builds successfully with:

```powershell
.\gradlew.bat assembleDebug
```

## Roadmap

- Replace system speech recognition with a China-friendly solution
- Add better release packaging and signing flow
- Improve task editing and deletion controls in UI
- Add release notes and APK distribution
