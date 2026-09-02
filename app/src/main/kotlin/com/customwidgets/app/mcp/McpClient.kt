package com.customwidgets.app.mcp

import com.customwidgets.app.mcp.model.JsonRpcRequest
import com.customwidgets.app.mcp.model.JsonRpcResponse
import com.customwidgets.app.mcp.model.McpServerConfig
import com.customwidgets.app.mcp.model.McpTool
import com.customwidgets.app.mcp.model.McpToolCallResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class McpClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    private val idCounter = AtomicLong(1)

    /**
     * Queries an MCP Server for its list of available tools.
     */
    suspend fun listTools(server: McpServerConfig): List<McpTool> = withContext(Dispatchers.IO) {
        val request = JsonRpcRequest(
            id = idCounter.getAndIncrement(),
            method = "tools/list",
            params = buildJsonObject {}
        )

        try {
            val response = executeRpc(server, request) ?: return@withContext emptyList()
            val toolsArray = response.result?.jsonObject?.get("tools")?.jsonArray
                ?: return@withContext emptyList()

            toolsArray.mapNotNull { element ->
                try {
                    val obj = element.jsonObject
                    McpTool(
                        name = obj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                        description = obj["description"]?.jsonPrimitive?.content ?: "",
                        inputSchema = obj["inputSchema"]?.jsonObject
                    )
                } catch (_: Exception) {
                    null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Executes an MCP Tool call and returns the text content of the result.
     */
    suspend fun callTool(
        server: McpServerConfig,
        toolName: String,
        arguments: JsonObject
    ): String? = withContext(Dispatchers.IO) {
        val params = buildJsonObject {
            put("name", kotlinx.serialization.json.JsonPrimitive(toolName))
            put("arguments", arguments)
        }

        val request = JsonRpcRequest(
            id = idCounter.getAndIncrement(),
            method = "tools/call",
            params = params
        )

        try {
            val response = executeRpc(server, request) ?: return@withContext null
            val resultElement = response.result ?: return@withContext null

            val toolResult = json.decodeFromJsonElement<McpToolCallResult>(resultElement)
            toolResult.content.firstOrNull { it.type == "text" }?.text
        } catch (_: Exception) {
            null
        }
    }

    private fun executeRpc(server: McpServerConfig, rpcRequest: JsonRpcRequest): JsonRpcResponse? {
        val bodyString = json.encodeToString(rpcRequest)
        val httpRequest = Request.Builder()
            .url(server.url)
            .apply {
                if (!server.apiKey.isNullOrBlank()) {
                    addHeader("Authorization", "Bearer ${server.apiKey}")
                }
                addHeader("Content-Type", "application/json")
                addHeader("Accept", "application/json, text/event-stream")
            }
            .post(bodyString.toRequestBody(mediaType))
            .build()

        val response = okHttpClient.newCall(httpRequest).execute()
        if (!response.isSuccessful) {
            response.close()
            return null
        }

        val responseBody = response.body?.string() ?: return null
        return json.decodeFromString<JsonRpcResponse>(responseBody)
    }
}
