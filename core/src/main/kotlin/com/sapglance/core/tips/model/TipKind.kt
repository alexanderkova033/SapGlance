package com.sapglance.core.tips.model

/**
 * What *kind* of thing a tip is, which determines how it can honestly be presented.
 *
 * The catalog started out as nothing but evidence-backed wellness advice, so every tip was
 * required to carry at least [Tip.MIN_SOURCES] research citations. That bar is right for
 * "mild dehydration dents focus" and nonsense for "you're allowed to begin again": there is no
 * study behind a piece of encouragement, and inventing one, or stretching a real paper to cover
 * it, would be exactly the dishonesty the citation model exists to prevent. So the requirement
 * is per-kind rather than global, and the UI reads [Tip.kind] to decide what to show underneath
 * a tip: sources for [PRACTICAL], an attribution for a quoted [PHILOSOPHY] tip, nothing at all
 * for an original line.
 */
enum class TipKind {
    /** Evidence-backed wellness advice. Must cite at least [Tip.MIN_SOURCES] real sources. */
    PRACTICAL,

    /** Encouragement and momentum. Original writing, no citation, none implied. */
    MOTIVATION,

    /**
     * Reflection and perspective. Two sub-cases share a kind: public-domain quotations, which
     * carry exactly one source (the text they come from, so the attribution is checkable), and
     * original reflections, which carry none.
     */
    PHILOSOPHY,

    /** Gentle self-compassion and emotional check-ins. Original writing, no citation. */
    WELLBEING,
    ;

    /** Only [PRACTICAL] tips make empirical claims, so only they owe the reader evidence. */
    val requiresCitation: Boolean get() = this == PRACTICAL
}
