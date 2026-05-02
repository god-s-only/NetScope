package com.netscope.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.netscope.app.presentation.screens.connections.ConnectionsScreen
import com.netscope.app.presentation.screens.dashboard.DashboardScreen
import com.netscope.app.presentation.screens.detail.TrafficDetailScreen
import com.netscope.app.presentation.screens.replay.ReplayScreen
import com.netscope.app.presentation.screens.timeline.TimelineScreen
import com.netscope.app.presentation.screens.traffic.TrafficListScreen
import com.netscope.presentation.screens.dns.DnsScreen

@Composable
fun NetScopeNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.DASHBOARD,
    ) {
        composable(NavRoutes.DASHBOARD) {
            DashboardScreen(
                onNavigateToTraffic  = { navController.navigate(NavRoutes.TRAFFIC_LIST) },
                onNavigateToDns = { navController.navigate(NavRoutes.DNS) },
                onNavigateToConnections = { navController.navigate(NavRoutes.CONNECTIONS) },
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
                onNavigateBack  = { navController.popBackStack() },
                onNavigateToReplay = { id ->
                    navController.navigate(NavRoutes.replay(id))
                },
            )
        }

        composable(NavRoutes.DNS) {
            DnsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(NavRoutes.CONNECTIONS) {
            ConnectionsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(NavRoutes.TIMELINE) {
            TimelineScreen(
                onNavigateBack     = { navController.popBackStack() },
                onTransactionClick = { id ->
                    navController.navigate(NavRoutes.trafficDetail(id))
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