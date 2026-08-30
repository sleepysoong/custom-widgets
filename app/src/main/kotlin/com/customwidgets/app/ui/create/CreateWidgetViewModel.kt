package com.customwidgets.app.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.customwidgets.app.ai.AiConfigStore
import com.customwidgets.app.ai.GenerationState
import com.customwidgets.app.ai.WidgetGenerationService
import com.customwidgets.app.data.repository.WidgetRepository
import com.customwidgets.app.domain.model.AppError
import com.customwidgets.app.domain.model.WidgetDefinition
import com.customwidgets.app.domain.model.WidgetMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateWidgetUiState(
    val currentStep: Int = 0, // 0: Size, 1: Describe, 2: Generating/Preview, 3: Saved
    val widthCells: Int = 2,
    val heightCells: Int = 2,
    val description: String = "",
    val widgetName: String = "",
    val generationState: GenerationState = GenerationState.Idle,
    val generatedDefinition: WidgetDefinition? = null,
    val rawJson: String = "",
    val isJsonEditorOpen: Boolean = false,
    val editableJson: String = "",
    val savedWidgetId: Long? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class CreateWidgetViewModel @Inject constructor(
    private val generationService: WidgetGenerationService,
    private val aiConfigStore: AiConfigStore,
    private val widgetRepository: WidgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateWidgetUiState())
    val uiState: StateFlow<CreateWidgetUiState> = _uiState.asStateFlow()

    fun selectSize(width: Int, height: Int) {
        _uiState.update {
            it.copy(
                widthCells = width,
                heightCells = height,
                currentStep = 1
            )
        }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun updateWidgetName(name: String) {
        _uiState.update { it.copy(widgetName = name) }
    }

    fun setStep(step: Int) {
        _uiState.update { it.copy(currentStep = step) }
    }

    fun generateWidget() {
        val state = _uiState.value
        if (state.description.isBlank()) return

        val config = aiConfigStore.config.value

        _uiState.update {
            it.copy(
                currentStep = 2,
                generationState = GenerationState.Loading,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            generationService.generateWidget(
                description = state.description,
                widthCells = state.widthCells,
                heightCells = state.heightCells,
                config = config
            ).collect { genState ->
                when (genState) {
                    is GenerationState.Success -> {
                        _uiState.update {
                            it.copy(
                                generationState = genState,
                                generatedDefinition = genState.definition,
                                rawJson = genState.rawJson,
                                editableJson = genState.rawJson,
                                widgetName = if (it.widgetName.isBlank()) "Widget ${it.widthCells}x${it.heightCells}" else it.widgetName
                            )
                        }
                    }
                    is GenerationState.Error -> {
                        _uiState.update {
                            it.copy(
                                generationState = genState,
                                errorMessage = genState.error.userMessage
                            )
                        }
                    }
                    else -> {
                        _uiState.update { it.copy(generationState = genState) }
                    }
                }
            }
        }
    }

    fun toggleJsonEditor(open: Boolean) {
        _uiState.update { it.copy(isJsonEditorOpen = open) }
    }

    fun updateEditableJson(json: String) {
        _uiState.update { it.copy(editableJson = json) }
    }

    fun applyEditedJson() {
        val currentJson = _uiState.value.editableJson
        try {
            val updatedDef = WidgetDefinition.fromJson(currentJson)
            _uiState.update {
                it.copy(
                    generatedDefinition = updatedDef,
                    rawJson = currentJson,
                    isJsonEditorOpen = false,
                    errorMessage = null
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(errorMessage = "Invalid JSON: ${e.message}")
            }
        }
    }

    fun saveWidget(appWidgetId: Int? = null, onComplete: (Long) -> Unit = {}) {
        val state = _uiState.value
        val definition = state.generatedDefinition ?: return
        val jsonString = state.rawJson.ifBlank { WidgetDefinition.toJson(definition) }

        viewModelScope.launch {
            val metadata = WidgetMetadata(
                name = state.widgetName.ifBlank { "Custom Widget ${state.widthCells}x${state.heightCells}" },
                description = state.description,
                widthCells = state.widthCells,
                heightCells = state.heightCells,
                definitionJson = jsonString
            )

            val savedId = widgetRepository.saveWidget(metadata)

            if (appWidgetId != null && appWidgetId != android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) {
                widgetRepository.linkAppWidgetId(savedId, appWidgetId)
            }

            _uiState.update {
                it.copy(
                    savedWidgetId = savedId,
                    currentStep = 3
                )
            }

            onComplete(savedId)
        }
    }
}
