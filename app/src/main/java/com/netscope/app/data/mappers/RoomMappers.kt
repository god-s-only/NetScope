package com.netscope.app.data.mappers

import com.netscope.app.data.local.entity.ConnectionEntryEntity
import com.netscope.app.data.local.entity.DnsEntryEntity
import com.netscope.app.data.local.entity.HttpTransactionEntity
import com.netscope.app.domain.model.AppInfo
import com.netscope.app.domain.model.ConnectionEntry
import com.netscope.app.domain.model.DnsEntry
import com.netscope.app.domain.model.HttpTransaction

fun HttpTransactionEntity.toDomain(): HttpTransaction =
    HttpTransaction(
        id = id,
        timestampMs = timestampMs,
        url = url,
        host = host,
        path = path,
        method = method,
        requestHeaders = requestHeaders,
        requestBody = requestBody,
        requestSizeBytes = requestSizeBytes,
        responseCode = responseCode,
        responseMessage = responseMessage,
        responseHeaders = responseHeaders,
        responseBody = responseBody,
        responseSizeBytes = responseSizeBytes,
        durationMs = durationMs,
        protocol = protocol,
        uid = uid,
        appInfo = if (packageName != null && appName != null) {
            AppInfo(
                uid = uid ?: -1,
                packageName = packageName,
                appName = appName,
                isSystemApp = false,
            )
        } else null,
        isReplay = isReplay,
        error = error,
    )

fun HttpTransaction.toEntity(): HttpTransactionEntity =
    HttpTransactionEntity(
        id = id,
        timestampMs = timestampMs,
        url = url,
        host = host,
        path = path,
        method = method,
        requestHeaders = requestHeaders,
        requestBody = requestBody,
        requestSizeBytes = requestSizeBytes,
        responseCode = responseCode,
        responseMessage = responseMessage,
        responseHeaders = responseHeaders,
        responseBody = responseBody,
        responseSizeBytes = responseSizeBytes,
        durationMs = durationMs,
        protocol = protocol,
        uid = uid,
        packageName = appInfo?.packageName,
        appName = appInfo?.appName,
        isReplay = isReplay,
        error = error,
    )


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


fun ConnectionEntryEntity.toDomain(): ConnectionEntry =
    ConnectionEntry(
        id = id,
        timestampMs = timestampMs,
        appInfo = if (packageName != null && appName != null) {
            AppInfo(
                uid = uid ?: -1,
                packageName = packageName,
                appName = appName,
                isSystemApp = false,
            )
        } else null,
        destinationIp = destinationIp,
        destinationHost = destinationHost,
        destinationPort = destinationPort,
        protocol = protocol,
        totalBytesSent = totalBytesSent,
        totalBytesReceived = totalBytesReceived,
        durationMs = durationMs,
        isActive = isActive,
        isFlagged = isFlagged,
        flagReason = flagReason,
    )

fun ConnectionEntry.toEntity(): ConnectionEntryEntity =
    ConnectionEntryEntity(
        id = id,
        timestampMs = timestampMs,
        uid = appInfo?.uid,
        packageName = appInfo?.packageName,
        appName = appInfo?.appName,
        destinationIp = destinationIp,
        destinationHost = destinationHost,
        destinationPort = destinationPort,
        protocol = protocol,
        totalBytesSent = totalBytesSent,
        totalBytesReceived = totalBytesReceived,
        durationMs = durationMs,
        isActive = isActive,
        isFlagged = isFlagged,
        flagReason = flagReason,
    )