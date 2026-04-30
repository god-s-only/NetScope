package com.netscope.app.data.vpn

import android.content.Context
import android.content.pm.PackageManager
import com.netscope.app.domain.model.AppInfo
import com.netscope.app.domain.model.Protocol
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UidResolver @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val appInfoCache = mutableMapOf<Int, AppInfo?>()

    private val portUidCache = mutableMapOf<Int, Int>()

    suspend fun resolveUid(
        sourcePort: Int,
        protocol: Protocol,
    ): Int? = withContext(Dispatchers.IO) {
        try {
            portUidCache[sourcePort]?.let { return@withContext it }

            val procFile = when (protocol) {
                Protocol.TCP -> "/proc/net/tcp"
                Protocol.UDP -> "/proc/net/udp"
                else -> return@withContext null
            }

            val uid = parseProcNet(procFile, sourcePort)
            if (uid != null) portUidCache[sourcePort] = uid
            uid
        } catch (e: Exception) {
            Timber.w(e, "Failed to resolve UID for port $sourcePort")
            null
        }
    }

    suspend fun resolveAppInfo(uid: Int): AppInfo? = withContext(Dispatchers.IO) {
        appInfoCache[uid]?.let { return@withContext it }

        try {
            val pm = context.packageManager
            val packages = pm.getPackagesForUid(uid)

            val appInfo = packages?.firstOrNull()?.let { packageName ->
                val applicationInfo = pm.getApplicationInfo(packageName, 0)
                val appName = pm.getApplicationLabel(applicationInfo).toString()
                val isSystem = applicationInfo.flags and
                        android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0

                AppInfo(
                    uid = uid,
                    packageName = packageName,
                    appName = appName,
                    isSystemApp = isSystem,
                )
            }

            appInfoCache[uid] = appInfo
            appInfo
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.w("No app found for UID $uid")
            appInfoCache[uid] = null
            null
        } catch (e: Exception) {
            Timber.w(e, "Failed to resolve AppInfo for UID $uid")
            null
        }
    }

    suspend fun resolve(
        sourcePort: Int,
        protocol: Protocol,
    ): AppInfo? {
        val uid = resolveUid(sourcePort, protocol) ?: return null
        return resolveAppInfo(uid)
    }

    private fun parseProcNet(path: String, targetPort: Int): Int? {
        val file = File(path)
        if (!file.exists() || !file.canRead()) return null

        val targetPortHex = targetPort.toString(16).padStart(4, '0').uppercase()

        return file.bufferedReader().use { reader ->
            reader.readLine()

            reader.lineSequence()
                .mapNotNull { line ->
                    try {
                        val parts = line.trim().split("\\s+".toRegex())
                        if (parts.size < 8) return@mapNotNull null

                        val localAddress = parts[1]
                        val portHex = localAddress.substringAfter(":")

                        if (portHex.uppercase() != targetPortHex) return@mapNotNull null

                        parts[7].toIntOrNull()
                    } catch (e: Exception) {
                        null
                    }
                }
                .firstOrNull()
        }
    }

    fun clearCache() {
        portUidCache.clear()
    }
}