package com.netscope.app.data.proxy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.netscope.app.MainActivity
import com.netscope.app.data.repository.TrafficRepositoryImpl
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

private const val TAG = "ProxyForegroundService"

@AndroidEntryPoint
class ProxyForegroundService : Service() {

    @Inject lateinit var localProxyServer: LocalProxyServer
    @Inject lateinit var trafficRepository: TrafficRepositoryImpl

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var notificationManager: NotificationManager

    companion object {
        const val ACTION_START = "com.netscope.app.proxy.START"
        const val ACTION_STOP = "com.netscope.app.proxy.STOP"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "netscope_proxy"

        fun startIntent(context: Context): Intent =
            Intent(context, ProxyForegroundService::class.java).apply {
                action = ACTION_START
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, ProxyForegroundService::class.java).apply {
                action = ACTION_STOP
            }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_START -> {
                start()
                START_STICKY
            }
            ACTION_STOP -> {
                stop()
                START_NOT_STICKY
            }
            else -> START_NOT_STICKY
        }
    }

    private fun start() {
        Log.d(TAG, "Starting foreground service")
        startForeground(NOTIFICATION_ID, buildNotification(0))
        localProxyServer.start()
        observeRequestCount()
    }

    private fun observeRequestCount() {
        trafficRepository.observeHttpTransactions()
            .map { it.size }
            .onEach { count ->
                notificationManager.notify(
                    NOTIFICATION_ID,
                    buildNotification(count),
                )
            }
            .launchIn(serviceScope)
    }

    private fun stop() {
        Log.d(TAG, "Stopping foreground service")
        localProxyServer.stop()
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        localProxyServer.stop()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "NetScope Proxy",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "NetScope proxy is capturing traffic"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(requestCount: Int): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = PendingIntent.getService(
            this, 0,
            stopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NetScope capturing")
            .setContentText(
                if (requestCount == 0) "Waiting for traffic…"
                else "$requestCount request${if (requestCount == 1) "" else "s"} captured"
            )
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(openIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopIntent,
            )
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}