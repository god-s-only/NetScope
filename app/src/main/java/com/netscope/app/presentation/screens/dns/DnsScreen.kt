package com.netscope.presentation.screens.dns

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.netscope.app.domain.model.DnsEntry
import com.netscope.app.presentation.components.EmptyState
import com.netscope.app.presentation.components.NetScopeTopBar
import com.netscope.app.presentation.components.formatDuration
import com.netscope.app.presentation.screens.dns.DnsViewModel
import com.netscope.app.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DnsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DnsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = NetScopeBackground,
        topBar = {
            NetScopeTopBar(
                title = "DNS Log",
                onNavigateBack = onNavigateBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search domain…", color = TextTertiary) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextTertiary)
                },
                trailingIcon = {
                    if (state.searchQuery.isNotBlank()) {
                        IconButton(onClick = viewModel::clearSearch) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextTertiary)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NetScopePrimary,
                    unfocusedBorderColor = TextTertiary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = NetScopePrimary,
                ),
            )

            Text(
                text = "${state.totalCount} queries",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            if (state.entries.isEmpty()) {
                EmptyState(
                    title = "No DNS queries",
                    subtitle = "DNS lookups will appear here once capture is active",
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(
                        items = state.entries,
                        key = { it.id },
                    ) { entry ->
                        DnsEntryRow(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun DnsEntryRow(entry: DnsEntry) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NetScopeSurface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = entry.domain,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = entry.queryType.name,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = NetScopePrimary,
            )
        }

        if (entry.resolvedIps.isNotEmpty()) {
            Text(
                text = entry.resolvedIps.joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = entry.appInfo?.appName ?: "Unknown app",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
            )
            Text(
                text = timeFormat.format(Date(entry.timestampMs)),
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
            )
        }
    }
}