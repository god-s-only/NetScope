package com.netscope.app.presentation.screens.setup

data class ProxySetupUiState(
    val isCertInstalled: Boolean = false,
    val isProxyRunning: Boolean = false,
    val proxyHost: String = "127.0.0.1",
    val proxyPort: Int = 8888,
)
