package com.netscope.app.data.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.netscope.app.data.local.entity.HttpTransactionEntity
import com.netscope.app.data.proxy.HttpTransactionEmitter
import com.netscope.app.domain.model.HttpMethod
import com.netscope.interceptor.NetScopeContract
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "NetScopeTransactionProvider"
private const val TRANSACTIONS = 1

class NetScopeTransactionProvider : ContentProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NetScopeProviderEntryPoint {
        fun httpTransactionEmitter(): HttpTransactionEmitter
    }

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(
            NetScopeContract.AUTHORITY,
            NetScopeContract.TRANSACTION_PATH,
            TRANSACTIONS,
        )
    }

    override fun onCreate(): Boolean {
        Log.d(TAG, "NetScopeTransactionProvider created")
        return true
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (uriMatcher.match(uri) != TRANSACTIONS) return null
        if (values == null) return null

        val ctx = context ?: return null

        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                ctx.applicationContext,
                NetScopeProviderEntryPoint::class.java,
            )
            val emitter = entryPoint.httpTransactionEmitter()

            val mapType = object : TypeToken<Map<String, String>>() {}.type
            val reqHeaders: Map<String, String> = try {
                gson.fromJson(
                    values.getAsString(NetScopeContract.COL_REQUEST_HEADERS) ?: "{}",
                    mapType,
                )
            } catch (e: Exception) { emptyMap() }

            val respHeaders: Map<String, String> = try {
                gson.fromJson(
                    values.getAsString(NetScopeContract.COL_RESPONSE_HEADERS) ?: "{}",
                    mapType,
                )
            } catch (e: Exception) { emptyMap() }

            val methodStr = values.getAsString(NetScopeContract.COL_METHOD) ?: "GET"
            val method = try {
                HttpMethod.valueOf(methodStr.uppercase())
            } catch (e: Exception) { HttpMethod.GET }

            val transaction = com.netscope.app.domain.model.HttpTransaction(
                id = values.getAsString(NetScopeContract.COL_ID)
                    ?: java.util.UUID.randomUUID().toString(),
                timestampMs = values.getAsLong(NetScopeContract.COL_TIMESTAMP_MS)
                    ?: System.currentTimeMillis(),
                url = values.getAsString(NetScopeContract.COL_URL) ?: "",
                host = values.getAsString(NetScopeContract.COL_HOST) ?: "",
                path = values.getAsString(NetScopeContract.COL_PATH) ?: "/",
                method = method,
                requestHeaders = reqHeaders,
                requestBody = values.getAsString(NetScopeContract.COL_REQUEST_BODY)
                    ?.takeIf { it.isNotBlank() },
                requestSizeBytes = values.getAsLong(NetScopeContract.COL_REQUEST_SIZE)
                    ?: 0L,
                responseCode = values.getAsInteger(NetScopeContract.COL_RESPONSE_CODE),
                responseMessage = values.getAsString(NetScopeContract.COL_RESPONSE_MESSAGE)
                    ?.takeIf { it.isNotBlank() },
                responseHeaders = respHeaders,
                responseBody = values.getAsString(NetScopeContract.COL_RESPONSE_BODY)
                    ?.takeIf { it.isNotBlank() },
                responseSizeBytes = values.getAsLong(NetScopeContract.COL_RESPONSE_SIZE)
                    ?: 0L,
                durationMs = values.getAsLong(NetScopeContract.COL_DURATION_MS) ?: 0L,
                protocol = values.getAsString(NetScopeContract.COL_PROTOCOL),
                isReplay = false,
                error = values.getAsString(NetScopeContract.COL_ERROR)
                    ?.takeIf { it.isNotBlank() },
            )

            scope.launch {
                emitter.emit(transaction)
                Log.d(TAG, "Received: ${transaction.method} ${transaction.url}" +
                        " → ${transaction.responseCode}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to process transaction: ${e.message}")
        }

        return uri
    }

    override fun query(uri: Uri, p: Array<String>?, s: String?,
                       sA: Array<String>?, sO: String?): Cursor? = null
    override fun update(uri: Uri, v: ContentValues?,
                        s: String?, sA: Array<String>?): Int = 0
    override fun delete(uri: Uri, s: String?, sA: Array<String>?): Int = 0
    override fun getType(uri: Uri): String? = null
}