package com.healthwidget.app.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.healthwidget.app.widget.scheduling.WidgetScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * FR7: WorkManager itself survives reboot, but this makes rescheduling explicit and also
 * forces one immediate widget refresh so the widget isn't stale until the next periodic tick.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                WidgetScheduler(appContext).apply {
                    ensureScheduled()
                    refreshNow()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
