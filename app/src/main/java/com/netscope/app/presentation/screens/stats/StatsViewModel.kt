package com.netscope.app.presentation.screens.stats

import androidx.lifecycle.viewModelScope
import com.netscope.app.domain.model.HttpTransaction
import com.netscope.app.domain.usecase.ObserveConnectionsUseCase
import com.netscope.app.domain.usecase.ObserveDnsEntriesUseCase
import com.netscope.app.domain.usecase.ObserveHttpTransactionsUseCase
import com.netscope.app.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class StatsUiState(
    val totalRequests: Int = 0,
    val totalBytesSent: Long = 0L,
    val totalBytesReceived: Long = 0L,
    val avgResponseTimeMs: Long = 0L,
    val slowestRequestMs: Long = 0L,
    val fastestRequestMs: Long = 0L,
    val slowestRequestUrl: String = "",
    val fastestRequestUrl: String = "",
    val errorCount: Int = 0,
    val errorRate: Float = 0f,
    val mostActiveHost: String = "",
    val mostActiveHostCount: Int = 0,
    val mostUsedMethod: String = "",
    val totalUniqueHosts: Int = 0,
    val totalUniqueDomains: Int = 0,
    val totalConnections: Int = 0,
    val successCount: Int = 0,
    val redirectCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val observeHttpTransactionsUseCase: ObserveHttpTransactionsUseCase,
    private val observeDnsEntriesUseCase: ObserveDnsEntriesUseCase,
    private val observeConnectionsUseCase: ObserveConnectionsUseCase,
) : BaseViewModel<StatsUiState>(StatsUiState()) {

    init {
        observeStats()
    }

    private fun observeStats() {
        combine(
            observeHttpTransactionsUseCase(),
            observeDnsEntriesUseCase(),
            observeConnectionsUseCase(),
        ) { transactions, dnsEntries, connections ->
            computeStats(
                transactions = transactions,
                uniqueDomains = dnsEntries.size,
                totalConnections = connections.all.size,
            )
        }
            .onEach { newState -> updateState { newState } }
            .catch { e -> updateState { copy(error = e.message, isLoading = false) } }
            .launchIn(viewModelScope)
    }

    private fun computeStats(
        transactions: List<HttpTransaction>,
        uniqueDomains: Int,
        totalConnections: Int,
    ): StatsUiState {
        if (transactions.isEmpty()) {
            return StatsUiState(
                isLoading = false,
                totalUniqueDomains = uniqueDomains,
                totalConnections = totalConnections,
            )
        }

        val totalRequests = transactions.size
        val totalBytesSent = transactions.sumOf { it.requestSizeBytes }
        val totalBytesReceived = transactions.sumOf { it.responseSizeBytes }

        val successCount = transactions.count { it.isSuccess }
        val redirectCount = transactions.count { it.isRedirect }
        val errorCount = transactions.count { it.isClientError || it.isServerError }
        val errorRate = if (totalRequests > 0)
            errorCount.toFloat() / totalRequests.toFloat() * 100f
        else 0f

        val completedTransactions = transactions.filter {
            it.responseCode != null && it.durationMs > 0
        }

        val avgResponseTimeMs = if (completedTransactions.isNotEmpty())
            completedTransactions.map { it.durationMs }.average().toLong()
        else 0L

        val slowest = completedTransactions.maxByOrNull { it.durationMs }
        val fastest = completedTransactions.minByOrNull { it.durationMs }

        val hostCounts = transactions
            .groupBy { it.host }
            .mapValues { it.value.size }
        val mostActiveEntry = hostCounts.maxByOrNull { it.value }

        val methodCounts = transactions
            .groupBy { it.method.name }
            .mapValues { it.value.size }
        val mostUsedMethod = methodCounts.maxByOrNull { it.value }?.key ?: ""

        val uniqueHosts = transactions.map { it.host }.toSet().size

        return StatsUiState(
            totalRequests = totalRequests,
            totalBytesSent = totalBytesSent,
            totalBytesReceived = totalBytesReceived,
            avgResponseTimeMs = avgResponseTimeMs,
            slowestRequestMs = slowest?.durationMs ?: 0L,
            fastestRequestMs = fastest?.durationMs ?: 0L,
            slowestRequestUrl = slowest?.host ?: "",
            fastestRequestUrl = fastest?.host ?: "",
            errorCount = errorCount,
            errorRate = errorRate,
            mostActiveHost = mostActiveEntry?.key ?: "",
            mostActiveHostCount = mostActiveEntry?.value ?: 0,
            mostUsedMethod = mostUsedMethod,
            totalUniqueHosts = uniqueHosts,
            totalUniqueDomains = uniqueDomains,
            totalConnections = totalConnections,
            successCount = successCount,
            redirectCount = redirectCount,
            isLoading = false,
            error = null,
        )
    }
}