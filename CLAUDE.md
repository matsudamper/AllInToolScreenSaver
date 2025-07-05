# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AllInToolScreenSaver is an Android application that provides a screen saver (DreamService) with calendar integration and event alerts. The app displays calendar events and provides notification alerts for upcoming events.

## Development Commands

### Build and Test
```bash
# Build debug APK and run all checks
./gradlew assembleDebug assembleDebugAndroidTest detektDebug :app:lintDebug

# Run UI tests on managed device
./gradlew pixel9api35DebugAndroidTest

# Format code
./gradlew ktlintFormat
```

### Individual Commands
```bash
# Build only
./gradlew assembleDebug

# Run static analysis
./gradlew detektDebug

# Run lint
./gradlew :app:lintDebug

# Run unit tests
./gradlew testDebug
```

## Code Architecture

### Core Components

- **ClockDreamService**: Main DreamService implementation that displays the screen saver
- **MainActivity**: Main configuration activity using Navigation3 and adaptive layouts
- **MyApp**: Application class with Koin dependency injection setup
- **AlertManager**: Manages calendar event alerts and notifications
- **AlertService**: Foreground service for event notifications

### Key Patterns

- **Navigation**: Uses Navigation3 with custom two-pane strategy for adaptive layouts
- **Dependency Injection**: Koin for dependency management
- **Architecture**: MVVM pattern with ViewModels and UiState classes
- **Data Layer**: Repository pattern with CalendarRepository and SettingsRepository
- **UI**: Jetpack Compose with Material3 theming

### Module Structure

- `app`: Main application module
- `ktlint-custom-rules`: Custom Ktlint rules
- `costom-detekt-rules`: Custom Detekt rules (note: intentional typo in module name)

### Key Directories

- `compose/`: Compose UI screens and components
- `viewmodel/`: ViewModels for screens
- `lib/`: Utility classes and event handling
- `theme/`: Material3 theme configuration

## Code Style Requirements

This project follows strict coding conventions from `.cursor/rules/coding-rule.mdc`:

## Key Technologies

- **Kotlin**: Primary language with strict coding conventions
- **Jetpack Compose**: Modern UI toolkit with Material3
- **Navigation3**: Latest navigation with adaptive layouts
- **Koin**: Dependency injection
- **Protobuf**: Data serialization
- **Coil**: Image loading
- **Haze**: Visual effects library
- **DataStore**: Settings persistence
