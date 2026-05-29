package com.netscope.app.domain.usecase

import com.netscope.app.domain.model.AppSettings
import com.netscope.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<AppSettings> =
        settingsRepository.observeSettings()
}