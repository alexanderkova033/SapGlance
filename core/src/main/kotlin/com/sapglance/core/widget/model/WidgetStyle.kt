package com.sapglance.core.widget.model

/**
 * Which background style the home-screen widget renders with. Purely a symbolic choice here —
 * mapping a style to an actual drawable/gradient is `:app`'s job (this module has no Android
 * resource concept).
 */
enum class WidgetStyle {
    // Dark cards (pale text on deep artwork).
    FOREST,
    OCEAN,
    SUNSET,
    MIDNIGHT,
    AURORA,
    DAWN,
    RAIN,

    // Light cards (dark text on pale artwork). Which group a style belongs to isn't recorded
    // here — `:app` pairs each one with both its drawable and its ink in a single `when`, so
    // the two can't be set independently and a new style can't ship without an ink choice.
    WINTER,
    PAPER,
    MEADOW,
    BLOSSOM,
    ;

    companion object {
        /**
         * Ties the background to whichever tip is currently showing rather than to a stored
         * user preference: the same tip text always maps to the same style, and a different
         * tip (almost always, modulo hash collisions) means a different style — a new tip
         * arriving is meant to visibly refresh the whole card, not just the text.
         */
        fun forTip(tipText: String): WidgetStyle = entries[Math.floorMod(tipText.hashCode(), entries.size)]
    }
}
