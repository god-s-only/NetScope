package com.netscope.app.data.vpn

import com.netscope.app.domain.model.HttpTransaction
import com.netscope.app.domain.model.PacketInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PacketEventBus @Inject constructor() {

    private val _packets = MutableSharedFlow<PacketInfo>(
        replay = 0,
        extraBufferCapacity = 1000,
    )
    val packets: SharedFlow<PacketInfo> = _packets.asSharedFlow()

    private val _dnsEvents = MutableSharedFlow<DnsEvent>(
        replay = 0,
        extraBufferCapacity = 200,
    )
    val dnsEvents: SharedFlow<DnsEvent> = _dnsEvents.asSharedFlow()

    private val _httpTransactions = MutableSharedFlow<HttpTransaction>(
        replay = 0,
        extraBufferCapacity = 500,
    )
    val httpTransactions: SharedFlow<HttpTransaction> = _httpTransactions.asSharedFlow()

    suspend fun emitPacket(packet: PacketInfo) {
        _packets.emit(packet)
    }

    suspend fun emitDnsEvent(event: DnsEvent) {
        _dnsEvents.emit(event)
    }

    suspend fun emitHttpTransaction(transaction: HttpTransaction) {
        _httpTransactions.emit(transaction)
    }
}

data class DnsEvent(
    val query: DnsQuery?,
    val response: DnsResponse?,
    val uid: Int,
    val timestampMs: Long = System.currentTimeMillis(),
)