package com.netscope.app.domain.usecase

import com.netscope.app.domain.model.AppInfo
import com.netscope.app.domain.repository.AppInfoRepository
import javax.inject.Inject

class GetAppListUseCase @Inject constructor(
    private val appInfoRepository: AppInfoRepository,
) {
    suspend operator fun invoke(includeSystemApps: Boolean = false): List<AppInfo> {
        val all = appInfoRepository.getAllApps()
        return if (includeSystemApps) all
        else all.filter { !it.isSystemApp }
    }
}