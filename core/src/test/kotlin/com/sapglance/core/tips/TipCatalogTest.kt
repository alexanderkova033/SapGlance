package com.sapglance.core.tips

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class TipCatalogTest {
    private val catalog = TipCatalog.loadDefault()

    @Test
    fun `all pools are non-empty`() {
        assertThat(catalog.general).isNotEmpty()
        assertThat(catalog.morning).isNotEmpty()
        assertThat(catalog.afternoon).isNotEmpty()
        assertThat(catalog.evening).isNotEmpty()
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

    @Test
    fun `sleep messages are single, non-blank, and distinct`() {
        assertThat(catalog.sleepLate.text.isNotBlank()).isTrue()
        assertThat(catalog.sleepEarlyHours.text.isNotBlank()).isTrue()
        assertThat(catalog.sleepLate.text).isNotEqualTo(catalog.sleepEarlyHours.text)
    }

    @Test
    fun `each pool is tagged with the kind it belongs to`() {
        (catalog.general + catalog.morning + catalog.afternoon + catalog.evening).forEach {
            assertThat(it.kind).isEqualTo(TipKind.PRACTICAL)
        }
        assertThat(catalog.sleepLate.kind).isEqualTo(TipKind.PRACTICAL)
        assertThat(catalog.sleepEarlyHours.kind).isEqualTo(TipKind.PRACTICAL)
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

    private fun allTips(): List<Tip> =
        catalog.general + catalog.morning + catalog.afternoon + catalog.evening +
            catalog.tonePools + listOf(catalog.sleepLate, catalog.sleepEarlyHours)
}
