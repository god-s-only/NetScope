package com.netscope.app.presentation

import android.os.Bundle
import android.security.KeyChain
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.compose.rememberNavController
import com.netscope.app.presentation.navigation.NetScopeNavGraph
import com.netscope.app.presentation.screens.dashboard.DashboardViewModel
import com.netscope.app.presentation.theme.NetScopeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var dashboardViewModel: DashboardViewModel? = null

    private val certInstallLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // user returned from cert install screen
        dashboardViewModel?.markCertificateInstalled()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NetScopeTheme {
                val navController = rememberNavController()
                NetScopeNavGraph(
                    navController = navController,
                    onInstallCertificate = ::installCert,
                    onDashboardReady = { vm -> dashboardViewModel = vm },
                )
            }
        }
    }

    private fun installCert(certBytes: ByteArray) {
        val intent = KeyChain.createInstallIntent()
        intent.putExtra(KeyChain.EXTRA_CERTIFICATE, certBytes)
        intent.putExtra(KeyChain.EXTRA_NAME, "NetScope CA")
        certInstallLauncher.launch(intent)
    }
}