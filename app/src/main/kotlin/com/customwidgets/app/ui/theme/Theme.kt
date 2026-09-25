package com.customwidgets.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WhiteLiquidGlassColorScheme = lightColorScheme(
    primary = AppBlue,
    onPrimary = AppWhite,
    primaryContainer = AppBlueSoft,
    onPrimaryContainer = Color(0xFF103A73),
    secondary = Color(0xFF285A96),
    onSecondary = AppWhite,
    secondaryContainer = AppBlueSoft,
    onSecondaryContainer = Color(0xFF193B68),
    tertiary = Color(0xFF1468D4),
    onTertiary = AppWhite,
    tertiaryContainer = AppBlueTint,
    onTertiaryContainer = Color(0xFF103A73),
    background = AppCanvas,
    onBackground = AppInk,
    surface = AppCanvas,
    onSurface = AppInk,
    surfaceVariant = AppSurface,
    onSurfaceVariant = AppMutedInk,
    surfaceContainerLowest = AppWhite,
    surfaceContainerLow = AppSurfaceSubtle,
    surfaceContainer = AppSurface,
    surfaceContainerHigh = AppSurfaceRaised,
    surfaceContainerHighest = AppSurfaceSelected,
    outline = AppOutline,
    outlineVariant = AppOutlineSoft,
    error = AppError,
    onError = AppWhite,
    errorContainer = AppErrorSoft,
    onErrorContainer = Color(0xFF7A271A)
)

@Composable
fun CustomWidgetsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WhiteLiquidGlassColorScheme,
        shapes = ExpressiveShapes,
        typography = Typography,
        content = {
            com.customwidgets.app.ui.glass.GlassTheme {
                com.customwidgets.app.ui.glass.GlassHost { content() }
            }
        }
    )
}
