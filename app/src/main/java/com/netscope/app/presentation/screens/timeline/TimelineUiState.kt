package com.netscope.app.presentation.screens.timeline

import com.netscope.app.domain.model.HttpTransaction

data class TimelineUiState(
    val entries: List<TimelineEntry> = emptyList(),
    val totalDurationMs: Long = 0L,
    val earliestMs: Long = 0L,
    val latestMs: Long = 0L,
    val isLoading: Boolean = true,
    val error: String? = null,
)

data class TimelineEntry(
    val transaction: HttpTransaction,
    val startOffsetMs: Long,
    val durationMs: Long,
    val laneIndex: Int,
)