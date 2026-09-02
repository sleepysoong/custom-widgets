package com.customwidgets.app.widget.update

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.customwidgets.app.data.local.WidgetDatabase
import com.customwidgets.app.mcp.McpClient
import com.customwidgets.app.mcp.McpServerRepository
import com.customwidgets.app.widget.WidgetManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@HiltWorker
class WidgetUpdateWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val httpDataSource: HttpDataSource,
    private val mcpRepository: McpServerRepository,
    private val mcpClient: McpClient
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = androidx.room.Room.databaseBuilder(
                context.applicationContext,
                WidgetDatabase::class.java,
                WidgetDatabase.DATABASE_NAME
            ).build()

            val instances = try {
                db.widgetDao().getAllAppWidgetIds()
            } catch (_: Exception) {
                emptyList()
            }

            db.close()

            if (instances.isEmpty()) {
                return@withContext Result.success()
            }

            // Update all widgets
            WidgetManager.updateAllWidgets(context)

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
