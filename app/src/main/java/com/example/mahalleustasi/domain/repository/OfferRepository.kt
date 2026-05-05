package com.example.mahalleustasi.domain.repository

import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.Offer
import com.example.mahalleustasi.domain.model.OfferStatus
import kotlinx.coroutines.flow.Flow

interface OfferRepository {
    suspend fun createOffer(offer: Offer): Resource<Unit>
    fun getOffersByJobId(jobId: String): Flow<Resource<List<Offer>>>
    fun getOffersByUserId(userId: String): Flow<Resource<List<Offer>>>
    suspend fun updateOfferStatus(offerId: String, status: OfferStatus): Resource<Unit>

    /**
     * Bir teklifi kabul eder ve aynı ilandaki diğer tüm teklifleri reddeder.
     * Atomik görünüm için Firestore batch write kullanılır.
     */
    suspend fun acceptOfferAndRejectOthers(
        acceptedOfferId: String,
        jobId: String
    ): Resource<Unit>

    /** İlan sahibinin ilanlarına gelen teklifleri getirir */
    fun getOffersReceivedByUser(userId: String): Flow<Resource<List<Offer>>>
}

