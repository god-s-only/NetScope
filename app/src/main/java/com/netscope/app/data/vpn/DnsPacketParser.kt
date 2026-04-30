package com.netscope.app.data.vpn

import com.netscope.app.domain.model.DnsQueryType
import timber.log.Timber
import java.nio.ByteBuffer

data class DnsQuery(
    val transactionId: Int,
    val domain: String,
    val queryType: DnsQueryType,
)

data class DnsResponse(
    val transactionId: Int,
    val domain: String,
    val resolvedIps: List<String>,
    val queryType: DnsQueryType,
)

object DnsPacketParser {

    private const val TYPE_A = 1
    private const val TYPE_AAAA = 28
    private const val TYPE_CNAME = 5
    private const val TYPE_MX = 15
    private const val TYPE_TXT = 16

    // DNS flags
    private const val FLAG_QR = 0x8000
    private const val FLAG_RCODE_MASK = 0x000F

    fun parseQuery(payload: ByteArray): DnsQuery? {
        return try {
            if (payload.size < 12) return null
            val buffer = ByteBuffer.wrap(payload)

            val transactionId = buffer.short.toInt() and 0xFFFF
            val flags = buffer.short.toInt() and 0xFFFF

            if (flags and FLAG_QR != 0) return null

            val questionCount = buffer.short.toInt() and 0xFFFF
            if (questionCount == 0) return null

            buffer.getShort()
            buffer.getShort()
            buffer.getShort()

            val domain = readDomainName(buffer, payload)
            val qtype = buffer.short.toInt() and 0xFFFF

            DnsQuery(
                transactionId = transactionId,
                domain = domain,
                queryType = mapQueryType(qtype),
            )
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse DNS query")
            null
        }
    }

    fun parseResponse(payload: ByteArray): DnsResponse? {
        return try {
            if (payload.size < 12) return null
            val buffer = ByteBuffer.wrap(payload)

            val transactionId = buffer.short.toInt() and 0xFFFF
            val flags = buffer.short.toInt() and 0xFFFF

            if (flags and FLAG_QR == 0) return null

            if (flags and FLAG_RCODE_MASK != 0) return null

            val questionCount = buffer.short.toInt() and 0xFFFF
            val answerCount = buffer.short.toInt() and 0xFFFF
            buffer.getShort()
            buffer.getShort()

            if (answerCount == 0) return null

            var domain = ""
            repeat(questionCount) {
                domain = readDomainName(buffer, payload)
                buffer.getShort()
                buffer.getShort()
            }

            val resolvedIps = mutableListOf<String>()
            var queryType = DnsQueryType.UNKNOWN

            repeat(answerCount) {
                readDomainName(buffer, payload)
                val rtype = buffer.short.toInt() and 0xFFFF
                buffer.getShort()
                buffer.getInt()
                val rdLength = buffer.short.toInt() and 0xFFFF

                queryType = mapQueryType(rtype)

                when (rtype) {
                    TYPE_A -> {
                        if (rdLength == 4) {
                            val ip = buildString {
                                repeat(4) { i ->
                                    if (i > 0) append(".")
                                    append(buffer.get().toInt() and 0xFF)
                                }
                            }
                            resolvedIps.add(ip)
                        } else {
                            repeat(rdLength) { buffer.get() }
                        }
                    }
                    TYPE_AAAA -> {
                        if (rdLength == 16) {
                            val ipv6 = buildString {
                                for (i in 0 until 8) {
                                    if (i > 0) append(":")
                                    append(
                                        ((buffer.get().toInt() and 0xFF) shl 8 or
                                                (buffer.get().toInt() and 0xFF))
                                            .toString(16)
                                    )
                                }
                            }
                            resolvedIps.add(ipv6)
                        } else {
                            repeat(rdLength) { buffer.get() }
                        }
                    }
                    else -> repeat(rdLength) { buffer.get() }
                }
            }

            if (domain.isEmpty() || resolvedIps.isEmpty()) return null

            DnsResponse(
                transactionId = transactionId,
                domain = domain,
                resolvedIps = resolvedIps,
                queryType = queryType,
            )
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse DNS response")
            null
        }
    }

    private fun readDomainName(buffer: ByteBuffer, raw: ByteArray): String {
        val parts = mutableListOf<String>()
        var jumped = false
        var safetyLimit = 64

        while (safetyLimit-- > 0) {
            val length = buffer.get().toInt() and 0xFF
            when {
                length == 0 -> break
                length and 0xC0 == 0xC0 -> {
                    // pointer compression
                    val offset = ((length and 0x3F) shl 8) or (buffer.get().toInt() and 0xFF)
                    if (!jumped) {
                        jumped = true
                    }
                    buffer.position(offset)
                }
                else -> {
                    val label = ByteArray(length)
                    buffer.get(label)
                    parts.add(String(label, Charsets.US_ASCII))
                }
            }
        }
        return parts.joinToString(".")
    }

    private fun mapQueryType(type: Int): DnsQueryType = when (type) {
        TYPE_A -> DnsQueryType.A
        TYPE_AAAA -> DnsQueryType.AAAA
        TYPE_CNAME -> DnsQueryType.CNAME
        TYPE_MX -> DnsQueryType.MX
        TYPE_TXT -> DnsQueryType.TXT
        else -> DnsQueryType.UNKNOWN
    }
}