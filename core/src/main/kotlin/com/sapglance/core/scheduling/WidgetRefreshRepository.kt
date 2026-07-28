package com.sapglance.core.scheduling

import kotlinx.coroutines.flow.Flow

/**
 * Domain-layer contract for the persisted screen-on tick counter that paces tip advancement —
 * see [shouldAdvanceTip] and [TICKS_UNTIL_ADVANCE] for why this is a tick count rather than a
 * duration. Persisted (not held in memory) so the count survives the process dying between
 * ticks. The DataStore-backed implementation lives in `:app` (`DataStoreWidgetRefreshRepository`).
 */
interface WidgetRefreshRepository {
    val screenOnTicks: Flow<Int>

    suspend fun setScreenOnTicks(ticks: Int)
}
