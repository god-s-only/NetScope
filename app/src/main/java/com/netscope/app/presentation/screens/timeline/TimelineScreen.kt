package com.netscope.app.presentation.screens.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.netscope.app.presentation.theme.*

private const val LANE_HEIGHT_DP = 28
private const val LANE_GAP_DP = 4
private const val MIN_BAR_WIDTH_DP = 4
private const val MS_PER_DP = 10f

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
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = NetScopePrimary) }
            }
            state.entries.isEmpty() -> {
                EmptyState(
                    title = "No requests",
                    subtitle = "Make some network requests to see the timeline",
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
    val laneCount    = (state.entries.maxOfOrNull { it.laneIndex } ?: 0) + 1
    val totalWidthDp = (state.totalDurationMs / MS_PER_DP).coerceAtLeast(300f)
    val totalHeightDp = laneCount * (LANE_HEIGHT_DP + LANE_GAP_DP)

    Column(modifier = modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NetScopeSurface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "${state.entries.size} requests",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Text(
                "Total: ${formatDuration(state.totalDurationMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
                .verticalScroll(rememberScrollState()),
        ) {
            Canvas(
                modifier = Modifier
                    .width(totalWidthDp.dp + 32.dp)
                    .height(totalHeightDp.dp + 32.dp),
            ) {
                state.entries.forEach { entry ->
                    val barColor = when (entry.transaction.statusCategory) {
                        StatusCategory.SUCCESS      -> NetScopeSuccess
                        StatusCategory.CLIENT_ERROR,
                        StatusCategory.SERVER_ERROR,
                        StatusCategory.ERROR        -> NetScopeError
                        StatusCategory.REDIRECT     -> NetScopeWarning
                        StatusCategory.PENDING      -> NetScopeInfo
                        else                        -> Color(0xFF484F58)
                    }

                    val x = (entry.startOffsetMs / MS_PER_DP) + 16f
                    val y = entry.laneIndex * (LANE_HEIGHT_DP + LANE_GAP_DP).toFloat() + 16f
                    val w = (entry.durationMs / MS_PER_DP)
                        .coerceAtLeast(MIN_BAR_WIDTH_DP.toFloat())
                    val h = LANE_HEIGHT_DP.toFloat()

                    drawRoundRect(
                        color = barColor.copy(alpha = 0.85f),
                        topLeft = Offset(x, y),
                        size = Size(w, h),
                        cornerRadius = CornerRadius(4f, 4f),
                    )
                }
            }

            state.entries.forEach { entry ->
                val x = (entry.startOffsetMs / MS_PER_DP) + 16f
                val y = entry.laneIndex * (LANE_HEIGHT_DP + LANE_GAP_DP).toFloat() + 16f
                val w = (entry.durationMs / MS_PER_DP)
                    .coerceAtLeast(MIN_BAR_WIDTH_DP.toFloat())

                Box(
                    modifier = Modifier
                        .offset(x = x.dp, y = y.dp)
                        .size(width = w.dp, height = LANE_HEIGHT_DP.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onTransactionClick(entry.transaction.id) },
                )
            }
        }
    }
}