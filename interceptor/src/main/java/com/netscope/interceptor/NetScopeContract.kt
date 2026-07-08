package com.netscope.interceptor

object NetScopeContract {
    const val NETSCOPE_PACKAGE = "com.netscope.app"
    const val AUTHORITY = "$NETSCOPE_PACKAGE.provider"
    const val TRANSACTION_PATH = "transactions"

    const val COL_ID = "id"
    const val COL_TIMESTAMP_MS = "timestampMs"
    const val COL_URL = "url"
    const val COL_HOST = "host"
    const val COL_PATH = "path"
    const val COL_METHOD = "method"
    const val COL_REQUEST_HEADERS = "requestHeaders"
    const val COL_REQUEST_BODY = "requestBody"
    const val COL_REQUEST_SIZE = "requestSizeBytes"
    const val COL_RESPONSE_CODE = "responseCode"
    const val COL_RESPONSE_MESSAGE = "responseMessage"
    const val COL_RESPONSE_HEADERS = "responseHeaders"
    const val COL_RESPONSE_BODY = "responseBody"
    const val COL_RESPONSE_SIZE = "responseSizeBytes"
    const val COL_DURATION_MS = "durationMs"
    const val COL_PROTOCOL = "protocol"
    const val COL_ERROR = "error"
}