package com.sapglance.app.widget.presentation

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.sapglance.app.SapGlanceApp
import kotlinx.coroutines.flow.first
import java.time.LocalTime

/**
 * Bound to a tap on the widget's tip card: lets the user pull a new tip straight from the
 * home screen, without opening the app. Same selection/anti-repeat logic and shared "last
 * tip" persistence (FR5) as the scheduled refresh in [WidgetRefreshWorker], so this doesn't
 * create a second source of truth — it just runs that logic out of turn, at every hour of the
 * day: the sleep-hours window (23:00-05:59) used to need a `manual` flag here to return anything
 * new, and now draws from a real pool like any other hour.
 */
class RefreshTipAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val container = (context.applicationContext as SapGlanceApp).container
        val settings = container.settingsRepository.settings.first()
        container.advanceTip(
            container.tipEngine(settings.language),
            LocalTime.now(),
            varietyLevel = settings.varietyLevel,
        )
        container.refreshWidget()
    }
}
