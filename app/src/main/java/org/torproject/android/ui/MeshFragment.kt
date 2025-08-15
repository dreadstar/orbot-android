package org.torproject.android.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
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
            MeshScreen(
                uiState = viewModel.uiState.collectAsState().value,
                isSharingTor = viewModel.isSharingTor.collectAsState().value,
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
                }
            )
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
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onChooseAppsClick: () -> Unit,
    onToggleTorSharing: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = when (uiState) {
                is MeshViewModel.MeshUiState.Connected -> "Ready to Mesh"
                is MeshViewModel.MeshUiState.Connecting -> "Connecting..."
                is MeshViewModel.MeshUiState.Disconnected -> "Not Connected"
                is MeshViewModel.MeshUiState.Disconnecting -> "Disconnecting..."
                is MeshViewModel.MeshUiState.Error -> "Error: ${uiState.message}"
            }
        )

        Button(
            onClick = when (uiState) {
                is MeshViewModel.MeshUiState.Connected -> onDisconnectClick
                else -> onConnectClick
            }
        ) {
            Text(
                text = when (uiState) {
                    is MeshViewModel.MeshUiState.Connected -> "TURN MESH OFF"
                    else -> "START MESH"
                }
            )
        }

        if (uiState is MeshViewModel.MeshUiState.Connected) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Switch(
                    checked = isSharingTor,
                    onCheckedChange = { onToggleTorSharing() },
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = if (isSharingTor) "Stop Sharing Tor" else "Share Tor",
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }

            // Add mesh role information display
            MeshRoleDisplay(
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onChooseAppsClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Choose Apps")
            }

            Button(
                onClick = onRefreshClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh")
            }
        }
    }
}

@Composable
private fun MeshRoleDisplay(
    viewModel: MeshViewModel,
    modifier: Modifier = Modifier
) {
    val currentRoles by viewModel.currentMeshRoles.collectAsState()
    val isRoleTransitionInProgress by viewModel.isRoleTransitionInProgress.collectAsState()
    val meshIntelligence by viewModel.meshIntelligence.collectAsState()

    Card(
        modifier = modifier.padding(vertical = 8.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mesh Roles",
                    style = MaterialTheme.typography.h6
                )
                if (isRoleTransitionInProgress) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Transitioning",
                            style = MaterialTheme.typography.caption
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (currentRoles.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(currentRoles.toList()) { role ->
                        MeshRoleChip(role = role)
                    }
                }
            } else {
                Text(
                    text = "No active roles",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            meshIntelligence?.let { intelligence ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Network: ${intelligence.totalNodes} nodes, ${intelligence.availableRoles.size} role types",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = { viewModel.updateMeshRoles() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Update Roles")
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