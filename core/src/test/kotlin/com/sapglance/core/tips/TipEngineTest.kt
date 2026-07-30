package com.sapglance.core.tips

import com.google.common.truth.Truth.assertThat
import com.sapglance.core.settings.VarietyLevel
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalTime
import java.util.stream.Stream
import kotlin.random.Random

/** Deterministic [Random] stand-in: every `nextInt(until)` call returns the same fixed roll. */
private class FixedIndexRandom(private val index: Int) : Random() {
    override fun nextBits(bitCount: Int): Int = index

    override fun nextInt(until: Int): Int = index
}

private fun tip(text: String) =
    Tip(
        text = text,
        sources =
            listOf(
                TipSource("Test source A", "https://example.test/a"),
                TipSource("Test source B", "https://example.test/b"),
            ),
    )

private fun toneTip(
    text: String,
    kind: TipKind,
) = Tip(text = text, kind = kind)

private val testCatalog =
    TipCatalog(
        general = listOf("G1", "G2", "G3", "G4").map(::tip),
        morning = listOf("M1", "M2").map(::tip),
        afternoon = listOf("A1").map(::tip),
        evening = listOf("E1").map(::tip),
        sleepLate = tip("Sleep late fixed message"),
        sleepEarlyHours = tip("Sleep early-hours fixed message"),
    )

/** [testCatalog] with all three tone pools populated, for the tone-mix cases. */
private val tonedCatalog =
    testCatalog.copy(
        motivation = listOf("MOT1", "MOT2").map { toneTip(it, TipKind.MOTIVATION) },
        philosophy = listOf("PHI1", "PHI2").map { toneTip(it, TipKind.PHILOSOPHY) },
        wellbeing = listOf("WEL1", "WEL2").map { toneTip(it, TipKind.WELLBEING) },
    )

private const val SAMPLE_DRAWS = 2000

class TipEngineTest {
    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("dayPartBoundaries")
    fun `day part boundaries`(
        time: LocalTime,
        expected: DayPart,
    ) {
        val engine = TipEngine(testCatalog, Random.Default)
        assertThat(engine.dayPartFor(time)).isEqualTo(expected)
    }

    @Test
    fun `morning pool is general plus morning-specific tips`() {
        assertPoolComposition(LocalTime.of(9, 0), testCatalog.general + testCatalog.morning)
    }

    @Test
    fun `afternoon pool is general plus afternoon-specific tips`() {
        assertPoolComposition(LocalTime.of(14, 0), testCatalog.general + testCatalog.afternoon)
    }

    @Test
    fun `evening pool is general plus evening-specific tips`() {
        assertPoolComposition(LocalTime.of(20, 0), testCatalog.general + testCatalog.evening)
    }

    @Test
    fun `sleep late message is the fixed catalog message`() {
        val engine = TipEngine(testCatalog, FixedIndexRandom(0))
        val message = engine.messageFor(LocalTime.of(23, 30), recentTips = emptyList())
        assertThat(message).isEqualTo(testCatalog.sleepLate)
    }

    @Test
    fun `sleep early-hours message is the fixed catalog message`() {
        val engine = TipEngine(testCatalog, FixedIndexRandom(0))
        val message = engine.messageFor(LocalTime.of(2, 0), recentTips = emptyList())
        assertThat(message).isEqualTo(testCatalog.sleepEarlyHours)
    }

    @Test
    fun `sleep messages are exempt from anti-repeat`() {
        val engine = TipEngine(testCatalog, FixedIndexRandom(0))
        val time = LocalTime.of(23, 30)
        val first = engine.messageFor(time, recentTips = emptyList())
        val second = engine.messageFor(time, recentTips = listOf(first.text))
        assertThat(second).isEqualTo(first)
        assertThat(second).isEqualTo(testCatalog.sleepLate)
    }

    @Test
    fun `manual sleep-late request draws from the general pool instead of the fixed message`() {
        val engine = TipEngine(testCatalog, FixedIndexRandom(0))
        val message = engine.messageFor(LocalTime.of(23, 30), recentTips = emptyList(), manual = true)
        assertThat(message).isEqualTo(testCatalog.general[0])
    }

