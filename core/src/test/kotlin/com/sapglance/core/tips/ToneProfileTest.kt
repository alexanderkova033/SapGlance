package com.sapglance.core.tips

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ToneProfileTest {
    @Test
    fun `every day part has a profile`() {
        DayPart.entries.forEach { dayPart ->
            val profile = ToneProfile.forDayPart(dayPart)
            assertThat(profile.motivation + profile.philosophy + profile.wellbeing)
                .isEqualTo(ToneProfile.SCALE)
        }
    }

    /**
     * The one deliberate zero in the table, and the only place tone selection filters rather
     * than leans - see [ToneProfile.forDayPart] for the argument. Pinned here so it can't be
     * softened by accident during a later tuning pass.
     */
    @Test
    fun `motivation is silenced at night and present at every waking hour`() {
        val night = listOf(DayPart.SLEEP_LATE, DayPart.SLEEP_EARLY_HOURS)
        DayPart.entries.forEach { dayPart ->
            val motivation = ToneProfile.forDayPart(dayPart).motivation
            if (dayPart in night) {
                assertThat(motivation).isEqualTo(0)
            } else {
                assertThat(motivation).isGreaterThan(0)
            }
        }
    }

    /** Wellbeing and philosophy carry the night between them, so night is never empty of tone. */
    @Test
    fun `night still has quiet tone content to offer`() {
        listOf(DayPart.SLEEP_LATE, DayPart.SLEEP_EARLY_HOURS).forEach { dayPart ->
            val profile = ToneProfile.forDayPart(dayPart)
            assertThat(profile.philosophy).isGreaterThan(0)
            assertThat(profile.wellbeing).isGreaterThan(0)
        }
    }

    @Test
    fun `weights that do not sum to the scale are rejected`() {
        assertThrows<IllegalArgumentException> {
            ToneProfile(motivation = 1, philosophy = 1, wellbeing = 1)
        }
    }

    @Test
    fun `negative weights are rejected`() {
        assertThrows<IllegalArgumentException> {
            ToneProfile(motivation = -1, philosophy = 5, wellbeing = 6)
        }
    }
}
