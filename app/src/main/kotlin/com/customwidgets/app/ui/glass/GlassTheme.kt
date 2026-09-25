package com.customwidgets.app.ui.glass

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens

/** Persisted names are deliberately independent of the API-specific effective mode. */
enum class GlassPreference { Auto, Reduced, Off }
enum class GlassMode { Full, BlurOnly, Off }

fun resolveGlassMode(sdk: Int, preference: GlassPreference): GlassMode = when {
    preference == GlassPreference.Off || sdk < 31 -> GlassMode.Off
    preference == GlassPreference.Reduced || sdk < 33 -> GlassMode.BlurOnly
    else -> GlassMode.Full
}

private const val MODE_KEY = "visual_effects"
// Emergency release override. Unlike a UI setting, this also works before the first frame.
private const val FORCE_EFFECTS_OFF = false

@Stable
class GlassSettings internal constructor(private val preferences: SharedPreferences) {
    var preference by mutableStateOf(read())
        private set
    private fun read() = runCatching {
        GlassPreference.valueOf(preferences.getString(MODE_KEY, "Auto") ?: "Auto")
    }.getOrDefault(GlassPreference.Auto)
    internal fun refresh() { preference = read() }
    fun select(value: GlassPreference) {
        preference = value
        preferences.edit { putString(MODE_KEY, value.name) }
    }
}

val LocalGlassSettings = staticCompositionLocalOf<GlassSettings?> { null }
val LocalGlassMode = staticCompositionLocalOf { GlassMode.Off }
internal val LocalGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }

@Composable
fun GlassTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { context.getSharedPreferences("glass_appearance", Context.MODE_PRIVATE) }
    val settings = remember(preferences) { GlassSettings(preferences) }
    DisposableEffect(preferences, settings) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == MODE_KEY) settings.refresh()
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val mode = if (FORCE_EFFECTS_OFF || LocalInspectionMode.current) GlassMode.Off
        else resolveGlassMode(Build.VERSION.SDK_INT, settings.preference)
    CompositionLocalProvider(LocalGlassSettings provides settings, LocalGlassMode provides mode) {
        content()
    }
}

/** Source and consumers are siblings. Never put layerBackdrop on the content container. */
@Composable
fun GlassHost(
    modifier: Modifier = Modifier,
    fillWindow: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val mode = LocalGlassMode.current
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < .5f
    val base = if (dark) Color(0xFF0B0B0D) else Color(0xFFF7F7F9)
    val brush = Brush.linearGradient(listOf(base, colors.primaryContainer, base, colors.surfaceContainer))
    val backdrop = if (mode != GlassMode.Off) rememberLayerBackdrop() else null
    Box(if (fillWindow) modifier.fillMaxSize() else modifier) {
        Box(
            Modifier.matchParentSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                .background(brush)
        )
        CompositionLocalProvider(
            LocalGlassBackdrop provides backdrop,
            LocalContentColor provides colors.onBackground
        ) { content() }
    }
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
    val dark = scheme.background.luminance() < .5f
    val surface = if (enabled) tint else scheme.surfaceContainerHighest
    val opacity = if (compact) .78f else if (dark) .72f else .64f
    val outline = outlineColor ?: scheme.outlineVariant.copy(alpha = if (enabled) .65f else .35f)
    val material = if (backdrop == null || mode == GlassMode.Off) {
        Modifier.background(surface.copy(alpha = 1f), shape)
    } else {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                colorControls(saturation = 1.05f)
                blur((if (compact || mode == GlassMode.BlurOnly) 4.dp else 8.dp).toPx())
                // Only supported corner shapes enter lens; icons and text remain foreground.
                if (mode == GlassMode.Full && shape is CornerBasedShape) {
                    val radius = minOf(
                        shape.topStart.toPx(size, this), shape.topEnd.toPx(size, this),
                        shape.bottomStart.toPx(size, this), shape.bottomEnd.toPx(size, this),
                        size.minDimension / 2f
                    )
                    lens(minOf(8.dp.toPx(), radius), minOf(12.dp.toPx(), size.minDimension))
                }
            },
            shadow = null,
            onDrawSurface = { drawRect(surface.copy(alpha = opacity)) }
        )
    }
    return then(material).border(1.dp, outline, shape).clip(shape)
}
