package com.netscope.app.presentation.screens.traffic

import com.netscope.app.domain.model.HttpTransaction
import com.netscope.app.domain.model.TrafficFilter

data class TrafficListUiState(
    val transactions: List<HttpTransaction> = emptyList(),
    val filter: TrafficFilter = TrafficFilter(),
    val isLoading: Boolean = false,
    val totalCount: Int = 0,
    val filteredCount: Int = 0,
    val error: String? = null,
)