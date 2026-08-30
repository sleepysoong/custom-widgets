package com.customwidgets.app.widget.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader

/**
 * Generates Bitmaps for gradient backgrounds in Glance widgets.
 */
object GradientRenderer {

    fun createGradientBitmap(
        colors: List<String>,
        orientation: String = "vertical",
        width: Int = 400,
        height: Int = 400
    ): Bitmap {
        val safeWidth = if (width > 0) width else 400
        val safeHeight = if (height > 0) height else 400

        val parsedColors = colors.map { parseColor(it) }.toIntArray()
        val finalColors = if (parsedColors.size < 2) {
            intArrayOf(parsedColors.firstOrNull() ?: Color.DKGRAY, Color.BLACK)
        } else {
            parsedColors
        }

        val bitmap = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val (x0, y0, x1, y1) = when (orientation.lowercase()) {
            "horizontal" -> listOf(0f, 0f, safeWidth.toFloat(), 0f)
            else -> listOf(0f, 0f, 0f, safeHeight.toFloat()) // vertical default
        }

        paint.shader = LinearGradient(
            x0, y0, x1, y1,
            finalColors,
            null,
            Shader.TileMode.CLAMP
        )

        canvas.drawRect(0f, 0f, safeWidth.toFloat(), safeHeight.toFloat(), paint)
        return bitmap
    }

    fun parseColor(hex: String, fallback: Int = Color.WHITE): Int {
        return try {
            val clean = hex.trim()
            if (clean.startsWith("#")) {
                Color.parseColor(clean)
            } else {
                Color.parseColor("#$clean")
            }
        } catch (_: Exception) {
            fallback
        }
    }
}
