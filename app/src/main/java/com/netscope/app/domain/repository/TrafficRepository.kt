package com.netscope.app.domain.repository

import com.netscope.app.domain.model.HttpTransaction
import com.netscope.app.domain.model.PacketInfo
import kotlinx.coroutines.flow.Flow

interface TrafficRepository {
    fun observeHttpTransactions(): Flow<List<HttpTransaction>>
    fun observePackets(): Flow<List<PacketInfo>>
    fun observeFilteredTransactions(query: String): Flow<List<HttpTransaction>>
    fun observeErrors(): Flow<List<HttpTransaction>>
    fun observeSlow(): Flow<List<HttpTransaction>>
    suspend fun getHttpTransaction(id: String): HttpTransaction?
    suspend fun deleteHttpTransaction(id: String)
    suspend fun clearAll()
}