    @Test
    fun `manual sleep-early-hours request draws from the general pool instead of the fixed message`() {
        val engine = TipEngine(testCatalog, FixedIndexRandom(0))
        val message = engine.messageFor(LocalTime.of(2, 0), recentTips = emptyList(), manual = true)
        assertThat(message).isEqualTo(testCatalog.general[0])
    }

    @Test
    fun `manual sleep-hours request still honors anti-repeat`() {
        val engine = TipEngine(testCatalog, Random.Default)
        val recentTips = listOf("G1", "G2", "G3")
        val message = engine.messageFor(LocalTime.of(23, 30), recentTips, manual = true)
        assertThat(message.text).isEqualTo("G4")
    }

    @Test
    fun `excludes every tip in recentTips, not just the most recent one`() {
        val engine = TipEngine(testCatalog, Random.Default)
        // Morning pool is G1-G4, M1-M2 (6 members); excluding all but M2 must deterministically
        // return M2 regardless of the random source, proving the whole list is honored.
        val recentTips = listOf("G1", "G2", "G3", "G4", "M1")
        val tip = engine.messageFor(LocalTime.of(9, 0), recentTips)
        assertThat(tip.text).isEqualTo("M2")
    }

    @Test
    fun `never repeats a tip within a bounded recent window`() {
        val engine = TipEngine(testCatalog, Random(seed = 42))
        val windowSize = 4
        val window = ArrayDeque<String>()
        repeat(200) {
            val tip = engine.messageFor(LocalTime.of(9, 0), window.toList())
            assertThat(window).doesNotContain(tip.text)
            window.addLast(tip.text)
            if (window.size > windowSize) window.removeFirst()
        }
    }

    /**
     * The anti-repeat guarantee (FR5) is stated in terms of
     * [TipHistoryRepository.ANTI_REPEAT_WINDOW], not the longer span the history now remembers
     * for recency weighting. This pins the two apart: a tip sitting in history *beyond* the
     * window must be eligible again, or growing the memory would have silently widened the
     * guarantee and starved the pools.
     */
    @Test
    fun `anti-repeat reaches exactly the window, not the whole remembered history`() {
        val engine = TipEngine(testCatalog.copy(morning = emptyList()), Random(seed = 3))
        val justOutsideWindow = listOf("G1") + filler(TipHistoryRepository.ANTI_REPEAT_WINDOW)

        val drawn = (1..SAMPLE_DRAWS).map { engine.messageFor(LocalTime.of(9, 0), justOutsideWindow).text }

        assertThat(drawn).contains("G1")
        // ...while everything actually inside the window stays excluded.
        val insideWindow = listOf("G1", "G2") + filler(TipHistoryRepository.ANTI_REPEAT_WINDOW - 1)
        repeat(50) {
            assertThat(engine.messageFor(LocalTime.of(9, 0), insideWindow).text).isNotEqualTo("G2")
        }
    }

    @Test
    fun `falls back to a repeat when recentTips covers the entire pool`() {
        val singleTipCatalog =
            testCatalog.copy(general = listOf(tip("Only")), morning = emptyList())
        val engine = TipEngine(singleTipCatalog, FixedIndexRandom(0))

        val tip = engine.messageFor(LocalTime.of(9, 0), recentTips = listOf("Only"))

        assertThat(tip.text).isEqualTo("Only")
    }

    @Test
    fun `variety level is a no-op while the tone pools are empty`() {
        // testCatalog has no tone content, so varietyLevel must not change anything while
        // that's true - the tone tier should drop out entirely.
        assertPoolComposition(
            LocalTime.of(9, 0),
            testCatalog.general + testCatalog.morning,
            varietyLevel = VarietyLevel.PLAYFUL,
        )
    }

    @Test
    fun `PLAYFUL makes the tone pool the overwhelming majority, not the only option`() {
        val toneCatalog = testCatalog.copy(philosophy = listOf(tip("P1"), tip("P2")))
        val engine = TipEngine(toneCatalog, Random(seed = 7))

        val toneShare = toneShareOverManyDraws(engine, VarietyLevel.PLAYFUL)

        // 80% target (TONE_DOMINANT_CHANCE_PERCENT) with tolerance for statistical noise -
        // the point of this test is "clearly dominant," not "exactly 80%."
        assertThat(toneShare).isAtLeast(0.65)
        assertThat(toneShare).isAtMost(0.95)
    }

