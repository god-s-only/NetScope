package com.netscope.app.presentation.screens.setup

import com.netscope.app.data.proxy.LocalProxyServer

data class ProxySetupUiState(
    val isCertInstalled: Boolean = false,
    val isProxyRunning: Boolean = false,
    val isProxyCorrectlySet: Boolean = false,
    val proxyHost: String = "127.0.0.1",
    val proxyPort: Int = LocalProxyServer.PORT,
    val proxyStatusMessage: String = "",
)