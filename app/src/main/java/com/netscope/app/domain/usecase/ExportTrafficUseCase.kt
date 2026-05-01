package com.netscope.app.domain.usecase

import com.google.gson.GsonBuilder
import com.netscope.app.domain.model.HttpTransaction
import com.netscope.app.domain.repository.TrafficRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ExportTrafficUseCase @Inject constructor(
    private val trafficRepository: TrafficRepository,
) {
    sealed class ExportResult {
        data class Success(val json: String, val fileName: String) : ExportResult()
        data class Failure(val error: String) : ExportResult()
    }

    suspend operator fun invoke(): ExportResult = withContext(Dispatchers.IO) {
        try {
            val transactions = trafficRepository.observeHttpTransactions().first()
            val har = buildHar(transactions)
            val gson = GsonBuilder().setPrettyPrinting().create()
            ExportResult.Success(
                json = gson.toJson(har),
                fileName = "netscope_export_${System.currentTimeMillis()}.har",
            )
        } catch (e: Exception) {
            ExportResult.Failure(e.message ?: "Export failed")
        }
    }

    private fun buildHar(transactions: List<HttpTransaction>): Map<String, Any> {
        val entries = transactions.map { tx ->
            mapOf(
                "startedDateTime" to java.util.Date(tx.timestampMs).toString(),
                "time" to tx.durationMs,
                "request" to mapOf(
                    "method" to tx.method.name,
                    "url" to tx.url,
                    "httpVersion" to (tx.protocol ?: "HTTP/1.1"),
                    "headers" to tx.requestHeaders.map { (k, v) ->
                        mapOf("name" to k, "value" to v)
                    },
                    "queryString" to emptyList<Any>(),
                    "postData" to mapOf(
                        "mimeType" to (tx.requestHeaders["Content-Type"] ?: ""),
                        "text" to (tx.requestBody ?: ""),
                    ),
                    "headersSize" to -1,
                    "bodySize" to tx.requestSizeBytes,
                ),
                "response" to mapOf(
                    "status" to (tx.responseCode ?: 0),
                    "statusText" to (tx.responseMessage ?: ""),
                    "httpVersion" to (tx.protocol ?: "HTTP/1.1"),
                    "headers" to tx.responseHeaders.map { (k, v) ->
                        mapOf("name" to k, "value" to v)
                    },
                    "content" to mapOf(
                        "size" to tx.responseSizeBytes,
                        "mimeType" to (tx.responseHeaders["Content-Type"] ?: ""),
                        "text" to (tx.responseBody ?: ""),
                    ),
                    "redirectURL" to "",
                    "headersSize" to -1,
                    "bodySize" to tx.responseSizeBytes,
                ),
                "timings" to mapOf(
                    "send" to 0,
                    "wait" to tx.durationMs,
                    "receive" to 0,
                ),
            )
        }

        return mapOf(
            "log" to mapOf(
                "version" to "1.2",
                "creator" to mapOf(
                    "name" to "NetScope",
                    "version" to "1.0.0",
                ),
                "entries" to entries,
            )
        )
    }
}