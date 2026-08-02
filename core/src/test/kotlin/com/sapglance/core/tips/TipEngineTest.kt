package com.sapglance.core.tips

import com.google.common.truth.Truth.assertThat
import com.sapglance.core.settings.PoolAmount
import com.sapglance.core.settings.PoolMix
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
        sleepLate = listOf("SL1", "SL2").map(::tip),
        sleepEarlyHours = listOf("SE1", "SE2").map(::tip),
    )

/** [testCatalog] with all three tone pools populated, for the tone-mix cases. */
private val tonedCatalog =
    testCatalog.copy(
        motivation = listOf("MOT1", "MOT2").map { toneTip(it, TipKind.MOTIVATION) },
        philosophy = listOf("PHI1", "PHI2").map { toneTip(it, TipKind.PHILOSOPHY) },
        wellbeing = listOf("WEL1", "WEL2").map { toneTip(it, TipKind.WELLBEING) },
    )

private const val SAMPLE_DRAWS = 2000

/**
 * The two mixes these tests name besides [PoolMix.DEFAULT], both differing from it only in the
 * practical amount — which is the one axis that changes how much tone there is at all.
 *
 * [TONE_ONLY_MIX] is where the old `VarietyLevel.PLAYFUL` cases landed, and it is not the same
 * claim: PLAYFUL meant 80% tone and this means 100%. Tests that asserted "overwhelming majority,
 * not the only option" were rewritten rather than retargeted, because that sentence is no longer
 * true and pretending otherwise would have been the easy way to keep them green.
 */
