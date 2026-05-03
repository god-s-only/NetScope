package com.netscope.app.data.repository

import com.netscope.app.domain.model.DnsEntry
import com.netscope.app.domain.repository.DnsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DnsRepositoryImpl @Inject constructor() : DnsRepository {

    override fun observeDnsEntries(): Flow<List<DnsEntry>> =
        flow { emit(emptyList()) }

    override fun observeByDomain(query: String): Flow<List<DnsEntry>> =
        flow { emit(emptyList()) }

    override suspend fun getDnsEntry(id: String): DnsEntry? = null

    override suspend fun getDomainsForApp(uid: Int): List<String> = emptyList()

    override suspend fun clearAll() = Unit
}