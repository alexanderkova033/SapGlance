package com.sapglance.core.settings

/**
 * How strongly tip selection leans towards the motivation/philosophy/wellbeing tone pools rather
 * than the practical one — never a filter, see [com.sapglance.core.tips.TipEngine]'s `pick`
 * for the actual weighting each level maps to.
 */
enum class VarietyLevel {
    PRACTICAL,
    BALANCED,
    PLAYFUL,
}
