package com.customwidgets.app.data.repository

import com.customwidgets.app.data.local.dao.WidgetDao
import com.customwidgets.app.data.local.entity.AppWidgetIdEntity
import com.customwidgets.app.data.local.entity.WidgetEntity
import com.customwidgets.app.domain.model.WidgetMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetRepository @Inject constructor(
    private val widgetDao: WidgetDao
) {
    fun getAllWidgets(): Flow<List<WidgetMetadata>> {
        return widgetDao.getAllWidgets().map { entities ->
            entities.map { entity ->
                val appWidgetIds = widgetDao.getAppWidgetIdsForWidget(entity.id)
                entity.toDomain(appWidgetIds)
            }
        }
    }

    suspend fun getWidgetById(id: Long): WidgetMetadata? {
        val entity = widgetDao.getWidgetById(id) ?: return null
        val appWidgetIds = widgetDao.getAppWidgetIdsForWidget(id)
        return entity.toDomain(appWidgetIds)
    }

    suspend fun getWidgetByAppWidgetId(appWidgetId: Int): WidgetMetadata? {
        val entity = widgetDao.getWidgetByAppWidgetId(appWidgetId) ?: return null
        val appWidgetIds = widgetDao.getAppWidgetIdsForWidget(entity.id)
        return entity.toDomain(appWidgetIds)
    }

    suspend fun saveWidget(metadata: WidgetMetadata): Long {
        val entity = WidgetEntity.fromDomain(metadata)
        return widgetDao.insertWidget(entity)
    }

    suspend fun updateWidget(metadata: WidgetMetadata) {
        val entity = WidgetEntity.fromDomain(metadata).copy(updatedAt = System.currentTimeMillis())
        widgetDao.updateWidget(entity)
    }

    suspend fun deleteWidget(id: Long) {
        widgetDao.deleteWidgetById(id)
    }

    suspend fun linkAppWidgetId(widgetId: Long, appWidgetId: Int) {
        widgetDao.insertAppWidgetId(
            AppWidgetIdEntity(
                widgetEntityId = widgetId,
                appWidgetId = appWidgetId
            )
        )
    }

    suspend fun unlinkAppWidgetId(appWidgetId: Int) {
        widgetDao.deleteAppWidgetId(appWidgetId)
    }

    suspend fun getAllLinkedAppWidgetIds(): List<Int> {
        return widgetDao.getAllAppWidgetIds()
    }
}
