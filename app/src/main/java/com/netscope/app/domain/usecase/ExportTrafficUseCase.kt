package com.netscope.app.domain.usecase

import android.util.Log
import com.netscope.app.domain.model.HttpTransaction
import com.netscope.app.domain.repository.TrafficRepository
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

private const val TAG = "ExportTrafficUseCase"

class ExportTrafficUseCase @Inject constructor(
    private val trafficRepository: TrafficRepository,
) {
    sealed class ExportResult {
        data class Success(
            val json: String,
            val fileName: String,
        ) : ExportResult()
        data class Failure(val error: String) : ExportResult()
    }

    suspend operator fun invoke(): ExportResult = withContext(Dispatchers.IO) {
        try {
            val transactions = trafficRepository.observeHttpTransactions().first()

            if (transactions.isEmpty()) {
                return@withContext ExportResult.Failure("No traffic to export")
            }

            val har = buildHar(transactions)
            val gson = GsonBuilder().setPrettyPrinting().create()
            val json = gson.toJson(har)
            val fileName = "netscope_${System.currentTimeMillis()}.har"

            Log.d(TAG, "Exported ${transactions.size} transactions to $fileName")
            ExportResult.Success(json = json, fileName = fileName)

        } catch (e: Exception) {
            Log.e(TAG, "Export failed: ${e.message}")
            ExportResult.Failure(e.message ?: "Export failed")
        }
    }

    private fun buildHar(transactions: List<HttpTransaction>): Map<String, Any> {
        val isoFormat = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            Locale.getDefault(),
        ).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val entries = transactions.map { tx ->
            mapOf(
                "startedDateTime" to isoFormat.format(Date(tx.timestampMs)),
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
                        "mimeType" to
                                (tx.responseHeaders["Content-Type"] ?: "text/plain"),
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
                "cache" to emptyMap<String, Any>(),
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