package com.netscope.app.presentation.screens.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.netscope.app.domain.model.StatusCategory
import com.netscope.app.presentation.components.EmptyState
import com.netscope.app.presentation.components.NetScopeTopBar
import com.netscope.app.presentation.components.formatDuration
import com.netscope.app.presentation.theme.NetScopeBackground
import com.netscope.app.presentation.theme.NetScopeError
import com.netscope.app.presentation.theme.NetScopeInfo
import com.netscope.app.presentation.theme.NetScopeSurface
import com.netscope.app.presentation.theme.NetScopeSuccess
import com.netscope.app.presentation.theme.NetScopeWarning
import com.netscope.app.presentation.theme.TextPrimary
import com.netscope.app.presentation.theme.TextSecondary
import com.netscope.app.presentation.theme.TextTertiary

private const val LANE_HEIGHT_DP = 32
private const val LANE_GAP_DP = 6
private const val MIN_BAR_WIDTH_DP = 8f
private const val HORIZONTAL_PADDING_DP = 16f

private const val MAX_CANVAS_WIDTH_DP = 4000f

@Composable
fun TimelineScreen(
    onNavigateBack: () -> Unit,
    onTransactionClick: (String) -> Unit,
    viewModel: TimelineViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = NetScopeBackground,
        topBar = {
            NetScopeTopBar(
                title = "Timeline",
                onNavigateBack = onNavigateBack,
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Loading…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }

            state.entries.isEmpty() -> {
                EmptyState(
                    title = "No requests yet",
                    subtitle = "Capture some traffic to see the timeline",
                    modifier = Modifier.padding(padding),
                )
            }

            else -> {
                TimelineContent(
                    state = state,
                    onTransactionClick = onTransactionClick,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun TimelineContent(
    state: TimelineUiState,
    onTransactionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val laneCount = (state.entries.maxOfOrNull { it.laneIndex } ?: 0) + 1

    val msPerDp = if (state.totalDurationMs > 0) {
        (state.totalDurationMs / (MAX_CANVAS_WIDTH_DP - HORIZONTAL_PADDING_DP * 2))
            .coerceAtLeast(1f)
    } else {
        8f
    }

    val canvasWidthDp = ((state.totalDurationMs / msPerDp) + HORIZONTAL_PADDING_DP * 2)
        .coerceIn(300f, MAX_CANVAS_WIDTH_DP)

    val canvasHeightDp = (laneCount * (LANE_HEIGHT_DP + LANE_GAP_DP).toFloat() + 32f)
        .coerceAtLeast(100f)

    Column(modifier = modifier.fillMaxSize()) {

        // ── Summary bar ───────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NetScopeSurface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SummaryItem(
                label = "Requests",
                value = state.entries.size.toString(),
            )
            SummaryItem(
                label = "Total time",
                value = formatDuration(state.totalDurationMs),
            )
            SummaryItem(
                label = "Lanes",
                value = laneCount.toString(),
            )
        }

        // ── Legend ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LegendItem(color = NetScopeSuccess, label = "2xx")
            LegendItem(color = NetScopeWarning, label = "3xx")
            LegendItem(color = NetScopeError, label = "4xx/5xx")
            LegendItem(color = NetScopeInfo, label = "Pending")
            LegendItem(color = Color(0xFF484F58), label = "Unknown")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Box(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .width(canvasWidthDp.dp)
                    .height(canvasHeightDp.dp),
            ) {
                // draw bars
                Canvas(
                    modifier = Modifier
                        .width(canvasWidthDp.dp)
                        .height(canvasHeightDp.dp),
                ) {
                    state.entries.forEach { entry ->
                        val color = barColor(entry.transaction.statusCategory)
                        val x = (entry.startOffsetMs / msPerDp) + HORIZONTAL_PADDING_DP
                        val y = entry.laneIndex *
                                (LANE_HEIGHT_DP + LANE_GAP_DP).toFloat() + 16f
                        val w = (entry.durationMs / msPerDp)
                            .coerceAtLeast(MIN_BAR_WIDTH_DP)
                        val h = LANE_HEIGHT_DP.toFloat()

                        drawRoundRect(
                            color = color.copy(alpha = 0.85f),
                            topLeft = Offset(x, y),
                            size = Size(w, h),
                            cornerRadius = CornerRadius(4f, 4f),
                        )
                    }
                }

                // clickable overlays
                state.entries.forEach { entry ->
                    val x = (entry.startOffsetMs / msPerDp) + HORIZONTAL_PADDING_DP
                    val y = entry.laneIndex *
                            (LANE_HEIGHT_DP + LANE_GAP_DP).toFloat() + 16f
                    val w = (entry.durationMs / msPerDp)
                        .coerceAtLeast(MIN_BAR_WIDTH_DP)

                    Box(
                        modifier = Modifier
                            .offset(x = x.dp, y = y.dp)
                            .width(w.dp)
                            .height(LANE_HEIGHT_DP.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onTransactionClick(entry.transaction.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .width(12.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
    }
}

private fun barColor(category: StatusCategory): Color = when (category) {
    StatusCategory.SUCCESS -> NetScopeSuccess
    StatusCategory.REDIRECT -> NetScopeWarning
    StatusCategory.CLIENT_ERROR,
    StatusCategory.SERVER_ERROR,
    StatusCategory.ERROR -> NetScopeError
    StatusCategory.PENDING -> NetScopeInfo
    else -> Color(0xFF484F58)
}