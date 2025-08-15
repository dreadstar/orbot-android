package org.torproject.android.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.torproject.android.service.MeshrabiyaService
import org.torproject.android.service.OrbotService
import com.ustadmobile.meshrabiya.mmcp.MeshRole
import com.ustadmobile.meshrabiya.vnet.MeshIntelligence

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

    // Mesh role state
    private val _currentMeshRoles = MutableStateFlow<Set<MeshRole>>(emptySet())
    val currentMeshRoles: StateFlow<Set<MeshRole>> = _currentMeshRoles.asStateFlow()

    private val _meshIntelligence = MutableStateFlow<MeshIntelligence?>(null)
    val meshIntelligence: StateFlow<MeshIntelligence?> = _meshIntelligence.asStateFlow()

    private val _isRoleTransitionInProgress = MutableStateFlow(false)
    val isRoleTransitionInProgress: StateFlow<Boolean> = _isRoleTransitionInProgress.asStateFlow()

    private var roleUpdateJob: kotlinx.coroutines.Job? = null

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
                
                // Start monitoring mesh roles
                updateMeshRoles()
                startPeriodicRoleUpdates()
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
                
                // Stop periodic updates
                stopPeriodicRoleUpdates()
                
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

    fun updateMeshRoles() {
        Log.d(TAG, "updateMeshRoles: Fetching current mesh roles")
        viewModelScope.launch {
            try {
                // Get the current EmergentRoleManager state
                val meshService = MeshrabiyaService.getInstance()
                if (meshService != null) {
                    val virtualNode = meshService.getVirtualNode()
                    if (virtualNode != null) {
                        val emergentRoleManager = virtualNode.emergentRoleManager
                        if (emergentRoleManager != null) {
                            val currentRoles = emergentRoleManager.getCurrentMeshRoles()
                            val intelligence = emergentRoleManager.getMeshIntelligence()
                            val isTransitioning = emergentRoleManager.isRoleTransitionInProgress()
                            
                            _currentMeshRoles.value = currentRoles
                            _meshIntelligence.value = intelligence
                            _isRoleTransitionInProgress.value = isTransitioning
                            
                            Log.i(TAG, "updateMeshRoles: Current roles: $currentRoles, transitioning: $isTransitioning")
                        } else {
                            Log.w(TAG, "updateMeshRoles: EmergentRoleManager not available")
                        }
                    } else {
                        Log.w(TAG, "updateMeshRoles: VirtualNode not available")
                    }
                } else {
                    Log.w(TAG, "updateMeshRoles: MeshrabiyaService not running")
                }
            } catch (e: Exception) {
                Log.e(TAG, "updateMeshRoles: Failed to update mesh roles", e)
            }
        }
    }

    fun requestRoleTransition(preferredRoles: Set<MeshRole>) {
        Log.d(TAG, "requestRoleTransition: Requesting transition to roles: $preferredRoles")
        viewModelScope.launch {
            try {
                val meshService = MeshrabiyaService.getInstance()
                if (meshService != null) {
                    val virtualNode = meshService.getVirtualNode()
                    if (virtualNode != null) {
                        val emergentRoleManager = virtualNode.emergentRoleManager
                        if (emergentRoleManager != null) {
                            // Set user preferences for role assignment
                            emergentRoleManager.setPreferredRoles(preferredRoles)
                            // Trigger immediate role update
                            emergentRoleManager.updateRoles()
                            Log.i(TAG, "requestRoleTransition: Role transition requested")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "requestRoleTransition: Failed to request role transition", e)
            }
        }
    }

    private fun startPeriodicRoleUpdates() {
        roleUpdateJob?.cancel()
        roleUpdateJob = viewModelScope.launch {
            while (true) {
                delay(30_000L) // Update every 30 seconds
                updateMeshRoles()
            }
        }
    }

    private fun stopPeriodicRoleUpdates() {
        roleUpdateJob?.cancel()
        roleUpdateJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPeriodicRoleUpdates()
    }
} 