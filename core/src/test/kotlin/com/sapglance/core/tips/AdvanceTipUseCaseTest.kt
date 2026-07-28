package com.sapglance.core.tips

import com.google.common.truth.Truth.assertThat
import com.sapglance.core.settings.VarietyLevel
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
            sleepLate = tip("Sleep late"),
            sleepEarlyHours = tip("Sleep early"),
        )

    @Test
    fun `persists the picked tip so it appears in recentTips`() =
        runTest {
            val repository = FakeTipHistoryRepository()
            val advanceTip = AdvanceTipUseCase(TipEngine(catalog, Random(seed = 1)), repository)

            val tip = advanceTip(LocalTime.of(9, 0))

            assertThat(repository.recentTips.first()).contains(tip.text)
        }

    @Test
    fun `never repeats a tip already in the recent history`() =
        runTest {
            val repository = FakeTipHistoryRepository(initial = listOf("G1"))
            val advanceTip = AdvanceTipUseCase(TipEngine(catalog, Random(seed = 2)), repository)

            val tip = advanceTip(LocalTime.of(9, 0))

            assertThat(tip.text).isNotEqualTo("G1")
        }

    @Test
    fun `a manual advance during sleep hours does not return the fixed sleep message`() =
        runTest {
            val repository = FakeTipHistoryRepository()
            val advanceTip = AdvanceTipUseCase(TipEngine(catalog, Random(seed = 3)), repository)

            val tip = advanceTip(LocalTime.of(23, 30), manual = true)

            assertThat(tip.text).isNotEqualTo("Sleep late")
        }

    @Test
    fun `a non-manual advance during sleep hours returns the fixed sleep message`() =
        runTest {
            val repository = FakeTipHistoryRepository()
            val advanceTip = AdvanceTipUseCase(TipEngine(catalog, Random(seed = 3)), repository)

            val tip = advanceTip(LocalTime.of(23, 30))

            assertThat(tip.text).isEqualTo("Sleep late")
        }

    @Test
    fun `varietyLevel is forwarded to the engine's weighting`() =
        runTest {
            val mixedCatalog = catalog.copy(philosophy = listOf(tip("Tone tip")))
            val repository = FakeTipHistoryRepository()
            val advanceTip = AdvanceTipUseCase(TipEngine(mixedCatalog, GroupChoiceRandom()), repository)

            val tip = advanceTip(LocalTime.of(9, 0), varietyLevel = VarietyLevel.PLAYFUL)

            // GroupChoiceRandom's fixed 50 falls under the 80% dominant threshold but not the
            // 20% minority one — only a PLAYFUL that actually reached the engine picks the tone
            // pool here; a dropped/defaulted-PRACTICAL level would have produced G1, G2, or M1.
            assertThat(tip.text).isEqualTo("Tone tip")
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
                    sleepLate = tip("Sleep late"),
                    sleepEarlyHours = tip("Sleep early"),
                )
            val repository = RaceProneTipHistoryRepository()
            val advanceTip = AdvanceTipUseCase(TipEngine(concurrentPool, Random(seed = 7)), repository)

            val results =
                List(10) { async { advanceTip(LocalTime.of(9, 0)) } }.awaitAll()

            val pickedTexts = results.map { it.text }
            assertThat(pickedTexts).hasSize(10)
            assertThat(pickedTexts.toSet()).hasSize(10)
            assertThat(repository.recentTips.first()).hasSize(10)
        }
}
