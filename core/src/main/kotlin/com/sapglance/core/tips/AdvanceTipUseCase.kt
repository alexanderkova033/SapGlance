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
 *
 * ## Why the engine arrives per call rather than per instance
 *
 * It was a constructor parameter until the language setting landed, and that setting is why it
 * moved. There is one [TipEngine] per language, because an engine *is* a catalog and a catalog is
 * one language's text. The obvious refactor — an [AdvanceTipUseCase] per language — quietly
 * breaks the paragraph above: a use case per language is a *mutex* per language, so a widget tap
 * and the tick worker racing across a language change would serialize against different locks and
 * could both write history. Keeping one instance and passing the engine in keeps the lock
 * covering every caller, which is the property that actually matters.
 */
class AdvanceTipUseCase(
    private val tipHistoryRepository: TipHistoryRepository,
) {
    private val mutex = Mutex()

    /** [tipEngine] is the engine for the reader's current language — see the class doc for why it
     * is a parameter rather than a field. [varietyLevel] is the Settings variety level, passed
     * straight through — see [TipEngine.messageFor] for what it does. An explicit user request for
     * a new tip (widget tap, Settings refresh button) is no different from the passive rotation
     * and used to be: there was a `manual` flag here to route a tap around the fixed sleep-hours
     * message, and the night pools removed the thing it worked around. */
    suspend operator fun invoke(
        tipEngine: TipEngine,
        now: LocalTime,
        varietyLevel: VarietyLevel = VarietyLevel.PRACTICAL,
    ): Tip =
        mutex.withLock {
            val recentTips = tipHistoryRepository.recentTips.first()
            val tip = tipEngine.messageFor(now, recentTips, varietyLevel)
            tipHistoryRepository.recordTip(tip.text)
            tip
        }
}
