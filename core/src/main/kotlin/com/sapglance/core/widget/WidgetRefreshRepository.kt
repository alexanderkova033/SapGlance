package com.sapglance.core.widget

import kotlinx.coroutines.flow.Flow

/**
 * Domain-layer contract for the one thing the refresh worker has to remember between runs: which
 * switch window it last advanced the tip for. See [currentSlotStart] and [TIP_SWITCH_HOURS].
 *
 * Persisted rather than held in memory because the process dies between ticks routinely, and
 * because a forgotten slot costs an extra tip rather than a missed one — on a fresh install, and
 * on the first run after upgrading from the screen-on-tick scheme this replaced, the stored value
 * is null and the next tick advances immediately. That is the intended behaviour for both.
 *
 * Stored as the slot's ISO string rather than as a timestamp or an index. An index would have to
 * be interpreted against a date to mean anything, and a timestamp invites arithmetic; the point
 * of the slot design is that the only operation ever performed on it is equality.
 *
 * The DataStore-backed implementation lives in `:app` (`DataStoreWidgetRefreshRepository`).
 */
interface WidgetRefreshRepository {
    /** Null until the first advance on this install. */
    val lastTipSlot: Flow<String?>

    suspend fun setLastTipSlot(slot: String)
}
