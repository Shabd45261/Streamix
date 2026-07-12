package com.streamix.core.network

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.streamix.core.model.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepository @Inject constructor(
    private val updateApiService: UpdateApiService,
    @ApplicationContext private val context: Context
) {
    private val updateMetadataUrl = "https://raw.githubusercontent.com/Shabd45261/Streamix.github.io/main/update.json"

    suspend fun getUpdateInfo(): Result<UpdateInfo> {
        var lastError: Exception? = null
        repeat(3) { attempt ->
            try {
                return Result.success(updateApiService.getUpdateInfo(updateMetadataUrl))
            } catch (e: Exception) {
                lastError = e
                if (attempt < 2) delay(10000)
            }
        }
        return Result.failure(lastError ?: Exception("Unknown error"))
    }

    fun downloadApk(url: String, versionName: String): Long {
        val fileName = "Streamix_v$versionName.apk"
        // Ensure old file is deleted before starting new download to avoid (1), (2) suffixes
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (file.exists()) file.delete()

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Downloading Streamix Update v$versionName")
            .setDescription("Please wait while the update is downloading...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            .setMimeType("application/vnd.android.package-archive")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return downloadManager.enqueue(request)
    }

    fun getDownloadProgress(downloadId: Long): Flow<Int> = flow {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        var isDownloading = true
        while (isDownloading) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)
            if (cursor.moveToFirst()) {
                val bytesDownloaded = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val bytesTotal = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))

                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    emit(100)
                    isDownloading = false
                } else if (status == DownloadManager.STATUS_FAILED) {
                    isDownloading = false
                } else if (bytesTotal > 0) {
                    val progress = (bytesDownloaded * 100L / bytesTotal).toInt()
                    emit(progress)
                }
            }
            cursor.close()
            if (isDownloading) delay(500)
        }
    }.flowOn(Dispatchers.IO)
}
