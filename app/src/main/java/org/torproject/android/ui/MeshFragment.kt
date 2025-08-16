package org.torproject.android.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.torproject.android.R
import org.torproject.android.ui.AppManagerActivity
import com.ustadmobile.meshrabiya.mmcp.MeshRole

class MeshFragment : Fragment() {
    private val TAG = "MeshFragment"
    private val viewModel: MeshViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Initializing MeshFragment")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView: Creating view for MeshFragment")
        return inflater.inflate(R.layout.fragment_mesh, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Setting up Compose UI")
        
        view.findViewById<ComposeView>(R.id.composeView).setContent {
            MaterialTheme {
                MeshScreen(
                    uiState = viewModel.uiState.collectAsState().value,
                    isSharingTor = viewModel.isSharingTor.collectAsState().value,
                    meshServices = viewModel.meshServices.collectAsState().value,
                    networkStats = viewModel.networkStats.collectAsState().value,
                    connectedNodes = viewModel.connectedNodes.collectAsState().value,
                    onConnectClick = { 
                        Log.d(TAG, "onConnectClick: User initiated mesh connection")
                        viewModel.connect() 
                    },
                    onDisconnectClick = { 
                        Log.d(TAG, "onDisconnectClick: User initiated mesh disconnection")
                        viewModel.disconnect() 
                    },
                    onRefreshClick = { 
                        Log.d(TAG, "onRefreshClick: User initiated mesh refresh")
                        viewModel.refresh() 
                    },
                    onChooseAppsClick = { 
                        Log.d(TAG, "onChooseAppsClick: Opening app selection")
                        startActivity(Intent(requireContext(), AppManagerActivity::class.java)) 
                    },
                    onToggleTorSharing = { 
                        Log.d(TAG, "onToggleTorSharing: User toggled Tor sharing")
                        viewModel.toggleTorSharing() 
                    },
                    onServiceClick = { serviceId ->
                        Log.d(TAG, "onServiceClick: User tapped service: $serviceId")
                        viewModel.onServiceTapped(serviceId)
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: Cleaning up MeshFragment")
        super.onDestroy()
    }
}

@Composable
fun MeshScreen(
    uiState: MeshViewModel.MeshUiState,
    isSharingTor: Boolean,
    meshServices: List<MeshService>,
    networkStats: NetworkStats,
    connectedNodes: List<MeshNode>,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onChooseAppsClick: () -> Unit,
    onToggleTorSharing: () -> Unit,
    onServiceClick: (String) -> Unit
) {
    when (uiState) {
        is MeshViewModel.MeshUiState.Connected -> {
            MeshConnectedScreen(
                isSharingTor = isSharingTor,
                meshServices = meshServices,
                networkStats = networkStats,
                connectedNodes = connectedNodes,
                onDisconnectClick = onDisconnectClick,
                onRefreshClick = onRefreshClick,
                onChooseAppsClick = onChooseAppsClick,
                onToggleTorSharing = onToggleTorSharing,
                onServiceClick = onServiceClick
            )
        }
        else -> {
            MeshDisconnectedScreen(
                uiState = uiState,
                onConnectClick = onConnectClick
            )
        }
    }
}

@Composable
private fun MeshConnectedScreen(
    isSharingTor: Boolean,
    meshServices: List<MeshService>,
    networkStats: NetworkStats,
    connectedNodes: List<MeshNode>,
    onDisconnectClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onChooseAppsClick: () -> Unit,
    onToggleTorSharing: () -> Unit,
    onServiceClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Network Overview Dashboard
        item {
            NetworkOverviewDashboard(
                networkStats = networkStats,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Mesh Services Grid
        item {
            Text(
                text = "Available Services",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            MeshServicesGrid(
                services = meshServices,
                onServiceClick = onServiceClick,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Connected Nodes Section
        if (connectedNodes.isNotEmpty()) {
            item {
                Text(
                    text = "Connected Nodes",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(connectedNodes) { node ->
                ConnectedNodeCard(
                    node = node,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Control Section
        item {
            MeshControlSection(
                isSharingTor = isSharingTor,
                onDisconnectClick = onDisconnectClick,
                onRefreshClick = onRefreshClick,
                onChooseAppsClick = onChooseAppsClick,
                onToggleTorSharing = onToggleTorSharing,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MeshDisconnectedScreen(
    uiState: MeshViewModel.MeshUiState,
    onConnectClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = when (uiState) {
                is MeshViewModel.MeshUiState.Connecting -> "Connecting to Mesh..."
                is MeshViewModel.MeshUiState.Disconnecting -> "Disconnecting..."
                is MeshViewModel.MeshUiState.Error -> "Error: ${uiState.message}"
                else -> "Ready to Connect"
            },
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        if (uiState is MeshViewModel.MeshUiState.Connecting || uiState is MeshViewModel.MeshUiState.Disconnecting) {
            CircularProgressIndicator(
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }

        Button(
            onClick = onConnectClick,
            enabled = uiState !is MeshViewModel.MeshUiState.Connecting && uiState !is MeshViewModel.MeshUiState.Disconnecting,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "START MESH NETWORK",
                style = MaterialTheme.typography.button
            )
        }
    }
}

@Composable
private fun NetworkOverviewDashboard(
    networkStats: NetworkStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        backgroundColor = MaterialTheme.colors.primarySurface,
        elevation = 8.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Network Overview",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onPrimary
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NetworkStatCard(
                    value = networkStats.totalNodes.toString(),
                    label = "Total Nodes",
                    icon = Icons.Default.DeviceHub,
                    modifier = Modifier.weight(1f)
                )
                NetworkStatCard(
                    value = networkStats.activeServices.toString(),
                    label = "Services",
                    icon = Icons.Default.Settings,
                    modifier = Modifier.weight(1f)
                )
                NetworkStatCard(
                    value = "${networkStats.networkLoad}%",
                    label = "Load",
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
                NetworkStatCard(
                    value = "${networkStats.stability}%",
                    label = "Stability",
                    icon = Icons.Default.NetworkCheck,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NetworkStatCard(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colors.onPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.onPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onPrimary.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun MeshServicesGrid(
    services: List<MeshService>,
    onServiceClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Create rows of service cards
        val rows = services.chunked(2)
        rows.forEach { rowServices ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowServices.forEach { service ->
                    MeshServiceCard(
                        service = service,
                        onClick = { onServiceClick(service.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Add empty space if odd number of services
                if (rowServices.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun MeshServiceCard(
    service: MeshService,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Service Icon and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = service.icon,
                    contentDescription = service.name,
                    tint = service.statusColor,
                    modifier = Modifier.size(24.dp)
                )
                ServiceStatusIndicator(
                    status = service.status,
                    modifier = Modifier.size(12.dp)
                )
            }
            
            // Service Name
            Text(
                text = service.name,
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Description (if space allows)
            Text(
                text = service.description,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            // Node Count with Visual Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Circle,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(8.dp)
                )
                Text(
                    text = "${service.nodeCount} nodes",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                )
            }
            
            // Capacity Indicator
            CapacityProgressBar(
                capacity = service.capacity,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ServiceStatusIndicator(
    status: ServiceStatus,
    modifier: Modifier = Modifier
) {
    val color = when (status) {
        ServiceStatus.ACTIVE -> Color.Green
        ServiceStatus.DEGRADED -> Color.Yellow
        ServiceStatus.OFFLINE -> Color.Red
        ServiceStatus.INITIALIZING -> Color.Blue
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color)
    )
}

@Composable
private fun CapacityProgressBar(
    capacity: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Capacity",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = "${(capacity * 100).toInt()}%",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = capacity,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
            color = when {
                capacity < 0.3f -> Color.Red
                capacity < 0.7f -> Color.Yellow
                else -> Color.Green
            }
        )
    }
}

@Composable
private fun ConnectedNodeCard(
    node: MeshNode,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Status indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                when (node.status) {
                                    NodeStatus.CONNECTED -> Color.Green
                                    NodeStatus.CHARGING -> Color.Blue
                                    NodeStatus.DISCONNECTED -> Color.Red
                                }
                            )
                    )
                    
                    Column {
                        Text(
                            text = node.name,
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = node.roles.joinToString(", "),
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${node.battery}%",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Quality: ${node.quality}%",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Role chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(node.roles) { role ->
                    Surface(
                        color = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colors.primary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = role,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MeshControlSection(
    isSharingTor: Boolean,
    onDisconnectClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onChooseAppsClick: () -> Unit,
    onToggleTorSharing: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Mesh Controls",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )
            
            // Tor Sharing Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSharingTor) "Sharing Tor" else "Share Tor",
                    style = MaterialTheme.typography.body2
                )
                Switch(
                    checked = isSharingTor,
                    onCheckedChange = { onToggleTorSharing() }
                )
            }
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onChooseAppsClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Apps")
                }
                
                OutlinedButton(
                    onClick = onRefreshClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Refresh")
                }
            }
            
            Button(
                onClick = onDisconnectClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error)
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DISCONNECT MESH",
                    color = MaterialTheme.colors.onError
                )
            }
        }
    }
}

@Composable
private fun MeshRoleChip(role: MeshRole) {
    val roleDisplayName = when (role) {
        MeshRole.TOR_GATEWAY -> "Tor Gateway"
        MeshRole.CLEARNET_GATEWAY -> "Internet Gateway"
        MeshRole.I2P_GATEWAY -> "I2P Gateway"
        MeshRole.STORAGE_NODE -> "Storage"
        MeshRole.COMPUTE_NODE -> "Compute"
        MeshRole.COORDINATOR -> "Coordinator"
        MeshRole.MESH_PARTICIPANT -> "Participant"
        MeshRole.MESH_ROUTER -> "Router"
    }

    val roleColor = when (role) {
        MeshRole.TOR_GATEWAY -> MaterialTheme.colors.primary
        MeshRole.CLEARNET_GATEWAY -> MaterialTheme.colors.secondary
        MeshRole.I2P_GATEWAY -> MaterialTheme.colors.primaryVariant
        MeshRole.COORDINATOR -> MaterialTheme.colors.error
        else -> MaterialTheme.colors.surface
    }

    Card(
        backgroundColor = roleColor.copy(alpha = 0.1f),
        border = BorderStroke(
            1.dp, 
            roleColor.copy(alpha = 0.3f)
        )
    ) {
        Text(
            text = roleDisplayName,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.caption,
            color = roleColor
        )
    }
}