package com.customwidgets.app.ui.mcp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import com.customwidgets.app.ui.glass.GlassDialog
import com.customwidgets.app.ui.glass.GlassButton
import androidx.compose.material3.CircularProgressIndicator
import com.customwidgets.app.ui.glass.GlassCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import com.customwidgets.app.ui.glass.GlassIconButton
import androidx.compose.material3.MaterialTheme
import com.customwidgets.app.ui.glass.GlassTextField
import androidx.compose.material3.Scaffold
import com.customwidgets.app.ui.glass.GlassSurface
import com.customwidgets.app.ui.glass.GlassSwitch
import androidx.compose.material3.Text
import com.customwidgets.app.ui.glass.GlassSecondaryButton
import com.customwidgets.app.ui.glass.GlassTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.customwidgets.app.mcp.model.McpServerConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McpServerScreen(
    viewModel: McpServerViewModel,
    onNavigateBack: () -> Unit
) {
    val servers by viewModel.servers.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var newServerName by rememberSaveable { mutableStateOf("") }
    var newServerUrl by rememberSaveable { mutableStateOf("") }
    var newServerApiKey by rememberSaveable { mutableStateOf("") }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            GlassTopAppBar(
                title = { Text("MCP 서버 관리", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    GlassIconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    GlassIconButton(onClick = { viewModel.toggleAddDialog(true) }) {
                        Icon(Icons.Default.Add, contentDescription = "MCP 서버 추가")
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
            if (servers.isEmpty()) {
                EmptyMcpState(onAddClicked = { viewModel.toggleAddDialog(true) })
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentPadding = PaddingValues(bottom = 104.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        GlassSurface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Model Context Protocol (MCP)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "MCP 서버를 등록하면 AI가 서버가 제공하는 외부 도구(날씨, 금융, 캘린더 등)를 이용해 위젯의 실시간 데이터를 바인딩합니다.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    items(servers, key = { it.id }) { server ->
                        McpServerCard(
                            server = server,
                            tools = uiState.serverToolsMap[server.id] ?: emptyList(),
                            isTesting = uiState.testingServerId == server.id,
                            onToggleEnabled = { enabled -> viewModel.toggleServer(server.id, enabled) },
                            onTestClicked = { viewModel.testServer(server) },
                            onDeleteClicked = { viewModel.deleteServer(server.id) }
                        )
                    }
                }
            }
        }
    }

    // Add Server Dialog
    if (uiState.isAddDialogOpen) {
        GlassDialog(
            onDismissRequest = { viewModel.toggleAddDialog(false) },
            title = { Text("새 MCP 서버 등록", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassTextField(
                        value = newServerName,
                        onValueChange = { newServerName = it },
                        label = { Text("서버 이름 (예: WeatherHub)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    GlassTextField(
                        value = newServerUrl,
                        onValueChange = { newServerUrl = it },
                        label = { Text("서버 URL (HTTP/SSE)") },
                        placeholder = { Text("https://mcp.example.com/sse") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    GlassTextField(
                        value = newServerApiKey,
                        onValueChange = { newServerApiKey = it },
                        label = { Text("API Key (선택)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                GlassButton(
                    onClick = {
                        viewModel.addServer(newServerName, newServerUrl, newServerApiKey)
                        newServerName = ""
                        newServerUrl = ""
                        newServerApiKey = ""
                    },
                    enabled = newServerName.isNotBlank() && newServerUrl.isNotBlank()
                ) {
                    Text("등록")
                }
            },
            dismissButton = {
                GlassSecondaryButton(onClick = { viewModel.toggleAddDialog(false) }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
private fun McpServerCard(
    server: McpServerConfig,
    tools: List<com.customwidgets.app.mcp.model.McpTool>,
    isTesting: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onTestClicked: () -> Unit,
    onDeleteClicked: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = server.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = server.url,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                GlassSwitch(
                    checked = server.isEnabled,
                    modifier = Modifier.semantics { contentDescription = "${server.name} 사용" },
                    onCheckedChange = onToggleEnabled
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassSecondaryButton(
                    onClick = { isExpanded = !isExpanded }
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.height(16.dp))
                    Spacer(modifier = Modifier.padding(2.dp))
                    Text(
                        if (tools.isNotEmpty()) "${tools.size}개 도구 사용 가능" else "도구 확인",
                        fontSize = 12.sp
                    )
                }

                Row {
                    GlassIconButton(
                        onClick = onTestClicked, enabled = !isTesting,
                        modifier = Modifier.semantics { contentDescription = "연결 테스트" }
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.height(16.dp))
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Test connection")
                        }
                    }
                    GlassIconButton(onClick = onDeleteClicked) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded && tools.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    tools.forEach { tool ->
                        GlassSurface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = tool.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                if (tool.description.isNotBlank()) {
                                    Text(
                                        text = tool.description,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMcpState(onAddClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GlassSurface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "등록된 MCP 서버가 없습니다",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "MCP(Model Context Protocol) 서버를 추가하면\n날씨, 금융, 암호화폐 등의 라이브 데이터를 위젯에 실시간 바인딩할 수 있습니다.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        GlassButton(onClick = onAddClicked) {
            Text("MCP 서버 추가하기")
        }
    }
}
