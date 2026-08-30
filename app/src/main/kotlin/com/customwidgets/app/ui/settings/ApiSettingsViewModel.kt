package com.customwidgets.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.customwidgets.app.ai.AiConfigStore
import com.customwidgets.app.ai.AiService
import com.customwidgets.app.ai.model.AiConfig
import com.customwidgets.app.ai.model.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ApiSettingsUiState(
    val baseUrl: String = "https://api.openai.com",
    val apiKey: String = "",
    val model: String = "gpt-4o-mini",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val isTesting: Boolean = false,
    val testSuccess: Boolean? = null,
    val testMessage: String? = null,
    val isSaved: Boolean = false
)

@HiltViewModel
class ApiSettingsViewModel @Inject constructor(
    private val aiConfigStore: AiConfigStore,
    private val aiService: AiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApiSettingsUiState())
    val uiState: StateFlow<ApiSettingsUiState> = _uiState.asStateFlow()

    init {
        val current = aiConfigStore.config.value
        _uiState.update {
            it.copy(
                baseUrl = current.baseUrl,
                apiKey = current.apiKey,
                model = current.model,
                temperature = current.temperature.toFloat(),
                maxTokens = current.maxTokens
            )
        }
    }

    fun setBaseUrl(url: String) {
        _uiState.update { it.copy(baseUrl = url, isSaved = false, testSuccess = null) }
    }

    fun setApiKey(key: String) {
        _uiState.update { it.copy(apiKey = key, isSaved = false, testSuccess = null) }
    }

    fun setModel(model: String) {
        _uiState.update { it.copy(model = model, isSaved = false, testSuccess = null) }
    }

    fun setTemperature(temperature: Float) {
        _uiState.update { it.copy(temperature = temperature, isSaved = false) }
    }

    fun setMaxTokens(tokens: Int) {
        _uiState.update { it.copy(maxTokens = tokens, isSaved = false) }
    }

    fun applyPreset(presetName: String) {
        when (presetName) {
            "OpenAI" -> _uiState.update {
                it.copy(
                    baseUrl = "https://api.openai.com",
                    model = "gpt-4o-mini"
                )
            }
            "Ollama" -> _uiState.update {
                it.copy(
                    baseUrl = "http://10.0.2.2:11434", // Android emulator localhost
                    model = "llama3.2"
                )
            }
            "LM Studio" -> _uiState.update {
                it.copy(
                    baseUrl = "http://10.0.2.2:1234",
                    model = "local-model"
                )
            }
        }
    }

    fun testConnection() {
        val state = _uiState.value
        _uiState.update { it.copy(isTesting = true, testSuccess = null, testMessage = null) }

        val testConfig = AiConfig(
            baseUrl = state.baseUrl,
            apiKey = state.apiKey,
            model = state.model,
            temperature = 0.1,
            maxTokens = 50
        )

        viewModelScope.launch {
            try {
                val response = aiService.chatCompletionSync(
                    messages = listOf(ChatMessage("user", "Respond with exact word OK")),
                    config = testConfig,
                    useJsonMode = false
                )
                _uiState.update {
                    it.copy(
                        isTesting = false,
                        testSuccess = true,
                        testMessage = "연결 성공! 응답: $response"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isTesting = false,
                        testSuccess = false,
                        testMessage = "연결 실패: ${e.message}"
                    )
                }
            }
        }
    }

    fun saveSettings() {
        val state = _uiState.value
        val config = AiConfig(
            baseUrl = state.baseUrl,
            apiKey = state.apiKey,
            model = state.model,
            temperature = state.temperature.toDouble(),
            maxTokens = state.maxTokens
        )
        aiConfigStore.saveConfig(config)
        _uiState.update { it.copy(isSaved = true) }
    }
}
