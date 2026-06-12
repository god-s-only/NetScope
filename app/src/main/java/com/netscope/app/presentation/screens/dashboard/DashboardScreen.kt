package com.netscope.app.presentation.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Cable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.netscope.app.domain.usecase.DetectAnomaliesUseCase
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
    onNavigateToConnections: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = NetScopeBackground,
        topBar = {
            NetScopeTopBar(
                title = "NetScope",
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
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

            // ── Cert banner ───────────────────────────────────
            if (!state.isCertificateInstalled) {
                item {
                    CertBanner(
                        onInstall = {
                            onInstallCertificate(viewModel.getCaCertificateBytes())
                        },
                    )
                }
            }

            // ── Proxy status ──────────────────────────────────
            item {
                ProxyStatusBanner(
                    isRunning = state.isProxyRunning,
                    onSetupTap = onNavigateToSetup,
                )
            }

            // ── Bandwidth stats ───────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatCard(
                        label = "Upload",
                        value = formatBytes(state.totalUploadBytesPerSec) + "/s",
                        valueColor = NetScopeError,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Download",
                        value = formatBytes(state.totalDownloadBytesPerSec) + "/s",
                        valueColor = NetScopeSuccess,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Conns",
                        value = state.activeConnectionCount.toString(),
                        valueColor = NetScopeInfo,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ── Quick nav grid (2-column) ─────────────────────
            item {
                QuickNavGrid(
                    onNavigateToTraffic = onNavigateToTraffic,
                    onNavigateToDns = onNavigateToDns,
                    onNavigateToTimeline = onNavigateToTimeline,
                    onNavigateToConnections = onNavigateToConnections,
                    onNavigateToStats = onNavigateToStats,
                )
            }

            // ── Anomalies ─────────────────────────────────────
            if (state.anomalies.isNotEmpty()) {
                item { SectionHeader(title = "Anomalies") }
                items(state.anomalies) { anomaly ->
                    AnomalyCard(anomaly = anomaly)
                }
            }
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

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
        Icon(
            Icons.Default.Security,
            contentDescription = null,
            tint = NetScopeWarning,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Install CA Certificate",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Required for HTTPS capture",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Button(
            onClick = onInstall,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NetScopeWarning),
        ) {
            Text(
                text = "Install",
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }
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
                if (isRunning) NetScopePrimary.copy(alpha = 0.12f) else NetScopeSurface,
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
                text = if (isRunning) "Proxy active" else "Proxy not running",
                style = MaterialTheme.typography.titleSmall,
                color = if (isRunning) NetScopePrimary else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (isRunning) "Capturing on 127.0.0.1:8888" else "Tap to open setup",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.Default.Settings,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun QuickNavGrid(
    onNavigateToTraffic: () -> Unit,
    onNavigateToDns: () -> Unit,
    onNavigateToTimeline: () -> Unit,
    onNavigateToConnections: () -> Unit,
    onNavigateToStats: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
        }
        // Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickNavCard(
                icon = Icons.Default.Timeline,
                label = "Timeline",
                onClick = onNavigateToTimeline,
                modifier = Modifier.weight(1f),
            )
            QuickNavCard(
                icon = Icons.Default.Cable,
                label = "Connections",
                onClick = onNavigateToConnections,
                modifier = Modifier.weight(1f),
            )
        }
        // Row 3 — Stats spans full width
        QuickNavCard(
            icon = Icons.Default.BarChart,
            label = "Stats",
            onClick = onNavigateToStats,
            modifier = Modifier.fillMaxWidth(),
        )
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
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = NetScopePrimary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun AnomalyCard(anomaly: DetectAnomaliesUseCase.Anomaly) {
    val color = when (anomaly.severity) {
        DetectAnomaliesUseCase.Severity.HIGH -> NetScopeError
        DetectAnomaliesUseCase.Severity.MEDIUM -> NetScopeWarning
        DetectAnomaliesUseCase.Severity.LOW -> NetScopeInfo
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = anomaly.appName ?: "Unknown",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = anomaly.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}