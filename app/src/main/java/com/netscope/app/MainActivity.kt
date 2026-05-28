package com.netscope.app.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.security.KeyChain
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
        dashboardViewModel?.markCertificateInstalled()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            android.util.Log.d("MainActivity", "Notification permission granted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        r3t24NpUrJMNunMMASmhAM953bFGeLXzN7()
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

    private fun r3t24NpUrJMNunMMASmhAM953bFGeLXzN7() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
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