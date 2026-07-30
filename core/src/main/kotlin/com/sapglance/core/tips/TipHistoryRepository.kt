package com.sapglance.core.tips

import kotlinx.coroutines.flow.Flow

/**
 * Domain-layer contract for the recently-shown tips (oldest first), which is what makes the
 * anti-repeat rule (FR5) hold across every call site combined, within a span of
 * [ANTI_REPEAT_WINDOW] tips rather than just the single previous one. The DataStore-backed
 * implementation lives in `:app` (`DataStoreTipHistoryRepository`).
 *
 * How much is *remembered* ([MAX_RECENT_TIPS]) and how far back the no-repeats rule *reaches*
 * ([ANTI_REPEAT_WINDOW]) are deliberately two different numbers — see their docs below.
 */
interface TipHistoryRepository {
    val recentTips: Flow<List<String>>

    /** Appends [tip], trimming to the [MAX_RECENT_TIPS] most recent entries. */
    suspend fun recordTip(tip: String)

    companion object {
        /**
         * How many recently-shown tips are remembered. Larger than [ANTI_REPEAT_WINDOW] on
         * purpose: everything inside the window is hard-excluded anyway, so a history exactly
         * as long as the window carries no usable recency signal at all — every eligible tip
         * looks equally unseen, and selection can do no better than uniform random. The extra
         * span is what [TipEngine] weighs by, so a tip that only just aged out of the window is
         * much less likely to come straight back than one that hasn't been shown in months.
         *
         * Chosen as window + 60 rather than as a round number: that difference *is*
         * `TipEngine.MAX_RECENCY_WEIGHT`, so holding it fixed at 60 while the window grew from 30
         * to 100 keeps the recency weighting behaving exactly as it did before, and leaves the
         * window the only thing that actually changed.
         *
         * These are the last N tip *texts*, on-device, and nothing else; see PRIVACY.md. At this
         * length that is roughly 12KB of preference data, rewritten whole on each advance.
         */
        const val MAX_RECENT_TIPS = 160

        /**
         * The anti-repeat guarantee (FR5): the same tip never comes back within this many
         * shown tips. This is the product promise and the number [TipEngine] filters on. It is
         * separate from [MAX_RECENT_TIPS] so that growing the memory to improve *how varied
         * selection feels* can never quietly widen or narrow the guarantee itself.
         *
         * **This number is capped by the content, not by preference.** [TipEngine] excludes the
         * whole window before it picks, so a window the pools cannot cover doesn't degrade
         * gracefully — groups empty out, their share is redistributed, and once nothing anywhere
         * is eligible the engine falls back to an outright repeat, breaking the promise it exists
         * to keep. The binding case is not the 280-tip catalog total but the narrowest *reachable*
         * set: a user who only ever sees the widget in one day part reaches
         * `general` + one day-part pool for practical draws, which is 76-78 tips, and at
         * `VarietyLevel.PRACTICAL` around 80% of draws want to come from exactly there.
         *
         * 100 survives that because an exhausted practical tier redistributes into tone rather
         * than repeating — measured over 3000 draws per variety level, in every single day part,
         * the closest a tip came back was 101 draws and the practical share drifted only from
         * ~81% to ~73%. `TipCatalogTest` pins this so a future content trim can't quietly
         * invalidate it. Raising the window further needs more tips per pool first, not just a
         * bigger number here.
         */
        const val ANTI_REPEAT_WINDOW = 100
    }
}
