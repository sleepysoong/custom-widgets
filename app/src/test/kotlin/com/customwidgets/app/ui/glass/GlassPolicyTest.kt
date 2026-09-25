package com.customwidgets.app.ui.glass

import org.junit.Assert.assertEquals
import org.junit.Test

class GlassPolicyTest {
    @Test fun autoUsesOnlySupportedEffects() {
        assertEquals(GlassMode.Off, resolveGlassMode(30, GlassPreference.Auto))
        assertEquals(GlassMode.BlurOnly, resolveGlassMode(31, GlassPreference.Auto))
        assertEquals(GlassMode.BlurOnly, resolveGlassMode(32, GlassPreference.Auto))
        assertEquals(GlassMode.Full, resolveGlassMode(33, GlassPreference.Auto))
        assertEquals(GlassMode.Full, resolveGlassMode(37, GlassPreference.Auto))
    }
    @Test fun explicitPreferencesOverrideAutomaticMode() {
        for (api in 31..37) {
            assertEquals(GlassMode.BlurOnly, resolveGlassMode(api, GlassPreference.Reduced))
            assertEquals(GlassMode.Off, resolveGlassMode(api, GlassPreference.Off))
        }
    }
}
