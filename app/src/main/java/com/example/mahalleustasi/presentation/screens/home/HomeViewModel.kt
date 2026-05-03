package com.example.mahalleustasi.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.mahalleustasi.domain.model.Job
import com.example.mahalleustasi.domain.repository.JobRepository
import com.example.mahalleustasi.core.util.Resource
import javax.inject.Inject

data class HomeUiState(
    val jobs: List<Job>    = emptyList(),
    val isLoading: Boolean = false,
    val error: String?     = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val jobRepository: JobRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadRealJobs()
    }

    private fun loadRealJobs() {
        viewModelScope.launch {
            jobRepository.getJobs().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                    is Resource.Success -> {
                        _uiState.update { it.copy(isLoading = false, jobs = result.data) }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                    is Resource.Idle -> {
                        // Do nothing
                    }
                }
            }
        }
    }
}
