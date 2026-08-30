package com.customwidgets.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Exception thrown when a Widget DSL definition violates safety constraints
 * (e.g. nesting depth > 5 or total node count > 50).
 */
class DslValidationException(message: String) : Exception(message)

/**
 * Root container for a custom widget definition.
 *
 * @property version DSL schema version. Note: version is for future use; v1->v2 migration is out of scope.
 * @property root The root layout node of the widget.
 * @property background Optional widget background (solid color or gradient).
 * @property updateIntervalMinutes Optional periodic update interval in minutes (minimum 15 on Android).
 */
@Serializable
data class WidgetDefinition(
    val version: Int = 1,
    val root: WidgetNode,
    val background: DslBackground? = null,
    val updateIntervalMinutes: Int? = null
) {
    companion object {
        const val MAX_NESTING_DEPTH = 5
        const val MAX_TOTAL_NODES = 50

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            classDiscriminator = "type"
        }

        fun fromJson(jsonString: String): WidgetDefinition {
            val definition = json.decodeFromString<WidgetDefinition>(jsonString)
            definition.validate()
            return definition
        }

        fun toJson(definition: WidgetDefinition): String {
            return json.encodeToString(serializer(), definition)
        }
    }

    /**
     * Validates that the widget definition does not exceed nesting depth or node count limits.
     * Throws [DslValidationException] if constraints are violated.
     */
    fun validate() {
        val depth = root.calculateDepth()
        if (depth > MAX_NESTING_DEPTH) {
            throw DslValidationException("Widget exceeds maximum nesting depth ($depth > $MAX_NESTING_DEPTH)")
        }
        val count = root.countNodes()
        if (count > MAX_TOTAL_NODES) {
            throw DslValidationException("Widget exceeds maximum total nodes ($count > $MAX_TOTAL_NODES)")
        }
    }
}

/**
 * Polymorphic sealed class representing any node in the widget UI tree.
 */
@Serializable
sealed class WidgetNode {
    abstract val modifier: DslModifier?

    open fun calculateDepth(): Int = 1
    open fun countNodes(): Int = 1

    // Layout Nodes

    @Serializable
    @SerialName("column")
    data class Column(
        val children: List<WidgetNode> = emptyList(),
        override val modifier: DslModifier? = null,
        val verticalArrangement: String? = null,
        val horizontalAlignment: String? = null
    ) : WidgetNode() {
        override fun calculateDepth(): Int =
            1 + (children.maxOfOrNull { it.calculateDepth() } ?: 0)

        override fun countNodes(): Int =
            1 + children.sumOf { it.countNodes() }
    }

    @Serializable
    @SerialName("row")
    data class Row(
        val children: List<WidgetNode> = emptyList(),
        override val modifier: DslModifier? = null,
        val horizontalArrangement: String? = null,
        val verticalAlignment: String? = null
    ) : WidgetNode() {
        override fun calculateDepth(): Int =
            1 + (children.maxOfOrNull { it.calculateDepth() } ?: 0)

        override fun countNodes(): Int =
            1 + children.sumOf { it.countNodes() }
    }

    @Serializable
    @SerialName("box")
    data class Box(
        val children: List<WidgetNode> = emptyList(),
        override val modifier: DslModifier? = null,
        val contentAlignment: String? = null
    ) : WidgetNode() {
        override fun calculateDepth(): Int =
            1 + (children.maxOfOrNull { it.calculateDepth() } ?: 0)

        override fun countNodes(): Int =
            1 + children.sumOf { it.countNodes() }
    }

    // Leaf Nodes

    @Serializable
    @SerialName("text")
    data class Text(
        val text: String,
        override val modifier: DslModifier? = null,
        val fontSize: Int? = null,
        val fontWeight: String? = null, // "normal", "bold", "medium"
        val color: String? = null, // Hex color e.g. "#FFFFFF"
        val textAlign: String? = null, // "start", "center", "end"
        val maxLines: Int? = null
    ) : WidgetNode()

    @Serializable
    @SerialName("image")
    data class Image(
        val url: String? = null,
        val resName: String? = null,
        val contentDescription: String? = null,
        val contentScale: String? = null, // "fit", "crop", "fill"
        override val modifier: DslModifier? = null
    ) : WidgetNode()

    @Serializable
    @SerialName("spacer")
    data class Spacer(
        val height: Int? = null,
        val width: Int? = null,
        override val modifier: DslModifier? = null
    ) : WidgetNode()

    @Serializable
    @SerialName("divider")
    data class Divider(
        val color: String? = null,
        val thickness: Int? = null,
        override val modifier: DslModifier? = null
    ) : WidgetNode()

    // Interactive Nodes

    @Serializable
    @SerialName("button")
    data class Button(
        val text: String,
        val action: DslAction,
        override val modifier: DslModifier? = null,
        val backgroundColor: String? = null,
        val textColor: String? = null
    ) : WidgetNode()

    @Serializable
    @SerialName("clickable")
    data class Clickable(
        val action: DslAction,
        val child: WidgetNode,
        override val modifier: DslModifier? = null
    ) : WidgetNode() {
        override fun calculateDepth(): Int = 1 + child.calculateDepth()
        override fun countNodes(): Int = 1 + child.countNodes()
    }
}

/**
 * Actions supported by interactive widget components.
 */
@Serializable
sealed class DslAction {
    @Serializable
    @SerialName("open_url")
    data class OpenUrl(val url: String) : DslAction()

    @Serializable
    @SerialName("refresh")
    data object Refresh : DslAction()

    @Serializable
    @SerialName("launch_app")
    data class LaunchApp(val packageName: String) : DslAction()
}

/**
 * Styling modifier for widget nodes.
 */
@Serializable
data class DslModifier(
    val padding: DslPadding? = null,
    val width: Int? = null,
    val height: Int? = null,
    val fillMaxWidth: Boolean = false,
    val fillMaxHeight: Boolean = false,
    val backgroundColor: String? = null,
    val cornerRadius: Int? = null,
    val border: DslBorder? = null
)

@Serializable
data class DslPadding(
    val all: Int? = null,
    val start: Int? = null,
    val top: Int? = null,
    val end: Int? = null,
    val bottom: Int? = null
)

@Serializable
data class DslBorder(
    val color: String,
    val width: Int = 1
)

/**
 * Widget background configuration (solid color or gradient).
 */
@Serializable
sealed class DslBackground {
    @Serializable
    @SerialName("solid")
    data class Solid(val color: String) : DslBackground()

    @Serializable
    @SerialName("gradient")
    data class Gradient(
        val colors: List<String>,
        val orientation: String = "vertical" // "vertical", "horizontal"
    ) : DslBackground()
}
