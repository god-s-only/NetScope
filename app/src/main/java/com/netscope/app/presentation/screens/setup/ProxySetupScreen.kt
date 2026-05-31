package com.netscope.app.presentation.screens.setup

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.netscope.app.presentation.components.NetScopeTopBar
import com.netscope.app.presentation.theme.NetScopeBackground
import com.netscope.app.presentation.theme.NetScopeError
import com.netscope.app.presentation.theme.NetScopePrimary
import com.netscope.app.presentation.theme.NetScopeSurface
import com.netscope.app.presentation.theme.NetScopeSuccess
import com.netscope.app.presentation.theme.NetScopeWarning
import com.netscope.app.presentation.theme.TextPrimary
import com.netscope.app.presentation.theme.TextSecondary
import com.netscope.app.presentation.theme.TextTertiary

@Composable
fun ProxySetupScreen(
    onInstallCertificate: (ByteArray) -> Unit,
    onNavigateToTraffic: () -> Unit,
    viewModel: ProxySetupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // start/stop polling based on screen lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onScreenResumed()
                Lifecycle.Event.ON_PAUSE -> viewModel.onScreenPaused()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = NetScopeBackground,
        topBar = {
            NetScopeTopBar(title = "Setup")
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

            Text(
                text = "Two steps to start capturing traffic.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )

            // ── Step 1 — Certificate ──────────────────────────
            SetupStepCard(
                stepNumber = 1,
                title = "Install CA Certificate",
                description = "Allows NetScope to read HTTPS traffic. One-time setup.",
                isDone = state.isCertInstalled,
            ) {
                Button(
                    onClick = {
                        onInstallCertificate(viewModel.getCaCertBytes())
                    },
                    enabled = !state.isCertInstalled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NetScopeWarning,
                        disabledContainerColor = NetScopeSuccess,
                    ),
                ) {
                    Text(if (state.isCertInstalled) "Installed ✓" else "Install")
                }
            }

            // ── Step 2 — Proxy ────────────────────────────────
            SetupStepCard(
                stepNumber = 2,
                title = "Start Proxy + Set WiFi Proxy",
                description = "Start the proxy server then go to:\n" +
                        "Settings → WiFi → long press network → " +
                        "Modify → Advanced → Proxy → Manual",
                isDone = state.isProxyRunning && state.isProxyCorrectlySet,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    // proxy values to copy
                    ProxyValueRow(
                        label = "Proxy hostname",
                        value = state.proxyHost,
                        onCopy = {
                            clipboard.setText(AnnotatedString(state.proxyHost))
                        },
                    )
                    ProxyValueRow(
                        label = "Port",
                        value = state.proxyPort.toString(),
                        onCopy = {
                            clipboard.setText(
                                AnnotatedString(state.proxyPort.toString())
                            )
                        },
                    )

                    // proxy detection status
                    ProxyStatusRow(
                        isCorrect = state.isProxyCorrectlySet,
                        message = state.proxyStatusMessage,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_WIFI_SETTINGS)
                                )
                            },
                        ) {
                            Icon(
                                Icons.Default.Wifi,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                "WiFi Settings",
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }

                        Button(
                            onClick = {
                                if (state.isProxyRunning) viewModel.stopProxy()
                                else viewModel.startProxy()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.isProxyRunning)
                                    NetScopeError else NetScopePrimary,
                            ),
                        ) {
                            Text(if (state.isProxyRunning) "Stop" else "Start")
                        }
                    }
                }
            }

            // ── Go to traffic once both done ──────────────────
            if (state.isCertInstalled &&
                state.isProxyRunning &&
                state.isProxyCorrectlySet) {
                Button(
                    onClick = onNavigateToTraffic,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NetScopeSuccess,
                    ),
                ) {
                    Text("View HTTP Traffic →")
                }
            }
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun SetupStepCard(
    stepNumber: Int,
    title: String,
    description: String,
    isDone: Boolean,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NetScopeSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isDone) NetScopeSuccess else NetScopePrimary),
                contentAlignment = Alignment.Center,
            ) {
                if (isDone) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = NetScopeBackground,
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Text(
                        text = stepNumber.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = NetScopeBackground,
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
        content()
    }
}

@Composable
private fun ProxyStatusRow(
    isCorrect: Boolean,
    message: String,
) {
    if (message.isBlank()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isCorrect)
                    NetScopeSuccess.copy(alpha = 0.12f)
                else
                    NetScopeWarning.copy(alpha = 0.12f)
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = if (isCorrect) Icons.Default.Check else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isCorrect) NetScopeSuccess else NetScopeWarning,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = if (isCorrect) NetScopeSuccess else NetScopeWarning,
        )
    }
}

@Composable
private fun ProxyValueRow(
    label: String,
    value: String,
    onCopy: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NetScopeBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace,
            )
        }
        IconButton(onClick = onCopy) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "Copy",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}