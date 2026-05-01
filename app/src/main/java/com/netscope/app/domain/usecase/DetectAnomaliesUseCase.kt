package com.netscope.app.domain.usecase

import com.netscope.app.domain.repository.BandwidthRepository
import com.netscope.app.domain.repository.ConnectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class DetectAnomaliesUseCase @Inject constructor(
    private val bandwidthRepository: BandwidthRepository,
    private val connectionRepository: ConnectionRepository,
) {
    data class Anomaly(
        val type: AnomalyType,
        val description: String,
        val appName: String?,
        val severity: Severity,
        val timestampMs: Long,
    )

    enum class AnomalyType {
        BANDWIDTH_SPIKE,
        UNEXPECTED_PORT,
        UNKNOWN_ENDPOINT,
        EXCESSIVE_DNS,
        FLAGGED_CONNECTION,
    }

    enum class Severity { LOW, MEDIUM, HIGH }

    private companion object {
        const val SPIKE_THRESHOLD_BYTES = 5 * 1024 * 1024L
        val SUSPICIOUS_PORTS = setOf(4444, 6666, 6667, 31337, 1337, 9001)
        val COMMON_PORTS = setOf(80, 443, 53, 8080, 8443, 25, 587, 993, 995)
    }

    operator fun invoke(): Flow<List<Anomaly>> = combine(
        bandwidthRepository.observeBandwidthSnapshots(),
        connectionRepository.observeActiveConnections(),
    ) { snapshots, connections ->
        val anomalies = mutableListOf<Anomaly>()
        val now = System.currentTimeMillis()

        snapshots
            .filter { it.totalBytesPerSec > SPIKE_THRESHOLD_BYTES }
            .forEach { snap ->
                anomalies.add(
                    Anomaly(
                        type = AnomalyType.BANDWIDTH_SPIKE,
                        description = "Sending ${formatBytes(snap.uploadBytesPerSec)}/s " +
                                "upload, ${formatBytes(snap.downloadBytesPerSec)}/s download",
                        appName = snap.appInfo?.appName,
                        severity = if (snap.totalBytesPerSec > 20 * 1024 * 1024L)
                            Severity.HIGH else Severity.MEDIUM,
                        timestampMs = now,
                    )
                )
            }

        connections
            .filter { it.destinationPort in SUSPICIOUS_PORTS }
            .forEach { conn ->
                anomalies.add(
                    Anomaly(
                        type = AnomalyType.UNEXPECTED_PORT,
                        description = "Connection to suspicious port ${conn.destinationPort} " +
                                "on ${conn.displayHost}",
                        appName = conn.appInfo?.appName,
                        severity = Severity.HIGH,
                        timestampMs = conn.timestampMs,
                    )
                )
            }

        connections
            .filter { it.destinationHost == null && it.destinationPort !in COMMON_PORTS }
            .forEach { conn ->
                anomalies.add(
                    Anomaly(
                        type = AnomalyType.UNKNOWN_ENDPOINT,
                        description = "Connection to unknown IP ${conn.destinationIp}" +
                                ":${conn.destinationPort}",
                        appName = conn.appInfo?.appName,
                        severity = Severity.MEDIUM,
                        timestampMs = conn.timestampMs,
                    )
                )
            }

        connections
            .filter { it.isFlagged }
            .forEach { conn ->
                anomalies.add(
                    Anomaly(
                        type = AnomalyType.FLAGGED_CONNECTION,
                        description = conn.flagReason ?: "Connection flagged",
                        appName = conn.appInfo?.appName,
                        severity = Severity.HIGH,
                        timestampMs = conn.timestampMs,
                    )
                )
            }

        anomalies.sortedByDescending { it.severity.ordinal }
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1_048_576 -> "${"%.1f".format(bytes / 1_048_576.0)} MB"
        bytes >= 1_024 -> "${"%.1f".format(bytes / 1_024.0)} KB"
        else -> "$bytes B"
    }
}