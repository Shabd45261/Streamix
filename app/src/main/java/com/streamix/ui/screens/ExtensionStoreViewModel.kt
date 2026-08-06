package com.streamix.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamix.core.plugin.ExtensionStoreRepository
import com.streamix.core.plugin.InstallState
import com.streamix.core.plugin.PluginManager
import com.streamix.core.plugin.RemoteExtension
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class StoreUiState {
    object Loading : StoreUiState()
    data class Success(val extensions: List<RemoteExtension>, val lastUpdated: String) : StoreUiState()
    data class Error(val message: String) : StoreUiState()
}

@HiltViewModel
class ExtensionStoreViewModel @Inject constructor(
    private val storeRepository: ExtensionStoreRepository,
    private val pluginManager: PluginManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<StoreUiState>(StoreUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _installingIds = MutableStateFlow<Set<String>>(emptySet())
    val installingIds = _installingIds.asStateFlow()

    val installedPlugins = pluginManager.plugins

    init { fetchStore() }

    fun fetchStore() = viewModelScope.launch {
        _uiState.value = StoreUiState.Loading
        storeRepository.fetchExtensions()
            .onSuccess { (list, date) -> _uiState.value = StoreUiState.Success(list, date) }
            .onFailure { _uiState.value = StoreUiState.Error(it.message ?: "Failed to load store") }
    }

    fun isInstalled(id: String) = pluginManager.isInstalled(id)
    fun isInstalling(id: String) = id in _installingIds.value

    fun install(ext: RemoteExtension) = viewModelScope.launch {
        _installingIds.update { it + ext.id }
        pluginManager.installFromUrl(ext.apkUrl, ext.manifestUrl).collect { state ->
            if (state is InstallState.Success || state is InstallState.Error) {
                _installingIds.update { it - ext.id }
            }
        }
    }

    fun uninstall(id: String) = viewModelScope.launch { pluginManager.uninstall(id) }
}
