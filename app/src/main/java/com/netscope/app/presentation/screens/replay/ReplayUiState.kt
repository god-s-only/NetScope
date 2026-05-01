package com.netscope.app.presentation.screens.replay

import com.netscope.app.domain.model.HttpTransaction

data class ReplayUiState(
    val originalTransaction: HttpTransaction? = null,
    val editableUrl: String = "",
    val editableHeaders: Map<String, String> = emptyMap(),
    val editableBody: String = "",
    val isLoading: Boolean = true,
    val isReplaying: Boolean = false,
    val replayResult: HttpTransaction? = null,
    val error: String? = null,
)