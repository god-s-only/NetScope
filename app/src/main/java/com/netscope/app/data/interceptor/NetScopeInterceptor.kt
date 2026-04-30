package com.netscope.app.data.interceptor

import com.netscope.app.data.vpn.PacketEventBus
import com.netscope.app.domain.model.HttpMethod
import com.netscope.app.domain.model.HttpTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.internal.http.promisesBody
import okio.Buffer
import okio.GzipSource
import timber.log.Timber
import java.nio.charset.Charset
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetScopeInterceptor @Inject constructor(
    private val packetEventBus: PacketEventBus,
) : Interceptor {

    private val interceptorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val MAX_BODY_SIZE = 250_000L
        private val UTF8 = Charsets.UTF_8
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startMs = System.currentTimeMillis()
        val transactionId = UUID.randomUUID().toString()

        val requestBody = request.body
        val requestBodyString = requestBody?.let { body ->
            try {
                val buffer = Buffer()
                body.writeTo(buffer)
                val contentType = body.contentType()
                val charset = contentType?.charset(UTF8) ?: UTF8
                if (buffer.size <= MAX_BODY_SIZE) {
                    buffer.readString(charset)
                } else {
                    "[body too large: ${buffer.size} bytes]"
                }
            } catch (e: Exception) {
                "[failed to read request body]"
            }
        }

        val requestHeaders = request.headers.toMap()

        val response: Response
        val error: String?

        try {
            response = chain.proceed(request)
            error = null
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startMs
            emitTransaction(
                id = transactionId,
                request = request,
                requestHeaders = requestHeaders,
                requestBodyString = requestBodyString,
                response = null,
                responseBodyString = null,
                durationMs = durationMs,
                error = e.message ?: "Unknown error",
            )
            throw e
        }

        val durationMs = System.currentTimeMillis() - startMs

        val responseBodyString = if (response.promisesBody()) {
            try {
                val responseBody = response.peekBody(MAX_BODY_SIZE)
                val source = responseBody.source()
                source.request(Long.MAX_VALUE)

                val buffer = source.buffer.clone()
                val encoding = response.headers["Content-Encoding"]
                val contentType = responseBody.contentType()
                val charset: Charset = contentType?.charset(UTF8) ?: UTF8

                if (encoding?.equals("gzip", ignoreCase = true) == true) {
                    GzipSource(buffer).use { gzip ->
                        val decompressed = Buffer()
                        decompressed.writeAll(gzip)
                        decompressed.readString(charset)
                    }
                } else {
                    buffer.readString(charset)
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to read response body")
                "[failed to read response body]"
            }
        } else null

        emitTransaction(
            id = transactionId,
            request = request,
            requestHeaders = requestHeaders,
            requestBodyString = requestBodyString,
            response = response,
            responseBodyString = responseBodyString,
            durationMs = durationMs,
            error = null,
        )

        return response
    }

    private fun emitTransaction(
        id: String,
        request: okhttp3.Request,
        requestHeaders: Map<String, String>,
        requestBodyString: String?,
        response: Response?,
        responseBodyString: String?,
        durationMs: Long,
        error: String?,
    ) {
        val url = request.url
        val method = mapMethod(request.method)

        val transaction = HttpTransaction(
            id = id,
            timestampMs = System.currentTimeMillis() - durationMs,
            url = url.toString(),
            host = url.host,
            path = url.encodedPath,
            method = method,
            requestHeaders = requestHeaders,
            requestBody = requestBodyString,
            requestSizeBytes = request.body?.contentLength() ?: 0L,
            responseCode = response?.code,
            responseMessage = response?.message,
            responseHeaders = response?.headers?.toMap() ?: emptyMap(),
            responseBody = responseBodyString,
            responseSizeBytes = response?.body?.contentLength() ?: 0L,
            durationMs = durationMs,
            protocol = response?.protocol?.toString(),
            isReplay = false,
            error = error,
        )

        interceptorScope.launch {
            packetEventBus.emitHttpTransaction(transaction)
        }
    }

    private fun mapMethod(method: String): HttpMethod = when (method.uppercase()) {
        "GET" -> HttpMethod.GET
        "POST" -> HttpMethod.POST
        "PUT" -> HttpMethod.PUT
        "DELETE" -> HttpMethod.DELETE
        "PATCH" -> HttpMethod.PATCH
        "HEAD" -> HttpMethod.HEAD
        "OPTIONS" -> HttpMethod.OPTIONS
        else -> HttpMethod.UNKNOWN
    }

    private fun okhttp3.Headers.toMap(): Map<String, String> =
        (0 until size).associate { name(it) to value(it) }
}