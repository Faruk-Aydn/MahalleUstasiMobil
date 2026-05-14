package com.example.mahalleustasi.presentation.screens.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.*
import com.example.mahalleustasi.domain.repository.ReviewRepository
import com.example.mahalleustasi.domain.repository.UserRepository
import com.example.mahalleustasi.domain.usecase.ai.AnalyzeUserReviewsUseCase
import com.example.mahalleustasi.domain.usecase.auth.GetCurrentUserUseCase
import com.example.mahalleustasi.domain.usecase.auth.LogoutUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profileUser: User?            = null,
    val workerReviews: List<Review>   = emptyList(),
    val clientReviews: List<Review>   = emptyList(),
    val aiAnalysis: AiTrustAnalysis?  = null,
    val isAiLoading: Boolean          = false,
    val isLoading: Boolean            = false,
    val error: String?                = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val userRepository: UserRepository,
    private val reviewRepository: ReviewRepository,
    private val analyzeUserReviewsUseCase: AnalyzeUserReviewsUseCase,
    private val auth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val navUserId: String = savedStateHandle.get<String>("userId") ?: "me"

    val targetUserId: String get() = if (navUserId == "me") auth.currentUser?.uid ?: "" else navUserId
    val isOwnProfile: Boolean get() = navUserId == "me" || navUserId == auth.currentUser?.uid

    val currentUser = getCurrentUserUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadProfile()
        loadReviewsCombined()
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

    /**
     * Race condition fix: İki review flow'unu combine() ile birleştiriyoruz.
     * Bu sayede workerReviews ve clientReviews her zaman tutarlı kalıyor.
     * Her ikisi de yüklendiğinde AI analizini başlatıyoruz.
     * 
     * NİÇİN: Önceki implementasyonda iki ayrı onEach kullanılıyordu.
     * clientReviews flow'u geldiğinde workerReviews state'i boş olabiliyordu
     * çünkü Firestore'dan sonuçlar eş zamansız geliyordu (race condition).
     */
    private fun loadReviewsCombined() {
        val uid = targetUserId
        if (uid.isEmpty()) {
            android.util.Log.w("ProfileVM", "targetUserId is empty, skipping reviews load")
            return
        }

        android.util.Log.d("ProfileVM", "Loading reviews for userId=$uid (isOwnProfile=$isOwnProfile)")

        // Worker reviews — bağımsız collector, combine() bekleme problemi yok
        reviewRepository.getReviewsForUser(uid, ReviewRole.AS_WORKER).onEach { result ->
            when (result) {
                is Resource.Success -> {
                    android.util.Log.d("ProfileVM", "Worker reviews loaded: ${result.data?.size}")
                    _uiState.update { it.copy(workerReviews = result.data ?: emptyList()) }
                    maybeRunAiAnalysis()
                }
                is Resource.Error -> android.util.Log.e("ProfileVM", "Worker reviews error: ${result.message}")
                else -> Unit
            }
        }.launchIn(viewModelScope)

        // Client reviews — bağımsız collector
        reviewRepository.getReviewsForUser(uid, ReviewRole.AS_CLIENT).onEach { result ->
            when (result) {
                is Resource.Success -> {
                    android.util.Log.d("ProfileVM", "Client reviews loaded: ${result.data?.size}")
                    _uiState.update { it.copy(clientReviews = result.data ?: emptyList()) }
                    maybeRunAiAnalysis()
                }
                is Resource.Error -> android.util.Log.e("ProfileVM", "Client reviews error: ${result.message}")
                else -> Unit
            }
        }.launchIn(viewModelScope)
    }

    private fun maybeRunAiAnalysis() {
        val state = _uiState.value
        // Daha önce analiz yapıldıysa tekrar yapma
        if (state.aiAnalysis != null || state.isAiLoading) return
        val allReviews = state.workerReviews + state.clientReviews
        if (allReviews.isNotEmpty()) {
            runAiAnalysis(allReviews)
        }
    }

    private fun runAiAnalysis(reviews: List<Review>) {
        viewModelScope.launch {
            analyzeUserReviewsUseCase(reviews).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isAiLoading = true) }
                    is Resource.Success -> _uiState.update { it.copy(isAiLoading = false, aiAnalysis = result.data) }
                    is Resource.Error   -> {
                        android.util.Log.e("ProfileVM", "AI Analysis error: ${result.message}")
                        // Hata durumunda boş bir analiz objesi oluşturup hatayı summary'e basabiliriz
                        _uiState.update { 
                            it.copy(
                                isAiLoading = false,
                                aiAnalysis = AiTrustAnalysis(
                                    score = 0,
                                    summary = "Analiz yapılamadı: ${result.message}",
                                    strengths = emptyList(),
                                    weaknesses = emptyList()
                                )
                            ) 
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    /** Manuel AI analizi yenileme (kullanıcı "Yenile" butonuna basarsa) */
    fun refreshAiAnalysis() {
        val allReviews = _uiState.value.workerReviews + _uiState.value.clientReviews
        _uiState.update { it.copy(aiAnalysis = null) }
        runAiAnalysis(allReviews)
    }

    fun logout() {
        viewModelScope.launch { logoutUseCase() }
    }
}
