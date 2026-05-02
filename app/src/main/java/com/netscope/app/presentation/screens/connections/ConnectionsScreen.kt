package com.netscope.app.presentation.screens.connections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.netscope.app.presentation.theme.*

@Composable
fun ConnectionsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ConnectionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = NetScopeBackground,
        topBar = {
            NetScopeTopBar(
                title = "Connections (${state.activeCount} active)",
                onNavigateBack = onNavigateBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
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
                                when (tab) {
                                    ConnectionTab.ACTIVE  -> "Active (${state.activeConnections.size})"
                                    ConnectionTab.ALL     -> "All (${state.allConnections.size})"
                                    ConnectionTab.FLAGGED -> "Flagged (${state.flaggedConnections.size})"
                                },
                                color = if (state.selectedTab == tab) NetScopePrimary else TextSecondary,
                            )
                        },
                    )
                }
            }

            val displayList = when (state.selectedTab) {
                ConnectionTab.ACTIVE  -> state.activeConnections
                ConnectionTab.ALL     -> state.allConnections
                ConnectionTab.FLAGGED -> state.flaggedConnections
            }

            if (displayList.isEmpty()) {
                EmptyState(
                    title = "No connections",
                    subtitle = when (state.selectedTab) {
                        ConnectionTab.ACTIVE  -> "No active connections right now"
                        ConnectionTab.ALL     -> "No connections captured yet"
                        ConnectionTab.FLAGGED -> "No suspicious connections detected"
                    },
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
                    NetScopeError.copy(alpha = 0.1f)
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
                        connection.isActive  -> NetScopeSuccess
                        else                 -> TextTertiary
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
                text = "${connection.protocol.name}  ·  ${connection.displayPort}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary,
            )
            connection.appInfo?.let {
                Text(
                    text = it.appName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                )
            }
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