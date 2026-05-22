package com.netscope.app.presentation.navigation

object NavRoutes {
    const val DASHBOARD = "dashboard"
    const val SETUP = "setup"
    const val TRAFFIC_LIST = "traffic_list"
    const val TRAFFIC_DETAIL = "traffic_detail/{${NavArgs.TRANSACTION_ID}}"
    const val DNS = "dns"
    const val TIMELINE = "timeline"
    const val REPLAY = "replay/{${NavArgs.TRANSACTION_ID}}"

    fun trafficDetail(id: String) = "traffic_detail/$id"
    fun replay(id: String) = "replay/$id"
}