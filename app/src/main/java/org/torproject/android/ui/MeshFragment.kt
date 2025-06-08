package org.torproject.android.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
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