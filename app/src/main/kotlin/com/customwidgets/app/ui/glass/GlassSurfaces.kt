package com.customwidgets.app.ui.glass

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    colors: CardColors = CardDefaults.cardColors(),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.glassMaterial(shape, colors.containerColor), shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent, contentColor = colors.contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = content
    )
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.glassMaterial(shape, color), shape = shape,
        color = Color.Transparent, contentColor = contentColor,
        tonalElevation = tonalElevation, content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors()
) {
    TopAppBar(
        title = title, navigationIcon = navigationIcon, actions = actions,
        modifier = modifier.glassMaterial(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
        colors = colors.copy(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassMediumTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors()
) {
    MediumTopAppBar(
        title = title, navigationIcon = navigationIcon, actions = actions,
        modifier = modifier.glassMaterial(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)),
        colors = colors.copy(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent)
    )
}

@Composable
fun GlassNavigationBar(
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable RowScope.() -> Unit
) {
    NavigationBar(
        modifier = modifier.glassMaterial(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
        containerColor = Color.Transparent, contentColor = contentColor,
        tonalElevation = 0.dp, content = content
    )
}

@Composable
fun GlassFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    content: @Composable () -> Unit
) {
    FloatingActionButton(
        onClick = onClick, modifier = modifier.glassMaterial(shape, containerColor),
        shape = shape, containerColor = Color.Transparent, contentColor = contentColor,
        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp), content = content
    )
}

@Composable
fun GlassExtendedFab(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    ExtendedFloatingActionButton(
        onClick = onClick, icon = icon, text = text,
        modifier = modifier.glassMaterial(shape, containerColor), shape = shape,
        containerColor = Color.Transparent, contentColor = contentColor,
        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
    )
}

/** Each Dialog gets its own source. Never reuse the Activity's graphics layer across windows. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape = RoundedCornerShape(28.dp)
) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = true)) {
        // System Dialog provides modality, dim, outside dismissal and Back handling.
        BoxWithConstraints(Modifier.widthIn(max = 560.dp).fillMaxWidth().safeDrawingPadding().imePadding()) {
            GlassHost(
                modifier = modifier.widthIn(max = 560.dp).fillMaxWidth().heightIn(max = maxHeight),
                fillWindow = false
            ) {
                GlassSurface(shape = shape) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        if (title != null) {
                            Box(Modifier.semantics { heading() }) {
                                ProvideTextStyle(MaterialTheme.typography.headlineSmall) { title() }
                            }
                        }
                        if (text != null) {
                            Box(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                                ProvideTextStyle(MaterialTheme.typography.bodyMedium) { text() }
                            }
                        }
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            dismissButton?.invoke()
                            confirmButton()
                        }
                    }
                }
            }
        }
    }
}
