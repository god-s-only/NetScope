package com.netscope.app.domain.usecase

import com.netscope.app.domain.model.ConnectionEntry
import com.netscope.app.domain.repository.ConnectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ObserveConnectionsUseCase @Inject constructor(
    private val connectionRepository: ConnectionRepository,
) {
    data class ConnectionState(
        val all: List<ConnectionEntry>,
        val active: List<ConnectionEntry>,
        val flagged: List<ConnectionEntry>,
        val activeCount: Int,
    )

    operator fun invoke(): Flow<ConnectionState> = combine(
        connectionRepository.observeConnections(),
        connectionRepository.observeActiveConnections(),
        connectionRepository.observeFlaggedConnections(),
    ) { all, active, flagged ->
        ConnectionState(
            all = all,
            active = active,
            flagged = flagged,
            activeCount = active.size,
        )
    }
}