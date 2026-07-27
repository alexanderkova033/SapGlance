package com.healthwidget.core.scheduling

/**
 * The tip should only change once it's actually been on screen for a while — not on a pure
 * wall-clock timer, which could rotate a tip nobody ever saw (phone face-down all afternoon,
 * screen off overnight). WorkManager has no "notify me when the screen turns on" primitive
 * and a real screen-on/off listener can't survive process death without a foreground service,
 * so this instead samples: [WidgetRefreshWorker][com.healthwidget.app.widget.scheduling.WidgetRefreshWorker]
 * ticks every [TICK_INTERVAL_MINUTES] (WorkManager's own minimum periodic interval) and counts
 * a tick only if the screen happens to be on at that instant. [TICKS_UNTIL_ADVANCE] such ticks
 * (spaced [TICK_INTERVAL_MINUTES] apart, so ~90 minutes of *confirmed* on-screen time,
 * accumulated across ticks rather than requiring one unbroken session) since the tip was last
 * shown is treated as "the user has had a real chance to see it" and advances to the next one.
 *
 * This is a sampling approximation, not a precise stopwatch — a tick only reflects the instant
 * it fired, not the full interval before it — but it's simple, survives process death (the
 * count is persisted, not held in memory), and needs no extra permissions or a live receiver.
 */
const val TICK_INTERVAL_MINUTES = 15L
const val TICKS_UNTIL_ADVANCE = 6 // 6 * 15 = 90 minutes of confirmed screen-on ticks

/** Whether the *next* tick (the one about to be counted) reaches [TICKS_UNTIL_ADVANCE]. */
fun shouldAdvanceTip(screenOnTicks: Int): Boolean = screenOnTicks + 1 >= TICKS_UNTIL_ADVANCE
