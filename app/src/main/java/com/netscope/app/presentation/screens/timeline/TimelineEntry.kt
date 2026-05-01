package com.netscope.app.presentation.screens.timeline

import com.netscope.app.domain.model.HttpTransaction

data class TimelineEntry(
    val transaction: HttpTransaction,
    val startOffsetMs: Long,
    val durationMs: Long,
    val laneIndex: Int,
)