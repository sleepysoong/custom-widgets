package com.customwidgets.app.ui.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.customwidgets.app.data.repository.WidgetRepository
import com.customwidgets.app.domain.model.WidgetDefinition
import com.customwidgets.app.domain.model.WidgetMetadata
import com.customwidgets.app.ui.preview.ComposeWidgetPreview
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetDetailScreen(
    widgetId: Long,
    repository: WidgetRepository,
    onNavigateBack: () -> Unit
) {
    var widget by remember { mutableStateOf<WidgetMetadata?>(null) }
    var editableJson by remember { mutableStateOf("") }
    var isEditingJson by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(widgetId) {
        val loaded = repository.getWidgetById(widgetId)
        widget = loaded
        editableJson = loaded?.definitionJson ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(widget?.name ?: "Widget Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val currentWidget = widget
        if (currentWidget != null) {
            val definition = remember(currentWidget.definitionJson) {
                try {
                    WidgetDefinition.fromJson(currentWidget.definitionJson)
                } catch (_: Exception) {
                    null
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Live Preview (${currentWidget.sizeLabel})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((currentWidget.heightCells * 80).coerceIn(140, 320).dp)
                ) {
                    if (definition != null) {
                        ComposeWidgetPreview(
                            definition = definition,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("설명 / Description:", fontWeight = FontWeight.Bold)
                        Text(currentWidget.description.ifBlank { "(설명 없음)" })
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("활성 인스턴스 수: ${currentWidget.appWidgetIds.size}개", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isEditingJson) {
                    OutlinedTextField(
                        value = editableJson,
                        onValueChange = { editableJson = it },
                        label = { Text("Widget JSON DSL") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isEditingJson = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("취소")
                        }
                        Button(
                            onClick = {
                                try {
                                    WidgetDefinition.fromJson(editableJson)
                                    scope.launch {
                                        repository.updateWidget(currentWidget.copy(definitionJson = editableJson))
                                        widget = currentWidget.copy(definitionJson = editableJson)
                                        isEditingJson = false
                                        errorMessage = null
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Invalid JSON: ${e.message}"
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("저장")
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { isEditingJson = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("JSON 수동 편집 / Edit JSON")
                    }
                }

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
