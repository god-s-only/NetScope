package com.netscope.app.data.repostiory

import com.netscope.app.data.local.dao.ConnectionEntryDao
import com.netscope.app.data.mappers.toDomain
import com.netscope.app.data.mappers.toEntity
import com.netscope.app.data.vpn.PacketEventBus
import com.netscope.app.domain.model.ConnectionEntry
import com.netscope.app.domain.model.Direction
import com.netscope.app.domain.model.Protocol
import com.netscope.app.domain.repository.ConnectionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionRepositoryImpl @Inject constructor(
    private val connectionEntryDao: ConnectionEntryDao,
    private val packetEventBus: PacketEventBus,
) : ConnectionRepository {

    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // key: "destinationIp:destinationPort:protocol"
    // note: we key by DESTINATION not source port
    // source port changes every connection but destination is stable
    private val activeConnections = ConcurrentHashMap<String, ConnectionEntry>()

    // track recently seen keys to deduplicate burst packets
    private val recentKeys = mutableSetOf<String>()

    companion object {
        // ports to ignore entirely — system noise
        private val IGNORED_DESTINATION_PORTS = setOf(
            123,  // NTP
            5353, // mDNS
            1900, // SSDP
        )
        // only track meaningful protocols
        private val TRACKED_PROTOCOLS = setOf(Protocol.TCP, Protocol.UDP)
    }

    init {
        Timber.d("ConnectionRepositoryImpl: starting collector")
        repoScope.launch {
            packetEventBus.packets.collect { packet ->

                // skip noise
                if (packet.destinationPort in IGNORED_DESTINATION_PORTS) return@collect
                if (packet.protocol !in TRACKED_PROTOCOLS) return@collect

                // key by destination — this groups all packets to the
                // same server as one connection regardless of source port
                val key = "${packet.destinationIp}:${packet.destinationPort}" +
                        ":${packet.protocol.name}"

                val existing = activeConnections[key]

                if (existing != null) {
                    // update byte counts on existing connection
                    val updated = existing.copy(
                        totalBytesSent = if (packet.direction == Direction.OUTBOUND)
                            existing.totalBytesSent + packet.sizeBytes
                        else existing.totalBytesSent,
                        totalBytesReceived = if (packet.direction == Direction.INBOUND)
                            existing.totalBytesReceived + packet.sizeBytes
                        else existing.totalBytesReceived,
                        durationMs = System.currentTimeMillis() - existing.timestampMs,
                        isActive   = true,
                    )
                    activeConnections[key] = updated
                    connectionEntryDao.update(updated.toEntity())
                } else {
                    // new unique destination — create entry
                    val entry = ConnectionEntry(
                        id                 = UUID.randomUUID().toString(),
                        timestampMs        = packet.timestampMs,
                        appInfo            = packet.appInfo,
                        destinationIp      = packet.destinationIp,
                        destinationHost    = null,
                        destinationPort    = packet.destinationPort,
                        protocol           = packet.protocol,
                        totalBytesSent     = if (packet.direction == Direction.OUTBOUND)
                            packet.sizeBytes.toLong() else 0L,
                        totalBytesReceived = if (packet.direction == Direction.INBOUND)
                            packet.sizeBytes.toLong() else 0L,
                        durationMs         = 0L,
                        isActive           = true,
                        isFlagged          = false,
                    )
                    activeConnections[key] = entry
                    connectionEntryDao.insert(entry.toEntity())
                    Timber.d(
                        "New connection: ${packet.protocol} → " +
                                "${packet.destinationIp}:${packet.destinationPort}"
                    )
                }
            }
        }
    }

    override fun observeConnections(): Flow<List<ConnectionEntry>> =
        connectionEntryDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }

    override fun observeActiveConnections(): Flow<List<ConnectionEntry>> =
        connectionEntryDao.observeActive()
            .map { entities -> entities.map { it.toDomain() } }

    override fun observeFlaggedConnections(): Flow<List<ConnectionEntry>> =
        connectionEntryDao.observeFlagged()
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun clearAll() {
        activeConnections.clear()
        connectionEntryDao.clearAll()
    }
}