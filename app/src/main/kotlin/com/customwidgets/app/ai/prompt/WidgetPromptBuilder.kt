package com.customwidgets.app.ai.prompt

import com.customwidgets.app.ai.model.ChatMessage
import com.customwidgets.app.domain.model.DslAction
import com.customwidgets.app.domain.model.DslBackground
import com.customwidgets.app.domain.model.DslModifier
import com.customwidgets.app.domain.model.DslPadding
import com.customwidgets.app.domain.model.WidgetDefinition
import com.customwidgets.app.domain.model.WidgetNode

object WidgetPromptBuilder {

    fun buildSystemPrompt(): String {
        return """
You are an expert Android widget UI designer. Your task is to generate valid JSON widget definitions matching the exact DSL schema.

### DSL SCHEMA SPECIFICATION:

1. Root Object:
{
  "version": 1,
  "background": { "type": "solid", "color": "#HEX" } OR { "type": "gradient", "colors": ["#HEX1", "#HEX2"], "orientation": "vertical"|"horizontal" },
  "updateIntervalMinutes": 15,
  "root": <WidgetNode>
}

2. WidgetNode Types (discriminator is "type"):
- "column": { "type": "column", "children": [<WidgetNode>, ...], "modifier": <DslModifier>, "verticalArrangement": "center"|"bottom"|"top", "horizontalAlignment": "center"|"end"|"start" }
- "row": { "type": "row", "children": [<WidgetNode>, ...], "modifier": <DslModifier>, "horizontalArrangement": "center"|"end"|"start", "verticalAlignment": "center"|"bottom"|"top" }
- "box": { "type": "box", "children": [<WidgetNode>, ...], "modifier": <DslModifier>, "contentAlignment": "center"|"top_start"|"bottom_end" }
- "text": { "type": "text", "text": "string", "modifier": <DslModifier>, "fontSize": 16, "fontWeight": "normal"|"bold"|"medium", "color": "#HEX", "textAlign": "start"|"center"|"end", "maxLines": 2 }
- "image": { "type": "image", "resName": "ic_clock", "contentDescription": "desc", "modifier": <DslModifier> }
- "spacer": { "type": "spacer", "height": 12, "width": 8, "modifier": <DslModifier> }
- "divider": { "type": "divider", "color": "#44FFFFFF", "thickness": 1, "modifier": <DslModifier> }
- "button": { "type": "button", "text": "Click", "action": <DslAction>, "backgroundColor": "#HEX", "textColor": "#HEX", "modifier": <DslModifier> }
- "clickable": { "type": "clickable", "action": <DslAction>, "child": <WidgetNode>, "modifier": <DslModifier> }

3. DslModifier:
{
  "padding": { "all": 16 } OR { "start": 8, "top": 8, "end": 8, "bottom": 8 },
  "width": 100,
  "height": 50,
  "fillMaxWidth": true,
  "fillMaxHeight": true,
  "backgroundColor": "#HEX",
  "cornerRadius": 16,
  "border": { "color": "#HEX", "width": 1 }
}

4. DslAction Types:
- "open_url": { "type": "open_url", "url": "https://example.com" }
- "refresh": { "type": "refresh" }
- "launch_app": { "type": "launch_app", "packageName": "com.android.settings" }

5. Dynamic Data Tokens (use inside "text" fields):
- "{{time}}" -> Current time (e.g. "14:30")
- "{{date}}" -> Current date (e.g. "Aug 30, 2026")
- "{{battery}}" -> Battery percentage (e.g. "85%")
- "{{http:<url>:<jsonpath>}}" -> Dynamic HTTP value (e.g. "{{http:https://wttr.in/?format=j1:current_condition.0.temp_C}}")

### SAFETY AND DESIGN CONSTRAINTS:
- Output MUST be valid JSON only. DO NOT wrap with markdown fences (no ```json).
- Maximum nesting depth is 5 levels.
- Maximum total node count is 50 nodes.
- Use attractive Material You-inspired colors with high contrast.
- Ensure the widget layout fits the requested grid cell size ratio.
""".trimIndent()
    }

    fun buildUserPrompt(description: String, widthCells: Int, heightCells: Int): String {
        return """
Create a custom Android widget with the following requirements:
- Widget Size: ${widthCells}x${heightCells} cells (grid width: $widthCells, height: $heightCells)
- User Description: "$description"

Generate the complete JSON WidgetDefinition object adhering strictly to the DSL schema.
""".trimIndent()
    }

