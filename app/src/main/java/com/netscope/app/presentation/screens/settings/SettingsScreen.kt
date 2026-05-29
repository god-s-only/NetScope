package com.netscope.app.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.netscope.app.domain.model.MaxRequestsOption
import com.netscope.app.presentation.components.NetScopeTopBar
import com.netscope.app.presentation.theme.NetScopeBackground
import com.netscope.app.presentation.theme.NetScopeError
import com.netscope.app.presentation.theme.NetScopePrimary
import com.netscope.app.presentation.theme.NetScopeSurface
import com.netscope.app.presentation.theme.NetScopeSurface2
import com.netscope.app.presentation.theme.NetScopeSuccess
import com.netscope.app.presentation.theme.TextPrimary
import com.netscope.app.presentation.theme.TextSecondary
import com.netscope.app.presentation.theme.TextTertiary

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSetup: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.clearSuccess) {
        if (state.clearSuccess) {
            viewModel.onClearSuccessConsumed()
        }
    }

    Scaffold(
        containerColor = NetScopeBackground,
        topBar = {
            NetScopeTopBar(
                title = "Settings",
                onNavigateBack = onNavigateBack,
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

            // ── Capture ───────────────────────────────────────
            item { SectionTitle("Capture") }

            item {
                SettingsCard {
                    MaxRequestsRow(
                        current = state.settings.maxStoredRequests,
                        onChanged = viewModel::onMaxRequestsChanged,
                    )
                    SettingsDivider()
                    SwitchRow(
                        title = "Show replayed requests",
                        subtitle = "Include replay results in HTTP Traffic list",
                        checked = state.settings.showReplayedRequests,
                        onCheckedChange = viewModel::onShowReplayedChanged,
                    )
                    SettingsDivider()
                    SwitchRow(
                        title = "Auto scroll traffic list",
                        subtitle = "Scroll to latest request automatically",
                        checked = state.settings.autoScrollTrafficList,
                        onCheckedChange = viewModel::onAutoScrollChanged,
                    )
                }
            }

            // ── Certificate ───────────────────────────────────
            item { SectionTitle("Certificate") }

            item {
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                "CA Certificate",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                            )
                            Text(
                                if (state.isCertInstalled) "Installed"
                                else "Not installed",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.isCertInstalled)
                                    NetScopeSuccess else NetScopeError,
                            )
                        }
                        OutlinedButton(onClick = onNavigateToSetup) {
                            Text(
                                if (state.isCertInstalled) "Reinstall"
                                else "Install",
                                color = NetScopePrimary,
                            )
                        }
                    }
                }
            }

            // ── Data ──────────────────────────────────────────
            item { SectionTitle("Data") }

            item {
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                "Clear all data",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                            )
                            Text(
                                "Delete all traffic, DNS and connection logs",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        Button(
                            onClick = viewModel::onClearDataTapped,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NetScopeError,
                            ),
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }

            // ── About ─────────────────────────────────────────
            item { SectionTitle("About") }

            item {
                SettingsCard {
                    AboutRow(label = "Version", value = state.appVersion)
                    SettingsDivider()
                    AboutRow(
                        label = "Package",
                        value = state.packageName,
                        monospace = true,
                    )
                    SettingsDivider()
                    AboutRow(label = "Proxy port", value = "8888")
                    SettingsDivider()
                    AboutRow(
                        label = "CA certificate",
                        value = if (state.isCertInstalled)
                            "Installed" else "Not installed",
                        valueColor = if (state.isCertInstalled)
                            NetScopeSuccess else NetScopeError,
                    )
                }
            }
        }
    }

    // ── Clear confirm dialog ──────────────────────────────────
    if (state.showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = viewModel::onClearDataDismissed,
            containerColor = NetScopeSurface,
            title = {
                Text("Clear all data?", color = TextPrimary)
            },
            text = {
                Text(
                    "This will permanently delete all captured traffic, " +
                            "DNS entries and connection logs.",
                    color = TextSecondary,
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::onClearDataConfirmed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NetScopeError,
                    ),
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onClearDataDismissed) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        )
    }
}


@Composable
fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.bodySmall,
        color = TextTertiary,
        modifier = Modifier.padding(top = 4.dp, start = 4.dp),
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NetScopeSurface),
        content = content,
    )
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 14.dp),
        color = NetScopeSurface2,
    )
}

@Composable
fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NetScopeBackground,
                checkedTrackColor = NetScopePrimary,
            ),
        )
    }
}

@Composable
fun MaxRequestsRow(
    current: Int,
    onChanged: (MaxRequestsOption) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = MaxRequestsOption.entries
        .find { it.value == current }?.label ?: "500"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                "Max stored requests",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
            )
            Text(
                "Older requests deleted when limit reached",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }

        Column {
            OutlinedButton(onClick = { expanded = true }) {
                Text(currentLabel, color = NetScopePrimary)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                MaxRequestsOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label, color = TextPrimary) },
                        onClick = {
                            onChanged(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun AboutRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = TextPrimary,
    monospace: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
        )
    }
}