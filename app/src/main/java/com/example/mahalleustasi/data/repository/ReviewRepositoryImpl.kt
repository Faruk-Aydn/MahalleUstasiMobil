package com.example.mahalleustasi.data.repository

import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.Review
import com.example.mahalleustasi.domain.model.ReviewRole
import com.example.mahalleustasi.domain.repository.ReviewRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ReviewRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ReviewRepository {

    private val reviewsCollection = firestore.collection("reviews")

    override fun getReviewsForUser(userId: String, role: ReviewRole?): Flow<Resource<List<Review>>> = callbackFlow {
        trySend(Resource.Loading)

        var query = reviewsCollection
            .whereEqualTo("revieweeId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        if (role != null) {
            query = query.whereEqualTo("role", role)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.Error(error.localizedMessage ?: "Yorumlar alınamadı"))
                return@addSnapshotListener
            }
            val reviews = snapshot?.documents?.mapNotNull { it.toObject(Review::class.java) } ?: emptyList()
            trySend(Resource.Success(reviews))
        }

        awaitClose { listener.remove() }
    }

    override fun getReviewsForJob(jobId: String): Flow<Resource<List<Review>>> = callbackFlow {
        trySend(Resource.Loading)

        val listener = reviewsCollection
            .whereEqualTo("jobId", jobId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Yorumlar alınamadı"))
                    return@addSnapshotListener
                }
                val reviews = snapshot?.documents?.mapNotNull { it.toObject(Review::class.java) } ?: emptyList()
                trySend(Resource.Success(reviews))
            }

        awaitClose { listener.remove() }
    }

    override suspend fun createReview(review: Review): Resource<Unit> {
        return try {
            val docRef = reviewsCollection.document()
            docRef.set(review.copy(id = docRef.id)).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Yorum gönderilemedi.")
        }
    }

    override suspend fun updateUserRating(userId: String): Resource<Unit> {
        return try {
            // Kullanıcının tüm yorumlarını çek ve ortalamayı hesapla
            val snapshot = reviewsCollection
                .whereEqualTo("revieweeId", userId)
                .get()
                .await()

            val reviews = snapshot.documents.mapNotNull { it.toObject(Review::class.java) }

            if (reviews.isNotEmpty()) {
                val avgRating = reviews.map { it.rating }.average().toFloat()
                val reviewCount = reviews.size

                firestore.collection("users").document(userId)
                    .update(
                        mapOf(
                            "rating" to avgRating,
                            "reviewCount" to reviewCount
                        )
                    ).await()
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Puan güncellenemedi.")
        }
    }
}
