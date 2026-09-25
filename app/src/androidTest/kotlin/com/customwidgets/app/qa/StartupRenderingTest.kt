package com.customwidgets.app.qa

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.customwidgets.app.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StartupRenderingTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun applicationRendersAfterRecreation() {
        capture()
        compose.activityRule.scenario.recreate()
        capture()
    }
    private fun capture() {
        compose.waitForIdle()
        val pixels = compose.onRoot().captureToImage()
        assertTrue(pixels.width > 0 && pixels.height > 0)
        compose.onNodeWithText("커스텀 위젯").assertIsDisplayed()
    }
}
