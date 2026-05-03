package com.netscope.app.presentation.screens.setup

import android.content.Context
import androidx.lifecycle.ViewModel
import com.netscope.app.data.proxy.ProxyManager
import com.netscope.app.data.proxy.cert.CertificateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ProxySetupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val proxyManager: ProxyManager,
    private val certificateManager: CertificateManager,
) : ViewModel() {

    private val prefs get() =
        context.getSharedPreferences("netscope_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        ProxySetupUiState(
            isCertInstalled = isCertInstalled(),
            isProxyRunning  = proxyManager.isRunning(),
            proxyHost       = proxyManager.getProxyHost(),
            proxyPort       = proxyManager.getProxyPort(),
        )
    )
    val uiState: StateFlow<ProxySetupUiState> = _uiState.asStateFlow()

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
}