package com.sapglance.core.settings

import com.google.common.truth.Truth.assertThat
import com.sapglance.core.tips.TipCatalog
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class TipLanguageTest {
    @Test
    fun `a pinned language ignores the device`() {
        assertThat(TipLanguage.ENGLISH.resolve("ru")).isEqualTo("en")
        assertThat(TipLanguage.RUSSIAN.resolve("en")).isEqualTo("ru")
    }

    @Test
    fun `SYSTEM follows the device`() {
        assertThat(TipLanguage.SYSTEM.resolve("ru")).isEqualTo("ru")
        assertThat(TipLanguage.SYSTEM.resolve("en")).isEqualTo("en")
    }

    /**
     * [TipLanguage.SYSTEM] deliberately does *not* sanitise an unsupported device language, so
     * that the fallback lives in exactly one place. This pins the contract that makes that safe:
     * whatever comes out of [TipLanguage.resolve] is something [TipCatalog.loadDefault] will
     * accept. If the fallback were ever duplicated here, the two would eventually disagree and
     * the bug would be a catalog silently in the wrong language rather than a crash.
     */
    @ParameterizedTest
    @ValueSource(strings = ["qq", "", "he", "zz-ZZ"])
    fun `an unsupported device language survives the round trip to a real catalog`(deviceLanguage: String) {
        val resolved = TipLanguage.SYSTEM.resolve(deviceLanguage)
        val catalog = TipCatalog.loadDefault(resolved)

        assertThat(catalog.general.map { it.text })
            .isEqualTo(TipCatalog.loadDefault(TipCatalog.DEFAULT_LANGUAGE).general.map { it.text })
    }

    /**
     * Every pinned option must actually have a catalog behind it. Without this, adding an enum
     * constant for a language nobody has translated yet compiles, ships, and silently serves
     * English to anyone who picks it.
     */
    @Test
    fun `every pinned language is one the catalog ships`() {
        val pinned = TipLanguage.entries.mapNotNull { it.code }

        assertThat(TipCatalog.SUPPORTED_LANGUAGES).containsAtLeastElementsIn(pinned)
    }

    @Test
    fun `a persisted name round-trips, and an unknown one is null rather than an exception`() {
        TipLanguage.entries.forEach {
            assertThat(TipLanguage.fromNameOrNull(it.name)).isEqualTo(it)
        }
        assertThat(TipLanguage.fromNameOrNull("KLINGON")).isNull()
        assertThat(TipLanguage.fromNameOrNull(null)).isNull()
    }
}
