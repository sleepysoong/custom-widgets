package com.customwidgets.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.customwidgets.app.ui.glass.GlassButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import com.customwidgets.app.ui.glass.GlassCard
import androidx.compose.material3.ExperimentalMaterial3Api
import com.customwidgets.app.ui.glass.GlassFilterChip
import androidx.compose.material3.Icon
import com.customwidgets.app.ui.glass.GlassIconButton
import androidx.compose.material3.MaterialTheme
import com.customwidgets.app.ui.glass.GlassTextField
import androidx.compose.material3.Scaffold
import com.customwidgets.app.ui.glass.GlassSlider
import com.customwidgets.app.ui.glass.GlassSurface
import androidx.compose.material3.Text
import com.customwidgets.app.ui.glass.GlassSecondaryButton
import com.customwidgets.app.ui.glass.GlassTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.customwidgets.app.ai.model.AiConfig
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiSettingsScreen(
    viewModel: ApiSettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var isPasswordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            GlassTopAppBar(
                title = { Text("OpenAI API 설정", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    GlassIconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Info Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🤖 OpenAI 전용 위젯 생성 엔진",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "OpenAI API 키를 등록하면 최신 GPT 모델과 등록된 MCP 도구를 결합하여 아름다운 맞춤형 위젯을 제작합니다.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    GlassSecondaryButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://platform.openai.com/api-keys"))
                            context.startActivity(intent)
                        }
                    ) {
                        Text("OpenAI API 키 발급받기 →", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Settings Form Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "OpenAI API Key",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    GlassTextField(
                        value = uiState.apiKey,
                        onValueChange = { viewModel.setApiKey(it) },
                        placeholder = { Text("sk-proj-...") },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            GlassIconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Text(if (isPasswordVisible) "숨김" else "표시", fontSize = 12.sp)
                            }
                        },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "모델 선택 / Model",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AiConfig.SUPPORTED_MODELS.forEach { (modelId, desc) ->
                            val isSelected = uiState.model == modelId
                            GlassFilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setModel(modelId) },
                                label = {
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text(modelId, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                        Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "창의성 (Temperature): ${String.format(Locale.US, "%.1f", uiState.temperature)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    GlassSlider(
                        value = uiState.temperature,
                        modifier = Modifier.semantics { contentDescription = "창의성" },
                        onValueChange = { viewModel.setTemperature(it) },
                        valueRange = 0.0f..1.5f,
                        steps = 14
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    GlassTextField(
                        value = uiState.maxTokens.toString(),
                        onValueChange = { str ->
                            str.toIntOrNull()?.let { viewModel.setMaxTokens(it) }
                        },
                        label = { Text("최대 토큰 (Max Tokens)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Test Message Banner
            val testMsg = uiState.testMessage
            if (testMsg != null) {
                val isSuccess = uiState.testSuccess == true
                GlassSurface(
                    color = if (isSuccess) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = testMsg,
                        color = if (isSuccess) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (uiState.isSaved) {
                Text(
                    text = "설정이 저장되었습니다! ✓",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassButton(
                    onClick = { viewModel.testConnection() },
                    enabled = !uiState.isTesting,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.isTesting) {
                        CircularProgressIndicator(modifier = Modifier.height(16.dp))
                        Spacer(Modifier.padding(4.dp))
                    }
                    Text("연결 테스트")
                }

                GlassButton(
                    onClick = { viewModel.saveSettings() },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("설정 저장", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
