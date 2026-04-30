package com.netscope.app.data.repostiory

import com.netscope.app.data.vpn.PacketEventBus
import com.netscope.app.domain.model.AppInfo
import com.netscope.app.domain.model.BandwidthSnapshot
import com.netscope.app.domain.model.Direction
import com.netscope.app.domain.repository.BandwidthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private data class ByteAccumulator(
    val appInfo: AppInfo?,
    var uploadBytes: Long = 0L,
    var downloadBytes: Long = 0L,
    var totalUpload: Long = 0L,
    var totalDownload: Long = 0L,
)

@Singleton
class BandwidthRepositoryImpl @Inject constructor(
    private val packetEventBus: PacketEventBus,
) : BandwidthRepository {

    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val accumulators = ConcurrentHashMap<String, ByteAccumulator>()

    private val _snapshots = MutableStateFlow<Map<String, BandwidthSnapshot>>(emptyMap())
    val snapshots: StateFlow<Map<String, BandwidthSnapshot>> = _snapshots.asStateFlow()

    init {
        repoScope.launch {
            packetEventBus.packets.collect { packet ->
                val key = packet.appInfo?.packageName ?: "unknown"
                val acc = accumulators.getOrPut(key) {
                    ByteAccumulator(appInfo = packet.appInfo)
                }
                if (packet.direction == Direction.OUTBOUND) {
                    acc.uploadBytes += packet.sizeBytes
                    acc.totalUpload += packet.sizeBytes
                } else {
                    acc.downloadBytes += packet.sizeBytes
                    acc.totalDownload += packet.sizeBytes
                }
            }
        }

        repoScope.launch {
            while (true) {
                delay(1_000L)
                val now = System.currentTimeMillis()
                val newSnapshots = accumulators.mapValues { (_, acc) ->
                    BandwidthSnapshot(
                        timestampMs = now,
                        appInfo = acc.appInfo,
                        uploadBytesPerSec = acc.uploadBytes,
                        downloadBytesPerSec = acc.downloadBytes,
                        totalUploadBytes = acc.totalUpload,
                        totalDownloadBytes = acc.totalDownload,
                    ).also {
                        acc.uploadBytes = 0L
                        acc.downloadBytes = 0L
                    }
                }
                _snapshots.value = newSnapshots
            }
        }
    }

    override fun observeBandwidthSnapshots(): Flow<List<BandwidthSnapshot>> =
        _snapshots.map { it.values.toList() }

    override fun observePerAppBandwidth(): Flow<Map<String, BandwidthSnapshot>> =
        _snapshots.asStateFlow()
}