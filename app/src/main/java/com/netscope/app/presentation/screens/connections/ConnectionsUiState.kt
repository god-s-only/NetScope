package com.netscope.app.presentation.screens.connections

import com.netscope.app.domain.model.ConnectionEntry

data class ConnectionsUiState(
    val allConnections: List<ConnectionEntry> = emptyList(),
    val activeConnections: List<ConnectionEntry> = emptyList(),
    val flaggedConnections: List<ConnectionEntry> = emptyList(),
    val activeCount: Int = 0,
    val selectedTab: ConnectionTab = ConnectionTab.ACTIVE,
    val error: String? = null,
)

enum class ConnectionTab { ACTIVE, ALL, FLAGGED }