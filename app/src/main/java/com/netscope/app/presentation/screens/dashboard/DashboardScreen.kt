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
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShieldMoon
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
    onInstallCertificate: (ByteArray) -> Unit,
    onNavigateToSetup: () -> Unit,
    onNavigateToTraffic: () -> Unit,
    onNavigateToDns: () -> Unit,
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
                    androidx.compose.material3.IconButton(onClick = onNavigateToSetup) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Setup",
                            tint = TextSecondary,
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
                    CertBanner(
                        onInstall = {
                            onInstallCertificate(viewModel.getCaCertificateBytes())
                        }
                    )
                }
            }

            item {
                ProxyStatusBanner(
                    isRunning   = state.isProxyRunning,
                    onSetupTap  = onNavigateToSetup,
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
                        label      = "Connections",
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
                        icon = Icons.Default.List,
                        label = "HTTP",
                        onClick = onNavigateToTraffic,
                        modifier = Modifier.weight(1f),
                    )
                    QuickNavCard(
                        icon = Icons.Default.Dns,
                        label = "DNS",
                        onClick = onNavigateToDns,
                        modifier = Modifier.weight(1f),
                    )
                    QuickNavCard(
                        icon = Icons.Default.Timeline,
                        label = "Timeline",
                        onClick = onNavigateToTimeline,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (state.anomalies.isNotEmpty()) {
                item { SectionHeader(title = "Anomalies") }
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

            if (!state.isProxyRunning) {
                item {
                    EmptyState(
                        title    = "Proxy not running",
                        subtitle = "Tap the settings icon to set up capture",
                        modifier = Modifier.padding(top = 32.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CertBanner(onInstall: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NetScopeWarning.copy(alpha = 0.15f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Default.Security, null, tint = NetScopeWarning, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Install CA Certificate", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text("Required for HTTPS capture", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Button(
            onClick = onInstall,
            colors  = ButtonDefaults.buttonColors(containerColor = NetScopeWarning),
        ) { Text("Install") }
    }
}

@Composable
private fun ProxyStatusBanner(
    isRunning: Boolean,
    onSetupTap: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isRunning) NetScopePrimary.copy(alpha = 0.12f) else NetScopeSurface
            )
            .clickable(onClick = onSetupTap)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = if (isRunning) Icons.Default.Shield else Icons.Default.ShieldMoon,
            contentDescription = null,
            tint = if (isRunning) NetScopePrimary else TextSecondary,
            modifier = Modifier.size(28.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = if (isRunning) "Proxy active" else "Proxy not running",
                style = MaterialTheme.typography.titleMedium,
                color = if (isRunning) NetScopePrimary else TextSecondary,
            )
            Text(
                text  = if (isRunning)
                    "Capturing traffic on 127.0.0.1:8888"
                else
                    "Tap to open setup",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
        Icon(Icons.Default.Settings, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun QuickNavCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(NetScopeSurface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = label, tint = NetScopePrimary, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
    }
}

@Composable
private fun AnomalyCard(anomaly: DetectAnomaliesUseCase.Anomaly) {
    val color = when (anomaly.severity) {
        DetectAnomaliesUseCase.Severity.HIGH   -> NetScopeError
        DetectAnomaliesUseCase.Severity.MEDIUM -> NetScopeWarning
        DetectAnomaliesUseCase.Severity.LOW    -> NetScopeInfo
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color),
        )
        Column {
            Text(anomaly.appName ?: "Unknown", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(anomaly.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
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
            Text(snapshot.appInfo?.appName ?: "Unknown", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(snapshot.appInfo?.packageName ?: "", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("↑ ${formatBytes(snapshot.uploadBytesPerSec)}/s", style = MaterialTheme.typography.bodySmall, color = NetScopeError)
            Text("↓ ${formatBytes(snapshot.downloadBytesPerSec)}/s", style = MaterialTheme.typography.bodySmall, color = NetScopeSuccess)
        }
    }
}