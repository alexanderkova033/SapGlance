package com.healthwidget.app.common

import android.util.Log

/** TEMPORARY measurement scaffolding for the tap-to-refresh latency work. Deleted before commit. */
object PerfLog {
    private const val TAG = "HWPerf"
    private val processStart = System.nanoTime()

    fun mark(label: String) {
        Log.d(TAG, "$label @ ${sinceStart()}ms since process start")
    }

    inline fun <T> time(
        label: String,
        block: () -> T,
    ): T {
        val start = System.nanoTime()
        val result = block()
        log(label, (System.nanoTime() - start) / 1_000_000.0)
        return result
    }

    fun log(
        label: String,
        ms: Double,
    ) {
        Log.d(TAG, "%s took %.1fms (at %sms)".format(label, ms, sinceStart()))
    }

    fun sinceStart(): String = "%.1f".format((System.nanoTime() - processStart) / 1_000_000.0)
}
