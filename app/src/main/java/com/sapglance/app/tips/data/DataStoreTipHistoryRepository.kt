package com.sapglance.app.tips.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sapglance.core.tips.port.TipHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [TipHistoryRepository] backed by Jetpack DataStore, so recent tips survive process death
 * (FR7) and are shared across every surface that advances a tip (FR5). Stored as a single
 * newline-joined string rather than a `Set` preference, since a `Set` doesn't preserve the
 * order needed to trim the oldest entries first — tips are always single-line (one per line
 * in the bundled catalog resources), so newline is a safe delimiter.
 */
class DataStoreTipHistoryRepository(private val dataStore: DataStore<Preferences>) : TipHistoryRepository {
    override val recentTips: Flow<List<String>> = dataStore.data.map { it.toRecentTips() }

    override suspend fun recordTip(tip: String) {
        dataStore.edit { prefs ->
            val updated = (prefs.toRecentTips() + tip).takeLast(TipHistoryRepository.MAX_RECENT_TIPS)
            prefs[Keys.RECENT_TIPS] = updated.joinToString(DELIMITER)
        }
    }

    private fun Preferences.toRecentTips(): List<String> =
        this[Keys.RECENT_TIPS]?.split(DELIMITER)?.filter { it.isNotEmpty() } ?: emptyList()

    private object Keys {
        val RECENT_TIPS = stringPreferencesKey("recent_tips")
    }

    private companion object {
        const val DELIMITER = "\n"
    }
}
