package com.sapglance.app.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sapglance.core.settings.model.AppSettings
import com.sapglance.core.settings.model.VarietyLevel
import com.sapglance.core.settings.port.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [SettingsRepository] backed by Jetpack DataStore. Depends on [DataStore] rather than
 * [android.content.Context] so it can be unit-tested on the plain JVM with an
 * in-memory-backed DataStore, with no Robolectric/Android dependency.
 */
class DataStoreSettingsRepository(private val dataStore: DataStore<Preferences>) : SettingsRepository {
    override val settings: Flow<AppSettings> = dataStore.data.map { it.toAppSettings() }

    override suspend fun setVarietyLevel(level: VarietyLevel) {
        dataStore.edit { it[Keys.VARIETY_LEVEL] = level.name }
    }

    /**
     * Falls back to [Keys.LEGACY_MORE_VARIETY_ENABLED] — the boolean toggle this setting
     * replaced — when [Keys.VARIETY_LEVEL] hasn't been written yet, so a preference set before
     * this migration isn't silently reset to the default on upgrade.
     */
    private fun Preferences.toAppSettings(): AppSettings =
        AppSettings(
            varietyLevel =
                this[Keys.VARIETY_LEVEL]?.let { runCatching { VarietyLevel.valueOf(it) }.getOrNull() }
                    ?: legacyVarietyLevel()
                    ?: AppSettings.DEFAULT.varietyLevel,
        )

    private fun Preferences.legacyVarietyLevel(): VarietyLevel? =
        this[Keys.LEGACY_MORE_VARIETY_ENABLED]?.let { if (it) VarietyLevel.PLAYFUL else VarietyLevel.PRACTICAL }

    private object Keys {
        val VARIETY_LEVEL = stringPreferencesKey("variety_level")
        val LEGACY_MORE_VARIETY_ENABLED = booleanPreferencesKey("more_variety_enabled")
    }
}
