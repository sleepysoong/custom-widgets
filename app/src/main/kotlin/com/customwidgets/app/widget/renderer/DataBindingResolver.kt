package com.customwidgets.app.widget.renderer

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves dynamic data bindings in widget text templates.
 *
 * Supported tokens:
 * - `{{time}}` -> e.g. "14:30"
 * - `{{date}}` -> e.g. "Aug 30, 2026"
 * - `{{battery}}` -> e.g. "85%"
 * - `{{http:<url>:<jsonpath>}}` -> HTTP API data binding
 * - `{{mcp:<server_name>:<tool_name>:<arg=val>:jsonpath}}` -> Model Context Protocol (MCP) data binding
 */
class DataBindingResolver(
    private val timeProvider: () -> Date = { Date() },
    private val batteryProvider: () -> Int = { 100 }
) {
    private val httpCache = ConcurrentHashMap<String, String>()
    private val mcpCache = ConcurrentHashMap<String, String>()

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    fun updateHttpCache(key: String, value: String) {
        httpCache[key] = value
    }

    fun updateMcpCache(key: String, value: String) {
        mcpCache[key] = value
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

        // 4. MCP tool bindings: {{mcp:server:tool:args:jsonpath}} or {{mcp:server:tool:jsonpath}}
        val mcpRegex = "\\{\\{mcp:([^:}]+):([^:}]+)(?::([^:}]+))?(?::([^}]+))?\\}\\}".toRegex()
        result = mcpRegex.replace(result) { matchResult ->
            val serverName = matchResult.groupValues[1]
            val toolName = matchResult.groupValues[2]
            val args = matchResult.groupValues[3]
            val path = matchResult.groupValues[4]
            val cacheKey = "$serverName:$toolName:$args:$path"
            mcpCache[cacheKey] ?: "Loading..."
        }

        return result
    }
}
