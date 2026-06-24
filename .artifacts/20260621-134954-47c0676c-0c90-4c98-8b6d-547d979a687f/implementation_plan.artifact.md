# Implementation Plan - Streamix Android App

Build a premium Android streaming app with AMOLED black design, frosted glass UI, multiple profiles (Movies, Songs, YouTube, Adult), and scraper integration for video streaming.

## Proposed Changes

### Project Configuration & Foundation
- Setup `libs.versions.toml` with all necessary dependencies (Compose, Hilt, Retrofit, Room, etc.).
- Setup root `build.gradle.kts` and `settings.gradle.kts`.
- Setup `app/build.gradle.kts` with proper plugins and dependencies.
- Implement `StreamixTheme` with AMOLED colors and Frosted Glass effects.
- Create basic folder structure as defined in the master prompt.

### Core Architecture & UI Components
- Implement `MainActivity.kt` and `StreamixApp.kt`.
- Implement `NavGraph.kt` with all defined screens.
- Implement core UI components:
    - `FrostedGlassBox.kt`
    - `StreamixHeader.kt`
    - `StreamixSearchBar.kt`
    - `StreamixBottomDock.kt`
    - `ProfileSwitcherButton.kt`

### Profile System & Persistence
- Implement `Profile.kt` enum.
- Setup `PreferencesManager.kt` using DataStore for profile and passcode management.
- Implement `PasscodeScreen.kt` for Adult profile protection.
- Setup Room Database for the Library (Watchlist).

### Movies Profile (TMDB + Scrapers)
- Implement `TmdbApiService.kt` and `TmdbRepository.kt`.
- Implement `BaseScraper.kt` and `MovieboxProvider`, `MovieboxInProvider`.
- Implement `ScraperRepository.kt` with failover logic.
- Implement `HomeScreen.kt`, `SearchScreen.kt`, and `DetailScreen.kt` for Movies.
- Integrate `VlcPlayerLauncher.kt`.

### Adult Profile (RedTube)
- Implement `RedTubeScraper.kt`.
- Implement `AdultHomeScreen.kt` and `AdultDetailScreen.kt`.

### YouTube & Shorts Feature
- Implement `YoutubeHomeScreen.kt` (Placeholder for now).
- Implement `SwipeableProfileHost.kt` for Instagram-style swipe to shorts.
- Implement `ShortsScreen.kt` with `VerticalPager`.
- Implement `ShortsViewModel.kt` to feed shorts from context (YouTube/Adult).

## Verification Plan

### Automated Tests
- I'll try to run `./gradlew assembleDebug` to ensure the project builds correctly.
- If possible, I'll add unit tests for the Scrapers and Repositories.

### Manual Verification
- I will use `render_compose_preview` to verify the UI components individually.
- I will use `deploy` to run the app on an emulator/device and verify:
    - Navigation between screens.
    - Profile switching gestures.
    - Search functionality (mocked or real API).
    - Passcode screen logic.
    - Swipe to shorts gesture.
