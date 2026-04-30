package com.netscope.app.data.vpn

import com.netscope.app.domain.model.Direction
import com.netscope.app.domain.model.Protocol
import timber.log.Timber
import java.nio.ByteBuffer

data class RawPacket(
    val sourceIp: String,
    val destinationIp: String,
    val sourcePort: Int,
    val destinationPort: Int,
    val protocol: Protocol,
    val direction: Direction,
    val sizeBytes: Int,
    val payload: ByteArray,
    val timestampMs: Long = System.currentTimeMillis(),
)

object PacketParser {

    private const val PROTOCOL_TCP = 6
    private const val PROTOCOL_UDP = 17
    private const val PROTOCOL_ICMP = 1

    const val DNS_PORT = 53

    fun parse(buffer: ByteBuffer, limit: Int): RawPacket? {
        return try {
            if (limit < 20) return null

            buffer.rewind()

            val firstByte = buffer.get().toInt() and 0xFF
            val ipVersion = firstByte shr 4

            if (ipVersion != 4) return null

            val headerLength = (firstByte and 0x0F) * 4
            if (limit < headerLength) return null

            buffer.position(1)
            buffer.get()
            val totalLength = buffer.short.toInt() and 0xFFFF
            buffer.getShort()
            buffer.getShort()
            buffer.get()

            val protocolByte = buffer.get().toInt() and 0xFF
            buffer.getShort()

            val protocol = when (protocolByte) {
                PROTOCOL_TCP -> Protocol.TCP
                PROTOCOL_UDP -> Protocol.UDP
                PROTOCOL_ICMP -> Protocol.ICMP
                else -> Protocol.UNKNOWN
            }

            val sourceIp = readIp(buffer)
            val destinationIp = readIp(buffer)

            buffer.position(headerLength)

            val sourcePort: Int
            val destinationPort: Int

            when (protocol) {
                Protocol.TCP, Protocol.UDP -> {
                    if (limit < headerLength + 4) return null
                    sourcePort = buffer.short.toInt() and 0xFFFF
                    destinationPort = buffer.short.toInt() and 0xFFFF
                }
                else -> {
                    sourcePort = 0
                    destinationPort = 0
                }
            }

            val direction = if (destinationPort in OUTBOUND_PORTS || destinationPort > sourcePort) {
                Direction.OUTBOUND
            } else {
                Direction.INBOUND
            }

            val payloadStart = headerLength
            val payloadSize = (totalLength - headerLength).coerceAtLeast(0)
            val payload = ByteArray(payloadSize.coerceAtMost(limit - payloadStart))
            if (payload.isNotEmpty()) {
                buffer.position(payloadStart)
                buffer.get(payload, 0, payload.size)
            }

            RawPacket(
                sourceIp = sourceIp,
                destinationIp = destinationIp,
                sourcePort = sourcePort,
                destinationPort = destinationPort,
                protocol = protocol,
                direction = direction,
                sizeBytes = totalLength,
                payload = payload,
            )
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse packet")
            null
        }
    }

    private fun readIp(buffer: ByteBuffer): String {
        val a = buffer.get().toInt() and 0xFF
        val b = buffer.get().toInt() and 0xFF
        val c = buffer.get().toInt() and 0xFF
        val d = buffer.get().toInt() and 0xFF
        return "$a.$b.$c.$d"
    }

    private val OUTBOUND_PORTS = setOf(
        80, 443, 53, 8080, 8443, 25, 587, 465,
        993, 995, 110, 143, 21, 22, 3306, 5432,
    )
}