private val BALANCED_MIX = PoolMix.DEFAULT.copy(practical = PoolAmount.SOME)
private val TONE_ONLY_MIX = PoolMix.DEFAULT.copy(practical = PoolAmount.NONE)

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
    fun `late-evening pool is the sleep-late tips alone`() {
        assertPoolComposition(LocalTime.of(23, 30), testCatalog.sleepLate)
    }

    @Test
    fun `small-hours pool is the sleep-early tips alone`() {
        assertPoolComposition(LocalTime.of(2, 0), testCatalog.sleepEarlyHours)
    }

    /**
     * `general` is mixed into every other day part's practical tier and deliberately not into
     * these two: half of it ("stand up and stretch", "a quick 5-minute walk") is the opposite of
     * what 3am calls for. Pinned rather than left to the reading, because it is the one thing
     * still special about night selection and the easy "fix" is to make night look like the rest.
     */
    @Test
    fun `the general pool is not reachable during the sleep hours`() {
        val engine = TipEngine(testCatalog, Random(seed = 43))
        repeat(SAMPLE_DRAWS) {
            val tip = engine.messageFor(LocalTime.of(2, 0), recentTips = emptyList())
            assertThat(testCatalog.general).doesNotContain(tip)
        }
    }

    /**
     * The night windows were one fixed message each, exempt from anti-repeat because excluding
     * the only message there was would have left nothing to show. They are pools now, so the
     * exemption is gone and the rule that applies everywhere else applies here too.
     */
    @Test
    fun `the night pools honour anti-repeat like any other pool`() {
        val engine = TipEngine(testCatalog, FixedIndexRandom(0))
        val time = LocalTime.of(23, 30)

        val first = engine.messageFor(time, recentTips = emptyList())
        val second = engine.messageFor(time, recentTips = listOf(first.text))

        assertThat(second).isNotEqualTo(first)
        assertThat(testCatalog.sleepLate).contains(second)
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
    fun `the mix is a no-op while the tone pools are empty`() {
        // testCatalog has no tone content, so the mix must not change anything while that's
        // true - the tone tier should drop out entirely.
        assertPoolComposition(
            LocalTime.of(9, 0),
            testCatalog.general + testCatalog.morning,
            poolMix = TONE_ONLY_MIX,
        )
    }

    /**
     * The promise that replaced "a lean, never a filter". The old control's most tone-heavy
     * setting still let practical tips through 20% of the time, which is not an answer to a
     * reader who does not want them. Off has to mean off, or the control is a lie.
     */
    @Test
    fun `switching the practical pool off means none at all, not merely fewer`() {
        val toneCatalog = testCatalog.copy(philosophy = listOf(tip("P1"), tip("P2")))
        val engine = TipEngine(toneCatalog, Random(seed = 7))

        val toneShare = toneShareOverManyDraws(engine, TONE_ONLY_MIX)

        assertThat(toneShare).isEqualTo(1.0)
    }

    /** The same promise for a tone voice, which is the half a reader is more likely to use. */
    @Test
    fun `a tone voice switched off never appears, at any hour`() {
        val engine = TipEngine(tonedCatalog, Random(seed = 3))
        val withoutMotivation =
            PoolMix.DEFAULT.copy(motivation = PoolAmount.NONE, practical = PoolAmount.NONE)

        listOf(9, 14, 20, 23, 3).forEach { hour ->
            repeat(SAMPLE_DRAWS / 5) {
                val tip = engine.messageFor(LocalTime.of(hour, 0), emptyList(), poolMix = withoutMotivation)
                assertThat(tip.kind).isNotEqualTo(TipKind.MOTIVATION)
            }
        }
    }

    /**
     * Turning a voice *up* cannot conjure it into an hour the [ToneProfile] rules it out of: the
     * reader tunes the shape, and the hour decides what shape there is to tune. Multiplying the
     * profile rather than replacing it is what makes both zeroes stick.
     */
    @Test
    fun `PLENTY does not override the hour, so motivation stays out of the night`() {
        val engine = TipEngine(tonedCatalog, Random(seed = 4))
        val allMotivation =
            PoolMix.DEFAULT.copy(motivation = PoolAmount.PLENTY, practical = PoolAmount.NONE)

        repeat(SAMPLE_DRAWS) {
            val tip = engine.messageFor(LocalTime.of(3, 0), emptyList(), poolMix = allMotivation)
            assertThat(tip.kind).isNotEqualTo(TipKind.MOTIVATION)
        }
    }

    /**
     * The documented cost of a real "none": narrow the reachable set below the anti-repeat window
     * and the window cannot be honoured, because no ordering of a 3-tip pool avoids repeating
     * inside 100 draws. What must not happen is a crash or a tip from a pool the reader switched
     * off — it repeats, and it repeats *within what they asked for*.
     */
    @Test
    fun `a mix narrower than the anti-repeat window repeats rather than failing`() {
        val engine = TipEngine(tonedCatalog, Random(seed = 5))
        val philosophyOnly =
            PoolMix(
                practical = PoolAmount.NONE,
                philosophy = PoolAmount.SOME,
                motivation = PoolAmount.NONE,
                wellbeing = PoolAmount.NONE,
            )
        val everyPhilosophyTip = tonedCatalog.philosophy.map { it.text }

        val drawn =
            (1..SAMPLE_DRAWS).map {
                engine.messageFor(LocalTime.of(9, 0), everyPhilosophyTip, poolMix = philosophyOnly).text
            }

        assertThat(drawn).hasSize(SAMPLE_DRAWS)
        assertThat(everyPhilosophyTip).containsAtLeastElementsIn(drawn.toSet())
    }

    @Test
    fun `PoolMix DEFAULT still lets the tone pool through sometimes, not never`() {
        val toneCatalog = testCatalog.copy(philosophy = listOf(tip("P1"), tip("P2")))
        val engine = TipEngine(toneCatalog, Random(seed = 7))

        val toneShare = toneShareOverManyDraws(engine, PoolMix.DEFAULT)

        // 20% target (TONE_MINORITY_CHANCE_PERCENT) with the same generous tolerance.
        assertThat(toneShare).isAtLeast(0.05)
        assertThat(toneShare).isAtMost(0.35)
    }

    @Test
    fun `practical at SOME sits roughly at an even split`() {
        val toneCatalog = testCatalog.copy(philosophy = listOf(tip("P1"), tip("P2")))
        val engine = TipEngine(toneCatalog, Random(seed = 7))

        val toneShare = toneShareOverManyDraws(engine, BALANCED_MIX)

        // 50% target (TONE_BALANCED_CHANCE_PERCENT) with the same generous tolerance.
        assertThat(toneShare).isAtLeast(0.35)
        assertThat(toneShare).isAtMost(0.65)
    }

    /**
     * The last link in [TipEngine]'s fallback chain, and the one a reader can trigger: with the
     * practical pool switched off and a catalog whose tone pools are empty, honouring the mix
     * would mean showing nothing. Something must still come back.
     */
    @Test
    fun `falls back to the practical pool if the tone pools are empty, even with practical off`() {
        val engine = TipEngine(testCatalog, Random(seed = 1))
        repeat(50) {
            val tip =
                engine.messageFor(LocalTime.of(9, 0), recentTips = emptyList(), poolMix = TONE_ONLY_MIX)
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

        val tip = engine.messageFor(LocalTime.of(9, 0), recentTips = emptyList(), poolMix = PoolMix.DEFAULT)

        assertThat(tip.text).isEqualTo("P1")
    }

    @Test
    fun `prefers an unseen tip from the other group over repeating within the weighted group`() {
        // Single-tip tone pool, just shown - repeating it would be an unforced anti-repeat
        // violation while G1-G4/M1/M2 sit fresh and unused right next to it. Both tiers are live
        // here (practical at SOME), so the draw could legitimately land on either; with nothing
        // unseen left in the tone pool, the practical pool's fresh tips must win regardless of
        // the weighting - Random.Default is fine since the fix makes this branch deterministic
        // (no draw happens once one side has no fresh candidates).
        //
        // Deliberately not TONE_ONLY_MIX: with practical switched off, repeating P1 would be the
        // *correct* answer rather than a bug, because the reader asked for no practical tips and
        // that outranks the anti-repeat window. That case is its own test above.
        val catalog = testCatalog.copy(philosophy = listOf(tip("P1")))
        val engine = TipEngine(catalog, Random.Default)

        val tip = engine.messageFor(LocalTime.of(9, 0), recentTips = listOf("P1"), poolMix = BALANCED_MIX)

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
                engine.messageFor(LocalTime.of(9, 0), philosophyShown, poolMix = PoolMix.DEFAULT).text
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

        val tip = engine.messageFor(LocalTime.of(9, 0), recentTips = allShown, poolMix = PoolMix.DEFAULT)

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
            shareOverManyDraws(engine, LocalTime.of(9, 0), PoolMix.DEFAULT) { it in testCatalog.general }

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

        val drawn =
            (1..SAMPLE_DRAWS).map {
                engine.messageFor(time, emptyList(), poolMix = TONE_ONLY_MIX).text
            }

        assertThat(drawn).containsNoneIn(motivationTexts)
    }

    /**
     * Night leans harder towards tone than any waking hour does, and at [PoolMix.DEFAULT]
     * that lean is 50/50 rather than the daytime 80/20 — see [TipEngine.toneChancePercent] for
     * the two arguments that survive now that night is a real pool rather than a fixed message.
     * Both halves are pinned: a night that had quietly become all-tone would be as wrong as the
     * fixed-message version was.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("sleepHours")
    fun `sleep hours weigh the night pool and the quiet tone pools about evenly`(time: LocalTime) {
        val engine = TipEngine(tonedCatalog, Random(seed = 29))
        val nightPool =
            if (engine.dayPartFor(time) == DayPart.SLEEP_LATE) {
                tonedCatalog.sleepLate
            } else {
                tonedCatalog.sleepEarlyHours
            }

        val drawn =
            (1..SAMPLE_DRAWS).map {
                engine.messageFor(time, emptyList(), poolMix = PoolMix.DEFAULT).text
            }

        val quietTone = (tonedCatalog.philosophy + tonedCatalog.wellbeing).map { it.text }
        assertThat(drawn.any { it in quietTone }).isTrue()
        // 50% target (NIGHT_TONE_MINORITY_CHANCE_PERCENT) with the same generous tolerance the
        // other share assertions use.
        val nightShare = drawn.count { text -> nightPool.any { it.text == text } }.toDouble() / drawn.size
        assertThat(nightShare).isAtLeast(0.35)
        assertThat(nightShare).isAtMost(0.65)
    }

    /**
     * Two rules can each empty the board and they are not equal: anti-repeat is the promise
     * (FR5), the tone run limit is a preference about which voice comes next. Night is where
     * they actually collide, because it reaches one practical pool plus two tone pools rather
     * than the daytime's five groups, so a blocked voice can leave nothing unseen at all.
     *
     * Here the whole night pool, all of wellbeing, and two of three philosophy lines have just
     * been shown, and those two were the last draws, so the run limit wants philosophy gone for
     * this one. That would leave nothing at all. PHI3 is the only tip in the app that is both
     * unseen and drawable, so the engine must put the blocked voice back and pick it, whatever
     * the random source says.
     */
    @Test
    fun `the tone run limit yields before anything repeats`() {
        val catalog =
            tonedCatalog.copy(
                philosophy = listOf("PHI1", "PHI2", "PHI3").map { toneTip(it, TipKind.PHILOSOPHY) },
            )
        val everythingElseSeen =
            catalog.sleepEarlyHours.map { it.text } + catalog.wellbeing.map { it.text } + listOf("PHI1", "PHI2")

        repeat(50) { roll ->
            val engine = TipEngine(catalog, FixedIndexRandom(roll))
            val next = engine.messageFor(LocalTime.of(2, 0), everythingElseSeen, poolMix = PoolMix.DEFAULT)
            assertThat(next.text).isEqualTo("PHI3")
        }
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
            shareOverManyDraws(engine, LocalTime.of(9, 0), PoolMix.DEFAULT, justOutsideWindow) {
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
                    shareOverManyDraws(engine, LocalTime.of(9, 0), PoolMix.DEFAULT) { it.text == candidate.text }
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
        poolMix: PoolMix,
        draws: Int = SAMPLE_DRAWS,
    ): Double {
        val toneHits =
            (1..draws).count {
                val tip =
                    engine.messageFor(
                        LocalTime.of(9, 0),
                        recentTips = emptyList(),
                        poolMix = poolMix,
                    )
                tip.text.startsWith("P")
            }
        return toneHits.toDouble() / draws
    }

    private fun shareOfPool(
        engine: TipEngine,
        time: LocalTime,
        pool: List<Tip>,
    ): Double = shareOverManyDraws(engine, time, TONE_ONLY_MIX) { it in pool }

    private fun shareOverManyDraws(
        engine: TipEngine,
        time: LocalTime,
        poolMix: PoolMix,
        recentTips: List<String> = emptyList(),
        draws: Int = SAMPLE_DRAWS,
        matches: (Tip) -> Boolean,
    ): Double {
        val hits = (1..draws).count { matches(engine.messageFor(time, recentTips, poolMix = poolMix)) }
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
        poolMix: PoolMix = PoolMix.DEFAULT,
    ) {
        val engine = TipEngine(testCatalog, Random(seed = 11))
        val seen =
            (1..SAMPLE_DRAWS)
                .map { engine.messageFor(time, recentTips = emptyList(), poolMix = poolMix) }
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
                    poolMix = TONE_ONLY_MIX,
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
                        poolMix = TONE_ONLY_MIX,
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
                        poolMix = TONE_ONLY_MIX,
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
                        poolMix = PoolMix.DEFAULT,
                    ).kind
            }

        assertThat(kinds).contains(TipKind.PRACTICAL)
    }

    @Test
    fun `a blocked tone kind gives its share to the other voices rather than to practical`() {
        val engine = TipEngine(tonedCatalog, Random(seed = 15))

        // The run limit is about *which* tone comes next, not about how much tone there is:
        // blocking wellbeing must not quietly hand the tier's share back to the practical pool.
        //
        // BALANCED_MIX rather than TONE_ONLY_MIX, because a mix with practical switched off has
        // no practical share for the blocked voice's turn to leak into, which would make this
        // assertion true for the wrong reason.
        val toneShare =
            (1..SAMPLE_DRAWS).count {
                engine
                    .messageFor(
                        time = LocalTime.of(14, 0),
                        recentTips = listOf("WEL1", "WEL2"),
                        poolMix = BALANCED_MIX,
                    ).kind != TipKind.PRACTICAL
            } / SAMPLE_DRAWS.toDouble()

        // The same 50% target as the unblocked case, with the same generous tolerance: blocking
        // one voice moves the split between voices, not the split between tiers.
        assertThat(toneShare).isAtLeast(0.35)
        assertThat(toneShare).isAtMost(0.65)
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
                        poolMix = TONE_ONLY_MIX,
                    ).kind
            }

        assertThat(kinds.toSet())
            .containsAtLeast(TipKind.MOTIVATION, TipKind.PHILOSOPHY, TipKind.WELLBEING)
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
