package com.netscope.app.domain.model

data class DnsEntry(
    val id: String,
    val timestampMs: Long,
    val domain: String,
    val resolvedIps: List<String>,
    val queryType: DnsQueryType,
    val uid: Int,
    val appInfo: AppInfo? = null,
    val responseTimeMs: Long? = null,
)

enum class DnsQueryType { A, AAAA, CNAME, MX, TXT, UNKNOWN }