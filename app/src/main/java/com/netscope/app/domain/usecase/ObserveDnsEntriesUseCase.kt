package com.netscope.app.domain.usecase

import com.netscope.app.domain.model.DnsEntry
import com.netscope.app.domain.repository.DnsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDnsEntriesUseCase @Inject constructor(
    private val dnsRepository: DnsRepository,
) {
    operator fun invoke(searchQuery: String = ""): Flow<List<DnsEntry>> =
        if (searchQuery.isBlank()) {
            dnsRepository.observeDnsEntries()
        } else {
            dnsRepository.observeByDomain(searchQuery)
        }
}