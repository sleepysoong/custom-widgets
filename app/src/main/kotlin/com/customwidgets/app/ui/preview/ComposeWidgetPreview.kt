package com.customwidgets.app.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.customwidgets.app.domain.model.DslBackground
import com.customwidgets.app.domain.model.DslModifier
import com.customwidgets.app.domain.model.WidgetDefinition
import com.customwidgets.app.domain.model.WidgetNode
import com.customwidgets.app.widget.renderer.DataBindingResolver
import com.customwidgets.app.widget.renderer.GradientRenderer

/**
 * Renders a [WidgetDefinition] using standard Jetpack Compose for in-app preview.
 */
@Composable
fun ComposeWidgetPreview(
    definition: WidgetDefinition,
    modifier: Modifier = Modifier,
    bindingResolver: DataBindingResolver = DataBindingResolver()
) {
    var boxModifier = modifier.clip(RoundedCornerShape(16.dp))

    when (val bg = definition.background) {
        is DslBackground.Solid -> {
            val colorInt = GradientRenderer.parseColor(bg.color, android.graphics.Color.DKGRAY)
            boxModifier = boxModifier.background(Color(colorInt))
        }
        is DslBackground.Gradient -> {
            val colors = bg.colors.map { Color(GradientRenderer.parseColor(it)) }
            val safeColors = if (colors.size < 2) listOf(Color.DarkGray, Color.Black) else colors
            val brush = if (bg.orientation.lowercase() == "horizontal") {
                Brush.horizontalGradient(safeColors)
            } else {
                Brush.verticalGradient(safeColors)
            }
            boxModifier = boxModifier.background(brush)
        }
        null -> {
            boxModifier = boxModifier.background(Color(0xFF1E1E1E))
        }
    }

    Box(modifier = boxModifier) {
        ComposeWidgetNode(node = definition.root, bindingResolver = bindingResolver)
    }
}

