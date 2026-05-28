package com.netscope.app.presentation.screens.traffic

import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.netscope.app.domain.model.HttpMethod
import com.netscope.app.domain.model.HttpTransaction
import com.netscope.app.presentation.components.EmptyState
import com.netscope.app.presentation.components.MethodChip
import com.netscope.app.presentation.components.NetScopeTopBar
import com.netscope.app.presentation.components.StatusChip
import com.netscope.app.presentation.components.formatDuration
import com.netscope.app.presentation.theme.NetScopeBackground
import com.netscope.app.presentation.theme.NetScopePrimary
import com.netscope.app.presentation.theme.NetScopeSurface
import com.netscope.app.presentation.theme.NetScopeWarning
import com.netscope.app.presentation.theme.TextPrimary
import com.netscope.app.presentation.theme.TextSecondary
import com.netscope.app.presentation.theme.TextTertiary

@Composable
fun TrafficListScreen(
    onTransactionClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: TrafficListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showFilterSheet by remember { mutableStateOf(false) }

    // launch share intent when export completes
    LaunchedEffect(state.exportIntent) {
        state.exportIntent?.let { intent ->
            context.startActivity(
                Intent.createChooser(intent, "Share HAR file")
            )
            viewModel.onExportIntentConsumed()
        }
    }

    Scaffold(
        containerColor = NetScopeBackground,
        topBar = {
            NetScopeTopBar(
                title = "HTTP Traffic",
                onNavigateBack = onNavigateBack,
                actions = {
                    if (state.filter.isActive) {
                        IconButton(onClick = { viewModel.clearFilters() }) {
                            Icon(
                                Icons.Default.FilterAltOff,
                                contentDescription = "Clear filters",
                                tint = NetScopePrimary,
                            )
                        }
                    }
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = TextSecondary,
                        )
                    }
                    // export button
                    IconButton(
                        onClick = viewModel::exportTraffic,
                        enabled = !state.isExporting,
                    ) {
                        if (state.isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = NetScopePrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Export HAR",
                                tint = TextSecondary,
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.clearAllTraffic() }) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Clear all",
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

            // ── Search ────────────────────────────────────────
            OutlinedTextField(
                value = state.filter.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = {
                    Text("Search URL, host, body…", color = TextTertiary)
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, null, tint = TextTertiary)
                },
                trailingIcon = {
                    if (state.filter.searchQuery.isNotBlank()) {
                        IconButton(
                            onClick = { viewModel.onSearchQueryChanged("") }
                        ) {
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

            // ── Active filter chips ───────────────────────────
            if (state.filter.isActive) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.filter.showSlowOnly) {
                        item {
                            FilterChip(
                                selected = true,
                                onClick = viewModel::onShowSlowOnlyToggled,
                                label = { Text("Slow > 2s") },
                            )
                        }
                    }
                    if (state.filter.showErrorsOnly) {
                        item {
                            FilterChip(
                                selected = true,
                                onClick = viewModel::onShowErrorsOnlyToggled,
                                label = { Text("Errors only") },
                            )
                        }
                    }
                    items(state.filter.methods.toList()) { method ->
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.onMethodFilterToggled(method) },
                            label = { Text(method.name) },
                        )
                    }
                }
            }

            // ── Count ─────────────────────────────────────────
            Text(
                text = "${state.filteredCount} requests",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            // ── Error ─────────────────────────────────────────
            state.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = com.netscope.app.presentation.theme.NetScopeError,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            // ── List ──────────────────────────────────────────
            if (state.transactions.isEmpty()) {
                EmptyState(
                    title = "No requests captured",
                    subtitle = "Start the proxy and browse any site",
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = state.transactions,
                        key = { it.id },
                    ) { transaction ->
                        TransactionRow(
                            transaction = transaction,
                            onClick = { onTransactionClick(transaction.id) },
                        )
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            currentFilter = state.filter,
            onMethodToggled = viewModel::onMethodFilterToggled,
            onSlowToggled = viewModel::onShowSlowOnlyToggled,
            onErrorsToggled = viewModel::onShowErrorsOnlyToggled,
            onDismiss = { showFilterSheet = false },
        )
    }
}

@Composable
private fun TransactionRow(
    transaction: HttpTransaction,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NetScopeSurface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MethodChip(method = transaction.method)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.host,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = transaction.path,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (transaction.isReplay) {
                Text(
                    text = "REPLAY",
                    style = MaterialTheme.typography.bodySmall,
                    color = NetScopePrimary,
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            StatusChip(
                code = transaction.responseCode,
                category = transaction.statusCategory,
            )
            Text(
                text = formatDuration(transaction.durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = if (transaction.isSlow) NetScopeWarning else TextTertiary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    currentFilter: com.netscope.app.domain.model.TrafficFilter,
    onMethodToggled: (HttpMethod) -> Unit,
    onSlowToggled: () -> Unit,
    onErrorsToggled: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NetScopeSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Filter",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )

            Text(
                "Method",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    HttpMethod.entries.filter { it != HttpMethod.UNKNOWN }
                ) { method ->
                    FilterChip(
                        selected = method in currentFilter.methods,
                        onClick = { onMethodToggled(method) },
                        label = { Text(method.name) },
                    )
                }
            }

            Text(
                "Other",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = currentFilter.showSlowOnly,
                    onClick = onSlowToggled,
                    label = { Text("Slow > 2s") },
                )
                FilterChip(
                    selected = currentFilter.showErrorsOnly,
                    onClick = onErrorsToggled,
                    label = { Text("Errors only") },
                )
            }
        }
    }
}