    fun buildFewShotExamples(): List<ChatMessage> {
        val clock2x1 = WidgetDefinition(
            version = 1,
            background = DslBackground.Solid("#FF1E1E1E"),
            updateIntervalMinutes = 15,
            root = WidgetNode.Column(
                modifier = DslModifier(padding = DslPadding(all = 12), fillMaxWidth = true, cornerRadius = 16),
                horizontalAlignment = "center",
                children = listOf(
                    WidgetNode.Text(text = "{{time}}", fontSize = 28, fontWeight = "bold", color = "#FFFFFF", textAlign = "center"),
                    WidgetNode.Spacer(height = 4),
                    WidgetNode.Text(text = "{{date}}", fontSize = 12, color = "#BB86FC", textAlign = "center")
                )
            )
        )

        val battery1x1 = WidgetDefinition(
            version = 1,
            background = DslBackground.Gradient(listOf("#FF0D47A1", "#FF1976D2"), "vertical"),
            updateIntervalMinutes = 15,
            root = WidgetNode.Column(
                modifier = DslModifier(padding = DslPadding(all = 8), fillMaxWidth = true, cornerRadius = 16),
                horizontalAlignment = "center",
                verticalArrangement = "center",
                children = listOf(
                    WidgetNode.Text(text = "BATTERY", fontSize = 10, fontWeight = "bold", color = "#BBDEFB"),
                    WidgetNode.Spacer(height = 4),
                    WidgetNode.Text(text = "{{battery}}", fontSize = 22, fontWeight = "bold", color = "#FFFFFF", textAlign = "center")
                )
            )
        )

        val quickLaunch4x1 = WidgetDefinition(
            version = 1,
            background = DslBackground.Solid("#FF2C2C2C"),
            root = WidgetNode.Row(
                modifier = DslModifier(padding = DslPadding(all = 8), fillMaxWidth = true, cornerRadius = 16),
                horizontalArrangement = "center",
                children = listOf(
                    WidgetNode.Button(text = "Settings", action = DslAction.LaunchApp("com.android.settings"), backgroundColor = "#FF424242", textColor = "#FFFFFF"),
                    WidgetNode.Spacer(width = 8),
                    WidgetNode.Button(text = "Camera", action = DslAction.LaunchApp("com.android.camera"), backgroundColor = "#FF424242", textColor = "#FFFFFF"),
                    WidgetNode.Spacer(width = 8),
                    WidgetNode.Button(text = "Refresh", action = DslAction.Refresh, backgroundColor = "#FF6200EE", textColor = "#FFFFFF")
                )
            )
        )

        val quote4x2 = WidgetDefinition(
            version = 1,
            background = DslBackground.Gradient(listOf("#FF283048", "#FF859398"), "vertical"),
            root = WidgetNode.Column(
                modifier = DslModifier(padding = DslPadding(all = 16), fillMaxWidth = true, cornerRadius = 16),
                children = listOf(
                    WidgetNode.Text(text = "Daily Inspiration", fontSize = 12, fontWeight = "bold", color = "#FFD700"),
                    WidgetNode.Spacer(height = 8),
                    WidgetNode.Text(text = "\"The secret of getting ahead is getting started.\"", fontSize = 16, color = "#FFFFFF", maxLines = 3),
                    WidgetNode.Spacer(height = 8),
                    WidgetNode.Text(text = "- Mark Twain", fontSize = 12, color = "#CCCCCC", textAlign = "end")
                )
            )
        )

        val systemInfo2x2 = WidgetDefinition(
            version = 1,
            background = DslBackground.Solid("#FF121212"),
            updateIntervalMinutes = 15,
            root = WidgetNode.Column(
                modifier = DslModifier(padding = DslPadding(all = 12), fillMaxWidth = true, cornerRadius = 16),
                children = listOf(
                    WidgetNode.Text(text = "{{time}}", fontSize = 24, fontWeight = "bold", color = "#00E676"),
                    WidgetNode.Text(text = "{{date}}", fontSize = 12, color = "#B0BEC5"),
                    WidgetNode.Divider(color = "#37474F", thickness = 1, modifier = DslModifier(padding = DslPadding(top = 8, bottom = 8))),
                    WidgetNode.Text(text = "Battery: {{battery}}", fontSize = 14, color = "#FFFFFF")
                )
            )
        )

        return listOf(
            ChatMessage("user", buildUserPrompt("Simple clock widget", 2, 1)),
            ChatMessage("assistant", WidgetDefinition.toJson(clock2x1)),
            ChatMessage("user", buildUserPrompt("Battery widget", 1, 1)),
            ChatMessage("assistant", WidgetDefinition.toJson(battery1x1)),
            ChatMessage("user", buildUserPrompt("Quick launcher with settings and camera", 4, 1)),
            ChatMessage("assistant", WidgetDefinition.toJson(quickLaunch4x1)),
            ChatMessage("user", buildUserPrompt("Daily inspirational quote widget", 4, 2)),
            ChatMessage("assistant", WidgetDefinition.toJson(quote4x2)),
            ChatMessage("user", buildUserPrompt("System info with time and battery", 2, 2)),
            ChatMessage("assistant", WidgetDefinition.toJson(systemInfo2x2))
        )
    }

    /**
     * Extracts and validates a WidgetDefinition from AI response text.
     * Cleans markdown fences, leading/trailing prose, and validates constraints.
     */
    fun parseAiResponse(rawResponse: String): Result<WidgetDefinition> {
        return try {
            val cleaned = cleanJsonResponse(rawResponse)
            val definition = WidgetDefinition.fromJson(cleaned)
            Result.success(definition)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cleanJsonResponse(raw: String): String {
        var text = raw.trim()

        // Strip markdown code fences if present (```json ... ```)
        if (text.startsWith("```")) {
            text = text.substringAfter("\n")
        }
        if (text.endsWith("```")) {
            text = text.substringBeforeLast("```")
        }
        text = text.trim()

        // Find outer JSON object boundaries if surrounded by conversational prose
        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            text = text.substring(firstBrace, lastBrace + 1)
        }

        return text
    }
}
