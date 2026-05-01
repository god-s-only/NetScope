package com.netscope.app.presentation.screens.timeline

import androidx.lifecycle.viewModelScope
import com.netscope.app.domain.model.HttpTransaction
import com.netscope.app.domain.usecase.ObserveHttpTransactionsUseCase
import com.netscope.app.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val observeHttpTransactionsUseCase: ObserveHttpTransactionsUseCase,
) : BaseViewModel<TimelineUiState>(TimelineUiState()) {

    init {
        observeTimeline()
    }

    private fun observeTimeline() {
        observeHttpTransactionsUseCase()
            .map { transactions -> buildTimeline(transactions) }
            .onEach { timelineState ->
                updateState {
                    copy(
                        entries = timelineState.entries,
                        totalDurationMs = timelineState.totalDurationMs,
                        earliestMs = timelineState.earliestMs,
                        latestMs = timelineState.latestMs,
                        isLoading = false,
                        error = null,
                    )
                }
            }
            .catch { e -> updateState { copy(error = e.message, isLoading = false) } }
            .launchIn(viewModelScope)
    }

    private fun buildTimeline(transactions: List<HttpTransaction>): TimelineUiState {
        if (transactions.isEmpty()) return TimelineUiState(isLoading = false)

        val sorted = transactions.sortedBy { it.timestampMs }
        val earliest = sorted.first().timestampMs
        val latest = sorted.maxOf { it.timestampMs + it.durationMs }
        val totalDuration = latest - earliest

        val laneEndTimes = mutableListOf<Long>()

        val entries = sorted.map { tx ->
            val startOffset = tx.timestampMs - earliest
            val laneIndex = laneEndTimes.indexOfFirst { endTime ->
                endTime <= tx.timestampMs
            }.takeIf { it >= 0 } ?: run {
                laneEndTimes.add(0L)
                laneEndTimes.lastIndex
            }
            laneEndTimes[laneIndex] = tx.timestampMs + tx.durationMs

            TimelineEntry(
                transaction = tx,
                startOffsetMs = startOffset,
                durationMs = tx.durationMs.coerceAtLeast(1L),
                laneIndex = laneIndex,
            )
        }

        return TimelineUiState(
            entries = entries,
            totalDurationMs = totalDuration,
            earliestMs = earliest,
            latestMs = latest,
            isLoading = false,
        )
    }
}