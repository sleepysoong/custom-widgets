package com.customwidgets.app.ui.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.customwidgets.app.R
import com.customwidgets.app.ai.GenerationState
import com.customwidgets.app.ui.preview.ComposeWidgetPreview

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
    "Minimal Clock" to "미니멀한 디지털 시계 위젯, 날짜와 시간 표시",
    "Battery Monitor" to "배터리 잔량 표시 및 상태 위젯",
    "Quick Launcher" to "자주 쓰는 앱 빠른 실행 버튼 바",
    "Daily Quote" to "매일 새로운 영감을 주는 명언 위젯",
    "System Dashboard" to "시간, 날짜, 배터리 통합 시스템 정보 대시보드"
)

@Composable
fun CreateWidgetScreen(
    viewModel: CreateWidgetViewModel,
    appWidgetId: Int? = null,
    onNavigateBack: () -> Unit = {},
    onWidgetCreated: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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

@Composable
private fun SizeSelectionStep(
    selectedWidth: Int,
    selectedHeight: Int,
    onSelectSize: (Int, Int) -> Unit
) {
    Text(
        text = "위젯 크기 선택 / Select Size",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(SIZE_OPTIONS) { opt ->
            val isSelected = opt.width == selectedWidth && opt.height == selectedHeight
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelectSize(opt.width, opt.height) }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = opt.label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${opt.width}x${opt.height}",
                            fontSize = 12.sp,
                            color = Color.Gray
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
            text = "위젯 설명 입력 / Describe Widget (${width}x${height})",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "템플릿 예시 / Suggestions:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
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
            label = { Text("원하는 위젯의 내용을 설명해주세요") },
            placeholder = { Text("예: 상단에 시계와 날짜가 있고 아래에 배터리와 리프레시 버튼이 있는 위젯") },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBackClicked) {
                Text("이전 / Back")
            }
            Button(
                onClick = onGenerateClicked,
                enabled = description.isNotBlank()
            ) {
                Text("AI 생성 / Generate")
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
                        Text("AI가 위젯을 디자인하는 중...")
                    }
                }
            }
            is GenerationState.Streaming -> {
                Column {
                    Text("생성 중... / Streaming:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = genState.partialText.takeLast(500),
                            color = Color(0xFF00FF00),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            is GenerationState.Error -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "생성 오류 / Error",
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
                Button(onClick = onRegenerate) {
                    Text("다시 시도 / Retry")
                }
            }
            is GenerationState.Success -> {
                Text(
                    text = "위젯 미리보기 / Preview (${uiState.widthCells}x${uiState.heightCells})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // In-app Live Compose Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((uiState.heightCells * 80).coerceIn(120, 300).dp)
                        .padding(horizontal = 16.dp)
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
                    label = { Text("위젯 이름 / Widget Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onToggleJsonEditor(!uiState.isJsonEditorOpen) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (uiState.isJsonEditorOpen) "JSON 닫기" else "JSON 편집")
                    }
                    OutlinedButton(
                        onClick = onRegenerate,
                        modifier = Modifier.weight(1f)
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
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onApplyJson) {
                            Text("JSON 적용 / Apply")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(onClick = onBackClicked) {
                        Text("수정 / Edit")
                    }
                    Button(onClick = onSave) {
                        Text("위젯 저장 / Save")
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
        Text(
            text = "🎉",
            fontSize = 48.sp
        )
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
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "홈 화면을 길게 누르고 '위젯'을 선택한 뒤\n'Custom Widgets'를 추가해주세요.",
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onDoneClicked) {
            Text("완료 / Done")
        }
    }
}
