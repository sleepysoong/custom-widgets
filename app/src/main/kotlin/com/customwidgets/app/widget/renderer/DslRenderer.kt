package com.customwidgets.app.widget.renderer

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.customwidgets.app.domain.model.DslAction
import com.customwidgets.app.domain.model.DslBackground
import com.customwidgets.app.domain.model.DslModifier
import com.customwidgets.app.domain.model.WidgetDefinition
import com.customwidgets.app.domain.model.WidgetNode

object DslRenderer {

    @Composable
    fun RenderWidget(
        definition: WidgetDefinition,
        bindingResolver: DataBindingResolver = DataBindingResolver()
    ) {
        // Enforce safety constraints
        definition.validate()

        var rootModifier = GlanceModifier.fillMaxSize()

        // Apply background
        when (val bg = definition.background) {
            is DslBackground.Solid -> {
                val colorInt = GradientRenderer.parseColor(bg.color, android.graphics.Color.TRANSPARENT)
                rootModifier = rootModifier.background(Color(colorInt))
            }
            is DslBackground.Gradient -> {
                val bitmap = GradientRenderer.createGradientBitmap(bg.colors, bg.orientation)
                rootModifier = rootModifier.background(ImageProvider(bitmap))
            }
            null -> { /* default transparent */ }
        }

        Box(modifier = rootModifier) {
            RenderNode(node = definition.root, bindingResolver = bindingResolver)
        }
    }

