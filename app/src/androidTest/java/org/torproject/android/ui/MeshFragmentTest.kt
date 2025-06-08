package org.torproject.android.ui

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.torproject.android.service.MeshrabiyaService

@RunWith(AndroidJUnit4::class)
class MeshFragmentTest {

    private lateinit var viewModel: MeshViewModel

    @Before
    fun setup() {
        viewModel = MeshViewModel(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun testInitialState() {
        val scenario = launchFragmentInContainer<MeshFragment>()
        
        scenario.onFragment { fragment ->
            assertThat(fragment.viewModel.uiState.value).isEqualTo(MeshViewModel.MeshUiState.Disconnected)
            assertThat(fragment.viewModel.isTorSharingEnabled.value).isFalse()
        }
    }

    @Test
    fun testConnectButton() {
        val scenario = launchFragmentInContainer<MeshFragment>()
        
        scenario.onFragment { fragment ->
            // Click connect button
            fragment.binding.connectButton.performClick()
            
            // Verify service is started
            assertThat(fragment.viewModel.uiState.value).isEqualTo(MeshViewModel.MeshUiState.Connected)
        }
    }

    @Test
    fun testDisconnectButton() {
        val scenario = launchFragmentInContainer<MeshFragment>()
        
        scenario.onFragment { fragment ->
            // First connect
            fragment.binding.connectButton.performClick()
            
            // Then disconnect
            fragment.binding.connectButton.performClick()
            
            // Verify service is stopped
            assertThat(fragment.viewModel.uiState.value).isEqualTo(MeshViewModel.MeshUiState.Disconnected)
        }
    }

    @Test
    fun testTorSharingToggle() {
        val scenario = launchFragmentInContainer<MeshFragment>()
        
        scenario.onFragment { fragment ->
            // First connect
            fragment.binding.connectButton.performClick()
            
            // Toggle Tor sharing
            fragment.binding.torSharingToggle.performClick()
            
            // Verify Tor sharing is enabled
            assertThat(fragment.viewModel.isTorSharingEnabled.value).isTrue()
            
            // Toggle again
            fragment.binding.torSharingToggle.performClick()
            
            // Verify Tor sharing is disabled
            assertThat(fragment.viewModel.isTorSharingEnabled.value).isFalse()
        }
    }

    @Test
    fun testRefreshButton() {
        val scenario = launchFragmentInContainer<MeshFragment>()
        
        scenario.onFragment { fragment ->
            // First connect
            fragment.binding.connectButton.performClick()
            
            // Click refresh
            fragment.binding.refreshButton.performClick()
            
            // Verify service is still running
            assertThat(fragment.viewModel.uiState.value).isEqualTo(MeshViewModel.MeshUiState.Connected)
        }
    }

    @Test
    fun testChooseAppsButton() {
        val scenario = launchFragmentInContainer<MeshFragment>()
        
        scenario.onFragment { fragment ->
            // Click choose apps button
            fragment.binding.chooseAppsButton.performClick()
            
            // Verify app selection dialog is shown
            // Note: This would require mocking the dialog or using Espresso to verify UI elements
        }
    }

    @Test
    fun testErrorState() {
        val scenario = launchFragmentInContainer<MeshFragment>()
        
        scenario.onFragment { fragment ->
            // Simulate an error
            fragment.viewModel.handleError("Test error")
            
            // Verify error state is shown
            assertThat(fragment.viewModel.uiState.value).isEqualTo(MeshViewModel.MeshUiState.Error("Test error"))
        }
    }

    @Test
    fun testLifecycleChanges() {
        val scenario = launchFragmentInContainer<MeshFragment>()
        
        // Test fragment lifecycle changes
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.moveToState(Lifecycle.State.DESTROYED)
    }
} 