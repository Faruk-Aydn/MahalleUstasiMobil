package com.example.mahalleustasi.presentation.screens.offers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.Chat
import com.example.mahalleustasi.domain.model.Job
import com.example.mahalleustasi.domain.model.JobStatus
import com.example.mahalleustasi.domain.model.Offer
import com.example.mahalleustasi.domain.model.OfferStatus
import com.example.mahalleustasi.domain.repository.ChatRepository
import com.example.mahalleustasi.domain.repository.JobRepository
import com.example.mahalleustasi.domain.repository.OfferRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OffersUiState(
    val sentOffers: List<Offer>       = emptyList(),
    val receivedOffers: List<Offer>   = emptyList(),
    /** jobId -> job title mapping for sent offers */
    val jobTitles: Map<String, String> = emptyMap(),
    val isLoading: Boolean             = false,
    val error: String?                 = null,
    val navigateToChatId: String?      = null,
    val actionSuccess: String?         = null
)

@HiltViewModel
class OffersViewModel @Inject constructor(
    private val offerRepository: OfferRepository,
    private val jobRepository: JobRepository,
    private val chatRepository: ChatRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(OffersUiState())
    val uiState = _uiState.asStateFlow()

    val currentUserId: String get() = auth.currentUser?.uid ?: ""

    init {
        loadSentOffers()
        loadReceivedOffers()
    }

    fun loadSentOffers() {
        val userId = currentUserId
        if (userId.isEmpty()) return

        offerRepository.getOffersByUserId(userId).onEach { result ->
            when (result) {
                is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                is Resource.Success -> {
                    val offers = result.data ?: emptyList()
                    _uiState.update { it.copy(isLoading = false, sentOffers = offers) }
                    // İlan başlıklarını fetch et (sadece yeni ID'ler için)
                    fetchJobTitlesFor(offers.map { it.jobId }.distinct())
                }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> Unit
            }
        }.launchIn(viewModelScope)
    }

    private fun fetchJobTitlesFor(jobIds: List<String>) {
        viewModelScope.launch {
            val existing = _uiState.value.jobTitles.toMutableMap()
            val missing = jobIds.filter { it !in existing }
            missing.forEach { jobId ->
                val result = jobRepository.getJobById(jobId)
                if (result is Resource.Success) {
                    existing[jobId] = result.data?.title ?: "İlan #${jobId.take(6)}"
                }
            }
            _uiState.update { it.copy(jobTitles = existing) }
        }
    }

    fun loadReceivedOffers() {
        val userId = currentUserId
        if (userId.isEmpty()) return

        offerRepository.getOffersReceivedByUser(userId).onEach { result ->
            when (result) {
                is Resource.Success -> _uiState.update {
                    it.copy(receivedOffers = result.data ?: emptyList())
                }
                else -> Unit
            }
        }.launchIn(viewModelScope)
    }

    fun acceptOffer(offer: Offer) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val batchResult = offerRepository.acceptOfferAndRejectOthers(offer.id, offer.jobId)
            if (batchResult is Resource.Error) {
                _uiState.update { it.copy(isLoading = false, error = batchResult.message) }
                return@launch
            }

            val jobResult = jobRepository.updateJobStatus(offer.jobId, JobStatus.IN_PROGRESS, offer.id)
            if (jobResult is Resource.Error) {
                _uiState.update { it.copy(isLoading = false, error = jobResult.message) }
                return@launch
            }

            val currentUser = auth.currentUser ?: return@launch
            val chatId = "${offer.jobId}_${offer.id}"
            val chat = Chat(
                id         = chatId,
                jobId      = offer.jobId,
                jobTitle   = _uiState.value.jobTitles[offer.jobId] ?: "",
                offerId    = offer.id,
                buyerId    = currentUser.uid,
                buyerName  = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "İlan Sahibi",
                sellerId   = offer.offeredByUserId,
                sellerName = offer.offeredByUserName
            )

            val chatResult = chatRepository.getOrCreateChat(chat)
            if (chatResult is Resource.Error) {
                _uiState.update { it.copy(isLoading = false, error = chatResult.message) }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    navigateToChatId = chatId,
                    actionSuccess = "Teklif kabul edildi! Sohbet başlatılıyor..."
                )
            }
        }
    }

    fun rejectOffer(offerId: String) {
        viewModelScope.launch {
            val result = offerRepository.updateOfferStatus(offerId, OfferStatus.REJECTED)
            if (result is Resource.Error) {
                _uiState.update { it.copy(error = result.message) }
            } else {
                _uiState.update { it.copy(actionSuccess = "Teklif reddedildi.") }
            }
        }
    }

    fun clearNavigation() {
        _uiState.update { it.copy(navigateToChatId = null, actionSuccess = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
