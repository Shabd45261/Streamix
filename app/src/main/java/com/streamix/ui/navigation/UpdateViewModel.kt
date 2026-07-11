package com.streamix.ui.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.BuildConfig
import com.streamix.core.model.UpdateInfo
import com.streamix.core.network.UpdateRepository
import com.streamix.core.storage.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed class UpdateUIState {
    object Idle : UpdateUIState()
    object Checking : UpdateUIState()
    data class UpdateAvailable(val info: UpdateInfo) : UpdateUIState()
    object UpToDate : UpdateUIState()
    data class Error(val message: String) : UpdateUIState()
    data class Downloading(val progress: Int) : UpdateUIState()
    data class DownloadCompleted(val file: File) : UpdateUIState()
}

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val repository: UpdateRepository,
    private val prefs: PreferencesManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<UpdateUIState>(UpdateUIState.Idle)
    val uiState: StateFlow<UpdateUIState> = _uiState

    fun checkForUpdates(isAuto: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = UpdateUIState.Checking
            repository.getUpdateInfo().onSuccess { info ->
                prefs.setLastUpdateCheck(System.currentTimeMillis())
                if (info.latestVersion > BuildConfig.VERSION_CODE) {
                    val ignored = prefs.ignoredVersion.first()
                    if (info.latestVersion != ignored || info.mandatory) {
                        _uiState.value = UpdateUIState.UpdateAvailable(info)
                    } else {
                        _uiState.value = UpdateUIState.Idle
                    }
                } else {
                    _uiState.value = UpdateUIState.UpToDate
                }
            }.onFailure {
                if (!isAuto) {
                    _uiState.value = UpdateUIState.Error(it.message ?: "Failed to check for updates")
                } else {
                    _uiState.value = UpdateUIState.Idle
                }
            }
        }
    }

    fun downloadUpdate(info: UpdateInfo) {
        if (_uiState.value is UpdateUIState.Downloading) return
        
        viewModelScope.launch {
            _uiState.value = UpdateUIState.Downloading(0)
            val downloadId = repository.downloadApk(info.apkUrl, info.versionName)
            repository.getDownloadProgress(downloadId).collect { progress ->
                if (progress < 100) {
                    _uiState.value = UpdateUIState.Downloading(progress)
                } else {
                    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Streamix_v${info.versionName}.apk")
                    _uiState.value = UpdateUIState.DownloadCompleted(file)
                }
            }
        }
    }

    fun ignoreUpdate(version: Int) {
        viewModelScope.launch {
            prefs.setIgnoredVersion(version)
            _uiState.value = UpdateUIState.Idle
        }
    }

    fun installUpdate(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    fun resetState() {
        _uiState.value = UpdateUIState.Idle
    }
}
