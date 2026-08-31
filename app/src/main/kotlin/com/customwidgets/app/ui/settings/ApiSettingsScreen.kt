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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.customwidgets.app.ai.model.AiConfig
import com.customwidgets.app.ui.theme.LiquidGlassBackground
import com.customwidgets.app.ui.theme.LiquidGlassButton
import com.customwidgets.app.ui.theme.LiquidGlassSurface
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

    LiquidGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("OpenAI API 설정", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
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
                LiquidGlassSurface(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🤖 OpenAI 전용 위젯 생성 엔진",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "OpenAI API 키를 입력하면 AI가 위젯의 레이아웃과 디자인을 자동으로 구성합니다.",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://platform.openai.com/api-keys"))
                                context.startActivity(intent)
                            }
                        ) {
                            Text("OpenAI API 키 발급받기 →", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Settings Form
                LiquidGlassSurface(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "OpenAI API Key",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = uiState.apiKey,
                            onValueChange = { viewModel.setApiKey(it) },
                            placeholder = { Text("sk-proj-...") },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Text(if (isPasswordVisible) "숨김" else "표시", fontSize = 12.sp)
                                }
                            },
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
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setModel(modelId) },
                                    label = {
                                        Column {
                                            Text(modelId, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                            Text(desc, fontSize = 10.sp, color = Color.Gray)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "창의성 (Temperature): ${String.format(Locale.US, "%.1f", uiState.temperature)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = uiState.temperature,
                            onValueChange = { viewModel.setTemperature(it) },
                            valueRange = 0.0f..1.5f,
                            steps = 14
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = uiState.maxTokens.toString(),
                            onValueChange = { str ->
                                str.toIntOrNull()?.let { viewModel.setMaxTokens(it) }
                            },
                            label = { Text("최대 토큰 (Max Tokens)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Test Message Banner
                uiState.testMessage?.let { msg ->
                    LiquidGlassSurface(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            color = if (uiState.testSuccess == true) Color(0xFF00E676) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (uiState.isSaved) {
                    Text(
                        text = "설정이 저장되었습니다! ✓",
                        color = Color(0xFF00E676),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LiquidGlassButton(
                        onClick = { viewModel.testConnection() },
                        enabled = !uiState.isTesting,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (uiState.isTesting) {
                            CircularProgressIndicator(modifier = Modifier.height(16.dp))
                        } else {
                            Text("연결 테스트")
                        }
                    }

                    LiquidGlassButton(
                        onClick = { viewModel.saveSettings() },
                        isPrimary = true,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("설정 저장", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
