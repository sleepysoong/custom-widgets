package com.customwidgets.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.customwidgets.app.data.local.dao.WidgetDao
import com.customwidgets.app.data.local.entity.AppWidgetIdEntity
import com.customwidgets.app.data.local.entity.WidgetEntity

@Database(
    entities = [WidgetEntity::class, AppWidgetIdEntity::class],
    version = 1,
    exportSchema = false
)
abstract class WidgetDatabase : RoomDatabase() {
    abstract fun widgetDao(): WidgetDao

    companion object {
        const val DATABASE_NAME = "custom_widgets.db"
    }
}
