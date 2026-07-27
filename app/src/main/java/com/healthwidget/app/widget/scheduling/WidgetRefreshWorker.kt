package com.healthwidget.app.widget.scheduling

import android.content.Context
import android.os.PowerManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.healthwidget.app.HealthWidgetApp
import com.healthwidget.core.scheduling.shouldAdvanceTip
import kotlinx.coroutines.flow.first
import java.time.LocalTime

/**
 * Ticks every [com.healthwidget.core.scheduling.TICK_INTERVAL_MINUTES] (see
 * [WidgetScheduler]) and only advances the tip once [com.healthwidget.core.scheduling.shouldAdvanceTip]
 * says enough confirmed screen-on ticks have accumulated since it was last shown — see
 * `TipRefreshSchedule.kt` for why. Quiet hours intentionally never gated this (there's no
 * notification here to silence) and that stays true now that ticks are gated on the screen
 * being on instead: a passive widget refresh isn't an interruption.
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
        val container = (applicationContext as HealthWidgetApp).container

        if (inputData.getBoolean(KEY_FORCE, false)) {
            container.refreshWidget()
            return Result.success()
        }

        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (powerManager.isInteractive) {
            val ticks = container.widgetRefreshRepository.screenOnTicks.first()
            if (shouldAdvanceTip(ticks)) {
                val varietyLevel = container.settingsRepository.settings.first().varietyLevel
                container.advanceTip(LocalTime.now(), varietyLevel = varietyLevel)
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
