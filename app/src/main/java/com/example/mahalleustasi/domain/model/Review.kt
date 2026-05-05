package com.example.mahalleustasi.domain.model

data class Review(
    val id: String = "",
    val jobId: String = "",
    val reviewerId: String = "",    // Yorumu yazan
    val reviewerName: String = "",
    val revieweeId: String = "",    // Yorumu alan
    val rating: Float = 0f,         // 1.0 - 5.0
    val comment: String = "",
    val role: ReviewRole = ReviewRole.AS_WORKER,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * AS_WORKER: Hizmet veren (usta) rolünde aldığı yorum
 * AS_CLIENT: Hizmet arayan (müşteri) rolünde aldığı yorum
 */
enum class ReviewRole {
    AS_WORKER,
    AS_CLIENT
}
