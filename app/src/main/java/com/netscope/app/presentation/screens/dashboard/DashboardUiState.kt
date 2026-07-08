package com.netscope.app.presentation.screens.dashboard

import com.netscope.app.domain.model.BandwidthSnapshot
import com.netscope.app.domain.model.ConnectionEntry
import com.netscope.app.domain.usecase.DetectAnomaliesUseCase

data class DashboardUiState(
    val isCertificateInstalled: Boolean = false,
    val totalUploadBytesPerSec: Long = 0L,
    val totalDownloadBytesPerSec: Long = 0L,
    val perAppBandwidth: List<BandwidthSnapshot> = emptyList(),
    val activeConnections: List<ConnectionEntry> = emptyList(),
    val activeConnectionCount: Int = 0,
    val anomalies: List<DetectAnomaliesUseCase.Anomaly> = emptyList(),
    val topConsumer: BandwidthSnapshot? = null,
    val error: String? = null,
)