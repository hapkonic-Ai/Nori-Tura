package com.nonituracare.presentation.ipd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonituracare.data.ConsentRepository
import com.nonituracare.data.dto.ConsentFormDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Read-only consent view: this consent was already generated (and, in the
 * printed copy, physically signed by hand). The app never signs it — this
 * screen just shows what was generated and lets the nurse download/open the
 * PDF again, optionally in the other language.
 */
class ConsentViewViewModel(
    private val consentId: String,
    private val repository: ConsentRepository = ConsentRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    init {
        loadConsent()
    }

    fun loadConsent() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.getConsentForm(consentId)
                .onSuccess { consent ->
                    _uiState.value = UiState.Success(consent)
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to load consent form")
                }
        }
    }

    /** Fetches a fresh download URL, optionally re-rendering in [language]. */
    fun downloadPdf(language: String? = null) {
        _downloadState.value = DownloadState.Loading
        viewModelScope.launch {
            repository.getConsentDownloadUrl(consentId, language)
                .onSuccess { response ->
                    if (response.pdfUrl != null) {
                        _downloadState.value = DownloadState.Ready(response.pdfUrl)
                    } else {
                        _downloadState.value = DownloadState.Error("PDF is not available yet")
                    }
                }
                .onFailure { error ->
                    _downloadState.value = DownloadState.Error(error.message ?: "Failed to download consent PDF")
                }
        }
    }

    fun resetDownloadState() {
        _downloadState.value = DownloadState.Idle
    }

    sealed class UiState {
        object Loading : UiState()
        data class Success(val consent: ConsentFormDto) : UiState()
        data class Error(val message: String) : UiState()
    }

    sealed class DownloadState {
        object Idle : DownloadState()
        object Loading : DownloadState()
        data class Ready(val pdfUrl: String) : DownloadState()
        data class Error(val message: String) : DownloadState()
    }
}
