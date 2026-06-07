package com.netscope.app.data.repository

import android.util.Log
import com.netscope.app.data.proxy.HttpTransactionEmitter
import com.netscope.app.domain.model.AppInfo
import com.netscope.app.domain.model.BandwidthSnapshot
import com.netscope.app.domain.repository.BandwidthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BandwidthRepositoryImpl"

@Singleton
class BandwidthRepositoryImpl @Inject constructor(
    private val httpTransactionEmitter: HttpTransactionEmitter,
) : BandwidthRepository {

    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var uploadBytesWindow = 0L
    private var downloadBytesWindow = 0L

    // running totals
    private var totalUpload = 0L
    private var totalDownload = 0L

    private val _snapshots = MutableStateFlow<List<BandwidthSnapshot>>(emptyList())
    private val _perApp = MutableStateFlow<Map<String, BandwidthSnapshot>>(emptyMap())

    init {
        Log.d(TAG, "init — starting collectors")

        repoScope.launch {
            httpTransactionEmitter.transactions.collect { transaction ->
                uploadBytesWindow += transaction.requestSizeBytes
                downloadBytesWindow += transaction.responseSizeBytes
                totalUpload += transaction.requestSizeBytes
                totalDownload += transaction.responseSizeBytes
            }
        }

        repoScope.launch {
            while (true) {
                delay(1_000L)
                val now = System.currentTimeMillis()

                val snapshot = BandwidthSnapshot(
                    timestampMs = now,
                    appInfo = null,
                    uploadBytesPerSec = uploadBytesWindow,
                    downloadBytesPerSec = downloadBytesWindow,
                    totalUploadBytes = totalUpload,
                    totalDownloadBytes = totalDownload,
                )

                _snapshots.value = listOf(snapshot)

                Log.v(TAG,
                    "Snapshot: upload=${uploadBytesWindow}B/s " +
                            "download=${downloadBytesWindow}B/s"
                )

                uploadBytesWindow = 0L
                downloadBytesWindow = 0L
            }
        }
    }

    override fun observeBandwidthSnapshots(): Flow<List<BandwidthSnapshot>> =
        _snapshots

    override fun observePerAppBandwidth(): Flow<Map<String, BandwidthSnapshot>> =
        _perApp
}