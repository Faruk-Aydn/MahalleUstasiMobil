package com.example.mahalleustasi.presentation.screens.rental

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.Rental
import com.example.mahalleustasi.domain.repository.RentalRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RentalDetailUiState(
    val rental: Rental?       = null,
    val isLoading: Boolean    = false,
    val isOwner: Boolean      = false,
    val error: String?        = null,
    val actionSuccess: String? = null
)

@HiltViewModel
class RentalDetailViewModel @Inject constructor(
    private val rentalRepository: RentalRepository,
    private val auth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val rentalId: String = savedStateHandle.get<String>("rentalId") ?: ""

    private val _uiState = MutableStateFlow(RentalDetailUiState())
    val uiState = _uiState.asStateFlow()

    val currentUserId: String get() = auth.currentUser?.uid ?: ""

    init { loadRental() }

    private fun loadRental() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = rentalRepository.getRentalById(rentalId)
            when (result) {
                is Resource.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        rental    = result.data,
                        isOwner   = result.data?.ownerId == currentUserId
                    )
                }
                is Resource.Error   -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> Unit
            }
        }
    }

    fun toggleAvailability() {
        val rental = _uiState.value.rental ?: return
        viewModelScope.launch {
            val result = rentalRepository.updateAvailability(rental.id, !rental.isAvailable)
            if (result is Resource.Success) {
                _uiState.update {
                    it.copy(
                        rental        = rental.copy(isAvailable = !rental.isAvailable),
                        actionSuccess = if (!rental.isAvailable) "İlan aktif edildi" else "İlan pasife alındı"
                    )
                }
            } else if (result is Resource.Error) {
                _uiState.update { it.copy(error = result.message) }
            }
        }
    }

    fun deleteRental(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val result = rentalRepository.deleteRental(rentalId)
            if (result is Resource.Success) onDeleted()
            else if (result is Resource.Error) _uiState.update { it.copy(error = result.message) }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, actionSuccess = null) }
    }
}
