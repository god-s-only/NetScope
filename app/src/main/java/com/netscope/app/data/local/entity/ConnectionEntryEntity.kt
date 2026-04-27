package com.netscope.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.netscope.app.domain.model.Protocol

@Entity(
    tableName = "connections",
    indices = [
        Index(value = ["timestampMs"]),
        Index(value = ["destinationIp"]),
        Index(value = ["uid"]),
        Index(value = ["isActive"]),
    ]
)
data class ConnectionEntryEntity(
    @PrimaryKey val id: String,
    val timestampMs: Long,
    val uid: Int?,
    val packageName: String?,
    val appName: String?,
    val destinationIp: String,
    val destinationHost: String?,
    val destinationPort: Int,
    val protocol: Protocol,
    val totalBytesSent: Long,
    val totalBytesReceived: Long,
    val durationMs: Long,
    val isActive: Boolean,
    val isFlagged: Boolean,
    val flagReason: String?,
)