package com.customwidgets.app.widget.update

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.customwidgets.app.data.local.WidgetDatabase
import com.customwidgets.app.domain.model.WidgetDefinition
import com.customwidgets.app.domain.model.WidgetNode
import com.customwidgets.app.widget.WidgetManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class WidgetUpdateWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val httpDataSource: HttpDataSource
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = androidx.room.Room.databaseBuilder(
                context.applicationContext,
                WidgetDatabase::class.java,
                WidgetDatabase.DATABASE_NAME
            ).build()

            val allWidgets = db.widgetDao().getAllWidgets()
            // Collect first list
            val widgetEntities = try {
                db.widgetDao().getWidgetById(1L) // sanity query
                // Query all instances
                val instances = db.widgetDao().getAllAppWidgetIds()
                if (instances.isEmpty()) {
                    db.close()
                    return@withContext Result.success()
                }
                instances
            } catch (_: Exception) {
                emptyList()
            }

            db.close()

            // Update all widgets
            WidgetManager.updateAllWidgets(context)

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
