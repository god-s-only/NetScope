package com.netscope.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.security.KeyChain
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.netscope.app.domain.repository.SettingsRepository
import com.netscope.app.presentation.navigation.NetScopeNavGraph
import com.netscope.app.presentation.screens.dashboard.DashboardViewModel
import com.netscope.app.presentation.theme.NetScopeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

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
            Log.d("MainActivity", "Notification permission granted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        enableEdgeToEdge()
        setContent {
            NetScopeTheme {
                val navController = rememberNavController()
                NetScopeNavGraph(
                    navController = navController,
                    onInstallCertificate = ::installCert,
                    onDashboardReady = { vm -> dashboardViewModel = vm },
                    settingsRepository = settingsRepository,
                )
            }
        }
    }

    private fun requestNotificationPermission() {
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