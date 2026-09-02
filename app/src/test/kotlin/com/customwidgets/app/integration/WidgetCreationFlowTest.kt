package com.customwidgets.app.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.customwidgets.app.ai.AiConfigStore
import com.customwidgets.app.ai.AiService
import com.customwidgets.app.ai.GenerationState
import com.customwidgets.app.ai.WidgetGenerationService
import com.customwidgets.app.ai.model.AiConfig
import com.customwidgets.app.ai.prompt.WidgetPromptBuilder
import com.customwidgets.app.data.local.WidgetDatabase
import com.customwidgets.app.data.repository.WidgetRepository
import com.customwidgets.app.mcp.McpClient
import com.customwidgets.app.mcp.McpServerRepository
import com.customwidgets.app.ui.create.CreateWidgetViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WidgetCreationFlowTest {

    private lateinit var server: MockWebServer
    private lateinit var database: WidgetDatabase
    private lateinit var repository: WidgetRepository
    private lateinit var aiService: AiService
    private lateinit var generationService: WidgetGenerationService
    private lateinit var aiConfigStore: AiConfigStore
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        server = MockWebServer()
        server.start()

        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, WidgetDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WidgetRepository(database.widgetDao())

        val okHttpClient = OkHttpClient()
        val mcpRepository = McpServerRepository(database.mcpServerDao())
        val mcpClient = McpClient(okHttpClient)
        aiService = AiService(okHttpClient)
        generationService = WidgetGenerationService(aiService, mcpRepository, mcpClient)
        aiConfigStore = AiConfigStore(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        server.shutdown()
        database.close()
    }

    @Test
    fun endToEndWidgetCreation_withMockAi() = runTest(testDispatcher) {
        val sampleWidgetJson = """
            {
                "version": 1,
                "background": { "type": "solid", "color": "#FF1E1E1E" },
                "updateIntervalMinutes": 15,
                "root": {
                    "type": "column",
                    "children": [
                        {
                            "type": "text",
                            "text": "{{time}}",
                            "fontSize": 24,
                            "fontWeight": "bold",
                            "color": "#FFFFFF"
                        },
                        {
                            "type": "text",
                            "text": "{{date}}",
                            "fontSize": 12,
                            "color": "#BB86FC"
                        }
                    ]
                }
            }
        """.trimIndent()

        val chunkJson = kotlinx.serialization.json.Json.encodeToString(
            com.customwidgets.app.ai.model.ChatChunk.serializer(),
            com.customwidgets.app.ai.model.ChatChunk(
                choices = listOf(
                    com.customwidgets.app.ai.model.ChunkChoice(
                        delta = com.customwidgets.app.ai.model.ChunkDelta(content = sampleWidgetJson)
                    )
                )
            )
        )
        val sseResponse = "data: $chunkJson\n\ndata: [DONE]\n\n"

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseResponse)
        )

        val config = AiConfig(
            customBaseUrl = server.url("/").toString(),
            apiKey = "test-key",
            model = "gpt-4o-mini"
        )
        aiConfigStore.saveConfig(config)

        val viewModel = CreateWidgetViewModel(
            generationService = generationService,
            aiConfigStore = aiConfigStore,
            widgetRepository = repository
        )

        // Step 1: Select size (2x2)
        viewModel.selectSize(2, 2)
        assertEquals(2, viewModel.uiState.value.widthCells)
        assertEquals(2, viewModel.uiState.value.heightCells)

        // Step 2: Description
        viewModel.updateDescription("Minimal clock widget")
        viewModel.updateWidgetName("My Clock")

        // Step 3: Generate
        viewModel.generateWidget()
        testScheduler.advanceUntilIdle()

        withContext(Dispatchers.Default) {
            var attempts = 0
            while (viewModel.uiState.value.generationState !is GenerationState.Success && attempts < 50) {
                Thread.sleep(50)
                attempts++
            }
        }
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success state but was ${state.generationState}", state.generationState is GenerationState.Success)
        assertNotNull(state.generatedDefinition)
        assertEquals(1, state.generatedDefinition?.version)

        // Step 4: Save to Room
        viewModel.saveWidget(appWidgetId = 1001)
        testScheduler.advanceUntilIdle()

        withContext(Dispatchers.Default) {
            var attempts = 0
            while (viewModel.uiState.value.currentStep != 3 && attempts < 50) {
                Thread.sleep(50)
                attempts++
            }
        }
        testScheduler.advanceUntilIdle()

        // Verify stored in Room
        val saved = repository.getWidgetByAppWidgetId(1001)
        assertNotNull(saved)
        assertEquals("My Clock", saved?.name)
        assertEquals(2, saved?.widthCells)
        assertEquals(2, saved?.heightCells)
    }

    @Test
    fun sampleWidgets_allFiveCategories_passValidation() {
        val examples = WidgetPromptBuilder.buildFewShotExamples()
        val assistantExamples = examples.filter { it.role == "assistant" }

        assertEquals(5, assistantExamples.size)
        assistantExamples.forEach { example ->
            val result = WidgetPromptBuilder.parseAiResponse(example.content)
            assertTrue("Expected valid definition for example", result.isSuccess)
            val definition = result.getOrThrow()
            definition.validate()
            assertNotNull(definition.root)
        }
    }
}
