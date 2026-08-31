package com.customwidgets.app.util

import android.app.Activity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Screen posture and size-class utilities optimized for Galaxy Fold (Z Fold cover vs main display)
 * and tablets.
 */
enum class DeviceScreenType {
    COMPACT, // Standard phone / Fold cover screen (< 600dp)
    MEDIUM,  // Fold unfolded / small tablet (600dp - 840dp)
    EXPANDED // Large tablet / wide unfolded Fold (>= 840dp)
}

object FoldableUtils {

    @Composable
    fun getScreenType(): DeviceScreenType {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp

        return when {
            screenWidth >= 840.dp -> DeviceScreenType.EXPANDED
            screenWidth >= 600.dp -> DeviceScreenType.MEDIUM
            else -> DeviceScreenType.COMPACT
        }
    }

    @Composable
    fun isExpandedScreen(): Boolean {
        return getScreenType() != DeviceScreenType.COMPACT
    }

    @Composable
    fun getGalleryGridColumns(): Int {
        return when (getScreenType()) {
            DeviceScreenType.EXPANDED -> 4
            DeviceScreenType.MEDIUM -> 3
            DeviceScreenType.COMPACT -> 2
        }
    }

    @Composable
    fun getWizardSizeGridColumns(): Int {
        return when (getScreenType()) {
            DeviceScreenType.EXPANDED -> 4
            DeviceScreenType.MEDIUM -> 3
            DeviceScreenType.COMPACT -> 3
        }
    }
}
