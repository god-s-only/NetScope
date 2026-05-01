package com.netscope.app.domain.usecase

import com.netscope.app.domain.model.HttpTransaction
import com.netscope.app.domain.repository.TrafficRepository
import javax.inject.Inject

class GetHttpTransactionDetailUseCase @Inject constructor(
    private val trafficRepository: TrafficRepository,
) {
    suspend operator fun invoke(id: String): HttpTransaction? =
        trafficRepository.getHttpTransaction(id)
}