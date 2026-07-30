package com.sapglance.core.tips.model

/**
 * How a draw's *tone* share splits across the three tone pools at a given [DayPart].
 *
 * The three tone pools are genuinely different things, not three names for "not practical" —
 * each `.txt` carries its own writing rule, and they land very differently by hour. A
 * motivational push ("Stop researching it. Go and do it badly.") is the right sentence at 9am
 * and completely the wrong one at 23:40; a quiet wellbeing invitation ("Put both hands around
 * something warm and stay there for a minute.") is the reverse. Weighing all three as one
 * undifferentiated blob, which is what selection used to do, meant a mistimed line roughly as
 * often as a well-timed one — and a mistimed line doesn't read as variety, it reads as the app
 * not knowing what time it is.
 *
 * This is deliberately *not* a user setting. `VarietyLevel` already decides how much tone the
 * user wants; which tone suits 2am is an editorial judgement they have no basis to make and
 * shouldn't be asked to. See README "Notable design decisions".
 *
 * Weights are relative within a draw's tone share, and are required to sum to [SCALE] purely so
 * the table below reads as "out of ten" at a glance rather than needing mental normalisation.
 */
internal data class ToneProfile(
    val motivation: Int,
    val philosophy: Int,
    val wellbeing: Int,
) {
    init {
        require(motivation >= 0 && philosophy >= 0 && wellbeing >= 0) {
            "Tone weights must not be negative: $this"
        }
        require(motivation + philosophy + wellbeing == SCALE) {
            "Tone weights must sum to $SCALE, found ${motivation + philosophy + wellbeing} in $this"
        }
    }

    companion object {
        /** What every profile's weights sum to, so each one reads as "out of ten". */
        const val SCALE = 10

        /**
         * Motivation leads the morning and fades across the day; wellbeing and philosophy do the
         * opposite. The night profile zeroes motivation outright, which is the one place this
         * table filters rather than leans: "Two minutes. Set a timer. Go." at 3am is not a
         * weaker version of good advice, it's the opposite of what the hour calls for. That
         * doesn't contradict the "variety is a lean, never a filter" rule, which is a promise
         * about what the *user's setting* can do — no `VarietyLevel` ever silences a group. This
         * is editorial timing, and it is enforced by a test rather than left to intention.
         */
        fun forDayPart(dayPart: DayPart): ToneProfile =
            when (dayPart) {
                DayPart.MORNING -> ToneProfile(motivation = 5, philosophy = 3, wellbeing = 2)
                DayPart.AFTERNOON -> ToneProfile(motivation = 4, philosophy = 2, wellbeing = 4)
                DayPart.EVENING -> ToneProfile(motivation = 1, philosophy = 4, wellbeing = 5)
                DayPart.SLEEP_LATE, DayPart.SLEEP_EARLY_HOURS ->
                    ToneProfile(motivation = 0, philosophy = 4, wellbeing = 6)
            }
    }
}
