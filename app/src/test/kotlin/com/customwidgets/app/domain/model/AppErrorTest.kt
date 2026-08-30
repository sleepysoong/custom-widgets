package com.customwidgets.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppErrorTest {

    @Test
    fun appError_allTypes_haveUserMessages() {
        val netErr = AppError.NetworkError()
        assertTrue(netErr.userMessage.isNotBlank())

        val apiErr = AppError.ApiError(429, "Rate limit reached")
        assertEquals(429, apiErr.code)
        assertTrue(apiErr.userMessage.contains("429") || apiErr.userMessage.contains("Rate limit"))

        val parseErr = AppError.ParseError("raw response", "Truncated JSON")
        assertEquals("raw response", parseErr.rawResponse)

        val valErr = AppError.ValidationError("description", "Description cannot be empty")
        assertEquals("description", valErr.field)

        val renderErr = AppError.WidgetRenderError(10L, "Render failed")
        assertEquals(10L, renderErr.widgetId)
    }

    @Test
    fun appError_isThrowable() {
        val error: Throwable = AppError.NetworkError("Offline")
        assertNotNull(error.message)
    }
}
