package com.sapglance.core.widget

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.sapglance.core.tips.DayPart
import com.sapglance.core.tips.TipKind
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

/** Every (kind, hour) the selection can be asked about, including the unknown-kind case. */
private val ALL_KINDS: List<TipKind?> = TipKind.entries + null

private fun stylesFor(
    kind: TipKind?,
    dayPart: DayPart,
    tips: Int = 400,
): Set<WidgetStyle> = (1..tips).map { WidgetStyle.forTip("Tip number $it", kind, dayPart) }.toSet()

class WidgetStyleTest {
    @Test
    fun `the same tip in the same hour always maps to the same style`() {
        val tipText = "Stand up and stretch for a moment."
        assertThat(WidgetStyle.forTip(tipText, TipKind.PRACTICAL, DayPart.MORNING))
            .isEqualTo(WidgetStyle.forTip(tipText, TipKind.PRACTICAL, DayPart.MORNING))
    }

    /**
     * The half of the old behaviour that had to survive narrowing by kind and hour: a new tip is
     * meant to visibly refresh the whole card. Selecting on kind and hour alone would have made
     * every consecutive tip of the same kind in the same hour draw the identical background.
     */
    @ParameterizedTest
    @EnumSource(DayPart::class)
    fun `a palette is wide enough that different tips still look different`(dayPart: DayPart) {
        ALL_KINDS.forEach { kind ->
            assertWithMessage("%s at %s", kind, dayPart)
                .that(stylesFor(kind, dayPart).size)
                .isAtLeast(MIN_PALETTE)
        }
    }

    /**
     * The defect this mapping exists to remove, and the one the roadmap named: a philosophy line
     * at 2am landing on the bright Meadow card. No voice overrides the hour here.
     */
    @Test
    fun `the sleep hours never draw a pale card`() {
        listOf(DayPart.SLEEP_LATE, DayPart.SLEEP_EARLY_HOURS).forEach { night ->
            ALL_KINDS.forEach { kind ->
                assertWithMessage("%s at %s", kind, night)
                    .that(stylesFor(kind, night).none { it.isLight })
                    .isTrue()
            }
        }
    }

    /** The mirror of the case above: a morning stretch tip should not arrive on Midnight. */
    @Test
    fun `daylight hours never draw the midnight card`() {
        listOf(DayPart.MORNING, DayPart.AFTERNOON, DayPart.EVENING).forEach { dayPart ->
            ALL_KINDS.forEach { kind ->
                assertWithMessage("%s at %s", kind, dayPart)
                    .that(stylesFor(kind, dayPart))
                    .doesNotContain(WidgetStyle.MIDNIGHT)
            }
        }
    }

    /**
     * Kind shifts which hour's palette is borrowed rather than narrowing the hour's own, so
     * philosophy in the morning gets the evening card set. Pinned because the tempting
     * simplification — filtering the morning palette down to its dark entries — is what shrinks
     * a palette to two and breaks the case above it.
     */
    @Test
    fun `philosophy borrows the evening palette in daylight, and motivation the morning one`() {
        val philosophyAtNoon = stylesFor(TipKind.PHILOSOPHY, DayPart.AFTERNOON)
        val motivationAtDusk = stylesFor(TipKind.MOTIVATION, DayPart.EVENING)

        assertThat(philosophyAtNoon).isEqualTo(stylesFor(TipKind.PHILOSOPHY, DayPart.EVENING))
        assertThat(motivationAtDusk).isEqualTo(stylesFor(TipKind.MOTIVATION, DayPart.MORNING))
        // ...and that is a real change from the neutral registers at the same hours.
        assertThat(philosophyAtNoon).isNotEqualTo(stylesFor(TipKind.PRACTICAL, DayPart.AFTERNOON))
    }

    /**
     * Narrowing by kind and hour can silently orphan artwork: a style in no palette is a drawable
     * that ships in the APK and is never drawn. This is the only check that would catch it.
     */
    @Test
    fun `every style is reachable from some kind and hour`() {
        val reachable =
            DayPart.entries
                .flatMap { dayPart -> ALL_KINDS.map { kind -> stylesFor(kind, dayPart) } }
                .flatten()
                .toSet()

        assertThat(reachable).containsExactlyElementsIn(WidgetStyle.entries)
    }

    /** An unrecognised tip has no kind, and must be treated as neutral rather than throwing. */
    @Test
    fun `an unknown kind falls back to the hour alone`() {
        assertThat(stylesFor(null, DayPart.MORNING))
            .isEqualTo(stylesFor(TipKind.PRACTICAL, DayPart.MORNING))
    }

    @Test
    fun `negative hash codes still resolve to a valid entry`() {
        // This text's hashCode() is negative (-356442182) — a naive `hashCode() % size` would
        // produce a negative index and crash; `Math.floorMod` must not.
        val style =
            WidgetStyle.forTip("Get up and walk around for two minutes.", TipKind.PRACTICAL, DayPart.AFTERNOON)
        assertThat(WidgetStyle.entries).contains(style)
    }

    private companion object {
        /**
         * Below this a palette stops being a rotation. Four is the night set, which is
         * deliberately the narrowest: there is less artwork that suits 3am, and the hours are
         * the app's least-visited.
         */
        const val MIN_PALETTE = 4
    }
}
