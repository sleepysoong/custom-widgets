package com.customwidgets.app.ui.gallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import com.customwidgets.app.ui.glass.GlassDialog
import androidx.compose.material3.CardDefaults
import com.customwidgets.app.ui.glass.GlassCard
import androidx.compose.material3.ExperimentalMaterial3Api
import com.customwidgets.app.ui.glass.GlassButton
import androidx.compose.material3.Icon
import com.customwidgets.app.ui.glass.GlassIconButton
import androidx.compose.material3.MaterialTheme
import com.customwidgets.app.ui.glass.GlassMediumTopAppBar
import androidx.compose.material3.Scaffold
import com.customwidgets.app.ui.glass.GlassSurface
import androidx.compose.material3.Text
import com.customwidgets.app.ui.glass.GlassSecondaryButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.customwidgets.app.domain.model.WidgetDefinition
import com.customwidgets.app.domain.model.WidgetMetadata
import com.customwidgets.app.ui.preview.ComposeWidgetPreview
import com.customwidgets.app.util.FoldableUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetGalleryScreen(
    viewModel: WidgetGalleryViewModel,
    onCreateWidgetClicked: () -> Unit,
    onWidgetClicked: (Long) -> Unit
) {
    val widgets by viewModel.widgets.collectAsState()
    var widgetToDelete by remember { mutableStateOf<WidgetMetadata?>(null) }
    val gridColumns = FoldableUtils.getGalleryGridColumns()

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            GlassMediumTopAppBar(
                title = {
                    Text(
                        text = "내 위젯",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (widgets.isEmpty()) {
                EmptyExpressiveState(onCreateClicked = onCreateWidgetClicked)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 112.dp),
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    items(widgets, key = { it.id }) { widget ->
                        ExpressiveWidgetCard(
                            widget = widget,
                            onClick = { onWidgetClicked(widget.id) },
                            onDeleteClick = { widgetToDelete = widget }
                        )
                    }
                }
            }
        }
    }

    widgetToDelete?.let { target ->
        GlassDialog(
            onDismissRequest = { widgetToDelete = null },
            title = { Text("위젯 삭제", fontWeight = FontWeight.Bold) },
            text = { Text("\"${target.name}\" 위젯을 홈 화면 및 목록에서 삭제하시겠습니까?") },
            confirmButton = {
                GlassSecondaryButton(
                    onClick = {
                        viewModel.deleteWidget(target.id)
                        widgetToDelete = null
                    }
                ) {
                    Text("삭제", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                GlassSecondaryButton(onClick = { widgetToDelete = null }) {
                    Text("취소")
                }
            },
            shape = MaterialTheme.shapes.extraLarge
        )
    }
}

@Composable
private fun ExpressiveWidgetCard(
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

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Live Preview Card
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.3f),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLowest
            ) {
                if (definition != null) {
                    ComposeWidgetPreview(
                        definition = definition,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("미리보기 오류", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
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
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${widget.sizeLabel} 그리드",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                GlassIconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyExpressiveState(onCreateClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
        GlassSurface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "저장된 위젯이 없습니다",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "원하는 크기와 내용을 입력하면\nAI와 MCP 도구가 결합된 나만의 홈 화면 위젯을 제작해드립니다.",
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                GlassButton(
                    onClick = onCreateClicked,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("첫 위젯 만들기", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
