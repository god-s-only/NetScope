package com.netscope.app.data.repostiory

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.netscope.app.data.vpn.UidResolver
import com.netscope.app.domain.model.AppInfo
import com.netscope.app.domain.repository.AppInfoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInfoRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val uidResolver: UidResolver,
) : AppInfoRepository {

    override suspend fun getAppInfo(uid: Int): AppInfo? =
        uidResolver.resolveAppInfo(uid)

    override suspend fun getAllApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .map { info ->
                AppInfo(
                    uid = info.uid,
                    packageName = info.packageName,
                    appName = pm.getApplicationLabel(info).toString(),
                    isSystemApp = info.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                )
            }
            .sortedBy { it.appName }
    }
}