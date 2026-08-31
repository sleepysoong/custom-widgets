package com.customwidgets.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Superellipse Squircle shape for modern Liquid Glass UI elements.
 */
class SquircleShape(private val cornerRadius: Dp = 24.dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val radiusPx = with(density) { cornerRadius.toPx().coerceAtMost(size.minDimension / 2f) }
        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(0f, 0f, size.width, size.height),
                    cornerRadius = CornerRadius(radiusPx, radiusPx)
                )
            )
        }
        return Outline.Generic(path)
    }
}

/**
 * Liquid Glass styling tokens and components.
 */
object LiquidGlassDefaults {

    val DefaultCornerRadius = 24.dp
    val ButtonCornerRadius = 16.dp

    fun glassShape(cornerRadius: Dp = DefaultCornerRadius): Shape =
        SquircleShape(cornerRadius)

    @Composable
    fun glassBrush(isDark: Boolean = isSystemInDarkTheme()): Brush {
        return if (isDark) {
            Brush.verticalGradient(
                listOf(
                    Color(0x33FFFFFF), // Subtle top specular reflection
                    Color(0x1A252528), // Frosted dark glass body
                    Color(0x1418181A)  // Deep translucent base
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0xE6FFFFFF), // Bright top reflection
                    Color(0xCCF0F2F5), // Frosted light glass body
                    Color(0xB3E4E7EB)  // Soft translucent base
                )
            )
        }
    }

    @Composable
    fun glassBorderBrush(isDark: Boolean = isSystemInDarkTheme()): Brush {
        return if (isDark) {
            Brush.verticalGradient(
                listOf(
                    Color(0x4DFFFFFF), // Top edge light highlight
                    Color(0x1AFFFFFF), // Subtle sides
                    Color(0x0A000000)  // Darker bottom edge
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0x80FFFFFF),
                    Color(0x33000000),
                    Color(0x1A000000)
                )
            )
        }
    }
}

/**
 * A beautiful Liquid Glass container with squircle corners, frosted translucency,
 * and specular border reflections.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = LiquidGlassDefaults.DefaultCornerRadius,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = LiquidGlassDefaults.glassShape(cornerRadius)
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = modifier
            .clip(shape)
            .background(LiquidGlassDefaults.glassBrush(isDark))
            .border(1.dp, LiquidGlassDefaults.glassBorderBrush(isDark), shape),
        contentAlignment = contentAlignment,
        content = content
    )
}

/**
 * Liquid Glass Button with glass sheen and responsive click interaction.
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val shape = SquircleShape(LiquidGlassDefaults.ButtonCornerRadius)

    val backgroundBrush = if (isPrimary) {
        Brush.horizontalGradient(
            listOf(
                Color(0xFF8E2DE2),
                Color(0xFF4A00E0)
            )
        )
    } else {
        LiquidGlassDefaults.glassBrush(isDark)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundBrush)
            .border(
                1.dp,
                if (isPrimary) Brush.verticalGradient(listOf(Color(0x80FFFFFF), Color(0x20FFFFFF)))
                else LiquidGlassDefaults.glassBorderBrush(isDark),
                shape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * Ambient background with glowing gradient meshes that shine through frosted glass surfaces.
 */
@Composable
fun LiquidGlassBackground(
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()

    val ambientBrush = if (isDark) {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF1E1035), // Purple glow
                Color(0xFF0F0C20), // Dark blue
                Color(0xFF08070D)  // Deep black background
            ),
            radius = 1200f
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFFE8E0F8),
                Color(0xFFE2EAF8),
                Color(0xFFF4F6FA)
            ),
            radius = 1200f
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ambientBrush),
        content = content
    )
}
