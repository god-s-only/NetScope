package com.netscope.app.presentation.screens.detail

import com.netscope.app.domain.model.HttpTransaction

data class TrafficDetailUiState(
    val transaction: HttpTransaction? = null,
    val isLoading: Boolean = true,
    val isReplaying: Boolean = false,
    val replayResult: HttpTransaction? = null,
    val activeTab: DetailTab = DetailTab.OVERVIEW,
    val error: String? = null,
    val replayError: String? = null,
)

enum class DetailTab { OVERVIEW, REQUEST, RESPONSE, HEADERS }