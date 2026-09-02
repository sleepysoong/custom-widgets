package com.customwidgets.app.ui.mcp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.customwidgets.app.mcp.McpClient
import com.customwidgets.app.mcp.McpServerRepository
import com.customwidgets.app.mcp.model.McpServerConfig
import com.customwidgets.app.mcp.model.McpTool
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class McpServerUiState(
    val serverToolsMap: Map<Long, List<McpTool>> = emptyMap(),
    val testingServerId: Long? = null,
    val testStatusMessage: String? = null,
    val isAddDialogOpen: Boolean = false
)

@HiltViewModel
class McpServerViewModel @Inject constructor(
    private val repository: McpServerRepository,
    private val mcpClient: McpClient
) : ViewModel() {

    val servers: StateFlow<List<McpServerConfig>> = repository.getAllServers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(McpServerUiState())
    val uiState: StateFlow<McpServerUiState> = _uiState.asStateFlow()

    fun addServer(name: String, url: String, apiKey: String? = null) {
        if (name.isBlank() || url.isBlank()) return
        viewModelScope.launch {
            val id = repository.saveServer(
                McpServerConfig(
                    name = name.trim(),
                    url = url.trim(),
                    apiKey = apiKey?.trim()?.ifBlank { null },
                    isEnabled = true
                )
            )
            toggleAddDialog(false)
            testServer(McpServerConfig(id = id, name = name, url = url, apiKey = apiKey))
        }
    }

    fun deleteServer(id: Long) {
        viewModelScope.launch {
            repository.deleteServer(id)
            _uiState.update {
                it.copy(serverToolsMap = it.serverToolsMap - id)
            }
        }
    }

    fun toggleServer(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleServer(id, enabled)
        }
    }

    fun toggleAddDialog(open: Boolean) {
        _uiState.update { it.copy(isAddDialogOpen = open) }
    }

    fun testServer(server: McpServerConfig) {
        _uiState.update {
            it.copy(
                testingServerId = server.id,
                testStatusMessage = null
            )
        }

        viewModelScope.launch {
            val tools = mcpClient.listTools(server)
            _uiState.update {
                it.copy(
                    testingServerId = null,
                    serverToolsMap = it.serverToolsMap + (server.id to tools),
                    testStatusMessage = if (tools.isNotEmpty()) {
                        "연결 성공! ${tools.size}개 도구 발견 (${tools.joinToString { t -> t.name }})"
                    } else {
                        "서버에 연결되었으나 사용 가능한 도구가 없습니다."
                    }
                )
            }
        }
    }
}
