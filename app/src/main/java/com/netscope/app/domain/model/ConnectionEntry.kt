package com.netscope.app.domain.model

data class ConnectionEntry(
    val id: String,
    val timestampMs: Long,
    val appInfo: AppInfo?,
    val destinationIp: String,
    val destinationHost: String?,
    val destinationPort: Int,
    val protocol: Protocol,
    val totalBytesSent: Long,
    val totalBytesReceived: Long,
    val durationMs: Long,
    val isActive: Boolean,
    val isFlagged: Boolean = false,
    val flagReason: String? = null,
) {
    val displayHost: String get() = destinationHost ?: destinationIp
    val displayPort: String get() = when (destinationPort) {
        80 -> "HTTP"
        443 -> "HTTPS"
        53 -> "DNS"
        22 -> "SSH"
        21 -> "FTP"
        else -> ":$destinationPort"
    }
}