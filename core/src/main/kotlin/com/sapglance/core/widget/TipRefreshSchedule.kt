package com.sapglance.core.widget

import com.sapglance.core.tips.DayPart
import java.time.LocalDateTime

/**
 * When the tip changes: three times a day, at [TIP_SWITCH_HOURS].
 *
 * ## What this replaced, and why
 *
 * Until 2026-07-31 the rule was "advance after ~90 minutes of *confirmed screen-on time*",
 * sampled by ticking every 15 minutes and counting only the ticks where the screen happened to
 * be on. The intent was that a tip nobody saw should not rotate past them. It was replaced on
 * the owner's call, and the honest summary is that it bought less than it cost:
 *
 * - **It was a sampling approximation dressed as a stopwatch.** A tick reflected the instant it
 *   fired, not the 15 minutes before it, so "90 minutes of screen-on time" really meant "six
 *   moments, spaced 15 minutes apart, at which the screen happened to be on".
 * - **It made the tip's arrival unpredictable.** A heavy phone day produced several tips; a day
 *   away from the phone produced none. Neither is wrong exactly, but neither is something a
 *   reader can form a habit around.
 * - **It fought the day-part pools.** A tip drawn at 11:50 came from `morning` and then sat on
 *   the home screen all afternoon, which is the one thing the whole time-of-day design exists
 *   to prevent.
 *
 * ## Why these three hours
 *
 * 06:00, 12:00 and 18:00 are not arbitrary: they are exactly the [DayPart] boundaries. Each
 * switch therefore hands over a tip drawn from the pool for the part of the day being entered,
 * and the tip on screen always belongs to the hours you are actually in. That alignment is the
 * real win, and it is the thing to protect — if these hours ever change, change [DayPart] with
 * them, or the pools quietly stop matching the clock again.
 *
 * ## The night, which is the known cost
 *
 * The list stops at 18:00, so the evening tip stays up until 06:00. **That makes `sleep_late`
 * and `sleep_early` unreachable on the schedule** — 29 tips written specifically for someone
 * awake at 3am, now reachable only by tapping the widget. Adding `23` to [TIP_SWITCH_HOURS] is
 * the entire fix and needs no other change anywhere; it is left out because the brief was three
 * switches between 06:00 and 22:00.
 *
 * ## Why a slot rather than a countdown
 *
 * [currentSlotStart] answers "which switch are we past?", and the worker advances when that
 * answer differs from the one it last acted on. That is idempotent, which matters more than it
 * sounds: WorkManager makes no promise about running on time, Doze can defer a tick for hours,
 * and the process can die between any two of them. A countdown loses time in all three cases; a
 * slot cannot. A whole missed day still produces exactly one advance rather than a burst of
 * catch-up ones.
 */
val TIP_SWITCH_HOURS = listOf(6, 12, 18)

/**
 * How often the worker checks. WorkManager's own minimum for periodic work, and kept at the
 * minimum because what it buys is *latency on a switch* rather than accuracy: at 15 minutes,
 * someone who looks at their phone just after 06:00 sees the morning tip rather than last
 * night's. The check itself is now a string comparison where it used to be a `PowerManager`
 * call plus a counter write.
 */
const val TICK_INTERVAL_MINUTES = 15L

/**
 * The start of the switch window [now] falls in — the most recent moment in [TIP_SWITCH_HOURS]
 * at or before it. Before the first switch of the day that is *yesterday's* last one, which is
 * what keeps the small hours on the evening tip instead of briefly inventing a fourth slot.
 *
 * Callers compare this against a persisted value, so it has to be stable for every instant
 * inside a window and change exactly once at each boundary. It is a pure function of the local
 * clock with no reference to when the app last ran, which is what makes the comparison safe
 * after a reboot, a Doze window, or a day with no ticks at all.
 */
fun currentSlotStart(now: LocalDateTime): LocalDateTime {
    val today = now.toLocalDate()
    val hourToday = TIP_SWITCH_HOURS.lastOrNull { it <= now.hour }
    return if (hourToday != null) {
        today.atTime(hourToday, 0)
    } else {
        today.minusDays(1).atTime(TIP_SWITCH_HOURS.last(), 0)
    }
}
