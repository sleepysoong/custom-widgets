package com.customwidgets.app.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.customwidgets.app.data.repository.WidgetRepository
import com.customwidgets.app.domain.model.WidgetMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WidgetGalleryViewModel @Inject constructor(
    private val widgetRepository: WidgetRepository
) : ViewModel() {

    val widgets: StateFlow<List<WidgetMetadata>> = widgetRepository.getAllWidgets()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteWidget(widgetId: Long) {
        viewModelScope.launch {
            widgetRepository.deleteWidget(widgetId)
        }
    }
}
