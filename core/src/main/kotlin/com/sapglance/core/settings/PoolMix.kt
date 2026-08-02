package com.sapglance.core.settings

/**
 * How much of each pool the reader wants, one [PoolAmount] per voice in the catalog.
 *
 * ## What replaced what, and why
 *
 * This replaced `VarietyLevel`, a single three-position control (Practical / Balanced / Playful)
 * that treated motivation, philosophy and wellbeing as one undifferentiated "tone" lump. That was
 * wrong in a way the pools themselves make obvious: the three `.txt` files carry three different
 * writing rules and are three genuinely different things to be handed at 9am. A reader who wants
 * Marcus Aurelius and does not want to be told to seize the day had exactly one control, and it
 * moved both at once.
 *
 * ## The two axes, which are deliberately separate
 *
 * [practical] decides **how much tone there is at all** — it is the practical-versus-everything-
 * else split, and it is the only control that changes that ratio. The three tone amounts decide
 * **how the tone share is divided between the voices**, relative to each other.
 *
 * The consequence worth knowing before anyone reports it as a bug: setting all three tone pools
 * to [PoolAmount.PLENTY] behaves exactly like setting all three to [PoolAmount.SOME], because
 * relative weights are all that a share can be divided by. Wanting more of all three at once is
 * really wanting less practical, and [practical] is where that is expressed.
 *
 * ## What the day part still decides
 *
 * These amounts scale [com.sapglance.core.tips.ToneProfile]'s per-hour weighting rather than
 * replacing it, so the editorial timing survives the reader's preferences. The visible case is
 * motivation, which the night profile zeroes: setting motivation to [PoolAmount.PLENTY] does not
 * produce "Two minutes. Set a timer. Go." at 3am, because anything multiplied by zero is zero.
 * That is intended. The reader is choosing which voices they like, not what time it is.
 */
data class PoolMix(
    val practical: PoolAmount = PoolAmount.PLENTY,
    val philosophy: PoolAmount = PoolAmount.SOME,
    val motivation: PoolAmount = PoolAmount.SOME,
    val wellbeing: PoolAmount = PoolAmount.SOME,
) {
    /**
     * Every pool turned off, which would leave the engine with nothing to draw and is the one
     * state this type exists to name. The settings screen refuses to produce it by not letting
     * the last pool be switched off, and [com.sapglance.core.tips.TipEngine] does not trust the
     * settings screen: persisted state outlives the UI that wrote it, and a migration or a
     * hand-edited DataStore file can produce anything.
     */
    val isSilent: Boolean get() = enabledCount == 0

    /**
     * How many pools are switched on. The settings screen reads this to disable the "none" option
     * on whichever pool is the last one standing, which is how [isSilent] is kept unreachable
     * from the UI — a disabled control the reader can see explains itself better than a tap that
     * silently does nothing.
     */
    val enabledCount: Int
        get() = listOf(practical, philosophy, motivation, wellbeing).count { it != PoolAmount.NONE }

    companion object {
        /**
         * Practical-heavy, every voice present in small measure. Chosen to be behaviourally
         * identical to the old `VarietyLevel.PRACTICAL` default rather than as a fresh judgement:
         * the practical tips are the app's core, and an upgrade should not quietly re-tune what
         * an existing reader sees. A test pins that equivalence.
         */
        val DEFAULT = PoolMix()
    }
}
