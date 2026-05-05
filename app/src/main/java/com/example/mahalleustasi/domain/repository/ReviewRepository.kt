package com.example.mahalleustasi.domain.repository

import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.Review
import com.example.mahalleustasi.domain.model.ReviewRole
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    /** Belirli bir kullanıcının aldığı yorumlar */
    fun getReviewsForUser(userId: String, role: ReviewRole? = null): Flow<Resource<List<Review>>>

    /** Belirli bir iş için yapılan yorumlar */
    fun getReviewsForJob(jobId: String): Flow<Resource<List<Review>>>

    /** Yorum oluştur */
    suspend fun createReview(review: Review): Resource<Unit>

    /** Kullanıcının ortalama puanını güncelle */
    suspend fun updateUserRating(userId: String): Resource<Unit>
}
