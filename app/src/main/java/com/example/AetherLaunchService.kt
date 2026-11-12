package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/**
 * Keeps the process at foreground-service importance while the desktop is
 * open. Taskbar - whose freeform launches float on HyperOS where ours went
 * fullscreen - always runs as a foreground service with an overlay. A bare
 * foreground-Activity process is a different launch role on some ROMs, so
 * this brings our process role to parity for the floating-launch path.
 * Shows a minimal ongoing notification (required for a foreground service).
 */
class AetherLaunchService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = "aether_launch"
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(channelId) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "AetherLinux desktop",
                    NotificationManager.IMPORTANCE_MIN
                )
            )
        }
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("AetherLinux desktop running")
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_STICKY
    }

    companion object {
        private const val NOTIFICATION_ID = 4101
    }
}
