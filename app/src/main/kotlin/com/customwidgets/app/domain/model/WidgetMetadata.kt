package com.customwidgets.app.domain.model

/**
 * Domain model representing metadata and stored definition of a custom widget.
 */
data class WidgetMetadata(
    val id: Long = 0L,
    val name: String,
    val description: String,
    val widthCells: Int,
    val heightCells: Int,
    val definitionJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val appWidgetIds: List<Int> = emptyList()
) {
    val sizeLabel: String
        get() = "${widthCells}x${heightCells}"
}