    @Test
    fun `PRACTICAL still lets the tone pool through sometimes, not never`() {
        val toneCatalog = testCatalog.copy(philosophy = listOf(tip("P1"), tip("P2")))
        val engine = TipEngine(toneCatalog, Random(seed = 7))

        val toneShare = toneShareOverManyDraws(engine, VarietyLevel.PRACTICAL)

        // 20% target (TONE_MINORITY_CHANCE_PERCENT) with the same generous tolerance.
        assertThat(toneShare).isAtLeast(0.05)
        assertThat(toneShare).isAtMost(0.35)
    }

    @Test
    fun `BALANCED sits roughly at an even split`() {
        val toneCatalog = testCatalog.copy(philosophy = listOf(tip("P1"), tip("P2")))
        val engine = TipEngine(toneCatalog, Random(seed = 7))

        val toneShare = toneShareOverManyDraws(engine, VarietyLevel.BALANCED)

        // 50% target (TONE_BALANCED_CHANCE_PERCENT) with the same generous tolerance.
        assertThat(toneShare).isAtLeast(0.35)
        assertThat(toneShare).isAtMost(0.65)
    }

    @Test
    fun `falls back to the practical pool if the tone pool is empty, even at PLAYFUL`() {
        val engine = TipEngine(testCatalog, Random(seed = 1))
        repeat(50) {
            val tip =
                engine.messageFor(LocalTime.of(9, 0), recentTips = emptyList(), varietyLevel = VarietyLevel.PLAYFUL)
            assertThat(testCatalog.general + testCatalog.morning).contains(tip)
        }
    }

    @Test
    fun `falls back to the tone pool if the practical pool is empty`() {
        val emptyGeneralCatalog =
            testCatalog.copy(
                general = emptyList(),
                morning = emptyList(),
                philosophy = listOf(tip("P1")),
            )
        val engine = TipEngine(emptyGeneralCatalog, FixedIndexRandom(0))

        val tip = engine.messageFor(LocalTime.of(9, 0), recentTips = emptyList(), varietyLevel = VarietyLevel.PRACTICAL)

        assertThat(tip.text).isEqualTo("P1")
    }

    @Test
    fun `prefers an unseen tip from the other group over repeating within the weighted group`() {
        // Single-tip tone pool, just shown - repeating it would be an unforced anti-repeat
        // violation while G1-G4/M1/M2 sit fresh and unused right next to it. PLAYFUL would
        // normally make the tone pool the 80% favorite, but with nothing unseen left in it, the
        // practical pool's fresh tips must win regardless of that weighting - Random.Default is
        // fine here since the fix makes this branch deterministic (no draw happens once one
        // side has no fresh candidates).
        val catalog = testCatalog.copy(philosophy = listOf(tip("P1")))
        val engine = TipEngine(catalog, Random.Default)

        val tip = engine.messageFor(LocalTime.of(9, 0), recentTips = listOf("P1"), varietyLevel = VarietyLevel.PLAYFUL)

        assertThat(tip.text).isNotEqualTo("P1")
        assertThat(catalog.general + catalog.morning).contains(tip)
    }

    /**
     * The group-level version of the case above, and the reason the fix had to become structural
     * rather than a hand-written empty check per group: with five groups instead of two there
     * are simply more places for "the draw landed on a group with nothing fresh" to come back.
     * Exhausting philosophy must shift its share to the other tone pools, not force a repeat and
     * not quietly shrink how much tone the user asked for.
     */
    @Test
    fun `an exhausted tone group yields to its siblings instead of repeating`() {
        val engine = TipEngine(tonedCatalog, Random(seed = 19))
        val philosophyShown = tonedCatalog.philosophy.map { it.text }

        val drawn =
            (1..SAMPLE_DRAWS).map {
                engine.messageFor(LocalTime.of(9, 0), philosophyShown, varietyLevel = VarietyLevel.PRACTICAL).text
            }

        assertThat(drawn).containsNoneIn(philosophyShown)
        val toneShare = drawn.count { it in tonedCatalog.tonePools.map { tone -> tone.text } }.toDouble() / drawn.size
        assertThat(toneShare).isAtLeast(0.10)
        assertThat(toneShare).isAtMost(0.30)
    }

