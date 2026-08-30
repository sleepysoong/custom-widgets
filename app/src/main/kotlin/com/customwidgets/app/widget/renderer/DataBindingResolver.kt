package com.customwidgets.app.widget.renderer

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves dynamic data bindings in widget text templates.
 *
 * Supported built-in tokens:
 * - `{{time}}` -> e.g. "14:30" or "02:30 PM"
 * - `{{date}}` -> e.g. "Aug 30, 2026"
 * - `{{battery}}` -> e.g. "85%"
 * - `{{http:<url>:<jsonpath>}}` -> resolved via cached value, or "Loading..." on first paint
 */
class DataBindingResolver(
    private val timeProvider: () -> Date = { Date() },
    private val batteryProvider: () -> Int = { 100 }
) {
    // In-memory cache for HTTP data bindings
    private val httpCache = ConcurrentHashMap<String, String>()

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    fun updateHttpCache(key: String, value: String) {
        httpCache[key] = value
    }

    fun resolve(text: String): String {
        if (!text.contains("{{")) return text

        var result = text

        // 1. Built-in time/date
        if (result.contains("{{time}}")) {
            result = result.replace("{{time}}", timeFormat.format(timeProvider()))
        }
        if (result.contains("{{date}}")) {
            result = result.replace("{{date}}", dateFormat.format(timeProvider()))
        }

        // 2. Battery
        if (result.contains("{{battery}}")) {
            result = result.replace("{{battery}}", "${batteryProvider()}%")
        }

        // 3. HTTP data bindings: {{http:url:jsonpath}}
        val httpRegex = "\\{\\{http:([^:}]+):([^}]+)\\}\\}".toRegex()
        result = httpRegex.replace(result) { matchResult ->
            val url = matchResult.groupValues[1]
            val path = matchResult.groupValues[2]
            val cacheKey = "$url:$path"
            httpCache[cacheKey] ?: "Loading..."
        }

        return result
    }
}
