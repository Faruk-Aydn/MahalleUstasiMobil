package com.example.mahalleustasi.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.Job
import com.example.mahalleustasi.domain.model.JobCategory
import com.example.mahalleustasi.domain.repository.JobRepository
import com.example.mahalleustasi.domain.repository.UserRepository
import com.example.mahalleustasi.domain.location.LocationTracker
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOrder(val label: String) {
    NEWEST("En Yeni"),
    LOWEST_PRICE("En Düşük Fiyat"),
    MOST_OFFERS("En Fazla Teklif")
}

data class HomeFilterState(
    val selectedCategories: Set<JobCategory> = emptySet(),
    val maxBudget: Int = 0,           // 0 = sınırsız
    val sortOrder: SortOrder = SortOrder.NEWEST,
    val searchQuery: String = ""
)

data class HomeUiState(
    val allJobs: List<Job>        = emptyList(),
    val filteredJobs: List<Job>   = emptyList(),
    val filterState: HomeFilterState = HomeFilterState(),
    val currentUserName: String   = "",
    val userLocation: LatLng?     = null,
    val isLoading: Boolean        = false,
    val error: String?            = null,
    val showFilterSheet: Boolean  = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val userRepository: UserRepository,
    private val locationTracker: LocationTracker,
    private val auth: FirebaseAuth
) : ViewModel() {

    val currentUserId: String get() = auth.currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadRealJobs()
        updateFcmToken()
        loadCurrentUserName()
    }

    private fun loadCurrentUserName() {
        val displayName = auth.currentUser?.displayName
        val email       = auth.currentUser?.email?.substringBefore("@")
        _uiState.update { it.copy(currentUserName = displayName ?: email ?: "") }
    }

    private fun updateFcmToken() {
        val userId = auth.currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener
            viewModelScope.launch { userRepository.updateFcmToken(userId, task.result) }
        }
    }

    fun fetchUserLocation() {
        if (_uiState.value.userLocation != null) return
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
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true, error = null) }
                    is Resource.Success -> {
                        val jobs = result.data
                        _uiState.update { it.copy(isLoading = false, allJobs = jobs) }
                        applyFilters()
                    }
                    is Resource.Error   -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                    is Resource.Idle    -> Unit
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(filterState = it.filterState.copy(searchQuery = query)) }
        applyFilters()
    }

    fun toggleCategory(category: JobCategory) {
        val current = _uiState.value.filterState.selectedCategories.toMutableSet()
        if (category in current) current.remove(category) else current.add(category)
        _uiState.update { it.copy(filterState = it.filterState.copy(selectedCategories = current)) }
        applyFilters()
    }

    fun setMaxBudget(budget: Int) {
        _uiState.update { it.copy(filterState = it.filterState.copy(maxBudget = budget)) }
        applyFilters()
    }

    fun setSortOrder(order: SortOrder) {
        _uiState.update { it.copy(filterState = it.filterState.copy(sortOrder = order)) }
        applyFilters()
    }

    fun clearFilters() {
        _uiState.update { it.copy(filterState = HomeFilterState()) }
        applyFilters()
    }

    fun toggleFilterSheet() {
        _uiState.update { it.copy(showFilterSheet = !it.showFilterSheet) }
    }

    private fun applyFilters() {
        val state = _uiState.value
        var jobs = state.allJobs

        // Arama filtresi
        if (state.filterState.searchQuery.isNotBlank()) {
            val q = state.filterState.searchQuery.lowercase()
            jobs = jobs.filter { it.title.lowercase().contains(q) || it.description.lowercase().contains(q) }
        }

        // Kategori filtresi
        if (state.filterState.selectedCategories.isNotEmpty()) {
            jobs = jobs.filter { it.category in state.filterState.selectedCategories }
        }

        // Bütçe filtresi
        if (state.filterState.maxBudget > 0) {
            jobs = jobs.filter {
                val budget = it.budget?.replace("[^0-9]".toRegex(), "")?.toIntOrNull() ?: Int.MAX_VALUE
                budget <= state.filterState.maxBudget
            }
        }

        // Sıralama
        jobs = when (state.filterState.sortOrder) {
            SortOrder.NEWEST      -> jobs.sortedByDescending { it.createdAt }
            SortOrder.LOWEST_PRICE -> jobs.sortedBy { it.budget?.replace("[^0-9]".toRegex(), "")?.toIntOrNull() ?: Int.MAX_VALUE }
            SortOrder.MOST_OFFERS  -> jobs.sortedByDescending { it.offerCount }
        }

        _uiState.update { it.copy(filteredJobs = jobs) }
    }
}
