package com.netscope.app

import android.app.Application
import com.netscope.app.data.proxy.cert.CertificateManager
import dagger.hilt.android.HiltAndroidApp
import org.conscrypt.BuildConfig
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class NetScopeApp : Application() {

    @Inject
    lateinit var certificateManager: CertificateManager

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        certificateManager.initialize()
        Timber.d("NetScopeApp started")
    }
}