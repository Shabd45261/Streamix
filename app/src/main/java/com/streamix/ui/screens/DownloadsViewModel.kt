package com.streamix.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import com.streamix.core.plugin.SearchResult
import com.streamix.core.plugin.StreamLink
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import javax.inject.Inject

enum class DownloadStatus { QUEUED, DOWNLOADING, COMPLETE, FAILED }

data class DownloadItem(
    val id           : String,
    val title        : String,
    val sourceId     : String,
    val posterUrl    : String?,
    val quality      : String,
    val fileSizeLabel: String,
    val status       : DownloadStatus,
    val progressPct  : Float = 0f,
    val localPath    : String? = null
)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads = _downloads.asStateFlow()

    fun deleteDownload(id: String) {
        val item = _downloads.value.find { it.id == id } ?: return
        item.localPath?.let { File(it).delete() }
        _downloads.update { it.filterNot { d -> d.id == id } }
    }

    // Call this from DetailScreen to enqueue a download
    fun enqueue(item: SearchResult, streamLink: StreamLink) {
        val download = DownloadItem(
            id            = item.id,
            title         = item.title,
            sourceId      = item.sourceId,
            posterUrl     = item.posterUrl,
            quality       = streamLink.quality,
            fileSizeLabel = "Calculating…",
            status        = DownloadStatus.QUEUED
        )
        _downloads.update { it + download }
        // TODO: kick off WorkManager download job
    }
}
