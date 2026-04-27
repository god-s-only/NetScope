package com.netscope.app.domain.model

data class PacketInfo(
    val id: String,
    val timestampMs: Long,
    val protocol: Protocol,
    val sourceIp: String,
    val destinationIp: String,
    val sourcePort: Int,
    val destinationPort: Int,
    val sizeBytes: Int,
    val direction: Direction,
    val uid: Int,
    val appInfo: AppInfo? = null,
)

enum class Protocol { TCP, UDP, ICMP, UNKNOWN }

enum class Direction { OUTBOUND, INBOUND }