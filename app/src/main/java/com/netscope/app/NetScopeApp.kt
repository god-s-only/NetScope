package com.netscope.app

import android.app.Application
import android.util.Log
import com.netscope.app.data.proxy.cert.CertificateManager
import com.netscope.app.data.repository.DnsRepositoryImpl
import com.netscope.app.data.repository.TrafficRepositoryImpl
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NetScopeApp : Application() {

    @Inject lateinit var certificateManager: CertificateManager
    @Inject lateinit var trafficRepository: TrafficRepositoryImpl
    @Inject lateinit var dnsRepository: DnsRepositoryImpl

    override fun onCreate() {
        super.onCreate()
        certificateManager.initialize()
        Log.d("NetScopeApp", "App started — repositories initialized")
    }
}