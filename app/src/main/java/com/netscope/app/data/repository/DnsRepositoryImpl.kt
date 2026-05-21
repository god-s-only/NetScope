package com.netscope.app.data.repository

import android.util.Log
import com.netscope.app.data.local.dao.DnsEntryDao
import com.netscope.app.data.local.mapper.toDomain
import com.netscope.app.data.local.mapper.toEntity
import com.netscope.app.data.proxy.HttpTransactionEmitter
import com.netscope.app.domain.model.DnsEntry
import com.netscope.app.domain.model.DnsQueryType
import com.netscope.app.domain.repository.DnsRepository
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

private const val TAG = "DnsRepositoryImpl"

@Singleton
class DnsRepositoryImpl @Inject constructor(
    private val dnsEntryDao: DnsEntryDao,
    private val httpTransactionEmitter: HttpTransactionEmitter,
) : DnsRepository {

    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val recentHosts = ConcurrentHashMap<String, Long>()
    private val DEDUP_WINDOW_MS = 60_000L

    init {
        Log.d(TAG, "init — starting collector")
        repoScope.launch {
            httpTransactionEmitter.transactions.collect { transaction ->
                val host = transaction.host
                    .trim()
                    .lowercase()

                if (host.isBlank()) return@collect

                val now = System.currentTimeMillis()
                val lastSeen = recentHosts[host]
                if (lastSeen != null && now - lastSeen < DEDUP_WINDOW_MS) {
                    return@collect
                }
                recentHosts[host] = now

                val entry = DnsEntry(
                    id = UUID.randomUUID().toString(),
                    timestampMs  = transaction.timestampMs,
                    domain = host,
                    resolvedIps = emptyList(),
                    queryType = DnsQueryType.A,
                    uid = -1,
                    appInfo = null,
                    responseTimeMs = transaction.durationMs,
                )

                Log.d(TAG, "DNS entry: $host")
                dnsEntryDao.insert(entry.toEntity())
            }
        }
    }

    override fun observeDnsEntries(): Flow<List<DnsEntry>> =
        dnsEntryDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }

    override fun observeByDomain(query: String): Flow<List<DnsEntry>> =
        dnsEntryDao.observeByDomain(query)
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun getDnsEntry(id: String): DnsEntry? =
        dnsEntryDao.getById(id)?.toDomain()

    override suspend fun getDomainsForApp(uid: Int): List<String> =
        dnsEntryDao.getDomainsForUid(uid)

    override suspend fun clearAll() {
        recentHosts.clear()
        dnsEntryDao.clearAll()
    }
}