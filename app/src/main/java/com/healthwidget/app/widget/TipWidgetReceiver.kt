package com.healthwidget.app.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.healthwidget.app.widget.presentation.TipWidget

/**
 * The manifest-declared `AppWidgetProvider`. Everything else in this feature moved down into
 * `presentation/`, `scheduling/` and `data/`; this one class deliberately stays at the feature
 * root, because its fully-qualified name is not an implementation detail — it is the
 * `ComponentName` the launcher persists for every widget the user has placed. Moving it to
 * another package renames that component, and on the next install Android finds no provider
 * matching the stored name and drops every placed widget off the home screen. A tidier package
 * is not worth making people re-add their widgets, so this name is frozen.
 */
class TipWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TipWidget()
}
