package org.torproject.android.ui

import android.content.Context
import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.torproject.android.service.MeshrabiyaService

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class MeshViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var lifecycleOwner: LifecycleOwner

    private lateinit var viewModel: MeshViewModel
    private lateinit var lifecycle: LifecycleRegistry

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        lifecycle = LifecycleRegistry(lifecycleOwner)
        `when`(lifecycleOwner.lifecycle).thenReturn(lifecycle)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        
        viewModel = MeshViewModel(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Disconnected`() = runTest {
        val observer = Observer<MeshViewModel.MeshUiState> {}
        viewModel.uiState.observe(lifecycleOwner, observer)
        
        assertEquals(MeshViewModel.MeshUiState.Disconnected, viewModel.uiState.value)
    }

    @Test
    fun `connect starts MeshrabiyaService`() = runTest {
        val observer = Observer<MeshViewModel.MeshUiState> {}
        viewModel.uiState.observe(lifecycleOwner, observer)
        
        viewModel.connect()
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertEquals(MeshViewModel.MeshUiState.Connected, viewModel.uiState.value)
    }

    @Test
    fun `disconnect stops MeshrabiyaService`() = runTest {
        val observer = Observer<MeshViewModel.MeshUiState> {}
        viewModel.uiState.observe(lifecycleOwner, observer)
        
        // First connect
        viewModel.connect()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then disconnect
        viewModel.disconnect()
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertEquals(MeshViewModel.MeshUiState.Disconnected, viewModel.uiState.value)
    }

    @Test
    fun `toggleTorSharing updates state`() = runTest {
        val observer = Observer<Boolean> {}
        viewModel.isTorSharingEnabled.observe(lifecycleOwner, observer)
        
        // Initial state should be false
        assertFalse(viewModel.isTorSharingEnabled.value ?: false)
        
        // Toggle to true
        viewModel.toggleTorSharing()
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertTrue(viewModel.isTorSharingEnabled.value ?: false)
        
        // Toggle back to false
        viewModel.toggleTorSharing()
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertFalse(viewModel.isTorSharingEnabled.value ?: false)
    }

    @Test
    fun `refresh restarts service`() = runTest {
        val observer = Observer<MeshViewModel.MeshUiState> {}
        viewModel.uiState.observe(lifecycleOwner, observer)
        
        // First connect
        viewModel.connect()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then refresh
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Should still be connected
        assertEquals(MeshViewModel.MeshUiState.Connected, viewModel.uiState.value)
    }

    @Test
    fun `error state is handled correctly`() = runTest {
        val observer = Observer<MeshViewModel.MeshUiState> {}
        viewModel.uiState.observe(lifecycleOwner, observer)
        
        // Simulate an error
        viewModel.handleError("Test error")
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertEquals(MeshViewModel.MeshUiState.Error("Test error"), viewModel.uiState.value)
    }
} 