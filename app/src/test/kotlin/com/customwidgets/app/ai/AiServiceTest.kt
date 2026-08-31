package com.customwidgets.app.ai

import com.customwidgets.app.ai.model.AiAuthException
import com.customwidgets.app.ai.model.AiConfig
import com.customwidgets.app.ai.model.AiRateLimitException
import com.customwidgets.app.ai.model.AiServerException
import com.customwidgets.app.ai.model.ChatMessage
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AiServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: AiService
    private val client = OkHttpClient()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        service = AiService(client)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun streamChatCompletion_emitsTokens() = runTest {
        val sseBody = "data: {\"choices\":[{\"delta\":{\"content\":\"hello\"}}]}\n" +
            "data: {\"choices\":[{\"delta\":{\"content\":\" world\"}}]}\n" +
            "data: [DONE]\n"

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseBody)
        )

        val config = AiConfig(customBaseUrl = server.url("/").toString(), apiKey = "test-key")
        val messages = listOf(ChatMessage("user", "Create clock widget"))

        val tokens = service.streamChatCompletion(messages, config).toList()

        assertEquals(listOf("hello", " world"), tokens)
        val recorded = server.takeRequest()
        assertEquals("Bearer test-key", recorded.getHeader("Authorization"))
        assertEquals("/v1/chat/completions", recorded.path)
    }

    @Test
    fun chatCompletionSync_returnsContent() = runTest {
        val responseJson = """
            {
                "choices": [
                    {
                        "message": {
                            "role": "assistant",
                            "content": "{\"type\":\"column\"}"
                        }
                    }
                ]
            }
        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseJson)
        )

        val config = AiConfig(customBaseUrl = server.url("/").toString(), apiKey = "test-key")
        val messages = listOf(ChatMessage("user", "Create widget"))

        val content = service.chatCompletionSync(messages, config)
        assertEquals("{\"type\":\"column\"}", content)
    }

    @Test
    fun error401_throwsAiAuthException() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("Unauthorized API key")
        )

        val config = AiConfig(customBaseUrl = server.url("/").toString())
        try {
            service.chatCompletionSync(listOf(ChatMessage("user", "hi")), config)
            org.junit.Assert.fail("Expected AiAuthException")
        } catch (e: Exception) {
            assertTrue(e is AiAuthException)
        }
    }

    @Test
    fun error429_throwsAiRateLimitException() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setBody("Rate limit reached")
        )

        val config = AiConfig(customBaseUrl = server.url("/").toString())
        try {
            service.chatCompletionSync(listOf(ChatMessage("user", "hi")), config)
            org.junit.Assert.fail("Expected AiRateLimitException")
        } catch (e: Exception) {
            assertTrue(e is AiRateLimitException)
        }
    }

    @Test
    fun error500_throwsAiServerException() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error")
        )

        val config = AiConfig(customBaseUrl = server.url("/").toString())
        try {
            service.chatCompletionSync(listOf(ChatMessage("user", "hi")), config)
            org.junit.Assert.fail("Expected AiServerException")
        } catch (e: Exception) {
            assertTrue(e is AiServerException)
        }
    }

    @Test
    fun responseFormat400_retriesWithoutResponseFormat() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("response_format is not supported")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"choices":[{"message":{"role":"assistant","content":"{\"ok\":true}"}}]}""")
        )

        val config = AiConfig(customBaseUrl = server.url("/").toString())
        val result = service.chatCompletionSync(listOf(ChatMessage("user", "hi")), config, useJsonMode = true)

        assertEquals("{\"ok\":true}", result)
        assertEquals(2, server.requestCount)
    }
}
