package com.netscope.app.data.repository

import android.util.Log
import com.netscope.app.data.local.dao.HttpTransactionDao
import com.netscope.app.data.local.mapper.toDomain
import com.netscope.app.data.local.mapper.toEntity
import com.netscope.app.data.mappers.toDomain
import com.netscope.app.data.mappers.toEntity
import com.netscope.app.data.proxy.HttpTransactionEmitter
import com.netscope.app.domain.model.HttpTransaction
import com.netscope.app.domain.model.PacketInfo
import com.netscope.app.domain.repository.SettingsRepository
import com.netscope.app.domain.repository.TrafficRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TrafficRepositoryImpl"

@Singleton
class TrafficRepositoryImpl @Inject constructor(
    private val httpTransactionDao: HttpTransactionDao,
    private val httpTransactionEmitter: HttpTransactionEmitter,
    private val settingsRepository: SettingsRepository,
) : TrafficRepository {

    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        Log.d(TAG, "init — starting collector")
        repoScope.launch {
            httpTransactionEmitter.transactions.collect { transaction ->
                Log.d(TAG, "saving ${transaction.method} ${transaction.url}")
                httpTransactionDao.insert(transaction.toEntity())
                enforceLimit()
            }
        }
    }

    private suspend fun enforceLimit() {
        try {
            val settings = settingsRepository.observeSettings().first()
            val max = settings.maxStoredRequests
            if (max == -1) return

            val count = httpTransactionDao.getCount()
            if (count > max) {
                val deleteCount = count - max
                httpTransactionDao.deleteOldest(deleteCount)
                Log.d(TAG, "enforceLimit: deleted $deleteCount oldest entries (max=$max)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "enforceLimit error: ${e.message}")
        }
    }

    override fun observeHttpTransactions(): Flow<List<HttpTransaction>> =
        httpTransactionDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }

    override fun observePackets(): Flow<List<PacketInfo>> =
        flowOf(emptyList())

    override fun observeFilteredTransactions(query: String): Flow<List<HttpTransaction>> =
        httpTransactionDao.observeFiltered(query)
            .map { entities -> entities.map { it.toDomain() } }

    override fun observeErrors(): Flow<List<HttpTransaction>> =
        httpTransactionDao.observeErrors()
            .map { entities -> entities.map { it.toDomain() } }

    override fun observeSlow(): Flow<List<HttpTransaction>> =
        httpTransactionDao.observeSlow()
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun getHttpTransaction(id: String): HttpTransaction? =
        httpTransactionDao.getById(id)?.toDomain()

    override suspend fun deleteHttpTransaction(id: String) =
        httpTransactionDao.deleteById(id)

    override suspend fun clearAll() =
        httpTransactionDao.clearAll()
}