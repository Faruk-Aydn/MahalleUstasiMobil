package com.example.mahalleustasi.presentation.screens.rental

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.Rental
import com.example.mahalleustasi.domain.model.RentalCategory
import com.example.mahalleustasi.domain.repository.RentalRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RentalListUiState(
    val rentals: List<Rental>         = emptyList(),
    val filteredRentals: List<Rental> = emptyList(),
    val selectedCategory: RentalCategory? = null,
    val searchQuery: String           = "",
    val isLoading: Boolean            = false,
    val error: String?                = null
)

@HiltViewModel
class RentalListViewModel @Inject constructor(
    private val rentalRepository: RentalRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    val currentUserId: String get() = auth.currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow(RentalListUiState())
    val uiState = _uiState.asStateFlow()

    init { loadRentals() }

    private fun loadRentals() {
        viewModelScope.launch {
            rentalRepository.getRentals().collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Resource.Success -> {
                        val rentals = result.data ?: emptyList()
                        _uiState.update { it.copy(isLoading = false, rentals = rentals) }
                        applyFilters()
                    }
                    is Resource.Error   -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                    else -> Unit
                }
            }
        }
    }

    fun setCategory(category: RentalCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
        applyFilters()
    }

    fun setSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        var list = state.rentals

        if (state.selectedCategory != null)
            list = list.filter { it.category == state.selectedCategory }

        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.lowercase()
            list = list.filter { it.title.lowercase().contains(q) || it.description.lowercase().contains(q) }
        }

        _uiState.update { it.copy(filteredRentals = list) }
    }
}
