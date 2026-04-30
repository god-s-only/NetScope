package com.netscope.app.data.repostiory

import com.netscope.app.data.local.dao.HttpTransactionDao
import com.netscope.app.data.mappers.toDomain
import com.netscope.app.data.mappers.toEntity
import com.netscope.app.data.vpn.PacketEventBus
import com.netscope.app.domain.model.HttpTransaction
import com.netscope.app.domain.model.PacketInfo
import com.netscope.app.domain.repository.TrafficRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrafficRepositoryImpl @Inject constructor(
    private val httpTransactionDao: HttpTransactionDao,
    private val packetEventBus: PacketEventBus,
) : TrafficRepository {

    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _packets = MutableSharedFlow<PacketInfo>(
        replay = 0,
        extraBufferCapacity = 1000,
    )
    val livePackets: SharedFlow<PacketInfo> = _packets.asSharedFlow()

    init {
        repoScope.launch {
            packetEventBus.httpTransactions.collect { transaction ->
                httpTransactionDao.insert(transaction.toEntity())
            }
        }

        repoScope.launch {
            packetEventBus.packets.collect { packet ->
                _packets.emit(packet)
            }
        }
    }

    override fun observeHttpTransactions(): Flow<List<HttpTransaction>> =
        httpTransactionDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }

    override fun observePackets(): Flow<List<PacketInfo>> =
        httpTransactionDao.observeAll()
            .map { emptyList() }

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