package com.netscope.app

import android.app.Application
import timber.log.Timber

class NetScopeApp: Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}