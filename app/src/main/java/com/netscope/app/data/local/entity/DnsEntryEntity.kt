package com.netscope.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.netscope.app.domain.model.DnsQueryType

@Entity(
    tableName = "dns_entries",
    indices = [
        Index(value = ["timestampMs"]),
        Index(value = ["domain"]),
        Index(value = ["uid"]),
    ]
)
data class DnsEntryEntity(
    @PrimaryKey val id: String,
    val timestampMs: Long,
    val domain: String,
    val resolvedIps: List<String>,
    val queryType: DnsQueryType,
    val uid: Int,
    val packageName: String?,
    val appName: String?,
    val responseTimeMs: Long?,
)