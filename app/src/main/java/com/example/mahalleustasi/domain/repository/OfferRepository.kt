package com.example.mahalleustasi.domain.repository

import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.Offer
import kotlinx.coroutines.flow.Flow

interface OfferRepository {
    suspend fun createOffer(offer: Offer): Resource<Unit>
    fun getOffersByJobId(jobId: String): Flow<Resource<List<Offer>>>
    fun getOffersByUserId(userId: String): Flow<Resource<List<Offer>>>
    suspend fun updateOfferStatus(offerId: String, status: com.example.mahalleustasi.domain.model.OfferStatus): Resource<Unit>
}
