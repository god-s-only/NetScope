package com.netscope.app.data.vpn

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpnController @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun startCapture() {
        Timber.d("VpnController: starting capture")
        val intent = Intent(context, NetScopeVpnService::class.java).apply {
            action = NetScopeVpnService.ACTION_START
        }
        context.startForegroundService(intent)
    }

    fun stopCapture() {
        Timber.d("VpnController: stopping capture")
        val intent = Intent(context, NetScopeVpnService::class.java).apply {
            action = NetScopeVpnService.ACTION_STOP
        }
        context.startService(intent)
    }
}