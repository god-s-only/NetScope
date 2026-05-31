package com.netscope.app.data.proxy

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ProxyDetector"

@Singleton
class ProxyDetector @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    data class ProxyStatus(
        val isSet: Boolean,
        val host: String,
        val port: Int,
        val isCorrect: Boolean,
    )

    fun getProxyStatus(): ProxyStatus {
        return try {
            val proxyHost = System.getProperty("http.proxyHost") ?: ""
            val proxyPort = System.getProperty("http.proxyPort")
                ?.toIntOrNull() ?: 0

            val isSet = proxyHost.isNotBlank() && proxyPort > 0
            val isCorrect = (proxyHost == "127.0.0.1" || proxyHost == "localhost") &&
                    proxyPort == LocalProxyServer.PORT

            Log.d(TAG, "Proxy status: host=$proxyHost port=$proxyPort " +
                    "isSet=$isSet isCorrect=$isCorrect")

            ProxyStatus(
                isSet = isSet,
                host = proxyHost,
                port = proxyPort,
                isCorrect = isCorrect,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get proxy status: ${e.message}")
            ProxyStatus(isSet = false, host = "", port = 0, isCorrect = false)
        }
    }

    fun isProxyCorrectlySet(): Boolean = getProxyStatus().isCorrect
}