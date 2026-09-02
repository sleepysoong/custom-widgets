package com.customwidgets.app.ui.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.customwidgets.app.ai.GenerationState
import com.customwidgets.app.ui.preview.ComposeWidgetPreview
import com.customwidgets.app.util.FoldableUtils

data class WidgetSizeOption(val width: Int, val height: Int, val label: String)

val SIZE_OPTIONS = listOf(
    WidgetSizeOption(1, 1, "1 x 1"),
    WidgetSizeOption(2, 1, "2 x 1"),
    WidgetSizeOption(2, 2, "2 x 2"),
    WidgetSizeOption(3, 2, "3 x 2"),
    WidgetSizeOption(4, 1, "4 x 1"),
    WidgetSizeOption(4, 2, "4 x 2"),
    WidgetSizeOption(4, 3, "4 x 3"),
    WidgetSizeOption(5, 2, "5 x 2"),
    WidgetSizeOption(5, 3, "5 x 3")
)

val TEMPLATE_PROMPTS = listOf(
    "시계 위젯" to "미니멀한 디지털 시계 위젯, 날짜와 시간 표시",
    "배터리 상태" to "배터리 잔량 표시 및 상태 위젯",
    "빠른 실행" to "자주 쓰는 앱 빠른 실행 버튼 바",
    "오늘의 명언" to "매일 새로운 영감을 주는 명언 위젯",
    "대시보드" to "시간, 날짜, 배터리 통합 시스템 정보 대시보드"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWidgetScreen(
    viewModel: CreateWidgetViewModel,
    appWidgetId: Int? = null,
    onNavigateBack: () -> Unit = {},
    onWidgetCreated: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isFoldExpanded = FoldableUtils.isExpandedScreen()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("위젯 만들기", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
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
            if (isFoldExpanded && uiState.currentStep in 1..2) {
                // Galaxy Fold Unfolded Dual-Pane Layout
                FoldableDualPaneWizard(
                    uiState = uiState,
                    viewModel = viewModel,
                    appWidgetId = appWidgetId,
                    onWidgetCreated = onWidgetCreated
                )
            } else {
                // Compact / Standard Stacked Wizard
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    when (uiState.currentStep) {
                        0 -> SizeSelectionStep(
                            selectedWidth = uiState.widthCells,
                            selectedHeight = uiState.heightCells,
                            onSelectSize = { w, h -> viewModel.selectSize(w, h) }
                        )
                        1 -> DescriptionStep(
                            description = uiState.description,
                            width = uiState.widthCells,
                            height = uiState.heightCells,
                            onDescriptionChanged = { viewModel.updateDescription(it) },
                            onGenerateClicked = { viewModel.generateWidget() },
                            onBackClicked = { viewModel.setStep(0) }
                        )
                        2 -> GenerationAndPreviewStep(
                            uiState = uiState,
                            onRegenerate = { viewModel.generateWidget() },
                            onToggleJsonEditor = { viewModel.toggleJsonEditor(it) },
                            onJsonChanged = { viewModel.updateEditableJson(it) },
                            onApplyJson = { viewModel.applyEditedJson() },
                            onNameChanged = { viewModel.updateWidgetName(it) },
                            onSave = {
                                viewModel.saveWidget(appWidgetId) { id ->
                                    onWidgetCreated(id)
                                }
                            },
                            onBackClicked = { viewModel.setStep(1) }
                        )
                        3 -> SaveConfirmationStep(
                            widgetName = uiState.widgetName,
                            onDoneClicked = onNavigateBack
                        )
                    }
                }
            }
        }
    }
}

/**
 * Galaxy Fold Main Screen Dual-Pane Wizard:
 * Left: Input & controls | Right: Live Material 3 Preview
 */