    @Test
    fun `falls back to a weighted repeat when both groups are fully exhausted`() {
        val catalog = testCatalog.copy(philosophy = listOf(tip("P1")))
        val allShown = (catalog.general + catalog.morning).map { it.text } + listOf("P1")
        val engine = TipEngine(catalog, FixedIndexRandom(0))

        val tip = engine.messageFor(LocalTime.of(9, 0), recentTips = allShown, varietyLevel = VarietyLevel.PRACTICAL)

        // Nothing is actually unseen any more; this just shouldn't crash, and whatever comes
        // back must still be a real catalog tip.
        assertThat(catalog.general + catalog.morning + catalog.philosophy).contains(tip)
    }

    // ---- The day-part share of the practical tier -------------------------------------------

    /**
     * `general` (41 real tips) used to be concatenated with the day-part pool (~23) and drawn
     * from uniformly, so file sizes decided the ratio and roughly two thirds of practical draws
     * came from the most time-neutral content in the catalog. The split is now deliberate.
     */
    @Test
    fun `general and the day-part pool split the practical tier evenly, not by pool size`() {
        val engine = TipEngine(testCatalog, Random(seed = 5))

        val generalShare =
            shareOverManyDraws(engine, LocalTime.of(9, 0), VarietyLevel.PRACTICAL) { it in testCatalog.general }

        // 50% target (GENERAL_SHARE_PERCENT). Drawing uniformly from the concatenated pools
        // would give 4/6 = 67% here, which is outside this range.
        assertThat(generalShare).isAtLeast(0.40)
        assertThat(generalShare).isAtMost(0.60)
    }

    // ---- Time-of-day tone profiles ----------------------------------------------------------

    @Test
    fun `morning leans the tone share towards motivation`() {
        val engine = TipEngine(tonedCatalog, Random(seed = 13))

        val motivation = shareOfPool(engine, LocalTime.of(9, 0), tonedCatalog.motivation)
        val wellbeing = shareOfPool(engine, LocalTime.of(9, 0), tonedCatalog.wellbeing)

        // Morning profile is 5:3:2, at PLAYFUL's 80% tone share -> ~40% motivation, ~16% wellbeing.
        assertThat(motivation).isAtLeast(0.30)
        assertThat(motivation).isGreaterThan(wellbeing)
    }

    @Test
    fun `evening leans the tone share towards wellbeing and philosophy, away from motivation`() {
        val engine = TipEngine(tonedCatalog, Random(seed = 13))
        val evening = LocalTime.of(20, 0)

        val motivation = shareOfPool(engine, evening, tonedCatalog.motivation)
        val wellbeing = shareOfPool(engine, evening, tonedCatalog.wellbeing)

        // Evening profile is 1:4:5, at PLAYFUL's 80% tone share -> ~8% motivation, ~40% wellbeing.
        assertThat(wellbeing).isGreaterThan(motivation)
        assertThat(motivation).isAtMost(0.20)
        // Still a lean, not a filter: motivation is quieter in the evening, never switched off.
        assertThat(motivation).isGreaterThan(0.0)
    }

