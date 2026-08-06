package com.streamix.core

import android.content.Context
import com.streamix.ui.screens.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPrefs by lazy { context.getSharedPreferences("streamix_settings", Context.MODE_PRIVATE) }
    private val gson = com.google.gson.Gson()

    private val _prefs = MutableStateFlow(loadPrefs())
    val prefs = _prefs.asStateFlow()

    private fun loadPrefs(): StreamixPrefs {
        val lastSourcesJson = sharedPrefs.getString("last_selected_sources", null)
        val lastSelectedSources: Map<String, String?> = if (lastSourcesJson != null) {
            try {
                val type = object : com.google.gson.reflect.TypeToken<Map<String, String?>>() {}.type
                gson.fromJson(lastSourcesJson, type)
            } catch (e: Exception) {
                emptyMap()
            }
        } else {
            emptyMap()
        }

        return StreamixPrefs(
            adultPinEnabled      = sharedPrefs.getBoolean("adult_pin", false),
            adultPin             = sharedPrefs.getString("adult_pin_val", "") ?: "",
            defaultQuality       = sharedPrefs.getString("quality", "Auto") ?: "Auto",
            autoplayNext         = sharedPrefs.getBoolean("autoplay", true),
            skipIntro            = sharedPrefs.getBoolean("skip_intro", false),
            subtitlesEnabled     = sharedPrefs.getBoolean("subtitles", false),
            subtitleLanguage     = sharedPrefs.getString("sub_lang", "English") ?: "English",
            autoUpdateExtensions = sharedPrefs.getBoolean("auto_update", true),
            tmdbKey              = sharedPrefs.getString("tmdb_key", "") ?: "",
            wifiOnly             = sharedPrefs.getBoolean("wifi_only", true),
            lastSelectedSources  = lastSelectedSources,
            adultConfirmed       = sharedPrefs.getBoolean("adult_confirmed", false),

            theme                = AppTheme.valueOf(sharedPrefs.getString("theme", AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name),
            accentColor          = sharedPrefs.getString("accent_color", "#7C6AF7") ?: "#7C6AF7",
            secondaryColor       = sharedPrefs.getString("secondary_color", "#4ECDC4") ?: "#4ECDC4",
            autoSecondary        = sharedPrefs.getBoolean("auto_secondary", true),
            font                 = AppFont.valueOf(sharedPrefs.getString("font", AppFont.DEFAULT.name) ?: AppFont.DEFAULT.name),
            fontSize             = FontSize.valueOf(sharedPrefs.getString("font_size", FontSize.MEDIUM.name) ?: FontSize.MEDIUM.name),
            cornerStyle          = CornerStyle.valueOf(sharedPrefs.getString("corner_style", CornerStyle.ROUNDED.name) ?: CornerStyle.ROUNDED.name),
            glassEffectsEnabled  = sharedPrefs.getBoolean("glass_enabled", true),
            glassBlurStrength    = sharedPrefs.getFloat("glass_blur", 1.0f),

            posterStyle          = PosterStyle.valueOf(sharedPrefs.getString("poster_style", PosterStyle.GRID.name) ?: PosterStyle.GRID.name),
            posterShape          = PosterShape.valueOf(sharedPrefs.getString("poster_shape", PosterShape.STANDARD.name) ?: PosterShape.STANDARD.name),
            cardDensity          = CardDensity.valueOf(sharedPrefs.getString("card_density", CardDensity.BALANCED.name) ?: CardDensity.BALANCED.name),

            homeLayout           = HomeLayout.valueOf(sharedPrefs.getString("home_layout", HomeLayout.MODERN.name) ?: HomeLayout.MODERN.name),
            heroBannerEnabled    = sharedPrefs.getBoolean("hero_banner", true),
            autoRotateBanner     = sharedPrefs.getBoolean("auto_rotate_banner", true),
            continueWatchingPosition = ContinueWatchingPosition.valueOf(sharedPrefs.getString("cw_position", ContinueWatchingPosition.TOP.name) ?: ContinueWatchingPosition.TOP.name),
            enabledHomeSections  = sharedPrefs.getStringSet("home_sections", setOf("Trending", "Popular", "New Releases", "Top Rated", "Recommended", "Anime", "TV Shows", "Adult Profile", "Kids Profile")) ?: emptySet(),

            defaultLibraryView   = LibraryView.valueOf(sharedPrefs.getString("library_view", LibraryView.MOVIES.name) ?: LibraryView.MOVIES.name),
            librarySortBy        = LibrarySort.valueOf(sharedPrefs.getString("library_sort", LibrarySort.RECENTLY_ADDED.name) ?: LibrarySort.RECENTLY_ADDED.name),
            libraryCollectionStyle = CollectionStyle.valueOf(sharedPrefs.getString("library_style", CollectionStyle.GRID.name) ?: CollectionStyle.GRID.name),

            searchLayout         = SearchLayout.valueOf(sharedPrefs.getString("search_layout", SearchLayout.GRID.name) ?: SearchLayout.GRID.name),
            searchSuggestionsEnabled = sharedPrefs.getBoolean("search_suggestions", true),
            recentSearchesEnabled = sharedPrefs.getBoolean("recent_searches", true),

            playerTheme          = PlayerTheme.valueOf(sharedPrefs.getString("player_theme", PlayerTheme.PREMIUM.name) ?: PlayerTheme.PREMIUM.name),
            subtitleStyle        = SubtitleStyle.valueOf(sharedPrefs.getString("subtitle_style", SubtitleStyle.NETFLIX.name) ?: SubtitleStyle.NETFLIX.name),
            subtitleFont         = sharedPrefs.getString("subtitle_font", "Default") ?: "Default",
            subtitleSize         = sharedPrefs.getInt("subtitle_size", 18),
            subtitleColor        = sharedPrefs.getString("subtitle_color", "#FFFFFF") ?: "#FFFFFF",
            subtitleBackground   = sharedPrefs.getString("subtitle_bg", "#000000") ?: "#000000",
            subtitleOpacity      = sharedPrefs.getFloat("subtitle_opacity", 0.5f),
            subtitlePosition     = sharedPrefs.getInt("subtitle_pos", 90),
            subtitleDelay        = sharedPrefs.getLong("subtitle_delay", 0L),
            audioDelay           = sharedPrefs.getLong("audio_delay", 0L),
            skipForwardInterval  = sharedPrefs.getInt("skip_forward", 10),
            skipBackwardInterval = sharedPrefs.getInt("skip_backward", 10),
            playbackSpeedPresets = sharedPrefs.getStringSet("speed_presets", setOf("0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "1.75x", "2.0x", "3.0x")) ?: emptySet(),
            rememberSpeed        = sharedPrefs.getBoolean("remember_speed", true),
            lastSpeed            = sharedPrefs.getFloat("last_speed", 1.0f),
            doubleTapSeekEnabled = sharedPrefs.getBoolean("double_tap_seek", true),
            longPressSpeedEnabled = sharedPrefs.getBoolean("long_press_speed", true),
            aspectRatio          = VideoAspectRatio.valueOf(sharedPrefs.getString("aspect_ratio", VideoAspectRatio.FIT.name) ?: VideoAspectRatio.FIT.name),
            brightnessBoost      = sharedPrefs.getBoolean("brightness_boost", false),
            volumeBoost          = sharedPrefs.getInt("volume_boost", 0),
            showStats            = sharedPrefs.getBoolean("show_stats", false),
            animeModeEnabled     = sharedPrefs.getBoolean("anime_mode", true),
            adultPanicEnabled    = sharedPrefs.getBoolean("adult_panic", true),

            bottomNavStyle       = NavStyle.valueOf(sharedPrefs.getString("nav_style", NavStyle.GLASS_DOCK.name) ?: NavStyle.GLASS_DOCK.name),
            navPosition          = NavPosition.valueOf(sharedPrefs.getString("nav_position", NavPosition.BOTTOM.name) ?: NavPosition.BOTTOM.name),
            hiddenTabs           = sharedPrefs.getStringSet("hidden_tabs", emptySet()) ?: emptySet(),

            backdropUsage        = BackdropUsage.valueOf(sharedPrefs.getString("backdrop_usage", BackdropUsage.FULL.name) ?: BackdropUsage.FULL.name),
            artworkQuality       = ArtworkQuality.valueOf(sharedPrefs.getString("artwork_quality", ArtworkQuality.MEDIUM.name) ?: ArtworkQuality.MEDIUM.name),
            posterAnimation      = AnimationLevel.valueOf(sharedPrefs.getString("poster_animation", AnimationLevel.REDUCED.name) ?: AnimationLevel.REDUCED.name),

            imageQuality         = ArtworkQuality.valueOf(sharedPrefs.getString("image_quality", ArtworkQuality.MEDIUM.name) ?: ArtworkQuality.MEDIUM.name),
            animationLevel       = AnimationLevel.valueOf(sharedPrefs.getString("animation_level", AnimationLevel.FULL.name) ?: AnimationLevel.FULL.name),
            autoPlayTrailers     = TrailerAutoPlay.valueOf(sharedPrefs.getString("auto_play_trailers", TrailerAutoPlay.WIFI_ONLY.name) ?: TrailerAutoPlay.WIFI_ONLY.name)
        )
    }

    fun getRecentSearches(): List<String> =
        sharedPrefs.getStringSet("recent_searches_list", emptySet())?.toList() ?: emptyList()

    fun saveRecentSearches(list: List<String>) =
        sharedPrefs.edit().putStringSet("recent_searches_list", list.toSet()).apply()

    fun updatePrefs(transform: (StreamixPrefs) -> StreamixPrefs) {
        val newPrefs = transform(_prefs.value)
        savePrefs(newPrefs)
        _prefs.value = newPrefs
    }

    private fun savePrefs(p: StreamixPrefs) {
        sharedPrefs.edit().apply {
            putBoolean("adult_pin", p.adultPinEnabled)
            putString("adult_pin_val", p.adultPin)
            putString("quality", p.defaultQuality)
            putBoolean("autoplay", p.autoplayNext)
            putBoolean("skip_intro", p.skipIntro)
            putBoolean("subtitles", p.subtitlesEnabled)
            putString("sub_lang", p.subtitleLanguage)
            putBoolean("auto_update", p.autoUpdateExtensions)
            putString("tmdb_key", p.tmdbKey)
            putBoolean("wifi_only", p.wifiOnly)

            putString("theme", p.theme.name)
            putString("accent_color", p.accentColor)
            putString("secondary_color", p.secondaryColor)
            putBoolean("auto_secondary", p.autoSecondary)
            putString("font", p.font.name)
            putString("font_size", p.fontSize.name)
            putString("corner_style", p.cornerStyle.name)
            putBoolean("glass_enabled", p.glassEffectsEnabled)
            putFloat("glass_blur", p.glassBlurStrength)

            putString("poster_style", p.posterStyle.name)
            putString("poster_shape", p.posterShape.name)
            putString("card_density", p.cardDensity.name)

            putString("home_layout", p.homeLayout.name)
            putBoolean("hero_banner", p.heroBannerEnabled)
            putBoolean("auto_rotate_banner", p.autoRotateBanner)
            putString("cw_position", p.continueWatchingPosition.name)
            putStringSet("home_sections", p.enabledHomeSections)

            putString("library_view", p.defaultLibraryView.name)
            putString("library_sort", p.librarySortBy.name)
            putString("library_style", p.libraryCollectionStyle.name)

            putString("search_layout", p.searchLayout.name)
            putBoolean("search_suggestions", p.searchSuggestionsEnabled)
            putBoolean("recent_searches", p.recentSearchesEnabled)

            putString("player_theme", p.playerTheme.name)
            putString("subtitle_style", p.subtitleStyle.name)
            putString("subtitle_font", p.subtitleFont)
            putInt("subtitle_size", p.subtitleSize)
            putString("subtitle_color", p.subtitleColor)
            putString("subtitle_bg", p.subtitleBackground)
            putFloat("subtitle_opacity", p.subtitleOpacity)
            putInt("subtitle_pos", p.subtitlePosition)
            putLong("subtitle_delay", p.subtitleDelay)
            putLong("audio_delay", p.audioDelay)
            putInt("skip_forward", p.skipForwardInterval)
            putInt("skip_backward", p.skipBackwardInterval)
            putStringSet("speed_presets", p.playbackSpeedPresets)
            putBoolean("remember_speed", p.rememberSpeed)
            putFloat("last_speed", p.lastSpeed)
            putBoolean("double_tap_seek", p.doubleTapSeekEnabled)
            putBoolean("long_press_speed", p.longPressSpeedEnabled)
            putString("aspect_ratio", p.aspectRatio.name)
            putBoolean("brightness_boost", p.brightnessBoost)
            putInt("volume_boost", p.volumeBoost)
            putBoolean("show_stats", p.showStats)
            putBoolean("anime_mode", p.animeModeEnabled)
            putBoolean("adult_panic", p.adultPanicEnabled)

            putString("nav_style", p.bottomNavStyle.name)
            putString("nav_position", p.navPosition.name)
            putStringSet("hidden_tabs", p.hiddenTabs)

            putString("backdrop_usage", p.backdropUsage.name)
            putString("artwork_quality", p.artworkQuality.name)
            putString("poster_animation", p.posterAnimation.name)

            putString("image_quality", p.imageQuality.name)
            putString("animation_level", p.animationLevel.name)
            putString("auto_play_trailers", p.autoPlayTrailers.name)
            putString("last_selected_sources", gson.toJson(p.lastSelectedSources))
            putBoolean("adult_confirmed", p.adultConfirmed)
        }.apply()
    }
}
