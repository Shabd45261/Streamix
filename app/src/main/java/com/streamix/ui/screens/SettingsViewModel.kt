package com.streamix.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import com.streamix.core.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

enum class AppTheme { LIGHT, DARK, SYSTEM, AMOLED }
enum class AppFont { DEFAULT, MODERN, ROUNDED, SERIF, MONOSPACE }
enum class FontSize { SMALL, MEDIUM, LARGE, EXTRA_LARGE }
enum class CornerStyle { SHARP, ROUNDED, EXTRA_ROUNDED }
enum class PosterStyle { GRID, CARD, LIST, COMPACT_GRID }
enum class PosterShape { STANDARD, SQUARE, ROUNDED, MINIMAL }
enum class CardDensity { MINIMAL, BALANCED, DETAILED }
enum class HomeLayout { MODERN, CLASSIC, NETFLIX, MINIMAL }
enum class ContinueWatchingPosition { TOP, MIDDLE, BOTTOM }
enum class LibraryView { MOVIES, SHOWS, ANIME, FAVORITES }
enum class LibrarySort { RECENTLY_ADDED, RATING, YEAR, NAME, RUNTIME }
enum class CollectionStyle { GRID, LIST, SHELF }
enum class SearchLayout { GRID, LIST, COMPACT }
enum class PlayerTheme { DEFAULT, MINIMAL, MODERN, PREMIUM }
enum class SubtitleStyle { NETFLIX, YOUTUBE, CUSTOM, ANIME, MINIMAL }
enum class NavStyle { ICONS, ICONS_LABELS, COMPACT, GLASS_DOCK }
enum class NavPosition { BOTTOM, SIDE }
enum class BackdropUsage { FULL, BLURRED, POSTER_ONLY }
enum class ArtworkQuality { LOW, MEDIUM, HIGH }
enum class AnimationLevel { NONE, REDUCED, FULL }
enum class TrailerAutoPlay { OFF, WIFI_ONLY, ALWAYS }
enum class VideoAspectRatio { FIT, FILL, STRETCH, SIXTEEN_NINE, FOUR_THREE }

