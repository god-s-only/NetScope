package com.netscope.app.data.repostiory

import com.netscope.app.data.local.dao.DnsEntryDao
import com.netscope.app.data.mappers.toDomain
import com.netscope.app.data.mappers.toEntity
import com.netscope.app.data.vpn.PacketEventBus
import com.netscope.app.domain.model.DnsEntry
import com.netscope.app.domain.model.DnsQueryType
import com.netscope.app.domain.repository.DnsRepository
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
class DnsRepositoryImpl @Inject constructor(
    private val dnsEntryDao: DnsEntryDao,
    private val packetEventBus: PacketEventBus,
) : DnsRepository {

    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // deduplicate: track domain → last seen timestamp
    // only log the same domain again if > 30 seconds have passed
    private val recentDomains = ConcurrentHashMap<String, Long>()
    private val DEDUP_WINDOW_MS = 30_000L

    init {
        Timber.d("DnsRepositoryImpl: starting collector")
        repoScope.launch {
            packetEventBus.dnsEvents.collect { event ->
                val response = event.response
                val query    = event.query

                // prefer response (has resolved IPs), fall back to query
                val domain = response?.domain ?: query?.domain ?: return@collect

                // skip empty or obviously invalid domains
                if (domain.isBlank() || domain.length < 3) return@collect

                // deduplicate — skip if we logged this domain recently
                val now      = System.currentTimeMillis()
                val lastSeen = recentDomains[domain]
                if (lastSeen != null && now - lastSeen < DEDUP_WINDOW_MS) {
                    return@collect
                }
                recentDomains[domain] = now

                val entry = DnsEntry(
                    id           = UUID.randomUUID().toString(),
                    timestampMs  = event.timestampMs,
                    domain       = domain,
                    resolvedIps  = response?.resolvedIps ?: emptyList(),
                    queryType    = response?.queryType ?: query?.queryType
                    ?: DnsQueryType.UNKNOWN,
                    uid          = event.uid,
                    appInfo      = null, // UID resolution not reliable API 29+
                    responseTimeMs = null,
                )

                Timber.d("DNS entry: $domain → ${entry.resolvedIps}")
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
        recentDomains.clear()
        dnsEntryDao.clearAll()
    }
}