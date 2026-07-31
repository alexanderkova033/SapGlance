package com.sapglance.app.widget.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sapglance.core.widget.WidgetRefreshRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [WidgetRefreshRepository] backed by Jetpack DataStore, so the last-advanced switch window
 * survives the process dying between ticks.
 *
 * The key this replaced, `widget_screen_on_ticks`, is deliberately neither migrated nor deleted.
 * Not migrated because a tick count carries no information about *which* window we are in, so
 * there is nothing in it to translate — the first tick after an upgrade sees a null slot and
 * advances, which is the right outcome anyway. Not deleted because removing it would mean a
 * DataStore write on every reader's first launch to reclaim four bytes.
 */
class DataStoreWidgetRefreshRepository(private val dataStore: DataStore<Preferences>) : WidgetRefreshRepository {
    override val lastTipSlot: Flow<String?> = dataStore.data.map { it[Keys.LAST_TIP_SLOT] }

    override suspend fun setLastTipSlot(slot: String) {
        dataStore.edit { it[Keys.LAST_TIP_SLOT] = slot }
    }

    private object Keys {
        val LAST_TIP_SLOT = stringPreferencesKey("widget_last_tip_slot")
    }
}
