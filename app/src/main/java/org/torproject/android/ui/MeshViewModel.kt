package org.torproject.android.ui

import android.app.Application
import android.util.Log
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
    private val TAG = "MeshViewModel"
    private val _uiState = MutableStateFlow<MeshUiState>(MeshUiState.Disconnected)
    val uiState: StateFlow<MeshUiState> = _uiState.asStateFlow()

    private val _isSharingTor = MutableStateFlow(false)
    val isSharingTor: StateFlow<Boolean> = _isSharingTor.asStateFlow()

    init {
        Log.d(TAG, "Initializing MeshViewModel")
    }

    fun connect() {
        Log.d(TAG, "connect: Initiating mesh connection")
        viewModelScope.launch {
            _uiState.value = MeshUiState.Connecting
            try {
                Log.d(TAG, "connect: Starting MeshrabiyaService")
                val intent = MeshrabiyaService.getStartIntent(getApplication())
                getApplication<Application>().startService(intent)
                _uiState.value = MeshUiState.Connected
                Log.i(TAG, "connect: Successfully connected to mesh network")
            } catch (e: Exception) {
                Log.e(TAG, "connect: Failed to start mesh networking", e)
                _uiState.value = MeshUiState.Error(e.message ?: "Failed to start mesh")
            }
        }
    }

    fun disconnect() {
        Log.d(TAG, "disconnect: Initiating mesh disconnection")
        viewModelScope.launch {
            _uiState.value = MeshUiState.Disconnecting
            try {
                Log.d(TAG, "disconnect: Stopping MeshrabiyaService")
                val intent = MeshrabiyaService.getStopIntent(getApplication())
                getApplication<Application>().startService(intent)
                _uiState.value = MeshUiState.Disconnected
                Log.i(TAG, "disconnect: Successfully disconnected from mesh network")
            } catch (e: Exception) {
                Log.e(TAG, "disconnect: Failed to stop mesh networking", e)
                _uiState.value = MeshUiState.Error(e.message ?: "Failed to stop mesh")
            }
        }
    }

    fun toggleTorSharing() {
        Log.d(TAG, "toggleTorSharing: Current state: ${_isSharingTor.value}")
        viewModelScope.launch {
            _isSharingTor.value = !_isSharingTor.value
            Log.i(TAG, "toggleTorSharing: New state: ${_isSharingTor.value}")
            // TODO: Implement mesh-to-tor routing logic
        }
    }

    fun refresh() {
        Log.d(TAG, "refresh: Refreshing mesh connection")
        viewModelScope.launch {
            disconnect()
            connect()
        }
    }

    fun handleError(error: String) {
        Log.e(TAG, "handleError: $error")
        _uiState.value = MeshUiState.Error(error)
    }
} 