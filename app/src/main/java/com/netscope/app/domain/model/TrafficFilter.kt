package com.netscope.app.domain.model

data class TrafficFilter(
    val searchQuery: String = "",
    val methods: Set<HttpMethod> = emptySet(),
    val statusCategories: Set<StatusCategory> = emptySet(),
    val protocols: Set<Protocol> = emptySet(),
    val showSlowOnly: Boolean = false,
    val showErrorsOnly: Boolean = false,
    val appPackages: Set<String> = emptySet(),
    val minDurationMs: Long? = null,
) {
    val isActive: Boolean get() =
        searchQuery.isNotBlank() ||
                methods.isNotEmpty() ||
                statusCategories.isNotEmpty() ||
                protocols.isNotEmpty() ||
                showSlowOnly ||
                showErrorsOnly ||
                appPackages.isNotEmpty() ||
                minDurationMs != null

    fun matches(transaction: HttpTransaction): Boolean {
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            val matchesUrl = transaction.url.lowercase().contains(q)
            val matchesHost = transaction.host.lowercase().contains(q)
            val matchesBody = transaction.responseBody?.lowercase()?.contains(q) == true
            if (!matchesUrl && !matchesHost && !matchesBody) return false
        }
        if (methods.isNotEmpty() && transaction.method !in methods) return false
        if (statusCategories.isNotEmpty() && transaction.statusCategory !in statusCategories) return false
        if (showSlowOnly && !transaction.isSlow) return false
        if (showErrorsOnly && !transaction.isClientError && !transaction.isServerError) return false
        if (appPackages.isNotEmpty() && transaction.appInfo?.packageName !in appPackages) return false
        if (minDurationMs != null && transaction.durationMs < minDurationMs) return false
        return true
    }
}