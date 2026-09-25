package com.customwidgets.app.ui.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

enum class GlassMode { Full, BlurOnly, Off }

/** Always choose the strongest effect supported by the running Android version. */
fun resolveGlassMode(sdk: Int): GlassMode = when {
    sdk < 31 -> GlassMode.Off
    sdk < 33 -> GlassMode.BlurOnly
    else -> GlassMode.Full
}

val LocalGlassMode = staticCompositionLocalOf { GlassMode.Off }
internal val LocalGlassBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

@Composable
fun GlassTheme(content: @Composable () -> Unit) {
    val mode = if (LocalInspectionMode.current) GlassMode.Off else resolveGlassMode(Build.VERSION.SDK_INT)
    CompositionLocalProvider(LocalGlassMode provides mode, content = content)
}

/** Draws the real page content as the glass source over a solid white base. */
@Composable
fun GlassHost(
    modifier: Modifier = Modifier,
    fillWindow: Boolean = true,
    backgroundShape: Shape? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val mode = LocalGlassMode.current
    val colors = MaterialTheme.colorScheme
    val backdrop = if (mode != GlassMode.Off) {
        rememberLayerBackdrop {
            drawRect(Color.White)
            drawContent()
        }
    } else {
        null
    }
    Box(if (fillWindow) modifier.fillMaxSize() else modifier) {
        Box(
            Modifier.matchParentSize()
                .then(backgroundShape?.let { Modifier.clip(it) } ?: Modifier)
                .background(Color.White)
        )
        CompositionLocalProvider(
            LocalGlassBackdrop provides backdrop,
            LocalContentColor provides colors.onBackground
        ) { content() }
    }
}

/** Attach only to the page layer that should appear behind floating glass surfaces. */
@Composable
internal fun Modifier.glassSourceLayer(): Modifier {
    val backdrop = LocalGlassBackdrop.current
    return if (backdrop == null) this else layerBackdrop(backdrop)
}

/** Background only: content is drawn after this modifier, so text never enters the shader. */
@Composable
internal fun Modifier.glassMaterial(
    shape: Shape = RoundedCornerShape(28.dp),
    tint: Color = MaterialTheme.colorScheme.surface,
    compact: Boolean = false,
    enabled: Boolean = true,
    outlineColor: Color? = null
): Modifier {
    val backdrop = LocalGlassBackdrop.current
    val mode = LocalGlassMode.current
    val scheme = MaterialTheme.colorScheme
    val surface = if (enabled) tint else scheme.surfaceContainerHighest
    val opacity = if (compact) .82f else .70f
    val outline = outlineColor ?: scheme.outlineVariant.copy(alpha = if (enabled) .65f else .35f)
    val material = if (backdrop == null || mode == GlassMode.Off) {
        Modifier.background(surface.copy(alpha = 1f), shape)
    } else {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                colorControls(saturation = 1.04f)
                blur((if (compact || mode == GlassMode.BlurOnly) 8.dp else 14.dp).toPx())
                // Only supported corner shapes enter lens; icons and text remain foreground.
                if (mode == GlassMode.Full && shape is CornerBasedShape) {
                    val radius = minOf(
                        shape.topStart.toPx(size, this), shape.topEnd.toPx(size, this),
                        shape.bottomStart.toPx(size, this), shape.bottomEnd.toPx(size, this),
                        size.minDimension / 2f
                    )
                    lens(minOf(14.dp.toPx(), radius), minOf(22.dp.toPx(), size.minDimension))
                }
            },
            shadow = null,
            onDrawSurface = { drawRect(surface.copy(alpha = opacity)) }
        )
    }
    return then(material).border(1.dp, outline, shape).clip(shape)
}
