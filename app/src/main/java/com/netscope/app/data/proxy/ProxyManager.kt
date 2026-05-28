package com.netscope.app.data.proxy

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ProxyManager"

@Singleton
class ProxyManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localProxyServer: LocalProxyServer,
) {
    fun start() {
        Log.d(TAG, "Starting proxy via foreground service")
        context.startForegroundService(
            ProxyForegroundService.startIntent(context)
        )
    }

    fun stop() {
        Log.d(TAG, "Stopping proxy via foreground service")
        context.startService(
            ProxyForegroundService.stopIntent(context)
        )
    }

    fun isRunning(): Boolean = localProxyServer.isRunning()

    fun getProxyHost(): String = "127.0.0.1"

    fun getProxyPort(): Int = LocalProxyServer.PORT
}