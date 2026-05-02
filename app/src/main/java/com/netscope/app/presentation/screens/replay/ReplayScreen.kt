package com.netscope.app.presentation.screens.replay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.netscope.app.presentation.theme.NetScopePrimary
import com.netscope.app.presentation.theme.TextPrimary
import com.netscope.app.presentation.theme.TextSecondary
import com.netscope.app.presentation.theme.TextTertiary
import com.netscope.app.presentation.components.NetScopeTopBar
import com.netscope.app.presentation.components.StatusChip
import com.netscope.app.presentation.components.formatDuration
import com.netscope.app.presentation.theme.*

@Composable
fun ReplayScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReplayViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddHeaderDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = NetScopeBackground,
        topBar = {
            NetScopeTopBar(
                title = "Replay request",
                onNavigateBack = onNavigateBack,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::sendReplay,
                containerColor = NetScopePrimary,
                contentColor = NetScopeBackground,
                icon = {
                    if (state.isReplaying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = NetScopeBackground,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                },
                text = { Text(if (state.isReplaying) "Sending…" else "Send") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            ReplaySection(label = "URL") {
                OutlinedTextField(
                    value = state.editableUrl,
                    onValueChange = viewModel::onUrlChanged,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = replayTextFieldColors(),
                )
            }

            ReplaySection(
                label = "Headers",
                action = {
                    IconButton(onClick = { showAddHeaderDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add header",
                            tint = NetScopePrimary)
                    }
                },
            ) {
                state.editableHeaders.entries.forEach { (key, value) ->
                    HeaderEditRow(
                        headerKey = key,
                        headerValue = value,
                        onRemove = { viewModel.onHeaderRemoved(key) },
                    )
                }
            }

            ReplaySection(label = "Body") {
                OutlinedTextField(
                    value = state.editableBody,
                    onValueChange = viewModel::onBodyChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    colors = replayTextFieldColors(),
                    placeholder = {
                        Text("Request body (JSON, form data…)", color = TextTertiary)
                    },
                )
            }

            state.replayResult?.let { result ->
                ReplaySection(label = "Result") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StatusChip(
                            code = result.responseCode,
                            category = result.statusCategory,
                        )
                        Text(
                            text = formatDuration(result.durationMs),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = result.responseBody ?: "No body",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                    )
                }
            }

            state.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = NetScopeError,
                )
            }

            Spacer(Modifier.height(72.dp))
        }
    }


    if (showAddHeaderDialog) {
        AddHeaderDialog(
            onConfirm = { key, value ->
                viewModel.onHeaderAdded(key, value)
                showAddHeaderDialog = false
            },
            onDismiss = { showAddHeaderDialog = false },
        )
    }
}

@Composable
private fun ReplaySection(
    label: String,
    action: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NetScopeSurface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
            )
            action?.invoke()
        }
        content()
    }
}

@Composable
private fun HeaderEditRow(
    headerKey: String,
    headerValue: String,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "$headerKey:",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.weight(0.35f),
        )
        Text(
            text = headerValue,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            modifier = Modifier.weight(0.55f),
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(24.dp),
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Remove header",
                tint = NetScopeError,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun AddHeaderDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var key   by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NetScopeSurface,
        title = { Text("Add header", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Key", color = TextTertiary) },
                    singleLine = true,
                    colors = replayTextFieldColors(),
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Value", color = TextTertiary) },
                    singleLine = true,
                    colors = replayTextFieldColors(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (key.isNotBlank()) onConfirm(key.trim(), value.trim()) },
            ) { Text("Add", color = NetScopePrimary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
    )
}

@Composable
private fun replayTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NetScopePrimary,
    unfocusedBorderColor = TextTertiary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = NetScopePrimary,
)