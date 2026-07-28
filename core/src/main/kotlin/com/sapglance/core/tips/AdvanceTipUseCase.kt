package com.sapglance.core.tips

import com.sapglance.core.settings.VarietyLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalTime

/**
 * Picks the next tip for [now] and records it in the shared recent-tips history — the one
 * rule that the nudge worker, the widget refresh worker, and the widget's first-render
 * fallback must all apply identically for the anti-repeat guarantee (FR5, no repeats within
 * the last [TipHistoryRepository.MAX_RECENT_TIPS] tips) to actually hold. Pulled out here so
 * those three call sites share one implementation instead of each re-deriving it.
 *
 * Read-select-persist is otherwise a classic read-modify-write race: the periodic worker and a
 * widget tap calling this concurrently could both read the same [recentTips][TipHistoryRepository.recentTips]
 * snapshot before either has persisted, and independently pick (and possibly repeat) the same
 * tip. [mutex] serializes the whole operation per process; it's a field on this class rather
 * than a `companion object`/top-level lock so it stays scoped to one [AdvanceTipUseCase]
 * instance — callers must share the single instance [com.sapglance.app.AppContainer] already
 * hands out (a `by lazy` singleton) for the lock to actually cover every caller, which is also
 * exactly what the shared-instance design already required for the anti-repeat rule itself.
 */
class AdvanceTipUseCase(
    private val tipEngine: TipEngine,
    private val tipHistoryRepository: TipHistoryRepository,
) {
    private val mutex = Mutex()

    /** [manual] should be `true` for an explicit user request for a new tip (widget tap,
     * Settings refresh button) and left `false` for the passive scheduled rotation; [varietyLevel]
     * is the Settings variety level, passed straight through — see [TipEngine.messageFor] for
     * what both actually do. */
    suspend operator fun invoke(
        now: LocalTime,
        manual: Boolean = false,
        varietyLevel: VarietyLevel = VarietyLevel.PRACTICAL,
    ): Tip =
        mutex.withLock {
            val recentTips = tipHistoryRepository.recentTips.first()
            val tip = tipEngine.messageFor(now, recentTips, manual, varietyLevel)
            tipHistoryRepository.recordTip(tip.text)
            tip
        }
}
