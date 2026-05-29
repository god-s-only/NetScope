package com.netscope.app.domain.usecase

import com.netscope.app.domain.repository.ConnectionRepository
import com.netscope.app.domain.repository.DnsRepository
import com.netscope.app.domain.repository.TrafficRepository
import javax.inject.Inject

class ClearAllTrafficUseCase @Inject constructor(
    private val trafficRepository: TrafficRepository,
    private val dnsRepository: DnsRepository,
    private val connectionRepository: ConnectionRepository,
) {
    suspend operator fun invoke() {
        trafficRepository.clearAll()
        dnsRepository.clearAll()
        connectionRepository.clearAll()
    }
}