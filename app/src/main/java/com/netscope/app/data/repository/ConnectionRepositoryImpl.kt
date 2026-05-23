package com.netscope.app.data.repository

import android.util.Log
import com.netscope.app.data.local.dao.ConnectionEntryDao
import com.netscope.app.data.mappers.toDomain
import com.netscope.app.data.mappers.toEntity
import com.netscope.app.data.proxy.HttpTransactionEmitter
import com.netscope.app.domain.model.ConnectionEntry
import com.netscope.app.domain.model.Protocol
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

private const val TAG = "ConnectionRepositoryImpl"

@Singleton
class ConnectionRepositoryImpl @Inject constructor(
    private val connectionEntryDao: ConnectionEntryDao,
    private val httpTransactionEmitter: HttpTransactionEmitter,
) : ConnectionRepository {

    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val activeConnections = ConcurrentHashMap<String, ConnectionEntry>()

    init {
        Log.d(TAG, "init — starting collector")
        repoScope.launch {
            httpTransactionEmitter.transactions.collect { transaction ->
                val host = transaction.host.trim().lowercase()
                if (host.isBlank()) return@collect

                val port = when {
                    transaction.protocol == "HTTPS" -> 443
                    transaction.protocol == "HTTP" -> 80
                    else -> 80
                }

                val key = "$host:$port"
                val now = System.currentTimeMillis()
                val existing = activeConnections[key]

                if (existing != null) {
                    val updated = existing.copy(
                        totalBytesSent = existing.totalBytesSent +
                                (transaction.requestSizeBytes),
                        totalBytesReceived = existing.totalBytesReceived +
                                (transaction.responseSizeBytes),
                        durationMs = now - existing.timestampMs,
                        isActive = true,
                    )
                    activeConnections[key] = updated
                    connectionEntryDao.update(updated.toEntity())
                } else {
                    // new connection
                    val entry = ConnectionEntry(
                        id = UUID.randomUUID().toString(),
                        timestampMs = now,
                        appInfo = null,
                        destinationIp = host,
                        destinationHost = host,
                        destinationPort = port,
                        protocol = if (transaction.protocol == "HTTPS")
                            Protocol.TCP else Protocol.TCP,
                        totalBytesSent = transaction.requestSizeBytes,
                        totalBytesReceived = transaction.responseSizeBytes,
                        durationMs = 0L,
                        isActive = true,
                        isFlagged = false,
                    )
                    activeConnections[key] = entry
                    connectionEntryDao.insert(entry.toEntity())
                    Log.d(TAG, "New connection: $host:$port")
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