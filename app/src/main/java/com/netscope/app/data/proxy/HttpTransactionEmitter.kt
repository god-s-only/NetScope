package com.netscope.app.data.proxy

import com.netscope.app.domain.model.HttpTransaction
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpTransactionEmitter @Inject constructor() {

    private val _transactions = MutableSharedFlow<HttpTransaction>(
        replay              = 0,
        extraBufferCapacity = 500,
    )
    val transactions: SharedFlow<HttpTransaction> = _transactions.asSharedFlow()

    suspend fun emit(transaction: HttpTransaction) {
        Timber.d(
            "HttpTransactionEmitter: ${transaction.method} " +
                    "${transaction.url} → ${transaction.responseCode}"
        )
        _transactions.emit(transaction)
    }
}