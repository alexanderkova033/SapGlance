package com.sapglance.core.settings

/**
 * User-configurable settings (FR6). A plain domain model — persistence is an implementation
 * detail of whatever [SettingsRepository] is backing it.
 *
 * [poolMix] is how much of each pool the reader wants, one amount per voice — see [PoolMix] for
 * the two axes it splits into, and for what the hour still decides regardless of it. It replaced
 * a single `VarietyLevel` lean on 2026-08-02, because that control moved all three tone voices
 * together and they are not one thing.
 *
 * [language] picks which language's catalog the tips are read from. [TipLanguage.SYSTEM] by
 * default, so a phone already set to a language SapGlance ships reads that language without
 * anyone opening Settings.
 */
data class AppSettings(
    val poolMix: PoolMix,
    val language: TipLanguage = TipLanguage.SYSTEM,
) {
    companion object {
        val DEFAULT =
            AppSettings(
                poolMix = PoolMix.DEFAULT,
                language = TipLanguage.SYSTEM,
            )
    }
}
