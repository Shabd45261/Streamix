package com.streamix.ui.youtube

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

@HiltViewModel
class YoutubeDetailViewModel @Inject constructor() : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _videoTitle = MutableStateFlow("")
    val videoTitle = _videoTitle.asStateFlow()

    private val _description = MutableStateFlow("")
    val description = _description.asStateFlow()

    fun load(videoId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _videoTitle.value = "Video $videoId"
            
            try {
                withContext(Dispatchers.IO) {
                    val url = "https://m.youtube.com/watch?v=$videoId"
                    val doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
                        .get()
                    
                    _videoTitle.value = doc.selectFirst("meta[name='title']")?.attr("content") 
                                       ?: doc.selectFirst("title")?.text() ?: "YouTube Video"
                    _description.value = doc.selectFirst("meta[name='description']")?.attr("content") ?: ""
                }
            } catch (e: Exception) {
                // error
            } finally {
                _isLoading.value = false
            }
        }
    }
}
