package com.example.nori_tura.presentation.surgeon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nori_tura.data.FollowUpRepository
import com.example.nori_tura.data.dto.SendMessageResponse
import com.example.nori_tura.data.dto.WhatsAppPreviewDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WhatsAppPreviewViewModel(
    private val recordId: String,
    private val repository: FollowUpRepository = FollowUpRepository()
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Success(
            val preview: WhatsAppPreviewDto,
            val editedBody: String,
            val sendState: SendState = SendState.Idle
        ) : UiState()
        data class Error(val message: String) : UiState()
    }

    sealed class SendState {
        object Idle : SendState()
        object Sending : SendState()
        data class Sent(val response: SendMessageResponse) : SendState()
        data class Failed(val message: String) : SendState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadPreview()
    }

    fun loadPreview() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.getPreview(recordId)
                .onSuccess { preview ->
                    _uiState.value = UiState.Success(preview = preview, editedBody = preview.body)
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to load preview")
                }
        }
    }

    fun onMessageChanged(newBody: String) {
        val current = _uiState.value as? UiState.Success ?: return
        _uiState.value = current.copy(editedBody = newBody, sendState = SendState.Idle)
    }

    fun sendMessage(channel: String) {
        val current = _uiState.value as? UiState.Success ?: return
        _uiState.value = current.copy(sendState = SendState.Sending)

        viewModelScope.launch {
            repository.sendMessage(
                recordId = recordId,
                channel = channel,
                message = current.editedBody.takeIf { it != current.preview.body }
            )
                .onSuccess { response ->
                    _uiState.value = current.copy(sendState = SendState.Sent(response))
                }
                .onFailure { error ->
                    _uiState.value = current.copy(sendState = SendState.Failed(error.message ?: "Send failed"))
                }
        }
    }
}
