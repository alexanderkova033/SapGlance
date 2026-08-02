package com.sapglance.core.tips

import com.google.common.truth.Truth.assertThat
import com.sapglance.core.settings.PoolAmount
import com.sapglance.core.settings.PoolMix
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Test
import java.time.LocalTime
import kotlin.random.Random

private class FakeTipHistoryRepository(initial: List<String> = emptyList()) : TipHistoryRepository {
    private val state = MutableStateFlow(initial)
    override val recentTips: Flow<List<String>> = state

    override suspend fun recordTip(tip: String) {
        state.value = (state.value + tip).takeLast(TipHistoryRepository.MAX_RECENT_TIPS)
    }
}

/** Suspends between reading and writing so concurrent callers actually get a chance to
 * interleave — a repository whose `recordTip` never suspends wouldn't exercise the race
 * [AdvanceTipUseCase]'s mutex is meant to close. */
private class RaceProneTipHistoryRepository(initial: List<String> = emptyList()) : TipHistoryRepository {
    private val state = MutableStateFlow(initial)
    override val recentTips: Flow<List<String>> = state

    override suspend fun recordTip(tip: String) {
        val current = state.value
        yield()
        state.value = (current + tip).takeLast(TipHistoryRepository.MAX_RECENT_TIPS)
    }
}

/** Returns 50 for the group-selection roll ([TipEngine]'s `nextInt(100)`) and 0 for every other
 * call (e.g. the subsequent candidate-index pick) — 50 sits strictly between the dominant (80)
 * and minority (20) tone-chance thresholds, so it deterministically reveals which one the
 * engine actually used, rather than needing a statistical test to prove `varietyLevel` reached
 * [TipEngine.messageFor] from [AdvanceTipUseCase.invoke] at all. */
private class GroupChoiceRandom : Random() {
    override fun nextBits(bitCount: Int): Int = 0

    override fun nextInt(until: Int): Int = if (until == 100) 50 else 0
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

class AdvanceTipUseCaseTest {
    private val catalog =
        TipCatalog(
            general = listOf("G1", "G2").map(::tip),
            morning = listOf("M1").map(::tip),
            afternoon = emptyList(),
            evening = emptyList(),
            sleepLate = listOf("Sleep late").map(::tip),
            sleepEarlyHours = listOf("Sleep early").map(::tip),
        )

    @Test
    fun `persists the picked tip so it appears in recentTips`() =
        runTest {
            val repository = FakeTipHistoryRepository()
            val engine = TipEngine(catalog, Random(seed = 1))
            val advanceTip = AdvanceTipUseCase(repository)

            val tip = advanceTip(engine, LocalTime.of(9, 0))

            assertThat(repository.recentTips.first()).contains(tip.text)
        }

    @Test
    fun `never repeats a tip already in the recent history`() =
        runTest {
            val repository = FakeTipHistoryRepository(initial = listOf("G1"))
            val engine = TipEngine(catalog, Random(seed = 2))
            val advanceTip = AdvanceTipUseCase(repository)

            val tip = advanceTip(engine, LocalTime.of(9, 0))

            assertThat(tip.text).isNotEqualTo("G1")
        }

    /**
     * The sleep hours used to need their own pair of cases here, because a `manual` advance was
     * routed around the fixed wind-down message and a passive one wasn't. Both windows are
     * ordinary pools now, so there is one behaviour to pin: an advance at 23:30 draws that hour's
     * pool, and it is recorded like any other, which is what makes the *next* one different.
     */
    @Test
    fun `an advance during sleep hours draws that window's pool and records it`() =
        runTest {
            val repository = FakeTipHistoryRepository()
            val engine = TipEngine(catalog, Random(seed = 3))
            val advanceTip = AdvanceTipUseCase(repository)

            val tip = advanceTip(engine, LocalTime.of(23, 30))

            assertThat(tip.text).isEqualTo("Sleep late")
            assertThat(repository.recentTips.first()).containsExactly("Sleep late")
        }

    @Test
    fun `poolMix is forwarded to the engine's weighting`() =
        runTest {
            val mixedCatalog = catalog.copy(philosophy = listOf(tip("Tone tip")))
            val repository = FakeTipHistoryRepository()
            val engine = TipEngine(mixedCatalog, GroupChoiceRandom())
            val advanceTip = AdvanceTipUseCase(repository)

            val tip =
                advanceTip(
                    engine,
                    LocalTime.of(9, 0),
                    poolMix = PoolMix.DEFAULT.copy(practical = PoolAmount.NONE),
                )

            // With practical switched off the tone tier is the only one left, so only a mix that
            // actually reached the engine picks the tone pool here; a dropped or defaulted mix
            // would have produced G1, G2, or M1.
            assertThat(tip.text).isEqualTo("Tone tip")
        }

    /**
     * The reason [AdvanceTipUseCase] takes an engine per call rather than holding one. The
     * language setting means there is an engine per language, and the tempting refactor is a use
     * case per language — which is a *mutex* per language, so two callers on different languages
     * would serialize against different locks and could both read the same history snapshot.
     *
     * Two engines over two disjoint catalogs stand in for two languages. Ten concurrent calls
     * split between them must still produce ten distinct tips: the pools do not overlap, so a
     * duplicate can only come from two calls that raced the same history read.
     */
    @Test
    fun `concurrent advances across two languages still serialize against one lock`() =
        runTest {
            fun poolOf(prefix: String) =
                TipCatalog(
                    general = (1..5).map { tip("$prefix$it") },
                    morning = emptyList(),
                    afternoon = emptyList(),
                    evening = emptyList(),
                    sleepLate = listOf(tip("$prefix sleep late")),
                    sleepEarlyHours = listOf(tip("$prefix sleep early")),
                )
            val repository = RaceProneTipHistoryRepository()
            val advanceTip = AdvanceTipUseCase(repository)
            val english = TipEngine(poolOf("EN"), Random(seed = 11))
            val russian = TipEngine(poolOf("RU"), Random(seed = 12))

            val results =
                List(10) { index ->
                    async { advanceTip(if (index % 2 == 0) english else russian, LocalTime.of(9, 0)) }
                }.awaitAll()

            val pickedTexts = results.map { it.text }
            assertThat(pickedTexts.toSet()).hasSize(10)
            assertThat(repository.recentTips.first()).hasSize(10)
        }

    @Test
    fun `concurrent advances from the same instance never select the same tip twice, up to pool size`() =
        runTest {
            // A pool exactly as large as the number of concurrent callers: if every call is
            // correctly serialized against the shared history, the anti-repeat rule forces all
            // ten calls to collectively pick all ten tips, one each, with zero duplicates. Any
            // unserialized read-select-persist race would let two callers see the same stale
            // history and pick the same tip, producing a duplicate.
            val concurrentPool =
                TipCatalog(
                    general = (1..10).map { tip("G$it") },
                    morning = emptyList(),
                    afternoon = emptyList(),
                    evening = emptyList(),
                    sleepLate = listOf("Sleep late").map(::tip),
                    sleepEarlyHours = listOf("Sleep early").map(::tip),
                )
            val repository = RaceProneTipHistoryRepository()
            val engine = TipEngine(concurrentPool, Random(seed = 7))
            val advanceTip = AdvanceTipUseCase(repository)

            val results =
                List(10) { async { advanceTip(engine, LocalTime.of(9, 0)) } }.awaitAll()

            val pickedTexts = results.map { it.text }
            assertThat(pickedTexts).hasSize(10)
            assertThat(pickedTexts.toSet()).hasSize(10)
            assertThat(repository.recentTips.first()).hasSize(10)
        }
}
