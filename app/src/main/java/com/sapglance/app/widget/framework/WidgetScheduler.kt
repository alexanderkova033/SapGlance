package com.sapglance.app.widget.framework

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.sapglance.core.widget.usecase.TICK_INTERVAL_MINUTES
import java.util.concurrent.TimeUnit

class WidgetScheduler(private val context: Context) {
    /** Safe to call on every app start / boot: won't reset an already-running periodic timer. */
    fun ensureScheduled() = apply(ExistingPeriodicWorkPolicy.KEEP)

    private fun apply(policy: ExistingPeriodicWorkPolicy) {
        val request =
            PeriodicWorkRequestBuilder<WidgetRefreshWorker>(TICK_INTERVAL_MINUTES, TimeUnit.MINUTES)
                .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, policy, request)
    }

    /** Forces an immediate re-render of the widget's *current* state (does not advance the
     * tip) — e.g. right after boot so the widget isn't stale until the next periodic tick. */
    fun refreshNow() {
        val request =
            OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                .setInputData(workDataOf(WidgetRefreshWorker.KEY_FORCE to true))
                .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(IMMEDIATE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "widget_periodic_refresh"
        private const val IMMEDIATE_WORK_NAME = "widget_immediate_refresh"
    }
}
