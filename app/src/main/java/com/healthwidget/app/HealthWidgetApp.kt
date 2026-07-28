package com.healthwidget.app

import android.app.Application
import com.healthwidget.app.widget.scheduling.WidgetScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HealthWidgetApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    // Lives as long as the process — for fire-and-forget work that must survive whatever UI
    // triggered it (e.g. the startup scheduling safety net below, or a widget re-render
    // kicked off from the settings screen), never for long-running work. A screen-scoped
    // rememberCoroutineScope() would get cancelled if the user navigates away mid-flight.
    internal val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Parsing the tip catalog is the most expensive step in answering a widget tap, and it
        // depends on nothing the tap provides — see AppContainer.warmUp. Starting it here lets it
        // run alongside the DataStore read instead of after it. Deliberately its own launch, so
        // the scheduling call below can't delay it (or be delayed by it).
        applicationScope.launch { container.warmUp() }
        applicationScope.launch {
            WidgetScheduler(this@HealthWidgetApp).ensureScheduled()
        }
    }
}
