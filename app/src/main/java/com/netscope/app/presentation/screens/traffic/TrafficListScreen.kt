package com.netscope.app.presentation.screens.traffic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.netscope.app.domain.model.HttpMethod
import com.netscope.app.domain.model.HttpTransaction
import com.netscope.app.domain.model.StatusCategory
import com.netscope.app.domain.model.TrafficFilter
import com.netscope.app.presentation.components.*
import com.netscope.app.presentation.screens.traffic.TrafficListViewModel
import com.netscope.app.presentation.theme.*

@Composable
fun TrafficListScreen(
    onTransactionClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: TrafficListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilterSheet by remember { mutableStateOf(false) }

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
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextTertiary)
                },
                trailingIcon = {
                    if (state.filter.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
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

            if (state.filter.isActive) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.filter.showSlowOnly) {
                        item {
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.onShowSlowOnlyToggled() },
                                label = { Text("Slow > 2s") },
                            )
                        }
                    }
                    if (state.filter.showErrorsOnly) {
                        item {
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.onShowErrorsOnlyToggled() },
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
                Spacer(Modifier.height(4.dp))
            }

            Text(
                text = "${state.filteredCount} requests",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            if (state.transactions.isEmpty()) {
                EmptyState(
                    title = "No requests captured",
                    subtitle = "Start VPN capture to see HTTP traffic",
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(NetScopeInfo.copy(alpha = 0.10f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector        = Icons.Default.Info,
                                contentDescription = null,
                                tint               = NetScopeInfo,
                                modifier           = Modifier.size(18.dp),
                            )
                            Text(
                                text  = "HTTP capture requires the NetScopeInterceptor " +
                                        "embedded in the target app. VPN mode captures " +
                                        "packet metadata only — not encrypted HTTPS bodies.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                    }
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
            transaction.appInfo?.let {
                Text(
                    text = it.appName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            StatusChip(
                code = transaction.responseCode,
                category = transaction.statusCategory,
            )
            Spacer(Modifier.height(4.dp))
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
    currentFilter: TrafficFilter,
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
        ) {
            Text(
                "Filter",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            Spacer(Modifier.height(16.dp))

            Text("Method", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(HttpMethod.entries.filter { it != HttpMethod.UNKNOWN }) { method ->
                    FilterChip(
                        selected = method in currentFilter.methods,
                        onClick = { onMethodToggled(method) },
                        label = { Text(method.name) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Other", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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
            Spacer(Modifier.height(32.dp))
        }
    }
}