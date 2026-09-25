package com.customwidgets.app.qa

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Run on a hardware accelerated Android target. JVM tests cannot detect RenderThread cycles. */
class GlassRenderingTest {
    @get:Rule val compose = createAndroidComposeRule<GlassCatalogActivity>()

    @Test fun firstFrameAndRecreationRender() {
        compose.onNodeWithTag("glass-card").performScrollTo()
        assertPixels()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("glass-card").performScrollTo()
        assertPixels()
    }

    @Test fun actionsInputAndDialogKeepMaterialBehavior() {
        compose.onNodeWithTag("glass-button").performScrollTo().performClick()
        compose.onNodeWithTag("click-count").assertTextEquals("Clicks: 1")
        compose.onNodeWithTag("disabled-button").assertIsNotEnabled()
        compose.onNodeWithTag("glass-input").performScrollTo().performTextInput("sample")
        compose.onNodeWithTag("glass-input").assertTextContains("sample")
        compose.onNodeWithTag("glass-switch").performScrollTo().performClick().assertIsOn()
        compose.onNodeWithTag("open-dialog").performScrollTo().performClick()
        compose.onNodeWithText("Glass dialog").assertIsDisplayed()
        val modal = compose.onNode(isDialog()).captureToImage()
        assertTrue(modal.width > 0 && modal.height > 0)
        compose.onNodeWithTag("close-dialog").performClick()
        compose.onNodeWithText("Glass dialog").assertDoesNotExist()
    }

    @Test fun sliderSupportsAccessibilityValueChanges() {
        compose.onNodeWithTag("glass-slider").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { set -> set(.8f) }
        compose.onNodeWithTag("glass-slider").assertRangeInfoEquals(
            androidx.compose.ui.semantics.ProgressBarRangeInfo(.8f, 0f..1f)
        )
    }

    private fun assertPixels() {
        compose.waitForIdle()
        val image = compose.onRoot().captureToImage()
        assertTrue(image.width > 0 && image.height > 0)
        compose.onNodeWithTag("glass-card").assertIsDisplayed()
    }
}
