package com.netscope.app.domain.model

data class BandwidthSnapshot(
    val timestampMs: Long,
    val appInfo: AppInfo?,
    val uploadBytesPerSec: Long,
    val downloadBytesPerSec: Long,
    val totalUploadBytes: Long,
    val totalDownloadBytes: Long,
) {
    val totalBytesPerSec: Long get() = uploadBytesPerSec + downloadBytesPerSec
}