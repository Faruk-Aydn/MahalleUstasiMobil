package com.example.mahalleustasi.data.repository

import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.Offer
import com.example.mahalleustasi.domain.model.OfferStatus
import com.example.mahalleustasi.domain.repository.OfferRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class OfferRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : OfferRepository {

    override suspend fun createOffer(offer: Offer): Resource<Unit> {
        return try {
            val user = auth.currentUser ?: return Resource.Error("Oturum açmanız gerekiyor.")
            
            val documentRef = firestore.collection("offers").document()
            val offerWithId = offer.copy(
                id = documentRef.id,
                offeredByUserId = user.uid,
                offeredByUserName = user.displayName ?: user.email?.substringBefore("@") ?: "Usta",
                createdAt = System.currentTimeMillis()
            )
            
            documentRef.set(offerWithId).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Teklif gönderilemedi.")
        }
    }

    override fun getOffersByJobId(jobId: String): Flow<Resource<List<Offer>>> = callbackFlow {
        trySend(Resource.Loading)
        val listener = firestore.collection("offers")
            .whereEqualTo("jobId", jobId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Hata oluştu"))
                    return@addSnapshotListener
                }
                val offers = snapshot?.documents?.mapNotNull { it.toObject(Offer::class.java) } ?: emptyList()
                trySend(Resource.Success(offers))
            }
        awaitClose { listener.remove() }
    }

    override fun getOffersByUserId(userId: String): Flow<Resource<List<Offer>>> = callbackFlow {
        trySend(Resource.Loading)
        val listener = firestore.collection("offers")
            .whereEqualTo("offeredByUserId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Hata oluştu"))
                    return@addSnapshotListener
                }
                val offers = snapshot?.documents?.mapNotNull { it.toObject(Offer::class.java) } ?: emptyList()
                trySend(Resource.Success(offers))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun updateOfferStatus(offerId: String, status: OfferStatus): Resource<Unit> {
        return try {
            firestore.collection("offers").document(offerId)
                .update("status", status).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Durum güncellenemedi.")
        }
    }
}
