package com.netscope.app.data.repository

import com.netscope.app.data.local.dao.HttpTransactionDao
import com.netscope.app.data.mappers.toDomain
import com.netscope.app.data.mappers.toEntity
import com.netscope.app.data.proxy.HttpTransactionEmitter
import com.netscope.app.domain.model.HttpTransaction
import com.netscope.app.domain.model.PacketInfo
import com.netscope.app.domain.repository.TrafficRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrafficRepositoryImpl @Inject constructor(
    private val httpTransactionDao: HttpTransactionDao,
    private val httpTransactionEmitter: HttpTransactionEmitter,
) : TrafficRepository {

    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        Timber.d("TrafficRepositoryImpl: init — starting collector")
        repoScope.launch {
            httpTransactionEmitter.transactions.collect { transaction ->
                Timber.d(
                    "TrafficRepositoryImpl: saving " +
                            "${transaction.method} ${transaction.url} → ${transaction.responseCode}"
                )
                httpTransactionDao.insert(transaction.toEntity())
            }
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