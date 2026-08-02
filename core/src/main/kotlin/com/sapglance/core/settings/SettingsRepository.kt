package com.sapglance.core.settings

import kotlinx.coroutines.flow.Flow

/**
 * Domain-layer contract for reading/writing [AppSettings]. The DataStore-backed
 * implementation lives in `:app` (`DataStoreSettingsRepository`) — nothing in this module
 * knows or cares how settings are actually persisted.
 */
interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setPoolMix(mix: PoolMix)

    suspend fun setLanguage(language: TipLanguage)
}
