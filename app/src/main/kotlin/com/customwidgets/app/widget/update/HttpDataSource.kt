package com.customwidgets.app.widget.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpDataSource @Inject constructor(
    private val client: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val cache = ConcurrentHashMap<String, CachedValue>()

    data class CachedValue(val value: String, val timestamp: Long)

    suspend fun fetchAndExtract(url: String, jsonPath: String): String? = withContext(Dispatchers.IO) {
        val cacheKey = "$url:$jsonPath"
        val cached = cache[cacheKey]
        val now = System.currentTimeMillis()

        // 1-minute in-memory cache
        if (cached != null && (now - cached.timestamp) < TimeUnit.MINUTES.toMillis(1)) {
            return@withContext cached.value
        }

        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            val rootElement = json.parseToJsonElement(body)
            val extracted = extractByPath(rootElement, jsonPath)

            extracted?.let {
                cache[cacheKey] = CachedValue(it, now)
            }
            extracted
        } catch (_: Exception) {
            null
        }
    }

    fun extractByPath(root: JsonElement, path: String): String? {
        val segments = path.split(".")
        var current: JsonElement? = root

        for (segment in segments) {
            if (current == null) return null
            current = when {
                current is JsonObject -> current[segment]
                current is JsonArray -> {
                    val index = segment.toIntOrNull() ?: return null
                    current.getOrNull(index)
                }
                else -> null
            }
        }

        return current?.jsonPrimitive?.content
    }
}
