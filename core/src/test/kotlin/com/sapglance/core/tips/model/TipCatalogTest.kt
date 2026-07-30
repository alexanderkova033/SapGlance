package com.sapglance.core.tips.model

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.sapglance.core.settings.model.VarietyLevel
import com.sapglance.core.tips.port.TipHistoryRepository
import com.sapglance.core.tips.usecase.TipEngine
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalTime
import kotlin.random.Random

class TipCatalogTest {
    private val catalog = TipCatalog.loadDefault()

    @Test
    fun `all pools are non-empty`() {
        assertThat(catalog.general).isNotEmpty()
        assertThat(catalog.morning).isNotEmpty()
        assertThat(catalog.afternoon).isNotEmpty()
        assertThat(catalog.evening).isNotEmpty()
        assertThat(catalog.sleepLate).isNotEmpty()
        assertThat(catalog.sleepEarlyHours).isNotEmpty()
        assertThat(catalog.motivation).isNotEmpty()
        assertThat(catalog.philosophy).isNotEmpty()
        assertThat(catalog.wellbeing).isNotEmpty()
    }

    @Test
    fun `pools contain no blank entries`() {
        allTips().forEach {
            assertThat(it.text.isNotBlank()).isTrue()
        }
    }

    @Test
    fun `pools contain no duplicate entries`() {
        listOf(
            catalog.general,
            catalog.morning,
            catalog.afternoon,
            catalog.evening,
            catalog.sleepLate,
            catalog.sleepEarlyHours,
            catalog.motivation,
            catalog.philosophy,
            catalog.wellbeing,
        ).forEach { pool ->
            assertThat(pool.map { it.text }.toSet()).hasSize(pool.size)
        }
    }

    /**
     * A tone tip repeating a practical tip's text would break anti-repeat in a confusing way
     * (the same string in two pools), and would also mean the same sentence is presented as
     * evidence-backed in one place and as an unsourced reflection in another.
     */
    @Test
    fun `no tip text appears in more than one pool`() {
        val texts = allTips().map { it.text }
        assertThat(texts.toSet()).hasSize(texts.size)
    }

    /**
     * The two night windows reach nothing but their own pool ([TipEngine.practicalGroups]), so
     * each one has to be deep enough to be a rotation on its own — where every other day part
     * has `general` underneath it, these have nothing. The floor is low on purpose: what makes
     * the promise keepable at night is pinned by the anti-repeat test at the bottom of this file,
     * and this only catches a pool trimmed back towards the fixed message it used to be.
     */
    @Test
    fun `each night window is a real pool, not a single message`() {
        assertThat(catalog.sleepLate.size).isAtLeast(MIN_NIGHT_POOL)
        assertThat(catalog.sleepEarlyHours.size).isAtLeast(MIN_NIGHT_POOL)
    }

    @Test
    fun `each pool is tagged with the kind it belongs to`() {
        (
            catalog.general + catalog.morning + catalog.afternoon + catalog.evening +
                catalog.sleepLate + catalog.sleepEarlyHours
        ).forEach {
            assertThat(it.kind).isEqualTo(TipKind.PRACTICAL)
        }
        catalog.motivation.forEach { assertThat(it.kind).isEqualTo(TipKind.MOTIVATION) }
        catalog.philosophy.forEach { assertThat(it.kind).isEqualTo(TipKind.PHILOSOPHY) }
        catalog.wellbeing.forEach { assertThat(it.kind).isEqualTo(TipKind.WELLBEING) }
    }

    /**
     * The citation bar is per-kind, not global: only [TipKind.PRACTICAL] tips make empirical
     * claims, so only they owe the reader evidence. Applying the old blanket rule to a
     * motivational line would just force an invented or stretched citation, which is the
     * dishonesty the citation model exists to prevent.
     */
    @Test
    fun `every practical tip carries at least the minimum number of real citations`() {
        allTips().filter { it.kind.requiresCitation }.forEach { tip ->
            assertThat(tip.sources.size).isAtLeast(Tip.MIN_SOURCES)
        }
    }

    @Test
    fun `tips that make no empirical claim carry no citation implying otherwise`() {
        (catalog.motivation + catalog.wellbeing).forEach { tip ->
            assertThat(tip.sources).isEmpty()
        }
    }

    /**
     * A quoted philosophy tip cites exactly the text it is quoted from, so the attribution can
     * be checked rather than taken on trust; an original reflection cites nothing. What must
     * never happen is a philosophy tip carrying research citations, which would present a
     * reflection as an empirical finding.
     */
    @Test
    fun `philosophy tips are either unsourced reflections or attributed quotations`() {
        assertThat(catalog.philosophy.any { it.sources.isEmpty() }).isTrue()
        assertThat(catalog.philosophy.any { it.sources.isNotEmpty() }).isTrue()
    }

