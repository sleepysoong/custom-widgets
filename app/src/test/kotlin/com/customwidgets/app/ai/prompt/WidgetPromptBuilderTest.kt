package com.customwidgets.app.ai.prompt

import com.customwidgets.app.domain.model.DslValidationException
import com.customwidgets.app.domain.model.WidgetDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPromptBuilderTest {

    @Test
    fun systemPrompt_containsAllRequiredConstraints() {
        val prompt = WidgetPromptBuilder.buildSystemPrompt()
        assertTrue(prompt.contains("column"))
        assertTrue(prompt.contains("row"))
        assertTrue(prompt.contains("box"))
        assertTrue(prompt.contains("text"))
        assertTrue(prompt.contains("{{time}}"))
        assertTrue(prompt.contains("{{date}}"))
        assertTrue(prompt.contains("{{battery}}"))
        assertTrue(prompt.contains("5 levels"))
        assertTrue(prompt.contains("50 nodes"))
    }

    @Test
    fun fewShotExamples_allDeserializeSuccessfully() {
        val examples = WidgetPromptBuilder.buildFewShotExamples()
        assertEquals(10, examples.size) // 5 user + 5 assistant

        val assistantMessages = examples.filter { it.role == "assistant" }
        assertEquals(5, assistantMessages.size)

        assistantMessages.forEach { msg ->
            val result = WidgetPromptBuilder.parseAiResponse(msg.content)
            assertTrue("Failed to parse example: ${msg.content}", result.isSuccess)
            val widget = result.getOrNull()
            assertNotNull(widget)
            widget?.validate()
        }
    }

    @Test
    fun parseAiResponse_cleanJson_succeeds() {
        val json = """
            {
                "version": 1,
                "root": {
                    "type": "text",
                    "text": "Hello World"
                }
            }
        """.trimIndent()

        val result = WidgetPromptBuilder.parseAiResponse(json)
        assertTrue(result.isSuccess)
    }

    @Test
    fun parseAiResponse_markdownWrapped_succeeds() {
        val wrapped = """
            Here is your widget:
            ```json
            {
                "version": 1,
                "root": {
                    "type": "text",
                    "text": "Fenced"
                }
            }
            ```
            Enjoy!
        """.trimIndent()

        val result = WidgetPromptBuilder.parseAiResponse(wrapped)
        assertTrue(result.isSuccess)
    }

    @Test
    fun parseAiResponse_garbageText_fails() {
        val garbage = "Sorry, I cannot help with that."
        val result = WidgetPromptBuilder.parseAiResponse(garbage)
        assertTrue(result.isFailure)
    }
}
