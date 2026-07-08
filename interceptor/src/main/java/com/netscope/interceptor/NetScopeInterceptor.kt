package com.netscope.interceptor

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.internal.http.promisesBody
import okio.Buffer
import okio.GzipSource
import java.util.UUID

private const val TAG = "NetScopeInterceptor"
private const val MAX_BODY_SIZE = 250_000L

class NetScopeInterceptor(private val context: Context) : Interceptor {

    private val gson = Gson()

    private val providerUri = Uri.parse(
        "content://${NetScopeContract.AUTHORITY}/${NetScopeContract.TRANSACTION_PATH}"
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startMs = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()

        val requestBodyString = request.body?.let { body ->
            try {
                val buffer = Buffer()
                body.writeTo(buffer)
                val charset = body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
                if (buffer.size <= MAX_BODY_SIZE) buffer.readString(charset)
                else "[body too large: ${buffer.size} bytes]"
            } catch (e: Exception) {
                "[failed to read request body]"
            }
        }

        val requestHeaders = headersToJson(
            (0 until request.headers.size).associate {
                request.headers.name(it) to request.headers.value(it)
            }
        )

        val response: Response
        val error: String?
        try {
            response = chain.proceed(request)
            error = null
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startMs
            emit(
                id = id,
                startMs = startMs,
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
                val charset = responseBody.contentType()
                    ?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
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
                "[failed to read response body]"
            }
        } else null

        emit(
            id = id,
            startMs = startMs,
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

    private fun emit(
        id: String,
        startMs: Long,
        request: okhttp3.Request,
        requestHeaders: String,
        requestBodyString: String?,
        response: Response?,
        responseBodyString: String?,
        durationMs: Long,
        error: String?,
    ) {
        try {
            val url = request.url
            val responseHeaders = response?.headers?.let { headers ->
                headersToJson(
                    (0 until headers.size).associate {
                        headers.name(it) to headers.value(it)
                    }
                )
            } ?: "{}"

            val values = ContentValues().apply {
                put(NetScopeContract.COL_ID, id)
                put(NetScopeContract.COL_TIMESTAMP_MS, startMs)
                put(NetScopeContract.COL_URL, url.toString())
                put(NetScopeContract.COL_HOST, url.host)
                put(NetScopeContract.COL_PATH, url.encodedPath)
                put(NetScopeContract.COL_METHOD, request.method.uppercase())
                put(NetScopeContract.COL_REQUEST_HEADERS, requestHeaders)
                put(NetScopeContract.COL_REQUEST_BODY, requestBodyString ?: "")
                put(NetScopeContract.COL_REQUEST_SIZE,
                    request.body?.contentLength() ?: 0L)
                put(NetScopeContract.COL_RESPONSE_CODE, response?.code)
                put(NetScopeContract.COL_RESPONSE_MESSAGE, response?.message ?: "")
                put(NetScopeContract.COL_RESPONSE_HEADERS, responseHeaders)
                put(NetScopeContract.COL_RESPONSE_BODY, responseBodyString ?: "")
                put(NetScopeContract.COL_RESPONSE_SIZE,
                    responseBodyString?.toByteArray()?.size?.toLong() ?: 0L)
                put(NetScopeContract.COL_DURATION_MS, durationMs)
                put(NetScopeContract.COL_PROTOCOL,
                    response?.protocol?.toString() ?: "HTTP/1.1")
                put(NetScopeContract.COL_ERROR, error ?: "")
            }

            context.contentResolver.insert(providerUri, values)
            Log.d(TAG, "Sent: ${request.method} ${url.host} → ${response?.code}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send to NetScope: ${e.message}")
        }
    }

    private fun headersToJson(headers: Map<String, String>): String =
        gson.toJson(headers)
}