@Composable
private fun FoldableDualPaneWizard(
    uiState: CreateWidgetUiState,
    viewModel: CreateWidgetViewModel,
    appWidgetId: Int?,
    onWidgetCreated: (Long) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left Pane: Controls & Description
        ElevatedCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "위젯 디자인 설정 (${uiState.widthCells}x${uiState.heightCells})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = { viewModel.updateDescription(it) },
                    label = { Text("위젯 설명 입력") },
                    placeholder = { Text("예: 날씨와 시계가 있는 세련된 Material 3 위젯") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("추천 템플릿:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TEMPLATE_PROMPTS.take(3).forEach { (label, prompt) ->
                        FilterChip(
                            selected = uiState.description == prompt,
                            onClick = { viewModel.updateDescription(prompt) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.generateWidget() },
                    enabled = uiState.description.isNotBlank() && uiState.generationState !is GenerationState.Loading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text("AI로 생성하기", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.widgetName,
                    onValueChange = { viewModel.updateWidgetName(it) },
                    label = { Text("위젯 이름") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                FilledTonalButton(
                    onClick = {
                        viewModel.saveWidget(appWidgetId) { id ->
                            onWidgetCreated(id)
                        }
                    },
                    enabled = uiState.generatedDefinition != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("홈 화면에 위젯 저장", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Right Pane: Live Material 3 Preview
        ElevatedCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "실시간 미리보기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                when (val genState = uiState.generationState) {
                    is GenerationState.Loading -> {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("OpenAI & MCP가 위젯을 구성하는 중...")
                    }
                    is GenerationState.Streaming -> {
                        Text("스트리밍 중...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest
                        ) {
                            Text(
                                text = genState.partialText.takeLast(400),
                                color = Color(0xFF00FF00),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    is GenerationState.Success -> {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .aspectRatio(1.3f),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest
                        ) {
                            ComposeWidgetPreview(
                                definition = genState.definition,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = "왼쪽에서 설명을 입력하고\n[AI로 생성하기] 버튼을 눌러주세요.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SizeSelectionStep(
    selectedWidth: Int,
    selectedHeight: Int,
    onSelectSize: (Int, Int) -> Unit
) {
    val gridColumns = FoldableUtils.getWizardSizeGridColumns()

    Text(
        text = "위젯 크기 선택",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(SIZE_OPTIONS) { opt ->
            val isSelected = opt.width == selectedWidth && opt.height == selectedHeight
            ElevatedCard(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clickable { onSelectSize(opt.width, opt.height) },
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = opt.label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 16.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${opt.width}x${opt.height}",
                            fontSize = 11.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DescriptionStep(
    description: String,
    width: Int,
    height: Int,
    onDescriptionChanged: (String) -> Unit,
    onGenerateClicked: () -> Unit,
    onBackClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "위젯 내용 설명 (${width}x${height})",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text("추천 템플릿:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TEMPLATE_PROMPTS.take(2).forEach { (label, prompt) ->
                FilterChip(
                    selected = description == prompt,
                    onClick = { onDescriptionChanged(prompt) },
                    label = { Text(label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChanged,
            placeholder = { Text("원하는 위젯의 내용을 설명해주세요 (예: 시계와 배터리가 있는 다크 모드 위젯)") },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            maxLines = 5,
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBackClicked, shape = MaterialTheme.shapes.medium) {
                Text("이전")
            }
            Button(
                onClick = onGenerateClicked,
                enabled = description.isNotBlank(),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.padding(4.dp))
                Text("AI 생성", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GenerationAndPreviewStep(
    uiState: CreateWidgetUiState,
    onRegenerate: () -> Unit,
    onToggleJsonEditor: (Boolean) -> Unit,
    onJsonChanged: (String) -> Unit,
    onApplyJson: () -> Unit,
    onNameChanged: (String) -> Unit,
    onSave: () -> Unit,
    onBackClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        when (val genState = uiState.generationState) {
            is GenerationState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("OpenAI & MCP 도구로 위젯을 디자인하는 중...")
                    }
                }
            }
            is GenerationState.Streaming -> {
                Column {
                    Text("생성 중... / Streaming:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerLowest
                    ) {
                        Text(
                            text = genState.partialText.takeLast(500),
                            color = Color(0xFF00FF00),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
            is GenerationState.Error -> {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "생성 오류",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = genState.error.userMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                FilledTonalButton(onClick = onRegenerate, shape = MaterialTheme.shapes.medium) {
                    Text("다시 시도")
                }
            }
            is GenerationState.Success -> {
                Text(
                    text = "위젯 미리보기 (${uiState.widthCells}x${uiState.heightCells})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((uiState.heightCells * 80).coerceIn(120, 300).dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest
                ) {
                    ComposeWidgetPreview(
                        definition = genState.definition,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.widgetName,
                    onValueChange = onNameChanged,
                    label = { Text("위젯 이름") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onToggleJsonEditor(!uiState.isJsonEditorOpen) },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(if (uiState.isJsonEditorOpen) "JSON 닫기" else "JSON 편집")
                    }
                    FilledTonalButton(
                        onClick = onRegenerate,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("다시 생성")
                    }
                }

                AnimatedVisibility(visible = uiState.isJsonEditorOpen) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        OutlinedTextField(
                            value = uiState.editableJson,
                            onValueChange = onJsonChanged,
                            label = { Text("Widget JSON DSL") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            shape = MaterialTheme.shapes.medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onApplyJson, shape = MaterialTheme.shapes.medium) {
                            Text("JSON 적용")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(onClick = onBackClicked, shape = MaterialTheme.shapes.medium) {
                        Text("이전")
                    }
                    Button(onClick = onSave, shape = MaterialTheme.shapes.medium) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("위젯 저장", fontWeight = FontWeight.Bold)
                    }
                }
            }
            GenerationState.Idle -> { /* Idle */ }
        }
    }
}

@Composable
private fun SaveConfirmationStep(
    widgetName: String,
    onDoneClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "🎉", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "위젯이 저장되었습니다!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"$widgetName\"",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "홈 화면을 길게 누르고 '위젯'을 선택한 뒤\n'Custom Widgets'를 추가해주세요.",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDoneClicked, shape = MaterialTheme.shapes.medium) {
                    Text("완료", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
