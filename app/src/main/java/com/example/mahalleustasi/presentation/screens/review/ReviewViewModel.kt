package com.example.mahalleustasi.presentation.screens.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.Review
import com.example.mahalleustasi.domain.model.ReviewRole
import com.example.mahalleustasi.domain.repository.ReviewRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewUiState(
    val isLoading: Boolean = false,
    val isSubmitted: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository,
    private val auth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Parametreler NavGraph'tan gelir
    val jobId: String = savedStateHandle.get<String>("jobId") ?: ""
    val revieweeId: String = savedStateHandle.get<String>("revieweeId") ?: ""
    val revieweeName: String = savedStateHandle.get<String>("revieweeName") ?: ""
    val roleStr: String = savedStateHandle.get<String>("role") ?: ReviewRole.AS_WORKER.name

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState = _uiState.asStateFlow()

    fun submitReview(rating: Float, comment: String) {
        val currentUser = auth.currentUser ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val review = Review(
                jobId = jobId,
                reviewerId = currentUser.uid,
                reviewerName = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "Kullanıcı",
                revieweeId = revieweeId,
                rating = rating,
                comment = comment,
                role = try { ReviewRole.valueOf(roleStr) } catch (e: Exception) { ReviewRole.AS_WORKER },
                createdAt = System.currentTimeMillis()
            )

            when (val result = reviewRepository.createReview(review)) {
                is Resource.Success -> {
                    // Puanı da güncelle
                    reviewRepository.updateUserRating(revieweeId)
                    _uiState.update { it.copy(isLoading = false, isSubmitted = true) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> Unit
            }
        }
    }
}
