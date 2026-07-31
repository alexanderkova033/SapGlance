package com.sapglance.core.tips

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.sapglance.core.settings.VarietyLevel
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalTime
import kotlin.random.Random

/**
 * Every structural claim here is parameterized over [TipCatalog.SUPPORTED_LANGUAGES] rather than
 * asserted about English, because none of them is a fact about English. A translation that loses
 * a line, repeats one, drops a citation or smuggles in an em dash is exactly as broken as an
 * English pool that does, and it is *more* likely: the translator is working line-by-line
 * against a file they cannot read the sources of.
 *
 * The alternative was a second test class covering the translations, which was rejected on the
 * one thing that matters here — a new invariant added to a second class is a new invariant the
 * translations are checked for and English is not, or the reverse, and nothing would say which.
 */
class TipCatalogTest {
    @ParameterizedTest
    @MethodSource("languages")
    fun `all pools are non-empty`(language: String) {
        val catalog = catalogFor(language)
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

    @ParameterizedTest
    @MethodSource("languages")
    fun `pools contain no blank entries`(language: String) {
        allTips(catalogFor(language)).forEach {
            assertThat(it.text.isNotBlank()).isTrue()
        }
    }

    @ParameterizedTest
    @MethodSource("languages")
    fun `pools contain no duplicate entries`(language: String) {
        pools(catalogFor(language)).forEach { pool ->
            assertThat(pool.map { it.text }.toSet()).hasSize(pool.size)
        }
    }

    /**
     * A tone tip repeating a practical tip's text would break anti-repeat in a confusing way
     * (the same string in two pools), and would also mean the same sentence is presented as
     * evidence-backed in one place and as an unsourced reflection in another.
     */
    @ParameterizedTest
    @MethodSource("languages")
    fun `no tip text appears in more than one pool`(language: String) {
        val texts = allTips(catalogFor(language)).map { it.text }
        assertThat(texts.toSet()).hasSize(texts.size)
    }

    /**
     * The two night windows reach nothing but their own pool ([TipEngine.practicalGroups]), so
     * each one has to be deep enough to be a rotation on its own — where every other day part
     * has `general` underneath it, these have nothing. The floor is low on purpose: what makes
     * the promise keepable at night is pinned by the anti-repeat test at the bottom of this file,
     * and this only catches a pool trimmed back towards the fixed message it used to be.
     */
    @ParameterizedTest
    @MethodSource("languages")
    fun `each night window is a real pool, not a single message`(language: String) {
        val catalog = catalogFor(language)
        assertThat(catalog.sleepLate.size).isAtLeast(MIN_NIGHT_POOL)
        assertThat(catalog.sleepEarlyHours.size).isAtLeast(MIN_NIGHT_POOL)
    }

    @ParameterizedTest
    @MethodSource("languages")
    fun `each pool is tagged with the kind it belongs to`(language: String) {
        val catalog = catalogFor(language)
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
    @ParameterizedTest
    @MethodSource("languages")
    fun `every practical tip carries at least the minimum number of real citations`(language: String) {
        allTips(catalogFor(language)).filter { it.kind.requiresCitation }.forEach { tip ->
            assertThat(tip.sources.size).isAtLeast(Tip.MIN_SOURCES)
        }
    }

    /**
     * Motivation is original writing end to end, so anything at all in [Tip.sources] there is a
     * mistake. Wellbeing is the looser case since the jokes group landed: a line lifted from
     * public-domain humour says whose it is, exactly as a quoted philosophy line does. What
     * neither may do is carry the *plural* citations that mean "this is an evidence-backed
     * claim", which is the thing the per-kind split exists to keep honest.
     */
    @ParameterizedTest
    @MethodSource("languages")
    fun `tips that make no empirical claim carry no citation implying otherwise`(language: String) {
        val catalog = catalogFor(language)
        catalog.motivation.forEach { tip ->
            assertThat(tip.sources).isEmpty()
        }
        catalog.wellbeing.forEach { tip ->
            assertThat(tip.sources.size).isAtMost(1)
        }
    }

    /** The jokes are sourced rather than written, so at least some of them must say from where. */
    @ParameterizedTest
    @MethodSource("languages")
    fun `the wellbeing pool carries both original lines and attributed ones`(language: String) {
        val catalog = catalogFor(language)
        assertThat(catalog.wellbeing.any { it.sources.isEmpty() }).isTrue()
        assertThat(catalog.wellbeing.any { it.sources.size == 1 }).isTrue()
    }

    /**
     * A quoted philosophy tip cites exactly the text it is quoted from, so the attribution can
     * be checked rather than taken on trust; an original reflection cites nothing. What must
     * never happen is a philosophy tip carrying research citations, which would present a
     * reflection as an empirical finding.
     */
    @ParameterizedTest
    @MethodSource("languages")
    fun `philosophy tips are either unsourced reflections or attributed quotations`(language: String) {
        val catalog = catalogFor(language)
        assertThat(catalog.philosophy.any { it.sources.isEmpty() }).isTrue()
        assertThat(catalog.philosophy.any { it.sources.isNotEmpty() }).isTrue()
    }

    /**
     * The philosophy pool is meant to be actual philosophers, not this project's own writing in
     * a philosophical register — a handful of original reflections is fine, a pool that has
     * drifted mostly-original is not. Attribution is the proxy: a quotation carries the
     * public-domain text it came from, an original carries nothing.
     */
    @ParameterizedTest
    @MethodSource("languages")
    fun `most philosophy tips are real attributed quotations, not original writing`(language: String) {
        val catalog = catalogFor(language)
        val quoted = catalog.philosophy.count { it.sources.isNotEmpty() }
        assertThat(quoted).isGreaterThan(catalog.philosophy.size / 2)
    }

    @ParameterizedTest
    @MethodSource("languages")
    fun `every citation present is well formed`(language: String) {
        allTips(catalogFor(language)).forEach { tip ->
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
    @ParameterizedTest
    @MethodSource("languages")
    fun `a tip's own sources are distinct from each other`(language: String) {
        allTips(catalogFor(language)).forEach { tip ->
            assertThat(tip.sources.map { it.url }.toSet()).hasSize(tip.sources.size)
        }
    }

    @ParameterizedTest
    @MethodSource("languages")
    fun `no tip text contains an em dash`(language: String) {
        allTips(catalogFor(language)).forEach { tip ->
            assertThat(tip.text).doesNotContain("—")
        }
    }

    /**
     * The translation contract, and the reason it is testable at all: every language of a pool
     * is zipped against **one** `_sources.txt`, so a translation that gains, loses or reorders a
     * line cannot line up any more. [TipCatalog.zipWithSources] already fails the load for the
     * practical pools, which makes this test redundant there and load-bearing for the *tone*
     * pools — those carry their attribution inline, so nothing but this notices when a
     * translation of `philosophy.txt` quietly drops a quotation.
     */
    @ParameterizedTest
    @MethodSource("translatedLanguages")
    fun `a translation has exactly the same shape as the English catalog`(language: String) {
        val english = catalogFor(TipCatalog.DEFAULT_LANGUAGE)
        val translated = catalogFor(language)

        pools(english).zip(pools(translated)).forEach { (englishPool, translatedPool) ->
            assertWithMessage("pool sizes must match line-for-line across languages")
                .that(translatedPool.size)
                .isEqualTo(englishPool.size)
        }
    }

    /**
     * Citations are shared rather than translated (see [TipCatalog]), so a Russian reader gets
     * English sources by design. This pins that it is the design: the same tip in two languages
     * must carry byte-identical citations, in the same order, or the translation has invented
     * evidence rather than rendered a sentence.
     */
    @ParameterizedTest
    @MethodSource("translatedLanguages")
    fun `a translation cites exactly what the English cites`(language: String) {
        val english = allTips(catalogFor(TipCatalog.DEFAULT_LANGUAGE))
        val translated = allTips(catalogFor(language))

        english.zip(translated).forEach { (englishTip, translatedTip) ->
            assertWithMessage("citations for \"%s\"", englishTip.text)
                .that(translatedTip.sources)
                .isEqualTo(englishTip.sources)
        }
    }

    /**
     * A translated tip that is still the English string is a line somebody forgot. Proper nouns
     * make a blanket rule wrong — "Две минуты. Ставь таймер. Пошёл." shares no words with its
     * original, but a line that is *entirely* untranslated is a different thing — so this asks
     * only that no pool is wholesale identical, which is what a missing file or a copied one
     * would look like.
     */
    @ParameterizedTest
    @MethodSource("translatedLanguages")
    fun `a translation is actually translated`(language: String) {
        pools(catalogFor(TipCatalog.DEFAULT_LANGUAGE))
            .zip(pools(catalogFor(language)))
            .forEach { (englishPool, translatedPool) ->
                val identical = englishPool.map { it.text } == translatedPool.map { it.text }
                assertWithMessage("a pool in '%s' is identical to the English one", language)
                    .that(identical)
                    .isFalse()
            }
    }

    /**
     * A device set to a language this app has never heard of must read English, not crash on a
     * missing resource. Worth pinning rather than trusting: the fallback is one `in` check, and
     * the failure it prevents is an exception thrown during [TipEngine] construction, which
     * happens on a background warm-up thread where nothing would catch it.
     */
    @Test
    fun `an unsupported language falls back to English`() {
        val fallback = TipCatalog.loadDefault("qq")
        assertThat(fallback.general.map { it.text })
            .isEqualTo(catalogFor(TipCatalog.DEFAULT_LANGUAGE).general.map { it.text })
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
    @MethodSource("languagesAndSingleDayPartHours")
    fun `the real catalog sustains the anti-repeat window in every single day part`(
        language: String,
        hour: Int,
    ) {
        val catalog = catalogFor(language)
        VarietyLevel.entries.forEach { variety ->
            val engine = TipEngine(catalog, Random(seed = 20260729))
            val history = ArrayDeque<String>()
            val time = LocalTime.of(hour, 0)

            repeat(DRAWS) {
                val tip = engine.messageFor(time, history.toList(), varietyLevel = variety)
                assertWithMessage(
                    "in %s at %s, %s: %s repeated within the %s-draw window",
                    language,
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

    private fun pools(catalog: TipCatalog): List<List<Tip>> =
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
        )

    private fun allTips(catalog: TipCatalog): List<Tip> = pools(catalog).flatten()

    private companion object {
        /**
         * Parsing is cheap but not free, and this class asks for the same catalog upwards of
         * twenty times per language. Built once here rather than per test.
         */
        private val catalogs: Map<String, TipCatalog> =
            TipCatalog.SUPPORTED_LANGUAGES.associateWith { TipCatalog.loadDefault(it) }

        fun catalogFor(language: String): TipCatalog = catalogs.getValue(language)

        /**
         * Long enough that the pools have to recycle many times over, so a window the content
         * cannot sustain shows up as a repeat rather than merely being untested.
         */
        const val DRAWS = 2000

        /** Enough to be a rotation rather than a message with variants. */
        const val MIN_NIGHT_POOL = 8

        @JvmStatic
        fun languages() = TipCatalog.SUPPORTED_LANGUAGES.toList()

        /** Everything except English, for the claims that are *about* being a translation. */
        @JvmStatic
        fun translatedLanguages() = TipCatalog.SUPPORTED_LANGUAGES - TipCatalog.DEFAULT_LANGUAGE

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
        fun languagesAndSingleDayPartHours() =
            TipCatalog.SUPPORTED_LANGUAGES.flatMap { language ->
                listOf(9, 14, 20, 23, 2).map { hour -> org.junit.jupiter.params.provider.Arguments.of(language, hour) }
            }
    }
}
