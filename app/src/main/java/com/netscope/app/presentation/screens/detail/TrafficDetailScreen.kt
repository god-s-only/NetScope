package com.netscope.app.presentation.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.netscope.app.domain.model.HttpTransaction
import com.netscope.app.presentation.components.NetScopeTopBar
import com.netscope.app.presentation.theme.NetScopeBackground
import com.netscope.app.presentation.components.*
import com.netscope.app.presentation.theme.*

@Composable
fun TrafficDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToReplay: (String) -> Unit,
    viewModel: TrafficDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = NetScopeBackground,
        topBar = {
            NetScopeTopBar(
                title = "Request detail",
                onNavigateBack = onNavigateBack,
                actions = {
                    state.transaction?.let { tx ->
                        IconButton(onClick = { onNavigateToReplay(tx.id) }) {
                            Icon(
                                Icons.Default.Replay,
                                contentDescription = "Replay",
                                tint = NetScopePrimary,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = NetScopePrimary)
                }
            }
            state.transaction == null -> {
                EmptyState(
                    title = "Not found",
                    subtitle = "This transaction no longer exists",
                    modifier = Modifier.padding(padding),
                )
            }
            else -> {
                val tx = state.transaction!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    TabRow(
                        selectedTabIndex = state.activeTab.ordinal,
                        containerColor = NetScopeSurface,
                        contentColor = NetScopePrimary,
                    ) {
                        DetailTab.entries.forEach { tab ->
                            Tab(
                                selected = state.activeTab == tab,
                                onClick = { viewModel.onTabSelected(tab) },
                                text = {
                                    Text(
                                        tab.name.lowercase()
                                            .replaceFirstChar { it.uppercase() },
                                        color = if (state.activeTab == tab)
                                            NetScopePrimary else TextSecondary,
                                    )
                                },
                            )
                        }
                    }

                    when (state.activeTab) {
                        DetailTab.OVERVIEW  -> OverviewTab(tx)
                        DetailTab.REQUEST   -> RequestTab(tx)
                        DetailTab.RESPONSE  -> ResponseTab(tx)
                        DetailTab.HEADERS   -> HeadersTab(tx)
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(tx: HttpTransaction) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MethodChip(method = tx.method)
            StatusChip(code = tx.responseCode, category = tx.statusCategory)
            if (tx.isReplay) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(NetScopeInfo.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text("REPLAY", color = NetScopeInfo, fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace)
                }
            }
        }

        DetailSection(label = "URL") {
            CodeText(text = tx.url)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = "Duration",
                value = formatDuration(tx.durationMs),
                valueColor = if (tx.isSlow) NetScopeWarning else NetScopePrimary,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Request size",
                value = formatBytes(tx.requestSizeBytes),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Response size",
                value = formatBytes(tx.responseSizeBytes),
                modifier = Modifier.weight(1f),
            )
        }

        tx.appInfo?.let { app ->
            DetailSection(label = "App") {
                Text(app.appName, color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium)
                Text(app.packageName, color = TextTertiary,
                    style = MaterialTheme.typography.bodySmall)
            }
        }

        tx.error?.let { err ->
            DetailSection(label = "Error") {
                Text(err, color = NetScopeError,
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun RequestTab(tx: HttpTransaction) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DetailSection(label = "Method") {
            Text(tx.method.name, color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium)
        }
        DetailSection(label = "URL") { CodeText(tx.url) }
        if (tx.requestBody != null) {
            DetailSection(label = "Body") { CodeText(tx.requestBody) }
        } else {
            Text("No request body", color = TextTertiary,
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ResponseTab(tx: HttpTransaction) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DetailSection(label = "Status") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                StatusChip(code = tx.responseCode, category = tx.statusCategory)
                Text(tx.responseMessage ?: "", color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (tx.responseBody != null) {
            DetailSection(label = "Body") { CodeText(tx.responseBody) }
        } else {
            Text("No response body", color = TextTertiary,
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun HeadersTab(tx: HttpTransaction) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DetailSection(label = "Request headers") {
            HeaderTable(headers = tx.requestHeaders)
        }
        DetailSection(label = "Response headers") {
            HeaderTable(headers = tx.responseHeaders)
        }
    }
}

@Composable
private fun HeaderTable(headers: Map<String, String>) {
    if (headers.isEmpty()) {
        Text("None", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        headers.forEach { (key, value) ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = key,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary,
                    modifier = Modifier.weight(0.4f),
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary,
                    modifier = Modifier.weight(0.6f),
                )
            }
        }
    }
}

@Composable
private fun DetailSection(label: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NetScopeSurface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.bodySmall,
            color = TextTertiary)
        content()
    }
}

@Composable
private fun CodeText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = TextPrimary,
        )
    }
}