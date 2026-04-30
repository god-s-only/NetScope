package com.netscope.app.domain.usecase

import com.netscope.app.domain.model.HttpTransaction
import com.netscope.app.domain.model.TrafficFilter
import com.netscope.app.domain.repository.TrafficRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveHttpTransactionsUseCase @Inject constructor(
    private val trafficRepository: TrafficRepository,
) {
    operator fun invoke(filter: TrafficFilter = TrafficFilter()): Flow<List<HttpTransaction>> =
        trafficRepository.observeHttpTransactions()
            .map { transactions ->
                if (!filter.isActive) return@map transactions

                transactions
                    .filter { filter.matches(it) }
                    .let { filtered ->
                        if (filter.showSlowOnly) filtered.filter { it.isSlow }
                        else filtered
                    }
                    .let { filtered ->
                        if (filter.showErrorsOnly) filtered.filter {
                            it.isClientError || it.isServerError
                        } else filtered
                    }
            }
}