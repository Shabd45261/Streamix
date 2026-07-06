# Implementation Plan - Movies Profile Extensions & Search (Vegamovies)

The goal is to integrate VegaMovies as the primary and only scraper for the Movies profile, replacing the previous planned providers (CastleTV and MovieBox).

## Proposed Changes

### [Cloudstream Scraper Component]

#### [NEW] [VegaMoviesProvider](file:///D:/Android/Streamix/app/src/main/java/com/streamix/scraper/cloudstream/providers/movies/VegaMoviesProvider.kt)
- Ported from `D:\Android\Streamix\Extensions\Movies\CSX\VegaMovies`.
- Implements `getMainPage`, `search`, `load`, and `loadLinks`.
- Includes `VCloud` extractor logic for link resolution.
- Adapted for Streamix's `MainAPI` and `dataUrl` storage (serializes links for the Movies profile UI).

#### [ProviderRegistry.kt](file:///D:/Android/Streamix/app/src/main/java/com/streamix/scraper/cloudstream/ProviderRegistry.kt)
- Register `VegaMoviesProvider`.
- Removed `CastleTvProvider`, `MovieBoxProvider`, `MovieBoxProviderIN`, and `MovieBoxPhisherProvider`.

---

### [Data Scraper Component]

#### [MoviesScraperRepository](file:///D:/Android/Streamix/app/src/main/java/com/streamix/data/scraper/MoviesScraperRepository.kt)
- Updated to use `VegaMoviesProvider` exclusively for home data and search.
- Removed fallback logic between multiple providers as VegaMovies is now the sole provider for this profile.

#### [VegamoviesScraper](file:///D:/Android/Streamix/app/src/main/java/com/streamix/scraper/moviebox/VegamoviesScraper.kt)
- Updated to wrap the new `VegaMoviesProvider` for consistency in the global search.

---

### [Movies UI Component]

#### [MoviesDetailViewModel.kt](file:///D:/Android/Streamix/app/src/main/java/com/streamix/ui/movies/MoviesDetailViewModel.kt)
- Updated default `apiName` to "VegaMovies".
- Simplified fallback logic (searches for alternate links within VegaMovies if primary load fails).

#### [SearchViewModel.kt](file:///D:/Android/Streamix/app/src/main/java/com/streamix/ui/search/SearchViewModel.kt)
- Replaced `CastleTvScraper` with `VegamoviesScraper`.

---

### [Build System]

#### [build.gradle.kts](file:///D:/Android/Streamix/app/build.gradle.kts)
- Update `versionCode` to `200`.

## Verification Plan

### Manual Verification
1. **Homescreen**: Verify that `VegaMovies` content (Home, Netflix, etc.) is displayed.
2. **Search**: Search for "Inception" and verify results from VegaMovies.
3. **Playback**: Play a movie. Verify that `V-Cloud` links are fetched and playable.
4. **Build**: Run `./gradlew assembleDebug` and verify the APK is created: `StreamixPro_20260630_200.apk`.

### Build APK Command
```powershell
./gradlew assembleDebug
# The APK should be copied to the /apk folder by the 'copyApkToRoot' task
# Or I can manually copy it:
# cp app/build/outputs/apk/debug/app-debug.apk apk/Streamix_20260630_200.apk
```
