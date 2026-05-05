package com.example.mahalleustasi.data.repository

import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.Job
import com.example.mahalleustasi.domain.model.JobStatus
import com.example.mahalleustasi.domain.repository.JobRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class JobRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : JobRepository {

    override fun getJobs(): Flow<Resource<List<Job>>> = callbackFlow {
        trySend(Resource.Loading)

        val listener = firestore.collection("jobs")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Bilinmeyen bir hata oluştu"))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val jobs = snapshot.documents.mapNotNull { it.toObject(Job::class.java) }
                    trySend(Resource.Success(jobs))
                } else {
                    trySend(Resource.Success(emptyList()))
                }
            }

        awaitClose { listener.remove() }
    }

    override fun getJobsByUserId(userId: String): Flow<Resource<List<Job>>> = callbackFlow {
        trySend(Resource.Loading)

        val listener = firestore.collection("jobs")
            .whereEqualTo("postedByUserId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "İlanlar alınamadı"))
                    return@addSnapshotListener
                }
                val jobs = snapshot?.documents?.mapNotNull { it.toObject(Job::class.java) } ?: emptyList()
                trySend(Resource.Success(jobs))
            }

        awaitClose { listener.remove() }
    }

    override suspend fun createJob(job: Job): Resource<Unit> {
        return try {
            val user = auth.currentUser ?: return Resource.Error("Oturum açmanız gerekiyor.")

            val documentRef = firestore.collection("jobs").document()
            val jobWithId = job.copy(
                id = documentRef.id,
                postedByUserId = user.uid,
                postedByUserName = user.displayName ?: user.email?.substringBefore("@") ?: "Kullanıcı",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            documentRef.set(jobWithId).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "İlan oluşturulamadı.")
        }
    }

    override suspend fun getJobById(jobId: String): Resource<Job> {
        return try {
            val snapshot = firestore.collection("jobs").document(jobId).get().await()
            val job = snapshot.toObject(Job::class.java)
            if (job != null) Resource.Success(job)
            else Resource.Error("İlan bulunamadı.")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "İlan getirilirken hata oluştu.")
        }
    }

    override suspend fun updateJobStatus(
        jobId: String,
        status: JobStatus,
        acceptedOfferId: String?
    ): Resource<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to status,
                "updatedAt" to System.currentTimeMillis()
            )
            acceptedOfferId?.let { updates["acceptedOfferId"] = it }

            firestore.collection("jobs").document(jobId)
                .update(updates)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "İlan durumu güncellenemedi.")
        }
    }
}
