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
         * These are the last N tip *texts*, on-device, and nothing else; see PRIVACY.md.
         */
        const val MAX_RECENT_TIPS = 90

        /**
         * The anti-repeat guarantee (FR5): the same tip never comes back within this many
         * shown tips. This is the product promise and the number [TipEngine] filters on. It is
         * separate from [MAX_RECENT_TIPS] so that growing the memory to improve *how varied
         * selection feels* can never quietly widen or narrow the guarantee itself.
         */
        const val ANTI_REPEAT_WINDOW = 30
    }
}
