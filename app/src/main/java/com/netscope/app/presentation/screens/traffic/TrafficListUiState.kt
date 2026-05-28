package com.netscope.app.presentation.screens.traffic

import android.content.Intent
import com.netscope.app.domain.model.HttpTransaction
import com.netscope.app.domain.model.TrafficFilter

data class TrafficListUiState(
    val transactions: List<HttpTransaction> = emptyList(),
    val filter: TrafficFilter = TrafficFilter(),
    val isLoading: Boolean = false,
    val totalCount: Int = 0,
    val filteredCount: Int = 0,
    val isExporting: Boolean = false,
    val exportIntent: Intent? = null,
    val error: String? = null,
)