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
import com.example.mahalleustasi.domain.location.LocationTracker
import com.example.mahalleustasi.core.util.Resource
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

data class HomeUiState(
    val jobs: List<Job>    = emptyList(),
    val userLocation: LatLng? = null,
    val isLoading: Boolean = false,
    val error: String?     = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val locationTracker: LocationTracker,
    private val auth: FirebaseAuth
) : ViewModel() {

    val currentUserId: String get() = auth.currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadRealJobs()
    }

    fun fetchUserLocation() {
        if (_uiState.value.userLocation != null) return // Zaten alındıysa tekrar alma
        
        viewModelScope.launch {
            val location = locationTracker.getCurrentLocation()
            if (location != null) {
                _uiState.update { it.copy(userLocation = LatLng(location.latitude, location.longitude)) }
            }
        }
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
