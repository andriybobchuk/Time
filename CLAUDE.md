# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Kotlin Multiplatform project targeting Android and iOS, built with Compose Multiplatform. The project combines two main applications:

1. **Time Tracking App** - Primary focus, for tracking time spent on different jobs/tasks
2. **Mooney** - Financial management component (appears to be legacy/secondary)

The project uses a clean architecture with domain, data, and presentation layers, dependency injection via Koin, and Room database for persistence.

## Build Commands

### Prerequisites
- Java Runtime Environment must be installed
- Android SDK for Android development
- Xcode for iOS development

### Common Build Commands
```bash
# Build the project (requires Java runtime)
./gradlew build

# Clean build
./gradlew clean

# Build Android APK
./gradlew assembleDebug

# Run tests
./gradlew test

# Check dependencies
./gradlew dependencies
```

**Note**: The project currently shows "Unable to locate a Java Runtime" error, so ensure Java is properly installed and configured.

## Architecture

### Module Structure
- **composeApp/**: Main shared code module
  - `commonMain/`: Shared business logic and UI
  - `androidMain/`: Android-specific implementations
  - `iosMain/`: iOS-specific implementations
- **iosApp/**: iOS application wrapper

### Core Architecture Layers

#### Domain Layer (`time/domain/`, `mooney/domain/`)
- **Models**: Core business entities (`TimeBlock`, `Job`, `Transaction`, `Account`)
- **Repositories**: Abstract interfaces for data access
- **Use Cases**: Business logic operations (e.g., `StartTimeTrackingUseCase`, `GetDailySummaryUseCase`)

#### Data Layer (`time/data/`, `mooney/data/`)
- **Repository Implementations**: `DefaultTimeRepositoryImpl`, `DefaultCoreRepositoryImpl`
- **Database**: Room database with DAOs for entities
- **Data Sources**: Local data handling

#### Presentation Layer (`time/presentation/`, `mooney/presentation/`)
- **ViewModels**: State management using Compose ViewModel
- **Screens**: Compose UI screens
- **Navigation**: Type-safe navigation using Compose Navigation

### Key Technologies
- **UI Framework**: Compose Multiplatform
- **Database**: Room with SQLite
- **HTTP Client**: Ktor
- **Image Loading**: Coil3
- **DI**: Koin
- **Serialization**: kotlinx.serialization
- **Date/Time**: kotlinx-datetime

### Database Schema & Room Cross-Platform Setup

#### Database Configuration
Current database version: 3 (see `AppDatabase.kt:9`)
Database name: `time_debug_v2.db`

**Entities:**
- **TimeBlockEntity**: Time tracking data with effectiveness field
- **TransactionEntity**: Financial transactions  
- **AccountEntity**: Financial accounts
- Schema files are stored in `composeApp/schemas/`

#### Room Cross-Platform Architecture

The project uses Room in a Kotlin Multiplatform context with the following architecture:

**Common Module (`commonMain/`):**
- `AppDatabase`: Main database class annotated with `@Database` and `@ConstructedBy(AppDatabaseConstructor::class)`
- `AppDatabaseConstructor`: Expect object implementing `RoomDatabaseConstructor<AppDatabase>`
- `MooneyDatabaseFactory`: Expect class with platform-specific implementations
- DAOs: `TimeBlockDao`, `TransactionDao`, `AccountDao`

**Platform-Specific Implementations:**

**Android (`androidMain/`):**
- `MooneyDatabaseFactory` takes `Context` parameter
- Uses `Room.databaseBuilder()` with Android application context
- Database file stored in app's database directory via `context.getDatabasePath()`
- Configured in `di/Modules.android.kt` with `androidApplication()` context

**iOS (`iosMain/`):**
- `MooneyDatabaseFactory` parameterless constructor  
- Uses `Room.databaseBuilder<AppDatabase>()` with file path
- Database stored in iOS Documents directory
- Uses `NSFileManager` and `NSDocumentDirectory` for cross-platform file system access
- Configured in `di/Modules.ios.kt` without parameters

**Dependency Injection Setup:**
```kotlin
// Shared module (Modules.kt)
single {
    get<MooneyDatabaseFactory>().create()
        .setDriver(BundledSQLiteDriver())
        .build()
}
single { get<AppDatabase>().timeBlockDao }
// ... other DAOs

// Platform modules inject the factory
// Android: single { MooneyDatabaseFactory(androidApplication()) }
// iOS: single { MooneyDatabaseFactory() }
```

**Key Room Multiplatform Features:**
- Uses `BundledSQLiteDriver` for consistent SQLite across platforms
- Schema directory configured in `build.gradle.kts`: `room { schemaDirectory("$projectDir/schemas") }`
- Expect/actual pattern for platform-specific database creation
- Shared business logic in common DAOs and entities

### Navigation Structure
- **TimeGraph**: Primary navigation graph
  - `TimeTracking`: Main time tracking screen
  - `TimeAnalytics`: Time analytics and reporting
- **MooneyGraph**: Financial management (currently commented out)

## Time Tracking Application - Detailed Analysis

The Time app is the primary focus of this project, implementing comprehensive time tracking with analytics. Here's the complete architecture:

### Domain Layer (`time/domain/`)

#### Core Models
- **TimeBlock**: The central entity representing a work session
  - `id`: Auto-generated identifier
  - `jobId`, `jobName`: Associated job information
  - `startTime`, `endTime`: DateTime boundaries (endTime is null for active blocks)
  - `duration`: Calculated duration in milliseconds
  - `effectiveness`: Enum (Productive, Unproductive) - new field for productivity tracking
  - Methods: `isActive()`, `calculateDuration()`, `getDurationInHours()`, `getFormattedDuration()`

- **Job**: Work categories/projects
  - `id`: String identifier
  - `name`: Display name
  - `color`: Long value for UI theming
  - Currently hardcoded in `TimeDataSource` (Rivian, Plato, Business)

- **DailySummary**: Aggregated daily analytics
  - `date`: Target date
  - `blocks`: List of TimeBlocks for the day
  - `totalHours`: Sum of all durations
  - `jobBreakdown`: Map of job summaries with percentages

- **WeeklyAnalytics**: 7-day rolling analytics
  - `weekStart`, `weekEnd`: Date range
  - `dailySummaries`: Day-by-day breakdown
  - `totalHours`, `averageDailyHours`: Week aggregations
  - `jobBreakdown`: Job analytics with averages and percentages

- **JobSummary** & **JobAnalytics**: Aggregation models for reporting

#### Repository Interface
`TimeRepository` defines the contract for data operations:
- CRUD operations for TimeBlocks
- Job management (currently read-only)
- Analytics computation (daily/weekly summaries)
- Real-time data via Flow APIs

### Data Layer (`time/data/`)

#### Database Schema
- **TimeBlockEntity**: Room entity with auto-generated ID
  - Stores startTime/endTime as ISO string format
  - Maps effectiveness enum to string for persistence
  - Table name: `time_blocks`

#### DAO Operations
`TimeBlockDao` provides:
- `@Upsert` for create/update operations
- Queries by date, date range, and active blocks
- SQLite date functions for filtering: `date(startTime) = :date`
- Flow-based reactive queries

#### Data Mapping
`TimeBlockMappers.kt` handles domain ↔ entity conversion:
- LocalDateTime ↔ ISO string conversion
- Effectiveness enum ↔ string mapping

#### Repository Implementation
`DefaultTimeRepositoryImpl` implements business logic:
- Analytics calculations with percentage computation
- Weekly analytics treats input date as END date (last 7 days)
- Job data sourced from hardcoded `TimeDataSource`

### Presentation Layer (`time/presentation/`)

#### State Management
**TimeTrackingState**:
- `selectedDate`: Current date filter
- `timeBlocks`: Filtered time blocks for selected date
- `activeTimeBlock`: Currently running session (if any)
- `jobs`: Available job categories
- `dailySummary`: Computed analytics for selected date
- UI state: loading, error, modal sheets

**AnalyticsState**:
- `selectedWeekStart`: Week filter
- `weeklyAnalytics`: 7-day rolling analytics
- Loading and error states

#### Actions & ViewModels
**TimeTrackingAction**:
- `StartTracking(jobId)`: Begin new time session
- `StopTracking`: End current session
- `StopTrackingWithEffectiveness`: End with productivity rating
- `SelectDate`: Filter by date
- CRUD operations: EditTimeBlock, DeleteTimeBlock, etc.

**TimeTrackingViewModel**:
- Reactive state management with StateFlow
- Job management for Flow subscriptions
- Error handling with Result types
- Real-time updates via use case observations

**AnalyticsViewModel**:
- Week-based analytics loading
- Date calculations for week boundaries (Monday start)

#### UI Components
**TimeTrackingScreen**:
- Date picker in top bar
- Job selection buttons for starting tracking
- Time block list with edit/delete actions
- Modal sheets for editing and adding blocks
- FAB for manual time entry

### Use Cases (`time/domain/usecase/`)

Eight specialized use cases handle business operations:

1. **StartTimeTrackingUseCase**: 
   - Validates job exists
   - Prevents multiple active sessions
   - Creates new TimeBlock with current timestamp

2. **StopTimeTrackingUseCase**:
   - Finds active block
   - Sets endTime and calculates duration
   - Updates database

3. **GetTimeBlocksUseCase**: Date-filtered block retrieval
4. **GetActiveTimeBlockUseCase**: Real-time active session monitoring
5. **GetJobsUseCase**: Job list retrieval
6. **GetDailySummaryUseCase**: Daily analytics computation
7. **GetWeeklyAnalyticsUseCase**: Weekly analytics computation
8. **DeleteTimeBlockUseCase** & **UpsertTimeBlockUseCase**: CRUD operations

### Navigation Structure

Time app uses its own navigation graph:
- **Route.TimeGraph**: Parent navigation container
  - **Route.TimeTracking**: Main time tracking interface
  - **Route.TimeAnalytics**: Analytics and reporting view
- **TimeBottomNavigationBar**: Two-tab navigation (Blocks, Analytics)

### Key Features & Behavior

1. **Real-time Tracking**: Active time blocks update duration continuously
2. **Date Navigation**: Calendar-based filtering of historical data
3. **Effectiveness Tracking**: Productivity rating for completed sessions
4. **Manual Entry**: Add/edit time blocks via modal sheets
5. **Analytics**: Daily summaries and 7-day rolling analytics
6. **Job Management**: Hardcoded categories with color theming

## Development Notes

### Database Migrations
When modifying entities, increment the database version in `AppDatabase.kt` and provide migration scripts if needed.

### Dependency Injection
All dependencies are configured in `di/Modules.kt`. Use Koin's DSL for new dependencies:
- `singleOf()` for repository implementations
- `viewModelOf()` for ViewModels
- Platform-specific modules in `di/Modules.android.kt` and `di/Modules.ios.kt`

### Adding New Features
1. Define domain models in appropriate `domain/Model.kt`
2. Create use cases in `domain/usecase/` 
3. Implement repository interfaces in `data/`
4. Add database entities and DAOs if needed
5. Create ViewModels and UI in `presentation/`
6. Wire dependencies in `di/Modules.kt`
7. Add navigation routes in `app/Route.kt`

### Project Configuration
- Root project name: "Mooney" (legacy naming)
- Application ID: `com.andriybobchuk.time`
- Target SDK: 35, Min SDK: 24
- Database name: `time_debug_v2.db`