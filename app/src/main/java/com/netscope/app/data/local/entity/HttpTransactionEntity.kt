package com.netscope.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.netscope.app.domain.model.HttpMethod

@Entity(
    tableName = "http_transactions",
    indices = [
        Index(value = ["timestampMs"]),
        Index(value = ["host"]),
        Index(value = ["responseCode"]),
        Index(value = ["uid"]),
    ]
)
data class HttpTransactionEntity(
    @PrimaryKey val id: String,
    val timestampMs: Long,
    val url: String,
    val host: String,
    val path: String,
    val method: HttpMethod,
    val requestHeaders: Map<String, String>,
    val requestBody: String?,
    val requestSizeBytes: Long,
    val responseCode: Int?,
    val responseMessage: String?,
    val responseHeaders: Map<String, String>,
    val responseBody: String?,
    val responseSizeBytes: Long,
    val durationMs: Long,
    val protocol: String?,
    val uid: Int?,
    val packageName: String?,
    val appName: String?,
    val isReplay: Boolean,
    val error: String?,
)
