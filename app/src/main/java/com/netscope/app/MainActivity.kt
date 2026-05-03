package com.netscope.app

import android.net.VpnService
import android.os.Bundle
import android.security.KeyChain
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.compose.rememberNavController
import com.netscope.app.data.vpn.VpnController
import com.netscope.app.presentation.navigation.NetScopeNavGraph
import com.netscope.app.presentation.screens.dashboard.DashboardViewModel
import com.netscope.app.presentation.theme.NetScopeTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var vpnController: VpnController
    private var dashboardViewModel: DashboardViewModel? = null

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Timber.d("VPN permission granted")
            vpnController.startCapture()
            dashboardViewModel?.onVpnStarted()
        } else {
            Timber.w("VPN permission denied")
        }
    }


    private val certInstallLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        Timber.d("Certificate install screen returned")
        dashboardViewModel?.markCertificateInstalled()
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NetScopeTheme {
                val navController = rememberNavController()
                NetScopeNavGraph(
                    navController = navController,
                    onRequestVpnPermission = ::requestVpnPermission,
                    onStopVpn = ::stopVpn,
                    onInstallCertificate = ::installCaCertificate,
                    onDashboardReady = { vm -> dashboardViewModel = vm },
                )
            }
        }
    }


    fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            Timber.d("Launching VPN permission dialog")
            vpnPermissionLauncher.launch(intent)
        } else {
            Timber.d("VPN permission already granted — starting capture")
            vpnController.startCapture()
            dashboardViewModel?.onVpnStarted()
        }
    }

    fun stopVpn() {
        vpnController.stopCapture()
        dashboardViewModel?.onVpnStopped()
    }

    fun installCaCertificate(certBytes: ByteArray) {
        Timber.d("Launching certificate install screen")
        val intent = KeyChain.createInstallIntent()
        intent.putExtra(KeyChain.EXTRA_CERTIFICATE, certBytes)
        intent.putExtra(KeyChain.EXTRA_NAME, "NetScope CA")
        certInstallLauncher.launch(intent)
    }
}