package com.netscope.app.presentation.screens.dashboard

import androidx.lifecycle.viewModelScope
import com.netscope.app.data.vpn.VpnController
import com.netscope.app.domain.usecase.DetectAnomaliesUseCase
import com.netscope.app.domain.usecase.ObserveBandwidthUseCase
import com.netscope.app.domain.usecase.ObserveConnectionsUseCase
import com.netscope.app.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val observeBandwidthUseCase: ObserveBandwidthUseCase,
    private val observeConnectionsUseCase: ObserveConnectionsUseCase,
    private val detectAnomaliesUseCase: DetectAnomaliesUseCase,
    private val vpnController: VpnController,
) : BaseViewModel<DashboardUiState>(DashboardUiState()) {

    init {
        observeLiveData()
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

    fun startCapture() {
        viewModelScope.launch {
            if (!vpnController.isVpnPermissionGranted()) {
                updateState { copy(error = "VPN permission required") }
                return@launch
            }
            vpnController.startCapture()
            updateState { copy(isVpnActive = true) }
        }
    }

    fun stopCapture() {
        vpnController.stopCapture()
        updateState { copy(isVpnActive = false) }
    }

    fun dismissError() = updateState { copy(error = null) }
}