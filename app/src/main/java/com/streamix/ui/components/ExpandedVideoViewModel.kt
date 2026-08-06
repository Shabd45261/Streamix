package com.streamix.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.core.model.SearchResult
import com.streamix.core.model.VideoLink
import com.streamix.data.scraper.AdultScraperRepository
import com.streamix.data.scraper.MoviesScraperRepository
import com.streamix.scraper.youtube.YouTubeScraper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExpandedVideoViewModel @Inject constructor(
    private val youtubeScraper: YouTubeScraper,
    private val moviesRepo: MoviesScraperRepository,
    private val adultRepo: AdultScraperRepository
) : ViewModel() {

    private val _previewLinks = MutableStateFlow<List<VideoLink>>(emptyList())
    val previewLinks = _previewLinks.asStateFlow()

    fun loadPreview(item: SearchResult) {
        _previewLinks.value = emptyList() // Clear old preview immediately
        viewModelScope.launch {
            try {
                when (item.mediaType) {
                    "youtube" -> {
                        _previewLinks.value = youtubeScraper.getVideoLinks(item.id, "youtube")
                    }
                    "adult" -> {
                        _previewLinks.value = adultRepo.getVideoLinks(item.id)
                    }
                    else -> { // Movie / TV
                        val detail = moviesRepo.getDetail(item.id, item.studio.ifBlank { "MovieBoxPh" })
                        if (detail != null && detail.trailers.isNotEmpty()) {
                            val trailer = detail.trailers.first()
                            val ytId = if (trailer.extractorUrl.contains("v=")) {
                                trailer.extractorUrl.substringAfter("v=").substringBefore("&")
                            } else if (trailer.extractorUrl.contains("youtu.be/")) {
                                trailer.extractorUrl.substringAfter("youtu.be/").substringBefore("?")
                            } else {
                                trailer.extractorUrl
                            }
                            _previewLinks.value = youtubeScraper.getVideoLinks(ytId, "youtube")
                        } else if (detail != null) {
                            // Search for trailer if none provided
                            val ytScraper = com.streamix.scraper.youtube.YouTubeScraper()
                            val searchResults = ytScraper.search("${detail.name} official trailer")
                            val firstResult = searchResults.firstOrNull()
                            if (firstResult != null) {
                                _previewLinks.value = ytScraper.getVideoLinks(firstResult.id, "youtube")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _previewLinks.value = emptyList()
            }
        }
    }

    fun clearPreview() {
        _previewLinks.value = emptyList()
    }
}