@Composable
fun ComposeWidgetNode(
    node: WidgetNode,
    bindingResolver: DataBindingResolver
) {
    val nodeModifier = toComposeModifier(node.modifier)

    when (node) {
        is WidgetNode.Column -> {
            Column(
                modifier = nodeModifier,
                verticalArrangement = when (node.verticalArrangement?.lowercase()) {
                    "center" -> Arrangement.Center
                    "bottom" -> Arrangement.Bottom
                    else -> Arrangement.Top
                },
                horizontalAlignment = when (node.horizontalAlignment?.lowercase()) {
                    "center" -> Alignment.CenterHorizontally
                    "end", "right" -> Alignment.End
                    else -> Alignment.Start
                }
            ) {
                node.children.forEach { child ->
                    ComposeWidgetNode(node = child, bindingResolver = bindingResolver)
                }
            }
        }
        is WidgetNode.Row -> {
            Row(
                modifier = nodeModifier,
                horizontalArrangement = when (node.horizontalArrangement?.lowercase()) {
                    "center" -> Arrangement.Center
                    "end", "right" -> Arrangement.End
                    else -> Arrangement.Start
                },
                verticalAlignment = when (node.verticalAlignment?.lowercase()) {
                    "center" -> Alignment.CenterVertically
                    "bottom" -> Alignment.Bottom
                    else -> Alignment.Top
                }
            ) {
                node.children.forEach { child ->
                    ComposeWidgetNode(node = child, bindingResolver = bindingResolver)
                }
            }
        }
        is WidgetNode.Box -> {
            Box(
                modifier = nodeModifier,
                contentAlignment = when (node.contentAlignment?.lowercase()) {
                    "center" -> Alignment.Center
                    "top_start" -> Alignment.TopStart
                    "top_end" -> Alignment.TopEnd
                    "bottom_start" -> Alignment.BottomStart
                    "bottom_end" -> Alignment.BottomEnd
                    else -> Alignment.Center
                }
            ) {
                node.children.forEach { child ->
                    ComposeWidgetNode(node = child, bindingResolver = bindingResolver)
                }
            }
        }
        is WidgetNode.Text -> {
            val resolved = bindingResolver.resolve(node.text)
            val textColor = node.color?.let {
                Color(GradientRenderer.parseColor(it, android.graphics.Color.WHITE))
            } ?: Color.White

            Text(
                text = resolved,
                color = textColor,
                fontSize = (node.fontSize ?: 14).sp,
                fontWeight = when (node.fontWeight?.lowercase()) {
                    "bold" -> FontWeight.Bold
                    "medium" -> FontWeight.Medium
                    else -> FontWeight.Normal
                },
                textAlign = when (node.textAlign?.lowercase()) {
                    "center" -> TextAlign.Center
                    "end", "right" -> TextAlign.End
                    else -> TextAlign.Start
                },
                maxLines = node.maxLines ?: Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis,
                modifier = nodeModifier
            )
        }
        is WidgetNode.Image -> {
            Box(
                modifier = nodeModifier
                    .size(32.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("📷", fontSize = 16.sp)
            }
        }
        is WidgetNode.Spacer -> {
            var spacerMod = nodeModifier
            node.height?.let { spacerMod = spacerMod.height(it.dp) }
            node.width?.let { spacerMod = spacerMod.width(it.dp) }
            Spacer(modifier = spacerMod)
        }
        is WidgetNode.Divider -> {
            val dividerColor = node.color?.let {
                Color(GradientRenderer.parseColor(it, android.graphics.Color.DKGRAY))
            } ?: Color.Gray
            Box(
                modifier = nodeModifier
                    .fillMaxWidth()
                    .height((node.thickness ?: 1).dp)
                    .background(dividerColor)
            )
        }
        is WidgetNode.Button -> {
            val btnBg = node.backgroundColor?.let {
                Color(GradientRenderer.parseColor(it, android.graphics.Color.WHITE))
            } ?: Color.White
            val btnTextColor = node.textColor?.let {
                Color(GradientRenderer.parseColor(it, android.graphics.Color.BLACK))
            } ?: Color.Black

            Button(
                onClick = { /* Preview click */ },
                colors = ButtonDefaults.buttonColors(containerColor = btnBg),
                modifier = nodeModifier
            ) {
                Text(node.text, color = btnTextColor, fontSize = 12.sp)
            }
        }
        is WidgetNode.Clickable -> {
            Box(modifier = nodeModifier.clickable { /* Preview click */ }) {
                ComposeWidgetNode(node = node.child, bindingResolver = bindingResolver)
            }
        }
    }
}

private fun toComposeModifier(modifier: DslModifier?): Modifier {
    if (modifier == null) return Modifier

    var result: Modifier = Modifier

    modifier.padding?.let { p ->
        if (p.all != null) {
            result = result.padding(p.all.dp)
        } else {
            result = result.padding(
                start = (p.start ?: 0).dp,
                top = (p.top ?: 0).dp,
                end = (p.end ?: 0).dp,
                bottom = (p.bottom ?: 0).dp
            )
        }
    }

    if (modifier.fillMaxWidth && modifier.fillMaxHeight) {
        result = result.fillMaxSize()
    } else {
        if (modifier.fillMaxWidth) result = result.fillMaxWidth()
        if (modifier.fillMaxHeight) result = result.fillMaxHeight()
    }

    modifier.width?.let { result = result.width(it.dp) }
    modifier.height?.let { result = result.height(it.dp) }

    modifier.cornerRadius?.let { radius ->
        result = result.clip(RoundedCornerShape(radius.dp))
    }

    modifier.backgroundColor?.let { hex ->
        val colorInt = GradientRenderer.parseColor(hex)
        result = result.background(Color(colorInt))
    }

    modifier.border?.let { b ->
        val colorInt = GradientRenderer.parseColor(b.color)
        result = result.border(b.width.dp, Color(colorInt), RoundedCornerShape(modifier.cornerRadius ?: 0))
    }

    return result
}
