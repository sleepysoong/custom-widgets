package com.customwidgets.app.mcp.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Model Context Protocol (MCP) Server configuration.
 */
@Serializable
data class McpServerConfig(
    val id: Long = 0L,
    val name: String,
    val url: String,
    val isEnabled: Boolean = true,
    val apiKey: String? = null
)

/**
 * Tool exposed by an MCP Server.
 */
@Serializable
data class McpTool(
    val name: String,
    val description: String = "",
    val inputSchema: JsonObject? = null
)

// JSON-RPC 2.0 Protocol Models

@Serializable
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: Long,
    val method: String,
    val params: JsonElement? = null
)

@Serializable
data class JsonRpcResponse(
    val jsonrpc: String = "2.0",
    val id: Long? = null,
    val result: JsonElement? = null,
    val error: JsonRpcError? = null
)

@Serializable
data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null
)

@Serializable
data class McpToolCallResult(
    val content: List<McpContent> = emptyList(),
    val isError: Boolean = false
)

@Serializable
data class McpContent(
    val type: String = "text",
    val text: String? = null
)
