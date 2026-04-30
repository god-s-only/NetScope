package com.netscope.app.data.repostiory

import com.netscope.app.data.local.dao.DnsEntryDao
import com.netscope.app.data.mappers.toDomain
import com.netscope.app.data.mappers.toEntity
import com.netscope.app.data.vpn.PacketEventBus
import com.netscope.app.domain.model.DnsEntry
import com.netscope.app.domain.repository.DnsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DnsRepositoryImpl @Inject constructor(
    private val dnsEntryDao: DnsEntryDao,
    private val packetEventBus: PacketEventBus,
) : DnsRepository {

    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        repoScope.launch {
            packetEventBus.dnsEvents.collect { event ->
                val response = event.response
                val query = event.query

                val entry = when {
                    response != null -> DnsEntry(
                        id = UUID.randomUUID().toString(),
                        timestampMs = event.timestampMs,
                        domain = response.domain,
                        resolvedIps = response.resolvedIps,
                        queryType = response.queryType,
                        uid = event.uid,
                        responseTimeMs = null,
                    )
                    query != null -> DnsEntry(
                        id = UUID.randomUUID().toString(),
                        timestampMs = event.timestampMs,
                        domain = query.domain,
                        resolvedIps = emptyList(),
                        queryType = query.queryType,
                        uid = event.uid,
                        responseTimeMs = null,
                    )
                    else -> return@collect
                }

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

    override suspend fun clearAll() =
        dnsEntryDao.clearAll()
}