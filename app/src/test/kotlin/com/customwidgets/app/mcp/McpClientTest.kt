package com.customwidgets.app.mcp

import com.customwidgets.app.mcp.model.McpServerConfig
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class McpClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: McpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = McpClient(OkHttpClient())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun listTools_parsesJsonRpcResponse() = runTest {
        val rpcResponse = """
            {
                "jsonrpc": "2.0",
                "id": 1,
                "result": {
                    "tools": [
                        {
                            "name": "get_weather",
                            "description": "Get weather for city",
                            "inputSchema": {"type": "object"}
                        },
                        {
                            "name": "get_crypto_price",
                            "description": "Get crypto prices"
                        }
                    ]
                }
            }
        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(rpcResponse)
        )

        val config = McpServerConfig(
            id = 1L,
            name = "TestServer",
            url = server.url("/rpc").toString()
        )

        val tools = client.listTools(config)
        assertEquals(2, tools.size)
        assertEquals("get_weather", tools[0].name)
        assertEquals("Get weather for city", tools[0].description)
        assertEquals("get_crypto_price", tools[1].name)
    }

    @Test
    fun callTool_returnsTextContent() = runTest {
        val rpcResponse = """
            {
                "jsonrpc": "2.0",
                "id": 2,
                "result": {
                    "content": [
                        {
                            "type": "text",
                            "text": "24°C Sunny"
                        }
                    ],
                    "isError": false
                }
            }
        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(rpcResponse)
        )

        val config = McpServerConfig(
            id = 1L,
            name = "TestServer",
            url = server.url("/rpc").toString()
        )

        val args = buildJsonObject { put("city", "Seoul") }
        val result = client.callTool(config, "get_weather", args)

        assertNotNull(result)
        assertEquals("24°C Sunny", result)
    }
}