    /**
     * The one place the tone profile filters rather than leans, argued in [ToneProfile]: "Two
     * minutes. Set a timer. Go." at 3am is not a weaker version of good advice, it's the
     * opposite of what the hour calls for. Enforced here rather than left to intention.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("sleepHours")
    fun `never shows a motivational push during sleep hours`(time: LocalTime) {
        val engine = TipEngine(tonedCatalog, Random(seed = 23))
        val motivationTexts = tonedCatalog.motivation.map { it.text }

        val passive =
            (1..SAMPLE_DRAWS).map {
                engine.messageFor(time, emptyList(), varietyLevel = VarietyLevel.PLAYFUL).text
            }
        val manual =
            (1..SAMPLE_DRAWS).map {
                engine.messageFor(time, emptyList(), manual = true, varietyLevel = VarietyLevel.PLAYFUL).text
            }

        assertThat(passive).containsNoneIn(motivationTexts)
        assertThat(manual).containsNoneIn(motivationTexts)
    }

    /**
     * Night used to be the one unpersonalized corner of the app: a single fixed wind-down
     * message, every time, for ~7 hours. It should still be the most likely single thing to
     * see at 2am, but no longer the only one.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("sleepHours")
    fun `sleep hours mix quiet tone tips in alongside the fixed wind-down message`(time: LocalTime) {
        val engine = TipEngine(tonedCatalog, Random(seed = 29))
        val windDown =
            if (engine.dayPartFor(time) == DayPart.SLEEP_LATE) tonedCatalog.sleepLate else tonedCatalog.sleepEarlyHours

        val drawn =
            (1..SAMPLE_DRAWS).map {
                engine.messageFor(time, emptyList(), varietyLevel = VarietyLevel.PRACTICAL).text
            }

        val quietTone = (tonedCatalog.philosophy + tonedCatalog.wellbeing).map { it.text }
        assertThat(drawn).contains(windDown.text)
        assertThat(drawn.any { it in quietTone }).isTrue()
        // The wind-down message is exempt from anti-repeat, so it stays the single most common
        // thing at night even though it no longer has the hours to itself.
        val windDownShare = drawn.count { it == windDown.text }.toDouble() / drawn.size
        assertThat(windDownShare).isAtLeast(0.35)
    }

    // ---- Recency weighting ------------------------------------------------------------------

    /**
     * Uniform random says nothing about the *gaps between* draws: a tip could leave the
     * anti-repeat window and come straight back on the very next pick while another went months
     * unseen, and the returns are what a user notices. Here G1 sits exactly one draw outside the
     * window and G2-G4 have never been shown, so G1 must be strongly disfavored.
     */
    @Test
    fun `a tip that only just left the window is far less likely than one never shown`() {
        val catalog = testCatalog.copy(morning = emptyList())
        val justOutsideWindow = listOf("G1") + filler(TipHistoryRepository.ANTI_REPEAT_WINDOW)
        val engine = TipEngine(catalog, Random(seed = 31))

        val g1Share =
            shareOverManyDraws(engine, LocalTime.of(9, 0), VarietyLevel.PRACTICAL, justOutsideWindow) {
                it.text == "G1"
            }

        // Uniform over the four fresh tips would be 25%; the recency weighting puts G1 at
        // 1 part in 1 + 60 + 60 + 60.
        assertThat(g1Share).isAtMost(0.05)
    }

    /**
     * The counterpart to the case above: the recency weighting is a lean like every other
     * weighting here, so the least-overdue tip still has to be reachable. A fixed roll of 0
     * lands on it; a roll of 1 has already moved past it to the next candidate.
     */
    @Test
    fun `the least overdue tip is still reachable, not filtered out`() {
        val catalog = testCatalog.copy(morning = emptyList())
        val justOutsideWindow = listOf("G1") + filler(TipHistoryRepository.ANTI_REPEAT_WINDOW)

        val firstRoll = TipEngine(catalog, FixedIndexRandom(0)).messageFor(LocalTime.of(9, 0), justOutsideWindow)
        val secondRoll = TipEngine(catalog, FixedIndexRandom(1)).messageFor(LocalTime.of(9, 0), justOutsideWindow)

        assertThat(firstRoll.text).isEqualTo("G1")
        assertThat(secondRoll.text).isEqualTo("G2")
    }

    /**
     * With no history at all there is no recency signal to weigh, so selection has to degrade
     * cleanly to plain uniform random rather than doing something arbitrary - this is the state
     * every fresh install starts in.
     */
    @Test
    fun `an empty history weighs every tip equally`() {
        val engine = TipEngine(testCatalog.copy(morning = emptyList()), Random(seed = 37))

        val shares =
            testCatalog.general.associate { candidate ->
                candidate.text to
                    shareOverManyDraws(engine, LocalTime.of(9, 0), VarietyLevel.PRACTICAL) { it.text == candidate.text }
            }

        shares.values.forEach { share ->
            assertThat(share).isAtLeast(0.15)
            assertThat(share).isAtMost(0.35)
        }
    }

    // ---- Helpers ----------------------------------------------------------------------------

    /** Distinct texts that are in no pool, purely to push real tips further back in history. */
    private fun filler(count: Int) = (1..count).map { "Filler $it" }

