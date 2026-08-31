package com.customwidgets.app.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.customwidgets.app.domain.model.WidgetDefinition
import com.customwidgets.app.domain.model.WidgetMetadata
import com.customwidgets.app.ui.preview.ComposeWidgetPreview
import com.customwidgets.app.ui.theme.LiquidGlassBackground
import com.customwidgets.app.ui.theme.LiquidGlassButton
import com.customwidgets.app.ui.theme.LiquidGlassDefaults
import com.customwidgets.app.ui.theme.LiquidGlassSurface
import com.customwidgets.app.util.FoldableUtils
import com.customwidgets.app.ui.theme.SquircleShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetGalleryScreen(
    viewModel: WidgetGalleryViewModel,
    onCreateWidgetClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onWidgetClicked: (Long) -> Unit
) {
    val widgets by viewModel.widgets.collectAsState()
    var widgetToDelete by remember { mutableStateOf<WidgetMetadata?>(null) }
    val gridColumns = FoldableUtils.getGalleryGridColumns()

    LiquidGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Custom Widgets",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    },
                    actions = {
                        IconButton(onClick = onSettingsClicked) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onCreateWidgetClicked,
                    containerColor = Color(0xFF6200EE),
                    shape = SquircleShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Widget", tint = Color.White)
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (widgets.isEmpty()) {
                    EmptyGalleryState(onCreateClicked = onCreateWidgetClicked)
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumns),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        items(widgets, key = { it.id }) { widget ->
                            LiquidGlassWidgetCard(
                                widget = widget,
                                onClick = { onWidgetClicked(widget.id) },
                                onDeleteClick = { widgetToDelete = widget }
                            )
                        }
                    }
                }
            }
        }
    }

    widgetToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { widgetToDelete = null },
            title = { Text("위젯 삭제", fontWeight = FontWeight.Bold) },
            text = { Text("\"${target.name}\" 위젯을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteWidget(target.id)
                        widgetToDelete = null
                    }
                ) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { widgetToDelete = null }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
private fun LiquidGlassWidgetCard(
    widget: WidgetMetadata,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val definition = remember(widget.definitionJson) {
        try {
            WidgetDefinition.fromJson(widget.definitionJson)
        } catch (_: Exception) {
            null
        }
    }

    LiquidGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = 20.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Live Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.3f)
            ) {
                if (definition != null) {
                    ComposeWidgetPreview(
                        definition = definition,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF2C2C2C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Preview Error", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = widget.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${widget.sizeLabel} grid",
                        fontSize = 11.sp,
                        color = Color(0xFFBB86FC)
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.LightGray.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyGalleryState(onCreateClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LiquidGlassSurface(
            modifier = Modifier.padding(16.dp),
            cornerRadius = 32.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "✨", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "저장된 위젯이 없습니다",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "원하는 크기와 내용을 입력하면\nAI가 나만의 홈 화면 위젯을 제작해드립니다.",
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(20.dp))
                LiquidGlassButton(
                    onClick = onCreateClicked,
                    isPrimary = true
                ) {
                    Text("첫 위젯 만들기", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
