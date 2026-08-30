package com.customwidgets.app.ai

import com.customwidgets.app.ai.model.AiApiException
import com.customwidgets.app.ai.model.AiAuthException
import com.customwidgets.app.ai.model.AiConfig
import com.customwidgets.app.ai.model.AiRateLimitException
import com.customwidgets.app.ai.model.AiServerException
import com.customwidgets.app.ai.model.ChatChunk
import com.customwidgets.app.ai.model.ChatCompletionRequest
import com.customwidgets.app.ai.model.ChatCompletionResponse
import com.customwidgets.app.ai.model.ChatMessage
import com.customwidgets.app.ai.model.ResponseFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSource
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiService @Inject constructor(
    private val client: OkHttpClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
    }

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Streams chat completion tokens from an OpenAI-compatible endpoint.
     * Yields individual string deltas as they arrive via Server-Sent Events (SSE).
     */
    fun streamChatCompletion(
        messages: List<ChatMessage>,
        config: AiConfig,
        useJsonMode: Boolean = true
    ): Flow<String> = flow {
        val requestBody = ChatCompletionRequest(
            model = config.model,
            messages = messages,
            temperature = config.temperature,
            maxTokens = config.maxTokens,
            stream = true,
            responseFormat = if (useJsonMode) ResponseFormat("json_object") else null
        )

        val url = "${config.cleanBaseUrl}/v1/chat/completions"
        val httpRequest = Request.Builder()
            .url(url)
            .apply {
                if (config.apiKey.isNotBlank()) {
                    addHeader("Authorization", "Bearer ${config.apiKey}")
                }
                addHeader("Content-Type", "application/json")
                addHeader("Accept", "text/event-stream")
            }
            .post(json.encodeToString(requestBody).toRequestBody(mediaType))
            .build()

        val response = client.newCall(httpRequest).execute()

        if (!response.isSuccessful) {
            val code = response.code
            val errorBody = response.body?.string() ?: "Unknown error"
            response.close()

            // Fallback: If 400 occurred and json_mode was requested, retry without json_mode
            if (code == 400 && useJsonMode) {
                emitAll(streamChatCompletion(messages, config, useJsonMode = false))
                return@flow
            }

            throw mapHttpError(code, errorBody)
        }

        val source: BufferedSource = response.body?.source()
            ?: throw AiApiException(response.code, "Empty response body")

        try {
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: continue
                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ").trim()
                    if (data == "[DONE]") break
                    if (data.isNotBlank()) {
                        try {
                            val chunk = json.decodeFromString<ChatChunk>(data)
                            val content = chunk.choices.firstOrNull()?.delta?.content
                            if (!content.isNullOrEmpty()) {
                                emit(content)
                            }
                        } catch (_: Exception) {
                            // Skip non-chunk lines
                        }
                    }
                }
            }
        } finally {
            response.close()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Non-streaming synchronous chat completion.
     */
    suspend fun chatCompletionSync(
        messages: List<ChatMessage>,
        config: AiConfig,
        useJsonMode: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        val requestBody = ChatCompletionRequest(
            model = config.model,
            messages = messages,
            temperature = config.temperature,
            maxTokens = config.maxTokens,
            stream = false,
            responseFormat = if (useJsonMode) ResponseFormat("json_object") else null
        )

        val url = "${config.cleanBaseUrl}/v1/chat/completions"
        val httpRequest = Request.Builder()
            .url(url)
            .apply {
                if (config.apiKey.isNotBlank()) {
                    addHeader("Authorization", "Bearer ${config.apiKey}")
                }
                addHeader("Content-Type", "application/json")
            }
            .post(json.encodeToString(requestBody).toRequestBody(mediaType))
            .build()

        val response = client.newCall(httpRequest).execute()
        val code = response.code
        val bodyString = response.body?.string() ?: ""
        response.close()

        if (!response.isSuccessful) {
            if (code == 400 && useJsonMode) {
                // Fallback without json_mode
                return@withContext chatCompletionSync(messages, config, useJsonMode = false)
            }
            throw mapHttpError(code, bodyString)
        }

        val parsed = json.decodeFromString<ChatCompletionResponse>(bodyString)
        parsed.choices.firstOrNull()?.message?.content
            ?: throw AiApiException(200, "Empty content in response")
    }

    private fun mapHttpError(code: Int, body: String): AiApiException = when (code) {
        401 -> AiAuthException("Invalid or missing API key: $body")
        429 -> AiRateLimitException("Rate limit exceeded: $body")
        in 500..599 -> AiServerException(code, "Server error: $body")
        else -> AiApiException(code, body)
    }
}
