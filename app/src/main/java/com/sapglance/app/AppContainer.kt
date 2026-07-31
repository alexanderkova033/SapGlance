package com.sapglance.app

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.sapglance.app.platform.sapGlanceDataStore
import com.sapglance.app.settings.data.DataStoreSettingsRepository
import com.sapglance.app.tips.data.DataStoreTipHistoryRepository
import com.sapglance.app.widget.data.DataStoreWidgetRefreshRepository
import com.sapglance.app.widget.presentation.TipWidget
import com.sapglance.core.settings.SettingsRepository
import com.sapglance.core.settings.TipLanguage
import com.sapglance.core.tips.AdvanceTipUseCase
import com.sapglance.core.tips.TipCatalog
import com.sapglance.core.tips.TipEngine
import com.sapglance.core.tips.TipHistoryRepository
import com.sapglance.core.widget.WidgetRefreshRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

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
        DataStoreSettingsRepository(appContext.sapGlanceDataStore)
    }

    val tipHistoryRepository: TipHistoryRepository by lazy {
        DataStoreTipHistoryRepository(appContext.sapGlanceDataStore)
    }

    val widgetRefreshRepository: WidgetRefreshRepository by lazy {
        DataStoreWidgetRefreshRepository(appContext.sapGlanceDataStore)
    }

    /**
     * One [TipEngine] per language, built on first use and kept for the process's lifetime.
     *
     * Building one parses nine bundled tip text resources and their six companion source files
     * out of the APK, which is the single most expensive step in the tip-refresh path and is pure
     * CPU with no I/O dependency on anything else the app is doing. See [warmUp].
     *
     * A map rather than a single lazy field because the language is now a *setting*
     * ([TipLanguage]) rather than a fact about the device, so it can change without the process
     * restarting — which is exactly what a `by lazy` singleton could not survive. Caching every
     * language the reader visits costs one parsed catalog each and means toggling back and forth
     * is free after the first time; there are two languages, so the map is not going to grow into
     * a memory question.
     *
     * `ConcurrentHashMap.computeIfAbsent` rather than a plain map: the tick worker, a widget tap
     * and the settings screen can all ask for an engine at once, and the atomic version means
     * they share one catalog instead of racing to parse three.
     *
     * Worth knowing: the tip history is keyed by tip *text*, so switching language leaves the
     * stored history full of strings the new catalog does not contain. That degrades exactly as
     * it should — [TipCatalog.kindOf] answers null, anti-repeat matches nothing, and the reader
     * gets a fresh rotation in the new language — but it is a reset, not a migration.
     */
    private val engines = ConcurrentHashMap<String, TipEngine>()

    fun tipEngine(language: TipLanguage): TipEngine =
        engines.computeIfAbsent(language.resolve(Locale.getDefault().language)) {
            TipEngine(TipCatalog.loadDefault(it))
        }

    /**
     * The engine for whatever the reader has currently chosen. Suspends because the choice lives
     * in DataStore; call sites that already read settings for the variety level should use
     * [tipEngine] with the value they already have rather than reading twice.
     */
    suspend fun currentTipEngine(): TipEngine = tipEngine(settingsRepository.settings.first().language)

    val advanceTip: AdvanceTipUseCase by lazy { AdvanceTipUseCase(tipHistoryRepository) }

    /**
     * Parses the tip catalog ahead of the first thing that needs it, off the main thread.
     *
     * A widget tap on a dead process pays for a cold start *and* the catalog parse *and* the
     * DataStore read, strictly one after another, before anything can repaint — the parse sits
     * on the critical path purely because an engine is built at the moment of first use. Nothing
     * about it depends on the tap: it reads bundled resources that never change. Started from
     * [com.sapglance.app.SapGlanceApp.onCreate], it runs in parallel with the DataStore read that
     * the tap has to do anyway, so by the time selection needs the catalog it is usually already
     * built.
     *
     * It suspends now, where it used to just touch a `by lazy`, because *which* catalog to warm
     * is a stored preference rather than a constant. That is a real trade and it goes the right
     * way: the DataStore read this waits on is the same one the tap was going to do regardless,
     * and warming the wrong language would leave the tap paying for the parse it came here to
     * avoid.
     *
     * `computeIfAbsent` is atomic, so a tap arriving mid-parse blocks on the same construction
     * rather than starting a second one, and one arriving after it is a map hit. This is a
     * scheduling change only: nothing is precomputed that wasn't already computed, and nothing is
     * cached that wasn't already cached for the process's lifetime.
     */
    suspend fun warmUp() {
        currentTipEngine()
    }

    suspend fun refreshWidget() {
        widgetRefreshMutex.withLock {
            TipWidget().updateAll(appContext)
        }
    }
}
