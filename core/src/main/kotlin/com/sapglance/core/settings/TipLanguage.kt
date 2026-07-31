package com.sapglance.core.settings

import com.sapglance.core.tips.TipCatalog

/**
 * Which language the tips are shown in.
 *
 * This is a *tip* setting, not an app-wide locale, and the distinction is the whole reason it
 * exists as its own preference rather than deferring to Android's per-app language picker. The
 * widget is the product, the widget draws nothing but tip text, and the tip text comes from
 * [TipCatalog] rather than from Android resources — so the thing a reader actually wants to
 * switch is not something the platform's picker was going to reach on its own below API 33.
 *
 * [SYSTEM] is the default and the reason this is a three-way choice rather than the two-way
 * toggle it looks like: a phone already set to Russian should show Russian tips without anyone
 * opening Settings, and it should keep doing so if the phone's language changes later. Pinning
 * [ENGLISH] or [RUSSIAN] is a statement that the reader wants *this* language regardless of what
 * the phone is set to, which is a different thing from having not chosen yet.
 */
enum class TipLanguage(
    /** ISO 639-1, or null for "whatever the phone is set to". */
    val code: String?,
) {
    SYSTEM(null),
    ENGLISH("en"),
    RUSSIAN("ru"),
    ;

    /**
     * The catalog language this resolves to. [systemLanguage] is the device's current language
     * and is only consulted for [SYSTEM]; it is a parameter rather than a `Locale.getDefault()`
     * call so this stays a pure function in a module with no platform behind it.
     *
     * Resolving an unsupported [systemLanguage] is deliberately *not* handled here.
     * [TipCatalog.loadDefault] already falls back to English for anything it does not have, and
     * duplicating that decision in two places is how the two of them eventually disagree.
     */
    fun resolve(systemLanguage: String): String = code ?: systemLanguage

    companion object {
        /** Parses a persisted name, tolerating anything unrecognised. Storage can outlive code. */
        fun fromNameOrNull(name: String?): TipLanguage? = entries.firstOrNull { it.name == name }
    }
}
