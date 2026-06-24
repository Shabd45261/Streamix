# Streamix Overhaul: Adult Extensions & Cloudstream UI/UX

Overhaul the Adult profile in Streamix by replacing existing scrapers with a curated list from Gizlikeyif and CXXX, enhancing the UI/UX based on Cloudstream's design, and adding a new okxxx1.com scraper.

## User Review Required

> [!IMPORTANT]
> - **Scraper Removal**: All existing scrapers in `com.streamix.scraper.cloudstream.providers` will be removed, except those identified as being used by the Shorts section.
> - **Cloudstream UI**: Replicating the full Cloudstream UI (Fragment-based) in Streamix (Compose-based) will be an approximation focused on the "look and feel" and specific features like the player and theme settings.
> - **Extensions Source**: Some extensions in the provided folders might have complex dependencies (e.g., custom extractors). I will attempt to port the necessary extraction logic.

## Proposed Changes

### Core Scraper Infrastructure
Enhance the `MainAPI` system to support Cloudstream-style extension features.

#### [MainAPI.kt](file:///D:/Android/Streamix/app/src/main/java/com/streamix/scraper/cloudstream/MainAPI.kt)
- Add missing enums: `ExtractorLinkType`, `SearchQuality`, `ProviderType`, `VPNStatus`.
- Add data classes: `TorrentSearchResponse`, `MovieSearchResponse`, `LiveSearchResponse`, `TvSeriesSearchResponse`, `AnimeSearchResponse`, `ActorData`, `Actor`, `TrailerData`.
- Update `LoadResponse` and `SearchResponse` interfaces to match Cloudstream's structure.
- Add helper functions like `newMovieSearchResponse`, `newMovieLoadResponse`, `newExtractorLink`.

#### [NiceHttpHelper.kt](file:///D:/Android/Streamix/app/src/main/java/com/streamix/scraper/cloudstream/NiceHttpHelper.kt)
- Ensure it supports the networking needs of the new extensions.

---

### Extensions Implementation
Import and adapt the specified extensions.

#### [NEW] [Gizlikeyif Providers](file:///D:/Android/Streamix/app/src/main/java/com/streamix/scraper/cloudstream/providers/gizlikeyif/)
- Implement: `Chaturbate`, `CollectionOfBestPorn`, `Fapix`, `HanimeTV`, `İnfluencerChicks`, `LiveCamRips`, `NetFapX`, `PerfectGirls`, `PornHat`.

#### [NEW] [CXXX Providers](file:///D:/Android/Streamix/app/src/main/java/com/streamix/scraper/cloudstream/providers/cxxx/)
- Implement: `Chatrubate`, `FreePornVideos`, `HahoMoe`, `HentaiCity`, `HStream`, `InternetChicks`, `NoodleMagazine`, `Onlyjerk`, `Paradisehill`, `Pornhoarder`, `Pornobae`, `Xhamster`, `XNXX`, `Xvideos`, `YesPornPlease`.

#### [NEW] [OkxxxProvider.kt](file:///D:/Android/Streamix/app/src/main/java/com/streamix/scraper/cloudstream/providers/OkxxxProvider.kt)
- Create a replica of `PornHatProvider` for `okxxx1.com`.

---

### Repository and ViewModels
Connect the new scrapers to the UI.

#### [AdultScraperRepository.kt](file:///D:/Android/Streamix/app/src/main/java/com/streamix/data/scraper/AdultScraperRepository.kt)
- Update to use the new `MainAPI` providers.
- Implement logic for getting trending from `PornHat`.
- Implement global search across all providers.

#### [AdultHomeViewModel.kt](file:///D:/Android/Streamix/app/src/main/java/com/streamix/ui/adult/AdultHomeViewModel.kt)
- Restrict home screen content to `PornHat`.
- Ensure search uses the updated repository.

#### [ShortsViewModel.kt](file:///D:/Android/Streamix/app/src/main/java/com/streamix/ui/shorts/ShortsViewModel.kt)
- Update to randomly pull videos from all enabled adult providers.

---

### UI/UX Enhancement
Enhance Streamix based on Cloudstream's design.

#### [Theme.kt](file:///D:/Android/Streamix/app/src/main/java/com/streamix/ui/theme/Theme.kt) / [ThemeViewModel.kt](file:///D:/Android/Streamix/app/src/main/java/com/streamix/ui/theme/ThemeViewModel.kt)
- Refine color palette and theme switching logic to match Cloudstream's AMOLED black and accented style.

#### [ExoPlayerScreen.kt](file:///D:/Android/Streamix/app/src/main/java/com/streamix/ui/player/ExoPlayerScreen.kt)
- Add UI elements for quality selection (if supported by links) and more advanced controls.

## Verification Plan

### Automated Tests
- N/A (Unit tests for scrapers are difficult due to network dependency, but I will perform manual verification via Logcat).

### Manual Verification
1.  **Launch Adult Profile**: Verify home screen shows ONLY `PornHat` videos.
2.  **Search**: Perform a search and verify results come from multiple providers.
3.  **Playback**: Test video playback for various providers.
4.  **Shorts**: Swipe through shorts and verify they come from different sources randomly.
5.  **Theme**: Toggle theme settings and verify UI updates correctly.
6.  **Build APK**: Run `./gradlew assembleDebug` and verify APK is created in `apk/` folder with the correct name format.
