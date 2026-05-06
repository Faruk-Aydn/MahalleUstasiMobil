package com.example.mahalleustasi.presentation.screens.job

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.*
import com.example.mahalleustasi.domain.repository.ChatRepository
import com.example.mahalleustasi.domain.repository.JobRepository
import com.example.mahalleustasi.domain.repository.OfferRepository
import com.example.mahalleustasi.domain.repository.ReviewRepository
import com.example.mahalleustasi.domain.usecase.ai.AnalyzeUserReviewsUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JobDetailUiState(
    val job: Job? = null,
    val offers: List<Offer> = emptyList(),
    val isLoading: Boolean = false,
    val isOfferLoading: Boolean = false,
    val isOwner: Boolean = false,
    val isAcceptedWorker: Boolean = false,
    val offerSuccess: Boolean = false,
    val navigateToChatId: String? = null,
    val navigateToReview: ReviewNavArgs? = null,
    val aiAnalysis: AiTrustAnalysis? = null,
    val isAiLoading: Boolean = false,
    val error: String? = null
)

data class ReviewNavArgs(
    val jobId: String,
    val revieweeId: String,
    val revieweeName: String,
    val role: String
)

@HiltViewModel
class JobDetailViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val offerRepository: OfferRepository,
    private val reviewRepository: ReviewRepository,
    private val chatRepository: ChatRepository,
    private val analyzeUserReviewsUseCase: AnalyzeUserReviewsUseCase,
    private val auth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val jobId: String = savedStateHandle.get<String>("jobId") ?: ""
    val currentUserId: String get() = auth.currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow(JobDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadJobDetail()
        loadOffers()
    }

    private fun loadOffers() {
        if (jobId.isBlank()) return

        offerRepository.getOffersByJobId(jobId).onEach { result ->
            when (result) {
                is Resource.Success -> {
                    val offers = result.data ?: emptyList()
                    val acceptedWorker = offers.any { it.status == OfferStatus.ACCEPTED && it.offeredByUserId == currentUserId }
                    _uiState.update { it.copy(offers = offers, isAcceptedWorker = acceptedWorker) }
                }
                else -> Unit
            }
        }.launchIn(viewModelScope)
    }

    fun loadJobDetail() {
        if (jobId.isBlank()) {
            _uiState.update { it.copy(error = "Geçersiz ilan ID") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = jobRepository.getJobById(jobId)) {
                is Resource.Success -> {
                    val job = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            job = job,
                            isOwner = job?.postedByUserId == currentUserId
                        )
                    }
                    // İlan sahibi başkasıysa onun için AI analizi başlat
                    if (job != null && job.postedByUserId != currentUserId) {
                        loadAiAnalysis(job.postedByUserId)
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> Unit
            }
        }
    }

    fun submitOffer(price: Double, description: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isOfferLoading = true, offerSuccess = false, error = null) }

            val newOffer = Offer(
                jobId = jobId,
                price = price,
                description = description
            )

            when (val result = offerRepository.createOffer(newOffer)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isOfferLoading = false, offerSuccess = true) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isOfferLoading = false, error = result.message) }
                }
                else -> Unit
            }
        }
    }

    /**
     * İlan sahibi teklifi kabul eder.
     * 1. Diğer teklifleri reddet (batch)
     * 2. İlanı IN_PROGRESS yap
     * 3. Chat oluştur
     * 4. Chat'e yönlendir
     */
    fun acceptOffer(offer: Offer) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val batchResult = offerRepository.acceptOfferAndRejectOthers(offer.id, jobId)
            if (batchResult is Resource.Error) {
                _uiState.update { it.copy(isLoading = false, error = batchResult.message) }
                return@launch
            }

            val jobResult = jobRepository.updateJobStatus(jobId, JobStatus.IN_PROGRESS, offer.id)
            if (jobResult is Resource.Error) {
                _uiState.update { it.copy(isLoading = false, error = jobResult.message) }
                return@launch
            }

            val currentUser = auth.currentUser ?: return@launch
            val chatId = "${jobId}_${offer.id}"
            val chat = Chat(
                id = chatId,
                jobId = jobId,
                jobTitle = _uiState.value.job?.title ?: "",
                offerId = offer.id,
                buyerId = currentUser.uid,
                buyerName = currentUser.displayName ?: "İlan Sahibi",
                sellerId = offer.offeredByUserId,
                sellerName = offer.offeredByUserName
            )

            chatRepository.getOrCreateChat(chat)

            _uiState.update { it.copy(isLoading = false, navigateToChatId = chatId) }
        }
    }

    fun rejectOffer(offerId: String) {
        viewModelScope.launch {
            offerRepository.updateOfferStatus(offerId, OfferStatus.REJECTED)
        }
    }

    /**
     * İşi tamamla:
     * 1. İlan durumunu COMPLETED yap
     * 2. Karşılıklı puanlama için ReviewScreen'e yönlendir
     */
    fun completeJob() {
        val job = _uiState.value.job ?: return
        val acceptedOffer = _uiState.value.offers.find { it.status == OfferStatus.ACCEPTED } ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = jobRepository.updateJobStatus(jobId, JobStatus.COMPLETED)
            if (result is Resource.Error) {
                _uiState.update { it.copy(isLoading = false, error = result.message) }
                return@launch
            }

            val isOwner = job.postedByUserId == currentUserId
            
            val navArgs = if (isOwner) {
                // İlan sahibi -> Ustayı değerlendirsin
                ReviewNavArgs(
                    jobId = jobId,
                    revieweeId = acceptedOffer.offeredByUserId,
                    revieweeName = acceptedOffer.offeredByUserName,
                    role = "AS_WORKER"
                )
            } else {
                // Usta -> İş sahibini değerlendirsin
                ReviewNavArgs(
                    jobId = jobId,
                    revieweeId = job.postedByUserId,
                    revieweeName = job.postedByUserName,
                    role = "AS_CLIENT"
                )
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    navigateToReview = navArgs
                )
            }
        }
    }

    /**
     * Usta → İş sahibini değerlendirsin (iş tamamlandıktan sonra)
     */
    fun navigateToReviewAsClient() {
        val job = _uiState.value.job ?: return
        if (job.status != JobStatus.COMPLETED) return

        _uiState.update {
            it.copy(
                navigateToReview = ReviewNavArgs(
                    jobId = jobId,
                    revieweeId = job.postedByUserId,
                    revieweeName = job.postedByUserName,
                    role = "AS_CLIENT"
                )
            )
        }
    }

    fun openChat() {
        val acceptedOffer = _uiState.value.offers.find { it.status == OfferStatus.ACCEPTED } ?: return
        val chatId = "${jobId}_${acceptedOffer.id}"
        _uiState.update { it.copy(navigateToChatId = chatId) }
    }

    /**
     * Teklifi kabul edilmiş olan usta mı bu?
     */
    fun isAcceptedWorker(): Boolean {
        return _uiState.value.isAcceptedWorker
    }

    private fun loadAiAnalysis(userId: String) {
        viewModelScope.launch {
            reviewRepository.getReviewsForUser(userId).collect { result ->
                if (result is Resource.Success) {
                    analyzeUserReviewsUseCase(result.data ?: emptyList()).collect { aiResult ->
                        when (aiResult) {
                            is Resource.Loading -> _uiState.update { it.copy(isAiLoading = true) }
                            is Resource.Success -> _uiState.update { it.copy(isAiLoading = false, aiAnalysis = aiResult.data) }
                            is Resource.Error -> _uiState.update { it.copy(isAiLoading = false) }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    fun resetOfferSuccess() {
        _uiState.update { it.copy(offerSuccess = false) }
    }

    fun clearNavigation() {
        _uiState.update { it.copy(navigateToChatId = null, navigateToReview = null) }
    }
}
