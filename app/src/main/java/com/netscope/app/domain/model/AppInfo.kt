package com.netscope.app.domain.model

data class AppInfo(
    val uid: Int,
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
)