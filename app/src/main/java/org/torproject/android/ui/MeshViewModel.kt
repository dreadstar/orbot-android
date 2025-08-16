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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color

sealed class MeshUiState {
    object Disconnected : MeshUiState()
    object Connecting : MeshUiState()
    object Connected : MeshUiState()
    object Disconnecting : MeshUiState()
    data class Error(val message: String) : MeshUiState()
}

class MeshViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MeshViewModel"
    
    sealed class MeshUiState {
        object Disconnected : MeshUiState()
        object Connecting : MeshUiState()
        object Connected : MeshUiState()
        object Disconnecting : MeshUiState()
        data class Error(val message: String) : MeshUiState()
    }
    
    private val _uiState = MutableStateFlow<MeshUiState>(MeshUiState.Disconnected)
    val uiState: StateFlow<MeshUiState> = _uiState.asStateFlow()

    private val _isSharingTor = MutableStateFlow(false)
    val isSharingTor: StateFlow<Boolean> = _isSharingTor.asStateFlow()

    // Enhanced mesh state
    private val _meshServices = MutableStateFlow<List<MeshService>>(emptyList())
    val meshServices: StateFlow<List<MeshService>> = _meshServices.asStateFlow()
    
    private val _networkStats = MutableStateFlow(NetworkStats.default())
    val networkStats: StateFlow<NetworkStats> = _networkStats.asStateFlow()
    
    private val _connectedNodes = MutableStateFlow<List<MeshNode>>(emptyList())
    val connectedNodes: StateFlow<List<MeshNode>> = _connectedNodes.asStateFlow()

    // Legacy mesh role state (keep for backward compatibility)
    private val _currentMeshRoles = MutableStateFlow<Set<MeshRole>>(emptySet())
    val currentMeshRoles: StateFlow<Set<MeshRole>> = _currentMeshRoles.asStateFlow()

    private val _meshIntelligence = MutableStateFlow<MeshIntelligence?>(null)
    val meshIntelligence: StateFlow<MeshIntelligence?> = _meshIntelligence.asStateFlow()

    private val _isRoleTransitionInProgress = MutableStateFlow(false)
    val isRoleTransitionInProgress: StateFlow<Boolean> = _isRoleTransitionInProgress.asStateFlow()

    private var roleUpdateJob: kotlinx.coroutines.Job? = null
    private var serviceUpdateJob: kotlinx.coroutines.Job? = null

    init {
        Log.d(TAG, "Initializing MeshViewModel")
        initializeMockData()
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
                
                // Start monitoring mesh roles and services
                updateMeshRoles()
                startPeriodicRoleUpdates()
                startServiceMonitoring()
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
                stopServiceMonitoring()
                
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
                        val emergentRoleManager = virtualNode.getEmergentRoleManager()
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
                        val emergentRoleManager = virtualNode.getEmergentRoleManager()
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
    
    private fun startServiceMonitoring() {
        serviceUpdateJob?.cancel()
        serviceUpdateJob = viewModelScope.launch {
            while (true) {
                updateMeshServices()
                updateNetworkStats()
                updateConnectedNodes()
                delay(5_000L) // Update every 5 seconds for more dynamic feel
            }
        }
    }
    
    private fun stopServiceMonitoring() {
        serviceUpdateJob?.cancel()
        serviceUpdateJob = null
    }
    
    private fun initializeMockData() {
        // Initialize with default mesh services
        _meshServices.value = createDefaultMeshServices()
        _networkStats.value = NetworkStats.default()
        _connectedNodes.value = emptyList()
    }
    
    private fun createDefaultMeshServices(): List<MeshService> {
        return listOf(
            MeshService(
                id = "tor-gateway",
                name = "Tor Gateway",
                description = "Anonymous internet access through Tor",
                icon = Icons.Default.Security,
                nodeCount = 0,
                capacity = 0.0f,
                status = ServiceStatus.OFFLINE,
                statusColor = Color.Red,
                priority = ServicePriority.HIGH
            ),
            MeshService(
                id = "clearnet-gateway",
                name = "Internet Gateway", 
                description = "Direct internet access for emergency",
                icon = Icons.Default.Language,
                nodeCount = 0,
                capacity = 0.0f,
                status = ServiceStatus.OFFLINE,
                statusColor = Color.Red,
                priority = ServicePriority.MEDIUM
            ),
            MeshService(
                id = "storage-node",
                name = "Distributed Storage",
                description = "Encrypted file sharing and backup",
                icon = Icons.Default.Storage,
                nodeCount = 0,
                capacity = 0.0f,
                status = ServiceStatus.OFFLINE,
                statusColor = Color.Red,
                priority = ServicePriority.MEDIUM
            ),
            MeshService(
                id = "compute-node",
                name = "Compute Network",
                description = "Distributed processing power",
                icon = Icons.Default.Memory,
                nodeCount = 0,
                capacity = 0.0f,
                status = ServiceStatus.OFFLINE,
                statusColor = Color.Red,
                priority = ServicePriority.LOW
            ),
            MeshService(
                id = "mesh-router",
                name = "Mesh Routing",
                description = "Network traffic routing and optimization",
                icon = Icons.Default.Router,
                nodeCount = 0,
                capacity = 0.0f,
                status = ServiceStatus.OFFLINE,
                statusColor = Color.Red,
                priority = ServicePriority.CRITICAL
            ),
            MeshService(
                id = "coordinator",
                name = "Network Coordinator",
                description = "Mesh intelligence and role assignment",
                icon = Icons.Default.Hub,
                nodeCount = 0,
                capacity = 0.0f,
                status = ServiceStatus.OFFLINE,
                statusColor = Color.Red,
                priority = ServicePriority.HIGH
            )
        )
    }
    
    private suspend fun updateMeshServices() {
        try {
            val meshService = MeshrabiyaService.getInstance()
            if (meshService != null) {
                val virtualNode = meshService.getVirtualNode()
                if (virtualNode != null) {
                    val emergentRoleManager = virtualNode.getEmergentRoleManager()
                    if (emergentRoleManager != null) {
                        val currentRoles = emergentRoleManager.getCurrentMeshRoles()
                        val intelligence = emergentRoleManager.getMeshIntelligence()
                        
                        // Update services based on current mesh state
                        val updatedServices = _meshServices.value.map { service ->
                            updateServiceBasedOnMeshState(service, currentRoles, intelligence)
                        }
                        
                        _meshServices.value = updatedServices
                    }
                }
            } else {
                // When not connected, reset all services to offline
                val offlineServices = _meshServices.value.map { service ->
                    service.copy(
                        nodeCount = 0,
                        capacity = 0.0f,
                        status = ServiceStatus.OFFLINE,
                        statusColor = Color.Red
                    )
                }
                _meshServices.value = offlineServices
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateMeshServices: Failed to update mesh services", e)
        }
    }
    
    private fun updateServiceBasedOnMeshState(
        service: MeshService,
        currentRoles: Set<MeshRole>,
        intelligence: MeshIntelligence?
    ): MeshService {
        // Simulate service updates based on mesh state
        val isServiceActive = when (service.id) {
            "tor-gateway" -> currentRoles.contains(MeshRole.TOR_GATEWAY)
            "clearnet-gateway" -> currentRoles.contains(MeshRole.CLEARNET_GATEWAY)
            "storage-node" -> currentRoles.contains(MeshRole.STORAGE_NODE)
            "compute-node" -> currentRoles.contains(MeshRole.COMPUTE_NODE)
            "mesh-router" -> currentRoles.contains(MeshRole.MESH_ROUTER)
            "coordinator" -> currentRoles.contains(MeshRole.COORDINATOR)
            else -> false
        }
        
        val nodeCount = intelligence?.let { intel ->
            when (service.id) {
                "tor-gateway" -> intel.activeGateways
                "clearnet-gateway" -> intel.activeGateways / 2
                "storage-node" -> intel.activeStorageNodes
                "compute-node" -> intel.activeComputeNodes
                "mesh-router" -> intel.totalNodes / 3
                "coordinator" -> intel.totalNodes / 10
                else -> 0
            }
        } ?: if (isServiceActive) 1 else 0
        
        val capacity = if (nodeCount > 0) {
            // Simulate capacity based on network load and utilization
            intelligence?.let { intel ->
                when (service.id) {
                    "storage-node" -> 1.0f - intel.storageUtilization
                    "compute-node" -> 1.0f - intel.computeUtilization
                    else -> 1.0f - intel.networkLoad
                }
            } ?: 0.5f
        } else 0.0f
        
        val status = when {
            nodeCount == 0 -> ServiceStatus.OFFLINE
            capacity < 0.3f -> ServiceStatus.DEGRADED
            else -> ServiceStatus.ACTIVE
        }
        
        val statusColor = when (status) {
            ServiceStatus.ACTIVE -> Color.Green
            ServiceStatus.DEGRADED -> Color.Yellow
            ServiceStatus.OFFLINE -> Color.Red
            ServiceStatus.INITIALIZING -> Color.Blue
        }
        
        return service.copy(
            nodeCount = nodeCount,
            capacity = capacity,
            status = status,
            statusColor = statusColor
        )
    }
    
    private suspend fun updateNetworkStats() {
        try {
            val meshService = MeshrabiyaService.getInstance()
            if (meshService != null) {
                val virtualNode = meshService.getVirtualNode()
                if (virtualNode != null) {
                    val emergentRoleManager = virtualNode.getEmergentRoleManager()
                    if (emergentRoleManager != null) {
                        val intelligence = emergentRoleManager.getMeshIntelligence()
                        
                        intelligence?.let { intel ->
                            _networkStats.value = NetworkStats(
                                totalNodes = intel.totalNodes,
                                activeServices = _meshServices.value.count { it.status == ServiceStatus.ACTIVE },
                                networkLoad = (intel.networkLoad * 100).toInt(),
                                stability = ((1.0f - intel.networkLoad) * 100).toInt(),
                                lastUpdate = System.currentTimeMillis()
                            )
                        }
                    }
                }
            } else {
                _networkStats.value = NetworkStats.default()
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateNetworkStats: Failed to update network stats", e)
        }
    }
    
    private suspend fun updateConnectedNodes() {
        try {
            val meshService = MeshrabiyaService.getInstance()
            if (meshService != null) {
                val virtualNode = meshService.getVirtualNode()
                if (virtualNode != null) {
                    val neighbors = virtualNode.neighbors()
                    
                    // Convert mesh neighbors to UI nodes
                    val nodes = neighbors.mapIndexed { index, (nodeId, _) ->
                        // Generate mock node data based on actual neighbor data
                        val mockRoles = generateMockRoles(index)
                        val mockBattery = (60 + (index * 7) % 40)
                        val mockQuality = (75 + (index * 5) % 25)
                        
                        MeshNode(
                            id = nodeId.toString(),
                            name = "Node-${nodeId.toString().takeLast(4)}",
                            roles = mockRoles,
                            battery = mockBattery,
                            status = NodeStatus.CONNECTED,
                            quality = mockQuality
                        )
                    }
                    
                    _connectedNodes.value = nodes
                }
            } else {
                _connectedNodes.value = emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateConnectedNodes: Failed to update connected nodes", e)
            _connectedNodes.value = emptyList()
        }
    }
    
    private fun generateMockRoles(index: Int): List<String> {
        val allRoles = listOf("Gateway", "Router", "Storage", "Compute", "Participant", "Coordinator")
        return when (index % 4) {
            0 -> listOf("Gateway", "Router")
            1 -> listOf("Storage", "Participant")
            2 -> listOf("Compute", "Router")
            else -> listOf("Participant")
        }
    }
    
    fun onServiceTapped(serviceId: String) {
        Log.d(TAG, "onServiceTapped: Service $serviceId tapped")
        // TODO: Implement service-specific actions
        // Could show service details, enable/disable service, etc.
    }

    override fun onCleared() {
        super.onCleared()
        stopPeriodicRoleUpdates()
        stopServiceMonitoring()
    }
}

// Data classes for enhanced mesh UI
data class MeshService(
    val id: String,
    val name: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val nodeCount: Int,
    val capacity: Float, // 0.0 to 1.0
    val status: ServiceStatus,
    val statusColor: androidx.compose.ui.graphics.Color,
    val priority: ServicePriority
)

enum class ServiceStatus {
    ACTIVE, DEGRADED, OFFLINE, INITIALIZING
}

enum class ServicePriority {
    CRITICAL, HIGH, MEDIUM, LOW
}

data class NetworkStats(
    val totalNodes: Int,
    val activeServices: Int,
    val networkLoad: Int, // 0-100
    val stability: Int, // 0-100
    val lastUpdate: Long
) {
    companion object {
        fun default() = NetworkStats(
            totalNodes = 0,
            activeServices = 0,
            networkLoad = 0,
            stability = 0,
            lastUpdate = System.currentTimeMillis()
        )
    }
}

data class MeshNode(
    val id: String,
    val name: String,
    val roles: List<String>,
    val battery: Int,
    val status: NodeStatus,
    val quality: Int
)

enum class NodeStatus {
    CONNECTED, CHARGING, DISCONNECTED
}