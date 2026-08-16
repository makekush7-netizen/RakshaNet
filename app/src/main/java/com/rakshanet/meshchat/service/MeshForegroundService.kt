package com.rakshanet.meshchat.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.rakshanet.meshchat.MainActivity
import com.rakshanet.meshchat.R
import com.rakshanet.meshchat.RakshaNetApplication

class MeshForegroundService : Service() {
    private val runtime get() = (application as RakshaNetApplication).meshRuntime

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = when (intent?.action) {
        ACTION_STOP -> {
            stopRelay()
            START_NOT_STICKY
        }
        else -> {
            startRelay()
            START_STICKY
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        runtime.stopNearby()
        MeshServiceStatus.set("Background relay stopped")
        super.onDestroy()
    }

    private fun startRelay() {
        try {
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            // The service owns lifecycle, not the selected router. Reporting
            // "mock" here while Nearby is active misleads the user.
            MeshServiceStatus.set("Background relay active")
            runtime.startNearby()
        } catch (exception: SecurityException) {
            MeshServiceStatus.set("Could not start relay: Bluetooth permission is required")
            stopSelf()
        }
    }

    private fun stopRelay() {
        runtime.stopNearby()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_REMOVE) else @Suppress("DEPRECATION") stopForeground(true)
        MeshServiceStatus.set("Background relay stopped")
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.mesh_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = getString(R.string.mesh_notification_channel_description) },
        )
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(getString(R.string.mesh_notification_title))
            .setContentText(getString(R.string.mesh_notification_text))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        const val ACTION_START = "com.rakshanet.meshchat.action.START_RELAY"
        const val ACTION_STOP = "com.rakshanet.meshchat.action.STOP_RELAY"
        private const val CHANNEL_ID = "mesh_relay_status"
        private const val NOTIFICATION_ID = 1001
    }
}
