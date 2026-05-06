package com.example.mahalleustasi.presentation.screens.job

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.Job
import com.example.mahalleustasi.domain.model.JobCategory
import com.example.mahalleustasi.domain.model.JobLocation
import com.example.mahalleustasi.domain.repository.JobRepository
import com.example.mahalleustasi.domain.location.LocationTracker
import com.example.mahalleustasi.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class JobCreateUiState(
    val title: String = "",
    val description: String = "",
    val category: JobCategory = JobCategory.OTHER,
    val address: String = "",
    val budget: String = "",
    val savedAddresses: List<com.example.mahalleustasi.domain.model.SavedAddress> = emptyList(),
    val useCurrentLocation: Boolean = true, // Varsayılan olarak mevcut konum
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class JobCreateViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val userRepository: UserRepository,
    private val locationTracker: LocationTracker,
    private val auth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val titleArg = savedStateHandle.get<String>("title") ?: ""
    private val descArg = savedStateHandle.get<String>("desc") ?: ""
    private val budgetArg = savedStateHandle.get<String>("budget") ?: ""
    private val catArg = savedStateHandle.get<String>("cat") ?: ""

    private val initialCategory = try {
        if (catArg.isNotBlank()) JobCategory.valueOf(catArg) else JobCategory.OTHER
    } catch (e: Exception) {
        JobCategory.OTHER
    }

    private val _uiState = MutableStateFlow(JobCreateUiState(
        title = titleArg,
        description = descArg,
        budget = budgetArg,
        category = initialCategory
    ))
    val uiState = _uiState.asStateFlow()

    init {
        fetchSavedAddresses()
    }

    private fun fetchSavedAddresses() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.getUserById(userId).collect { result ->
                if (result is Resource.Success) {
                    _uiState.update { it.copy(savedAddresses = result.data.savedAddresses) }
                }
            }
        }
    }

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

    fun onUseCurrentLocationToggle(use: Boolean) {
        _uiState.update { it.copy(useCurrentLocation = use, address = if (use) "" else it.address) }
    }

    fun saveNewAddress(title: String, addressText: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // 1. Önce adresi Geocoder ile doğrula ve koordinatlarını al
            val coords = locationTracker.getCoordinatesFromAddress(addressText)
            
            if (coords == null) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = "Bu adres haritada bulunamadı. Lütfen daha açık/detaylı yazın."
                    ) 
                }
                return@launch
            }

            // 2. Doğrulandıysa veritabanına kaydet
            val newAddress = com.example.mahalleustasi.domain.model.SavedAddress(
                id = UUID.randomUUID().toString(),
                title = title,
                addressText = addressText,
                lat = coords.first,
                lng = coords.second
            )
            
            when (userRepository.addSavedAddress(userId, newAddress)) {
                is Resource.Success -> {
                    // Adresi state'e ekleyip onu seçili hale getir
                    _uiState.update { it.copy(
                        address = addressText,
                        useCurrentLocation = false,
                        isLoading = false,
                        error = null
                    ) }
                    
                    // Adresleri yeniden çek (listeyi güncellemek için)
                    fetchSavedAddresses()
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = "Adres kaydedilemedi.") }
                }
                else -> Unit
            }
        }
    }

    fun submitJob() {
        val state = _uiState.value
        if (state.title.isBlank() || state.description.isBlank()) {
            _uiState.update { it.copy(error = "Lütfen başlık ve açıklama alanlarını doldurun.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            var lat = 0.0
            var lng = 0.0

            if (!state.useCurrentLocation && state.address.isNotBlank()) {
                // Kayıtlı adresler listesinden kontrol et (belki daha önce lat/lng kaydedilmiştir)
                val existingSaved = state.savedAddresses.find { it.addressText == state.address }
                if (existingSaved != null && existingSaved.lat != 0.0 && existingSaved.lng != 0.0) {
                    lat = existingSaved.lat
                    lng = existingSaved.lng
                } else {
                    // Geocoder ile bulmaya çalış
                    val coords = locationTracker.getCoordinatesFromAddress(state.address)
                    if (coords != null) {
                        lat = coords.first
                        lng = coords.second
                    }
                }
            }

            // Eğer "Şu Anki Konum" seçiliyse veya adres bulunamadıysa, GPS'ten mevcut konumu çek
            if (state.useCurrentLocation || (lat == 0.0 && lng == 0.0)) {
                val location = locationTracker.getCurrentLocation()
                lat = location?.latitude ?: 0.0
                lng = location?.longitude ?: 0.0
            }
            
            val newJob = Job(
                title = state.title,
                description = state.description,
                category = state.category,
                location = JobLocation(
                    address = state.address,
                    lat = lat,
                    lng = lng
                ),
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
