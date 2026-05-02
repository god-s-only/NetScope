package com.netscope.app.presentation.screens.dashboard

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.netscope.app.domain.model.BandwidthSnapshot
import com.netscope.app.domain.usecase.DetectAnomaliesUseCase
import com.netscope.app.presentation.components.*
import com.netscope.app.presentation.theme.*

@Composable
fun DashboardScreen(
    onNavigateToTraffic: () -> Unit,
    onNavigateToDns: () -> Unit,
    onNavigateToConnections: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val vpnLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.startCapture()
        }
    }

    Scaffold(
        containerColor = NetScopeBackground,
        topBar = {
            NetScopeTopBar(
                title = "NetScope",
                actions = {
                    IconButton(onClick = {
                        if (state.isVpnActive) {
                            viewModel.stopCapture()
                        } else {
                            val intent = VpnService.prepare(context)
                            if (intent != null) {
                                vpnLauncher.launch(intent)
                            } else {
                                viewModel.startCapture()
                            }
                        }
                    }) {
                        Icon(
                            imageVector = if (state.isVpnActive)
                                Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = "Toggle VPN",
                            tint = if (state.isVpnActive) NetScopePrimary else TextSecondary,
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

            item {
                VpnStatusBanner(
                    isActive = state.isVpnActive,
                    onToggle = {
                        if (state.isVpnActive) {
                            viewModel.stopCapture()
                        } else {
                            val intent = VpnService.prepare(context)
                            if (intent != null) vpnLauncher.launch(intent)
                            else viewModel.startCapture()
                        }
                    },
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                        label = "Connections",
                        value = state.activeConnectionCount.toString(),
                        valueColor = NetScopeInfo,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    QuickNavCard(
                        icon = Icons.Default.List,
                        label = "HTTP Traffic",
                        onClick = onNavigateToTraffic,
                        modifier = Modifier.weight(1f),
                    )
                    QuickNavCard(
                        icon = Icons.Default.Dns,
                        label = "DNS Log",
                        onClick = onNavigateToDns,
                        modifier = Modifier.weight(1f),
                    )
                    QuickNavCard(
                        icon = Icons.Default.Cable,
                        label = "Connections",
                        onClick = onNavigateToConnections,
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
                items(state.perAppBandwidth) { snapshot ->
                    AppBandwidthRow(snapshot = snapshot)
                }
            }

            if (!state.isVpnActive && state.perAppBandwidth.isEmpty()) {
                item {
                    EmptyState(
                        title = "No traffic captured",
                        subtitle = "Tap the WiFi icon to start capture",
                        modifier = Modifier.padding(top = 48.dp),
                    )
                }
            }
        }

        state.error?.let { error ->
            LaunchedEffect(error) {
                viewModel.dismissError()
            }
        }
    }
}

@Composable
private fun VpnStatusBanner(isActive: Boolean, onToggle: () -> Unit) {
    val bgColor = if (isActive)
        NetScopePrimary.copy(alpha = 0.15f)
    else
        NetScopeSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isActive) Icons.Default.Shield else Icons.Default.ShieldMoon,
            contentDescription = null,
            tint = if (isActive) NetScopePrimary else TextSecondary,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isActive) "Capture active" else "Capture stopped",
                style = MaterialTheme.typography.titleMedium,
                color = if (isActive) NetScopePrimary else TextSecondary,
            )
            Text(
                text = if (isActive)
                    "All device traffic is being monitored"
                else
                    "Tap to start monitoring network traffic",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
        Button(
            onClick = onToggle,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isActive) NetScopeError else NetScopePrimary,
            ),
        ) {
            Text(if (isActive) "Stop" else "Start")
        }
    }
}

@Composable
private fun QuickNavCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(NetScopeSurface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = NetScopePrimary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
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
            .background(severityColor.copy(alpha = 0.1f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(severityColor),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = anomaly.appName ?: "Unknown app",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Text(
                text = anomaly.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun AppBandwidthRow(
    snapshot: BandwidthSnapshot,
) {
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
                text = snapshot.appInfo?.appName ?: "Unknown",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Text(
                text = snapshot.appInfo?.packageName ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "↑ ${formatBytes(snapshot.uploadBytesPerSec)}/s",
                style = MaterialTheme.typography.bodySmall,
                color = NetScopeError,
            )
            Text(
                text = "↓ ${formatBytes(snapshot.downloadBytesPerSec)}/s",
                style = MaterialTheme.typography.bodySmall,
                color = NetScopeSuccess,
            )
        }
    }
}