    @Composable
    fun RenderNode(
        node: WidgetNode,
        bindingResolver: DataBindingResolver
    ) {
        val modifier = toGlanceModifier(node.modifier)

        when (node) {
            is WidgetNode.Column -> {
                Column(
                    modifier = modifier,
                    verticalAlignment = node.verticalArrangement.toVerticalAlignment(),
                    horizontalAlignment = node.horizontalAlignment.toHorizontalAlignment()
                ) {
                    node.children.forEach { child ->
                        RenderNode(node = child, bindingResolver = bindingResolver)
                    }
                }
            }
            is WidgetNode.Row -> {
                Row(
                    modifier = modifier,
                    verticalAlignment = node.verticalAlignment.toVerticalAlignment(),
                    horizontalAlignment = node.horizontalArrangement.toHorizontalAlignment()
                ) {
                    node.children.forEach { child ->
                        RenderNode(node = child, bindingResolver = bindingResolver)
                    }
                }
            }
            is WidgetNode.Box -> {
                Box(
                    modifier = modifier,
                    contentAlignment = node.contentAlignment.toAlignment()
                ) {
                    node.children.forEach { child ->
                        RenderNode(node = child, bindingResolver = bindingResolver)
                    }
                }
            }
            is WidgetNode.Text -> {
                val resolvedText = bindingResolver.resolve(node.text)
                val textColor = node.color?.let {
                    Color(GradientRenderer.parseColor(it, android.graphics.Color.WHITE))
                } ?: Color.White

                val style = TextStyle(
                    color = ColorProvider(textColor),
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
                    }
                )

                Text(
                    text = resolvedText,
                    style = style,
                    modifier = modifier,
                    maxLines = node.maxLines ?: Int.MAX_VALUE
                )
            }
            is WidgetNode.Image -> {
                val imageProvider = if (!node.resName.isNullOrBlank()) {
                    ImageProvider(android.R.drawable.ic_menu_info_details)
                } else {
                    ImageProvider(android.R.drawable.ic_menu_gallery)
                }

                Image(
                    provider = imageProvider,
                    contentDescription = node.contentDescription ?: "Widget image",
                    modifier = modifier
                )
            }
            is WidgetNode.Spacer -> {
                var spacerMod = modifier
                node.height?.let { spacerMod = spacerMod.height(it.dp) }
                node.width?.let { spacerMod = spacerMod.width(it.dp) }
                Spacer(modifier = spacerMod)
            }
            is WidgetNode.Divider -> {
                val dividerColor = node.color?.let {
                    Color(GradientRenderer.parseColor(it, android.graphics.Color.DKGRAY))
                } ?: Color.Gray
                val thickness = (node.thickness ?: 1).dp

                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .height(thickness)
                        .background(dividerColor)
                ) {}
            }
            is WidgetNode.Button -> {
                val btnAction = node.action.toGlanceAction()
                val btnBg = node.backgroundColor?.let {
                    Color(GradientRenderer.parseColor(it, android.graphics.Color.WHITE))
                } ?: Color.White
                val btnTextColor = node.textColor?.let {
                    Color(GradientRenderer.parseColor(it, android.graphics.Color.BLACK))
                } ?: Color.Black

                androidx.glance.Button(
                    text = node.text,
                    onClick = btnAction,
                    modifier = modifier.background(btnBg),
                    colors = androidx.glance.ButtonDefaults.buttonColors(
                        backgroundColor = ColorProvider(btnBg),
                        contentColor = ColorProvider(btnTextColor)
                    )
                )
            }
            is WidgetNode.Clickable -> {
                val clickAction = node.action.toGlanceAction()
                Box(modifier = modifier.clickable(clickAction)) {
                    RenderNode(node = node.child, bindingResolver = bindingResolver)
                }
            }
        }
    }

    private fun toGlanceModifier(modifier: DslModifier?): GlanceModifier {
        if (modifier == null) return GlanceModifier

        var result: GlanceModifier = GlanceModifier

        // Padding
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

        // Sizing
        if (modifier.fillMaxWidth && modifier.fillMaxHeight) {
            result = result.fillMaxSize()
        } else {
            if (modifier.fillMaxWidth) result = result.fillMaxWidth()
            if (modifier.fillMaxHeight) result = result.fillMaxHeight()
        }

        modifier.width?.let { result = result.width(it.dp) }
        modifier.height?.let { result = result.height(it.dp) }

        // Background color
        modifier.backgroundColor?.let { hex ->
            val colorInt = GradientRenderer.parseColor(hex)
            result = result.background(Color(colorInt))
        }

        // Corner radius
        modifier.cornerRadius?.let { radius ->
            result = result.cornerRadius(radius.dp)
        }

        return result
    }

    private fun DslAction.toGlanceAction(): androidx.glance.action.Action {
        return when (this) {
            is DslAction.OpenUrl -> {
                actionRunCallback<OpenUrlCallback>(
                    actionParametersOf(URL_KEY to url)
                )
            }
            is DslAction.Refresh -> {
                actionRunCallback<RefreshCallback>()
            }
            is DslAction.LaunchApp -> {
                actionRunCallback<LaunchAppCallback>(
                    actionParametersOf(PACKAGE_KEY to packageName)
                )
            }
        }
    }

    private fun String?.toHorizontalAlignment(): Alignment.Horizontal = when (this?.lowercase()) {
        "center" -> Alignment.Horizontal.CenterHorizontally
        "end", "right" -> Alignment.Horizontal.End
        else -> Alignment.Horizontal.Start
    }

    private fun String?.toVerticalAlignment(): Alignment.Vertical = when (this?.lowercase()) {
        "center" -> Alignment.Vertical.CenterVertically
        "bottom" -> Alignment.Vertical.Bottom
        else -> Alignment.Vertical.Top
    }

    private fun String?.toAlignment(): Alignment = when (this?.lowercase()) {
        "center" -> Alignment.Center
        "top_start" -> Alignment.TopStart
        "top_end" -> Alignment.TopEnd
        "bottom_start" -> Alignment.BottomStart
        "bottom_end" -> Alignment.BottomEnd
        else -> Alignment.Center
    }

    val URL_KEY = ActionParameters.Key<String>("extra_url")
    val PACKAGE_KEY = ActionParameters.Key<String>("extra_package")
}

// Action callbacks
class OpenUrlCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val url = parameters[DslRenderer.URL_KEY] ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

class RefreshCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // Trigger widget refresh
    }
}

class LaunchAppCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val packageName = parameters[DslRenderer.PACKAGE_KEY] ?: return
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        launchIntent?.let {
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(it)
        }
    }
}
