package com.example.mahalleustasi.presentation.screens.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.mahalleustasi.domain.model.Job
import javax.inject.Inject

data class HomeUiState(
    val jobs: List<Job>    = emptyList(),
    val isLoading: Boolean = false,
    val error: String?     = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    // İlerleyen sprintlerde JobRepository inject edilecek
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    // Sprint 2'de Firestore'dan ilanlar çekilecek.
    // Şimdilik placeholder veri kullanılıyor.
    init {
        loadSampleJobs()
    }

    private fun loadSampleJobs() {
        // TODO: Replace with real Firestore repository call
    }
}
