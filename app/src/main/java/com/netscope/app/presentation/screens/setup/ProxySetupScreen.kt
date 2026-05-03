package com.netscope.app.presentation.screens.setup

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.netscope.app.presentation.components.NetScopeTopBar
import com.netscope.app.presentation.theme.NetScopeBackground
import com.netscope.app.presentation.theme.NetScopePrimary
import com.netscope.app.presentation.theme.NetScopeSuccess
import com.netscope.app.presentation.theme.NetScopeSurface
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
    val state   = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

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
                text  = "Two quick steps to start capturing traffic.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )

            SetupStepCard(
                stepNumber  = 1,
                title       = "Install CA Certificate",
                description = "Allows NetScope to read HTTPS traffic. " +
                        "One-time setup.",
                isDone      = state.isCertInstalled,
                action = {
                    Button(
                        onClick = {
                            onInstallCertificate(viewModel.getCaCertBytes())
                        },
                        enabled = !state.isCertInstalled,
                        colors  = ButtonDefaults.buttonColors(
                            containerColor = NetScopeWarning,
                        ),
                    ) {
                        Text(if (state.isCertInstalled) "Installed ✓" else "Install")
                    }
                },
            )

            SetupStepCard(
                stepNumber  = 2,
                title       = "Set WiFi Proxy",
                description = "Go to Settings → WiFi → long press your network " +
                        "→ Modify → Advanced → Proxy → Manual.\n\n" +
                        "Enter the values below exactly.",
                isDone      = state.isProxyRunning,
                action = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // host row
                        ProxyValueRow(
                            label = "Proxy hostname",
                            value = state.proxyHost,
                            onCopy = {
                                clipboard.setText(AnnotatedString(state.proxyHost))
                            },
                        )
                        // port row
                        ProxyValueRow(
                            label = "Port",
                            value = state.proxyPort.toString(),
                            onCopy = {
                                clipboard.setText(
                                    AnnotatedString(state.proxyPort.toString())
                                )
                            },
                        )

                        Spacer(Modifier.height(4.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // open wifi settings shortcut
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
                                    "Open WiFi Settings",
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }

                            // start / stop proxy server
                            Button(
                                onClick = {
                                    if (state.isProxyRunning) viewModel.stopProxy()
                                    else viewModel.startProxy()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.isProxyRunning)
                                        com.netscope.app.presentation.theme.NetScopeError
                                    else NetScopePrimary,
                                ),
                            ) {
                                Text(if (state.isProxyRunning) "Stop" else "Start")
                            }
                        }
                    }
                },
            )

            if (state.isCertInstalled && state.isProxyRunning) {
                Button(
                    onClick  = onNavigateToTraffic,
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = NetScopeSuccess,
                    ),
                ) {
                    Text("View HTTP Traffic →")
                }
            }
        }
    }
}

@Composable
private fun SetupStepCard(
    stepNumber: Int,
    title: String,
    description: String,
    isDone: Boolean,
    action: @Composable () -> Unit,
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
            val badgeBg = if (isDone) NetScopeSuccess else NetScopePrimary
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(badgeBg),
                contentAlignment = Alignment.Center,
            ) {
                if (isDone) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint     = NetScopeBackground,
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Text(
                        text  = stepNumber.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = NetScopeBackground,
                    )
                }
            }

            Text(
                text  = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
        }

        Text(
            text  = description,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )

        action()
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
                text  = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
            )
            Text(
                text       = value,
                style      = MaterialTheme.typography.titleMedium,
                color      = TextPrimary,
                fontFamily = FontFamily.Monospace,
            )
        }
        IconButton(onClick = onCopy) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "Copy",
                tint     = TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}