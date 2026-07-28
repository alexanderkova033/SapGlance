package com.healthwidget.app

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.healthwidget.app.common.healthWidgetDataStore
import com.healthwidget.app.settings.data.DataStoreSettingsRepository
import com.healthwidget.app.tips.data.DataStoreTipHistoryRepository
import com.healthwidget.app.widget.data.DataStoreWidgetRefreshRepository
import com.healthwidget.app.widget.presentation.TipWidget
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

    /**
     * Constructing this parses all 15 bundled tip text resources out of the APK, which is the
     * single most expensive step in the tip-refresh path and is pure CPU with no I/O dependency
     * on anything else the app is doing. See [warmUp].
     */
    val tipEngine: TipEngine by lazy { TipEngine() }

    val advanceTip: AdvanceTipUseCase by lazy { AdvanceTipUseCase(tipEngine, tipHistoryRepository) }

    /**
     * Parses the tip catalog ahead of the first thing that needs it, off the main thread.
     *
     * A widget tap on a dead process pays for a cold start *and* the catalog parse *and* the
     * DataStore read, strictly one after another, before anything can repaint — the parse sits
     * on the critical path purely because [tipEngine] is built lazily at the moment of first use.
     * Nothing about it depends on the tap: it reads bundled resources that never change. Started
     * from [com.healthwidget.app.HealthWidgetApp.onCreate], it runs in parallel with the
     * DataStore read that the tap has to do anyway, so by the time selection needs the catalog
     * it is usually already built.
     *
     * `by lazy` is `SYNCHRONIZED` by default, so a tap arriving mid-parse blocks on the same
     * initialization rather than starting a second one, and one arriving after it is a plain
     * field read. This is a scheduling change only: nothing is precomputed that wasn't already
     * computed, and nothing is cached that wasn't already cached for the process's lifetime.
     */
    fun warmUp() {
        tipEngine
    }

    suspend fun refreshWidget() {
        widgetRefreshMutex.withLock {
            TipWidget().updateAll(appContext)
        }
    }
}
