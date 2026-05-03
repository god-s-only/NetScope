package com.netscope.app.domain.usecase

import com.netscope.app.domain.repository.ConnectionRepository
import com.netscope.app.domain.repository.DnsRepository
import com.netscope.app.domain.repository.TrafficRepository
import javax.inject.Inject

class ClearAllTrafficUseCase @Inject constructor(
    private val trafficRepository: TrafficRepository,
) {
    suspend operator fun invoke() {
        trafficRepository.clearAll()
    }
}