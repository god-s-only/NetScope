package com.netscope.app.domain.repository

import com.netscope.app.domain.model.AppInfo

interface AppInfoRepository {
    suspend fun getAppInfo(uid: Int): AppInfo?
    suspend fun getAllApps(): List<AppInfo>
}