package com.healthwidget.app

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.healthwidget.app.common.healthWidgetDataStore
import com.healthwidget.app.settings.data.DataStoreSettingsRepository
import com.healthwidget.app.tips.data.DataStoreTipHistoryRepository
import com.healthwidget.app.widget.TipWidget
import com.healthwidget.app.widget.data.DataStoreWidgetRefreshRepository
import com.healthwidget.core.scheduling.WidgetRefreshRepository
import com.healthwidget.core.settings.SettingsRepository
import com.healthwidget.core.tips.AdvanceTipUseCase
import com.healthwidget.core.tips.TipEngine
import com.healthwidget.core.tips.TipHistoryRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Small hand-written composition root. The fixed tech stack has no DI framework, and this
 * app is too small to justify pulling one in — a handful of `by lazy` singletons is enough.
 *
 * Exposes the domain-layer interfaces ([SettingsRepository], [TipHistoryRepository]), not
 * the DataStore-backed implementation classes, so callers depend on the abstraction.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    // Serializes every push of the widget's UI to the home screen. Three independent triggers
    // can call this (the periodic tick worker, the manual "get a different tip" button, and
    // tapping the widget itself) with no ordering guarantee between them, so the lock keeps two
    // overlapping GlanceAppWidget.updateAll() calls from interleaving.
    //
    // This lock was once believed to be the fix for "the widget's tip/background only sometimes
    // updates". It wasn't: that bug was TipWidget capturing the tip outside provideContent, so
    // no updateAll() could ever change what was drawn (see the comment in TipWidget). Now that
    // the widget observes the persisted tip directly, a new tip repaints as soon as it's
    // written, and updateAll()'s remaining job is to *start* a Glance session when none is
    // running (e.g. after process death) — still worth serializing, but no longer the mechanism
    // that delivers a new tip.
    private val widgetRefreshMutex = Mutex()

    val settingsRepository: SettingsRepository by lazy {
        DataStoreSettingsRepository(appContext.healthWidgetDataStore)
    }

    val tipHistoryRepository: TipHistoryRepository by lazy {
        DataStoreTipHistoryRepository(appContext.healthWidgetDataStore)
    }

    val widgetRefreshRepository: WidgetRefreshRepository by lazy {
        DataStoreWidgetRefreshRepository(appContext.healthWidgetDataStore)
    }

    val tipEngine: TipEngine by lazy { TipEngine() }

    val advanceTip: AdvanceTipUseCase by lazy { AdvanceTipUseCase(tipEngine, tipHistoryRepository) }

    suspend fun refreshWidget() {
        widgetRefreshMutex.withLock {
            TipWidget().updateAll(appContext)
        }
    }
}