    private fun toneShareOverManyDraws(
        engine: TipEngine,
        varietyLevel: VarietyLevel,
        draws: Int = SAMPLE_DRAWS,
    ): Double {
        val toneHits =
            (1..draws).count {
                val tip =
                    engine.messageFor(
                        LocalTime.of(9, 0),
                        recentTips = emptyList(),
                        varietyLevel = varietyLevel,
                    )
                tip.text.startsWith("P")
            }
        return toneHits.toDouble() / draws
    }

    private fun shareOfPool(
        engine: TipEngine,
        time: LocalTime,
        pool: List<Tip>,
    ): Double = shareOverManyDraws(engine, time, VarietyLevel.PLAYFUL) { it in pool }

    private fun shareOverManyDraws(
        engine: TipEngine,
        time: LocalTime,
        varietyLevel: VarietyLevel,
        recentTips: List<String> = emptyList(),
        draws: Int = SAMPLE_DRAWS,
        matches: (Tip) -> Boolean,
    ): Double {
        val hits = (1..draws).count { matches(engine.messageFor(time, recentTips, varietyLevel = varietyLevel)) }
        return hits.toDouble() / draws
    }

    /**
     * Weighted selection means a single fixed roll no longer enumerates a pool, so this samples
     * instead: with an empty history every tip carries the same recency weight, so enough draws
     * must cover the expected pool exactly and turn up nothing outside it.
     */
    private fun assertPoolComposition(
        time: LocalTime,
        expectedPool: List<Tip>,
        varietyLevel: VarietyLevel = VarietyLevel.PRACTICAL,
    ) {
        val engine = TipEngine(testCatalog, Random(seed = 11))
        val seen =
            (1..SAMPLE_DRAWS)
                .map { engine.messageFor(time, recentTips = emptyList(), varietyLevel = varietyLevel) }
                .toSet()
        assertThat(seen).isEqualTo(expectedPool.toSet())
    }

    @Test
    fun `a tone kind never appears three times in a row`() {
        val engine = TipEngine(tonedCatalog, Random(seed = 11))

        // Two wellbeing lines already shown back to back: the third draw must be anything else.
        // PLAYFUL so the tone tier wins most draws, which is exactly when the rut would show.
        repeat(SAMPLE_DRAWS) {
            val next =
                engine.messageFor(
                    time = LocalTime.of(14, 0),
                    recentTips = listOf("WEL1", "WEL2"),
                    varietyLevel = VarietyLevel.PLAYFUL,
                )
            assertThat(next.kind).isNotEqualTo(TipKind.WELLBEING)
        }
    }

    @Test
    fun `the run limit applies to each tone kind, not just wellbeing`() {
        val engine = TipEngine(tonedCatalog, Random(seed = 12))

        val runsByKind =
            listOf(
                TipKind.MOTIVATION to listOf("MOT1", "MOT2"),
                TipKind.PHILOSOPHY to listOf("PHI1", "PHI2"),
                TipKind.WELLBEING to listOf("WEL1", "WEL2"),
            )

        for ((kind, run) in runsByKind) {
            repeat(SAMPLE_DRAWS / 4) {
                val next =
                    engine.messageFor(
                        time = LocalTime.of(14, 0),
                        recentTips = run,
                        varietyLevel = VarietyLevel.PLAYFUL,
                    )
                assertThat(next.kind).isNotEqualTo(kind)
            }
        }
    }

    @Test
    fun `two of the same tone in a row is still allowed`() {
        val engine = TipEngine(tonedCatalog, Random(seed = 13))

        // One wellbeing line shown; a second is a pair, not a rut, so it must stay reachable.
        val kinds =
            (1..SAMPLE_DRAWS).map {
                engine
                    .messageFor(
                        time = LocalTime.of(14, 0),
                        recentTips = listOf("WEL1"),
                        varietyLevel = VarietyLevel.PLAYFUL,
                    ).kind
            }

        assertThat(kinds).contains(TipKind.WELLBEING)
    }

