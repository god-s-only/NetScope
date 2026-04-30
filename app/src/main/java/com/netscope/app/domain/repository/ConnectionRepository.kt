package com.netscope.app.domain.repository

import com.netscope.app.domain.model.ConnectionEntry
import kotlinx.coroutines.flow.Flow

interface ConnectionRepository {
    fun observeConnections(): Flow<List<ConnectionEntry>>
    fun observeActiveConnections(): Flow<List<ConnectionEntry>>
    fun observeFlaggedConnections(): Flow<List<ConnectionEntry>>
    suspend fun clearAll()
}