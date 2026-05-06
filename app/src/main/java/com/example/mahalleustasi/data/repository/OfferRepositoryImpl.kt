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
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Hata oluştu"))
                    return@addSnapshotListener
                }
                val offers = snapshot?.documents?.mapNotNull { it.toObject(Offer::class.java) } ?: emptyList()
                trySend(Resource.Success(offers.sortedByDescending { it.createdAt }))
            }
        awaitClose { listener.remove() }
    }

    override fun getOffersByUserId(userId: String): Flow<Resource<List<Offer>>> = callbackFlow {
        trySend(Resource.Loading)
        val listener = firestore.collection("offers")
            .whereEqualTo("offeredByUserId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Hata oluştu"))
                    return@addSnapshotListener
                }
                val offers = snapshot?.documents?.mapNotNull { it.toObject(Offer::class.java) } ?: emptyList()
                trySend(Resource.Success(offers.sortedByDescending { it.createdAt }))
            }
        awaitClose { listener.remove() }
    }

    override fun getOffersReceivedByUser(userId: String): Flow<Resource<List<Offer>>> = callbackFlow {
        trySend(Resource.Loading)
        // İlan sahibinin tekliflerini getirmek için jobId'leri önce çekmeliyiz.
        // Firestore'da cross-collection sorgu olmadığı için
        // jobs koleksiyonunda postedByUserId == userId olan ilanların jobId'lerini
        // offers koleksiyonunda whereIn ile sorguluyoruz.
        // NOT: whereIn max 30 element — yeterli MVP için.
        val jobsListener = firestore.collection("jobs")
            .whereEqualTo("postedByUserId", userId)
            .addSnapshotListener { jobsSnapshot, jobsError ->
                if (jobsError != null) {
                    trySend(Resource.Error(jobsError.localizedMessage ?: "İlanlar alınamadı"))
                    return@addSnapshotListener
                }

                val jobIds = jobsSnapshot?.documents?.mapNotNull { it.id } ?: emptyList()

                if (jobIds.isEmpty()) {
                    trySend(Resource.Success(emptyList()))
                    return@addSnapshotListener
                }

                // Firestore whereIn max 30 item — MVP için yeterli
                val chunks = jobIds.chunked(30)
                val allOffers = mutableListOf<Offer>()
                var pendingChunks = chunks.size

                chunks.forEach { chunk ->
                    firestore.collection("offers")
                        .whereIn("jobId", chunk)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val offers = snapshot.documents.mapNotNull { it.toObject(Offer::class.java) }
                            allOffers.addAll(offers)
                            pendingChunks--
                            if (pendingChunks == 0) {
                                trySend(Resource.Success(allOffers.sortedByDescending { it.createdAt }))
                            }
                        }
                        .addOnFailureListener {
                            trySend(Resource.Error(it.localizedMessage ?: "Teklifler alınamadı"))
                        }
                }
            }

        awaitClose { jobsListener.remove() }
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

    override suspend fun acceptOfferAndRejectOthers(
        acceptedOfferId: String,
        jobId: String
    ): Resource<Unit> {
        return try {
            // İlandaki tüm pending teklifleri getir
            val offersSnapshot = firestore.collection("offers")
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("status", OfferStatus.PENDING)
                .get()
                .await()

            // Batch yazma — atomik işlem garantisi
            val batch = firestore.batch()

            offersSnapshot.documents.forEach { doc ->
                val newStatus = if (doc.id == acceptedOfferId) OfferStatus.ACCEPTED else OfferStatus.REJECTED
                batch.update(doc.reference, "status", newStatus)
            }

            batch.commit().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Teklif kabul edilemedi.")
        }
    }
}
