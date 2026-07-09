package com.netscope.app.domain.usecase

import android.util.Log
import com.netscope.app.domain.model.HttpMethod
import com.netscope.app.domain.model.HttpTransaction
import com.netscope.app.domain.repository.TrafficRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.net.Socket
import java.util.UUID
import javax.inject.Inject

private const val TAG = "ReplayRequestUseCase"
private const val PROXY_PORT = 8888

class ReplayRequestUseCase @Inject constructor(
    private val trafficRepository: TrafficRepository,
) {
    data class ReplayConfig(
        val transaction: HttpTransaction,
        val overrideHeaders: Map<String, String> = emptyMap(),
        val overrideBody: String? = null,
        val overrideUrl: String? = null,
    )

    sealed class ReplayResult {
        data class Success(val transaction: HttpTransaction) : ReplayResult()
        data class Failure(val error: String) : ReplayResult()
    }

    suspend operator fun invoke(config: ReplayConfig): ReplayResult =
        withContext(Dispatchers.IO) {
            val original = config.transaction
            val url = config.overrideUrl ?: original.url
            val body = config.overrideBody ?: original.requestBody
            val headers = original.requestHeaders + config.overrideHeaders
            val startMs = System.currentTimeMillis()
            val id = UUID.randomUUID().toString()

            try {
                val socket = Socket("127.0.0.1", PROXY_PORT)
                socket.soTimeout = 30_000

                val output = socket.getOutputStream()
                val reader = socket.getInputStream().bufferedReader()

                val isHttps = url.startsWith("https://", ignoreCase = true)
                val host = extractHost(url)
                    ?: return@withContext ReplayResult.Failure("Invalid URL: $url")
                val path = extractPath(url)

                if (isHttps) {
                    val port = extractPort(url, 443)
                    val connectRequest = "CONNECT $host:$port HTTP/1.1\r\n" +
                            "Host: $host:$port\r\n\r\n"
                    output.write(connectRequest.toByteArray())
                    output.flush()

                    val connectResponse = reader.readLine()
                    if (connectResponse == null || !connectResponse.contains("200")) {
                        return@withContext ReplayResult.Failure(
                            "Proxy CONNECT failed: $connectResponse"
                        )
                    }
                    var line = reader.readLine()
                    while (!line.isNullOrBlank()) {
                        line = reader.readLine()
                    }
                }

                val method = original.method.name
                val requestBuilder = StringBuilder()
                requestBuilder.append("$method $path HTTP/1.1\r\n")
                requestBuilder.append("Host: $host\r\n")

                headers.forEach { (k, v) ->
                    if (!k.equals("Host", ignoreCase = true) &&
                        !k.equals("Connection", ignoreCase = true) &&
                        !k.equals("Proxy-Connection", ignoreCase = true)) {
                        requestBuilder.append("$k: $v\r\n")
                    }
                }

                body?.let {
                    requestBuilder.append("Content-Length: ${it.toByteArray().size}\r\n")
                }
                requestBuilder.append("Connection: close\r\n\r\n")
                body?.let { requestBuilder.append(it) }

                output.write(requestBuilder.toString().toByteArray())
                output.flush()

                val statusLine = reader.readLine()?.trim()
                    ?: return@withContext ReplayResult.Failure("No response from server")

                val statusParts = statusLine.split(" ", limit = 3)
                val statusCode = statusParts.getOrNull(1)?.toIntOrNull() ?: 0
                val statusMessage = statusParts.getOrNull(2) ?: ""

                val responseHeaders = mutableMapOf<String, String>()
                var contentLength = -1
                var transferEncoding = ""

                var rLine = reader.readLine()
                while (!rLine.isNullOrBlank()) {
                    val colon = rLine.indexOf(':')
                    if (colon > 0) {
                        val k = rLine.substring(0, colon).trim()
                        val v = rLine.substring(colon + 1).trim()
                        responseHeaders[k] = v
                        when {
                            k.equals("Content-Length", ignoreCase = true) ->
                                contentLength = v.toIntOrNull() ?: -1
                            k.equals("Transfer-Encoding", ignoreCase = true) ->
                                transferEncoding = v
                        }
                    }
                    rLine = reader.readLine()
                }

                val responseBody = readBody(reader, contentLength, transferEncoding)
                val durationMs = System.currentTimeMillis() - startMs

                runCatching { socket.close() }

                Log.d(TAG, "Replay: $method $url → $statusCode (${durationMs}ms)")

                ReplayResult.Success(
                    original.copy(
                        id = id,
                        timestampMs = startMs,
                        url = url,
                        requestHeaders = headers,
                        requestBody = body,
                        responseCode = statusCode,
                        responseMessage = statusMessage,
                        responseHeaders = responseHeaders,
                        responseBody = responseBody,
                        responseSizeBytes = responseBody.toByteArray().size.toLong(),
                        durationMs = durationMs,
                        isReplay = true,
                        error = null,
                    )
                )

            } catch (e: Exception) {
                Log.w(TAG, "Replay failed: ${e.message}")
                ReplayResult.Failure(e.message ?: "Replay failed")
            }
        }

    private fun readBody(
        reader: BufferedReader,
        contentLength: Int,
        transferEncoding: String,
    ): String {
        return try {
            when {
                transferEncoding.equals("chunked", ignoreCase = true) -> {
                    val sb = StringBuilder()
                    while (true) {
                        val sizeLine = reader.readLine()?.trim() ?: break
                        val chunkSize = sizeLine.toIntOrNull(16) ?: break
                        if (chunkSize == 0) break
                        val buf = CharArray(chunkSize)
                        reader.read(buf)
                        sb.append(buf)
                        reader.readLine()
                    }
                    sb.toString()
                }
                contentLength > 0 -> {
                    val buf = CharArray(contentLength)
                    reader.read(buf)
                    String(buf)
                }
                else -> reader.readText()
            }
        } catch (e: Exception) {
            Log.w(TAG, "readBody error: ${e.message}")
            ""
        }
    }

    private fun extractHost(url: String): String? = try {
        java.net.URL(url).host
    } catch (e: Exception) { null }

    private fun extractPort(url: String, default: Int): Int = try {
        val p = java.net.URL(url).port
        if (p == -1) default else p
    } catch (e: Exception) { default }

    private fun extractPath(url: String): String = try {
        val u = java.net.URL(url)
        val path = u.path.ifEmpty { "/" }
        if (u.query != null) "$path?${u.query}" else path
    } catch (e: Exception) { "/" }
}