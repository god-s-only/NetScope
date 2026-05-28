package com.netscope.app.presentation.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.netscope.app.presentation.components.EmptyState
import com.netscope.app.presentation.components.NetScopeTopBar
import com.netscope.app.presentation.components.formatBytes
import com.netscope.app.presentation.components.formatDuration
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
fun StatsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = NetScopeBackground,
        topBar = {
            NetScopeTopBar(
                title = "Statistics",
                onNavigateBack = onNavigateBack,
            )
        },
    ) { padding ->
        if (state.isLoading) {
            EmptyState(
                title = "Loading…",
                subtitle = "",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        if (state.totalRequests == 0) {
            EmptyState(
                title = "No data yet",
                subtitle = "Capture some traffic to see statistics",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // ── Overview ──────────────────────────────────────
            item {
                SectionTitle("Overview")
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatCard(
                        label = "Total requests",
                        value = state.totalRequests.toString(),
                        valueColor = NetScopePrimary,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Unique hosts",
                        value = state.totalUniqueHosts.toString(),
                        valueColor = NetScopeInfo,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatCard(
                        label = "DNS domains",
                        value = state.totalUniqueDomains.toString(),
                        valueColor = NetScopeInfo,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Connections",
                        value = state.totalConnections.toString(),
                        valueColor = NetScopePrimary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ── Data transferred ──────────────────────────────
            item { SectionTitle("Data transferred") }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatCard(
                        label = "Sent",
                        value = formatBytes(state.totalBytesSent),
                        valueColor = NetScopeError,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Received",
                        value = formatBytes(state.totalBytesReceived),
                        valueColor = NetScopeSuccess,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ── Response status ───────────────────────────────
            item { SectionTitle("Response status") }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatCard(
                        label = "Success (2xx)",
                        value = state.successCount.toString(),
                        valueColor = NetScopeSuccess,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Redirects (3xx)",
                        value = state.redirectCount.toString(),
                        valueColor = NetScopeWarning,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Errors (4xx/5xx)",
                        value = state.errorCount.toString(),
                        valueColor = NetScopeError,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                StatRow(
                    label = "Error rate",
                    value = "${"%.1f".format(state.errorRate)}%",
                    valueColor = if (state.errorRate > 10f)
                        NetScopeError else NetScopeSuccess,
                )
            }

            // ── Timing ────────────────────────────────────────
            item { SectionTitle("Timing") }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatCard(
                        label = "Avg response",
                        value = formatDuration(state.avgResponseTimeMs),
                        valueColor = NetScopePrimary,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Fastest",
                        value = formatDuration(state.fastestRequestMs),
                        valueColor = NetScopeSuccess,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        label = "Slowest",
                        value = formatDuration(state.slowestRequestMs),
                        valueColor = if (state.slowestRequestMs > 2000)
                            NetScopeError else NetScopeWarning,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                StatRow(
                    label = "Slowest host",
                    value = state.slowestRequestUrl,
                )
            }
            item {
                StatRow(
                    label = "Fastest host",
                    value = state.fastestRequestUrl,
                )
            }

            // ── Top activity ──────────────────────────────────
            item { SectionTitle("Top activity") }
            item {
                StatRow(
                    label = "Most active host",
                    value = "${state.mostActiveHost} " +
                            "(${state.mostActiveHostCount} requests)",
                )
            }
            item {
                StatRow(
                    label = "Most used method",
                    value = state.mostUsedMethod,
                    valueColor = NetScopePrimary,
                    monospace = true,
                )
            }
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.bodySmall,
        color = TextTertiary,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = NetScopePrimary,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(NetScopeSurface)
            .padding(14.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = TextPrimary,
    monospace: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NetScopeSurface)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.weight(0.45f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.55f),
        )
    }
}