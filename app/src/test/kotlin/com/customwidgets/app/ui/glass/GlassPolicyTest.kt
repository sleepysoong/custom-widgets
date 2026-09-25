package com.customwidgets.app.ui.glass

import org.junit.Assert.assertEquals
import org.junit.Test

class GlassPolicyTest {
    @Test fun alwaysUsesStrongestEffectSupportedByAndroidVersion() {
        assertEquals(GlassMode.Off, resolveGlassMode(30))
        assertEquals(GlassMode.BlurOnly, resolveGlassMode(31))
        assertEquals(GlassMode.BlurOnly, resolveGlassMode(32))
        for (api in 33..37) {
            assertEquals(GlassMode.Full, resolveGlassMode(api))
        }
    }
}
