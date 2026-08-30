package com.customwidgets.app.widget.renderer

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GradientRendererTest {

    @Test
    fun parseColor_validHex() {
        val color = GradientRenderer.parseColor("#FF6200EE")
        assertEquals(Color.parseColor("#FF6200EE"), color)
    }

    @Test
    fun parseColor_invalidHex_returnsFallback() {
        val fallback = Color.RED
        val color = GradientRenderer.parseColor("not_a_color", fallback)
        assertEquals(fallback, color)
    }

    @Test
    fun createGradientBitmap_vertical() {
        val bitmap = GradientRenderer.createGradientBitmap(
            colors = listOf("#FF6200EE", "#FF3700B3"),
            orientation = "vertical",
            width = 100,
            height = 100
        )
        assertEquals(100, bitmap.width)
        assertEquals(100, bitmap.height)
    }

    @Test
    fun createGradientBitmap_horizontal() {
        val bitmap = GradientRenderer.createGradientBitmap(
            colors = listOf("#FF000000", "#FFFFFFFF"),
            orientation = "horizontal",
            width = 200,
            height = 100
        )
        assertEquals(200, bitmap.width)
        assertEquals(100, bitmap.height)
    }
}
