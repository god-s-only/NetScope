package com.netscope.app.presentation.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.netscope.app.presentation.theme.NetScopeBackground
import com.netscope.app.presentation.theme.NetScopePrimary
import com.netscope.app.presentation.theme.NetScopeSurface
import com.netscope.app.presentation.theme.NetScopeSurface2
import com.netscope.app.presentation.theme.TextPrimary
import com.netscope.app.presentation.theme.TextSecondary
import com.netscope.app.presentation.theme.TextTertiary

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pages = OnboardingPage.entries
    val currentPage = pages[state.currentPage]
    val isLastPage = state.currentPage == pages.size - 1

    LaunchedEffect(state.isCompleted) {
        if (state.isCompleted) onFinished()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NetScopeBackground)
            .padding(24.dp),
    ) {

        // ── Skip button ───────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            if (!isLastPage) {
                TextButton(onClick = viewModel::onGetStarted) {
                    Text("Skip", color = TextTertiary)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // ── Page icon ─────────────────────────────────────────
        AnimatedContent(
            targetState = state.currentPage,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it } + fadeOut())
            },
            label = "page_transition",
        ) { pageIndex ->
            val page = pages[pageIndex]
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(NetScopePrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = pageIcon(page),
                        contentDescription = null,
                        tint = NetScopePrimary,
                        modifier = Modifier.size(48.dp),
                    )
                }

                Spacer(Modifier.height(32.dp))

                Text(
                    text = page.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = page.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(32.dp))

                // bullet points
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NetScopeSurface)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    page.bulletPoints.forEach { point ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(NetScopePrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = NetScopePrimary,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                            Text(
                                text = point,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // ── Page indicators ───────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            pages.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(
                            width = if (index == state.currentPage) 24.dp else 8.dp,
                            height = 8.dp,
                        )
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (index == state.currentPage)
                                NetScopePrimary
                            else
                                NetScopeSurface2
                        ),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Navigation buttons ────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.currentPage > 0) {
                OutlinedButton(
                    onClick = viewModel::onPreviousPage,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Back", color = TextSecondary)
                }
            }

            Button(
                onClick = {
                    if (isLastPage) viewModel.onGetStarted()
                    else viewModel.onNextPage()
                },
                modifier = Modifier.weight(if (state.currentPage > 0) 1f else Float.MAX_VALUE),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NetScopePrimary,
                ),
            ) {
                Text(
                    text = if (isLastPage) "Get Started" else "Next",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }
        }
    }
}

private fun pageIcon(page: OnboardingPage): ImageVector = when (page) {
    OnboardingPage.WELCOME -> Icons.Default.NetworkCheck
    OnboardingPage.HOW_IT_WORKS -> Icons.Default.Wifi
    OnboardingPage.WHAT_YOU_GET -> Icons.Default.Search
}