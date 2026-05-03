package com.netscope.app.presentation.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShieldMoon
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.netscope.app.domain.model.BandwidthSnapshot
import com.netscope.app.domain.usecase.DetectAnomaliesUseCase
import com.netscope.app.presentation.components.EmptyState
import com.netscope.app.presentation.components.NetScopeTopBar
import com.netscope.app.presentation.components.SectionHeader
import com.netscope.app.presentation.components.StatCard
import com.netscope.app.presentation.components.formatBytes
import com.netscope.app.presentation.theme.NetScopeBackground
import com.netscope.app.presentation.theme.NetScopeError
import com.netscope.app.presentation.theme.NetScopeInfo
import com.netscope.app.presentation.theme.NetScopePrimary
import com.netscope.app.presentation.theme.NetScopeSurface
import com.netscope.app.presentation.theme.NetScopeSuccess
import com.netscope.app.presentation.theme.NetScopeWarning
import com.netscope.app.presentation.theme.TextPrimary
import com.netscope.app.presentation.theme.TextSecondary
import com.netscope.app.presentation.theme.TextTertiary

@Composable
fun DashboardScreen(
    onRequestVpnPermission: () -> Unit,
    onStopVpn: () -> Unit,
    onInstallCertificate: (ByteArray) -> Unit,
    onNavigateToTraffic: () -> Unit,
    onNavigateToDns: () -> Unit,
    onNavigateToConnections: () -> Unit,
    onNavigateToTimeline: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = NetScopeBackground,
        topBar = {
            NetScopeTopBar(
                title = "NetScope",
                actions = {
                    IconButton(
                        onClick = {
                            if (state.isVpnActive) {
                                onStopVpn()
                                viewModel.onVpnStopped()
                            } else {
                                onRequestVpnPermission()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (state.isVpnActive)
                                Icons.Default.Wifi
                            else
                                Icons.Default.WifiOff,
                            contentDescription = "Toggle capture",
                            tint = if (state.isVpnActive) NetScopePrimary
                            else TextSecondary,
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            if (!state.isCertificateInstalled) {
                item {
                    CertInstallBanner(
                        onInstall = {
                            val certBytes = viewModel.getCaCertificateBytes()
                            onInstallCertificate(certBytes)
                        },
                    )
                }
            }

            item {
                VpnStatusBanner(
                    isActive = state.isVpnActive,
                    onStart  = onRequestVpnPermission,
                    onStop   = {
                        onStopVpn()
                        viewModel.onVpnStopped()
                    },
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard(
                        label      = "Upload",
                        value      = formatBytes(state.totalUploadBytesPerSec) + "/s",
                        valueColor = NetScopeError,
                        modifier   = Modifier.weight(1f),
                    )
                    StatCard(
                        label      = "Download",
                        value      = formatBytes(state.totalDownloadBytesPerSec) + "/s",
                        valueColor = NetScopeSuccess,
                        modifier   = Modifier.weight(1f),
                    )
                    StatCard(
                        label      = "Active",
                        value      = state.activeConnectionCount.toString(),
                        valueColor = NetScopeInfo,
                        modifier   = Modifier.weight(1f),
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    QuickNavCard(
                        icon     = Icons.Default.List,
                        label    = "HTTP",
                        onClick  = onNavigateToTraffic,
                        modifier = Modifier.weight(1f),
                    )
                    QuickNavCard(
                        icon     = Icons.Default.Dns,
                        label    = "DNS",
                        onClick  = onNavigateToDns,
                        modifier = Modifier.weight(1f),
                    )
                    QuickNavCard(
                        icon     = Icons.Default.Cable,
                        label    = "Connections",
                        onClick  = onNavigateToConnections,
                        modifier = Modifier.weight(1f),
                    )
                    QuickNavCard(
                        icon     = Icons.Default.Timeline,
                        label    = "Timeline",
                        onClick  = onNavigateToTimeline,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (state.anomalies.isNotEmpty()) {
                item { SectionHeader(title = "Anomalies detected") }
                items(state.anomalies) { anomaly ->
                    AnomalyCard(anomaly = anomaly)
                }
            }

            if (state.perAppBandwidth.isNotEmpty()) {
                item { SectionHeader(title = "App traffic") }
                items(
                    items = state.perAppBandwidth,
                    key   = { it.appInfo?.packageName ?: it.timestampMs.toString() },
                ) { snapshot ->
                    AppBandwidthRow(snapshot = snapshot)
                }
            }

            if (!state.isVpnActive && state.perAppBandwidth.isEmpty()) {
                item {
                    EmptyState(
                        title    = "No traffic captured",
                        subtitle = "Tap Start to begin monitoring all device traffic",
                        modifier = Modifier.padding(top = 48.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CertInstallBanner(onInstall: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NetScopeWarning.copy(alpha = 0.15f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector        = Icons.Default.Security,
            contentDescription = null,
            tint               = NetScopeWarning,
            modifier           = Modifier.size(28.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = "Install CA Certificate",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Text(
                text  = "Required once to capture HTTPS traffic from all apps.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
        Button(
            onClick = onInstall,
            colors  = ButtonDefaults.buttonColors(
                containerColor = NetScopeWarning,
            ),
        ) {
            Text("Install")
        }
    }
}

@Composable
private fun VpnStatusBanner(
    isActive: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isActive) NetScopePrimary.copy(alpha = 0.12f)
                else NetScopeSurface
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isActive) Icons.Default.Shield
            else Icons.Default.ShieldMoon,
            contentDescription = null,
            tint = if (isActive) NetScopePrimary else TextSecondary,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = if (isActive) "Capture active" else "Capture stopped",
                style = MaterialTheme.typography.titleMedium,
                color = if (isActive) NetScopePrimary else TextSecondary,
            )
            Text(
                text  = if (isActive)
                    "All device traffic is being monitored"
                else
                    "Tap Start to monitor network traffic",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = if (isActive) onStop else onStart,
            colors  = ButtonDefaults.buttonColors(
                containerColor = if (isActive) NetScopeError else NetScopePrimary,
            ),
        ) {
            Text(if (isActive) "Stop" else "Start")
        }
    }
}

@Composable
private fun QuickNavCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(NetScopeSurface)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = label,
            tint               = NetScopePrimary,
            modifier           = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text  = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
    }
}

@Composable
private fun AnomalyCard(anomaly: DetectAnomaliesUseCase.Anomaly) {
    val severityColor = when (anomaly.severity) {
        DetectAnomaliesUseCase.Severity.HIGH   -> NetScopeError
        DetectAnomaliesUseCase.Severity.MEDIUM -> NetScopeWarning
        DetectAnomaliesUseCase.Severity.LOW    -> NetScopeInfo
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(severityColor.copy(alpha = 0.10f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(severityColor),
        )
        Column {
            Text(
                text  = anomaly.appName ?: "Unknown app",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Text(
                text  = anomaly.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun AppBandwidthRow(snapshot: BandwidthSnapshot) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NetScopeSurface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = snapshot.appInfo?.appName ?: "Unknown",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Text(
                text  = snapshot.appInfo?.packageName ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text  = "↑ ${formatBytes(snapshot.uploadBytesPerSec)}/s",
                style = MaterialTheme.typography.bodySmall,
                color = NetScopeError,
            )
            Text(
                text  = "↓ ${formatBytes(snapshot.downloadBytesPerSec)}/s",
                style = MaterialTheme.typography.bodySmall,
                color = NetScopeSuccess,
            )
        }
    }
}