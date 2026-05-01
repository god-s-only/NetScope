package com.netscope.app.domain.usecase

import com.netscope.app.domain.model.HttpMethod
import com.netscope.app.domain.model.HttpTransaction
import com.netscope.app.domain.repository.TrafficRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import javax.inject.Inject

class ReplayRequestUseCase @Inject constructor(
    private val okHttpClient: OkHttpClient,
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
            try {
                val original = config.transaction
                val url = config.overrideUrl ?: original.url
                val body = config.overrideBody ?: original.requestBody
                val headers = original.requestHeaders + config.overrideHeaders

                val contentType = headers["Content-Type"]
                    ?.toMediaTypeOrNull()

                val requestBody = when (original.method) {
                    HttpMethod.GET, HttpMethod.HEAD, HttpMethod.DELETE -> null
                    else -> body?.toRequestBody(contentType)
                }

                val requestBuilder = Request.Builder().url(url)

                headers.forEach { (key, value) ->
                    requestBuilder.addHeader(key, value)
                }

                requestBuilder.method(
                    original.method.name,
                    requestBody,
                )

                val startMs = System.currentTimeMillis()
                val response = okHttpClient.newCall(requestBuilder.build()).execute()
                val durationMs = System.currentTimeMillis() - startMs

                val responseBody = response.peekBody(250_000L).string()

                val replayTransaction = original.copy(
                    id = UUID.randomUUID().toString(),
                    timestampMs = startMs,
                    responseCode = response.code,
                    responseMessage = response.message,
                    responseHeaders = response.headers.toMap(),
                    responseBody = responseBody,
                    responseSizeBytes = responseBody.length.toLong(),
                    durationMs = durationMs,
                    isReplay = true,
                    error = null,
                )

                ReplayResult.Success(replayTransaction)
            } catch (e: Exception) {
                ReplayResult.Failure(e.message ?: "Replay failed")
            }
        }

    private fun okhttp3.Headers.toMap(): Map<String, String> =
        (0 until size).associate { name(it) to value(it) }
}