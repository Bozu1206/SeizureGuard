package com.epfl.ch.seizureguard.widgets

import android.content.Context
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SeizureWidgetUpdater {
    fun updateWidgets(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(SeizureCountWidget::class.java)
            glanceIds.forEach { glanceId ->
                SeizureCountWidget().update(context, glanceId)
            }
        }

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetComponent = ComponentName(context, SeizureCountWidgetReceiver::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)
        appWidgetManager.notifyAppWidgetViewDataChanged(widgetIds, android.R.id.list)
    }
} 