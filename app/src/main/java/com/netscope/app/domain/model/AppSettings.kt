package com.netscope.app.domain.model

data class AppSettings(
    val maxStoredRequests: Int = 500,
    val autoScrollTrafficList: Boolean = true,
    val showReplayedRequests: Boolean = true,
)

enum class MaxRequestsOption(val value: Int, val label: String) {
    FIFTY(50, "50"),
    ONE_HUNDRED(100, "100"),
    FIVE_HUNDRED(500, "500"),
    ONE_THOUSAND(1000, "1000"),
    UNLIMITED(-1, "Unlimited"),
}