    /**
     * The philosophy pool is meant to be actual philosophers, not this project's own writing in
     * a philosophical register — a handful of original reflections is fine, a pool that has
     * drifted mostly-original is not. Attribution is the proxy: a quotation carries the
     * public-domain text it came from, an original carries nothing.
     */
    @Test
    fun `most philosophy tips are real attributed quotations, not original writing`() {
        val quoted = catalog.philosophy.count { it.sources.isNotEmpty() }
        assertThat(quoted).isGreaterThan(catalog.philosophy.size / 2)
    }

    @Test
    fun `every citation present is well formed`() {
        allTips().forEach { tip ->
            tip.sources.forEach { source ->
                assertThat(source.label.isNotBlank()).isTrue()
                assertThat(source.url).startsWith("https://")
            }
        }
    }

    /**
     * Citing the same URL twice would satisfy the count above while still being a single piece
     * of evidence, which is exactly what the multi-source citation model exists to rule out.
     * Sources repeating *across* tips is fine and deliberate; repeating within one is not.
     */
    @Test
    fun `a tip's own sources are distinct from each other`() {
        allTips().forEach { tip ->
            assertThat(tip.sources.map { it.url }.toSet()).hasSize(tip.sources.size)
        }
    }

    @Test
    fun `no tip text contains an em dash`() {
        allTips().forEach { tip ->
            assertThat(tip.text).doesNotContain("—")
        }
    }

    /**
     * The bundled content must be able to *afford* the anti-repeat promise
     * ([TipHistoryRepository.ANTI_REPEAT_WINDOW]), which is a fact about the catalog rather than
     * about [TipEngine] — hence a catalog test. The engine's own tests prove the rule is applied
     * correctly against small synthetic pools; only this one proves the real pools are deep
     * enough for the number actually shipped, so a content trim (or a raised window) that makes
     * the promise unkeepable fails here instead of on someone's home screen.
     *
     * The stress case is one day part, not the whole catalog. A user who only ever sees the
     * widget in the morning reaches `general` + `morning` for practical draws, and at
     * [VarietyLevel.PRACTICAL] roughly 80% of draws want to come from exactly that set — far
     * smaller than the catalog total that a naive "is 100 < 280?" check would look at.
     *
     * The two sleep windows are narrower still, and are the real reason this test exists in its
     * current form: night reaches one practical pool plus philosophy and wellbeing, with
     * motivation weighted to zero and `general` deliberately kept out. That is the smallest
     * reachable set in the app, and the only one where a content trim in a pool that looks
     * unrelated to sleep — wellbeing, say — could break the promise at 3am.
     */
    @ParameterizedTest
    @MethodSource("singleDayPartHours")
    fun `the real catalog sustains the anti-repeat window in every single day part`(hour: Int) {
        VarietyLevel.entries.forEach { variety ->
            val engine = TipEngine(catalog, Random(seed = 20260729))
            val history = ArrayDeque<String>()
            val time = LocalTime.of(hour, 0)

            repeat(DRAWS) {
                val tip = engine.messageFor(time, history.toList(), varietyLevel = variety)
                assertWithMessage(
                    "at %s, %s: %s repeated within the %s-draw window",
                    time,
                    variety,
                    tip.text,
                    TipHistoryRepository.ANTI_REPEAT_WINDOW,
                ).that(history.toList().takeLast(TipHistoryRepository.ANTI_REPEAT_WINDOW))
                    .doesNotContain(tip.text)

                history.addLast(tip.text)
                while (history.size > TipHistoryRepository.MAX_RECENT_TIPS) history.removeFirst()
            }
        }
    }

    private fun allTips(): List<Tip> =
        catalog.general + catalog.morning + catalog.afternoon + catalog.evening +
            catalog.sleepLate + catalog.sleepEarlyHours + catalog.tonePools

    private companion object {
        /**
         * Long enough that the pools have to recycle many times over, so a window the content
         * cannot sustain shows up as a repeat rather than merely being untested.
         */
        const val DRAWS = 2000

        /** Enough to be a rotation rather than a message with variants. */
        const val MIN_NIGHT_POOL = 8

        /**
         * One hour per day part, now including both sleep windows. They used to be excluded,
         * because their practical side was a single fixed message exempt from anti-repeat and
         * asserting no-repeat there would have been asserting against an intended exception.
         * They are ordinary pools now, and they are also the *narrowest* hours in the app: night
         * reaches its own pool plus philosophy and wellbeing, with motivation weighted out and
         * `general` deliberately not mixed in, so it is the hour where the window is hardest to
         * cover and the one most worth testing.
         */
        @JvmStatic
        fun singleDayPartHours() = listOf(9, 14, 20, 23, 2)
    }
}