    @Test
    fun `the run limit does not apply to practical tips`() {
        val engine = TipEngine(tonedCatalog, Random(seed = 14))

        // Two practical tips back to back, at PRACTICAL. A third must remain reachable: the
        // user asked for practical, and capping it would force tone in against that setting.
        val kinds =
            (1..SAMPLE_DRAWS).map {
                engine
                    .messageFor(
                        time = LocalTime.of(14, 0),
                        recentTips = listOf("G1", "G2"),
                        varietyLevel = VarietyLevel.PRACTICAL,
                    ).kind
            }

        assertThat(kinds).contains(TipKind.PRACTICAL)
    }

    @Test
    fun `a blocked tone kind gives its share to the other voices rather than to practical`() {
        val engine = TipEngine(tonedCatalog, Random(seed = 15))

        // The run limit is about *which* tone comes next, not about how much tone there is:
        // blocking wellbeing must not quietly hand the tier's share back to the practical pool.
        val toneShare =
            (1..SAMPLE_DRAWS).count {
                engine
                    .messageFor(
                        time = LocalTime.of(14, 0),
                        recentTips = listOf("WEL1", "WEL2"),
                        varietyLevel = VarietyLevel.PLAYFUL,
                    ).kind != TipKind.PRACTICAL
            } / SAMPLE_DRAWS.toDouble()

        // Same 80% target as the unblocked PLAYFUL case, same generous tolerance.
        assertThat(toneShare).isAtLeast(0.65)
        assertThat(toneShare).isAtMost(0.95)
    }

    @Test
    fun `an unrecognised history entry does not block anything`() {
        val engine = TipEngine(tonedCatalog, Random(seed = 16))

        // A tip reworded or dropped since it was persisted resolves to no kind at all. That has
        // to degrade to "no opinion" rather than blocking a pool or throwing.
        val kinds =
            (1..SAMPLE_DRAWS).map {
                engine
                    .messageFor(
                        time = LocalTime.of(14, 0),
                        recentTips = listOf("a tip that no longer exists", "nor does this one"),
                        varietyLevel = VarietyLevel.PLAYFUL,
                    ).kind
            }

        assertThat(kinds.toSet())
            .containsAtLeast(TipKind.MOTIVATION, TipKind.PHILOSOPHY, TipKind.WELLBEING)
    }

    @Test
    fun `kindOf resolves every kind from text alone, and unknown text to no kind`() {
        val engine = TipEngine(tonedCatalog, Random(seed = 17))

        // What the widget's kind label reads on every repaint, from the plain text the history
        // stores. The null case is the same "reworded or dropped tip" as above: the card has to
        // go unlabelled rather than guess or throw.
        assertThat(engine.kindOf("G1")).isEqualTo(TipKind.PRACTICAL)
        assertThat(engine.kindOf("Sleep late fixed message")).isEqualTo(TipKind.PRACTICAL)
        assertThat(engine.kindOf("MOT1")).isEqualTo(TipKind.MOTIVATION)
        assertThat(engine.kindOf("PHI1")).isEqualTo(TipKind.PHILOSOPHY)
        assertThat(engine.kindOf("WEL1")).isEqualTo(TipKind.WELLBEING)
        assertThat(engine.kindOf("a tip that no longer exists")).isNull()
    }

    companion object {
        @JvmStatic
        fun dayPartBoundaries(): Stream<Arguments> =
            Stream.of(
                Arguments.of(LocalTime.of(5, 59), DayPart.SLEEP_EARLY_HOURS),
                Arguments.of(LocalTime.of(6, 0), DayPart.MORNING),
                Arguments.of(LocalTime.of(11, 59), DayPart.MORNING),
                Arguments.of(LocalTime.of(12, 0), DayPart.AFTERNOON),
                Arguments.of(LocalTime.of(17, 59), DayPart.AFTERNOON),
                Arguments.of(LocalTime.of(18, 0), DayPart.EVENING),
                Arguments.of(LocalTime.of(22, 59), DayPart.EVENING),
                Arguments.of(LocalTime.of(23, 0), DayPart.SLEEP_LATE),
                Arguments.of(LocalTime.of(0, 0), DayPart.SLEEP_EARLY_HOURS),
            )

        @JvmStatic
        fun sleepHours(): Stream<Arguments> =
            Stream.of(
                Arguments.of(LocalTime.of(23, 30)),
                Arguments.of(LocalTime.of(2, 0)),
            )
    }
}
