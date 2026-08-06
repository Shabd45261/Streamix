package com.streamix.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.core.model.SearchResult
import com.streamix.core.model.SponsorSegment
import com.streamix.core.storage.PreferencesManager
import com.streamix.data.scraper.MoviesScraperRepository
import com.streamix.scraper.cloudstream.Episode
import com.streamix.scraper.youtube.YouTubeScraper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val moviesRepo: MoviesScraperRepository,
    private val youtubeScraper: YouTubeScraper,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _sponsorSegments = MutableStateFlow<List<SponsorSegment>>(emptyList())
    val sponsorSegments = _sponsorSegments.asStateFlow()

    fun loadSponsorSegments(videoId: String) {
        viewModelScope.launch {
            if (prefs.sponsorBlockEnabled.first()) {
                _sponsorSegments.value = youtubeScraper.getSponsorSegments(videoId)
            } else {
                _sponsorSegments.value = emptyList()
            }
        }
    }

    fun clearSponsorSegments() {
        _sponsorSegments.value = emptyList()
    }

    fun playNextEpisode(episode: Episode, currentVideo: SearchResult) {
        viewModelScope.launch {
            val links = moviesRepo.getVideoLinks(episode.data, currentVideo.studio, episode.name ?: "Episode")
            if (links.isNotEmpty()) {
                PlayerManager.play(
                    video = currentVideo.copy(title = "${currentVideo.title.substringBefore(" - ")} - S${episode.season}E${episode.episode} ${episode.name ?: ""}"),
                    links = links,
                    eps = PlayerManager.episodes.value,
                    episode = episode
                )
            }
        }
    }
}
