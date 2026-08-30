package com.customwidgets.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.customwidgets.app.data.local.WidgetDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WidgetManager {

    /**
     * Triggers a Glance update for a specific appWidgetId.
     */
    suspend fun updateWidget(context: Context, appWidgetId: Int) {
        withContext(Dispatchers.IO) {
            try {
                val glanceManager = GlanceAppWidgetManager(context)
                val glanceId = glanceManager.getGlanceIdBy(appWidgetId)
                CustomGlanceWidget().update(context, glanceId)
            } catch (_: Exception) {
                // Fallback to updateAll if glanceId cannot be resolved
                CustomGlanceWidget().updateAll(context)
            }
        }
    }

    /**
     * Triggers a Glance update for multiple appWidgetIds.
     */
    suspend fun updateWidgets(context: Context, appWidgetIds: List<Int>) {
        withContext(Dispatchers.IO) {
            val glanceManager = GlanceAppWidgetManager(context)
            appWidgetIds.forEach { id ->
                try {
                    val glanceId = glanceManager.getGlanceIdBy(id)
                    CustomGlanceWidget().update(context, glanceId)
                } catch (_: Exception) {
                }
            }
        }
    }

    /**
     * Updates all active custom widgets.
     */
    suspend fun updateAllWidgets(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                CustomGlanceWidget().updateAll(context)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Reconciles stored widget instances with the AppWidgetManager and removes stale IDs.
     */
    suspend fun cleanupOrphanedWidgets(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val activeIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, CustomWidgetProvider::class.java)
                ).toSet()

                val db = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    WidgetDatabase::class.java,
                    WidgetDatabase.DATABASE_NAME
                ).build()

                val storedIds = db.widgetDao().getAllAppWidgetIds()
                val orphanedIds = storedIds.filter { it !in activeIds }

                orphanedIds.forEach { id ->
                    db.widgetDao().deleteAppWidgetId(id)
                }
                db.close()
            } catch (_: Exception) {
            }
        }
    }
}
