package com.customwidgets.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.customwidgets.app.domain.model.WidgetMetadata

@Entity(tableName = "widgets")
data class WidgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val description: String,
    val widthCells: Int,
    val heightCells: Int,
    val definitionJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(appWidgetIds: List<Int> = emptyList()): WidgetMetadata {
        return WidgetMetadata(
            id = id,
            name = name,
            description = description,
            widthCells = widthCells,
            heightCells = heightCells,
            definitionJson = definitionJson,
            createdAt = createdAt,
            updatedAt = updatedAt,
            appWidgetIds = appWidgetIds
        )
    }

    companion object {
        fun fromDomain(domain: WidgetMetadata): WidgetEntity {
            return WidgetEntity(
                id = domain.id,
                name = domain.name,
                description = domain.description,
                widthCells = domain.widthCells,
                heightCells = domain.heightCells,
                definitionJson = domain.definitionJson,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt
            )
        }
    }
}
