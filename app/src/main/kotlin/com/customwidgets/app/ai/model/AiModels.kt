package com.customwidgets.app.ai.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OpenAI API Configuration.
 */
@Serializable
data class AiConfig(
    val apiKey: String = "",
    val model: String = OPENAI_DEFAULT_MODEL,
    val temperature: Double = 0.7,
    val maxTokens: Int = 2048,
    val customBaseUrl: String? = null // For testing with MockWebServer
) {
    val baseUrl: String
        get() = customBaseUrl ?: OPENAI_BASE_URL

    val cleanBaseUrl: String
        get() = baseUrl.trimEnd('/')

    companion object {
        const val OPENAI_BASE_URL = "https://api.openai.com"
        const val OPENAI_DEFAULT_MODEL = "gpt-4o-mini"
        val SUPPORTED_MODELS = listOf(
            "gpt-4o-mini" to "Fast & Cost-effective (Recommended)",
            "gpt-4o" to "High Intelligence & Rich Design",
            "o3-mini" to "Advanced Reasoning",
            "gpt-4-turbo" to "Legacy GPT-4 Turbo"
        )
    }
}

@Serializable
data class ChatMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val stream: Boolean = false,
    @SerialName("response_format") val responseFormat: ResponseFormat? = null
)

@Serializable
data class ResponseFormat(
    val type: String = "json_object"
)

@Serializable
data class ChatCompletionResponse(
    val choices: List<Choice> = emptyList()
)

@Serializable
data class Choice(
    val message: ChatMessage? = null,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class ChatChunk(
    val choices: List<ChunkChoice> = emptyList()
)

@Serializable
data class ChunkChoice(
    val delta: ChunkDelta = ChunkDelta(),
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class ChunkDelta(
    val content: String? = null
)

// Typed AI exceptions
open class AiApiException(val code: Int, override val message: String) : Exception("OpenAI API Error $code: $message")
class AiAuthException(message: String) : AiApiException(401, message)
class AiRateLimitException(message: String) : AiApiException(429, message)
class AiServerException(code: Int, message: String) : AiApiException(code, message)
