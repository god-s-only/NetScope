package com.netscope.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.netscope.app.presentation.screens.dashboard.DashboardScreen
import com.netscope.app.presentation.screens.dashboard.DashboardViewModel
import com.netscope.app.presentation.screens.detail.TrafficDetailScreen
import com.netscope.app.presentation.screens.setup.ProxySetupScreen

import com.netscope.app.presentation.screens.traffic.TrafficListScreen

@Composable
fun NetScopeNavGraph(
    navController: NavHostController,
    onInstallCertificate: (ByteArray) -> Unit,
    onDashboardReady: (DashboardViewModel) -> Unit,
) {
    NavHost(
        navController    = navController,
        startDestination = NavRoutes.DASHBOARD,
    ) {

        composable(NavRoutes.DASHBOARD) {
            val viewModel = hiltViewModel<DashboardViewModel>()

            // pass ViewModel reference back to MainActivity
            // so it can call onVpnStarted() after permission result
            LaunchedEffect(viewModel) {
                onDashboardReady(viewModel)
            }

            DashboardScreen(
                onRequestVpnPermission  = {},
                onStopVpn = {},
                onInstallCertificate = onInstallCertificate,
                onNavigateToTraffic = { navController.navigate(NavRoutes.TRAFFIC_LIST) },
                onNavigateToDns = {  },
                onNavigateToConnections = {  },
                onNavigateToTimeline = {  },
                viewModel = viewModel,
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
                onNavigateBack     = { navController.popBackStack() },
                onNavigateToReplay = { id ->
                    navController.navigate(NavRoutes.replay(id))
                },
            )
        }
        composable(NavRoutes.SETUP) {
            ProxySetupScreen(
                onInstallCertificate = onInstallCertificate,
                onNavigateToTraffic  = {
                    navController.navigate(NavRoutes.TRAFFIC_LIST)
                },
            )
        }
    }
}