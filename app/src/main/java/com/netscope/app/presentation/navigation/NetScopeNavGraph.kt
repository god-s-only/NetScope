package com.netscope.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.netscope.app.domain.repository.SettingsRepository
import com.netscope.app.presentation.screens.connections.ConnectionsScreen
import com.netscope.app.presentation.screens.dashboard.DashboardScreen
import com.netscope.app.presentation.screens.dashboard.DashboardViewModel
import com.netscope.app.presentation.screens.detail.TrafficDetailScreen
import com.netscope.app.presentation.screens.dns.DnsScreen
import com.netscope.app.presentation.screens.integration.IntegrationScreen
import com.netscope.app.presentation.screens.onboarding.OnboardingScreen
import com.netscope.app.presentation.screens.replay.ReplayScreen
import com.netscope.app.presentation.screens.settings.SettingsScreen
import com.netscope.app.presentation.screens.stats.StatsScreen
import com.netscope.app.presentation.screens.timeline.TimelineScreen
import com.netscope.app.presentation.screens.traffic.TrafficListScreen

@Composable
fun NetScopeNavGraph(
    navController: NavHostController,
    onInstallCertificate: (ByteArray) -> Unit,
    onDashboardReady: (DashboardViewModel) -> Unit,
    settingsRepository: SettingsRepository,
) {
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        startDestination = if (settingsRepository.isOnboardingCompleted()) {
            NavRoutes.DASHBOARD
        } else {
            NavRoutes.ONBOARDING
        }
    }

    val destination = startDestination ?: return

    NavHost(
        navController = navController,
        startDestination = destination,
    ) {

        composable(NavRoutes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(NavRoutes.DASHBOARD) {
                        popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        composable(NavRoutes.DASHBOARD) {
            val viewModel = hiltViewModel<DashboardViewModel>()
            LaunchedEffect(viewModel) { onDashboardReady(viewModel) }
            DashboardScreen(
                onInstallCertificate = onInstallCertificate,
                onNavigateToIntegration = {
                    navController.navigate(NavRoutes.INTEGRATION)
                },
                onNavigateToTraffic = {
                    navController.navigate(NavRoutes.TRAFFIC_LIST)
                },
                onNavigateToDns = {
                    navController.navigate(NavRoutes.DNS)
                },
                onNavigateToTimeline = {
                    navController.navigate(NavRoutes.TIMELINE)
                },
                onNavigateToConnections = {
                    navController.navigate(NavRoutes.CONNECTIONS)
                },
                onNavigateToStats = {
                    navController.navigate(NavRoutes.STATS)
                },
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.SETTINGS)
                },
                viewModel = viewModel,
            )
        }

        composable(NavRoutes.INTEGRATION) {
            IntegrationScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(NavRoutes.TRAFFIC_LIST) {
            TrafficListScreen(
                onTransactionClick = { id ->
                    navController.navigate(NavRoutes.trafficDetail(id))
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route = NavRoutes.TRAFFIC_DETAIL,
            arguments = listOf(
                navArgument(NavArgs.TRANSACTION_ID) { type = NavType.StringType }
            ),
        ) {
            TrafficDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToReplay = { id ->
                    navController.navigate(NavRoutes.replay(id))
                },
            )
        }

        composable(NavRoutes.DNS) {
            DnsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(NavRoutes.TIMELINE) {
            TimelineScreen(
                onNavigateBack = { navController.popBackStack() },
                onTransactionClick = { id ->
                    navController.navigate(NavRoutes.trafficDetail(id))
                },
            )
        }

        composable(NavRoutes.CONNECTIONS) {
            ConnectionsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(NavRoutes.STATS) {
            StatsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToIntegration = {
                    navController.navigate(NavRoutes.INTEGRATION)
                },
            )
        }

        composable(
            route = NavRoutes.REPLAY,
            arguments = listOf(
                navArgument(NavArgs.TRANSACTION_ID) { type = NavType.StringType }
            ),
        ) {
            ReplayScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}