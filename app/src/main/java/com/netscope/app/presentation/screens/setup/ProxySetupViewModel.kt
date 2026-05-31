package com.netscope.app.presentation.screens.setup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netscope.app.data.proxy.ProxyDetector
import com.netscope.app.data.proxy.ProxyManager
import com.netscope.app.data.proxy.LocalProxyServer
import com.netscope.app.data.proxy.cert.CertificateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProxySetupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val proxyManager: ProxyManager,
    private val certificateManager: CertificateManager,
    private val proxyDetector: ProxyDetector,
) : ViewModel() {

    private val prefs get() =
        context.getSharedPreferences("netscope_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        ProxySetupUiState(
            isCertInstalled = isCertInstalled(),
            isProxyRunning = proxyManager.isRunning(),
            proxyHost = proxyManager.getProxyHost(),
            proxyPort = proxyManager.getProxyPort(),
        )
    )
    val uiState: StateFlow<ProxySetupUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        checkProxyStatus()
    }

    /**
     * Called when screen resumes — user may have just come back
     * from WiFi settings after setting the proxy
     */
    fun onScreenResumed() {
        checkProxyStatus()
        startPolling()
    }

    fun onScreenPaused() {
        stopPolling()
    }

    /**
     * Poll every 2 seconds while screen is visible so the checkmark
     * appears automatically when user sets the proxy and comes back
     */
    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(2_000L)
                checkProxyStatus()
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun checkProxyStatus() {
        val status = proxyDetector.getProxyStatus()
        val message = when {
            status.isCorrect -> "Proxy detected on ${status.host}:${status.port} ✓"
            status.isSet -> "Wrong proxy: ${status.host}:${status.port} — " +
                    "set to 127.0.0.1:${LocalProxyServer.PORT}"
            else -> "No proxy set — follow the steps below"
        }
        _uiState.value = _uiState.value.copy(
            isProxyCorrectlySet = status.isCorrect,
            proxyStatusMessage = message,
            isProxyRunning = proxyManager.isRunning(),
        )
    }

    fun startProxy() {
        proxyManager.start()
        _uiState.value = _uiState.value.copy(isProxyRunning = true)
    }

    fun stopProxy() {
        proxyManager.stop()
        _uiState.value = _uiState.value.copy(isProxyRunning = false)
    }

    fun getCaCertBytes(): ByteArray =
        certificateManager.getCaCertificateBytes()

    fun markCertInstalled() {
        prefs.edit().putBoolean("cert_installed", true).apply()
        _uiState.value = _uiState.value.copy(isCertInstalled = true)
    }

    fun isCertInstalled(): Boolean =
        prefs.getBoolean("cert_installed", false)

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}