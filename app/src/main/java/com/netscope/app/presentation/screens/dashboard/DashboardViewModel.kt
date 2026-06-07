package com.netscope.app.presentation.screens.dashboard

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.netscope.app.data.proxy.ProxyManager
import com.netscope.app.data.proxy.cert.CertificateManager
import com.netscope.app.domain.model.BandwidthSnapshot
import com.netscope.app.domain.model.ConnectionEntry
import com.netscope.app.domain.usecase.DetectAnomaliesUseCase
import com.netscope.app.domain.usecase.ObserveBandwidthUseCase
import com.netscope.app.domain.usecase.ObserveConnectionsUseCase
import com.netscope.app.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val proxyManager: ProxyManager,
    private val certificateManager: CertificateManager,
    private val observeBandwidthUseCase: ObserveBandwidthUseCase,
    private val observeConnectionsUseCase: ObserveConnectionsUseCase,
    private val detectAnomaliesUseCase: DetectAnomaliesUseCase,
) : BaseViewModel<DashboardUiState>(DashboardUiState()) {

    private val prefs get() =
        context.getSharedPreferences("netscope_prefs", Context.MODE_PRIVATE)

    init {
        updateState { copy(isCertificateInstalled = isCertInstalled()) }
        observeProxyState()
        observeLiveData()
    }

    private fun observeProxyState() {
        proxyManager.isRunningFlow
            .onEach { running ->
                updateState { copy(isProxyRunning = running) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeLiveData() {
        combine(
            observeBandwidthUseCase(),
            observeConnectionsUseCase(),
            detectAnomaliesUseCase(),
        ) { bandwidth, connections, anomalies ->
            updateState {
                copy(
                    totalUploadBytesPerSec = bandwidth.totalUploadBytesPerSec,
                    totalDownloadBytesPerSec = bandwidth.totalDownloadBytesPerSec,
                    perAppBandwidth = bandwidth.perApp,
                    topConsumer = bandwidth.topConsumer,
                    activeConnections = connections.active,
                    activeConnectionCount = connections.activeCount,
                    anomalies = anomalies,
                    error = null,
                )
            }
        }
            .catch { e -> updateState { copy(error = e.message) } }
            .launchIn(viewModelScope)
    }

    fun getCaCertificateBytes(): ByteArray =
        certificateManager.getCaCertificateBytes()

    fun markCertificateInstalled() {
        prefs.edit().putBoolean("cert_installed", true).apply()
        updateState { copy(isCertificateInstalled = true) }
    }

    fun isCertInstalled(): Boolean =
        prefs.getBoolean("cert_installed", false)

    fun dismissError() = updateState { copy(error = null) }
}