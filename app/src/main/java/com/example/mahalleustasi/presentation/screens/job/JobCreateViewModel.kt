package com.example.mahalleustasi.presentation.screens.job

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.Job
import com.example.mahalleustasi.domain.model.JobCategory
import com.example.mahalleustasi.domain.model.JobLocation
import com.example.mahalleustasi.domain.repository.JobRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JobCreateUiState(
    val title: String = "",
    val description: String = "",
    val category: JobCategory = JobCategory.OTHER,
    val address: String = "",
    val budget: String = "",
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class JobCreateViewModel @Inject constructor(
    private val jobRepository: JobRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(JobCreateUiState())
    val uiState = _uiState.asStateFlow()

    fun onTitleChange(title: String) {
        _uiState.update { it.copy(title = title, error = null) }
    }

    fun onDescriptionChange(desc: String) {
        _uiState.update { it.copy(description = desc, error = null) }
    }

    fun onCategoryChange(category: JobCategory) {
        _uiState.update { it.copy(category = category, error = null) }
    }

    fun onAddressChange(address: String) {
        _uiState.update { it.copy(address = address, error = null) }
    }

    fun onBudgetChange(budget: String) {
        _uiState.update { it.copy(budget = budget, error = null) }
    }

    fun submitJob() {
        val state = _uiState.value
        if (state.title.isBlank() || state.description.isBlank()) {
            _uiState.update { it.copy(error = "Lütfen başlık ve açıklama alanlarını doldurun.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val newJob = Job(
                title = state.title,
                description = state.description,
                category = state.category,
                location = JobLocation(address = state.address),
                budget = state.budget.takeIf { it.isNotBlank() }
            )

            when (val result = jobRepository.createJob(newJob)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, success = true) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> Unit
            }
        }
    }
}
