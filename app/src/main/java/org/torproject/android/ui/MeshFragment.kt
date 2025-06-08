package org.torproject.android.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
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

class MeshFragment : Fragment() {
    private val viewModel: MeshViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_mesh, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        view.findViewById<ComposeView>(R.id.composeView).setContent {
            MeshScreen(
                uiState = viewModel.uiState.collectAsState().value,
                isSharingTor = viewModel.isSharingTor.collectAsState().value,
                onConnectClick = { viewModel.connect() },
                onDisconnectClick = { viewModel.disconnect() },
                onRefreshClick = { viewModel.refresh() },
                onChooseAppsClick = { startActivity(Intent(requireContext(), AppManagerActivity::class.java)) },
                onToggleTorSharing = { viewModel.toggleTorSharing() }
            )
        }
    }
}

@Composable
fun MeshScreen(
    uiState: MeshUiState,
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
        // Status text
        Text(
            text = when (uiState) {
                is MeshUiState.Disconnected -> "Ready to Mesh"
                is MeshUiState.Connecting -> "Starting Mesh..."
                is MeshUiState.Connected -> "Mesh Active"
                is MeshUiState.Disconnecting -> "Stopping Mesh..."
                is MeshUiState.Error -> "Error: ${uiState.message}"
            }
        )

        // Main action button
        Button(
            onClick = when (uiState) {
                is MeshUiState.Connected -> onDisconnectClick
                else -> onConnectClick
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = when (uiState) {
                    is MeshUiState.Connected -> "Turn Mesh Off"
                    else -> "Start Mesh"
                }
            )
        }

        // Tor sharing toggle
        if (uiState is MeshUiState.Connected) {
            Button(
                onClick = onToggleTorSharing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (isSharingTor) "Stop Sharing Tor" else "Share Tor")
            }
        }

        // Choose Apps button
        OutlinedButton(
            onClick = onChooseAppsClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Choose Apps")
        }

        // Refresh button
        if (uiState is MeshUiState.Connected) {
            OutlinedButton(
                onClick = onRefreshClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh")
            }
        }
    }
} 