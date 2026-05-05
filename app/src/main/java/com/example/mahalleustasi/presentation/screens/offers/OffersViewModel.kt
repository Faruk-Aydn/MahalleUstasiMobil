package com.example.mahalleustasi.presentation.screens.offers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.Job
import com.example.mahalleustasi.domain.model.JobStatus
import com.example.mahalleustasi.domain.model.Offer
import com.example.mahalleustasi.domain.model.OfferStatus
import com.example.mahalleustasi.domain.repository.ChatRepository
import com.example.mahalleustasi.domain.repository.JobRepository
import com.example.mahalleustasi.domain.repository.OfferRepository
import com.example.mahalleustasi.domain.model.Chat
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OffersUiState(
    // Verdiğim teklifler
    val sentOffers: List<Offer> = emptyList(),
    // Aldığım teklifler (ilanlarıma gelen)
    val receivedOffers: List<Offer> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    // Teklif kabul başarısında açılacak chat ID
    val navigateToChatId: String? = null,
    val actionSuccess: String? = null
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
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, sentOffers = result.data ?: emptyList())
                }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> Unit
            }
        }.launchIn(viewModelScope)
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

    /**
     * Teklifi kabul et:
     * 1. Aynı ilandaki diğer teklifleri REJECTED yap (batch)
     * 2. İlanın status'unu IN_PROGRESS yap
     * 3. Chat dökümanı oluştur
     * 4. ChatScreen'e yönlendir
     */
    fun acceptOffer(offer: Offer) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // 1. Teklifi kabul et, diğerlerini reddet
            val batchResult = offerRepository.acceptOfferAndRejectOthers(
                acceptedOfferId = offer.id,
                jobId = offer.jobId
            )
            if (batchResult is Resource.Error) {
                _uiState.update { it.copy(isLoading = false, error = batchResult.message) }
                return@launch
            }

            // 2. İlanı IN_PROGRESS yap
            val jobResult = jobRepository.updateJobStatus(
                jobId = offer.jobId,
                status = JobStatus.IN_PROGRESS,
                acceptedOfferId = offer.id
            )
            if (jobResult is Resource.Error) {
                _uiState.update { it.copy(isLoading = false, error = jobResult.message) }
                return@launch
            }

            // 3. Chat dökümanı oluştur (chatId = jobId_offerId)
            val currentUser = auth.currentUser ?: return@launch
            val chatId = "${offer.jobId}_${offer.id}"

            val chat = Chat(
                id = chatId,
                jobId = offer.jobId,
                offerId = offer.id,
                buyerId = currentUser.uid,
                buyerName = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "İlan Sahibi",
                sellerId = offer.offeredByUserId,
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
