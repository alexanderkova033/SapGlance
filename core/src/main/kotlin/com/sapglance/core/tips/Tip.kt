package com.sapglance.core.tips

/**
 * One citation backing a [Tip]: a human-readable [label] and a [url] the UI can open in the
 * system browser.
 */
data class TipSource(
    val label: String,
    val url: String,
)

/**
 * A single tip together with the research backing it, so the UI can show users *why* a tip is
 * shown, not just the tip itself.
 *
 * [sources] is a list rather than a single label/URL pair because one citation reads as one
 * study's opinion: a short health claim is far better supported by two or three independent
 * sources that happen to agree, and several tips legitimately share the same source (a
 * meta-analysis backs every tip drawing on it), so sources repeat across tips by design.
 * Every bundled tip is required to carry at least [MIN_SOURCES] of them (see `TipCatalogTest`)
 * rather than leaving the list nullable or possibly-empty. See `TIP_SOURCES.md` at the repo
 * root for the underlying research this data traces back to.
 */
data class Tip(
    val text: String,
    val kind: TipKind = TipKind.PRACTICAL,
    val sources: List<TipSource> = emptyList(),
) {
    companion object {
        /**
         * The evidence bar a [TipKind.PRACTICAL] tip has to clear, enforced by `TipCatalogTest`.
         * Tips of other kinds make no empirical claim and are exempt — see [TipKind].
         */
        const val MIN_SOURCES = 2
    }
}
