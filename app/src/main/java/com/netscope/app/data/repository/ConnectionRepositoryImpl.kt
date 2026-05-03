package com.netscope.app.data.repository

import com.netscope.app.domain.model.ConnectionEntry
import com.netscope.app.domain.repository.ConnectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ConnectionRepositoryImpl @Inject constructor(): ConnectionRepository {
    override fun observeConnections(): Flow<List<ConnectionEntry>> {
        return flow { emit(emptyList()) }
    }

    override fun observeActiveConnections(): Flow<List<ConnectionEntry>> {
        return flow { emit(emptyList()) }
    }

    override fun observeFlaggedConnections(): Flow<List<ConnectionEntry>> {
        return flow { emit(emptyList()) }
    }

    override suspend fun clearAll() {
        Unit
    }
}