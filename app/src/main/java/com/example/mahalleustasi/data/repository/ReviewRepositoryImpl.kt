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

    /**
     * FIX: Composite index gerektiren sorgu yerine, revieweeId ile tüm yorumları çekip
     * client-side filtreliyoruz. Bu:
     * 1. Firestore composite index sorununu ortadan kaldırır.
     * 2. `role` enum'unun string olarak kaydedilip edilmediği sorununu çözer
     *    (client-side'da enum.name ile karşılaştırıyoruz).
     * 3. Firestore'da yorum varsa kesinlikle gösterir.
     */
    override fun getReviewsForUser(userId: String, role: ReviewRole?): Flow<Resource<List<Review>>> = callbackFlow {
        trySend(Resource.Loading)

        // Sadece TEK alan filtresi — Firestore otomatik index oluşturur, composite index GEREKMEZ
        // orderBy kaldırıldı: client-side sort edilecek
        android.util.Log.d("ReviewRepo", "getReviewsForUser: userId=$userId, role=$role")
        val listener = reviewsCollection
            .whereEqualTo("revieweeId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("ReviewRepo", "Firestore error: ${error.message}", error)
                    trySend(Resource.Error(error.localizedMessage ?: "Yorumlar alınamadı: ${error.message}"))
                    return@addSnapshotListener
                }
                android.util.Log.d("ReviewRepo", "Snapshot received: ${snapshot?.documents?.size} docs, isEmpty=${snapshot?.isEmpty}")
                val allReviews = snapshot?.documents?.mapNotNull { doc ->
                    android.util.Log.d("ReviewRepo", "Doc: ${doc.id} => ${doc.data}")
                    try {
                        // Önce standart deserialize dene
                        doc.toObject(Review::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        android.util.Log.w("ReviewRepo", "toObject failed for ${doc.id}, using manual parse: ${e.message}")
                        // Enum serialization sorunu varsa manuel parse et
                        val data = doc.data ?: return@mapNotNull null
                        Review(
                            id           = doc.id,
                            jobId        = data["jobId"] as? String ?: "",
                            reviewerId   = data["reviewerId"] as? String ?: "",
                            reviewerName = data["reviewerName"] as? String ?: "",
                            revieweeId   = data["revieweeId"] as? String ?: "",
                            rating       = (data["rating"] as? Double)?.toFloat()
                                            ?: (data["rating"] as? Long)?.toFloat() ?: 0f,
                            comment      = data["comment"] as? String ?: "",
                            role         = try {
                                ReviewRole.valueOf(data["role"] as? String ?: "AS_WORKER")
                            } catch (_: Exception) { ReviewRole.AS_WORKER },
                            createdAt    = data["createdAt"] as? Long ?: 0L
                        )
                    }
                } ?: emptyList()

                // Role filtresi + createdAt sıralaması tamamen client-side
                val filtered = (if (role != null) allReviews.filter { it.role == role } else allReviews)
                    .sortedByDescending { it.createdAt }

                android.util.Log.d("ReviewRepo", "Final filtered reviews: ${filtered.size} (role filter: $role)")
                trySend(Resource.Success(filtered))
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
            // enum'u String olarak kaydet — Firestore'da okunabilir ve sorgulabilir
            val reviewMap = mapOf(
                "id"           to docRef.id,
                "jobId"        to review.jobId,
                "reviewerId"   to review.reviewerId,
                "reviewerName" to review.reviewerName,
                "revieweeId"   to review.revieweeId,
                "rating"       to review.rating,
                "comment"      to review.comment,
                "role"         to review.role.name,   // ← "AS_WORKER" veya "AS_CLIENT" olarak kaydet
                "createdAt"    to review.createdAt
            )
            docRef.set(reviewMap).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Yorum gönderilemedi.")
        }
    }

    override suspend fun updateUserRating(userId: String): Resource<Unit> {
        return try {
            val snapshot = reviewsCollection
                .whereEqualTo("revieweeId", userId)
                .get()
                .await()

            val reviews = snapshot.documents.mapNotNull { doc ->
                (doc.data?.get("rating") as? Double)?.toFloat()
                    ?: (doc.data?.get("rating") as? Long)?.toFloat()
            }

            if (reviews.isNotEmpty()) {
                val avgRating   = reviews.average().toFloat()
                val reviewCount = reviews.size

                firestore.collection("users").document(userId)
                    .update(
                        mapOf(
                            "rating"      to avgRating,
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
