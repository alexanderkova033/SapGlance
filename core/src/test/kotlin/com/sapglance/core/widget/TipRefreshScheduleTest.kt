package com.sapglance.core.widget

import com.google.common.truth.Truth.assertThat
import com.sapglance.core.tips.DayPart
import com.sapglance.core.tips.TipEngine
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate
import java.time.LocalDateTime

class TipRefreshScheduleTest {
    @ParameterizedTest(name = "{0}:{1} belongs to the {2}:00 window")
    @CsvSource(
        // Each switch hour, and the instant before the next one.
        "6, 0, 6",
        "11, 59, 6",
        "12, 0, 12",
        "17, 59, 12",
        "18, 0, 18",
        "22, 59, 18",
        "23, 59, 18",
    )
    fun `every hour of the day maps to the switch it is past`(
        hour: Int,
        minute: Int,
        expectedHour: Int,
    ) {
        val slot = currentSlotStart(LocalDateTime.of(2026, 7, 31, hour, minute))

        assertThat(slot).isEqualTo(LocalDateTime.of(2026, 7, 31, expectedHour, 0))
    }

    /**
     * The small hours belong to *yesterday's* last switch, which is what keeps the evening tip on
     * screen overnight rather than inventing a fourth window nobody asked for. Getting this wrong
     * the other way would advance the tip at midnight every night.
     */
    @ParameterizedTest(name = "{0}:{1} still belongs to yesterday evening")
    @CsvSource("0, 0", "3, 0", "5, 59")
    fun `before the first switch of the day, the slot is yesterday's last`(
        hour: Int,
        minute: Int,
    ) {
        val slot = currentSlotStart(LocalDateTime.of(2026, 7, 31, hour, minute))

        assertThat(slot).isEqualTo(LocalDateTime.of(2026, 7, 30, 18, 0))
    }

    /**
     * The property the worker actually relies on: walking a whole day a minute at a time yields
     * one slot per switch (plus the previous evening's, before 06:00) and never goes backwards.
     * Anything else and the tip either sticks or flickers.
     */
    @Test
    fun `a day contains exactly three switches and the slot never moves backwards`() {
        val start = LocalDate.of(2026, 7, 31).atStartOfDay()

        val slots = (0 until 24 * 60).map { currentSlotStart(start.plusMinutes(it.toLong())) }

        assertThat(slots.distinct()).hasSize(TIP_SWITCH_HOURS.size + 1)
        assertThat(slots).isInOrder()
        assertThat(slots.distinct().drop(1).map { it.hour }).isEqualTo(TIP_SWITCH_HOURS)
    }

    @Test
    fun `the slot is stable across every instant inside a window`() {
        val noon = LocalDateTime.of(2026, 7, 31, 12, 0)

        val inside =
            listOf(noon, noon.plusMinutes(1), noon.plusHours(3), noon.plusHours(5).plusMinutes(59))

        assertThat(inside.map { currentSlotStart(it) }.distinct()).hasSize(1)
    }

    @Test
    fun `slots on different days differ, so the same hour tomorrow still advances`() {
        val today = currentSlotStart(LocalDateTime.of(2026, 7, 31, 12, 30))
        val tomorrow = currentSlotStart(LocalDateTime.of(2026, 8, 1, 12, 30))

        assertThat(today).isNotEqualTo(tomorrow)
    }

    /**
     * The reason these three hours and not three others. A switch hands over a tip drawn for
     * whatever [DayPart] the engine reports at that moment, so if the two ever drift apart the
     * widget goes back to showing morning tips all afternoon — the bug the whole time-of-day
     * design exists to prevent.
     */
    @Test
    fun `each switch hour opens a different day part`() {
        val engine = TipEngine()

        val dayParts =
            TIP_SWITCH_HOURS.map { engine.dayPartFor(LocalDateTime.of(2026, 7, 31, it, 0).toLocalTime()) }

        assertThat(dayParts).containsExactly(DayPart.MORNING, DayPart.AFTERNOON, DayPart.EVENING).inOrder()
    }

    /**
     * Pins the known cost of stopping at 18:00 rather than leaving it to be discovered: the two
     * night pools are unreachable on the schedule, so only a tap gets you one. If someone adds
     * `23` to [TIP_SWITCH_HOURS], this is the test that should fail and then be deleted.
     */
    @Test
    fun `no switch opens a night day part, which is why the night pools are tap-only`() {
        val engine = TipEngine()

        val dayParts =
            TIP_SWITCH_HOURS.map { engine.dayPartFor(LocalDateTime.of(2026, 7, 31, it, 0).toLocalTime()) }

        assertThat(dayParts).containsNoneOf(DayPart.SLEEP_LATE, DayPart.SLEEP_EARLY_HOURS)
    }
}
