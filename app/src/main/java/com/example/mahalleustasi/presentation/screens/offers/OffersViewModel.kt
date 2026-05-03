package com.example.mahalleustasi.presentation.screens.offers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.Offer
import com.example.mahalleustasi.domain.repository.OfferRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class OffersUiState(
    val myOffers: List<Offer> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OffersViewModel @Inject constructor(
    private val offerRepository: OfferRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(OffersUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadMyOffers()
    }

    fun loadMyOffers() {
        val userId = auth.currentUser?.uid ?: return
        
        offerRepository.getOffersByUserId(userId).onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true, error = null) }
                }
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, myOffers = result.data ?: emptyList()) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> Unit
            }
        }.launchIn(viewModelScope)
    }
}
