package com.netscope.app.data.proxy

import android.net.VpnService
import timber.log.Timber
import java.net.Socket

object ProtectedSocketHolder {

    @Volatile
    private var vpnService: VpnService? = null

    fun register(service: VpnService) {
        vpnService = service
        Timber.d("ProtectedSocketHolder: VpnService registered")
    }

    fun unregister() {
        vpnService = null
        Timber.d("ProtectedSocketHolder: VpnService unregistered")
    }

    fun createProtectedSocket(): Socket? {
        val service = vpnService ?: run {
            Timber.w("ProtectedSocketHolder: no VpnService registered")
            return null
        }
        val socket = Socket()
        val protected = service.protect(socket)
        if (!protected) {
            Timber.w("ProtectedSocketHolder: protect() returned false")
        }
        return socket
    }
}