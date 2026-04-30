package com.netscope.app.data.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpnController @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun isVpnPermissionGranted(): Boolean =
        VpnService.prepare(context) == null

    fun startCapture() {
        val intent = Intent(context, NetScopeVpnService::class.java).apply {
            action = NetScopeVpnService.ACTION_START
        }
        context.startForegroundService(intent)
    }

    fun stopCapture() {
        val intent = Intent(context, NetScopeVpnService::class.java).apply {
            action = NetScopeVpnService.ACTION_STOP
        }
        context.startService(intent)
    }
}