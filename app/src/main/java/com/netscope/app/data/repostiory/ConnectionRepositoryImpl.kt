package com.netscope.app.data.repostiory

import com.netscope.app.data.local.dao.ConnectionEntryDao
import com.netscope.app.data.mappers.toDomain
import com.netscope.app.data.mappers.toEntity
import com.netscope.app.data.vpn.PacketEventBus
import com.netscope.app.domain.model.ConnectionEntry
import com.netscope.app.domain.model.Direction
import com.netscope.app.domain.repository.ConnectionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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

    private val activeConnections = ConcurrentHashMap<String, ConnectionEntry>()

    init {
        repoScope.launch {
            packetEventBus.packets.collect { packet ->
                val key = "${packet.sourcePort}:${packet.destinationIp}:${packet.destinationPort}"

                val existing = activeConnections[key]

                if (existing != null) {
                    val updated = existing.copy(
                        totalBytesSent = if (packet.direction == Direction.OUTBOUND)
                            existing.totalBytesSent + packet.sizeBytes
                        else existing.totalBytesSent,
                        totalBytesReceived = if (packet.direction == Direction.INBOUND)
                            existing.totalBytesReceived + packet.sizeBytes
                        else existing.totalBytesReceived,
                        durationMs = System.currentTimeMillis() - existing.timestampMs,
                    )
                    activeConnections[key] = updated
                    connectionEntryDao.update(updated.toEntity())
                } else {
                    val entry = ConnectionEntry(
                        id = UUID.randomUUID().toString(),
                        timestampMs = packet.timestampMs,
                        appInfo = packet.appInfo,
                        destinationIp = packet.destinationIp,
                        destinationHost = null,
                        destinationPort = packet.destinationPort,
                        protocol = packet.protocol,
                        totalBytesSent = if (packet.direction == Direction.OUTBOUND)
                            packet.sizeBytes.toLong() else 0L,
                        totalBytesReceived = if (packet.direction == Direction.INBOUND)
                            packet.sizeBytes.toLong() else 0L,
                        durationMs = 0L,
                        isActive = true,
                        isFlagged = false,
                    )
                    activeConnections[key] = entry
                    connectionEntryDao.insert(entry.toEntity())
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