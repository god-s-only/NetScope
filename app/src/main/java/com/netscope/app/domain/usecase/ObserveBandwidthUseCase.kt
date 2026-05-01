package com.netscope.app.domain.usecase

import com.netscope.app.domain.model.BandwidthSnapshot
import com.netscope.app.domain.repository.BandwidthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveBandwidthUseCase @Inject constructor(
    private val bandwidthRepository: BandwidthRepository,
) {
    data class BandwidthState(
        val perApp: List<BandwidthSnapshot>,
        val totalUploadBytesPerSec: Long,
        val totalDownloadBytesPerSec: Long,
        val topConsumer: BandwidthSnapshot?,
    )

    operator fun invoke(): Flow<BandwidthState> =
        bandwidthRepository.observeBandwidthSnapshots()
            .map { snapshots ->
                val sorted = snapshots.sortedByDescending { it.totalBytesPerSec }
                BandwidthState(
                    perApp = sorted,
                    totalUploadBytesPerSec = snapshots.sumOf { it.uploadBytesPerSec },
                    totalDownloadBytesPerSec = snapshots.sumOf { it.downloadBytesPerSec },
                    topConsumer = sorted.firstOrNull(),
                )
            }
}