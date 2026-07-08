package com.netscope.app.presentation.screens.integration

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.netscope.app.presentation.components.NetScopeTopBar
import com.netscope.app.presentation.theme.NetScopeBackground
import com.netscope.app.presentation.theme.NetScopePrimary
import com.netscope.app.presentation.theme.NetScopeSurface
import com.netscope.app.presentation.theme.NetScopeSurface2
import com.netscope.app.presentation.theme.TextPrimary
import com.netscope.app.presentation.theme.TextSecondary
import com.netscope.app.presentation.theme.TextTertiary

@Composable
fun IntegrationScreen(
    onNavigateBack: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current

    Scaffold(
        containerColor = NetScopeBackground,
        topBar = {
            NetScopeTopBar(
                title = "Add to your app",
                onNavigateBack = onNavigateBack,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            item {
                Text(
                    text = "Three steps to capture traffic from your app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }

            // ── Step 1 ────────────────────────────────────────
            item {
                StepCard(
                    step = 1,
                    title = "Add the dependency",
                    description = "Add NetScope interceptor to your app's build.gradle.kts",
                ) {
                    val code = """dependencies {
    debugImplementation("com.netscope:interceptor:1.0.0")
}"""
                    CodeBlock(
                        code = code,
                        onCopy = { clipboard.setText(AnnotatedString(code)) },
                    )
                }
            }

            // ── Step 2 ────────────────────────────────────────
            item {
                StepCard(
                    step = 2,
                    title = "Add to your OkHttpClient",
                    description = "Add the interceptor to your OkHttpClient builder.",
                ) {
                    val code = """val client = OkHttpClient.Builder()
    .addInterceptor(NetScopeInterceptor(context))
    .build()"""
                    CodeBlock(
                        code = code,
                        onCopy = { clipboard.setText(AnnotatedString(code)) },
                    )
                }
            }

            // ── Step 3 ────────────────────────────────────────
            item {
                StepCard(
                    step = 3,
                    title = "Trust the CA certificate",
                    description = "Add this to your app's res/xml/ folder so HTTPS " +
                            "bodies are captured in debug builds.",
                ) {
                    val code = """<!-- res/xml/network_security_config.xml -->
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <debug-overrides>
        <trust-anchors>
            <certificates src="system" />
            <certificates src="user" />
        </trust-anchors>
    </debug-overrides>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>"""
                    CodeBlock(
                        code = code,
                        onCopy = { clipboard.setText(AnnotatedString(code)) },
                    )

                    val manifestCode = """<!-- AndroidManifest.xml -->
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>"""
                    CodeBlock(
                        code = manifestCode,
                        onCopy = { clipboard.setText(AnnotatedString(manifestCode)) },
                    )
                }
            }

            // ── Note ──────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NetScopePrimary.copy(alpha = 0.10f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "How it works",
                        style = MaterialTheme.typography.titleMedium,
                        color = NetScopePrimary,
                    )
                    Text(
                        text = "The interceptor runs inside your app's OkHttp stack. " +
                                "It captures every request and response and sends them " +
                                "to NetScope via a ContentProvider. Works on WiFi and " +
                                "mobile data. No proxy setup required.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Text(
                        text = "Only use debugImplementation — never ship the " +
                                "interceptor in a production build.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun StepCard(
    step: Int,
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
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
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(NetScopePrimary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = step.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = NetScopeBackground,
                )
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
private fun CodeBlock(code: String, onCopy: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NetScopeSurface2)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(20.dp),
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = TextTertiary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            Text(
                text = code,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary,
            )
        }
    }
}