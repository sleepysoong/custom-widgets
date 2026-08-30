package com.customwidgets.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.customwidgets.app.data.local.entity.AppWidgetIdEntity
import com.customwidgets.app.data.local.entity.WidgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WidgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWidget(widget: WidgetEntity): Long

    @Update
    suspend fun updateWidget(widget: WidgetEntity)

    @Delete
    suspend fun deleteWidget(widget: WidgetEntity)

    @Query("DELETE FROM widgets WHERE id = :id")
    suspend fun deleteWidgetById(id: Long)

    @Query("SELECT * FROM widgets WHERE id = :id")
    suspend fun getWidgetById(id: Long): WidgetEntity?

    @Query("SELECT * FROM widgets ORDER BY updatedAt DESC")
    fun getAllWidgets(): Flow<List<WidgetEntity>>

    @Query("""
        SELECT w.* FROM widgets w
        INNER JOIN widget_instances wi ON w.id = wi.widgetEntityId
        WHERE wi.appWidgetId = :appWidgetId
    """)
    suspend fun getWidgetByAppWidgetId(appWidgetId: Int): WidgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppWidgetId(entity: AppWidgetIdEntity): Long

    @Query("DELETE FROM widget_instances WHERE appWidgetId = :appWidgetId")
    suspend fun deleteAppWidgetId(appWidgetId: Int)

    @Query("SELECT appWidgetId FROM widget_instances WHERE widgetEntityId = :widgetEntityId")
    suspend fun getAppWidgetIdsForWidget(widgetEntityId: Long): List<Int>

    @Query("SELECT appWidgetId FROM widget_instances")
    suspend fun getAllAppWidgetIds(): List<Int>
}