data class StreamixPrefs(
    val adultPinEnabled      : Boolean = false,
    val adultPin             : String  = "",
    val defaultQuality       : String  = "Auto",
    val autoplayNext         : Boolean = true,
    val skipIntro            : Boolean = false,
    val subtitlesEnabled     : Boolean = false,
    val subtitleLanguage     : String  = "English",
    val autoUpdateExtensions : Boolean = true,
    val tmdbKey              : String  = "",
    val wifiOnly             : Boolean = true,
    val lastSelectedSources  : Map<String, String?> = emptyMap(),
    val adultConfirmed       : Boolean = false,

    // Appearance
    val theme: AppTheme = AppTheme.SYSTEM,
    val accentColor: String = "#7C6AF7",
    val secondaryColor: String = "#4ECDC4",
    val autoSecondary: Boolean = true,
    val font: AppFont = AppFont.DEFAULT,
    val fontSize: FontSize = FontSize.MEDIUM,
    val cornerStyle: CornerStyle = CornerStyle.ROUNDED,
    val glassEffectsEnabled: Boolean = true,
    val glassBlurStrength: Float = 1.0f,

    // Content Layout
    val posterStyle: PosterStyle = PosterStyle.GRID,
    val posterShape: PosterShape = PosterShape.STANDARD,
    val cardDensity: CardDensity = CardDensity.BALANCED,

    // Home Screen
    val homeLayout: HomeLayout = HomeLayout.MODERN,
    val heroBannerEnabled: Boolean = true,
    val autoRotateBanner: Boolean = true,
    val continueWatchingPosition: ContinueWatchingPosition = ContinueWatchingPosition.TOP,
    val enabledHomeSections: Set<String> = setOf("Trending", "Popular", "New Releases", "Top Rated", "Recommended", "Anime", "TV Shows", "Adult Profile", "Kids Profile"),

    // Library
    val defaultLibraryView: LibraryView = LibraryView.MOVIES,
    val librarySortBy: LibrarySort = LibrarySort.RECENTLY_ADDED,
    val libraryCollectionStyle: CollectionStyle = CollectionStyle.GRID,

    // Search
    val searchLayout: SearchLayout = SearchLayout.GRID,
    val searchSuggestionsEnabled: Boolean = true,
    val recentSearchesEnabled: Boolean = true,

    // Player
    val playerTheme: PlayerTheme = PlayerTheme.PREMIUM,
    val subtitleStyle: SubtitleStyle = SubtitleStyle.NETFLIX,
    val subtitleFont: String = "Default",
    val subtitleSize: Int = 18,
    val subtitleColor: String = "#FFFFFF",
    val subtitleBackground: String = "#000000",
    val subtitleOpacity: Float = 0.5f,
    val subtitlePosition: Int = 90,
    val subtitleDelay: Long = 0,
    val audioDelay: Long = 0,
    val skipForwardInterval: Int = 10,
    val skipBackwardInterval: Int = 10,
    val playbackSpeedPresets: Set<String> = setOf("0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "1.75x", "2.0x", "3.0x"),
    val rememberSpeed: Boolean = true,
    val lastSpeed: Float = 1.0f,
    val doubleTapSeekEnabled: Boolean = true,
    val longPressSpeedEnabled: Boolean = true,
    val aspectRatio: VideoAspectRatio = VideoAspectRatio.FIT,
    val brightnessBoost: Boolean = false,
    val volumeBoost: Int = 0,
    val showStats: Boolean = false,
    val animeModeEnabled: Boolean = true,
    val adultPanicEnabled: Boolean = true,

    // Navigation
    val bottomNavStyle: NavStyle = NavStyle.GLASS_DOCK,
    val navPosition: NavPosition = NavPosition.BOTTOM,
    val hiddenTabs: Set<String> = emptySet(),

    // Artwork
    val backdropUsage: BackdropUsage = BackdropUsage.FULL,
    val artworkQuality: ArtworkQuality = ArtworkQuality.MEDIUM,
    val posterAnimation: AnimationLevel = AnimationLevel.REDUCED,

    // Performance
    val imageQuality: ArtworkQuality = ArtworkQuality.MEDIUM,
    val animationLevel: AnimationLevel = AnimationLevel.FULL,
    val autoPlayTrailers: TrailerAutoPlay = TrailerAutoPlay.WIFI_ONLY
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    val prefs: StateFlow<StreamixPrefs> = preferenceManager.prefs

    fun setTheme(v: AppTheme) = preferenceManager.updatePrefs { it.copy(theme = v) }
    fun setAccentColor(v: String) = preferenceManager.updatePrefs { it.copy(accentColor = v) }
    fun setSecondaryColor(v: String) = preferenceManager.updatePrefs { it.copy(secondaryColor = v) }
    fun setAutoSecondary(v: Boolean) = preferenceManager.updatePrefs { it.copy(autoSecondary = v) }
    fun setFont(v: AppFont) = preferenceManager.updatePrefs { it.copy(font = v) }
    fun setFontSize(v: FontSize) = preferenceManager.updatePrefs { it.copy(fontSize = v) }
    fun setCornerStyle(v: CornerStyle) = preferenceManager.updatePrefs { it.copy(cornerStyle = v) }

    fun setPosterStyle(v: PosterStyle) = preferenceManager.updatePrefs { it.copy(posterStyle = v) }
    fun setPosterShape(v: PosterShape) = preferenceManager.updatePrefs { it.copy(posterShape = v) }
    fun setCardDensity(v: CardDensity) = preferenceManager.updatePrefs { it.copy(cardDensity = v) }

    fun setHomeLayout(v: HomeLayout) = preferenceManager.updatePrefs { it.copy(homeLayout = v) }
    fun setHeroBanner(v: Boolean) = preferenceManager.updatePrefs { it.copy(heroBannerEnabled = v) }
    fun setAutoRotateBanner(v: Boolean) = preferenceManager.updatePrefs { it.copy(autoRotateBanner = v) }
    fun setCWPosition(v: ContinueWatchingPosition) = preferenceManager.updatePrefs { it.copy(continueWatchingPosition = v) }
    fun setHomeSections(v: Set<String>) = preferenceManager.updatePrefs { it.copy(enabledHomeSections = v) }

    fun setLibraryView(v: LibraryView) = preferenceManager.updatePrefs { it.copy(defaultLibraryView = v) }
    fun setLibrarySort(v: LibrarySort) = preferenceManager.updatePrefs { it.copy(librarySortBy = v) }
    fun setLibraryStyle(v: CollectionStyle) = preferenceManager.updatePrefs { it.copy(libraryCollectionStyle = v) }

    fun setSearchLayout(v: SearchLayout) = preferenceManager.updatePrefs { it.copy(searchLayout = v) }
    fun setSearchSuggestions(v: Boolean) = preferenceManager.updatePrefs { it.copy(searchSuggestionsEnabled = v) }
    fun setRecentSearches(v: Boolean) = preferenceManager.updatePrefs { it.copy(recentSearchesEnabled = v) }

    fun setPlayerTheme(v: PlayerTheme) = preferenceManager.updatePrefs { it.copy(playerTheme = v) }
    fun setSubtitleStyle(v: SubtitleStyle) = preferenceManager.updatePrefs { it.copy(subtitleStyle = v) }
    fun setSubtitleFont(v: String) = preferenceManager.updatePrefs { it.copy(subtitleFont = v) }
    fun setSubtitleSize(v: Int) = preferenceManager.updatePrefs { it.copy(subtitleSize = v) }
    fun setSubtitleColor(v: String) = preferenceManager.updatePrefs { it.copy(subtitleColor = v) }
    fun setSubtitleBackground(v: String) = preferenceManager.updatePrefs { it.copy(subtitleBackground = v) }
    fun setSubtitleOpacity(v: Float) = preferenceManager.updatePrefs { it.copy(subtitleOpacity = v) }
    fun setSkipForward(v: Int) = preferenceManager.updatePrefs { it.copy(skipForwardInterval = v) }
    fun setSkipBackward(v: Int) = preferenceManager.updatePrefs { it.copy(skipBackwardInterval = v) }
    fun setSpeedPresets(v: Set<String>) = preferenceManager.updatePrefs { it.copy(playbackSpeedPresets = v) }

    fun setNavStyle(v: NavStyle) = preferenceManager.updatePrefs { it.copy(bottomNavStyle = v) }
    fun setNavPosition(v: NavPosition) = preferenceManager.updatePrefs { it.copy(navPosition = v) }
    fun setHiddenTabs(v: Set<String>) = preferenceManager.updatePrefs { it.copy(hiddenTabs = v) }

    fun setBackdropUsage(v: BackdropUsage) = preferenceManager.updatePrefs { it.copy(backdropUsage = v) }
    fun setArtworkQuality(v: ArtworkQuality) = preferenceManager.updatePrefs { it.copy(artworkQuality = v) }
    fun setPosterAnimation(v: AnimationLevel) = preferenceManager.updatePrefs { it.copy(posterAnimation = v) }

    fun setImageQuality(v: ArtworkQuality) = preferenceManager.updatePrefs { it.copy(imageQuality = v) }
    fun setAnimationLevel(v: AnimationLevel) = preferenceManager.updatePrefs { it.copy(animationLevel = v) }
    fun setTrailerAutoPlay(v: TrailerAutoPlay) = preferenceManager.updatePrefs { it.copy(autoPlayTrailers = v) }

    fun setAdultPin(v: Boolean) = preferenceManager.updatePrefs { it.copy(adultPinEnabled = v) }
    fun setAdultPinValue(v: String) = preferenceManager.updatePrefs { it.copy(adultPin = v) }
    fun setAdultConfirmed(v: Boolean) = preferenceManager.updatePrefs { it.copy(adultConfirmed = v) }
    fun setDefaultQuality(v: String) = preferenceManager.updatePrefs { it.copy(defaultQuality = v) }
    fun setAutoplay(v: Boolean) = preferenceManager.updatePrefs { it.copy(autoplayNext = v) }
    fun setSkipIntro(v: Boolean) = preferenceManager.updatePrefs { it.copy(skipIntro = v) }
    fun setSubtitles(v: Boolean) = preferenceManager.updatePrefs { it.copy(subtitlesEnabled = v) }
    fun setAutoUpdate(v: Boolean) = preferenceManager.updatePrefs { it.copy(autoUpdateExtensions = v) }
    fun setWifiOnly(v: Boolean) = preferenceManager.updatePrefs { it.copy(wifiOnly = v) }

    fun setAspectRatio(v: VideoAspectRatio) = preferenceManager.updatePrefs { it.copy(aspectRatio = v) }

    fun getCacheSizeMb(): Int {
        val cacheDir = context.cacheDir
        return (cacheDir.walkTopDown().sumOf { if (it.isFile) it.length() else 0L } / (1024 * 1024)).toInt()
    }

    fun clearCache() { context.cacheDir.walkTopDown().forEach { if (it.isFile) it.delete() } }
}
