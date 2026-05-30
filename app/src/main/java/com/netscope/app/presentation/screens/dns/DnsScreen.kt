package com.netscope.app.presentation.screens.dns

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.netscope.app.presentation.theme.NetScopeBackground
import com.netscope.app.presentation.theme.NetScopeError
import com.netscope.app.presentation.theme.NetScopePrimary
import com.netscope.app.presentation.theme.NetScopeSurface
import com.netscope.app.presentation.theme.TextPrimary
import com.netscope.app.presentation.theme.TextSecondary
import com.netscope.app.presentation.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                actions = {
                    IconButton(onClick = viewModel::onClearTapped) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Clear DNS log",
                            tint = TextSecondary,
                        )
                    }
                },
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
                    Icon(Icons.Default.Search, null, tint = TextTertiary)
                },
                trailingIcon = {
                    if (state.searchQuery.isNotBlank()) {
                        IconButton(onClick = viewModel::clearSearch) {
                            Icon(Icons.Default.Clear, null, tint = TextTertiary)
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
                text = "${state.totalCount} domains",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            if (state.entries.isEmpty()) {
                EmptyState(
                    title = "No DNS queries",
                    subtitle = "Browse any site while the proxy is running",
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
                        items = state.entries,
                        key = { it.id },
                    ) { entry ->
                        DnsEntryRow(entry = entry)
                    }
                }
            }
        }
    }

    if (state.showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = viewModel::onClearDismissed,
            containerColor = NetScopeSurface,
            title = { Text("Clear DNS log?", color = TextPrimary) },
            text = {
                Text(
                    "All DNS entries will be permanently deleted.",
                    color = TextSecondary,
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::onClearConfirmed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NetScopeError,
                    ),
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onClearDismissed) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        )
    }
}

@Composable
private fun DnsEntryRow(entry: DnsEntry) {
    val timeFormat = remember {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NetScopeSurface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.domain,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            if (entry.resolvedIps.isNotEmpty()) {
                Text(
                    text = entry.resolvedIps.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary,
                )
            }
            entry.responseTimeMs?.let { ms ->
                Text(
                    text = "${ms}ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = entry.queryType.name,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = NetScopePrimary,
            )
            Text(
                text = timeFormat.format(Date(entry.timestampMs)),
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
            )
        }
    }
}