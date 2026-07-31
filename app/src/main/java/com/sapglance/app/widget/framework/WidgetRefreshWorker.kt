package com.sapglance.app.widget.framework

import android.content.Context
import android.os.PowerManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sapglance.app.SapGlanceApp
import com.sapglance.core.widget.shouldAdvanceTip
import kotlinx.coroutines.flow.first
import java.time.LocalTime

/**
 * Ticks every [com.sapglance.core.widget.TICK_INTERVAL_MINUTES] (see
 * [WidgetScheduler]) and only advances the tip once [com.sapglance.core.widget.shouldAdvanceTip]
 * says enough confirmed screen-on ticks have accumulated since it was last shown — see
 * `TipRefreshSchedule.kt` for why. Nothing here is ever gated on quiet hours: a passive widget
 * refresh isn't an interruption, so there is nothing to silence. (v1 has no notifications at
 * all — they were built and then deliberately removed.)
 *
 * [KEY_FORCE] bypasses the tick logic entirely and just re-renders the widget's current
 * state — used by [WidgetScheduler.refreshNow] (e.g. right after boot) where the point isn't
 * to advance the tip, only to make sure the home screen isn't showing a stale render.
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

        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (powerManager.isInteractive) {
            val ticks = container.widgetRefreshRepository.screenOnTicks.first()
            if (shouldAdvanceTip(ticks)) {
                val settings = container.settingsRepository.settings.first()
                container.advanceTip(
                    container.tipEngine(settings.language),
                    LocalTime.now(),
                    varietyLevel = settings.varietyLevel,
                )
                container.widgetRefreshRepository.setScreenOnTicks(0)
                container.refreshWidget()
            } else {
                container.widgetRefreshRepository.setScreenOnTicks(ticks + 1)
            }
        }

        return Result.success()
    }

    companion object {
        const val KEY_FORCE = "force"
    }
}
