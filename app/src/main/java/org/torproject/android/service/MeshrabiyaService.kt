package org.torproject.android.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.ustadmobile.meshrabiya.vnet.MeshRoleManager
import com.ustadmobile.meshrabiya.vnet.VirtualNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MeshrabiyaService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var virtualNode: VirtualNode? = null
    private var meshRoleManager: MeshRoleManager? = null

    override fun onCreate() {
        super.onCreate()
        virtualNode = VirtualNode(applicationContext)
        meshRoleManager = MeshRoleManager(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startMesh()
            ACTION_STOP -> stopMesh()
        }
        return START_STICKY
    }

    private fun startMesh() {
        serviceScope.launch {
            try {
                virtualNode?.start()
                meshRoleManager?.start()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun stopMesh() {
        serviceScope.launch {
            try {
                virtualNode?.stop()
                meshRoleManager?.stop()
                stopSelf()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopMesh()
    }

    companion object {
        private const val ACTION_START = "org.torproject.android.service.MeshrabiyaService.START"
        private const val ACTION_STOP = "org.torproject.android.service.MeshrabiyaService.STOP"

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