package com.example.mahalleustasi.data.repository

import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.SavedAddress
import com.example.mahalleustasi.domain.model.User
import com.example.mahalleustasi.domain.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    private val usersCollection = firestore.collection("users")

    override fun getUserById(userId: String): Flow<Resource<User>> = callbackFlow {
        trySend(Resource.Loading)

        val listener = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Kullanıcı alınamadı"))
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(User::class.java)
                    if (user != null) {
                        trySend(Resource.Success(user))
                    } else {
                        trySend(Resource.Error("Kullanıcı verisi okunamadı"))
                    }
                } else {
                    trySend(Resource.Error("Kullanıcı bulunamadı"))
                }
            }

        awaitClose { listener.remove() }
    }

    override suspend fun updateUser(user: User): Resource<Unit> {
        return try {
            usersCollection.document(user.id).set(user).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Kullanıcı güncellenemedi")
        }
    }

    override suspend fun createUser(user: User): Resource<Unit> {
        return try {
            usersCollection.document(user.id).set(user).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Kullanıcı oluşturulamadı")
        }
    }

    override suspend fun addSavedAddress(userId: String, address: SavedAddress): Resource<Unit> {
        return try {
            usersCollection.document(userId)
                .update("savedAddresses", com.google.firebase.firestore.FieldValue.arrayUnion(address))
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Adres kaydedilemedi")
        }
    }

    override suspend fun updateFcmToken(userId: String, token: String): Resource<Unit> {
        return try {
            usersCollection.document(userId)
                .update("fcmToken", token)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "FCM Token güncellenemedi")
        }
    }

    override suspend fun incrementCompletedJobsCount(userId: String): Resource<Unit> {
        return try {
            usersCollection.document(userId)
                .update("completedJobsCount", com.google.firebase.firestore.FieldValue.increment(1))
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Tamamlanan iş sayısı güncellenemedi")
        }
    }
}
