package com.netscope.app.domain.model

data class HttpTransaction(
    val id: String,
    val timestampMs: Long,
    val url: String,
    val host: String,
    val path: String,
    val method: HttpMethod,
    val requestHeaders: Map<String, String>,
    val requestBody: String?,
    val requestSizeBytes: Long,
    val responseCode: Int?,
    val responseMessage: String?,
    val responseHeaders: Map<String, String>,
    val responseBody: String?,
    val responseSizeBytes: Long,
    val durationMs: Long,
    val protocol: String?,
    val uid: Int? = null,
    val appInfo: AppInfo? = null,
    val isReplay: Boolean = false,
    val error: String? = null,
) {
    val isSuccess: Boolean get() = responseCode in 200..299
    val isRedirect: Boolean get() = responseCode in 300..399
    val isClientError: Boolean get() = responseCode in 400..499
    val isServerError: Boolean get() = responseCode in 500..599
    val isSlow: Boolean get() = durationMs > 2000
    val statusCategory: StatusCategory get() = when {
        error != null -> StatusCategory.ERROR
        responseCode == null -> StatusCategory.PENDING
        isSuccess -> StatusCategory.SUCCESS
        isRedirect -> StatusCategory.REDIRECT
        isClientError -> StatusCategory.CLIENT_ERROR
        isServerError -> StatusCategory.SERVER_ERROR
        else -> StatusCategory.UNKNOWN
    }
}

enum class HttpMethod { GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS, UNKNOWN }

enum class StatusCategory {
    PENDING, SUCCESS, REDIRECT, CLIENT_ERROR, SERVER_ERROR, ERROR, UNKNOWN
}