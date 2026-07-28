package com.healthwidget.app.widget.presentation

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.healthwidget.app.HealthWidgetApp
import kotlinx.coroutines.flow.first
import java.time.LocalTime

/**
 * Bound to a tap on the widget's tip card: lets the user pull a new tip straight from the
 * home screen, without opening the app. Same selection/anti-repeat logic and shared "last
 * tip" persistence (FR5) as the scheduled refresh in [WidgetRefreshWorker], so this doesn't
 * create a second source of truth — it just runs that logic out of turn. Passes
 * `manual = true` so a tap during the fixed sleep-hours message window (23:00-05:59) still
 * visibly changes the tip instead of silently returning the same fixed message every time —
 * see [com.healthwidget.core.tips.TipEngine.messageFor].
 */
class RefreshTipAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val container = (context.applicationContext as HealthWidgetApp).container
        val varietyLevel = container.settingsRepository.settings.first().varietyLevel
        container.advanceTip(LocalTime.now(), manual = true, varietyLevel = varietyLevel)
        container.refreshWidget()
    }
}
