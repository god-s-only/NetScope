package com.netscope.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.netscope.app.presentation.screens.connections.ConnectionsScreen
import com.netscope.app.presentation.screens.dashboard.DashboardScreen
import com.netscope.app.presentation.screens.dashboard.DashboardViewModel
import com.netscope.app.presentation.screens.detail.TrafficDetailScreen
import com.netscope.app.presentation.screens.dns.DnsScreen
import com.netscope.app.presentation.screens.replay.ReplayScreen
import com.netscope.app.presentation.screens.settings.SettingsScreen
import com.netscope.app.presentation.screens.setup.ProxySetupScreen
import com.netscope.app.presentation.screens.stats.StatsScreen
import com.netscope.app.presentation.screens.timeline.TimelineScreen
import com.netscope.app.presentation.screens.traffic.TrafficListScreen

@Composable
fun NetScopeNavGraph(
    navController: NavHostController,
    onInstallCertificate: (ByteArray) -> Unit,
    onDashboardReady: (DashboardViewModel) -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.DASHBOARD,
    ) {

        composable(NavRoutes.DASHBOARD) {
            val viewModel = hiltViewModel<DashboardViewModel>()
            LaunchedEffect(viewModel) { onDashboardReady(viewModel) }
            DashboardScreen(
                onInstallCertificate = onInstallCertificate,
                onNavigateToSetup = { navController.navigate(NavRoutes.SETUP) },
                onNavigateToTraffic = { navController.navigate(NavRoutes.TRAFFIC_LIST) },
                onNavigateToDns = { navController.navigate(NavRoutes.DNS) },
                onNavigateToTimeline = { navController.navigate(NavRoutes.TIMELINE) },
                onNavigateToConnections = { navController.navigate(NavRoutes.CONNECTIONS) },
                onNavigateToStats = { navController.navigate(NavRoutes.STATS) },
                viewModel = viewModel,
            )
        }

        composable(NavRoutes.SETUP) {
            ProxySetupScreen(
                onInstallCertificate = onInstallCertificate,
                onNavigateToTraffic = {
                    navController.navigate(NavRoutes.TRAFFIC_LIST)
                },
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

        composable(
            route = NavRoutes.REPLAY,
            arguments = listOf(
                navArgument(NavArgs.TRANSACTION_ID) { type = NavType.StringType }
            ),
        ) {
            ReplayScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(NavRoutes.STATS) {
            StatsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSetup = { navController.navigate(NavRoutes.SETUP) },
            )
        }
    }
}