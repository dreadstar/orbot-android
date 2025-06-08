package org.torproject.android.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.ustadmobile.meshrabiya.vnet.MeshRoleManager
import com.ustadmobile.meshrabiya.vnet.VirtualNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MeshrabiyaService : Service() {
    private val TAG = "MeshrabiyaService"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var virtualNode: VirtualNode? = null
    private var meshRoleManager: MeshRoleManager? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Initializing MeshrabiyaService")
        try {
            virtualNode = VirtualNode(applicationContext)
            meshRoleManager = MeshRoleManager(applicationContext)
            Log.d(TAG, "onCreate: Successfully initialized VirtualNode and MeshRoleManager")
        } catch (e: Exception) {
            Log.e(TAG, "onCreate: Failed to initialize mesh components", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: Received intent with action ${intent?.action}")
        when (intent?.action) {
            ACTION_START -> {
                Log.i(TAG, "onStartCommand: Starting mesh networking")
                startMesh()
            }
            ACTION_STOP -> {
                Log.i(TAG, "onStartCommand: Stopping mesh networking")
                stopMesh()
            }
            else -> Log.w(TAG, "onStartCommand: Unknown action received: ${intent?.action}")
        }
        return START_STICKY
    }

    private fun startMesh() {
        serviceScope.launch {
            try {
                Log.d(TAG, "startMesh: Starting VirtualNode")
                virtualNode?.start()
                Log.d(TAG, "startMesh: Starting MeshRoleManager")
                meshRoleManager?.start()
                Log.i(TAG, "startMesh: Successfully started mesh networking")
            } catch (e: Exception) {
                Log.e(TAG, "startMesh: Failed to start mesh networking", e)
            }
        }
    }

    private fun stopMesh() {
        serviceScope.launch {
            try {
                Log.d(TAG, "stopMesh: Stopping VirtualNode")
                virtualNode?.stop()
                Log.d(TAG, "stopMesh: Stopping MeshRoleManager")
                meshRoleManager?.stop()
                Log.i(TAG, "stopMesh: Successfully stopped mesh networking")
                stopSelf()
            } catch (e: Exception) {
                Log.e(TAG, "stopMesh: Failed to stop mesh networking", e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: Cleaning up MeshrabiyaService")
        super.onDestroy()
        stopMesh()
    }

    companion object {
        const val ACTION_START = "org.torproject.android.service.MeshrabiyaService.START"
        const val ACTION_STOP = "org.torproject.android.service.MeshrabiyaService.STOP"

        fun getStartIntent(context: Context): Intent {
            return Intent(context, MeshrabiyaService::class.java).apply {
                action = ACTION_START
            }
        }

        fun getStopIntent(context: Context): Intent {
            return Intent(context, MeshrabiyaService::class.java).apply {
                action = ACTION_STOP
            }
        }
    }
} 