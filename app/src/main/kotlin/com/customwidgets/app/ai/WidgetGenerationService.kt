package com.customwidgets.app.ai

import com.customwidgets.app.ai.model.AiApiException
import com.customwidgets.app.ai.model.AiAuthException
import com.customwidgets.app.ai.model.AiConfig
import com.customwidgets.app.ai.model.AiRateLimitException
import com.customwidgets.app.ai.model.ChatMessage
import com.customwidgets.app.ai.prompt.WidgetPromptBuilder
import com.customwidgets.app.domain.model.AppError
import com.customwidgets.app.domain.model.WidgetDefinition
import com.customwidgets.app.mcp.McpClient
import com.customwidgets.app.mcp.McpServerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

sealed interface GenerationState {
    data object Idle : GenerationState
    data object Loading : GenerationState
    data class Streaming(val partialText: String) : GenerationState
    data class Success(val definition: WidgetDefinition, val rawJson: String) : GenerationState
    data class Error(val error: AppError) : GenerationState
}

@Singleton
class WidgetGenerationService @Inject constructor(
    private val aiService: AiService,
    private val mcpRepository: McpServerRepository,
    private val mcpClient: McpClient
) {
    /**
     * Streams AI widget generation for the user's description and grid size,
     * augmenting the system prompt with active MCP server tools.
     */
    fun generateWidget(
        description: String,
        widthCells: Int,
        heightCells: Int,
        config: AiConfig
    ): Flow<GenerationState> = flow {
        emit(GenerationState.Loading)

        // Query active MCP servers and their exposed tools
        val mcpTools = try {
            val enabledServers = mcpRepository.getAllEnabledServers()
            enabledServers.map { server ->
                server.name to mcpClient.listTools(server)
            }.filter { it.second.isNotEmpty() }
        } catch (_: Exception) {
            emptyList()
        }

        val systemPrompt = WidgetPromptBuilder.buildSystemPrompt(mcpTools)
        val userPrompt = WidgetPromptBuilder.buildUserPrompt(description, widthCells, heightCells)
        val fewShot = WidgetPromptBuilder.buildFewShotExamples()

        val messages = buildList {
            add(ChatMessage("system", systemPrompt))
            addAll(fewShot)
            add(ChatMessage("user", userPrompt))
        }

        val accumulated = StringBuilder()

        try {
            aiService.streamChatCompletion(messages, config).collect { delta ->
                accumulated.append(delta)
                emit(GenerationState.Streaming(accumulated.toString()))
            }

            val rawOutput = accumulated.toString()
            val parseResult = WidgetPromptBuilder.parseAiResponse(rawOutput)

            if (parseResult.isSuccess) {
                emit(GenerationState.Success(parseResult.getOrThrow(), rawOutput))
            } else {
                val err = parseResult.exceptionOrNull()
                emit(
                    GenerationState.Error(
                        AppError.ParseError(
                            rawResponse = rawOutput,
                            userMessage = "Could not parse AI response as valid widget. ${err?.message ?: ""}",
                            cause = err
                        )
                    )
                )
            }
        } catch (e: Exception) {
            val appError = when (e) {
                is AiAuthException -> AppError.ApiError(401, "Invalid API Key: ${e.message}", e)
                is AiRateLimitException -> AppError.ApiError(429, "Rate limit reached. Please wait and try again.", e)
                is AiApiException -> AppError.ApiError(e.code, e.message, e)
                is IOException -> AppError.NetworkError("Network error: ${e.message}", e)
                else -> AppError.ApiError(500, e.message ?: "Unexpected error", e)
            }
            emit(GenerationState.Error(appError))
        }
    }
}
