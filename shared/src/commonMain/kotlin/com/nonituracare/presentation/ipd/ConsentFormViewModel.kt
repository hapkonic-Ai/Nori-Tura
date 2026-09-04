package com.nonituracare.presentation.ipd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nonituracare.data.ConsentFormResponse
import com.nonituracare.data.ConsentRepository
import com.nonituracare.data.SurgicalTemplateRepository
import com.nonituracare.data.dto.ConsentFormCreateRequest
import com.nonituracare.data.dto.ContentTemplateDto
import com.nonituracare.data.dto.SurgicalTemplateDto
import com.nonituracare.presentation.components.TemplatePickerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConsentFormViewModel(
    private val repository: ConsentRepository = ConsentRepository(),
    private val templateRepository: SurgicalTemplateRepository = SurgicalTemplateRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _templates = MutableStateFlow<List<SurgicalTemplateDto>>(emptyList())
    val templates: StateFlow<List<SurgicalTemplateDto>> = _templates.asStateFlow()

    private val _contentTemplates = MutableStateFlow<List<ContentTemplateDto>>(emptyList())
    val contentTemplates: StateFlow<List<ContentTemplateDto>> = _contentTemplates.asStateFlow()

    init {
        loadTemplates()
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            templateRepository.listTemplates()
                .onSuccess { _templates.value = it }
                .onFailure { /* templates are optional; picker will simply be empty */ }
        }
        viewModelScope.launch {
            templateRepository.getContentTemplates()
                .onSuccess { _contentTemplates.value = it }
                .onFailure { /* templates are optional; picker will simply be empty */ }
        }
    }

    /** Maps whatever the template picker returned into consent form fields. */
    fun applyPickerResult(result: TemplatePickerResult, language: String = "English"): PrefilledConsentFields =
        when (result) {
            is TemplatePickerResult.Blank -> PrefilledConsentFields()
            is TemplatePickerResult.Surgical -> applyTemplate(result.template)
            is TemplatePickerResult.Content -> applyContentTemplate(result.template, language)
        }

    /** Whether the given content template has Hindi content available yet. */
    fun hasHindiContent(template: ContentTemplateDto): Boolean =
        template.hiContentStatus != "missing"

    /**
     * Maps a surgical template into the subset of consent form fields that can be
     * safely prefilled.
     */
    private fun applyTemplate(template: SurgicalTemplateDto): PrefilledConsentFields {
        val procedureDescription = template.procedureDescription?.takeIf { it.isNotBlank() }
            ?: buildList {
                template.approach?.takeIf { it.isNotBlank() }?.let { add("Approach: $it") }
                template.technique?.takeIf { it.isNotBlank() }?.let { add("Technique: $it") }
                template.specialInstructions?.takeIf { it.isNotBlank() }?.let { add(it) }
            }.joinToString("\n\n")

        return PrefilledConsentFields(
            formType = template.name,
            procedure = template.procedure,
            anesthesia = template.anaesthesia.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "",
            procedureDescription = procedureDescription,
            risks = template.risks.takeIf { it.isNotEmpty() }?.joinToString("\n") ?: "",
            materialRisks = template.materialRisks?.takeIf { it.isNotBlank() }
                ?: template.complications.takeIf { it.isNotEmpty() }?.joinToString("\n") ?: "",
            possibleComplications = template.complications.takeIf { it.isNotEmpty() }?.joinToString("\n") ?: "",
            benefits = template.benefits.takeIf { it.isNotEmpty() }?.joinToString("\n") ?: "",
            alternatives = template.alternatives.takeIf { it.isNotEmpty() }?.joinToString("\n") ?: "",
            postOpCare = template.postOpCare ?: "",
            expectedRecovery = template.expectedRecovery ?: ""
        )
    }

    /**
     * Same mapping as [applyTemplate], but from an admin-curated global template.
     * Picks the `_en`/`_hi` bilingual column for [language], falling back to the
     * legacy single-language column for templates not yet migrated.
     */
    private fun applyContentTemplate(template: ContentTemplateDto, language: String): PrefilledConsentFields {
        val hindi = language == "Hindi"
        val procedureDescription = (if (hindi) template.procedureDescriptionHi else template.procedureDescriptionEn)
            ?: template.procedureDescription
        val anesthesiaList = (if (hindi) template.anaesthesiaHi else template.anaesthesiaEn).takeIf { it.isNotEmpty() }
            ?: template.anesthesia
        val risksList = (if (hindi) template.risksHi else template.risksEn).takeIf { it.isNotEmpty() } ?: template.risks
        val benefitsList = (if (hindi) template.benefitsHi else template.benefitsEn).takeIf { it.isNotEmpty() } ?: template.benefits
        val alternativesList = (if (hindi) template.alternativesHi else template.alternativesEn).takeIf { it.isNotEmpty() } ?: template.alternatives
        val complicationsList = (if (hindi) template.possibleComplicationsHi else template.possibleComplicationsEn).takeIf { it.isNotEmpty() }
            ?: template.possibleComplications
        val materialRisks = (if (hindi) template.materialRisksHi else template.materialRisksEn) ?: template.materialRisks
        val postOpCare = (if (hindi) template.postOpCareHi else template.postOpCareEn) ?: template.postOpCare
        val expectedRecovery = (if (hindi) template.expectedRecoveryHi else template.expectedRecoveryEn) ?: template.expectedRecovery

        val descriptionParts = buildList {
            if (!procedureDescription.isNullOrBlank()) {
                add(procedureDescription)
            } else {
                template.approach?.takeIf { it.isNotBlank() }?.let { add("Approach: $it") }
                template.technique?.takeIf { it.isNotBlank() }?.let { add("Technique: $it") }
                template.specialInstructions?.takeIf { it.isNotBlank() }?.let { add(it) }
            }
        }

        return PrefilledConsentFields(
            formType = template.name,
            procedure = template.procedure,
            anesthesia = anesthesiaList.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "",
            procedureDescription = descriptionParts.joinToString("\n\n"),
            risks = risksList.takeIf { it.isNotEmpty() }?.joinToString("\n") ?: "",
            materialRisks = materialRisks
                ?: complicationsList.takeIf { it.isNotEmpty() }?.joinToString("\n") ?: "",
            possibleComplications = complicationsList.takeIf { it.isNotEmpty() }?.joinToString("\n") ?: "",
            benefits = benefitsList.takeIf { it.isNotEmpty() }?.joinToString("\n") ?: "",
            alternatives = alternativesList.takeIf { it.isNotEmpty() }?.joinToString("\n") ?: "",
            postOpCare = postOpCare ?: "",
            expectedRecovery = expectedRecovery ?: "",
            contentTemplateId = template.id
        )
    }

    fun createConsentForm(request: ConsentFormCreateRequest) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.createConsentForm(request)
                .onSuccess { response ->
                    _uiState.value = UiState.Success(response)
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to create consent form")
                }
        }
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val response: ConsentFormResponse) : UiState()
        data class Error(val message: String) : UiState()
    }

    data class PrefilledConsentFields(
        val formType: String = "",
        val procedure: String = "",
        val anesthesia: String = "",
        val procedureDescription: String = "",
        val risks: String = "",
        val materialRisks: String = "",
        val possibleComplications: String = "",
        val benefits: String = "",
        val alternatives: String = "",
        val postOpCare: String = "",
        val expectedRecovery: String = "",
        val contentTemplateId: String? = null
    )
}
