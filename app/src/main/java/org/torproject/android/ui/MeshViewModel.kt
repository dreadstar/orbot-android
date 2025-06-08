package org.torproject.android.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.torproject.android.service.MeshrabiyaService
import org.torproject.android.service.OrbotService

sealed class MeshUiState {
    object Disconnected : MeshUiState()
    object Connecting : MeshUiState()
    object Connected : MeshUiState()
    object Disconnecting : MeshUiState()
    data class Error(val message: String) : MeshUiState()
}

class MeshViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<MeshUiState>(MeshUiState.Disconnected)
    val uiState: StateFlow<MeshUiState> = _uiState.asStateFlow()

    private val _isSharingTor = MutableStateFlow(false)
    val isSharingTor: StateFlow<Boolean> = _isSharingTor.asStateFlow()

    fun connect() {
        viewModelScope.launch {
            _uiState.value = MeshUiState.Connecting
            try {
                // Start Meshrabiya service
                val intent = MeshrabiyaService.getStartIntent(getApplication())
                getApplication<Application>().startService(intent)
                _uiState.value = MeshUiState.Connected
            } catch (e: Exception) {
                _uiState.value = MeshUiState.Error(e.message ?: "Failed to start mesh")
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            _uiState.value = MeshUiState.Disconnecting
            try {
                // Stop Meshrabiya service
                val intent = MeshrabiyaService.getStopIntent(getApplication())
                getApplication<Application>().startService(intent)
                _uiState.value = MeshUiState.Disconnected
            } catch (e: Exception) {
                _uiState.value = MeshUiState.Error(e.message ?: "Failed to stop mesh")
            }
        }
    }

    fun toggleTorSharing() {
        viewModelScope.launch {
            _isSharingTor.value = !_isSharingTor.value
            // TODO: Implement mesh-to-tor routing logic
        }
    }

    fun refresh() {
        viewModelScope.launch {
            disconnect()
            connect()
        }
    }
} 