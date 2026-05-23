package com.netscope.app.presentation.screens.connections

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.netscope.app.domain.model.ConnectionEntry
import com.netscope.app.presentation.components.EmptyState
import com.netscope.app.presentation.components.NetScopeTopBar
import com.netscope.app.presentation.components.formatBytes
import com.netscope.app.presentation.theme.NetScopeBackground
import com.netscope.app.presentation.theme.NetScopeError
import com.netscope.app.presentation.theme.NetScopePrimary
import com.netscope.app.presentation.theme.NetScopeSurface
import com.netscope.app.presentation.theme.NetScopeSuccess
import com.netscope.app.presentation.theme.TextPrimary
import com.netscope.app.presentation.theme.TextSecondary
import com.netscope.app.presentation.theme.TextTertiary

@Composable
fun ConnectionsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ConnectionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val displayList = when (state.selectedTab) {
        ConnectionTab.ALL -> state.allConnections
        ConnectionTab.FLAGGED -> state.flaggedConnections
    }

    Scaffold(
        containerColor = NetScopeBackground,
        topBar = {
            NetScopeTopBar(
                title = "Connections (${state.allConnections.size})",
                onNavigateBack = onNavigateBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {

            // ── Tab row ───────────────────────────────────────
            TabRow(
                selectedTabIndex = state.selectedTab.ordinal,
                containerColor = NetScopeSurface,
                contentColor = NetScopePrimary,
            ) {
                ConnectionTab.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { viewModel.onTabSelected(tab) },
                        text = {
                            Text(
                                text = when (tab) {
                                    ConnectionTab.ALL ->
                                        "All (${state.allConnections.size})"
                                    ConnectionTab.FLAGGED ->
                                        "Flagged (${state.flaggedConnections.size})"
                                },
                                color = if (state.selectedTab == tab)
                                    NetScopePrimary else TextSecondary,
                            )
                        },
                    )
                }
            }

            // ── List ──────────────────────────────────────────
            if (displayList.isEmpty()) {
                EmptyState(
                    title = "No connections",
                    subtitle = when (state.selectedTab) {
                        ConnectionTab.ALL -> "Browse some sites to see connections"
                        ConnectionTab.FLAGGED -> "No suspicious connections detected"
                    },
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(
                        items = displayList,
                        key = { it.id },
                    ) { connection ->
                        ConnectionRow(connection = connection)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionRow(connection: ConnectionEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (connection.isFlagged)
                    NetScopeError.copy(alpha = 0.10f)
                else
                    NetScopeSurface
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    when {
                        connection.isFlagged -> NetScopeError
                        connection.isActive -> NetScopeSuccess
                        else -> TextTertiary
                    }
                ),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = connection.displayHost,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            Text(
                text = "${connection.protocol.name} · ${connection.displayPort}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary,
            )
            connection.flagReason?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = NetScopeError,
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "↑ ${formatBytes(connection.totalBytesSent)}",
                style = MaterialTheme.typography.bodySmall,
                color = NetScopeError,
            )
            Text(
                text = "↓ ${formatBytes(connection.totalBytesReceived)}",
                style = MaterialTheme.typography.bodySmall,
                color = NetScopeSuccess,
            )
        }
    }
}