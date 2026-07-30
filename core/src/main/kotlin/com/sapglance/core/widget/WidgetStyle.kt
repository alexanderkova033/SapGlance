package com.sapglance.core.widget

import com.sapglance.core.tips.DayPart
import com.sapglance.core.tips.TipKind

/**
 * Which background style the home-screen widget renders with. Purely a symbolic choice here —
 * mapping a style to an actual drawable is `:app`'s job (this module has no Android resource
 * concept).
 *
 * [isLight] is the one thing about a style that `:core` does know, because the selection below
 * needs it: a pale card at 3am is the defect this whole mapping exists to remove. `:app` derives
 * the card's ink from it rather than choosing one alongside the drawable, so a style physically
 * cannot ship with text colour that fights its artwork — the property that the old single
 * drawable-and-ink `when` was protecting by convention is now protected by construction.
 */
enum class WidgetStyle(val isLight: Boolean) {
    // Dark cards (pale text on deep artwork).
    FOREST(isLight = false),
    OCEAN(isLight = false),
    SUNSET(isLight = false),
    MIDNIGHT(isLight = false),
    AURORA(isLight = false),
    DAWN(isLight = false),
    RAIN(isLight = false),

    // Light cards (dark text on pale artwork).
    WINTER(isLight = true),
    PAPER(isLight = true),
    MEADOW(isLight = true),
    BLOSSOM(isLight = true),
    ;

    companion object {
        /**
         * The background for the tip currently showing: narrow to a palette by what the tip *is*
         * and when it arrived, then pick within that palette by the text.
         *
         * The two halves do different jobs and both are load-bearing. The palette is what stops
         * the card contradicting the tip — this used to hash across all eleven styles, so a
         * philosophy line at 2am could land on the bright Meadow card and a morning stretch tip
         * on Midnight. The hash *within* the palette is what keeps a new tip looking like a new
         * card, which is the whole reason the background follows the tip rather than a setting.
         * Selecting on kind and hour alone would have thrown that away: consecutive tips of the
         * same kind in the same hour would all draw the identical background.
         *
         * [kind] is null for a text the catalog no longer knows (a tip reworded or dropped since
         * it was persisted is still sitting in someone's history), and is treated as the neutral
         * register — the hour alone decides. Both [kind] and [dayPart] are passed in rather than
         * read: this is pure `:core` with no clock and no catalog of its own, the same reason
         * `TipEngine` takes a `LocalTime`.
         */
        fun forTip(
            tipText: String,
            kind: TipKind?,
            dayPart: DayPart,
        ): WidgetStyle {
            val palette = paletteFor(kind, dayPart)
            return palette[Math.floorMod(tipText.hashCode(), palette.size)]
        }

        /**
         * Palettes are deliberately six-ish rather than a handful, and they overlap. Splitting
         * the eleven styles cleanly by hour would leave a morning-only user four backgrounds for
         * life, which trades one kind of monotony for another; the pale four appear across every
         * daylight hour and only the accents move. Every style is reachable from some palette,
         * which a test pins, so no artwork quietly becomes unused.
         */
        private val MORNING_PALETTE =
            listOf(WINTER, PAPER, MEADOW, BLOSSOM, DAWN, AURORA)

        private val AFTERNOON_PALETTE =
            listOf(WINTER, PAPER, MEADOW, BLOSSOM, OCEAN, FOREST)

        private val EVENING_PALETTE =
            listOf(SUNSET, RAIN, FOREST, OCEAN, AURORA, PAPER)

        /** Deep and quiet, and the only palette with no pale card in it at all. */
        private val NIGHT_PALETTE =
            listOf(MIDNIGHT, RAIN, OCEAN, FOREST)

        /**
         * Kind shifts *which hour's* palette is used rather than narrowing the hour's own, which
         * is what keeps every palette wide enough to stay varied. Philosophy reads as an evening
         * register whatever the clock says, so it borrows the evening palette; motivation reads
         * as a morning one and borrows that.
         *
         * Night is the exception and no voice overrides it. The point of a quiet card between
         * 23:00 and 05:59 is that the hour is what it is, and motivation cannot arrive then
         * anyway — `ToneProfile` weights it to zero.
         */
        private fun paletteFor(
            kind: TipKind?,
            dayPart: DayPart,
        ): List<WidgetStyle> =
            when (dayPart) {
                DayPart.SLEEP_LATE, DayPart.SLEEP_EARLY_HOURS -> NIGHT_PALETTE
                DayPart.MORNING -> forVoice(kind, whenNeutral = MORNING_PALETTE)
                DayPart.AFTERNOON -> forVoice(kind, whenNeutral = AFTERNOON_PALETTE)
                DayPart.EVENING -> forVoice(kind, whenNeutral = EVENING_PALETTE)
            }

        private fun forVoice(
            kind: TipKind?,
            whenNeutral: List<WidgetStyle>,
        ): List<WidgetStyle> =
            when (kind) {
                TipKind.PHILOSOPHY -> EVENING_PALETTE
                TipKind.MOTIVATION -> MORNING_PALETTE
                // PRACTICAL and WELLBEING are the app's neutral registers, and so is a tip the
                // catalog no longer recognises. The hour decides on its own.
                else -> whenNeutral
            }
    }
}
