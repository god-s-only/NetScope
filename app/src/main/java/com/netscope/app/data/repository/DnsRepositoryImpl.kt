package com.netscope.app.data.repository

import com.netscope.app.domain.model.DnsEntry
import com.netscope.app.domain.repository.DnsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DnsRepositoryImpl @Inject constructor(): DnsRepository {
    override fun observeDnsEntries(): Flow<List<DnsEntry>> {
        TODO("Not yet implemented")
    }

    override fun observeByDomain(query: String): Flow<List<DnsEntry>> {
        TODO("Not yet implemented")
    }

    override suspend fun getDnsEntry(id: String): DnsEntry? {
        TODO("Not yet implemented")
    }

    override suspend fun getDomainsForApp(uid: Int): List<String> {
        TODO("Not yet implemented")
    }

    override suspend fun clearAll() {
        TODO("Not yet implemented")
    }
}