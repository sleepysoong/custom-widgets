package com.customwidgets.app.widget.update

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class WidgetUpdateSchedulerTest {

    @Test
    fun periodicWorkRequest_intervalIsAtLeast15Minutes() {
        val request = WidgetUpdateScheduler.buildPeriodicWorkRequest()
        val durationMillis = request.workSpec.intervalDuration

        // Must be >= 15 minutes in milliseconds
        val minExpectedMillis = TimeUnit.MINUTES.toMillis(15)
        assertTrue(
            "Expected interval >= 15 min ($minExpectedMillis ms), but was $durationMillis ms",
            durationMillis >= minExpectedMillis
        )
    }

    @Test
    fun httpDataSource_extractByPath_nestedObject() {
        val jsonStr = """{"data": {"temperature": "22°C", "humidity": "45%"}}"""
        val element = Json.parseToJsonElement(jsonStr)
        val dataSource = HttpDataSource(okhttp3.OkHttpClient())

        val temp = dataSource.extractByPath(element, "data.temperature")
        assertEquals("22°C", temp)

        val hum = dataSource.extractByPath(element, "data.humidity")
        assertEquals("45%", hum)
    }

    @Test
    fun httpDataSource_extractByPath_nestedArray() {
        val jsonStr = """{"current_condition": [{"temp_C": "25", "weatherDesc": [{"value": "Sunny"}]}]}"""
        val element = Json.parseToJsonElement(jsonStr)
        val dataSource = HttpDataSource(okhttp3.OkHttpClient())

        val temp = dataSource.extractByPath(element, "current_condition.0.temp_C")
        assertEquals("25", temp)

        val desc = dataSource.extractByPath(element, "current_condition.0.weatherDesc.0.value")
        assertEquals("Sunny", desc)
    }

    @Test
    fun httpDataSource_extractByPath_invalidPath_returnsNull() {
        val jsonStr = """{"data": {"val": 10}}"""
        val element = Json.parseToJsonElement(jsonStr)
        val dataSource = HttpDataSource(okhttp3.OkHttpClient())

        val notFound = dataSource.extractByPath(element, "data.non_existent.field")
        assertNull(notFound)
    }
}
