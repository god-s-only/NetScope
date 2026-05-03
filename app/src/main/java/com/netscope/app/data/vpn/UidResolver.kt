package com.netscope.app.data.vpn

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
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
    // cache UID → AppInfo to avoid repeated PackageManager lookups
    private val appInfoCache = mutableMapOf<Int, AppInfo?>()

    /**
     * On API 29+, /proc/net/tcp is restricted to an app's own sockets.
     * This will return null for all other apps' traffic.
     * We keep this as best-effort — it works for our own app's traffic.
     */
    suspend fun resolveUid(
        sourcePort: Int,
        protocol: Protocol,
    ): Int? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= 29) {
            // /proc/net restricted on API 29+ — skip the attempt
            return@withContext null
        }
        try {
            val procFile = when (protocol) {
                Protocol.TCP -> "/proc/net/tcp"
                Protocol.UDP -> "/proc/net/udp"
                else         -> return@withContext null
            }
            parseProcNet(procFile, sourcePort)
        } catch (e: Exception) {
            Timber.w(e, "Failed to resolve UID for port $sourcePort")
            null
        }
    }

    suspend fun resolveAppInfo(uid: Int): AppInfo? = withContext(Dispatchers.IO) {
        if (uid <= 0) return@withContext null
        appInfoCache[uid]?.let { return@withContext it }
        try {
            val pm       = context.packageManager
            val packages = pm.getPackagesForUid(uid)
            val appInfo  = packages?.firstOrNull()?.let { packageName ->
                val info    = pm.getApplicationInfo(packageName, 0)
                val name    = pm.getApplicationLabel(info).toString()
                val isSystem = info.flags and ApplicationInfo.FLAG_SYSTEM != 0
                AppInfo(
                    uid         = uid,
                    packageName = packageName,
                    appName     = name,
                    isSystemApp = isSystem,
                )
            }
            appInfoCache[uid] = appInfo
            appInfo
        } catch (e: PackageManager.NameNotFoundException) {
            appInfoCache[uid] = null
            null
        } catch (e: Exception) {
            Timber.w(e, "Failed to resolve AppInfo for UID $uid")
            null
        }
    }

    suspend fun resolve(sourcePort: Int, protocol: Protocol): AppInfo? {
        val uid = resolveUid(sourcePort, protocol) ?: return null
        return resolveAppInfo(uid)
    }

    private fun parseProcNet(path: String, targetPort: Int): Int? {
        val file = File(path)
        if (!file.exists() || !file.canRead()) return null
        val targetPortHex = targetPort.toString(16)
            .padStart(4, '0').uppercase()
        return file.bufferedReader().use { reader ->
            reader.readLine() // skip header
            reader.lineSequence().mapNotNull { line ->
                try {
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size < 8) return@mapNotNull null
                    val portHex = parts[1].substringAfter(":")
                    if (portHex.uppercase() != targetPortHex) return@mapNotNull null
                    parts[7].toIntOrNull()
                } catch (e: Exception) { null }
            }.firstOrNull()
        }
    }

    fun clearCache() {
        appInfoCache.clear()
    }
}