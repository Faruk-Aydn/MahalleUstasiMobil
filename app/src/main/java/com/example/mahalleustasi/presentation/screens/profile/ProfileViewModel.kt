package com.example.mahalleustasi.presentation.screens.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.Review
import com.example.mahalleustasi.domain.model.ReviewRole
import com.example.mahalleustasi.domain.model.User
import com.example.mahalleustasi.domain.repository.ReviewRepository
import com.example.mahalleustasi.domain.repository.UserRepository
import com.example.mahalleustasi.domain.usecase.auth.GetCurrentUserUseCase
import com.example.mahalleustasi.domain.usecase.auth.LogoutUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profileUser: User? = null,
    val workerReviews: List<Review> = emptyList(),  // Usta olarak aldığı yorumlar
    val clientReviews: List<Review> = emptyList(),  // Müşteri olarak aldığı yorumlar
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val userRepository: UserRepository,
    private val reviewRepository: ReviewRepository,
    private val auth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // NavGraph'tan gelen userId ("me" ise kendi profilimizi göster)
    private val navUserId: String = savedStateHandle.get<String>("userId") ?: "me"

    val targetUserId: String get() = if (navUserId == "me") auth.currentUser?.uid ?: "" else navUserId
    val isOwnProfile: Boolean get() = navUserId == "me" || navUserId == auth.currentUser?.uid

    // Auth'tan mevcut kullanıcı (logout için)
    val currentUser = getCurrentUserUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadProfile()
        loadReviews()
    }

    private fun loadProfile() {
        val uid = targetUserId
        if (uid.isEmpty()) return

        userRepository.getUserById(uid).onEach { result ->
            when (result) {
                is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                is Resource.Success -> _uiState.update { it.copy(isLoading = false, profileUser = result.data) }
                is Resource.Error   -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> Unit
            }
        }.launchIn(viewModelScope)
    }

    private fun loadReviews() {
        val uid = targetUserId
        if (uid.isEmpty()) return

        // Usta olarak alınan yorumlar
        reviewRepository.getReviewsForUser(uid, ReviewRole.AS_WORKER).onEach { result ->
            if (result is Resource.Success) {
                _uiState.update { it.copy(workerReviews = result.data ?: emptyList()) }
            }
        }.launchIn(viewModelScope)

        // Müşteri olarak alınan yorumlar
        reviewRepository.getReviewsForUser(uid, ReviewRole.AS_CLIENT).onEach { result ->
            if (result is Resource.Success) {
                _uiState.update { it.copy(clientReviews = result.data ?: emptyList()) }
            }
        }.launchIn(viewModelScope)
    }

    fun logout() {
        viewModelScope.launch { logoutUseCase() }
    }
}
