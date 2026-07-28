package com.sapglance.app.widget.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.sapglance.core.scheduling.WidgetRefreshRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** [WidgetRefreshRepository] backed by Jetpack DataStore, so the screen-on tick count survives
 * process death between ticks. */
class DataStoreWidgetRefreshRepository(private val dataStore: DataStore<Preferences>) : WidgetRefreshRepository {
    override val screenOnTicks: Flow<Int> = dataStore.data.map { it[Keys.SCREEN_ON_TICKS] ?: 0 }

    override suspend fun setScreenOnTicks(ticks: Int) {
        dataStore.edit { it[Keys.SCREEN_ON_TICKS] = ticks }
    }

    private object Keys {
        val SCREEN_ON_TICKS = intPreferencesKey("widget_screen_on_ticks")
    }
}
