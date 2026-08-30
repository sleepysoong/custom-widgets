package com.customwidgets.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "widget_instances",
    foreignKeys = [
        ForeignKey(
            entity = WidgetEntity::class,
            parentColumns = ["id"],
            childColumns = ["widgetEntityId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["widgetEntityId"]),
        Index(value = ["appWidgetId"], unique = true)
    ]
)
data class AppWidgetIdEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val widgetEntityId: Long,
    @ColumnInfo(name = "appWidgetId")
    val appWidgetId: Int
)
