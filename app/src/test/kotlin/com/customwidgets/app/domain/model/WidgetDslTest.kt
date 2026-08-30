package com.customwidgets.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class WidgetDslTest {

    @Test
    fun roundTrip_allNodeTypes() {
        val sampleWidget = WidgetDefinition(
            version = 1,
            background = DslBackground.Gradient(
                colors = listOf("#FF6200EE", "#FF3700B3"),
                orientation = "vertical"
            ),
            updateIntervalMinutes = 15,
            root = WidgetNode.Column(
                modifier = DslModifier(
                    padding = DslPadding(all = 16),
                    fillMaxWidth = true,
                    fillMaxHeight = true,
                    cornerRadius = 16
                ),
                children = listOf(
                    WidgetNode.Row(
                        modifier = DslModifier(fillMaxWidth = true),
                        children = listOf(
                            WidgetNode.Text(
                                text = "Clock Widget",
                                fontSize = 18,
                                fontWeight = "bold",
                                color = "#FFFFFF"
                            ),
                            WidgetNode.Spacer(width = 8),
                            WidgetNode.Image(
                                resName = "ic_clock",
                                contentDescription = "Clock icon"
                            )
                        )
                    ),
                    WidgetNode.Divider(color = "#44FFFFFF", thickness = 1),
                    WidgetNode.Spacer(height = 12),
                    WidgetNode.Box(
                        children = listOf(
                            WidgetNode.Text(
                                text = "{{time}}",
                                fontSize = 32,
                                fontWeight = "bold",
                                color = "#FFFFFF",
                                textAlign = "center"
                            )
                        )
                    ),
                    WidgetNode.Text(
                        text = "{{date}}",
                        fontSize = 14,
                        color = "#CCCCCC",
                        textAlign = "center"
                    ),
                    WidgetNode.Spacer(height = 8),
                    WidgetNode.Button(
                        text = "Refresh",
                        action = DslAction.Refresh,
                        backgroundColor = "#FFFFFF",
                        textColor = "#6200EE"
                    ),
                    WidgetNode.Clickable(
                        action = DslAction.OpenUrl("https://example.com"),
                        child = WidgetNode.Text(text = "Open Web", color = "#BB86FC")
                    )
                )
            )
        )

        val json = WidgetDefinition.toJson(sampleWidget)
        assertNotNull(json)
        assertTrue(json.contains("\"type\":\"column\""))
        assertTrue(json.contains("\"type\":\"text\""))
        assertTrue(json.contains("\"type\":\"refresh\""))
        assertTrue(json.contains("\"type\":\"open_url\""))
        assertTrue(json.contains("\"type\":\"gradient\""))

        val decoded = WidgetDefinition.fromJson(json)
        assertEquals(sampleWidget.version, decoded.version)
        assertEquals(sampleWidget.updateIntervalMinutes, decoded.updateIntervalMinutes)
        assertTrue(decoded.root is WidgetNode.Column)
        assertEquals(8, (decoded.root as WidgetNode.Column).children.size)
    }

    @Test
    fun solidBackground_serializesCorrectly() {
        val widget = WidgetDefinition(
            root = WidgetNode.Text(text = "Hello"),
            background = DslBackground.Solid("#FF000000")
        )
        val json = WidgetDefinition.toJson(widget)
        assertTrue(json.contains("\"type\":\"solid\""))
        assertTrue(json.contains("#FF000000"))

        val decoded = WidgetDefinition.fromJson(json)
        assertTrue(decoded.background is DslBackground.Solid)
        assertEquals("#FF000000", (decoded.background as DslBackground.Solid).color)
    }

    @Test
    fun launchAppAction_serializesCorrectly() {
        val widget = WidgetDefinition(
            root = WidgetNode.Button(
                text = "Launch",
                action = DslAction.LaunchApp("com.android.calculator2")
            )
        )
        val json = WidgetDefinition.toJson(widget)
        assertTrue(json.contains("\"type\":\"launch_app\""))
        assertTrue(json.contains("com.android.calculator2"))

        val decoded = WidgetDefinition.fromJson(json)
        val button = decoded.root as WidgetNode.Button
        assertTrue(button.action is DslAction.LaunchApp)
        assertEquals("com.android.calculator2", (button.action as DslAction.LaunchApp).packageName)
    }

    @Test
    fun depthValidation_withinLimit_passes() {
        // Depth 3: Column -> Row -> Text
        val widget = WidgetDefinition(
            root = WidgetNode.Column(
                children = listOf(
                    WidgetNode.Row(
                        children = listOf(
                            WidgetNode.Text(text = "Depth 3")
                        )
                    )
                )
            )
        )
        widget.validate() // Should not throw
    }

    @Test(expected = DslValidationException::class)
    fun depthValidation_exceedsLimit_throws() {
        // Depth 6: Col -> Col -> Col -> Col -> Col -> Text
        val widget = WidgetDefinition(
            root = WidgetNode.Column(
                children = listOf(
                    WidgetNode.Column(
                        children = listOf(
                            WidgetNode.Column(
                                children = listOf(
                                    WidgetNode.Column(
                                        children = listOf(
                                            WidgetNode.Column(
                                                children = listOf(
                                                    WidgetNode.Text(text = "Too deep")
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
        widget.validate()
    }

    @Test(expected = DslValidationException::class)
    fun nodeCountValidation_exceedsLimit_throws() {
        val nodes = (1..55).map { WidgetNode.Text(text = "Node $it") }
        val widget = WidgetDefinition(
            root = WidgetNode.Column(children = nodes)
        )
        widget.validate()
    }

    @Test
    fun invalidJson_throwsException() {
        try {
            WidgetDefinition.fromJson("{\"invalid\": true}")
            fail("Expected exception for invalid JSON")
        } catch (e: Exception) {
            assertTrue(e is Exception)
        }
    }
}
