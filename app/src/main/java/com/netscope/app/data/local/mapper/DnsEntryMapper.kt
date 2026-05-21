package com.netscope.app.data.local.mapper

import com.netscope.app.data.local.entity.DnsEntryEntity
import com.netscope.app.domain.model.AppInfo
import com.netscope.app.domain.model.DnsEntry

fun DnsEntryEntity.toDomain(): DnsEntry =
    DnsEntry(
        id = id,
        timestampMs = timestampMs,
        domain = domain,
        resolvedIps = resolvedIps,
        queryType = queryType,
        uid = uid,
        appInfo = if (packageName != null && appName != null) {
            AppInfo(
                uid = uid,
                packageName = packageName,
                appName = appName,
                isSystemApp = false,
            )
        } else null,
        responseTimeMs = responseTimeMs,
    )

fun DnsEntry.toEntity(): DnsEntryEntity =
    DnsEntryEntity(
        id = id,
        timestampMs = timestampMs,
        domain = domain,
        resolvedIps = resolvedIps,
        queryType = queryType,
        uid = uid,
        packageName = appInfo?.packageName,
        appName = appInfo?.appName,
        responseTimeMs = responseTimeMs,
    )