package com.sapglance.core.settings.port

import com.sapglance.core.settings.model.AppSettings
import com.sapglance.core.settings.model.VarietyLevel
import kotlinx.coroutines.flow.Flow

/**
 * Domain-layer contract for reading/writing [AppSettings]. The DataStore-backed
 * implementation lives in `:app` (`DataStoreSettingsRepository`) — nothing in this module
 * knows or cares how settings are actually persisted.
 */
interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setVarietyLevel(level: VarietyLevel)
}
