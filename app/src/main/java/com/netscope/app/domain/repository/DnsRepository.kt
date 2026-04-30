package com.netscope.app.domain.repository

import com.netscope.app.domain.model.DnsEntry
import kotlinx.coroutines.flow.Flow

interface DnsRepository {
    fun observeDnsEntries(): Flow<List<DnsEntry>>
    fun observeByDomain(query: String): Flow<List<DnsEntry>>
    suspend fun getDnsEntry(id: String): DnsEntry?
    suspend fun getDomainsForApp(uid: Int): List<String>
    suspend fun clearAll()
}