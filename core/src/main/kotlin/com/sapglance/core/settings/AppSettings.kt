package com.sapglance.core.settings

/**
 * User-configurable settings (FR6). A plain domain model — persistence is an implementation
 * detail of whatever [SettingsRepository] is backing it.
 *
 * [varietyLevel] biases tip selection towards the motivation/philosophy/wellbeing tone pools rather
 * than switching them on or off outright — see [com.sapglance.core.tips.TipEngine]'s `pick`
 * for the actual weighting. [VarietyLevel.PRACTICAL] by default: the practical wellness tips
 * are the app's core and stay the overwhelming majority until a user opts into more variety.
 *
 * [language] picks which language's catalog the tips are read from. [TipLanguage.SYSTEM] by
 * default, so a phone already set to a language SapGlance ships reads that language without
 * anyone opening Settings.
 */
data class AppSettings(
    val varietyLevel: VarietyLevel,
    val language: TipLanguage = TipLanguage.SYSTEM,
) {
    companion object {
        val DEFAULT =
            AppSettings(
                varietyLevel = VarietyLevel.PRACTICAL,
                language = TipLanguage.SYSTEM,
            )
    }
}
