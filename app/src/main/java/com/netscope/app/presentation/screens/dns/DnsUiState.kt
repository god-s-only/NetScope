package com.netscope.app.presentation.screens.dns

import com.netscope.app.domain.model.DnsEntry

data class DnsUiState(
    val entries: List<DnsEntry> = emptyList(),
    val searchQuery: String = "",
    val totalCount: Int = 0,
    val showClearConfirmDialog: Boolean = false,
    val error: String? = null,
)