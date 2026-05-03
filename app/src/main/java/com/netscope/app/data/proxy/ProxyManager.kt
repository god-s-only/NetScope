package com.netscope.app.data.proxy

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ProxyManager"

@Singleton
class ProxyManager @Inject constructor(
    private val localProxyServer: LocalProxyServer,
) {
    fun start() {
        Log.d(TAG, "Starting proxy")
        localProxyServer.start()
    }

    fun stop() {
        Log.d(TAG, "Stopping proxy")
        localProxyServer.stop()
    }

    fun isRunning(): Boolean = localProxyServer.isRunning()

    fun getProxyHost(): String = "127.0.0.1"

    fun getProxyPort(): Int = LocalProxyServer.PORT
}