package com.customwidgets.app.widget.renderer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DataBindingResolverTest {

    @Test
    fun resolveTimeAndDate() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 30, 14, 30, 0)
        }
        val resolver = DataBindingResolver(
            timeProvider = { calendar.time },
            batteryProvider = { 85 }
        )

        val template = "Time: {{time}}, Date: {{date}}, Batt: {{battery}}"
        val resolved = resolver.resolve(template)

        assertTrue(resolved.contains("14:30"))
        assertTrue(resolved.contains("85%"))
        assertFalse(resolved.contains("{{time}}"))
        assertFalse(resolved.contains("{{battery}}"))
    }

    @Test
    fun resolveHttpBinding_firstPaint_showsLoading() {
        val resolver = DataBindingResolver()
        val template = "Weather: {{http:https://api.example.com/data:temperature}}"
        val resolved = resolver.resolve(template)

        assertEquals("Weather: Loading...", resolved)
    }

    @Test
    fun resolveHttpBinding_cached_showsCachedValue() {
        val resolver = DataBindingResolver()
        val key = "https://api.example.com/data:temperature"
        resolver.updateHttpCache(key, "24°C")

        val template = "Weather: {{http:https://api.example.com/data:temperature}}"
        val resolved = resolver.resolve(template)

        assertEquals("Weather: 24°C", resolved)
    }

    @Test
    fun resolveStaticText_unchanged() {
        val resolver = DataBindingResolver()
        val text = "Simple static text"
        assertEquals(text, resolver.resolve(text))
    }
}
