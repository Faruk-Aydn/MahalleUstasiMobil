package com.example.mahalleustasi.presentation.screens.rental

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.Rental
import com.example.mahalleustasi.domain.model.RentalCategory
import com.example.mahalleustasi.domain.repository.RentalRepository
import com.example.mahalleustasi.domain.repository.StorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RentalCreateUiState(
    val title: String              = "",
    val description: String        = "",
    val dailyPrice: String         = "",
    val depositAmount: String      = "",
    val category: RentalCategory   = RentalCategory.OTHER,
    val address: String            = "",
    val isLoading: Boolean         = false,
    val error: String?             = null,
    val isSuccess: Boolean         = false,
    val imageUri: String?          = null
)

@HiltViewModel
class RentalCreateViewModel @Inject constructor(
    private val rentalRepository: RentalRepository,
    private val storageRepository: StorageRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val navTitle = savedStateHandle.get<String>("title")?.takeIf { !it.startsWith("{") } ?: ""
    private val navDesc  = savedStateHandle.get<String>("desc")?.takeIf { !it.startsWith("{") } ?: ""
    private val navCat   = savedStateHandle.get<String>("cat")?.takeIf { !it.startsWith("{") } ?: ""
    private val navPrice = savedStateHandle.get<String>("price")?.takeIf { !it.startsWith("{") } ?: ""
    private val navImage = savedStateHandle.get<String>("imageUri")?.takeIf { !it.startsWith("{") } ?: ""

    private val _uiState = MutableStateFlow(
        RentalCreateUiState(
            title = navTitle,
            description = navDesc,
            dailyPrice = navPrice,
            category = try {
                if (navCat.isNotBlank()) RentalCategory.valueOf(navCat) else RentalCategory.OTHER
            } catch (e: Exception) {
                RentalCategory.OTHER
            },
            imageUri = if (navImage.isNotBlank()) navImage else null
        )
    )
    val uiState = _uiState.asStateFlow()

    fun onTitleChange(v: String)       = _uiState.update { it.copy(title = v) }
    fun onDescriptionChange(v: String) = _uiState.update { it.copy(description = v) }
    fun onDailyPriceChange(v: String)  = _uiState.update { it.copy(dailyPrice = v) }
    fun onDepositChange(v: String)     = _uiState.update { it.copy(depositAmount = v) }
    fun onCategoryChange(v: RentalCategory) = _uiState.update { it.copy(category = v) }
    fun onAddressChange(v: String)     = _uiState.update { it.copy(address = v) }
    fun onImageSelected(uri: String?)  = _uiState.update { it.copy(imageUri = uri) }

    fun submit() {
        val state = _uiState.value
        if (state.title.isBlank() || state.dailyPrice.isBlank()) {
            _uiState.update { it.copy(error = "Başlık ve günlük fiyat zorunludur.") }
            return
        }
        val price   = state.dailyPrice.toDoubleOrNull() ?: run {
            _uiState.update { it.copy(error = "Geçerli bir fiyat girin.") }
            return
        }
        val deposit = state.depositAmount.toDoubleOrNull() ?: 0.0

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // Eğer bir görsel seçilmişse önce onu yükle
            var uploadedPhotoUrl: String? = null
            if (!state.imageUri.isNullOrBlank() && !state.imageUri.startsWith("http")) {
                val uploadResult = storageRepository.uploadImage(
                    uri = android.net.Uri.parse(state.imageUri),
                    path = "rentals"
                )
                if (uploadResult is Resource.Success) {
                    uploadedPhotoUrl = uploadResult.data
                } else if (uploadResult is Resource.Error) {
                    _uiState.update { it.copy(isLoading = false, error = "Görsel yüklenemedi: ${uploadResult.message}") }
                    return@launch
                }
            } else if (!state.imageUri.isNullOrBlank() && state.imageUri.startsWith("http")) {
                uploadedPhotoUrl = state.imageUri
            }

            val rental = Rental(
                title         = state.title.trim(),
                description   = state.description.trim(),
                dailyPrice    = price,
                depositAmount = deposit,
                category      = state.category,
                photoUrls     = if (uploadedPhotoUrl != null) listOf(uploadedPhotoUrl) else emptyList()
            )
            val result = rentalRepository.createRental(rental)
            when (result) {
                is Resource.Success -> _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                is Resource.Error   -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> Unit
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
