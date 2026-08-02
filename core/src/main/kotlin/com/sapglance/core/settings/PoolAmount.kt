package com.sapglance.core.settings

/**
 * How much of one pool the reader wants. Three steps rather than a continuous slider: the
 * difference between 40% and 45% of a draw is not something anyone can perceive in a widget that
 * changes four times a day, so offering it would be a control that cannot be felt.
 *
 * [NONE] is the reason this type replaced `VarietyLevel`, and it is a real change of promise.
 * The old control was a lean and never a filter — every level still let every pool through
 * occasionally, and a test pinned that. A reader who does not want to be told to seize the day
 * was not served by "less motivation, sometimes"; they wanted none, and now they can have none.
 * What the app still refuses to do is silence *everything*: see [PoolMix.isSilent].
 */
enum class PoolAmount {
    NONE,
    SOME,
    PLENTY,
}
