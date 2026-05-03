package com.example.mahalleustasi.presentation.screens.job

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.Job
import com.example.mahalleustasi.domain.model.Offer
import com.example.mahalleustasi.domain.repository.JobRepository
import com.example.mahalleustasi.domain.repository.OfferRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JobDetailUiState(
    val job: Job? = null,
    val offers: List<Offer> = emptyList(),
    val isLoading: Boolean = false,
    val isOfferLoading: Boolean = false,
    val offerSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class JobDetailViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val offerRepository: OfferRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val jobId: String = savedStateHandle.get<String>("jobId") ?: ""

    private val _uiState = MutableStateFlow(JobDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadJobDetail()
        loadOffers()
    }

    private fun loadOffers() {
        if (jobId.isBlank()) return
        
        offerRepository.getOffersByJobId(jobId).onEach { result ->
            when (result) {
                is Resource.Success -> {
                    _uiState.update { it.copy(offers = result.data ?: emptyList()) }
                }
                else -> Unit
            }
        }.launchIn(viewModelScope)
    }

    fun loadJobDetail() {
        if (jobId.isBlank()) {
            _uiState.update { it.copy(error = "Geçersiz ilan ID") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            when (val result = jobRepository.getJobById(jobId)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, job = result.data) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> Unit
            }
        }
    }

    fun submitOffer(price: Double, description: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isOfferLoading = true, offerSuccess = false, error = null) }
            
            val newOffer = Offer(
                jobId = jobId,
                price = price,
                description = description
            )
            
            when (val result = offerRepository.createOffer(newOffer)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isOfferLoading = false, offerSuccess = true) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isOfferLoading = false, error = result.message) }
                }
                else -> Unit
            }
        }
    }

    fun resetOfferSuccess() {
        _uiState.update { it.copy(offerSuccess = false) }
    }
}
