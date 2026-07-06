package com.streamix.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.BuildConfig
import com.streamix.core.network.UpdateApiService
import com.streamix.core.network.UpdateInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val api: UpdateApiService
) : ViewModel() {

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo = _updateInfo.asStateFlow()

    fun checkUpdate() {
        viewModelScope.launch {
            try {
                // Replace with your actual update JSON URL
                val info = api.checkUpdate("https://raw.githubusercontent.com/your-username/your-repo/main/update.json")
                if (info.latestVersionCode > BuildConfig.VERSION_CODE) {
                    _updateInfo.value = info
                }
            } catch (e: Exception) {
                // Ignore check errors
            }
        }
    }

    fun dismissUpdate() {
        _updateInfo.value = null
    }
}
