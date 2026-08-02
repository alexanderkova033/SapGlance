package com.sapglance.app.widget.framework

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sapglance.app.SapGlanceApp
import com.sapglance.core.widget.currentSlotStart
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

/**
 * Ticks every [com.sapglance.core.widget.TICK_INTERVAL_MINUTES] (see [WidgetScheduler]) and
 * advances the tip the first time it notices a new switch window — see `TipRefreshSchedule.kt`
 * for the three hours, and for why this is a slot comparison rather than a countdown.
 *
 * Note what is *not* here any more: the `PowerManager.isInteractive` check that used to gate
 * every tick, and the counter it fed. The tip now changes on the clock whether or not anyone was
 * looking, which is the deliberate trade recorded in `TipRefreshSchedule.kt`.
 *
 * Nothing here is ever gated on quiet hours: a passive widget refresh isn't an interruption, so
 * there is nothing to silence. (v1 has no notifications at all — they were built and then
 * deliberately removed.)
 *
 * [KEY_FORCE] bypasses the slot logic entirely and just re-renders the widget's current state —
 * used by [WidgetScheduler.refreshNow] (e.g. right after boot) where the point isn't to advance
 * the tip, only to make sure the home screen isn't showing a stale render.
 */
class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as SapGlanceApp).container

        if (inputData.getBoolean(KEY_FORCE, false)) {
            container.refreshWidget()
            return Result.success()
        }

        val now = LocalDateTime.now()
        val slot = currentSlotStart(now).toString()
        if (slot != container.widgetRefreshRepository.lastTipSlot.first()) {
            val settings = container.settingsRepository.settings.first()
            container.advanceTip(
                container.tipEngine(settings.language),
                now.toLocalTime(),
                poolMix = settings.poolMix,
            )
            // Written after the advance, not before. If the process dies mid-run the worst case
            // is doing this window twice, which costs one extra tip; writing first would make the
            // worst case a window skipped entirely, and a missing tip is the worse failure.
            container.widgetRefreshRepository.setLastTipSlot(slot)
            container.refreshWidget()
        }

        return Result.success()
    }

    companion object {
        const val KEY_FORCE = "force"
    }
}
