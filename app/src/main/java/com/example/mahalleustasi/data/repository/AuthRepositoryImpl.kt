package com.example.mahalleustasi.data.repository

import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.User
import com.example.mahalleustasi.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    /**
     * Firebase Auth durum değişikliklerini Flow'a dönüştürür.
     * callbackFlow → callback-tabanlı API'yi coroutine Flow'a çevirmenin
     * idiomatik yoludur.
     */
    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            trySend(
                firebaseUser?.let {
                    User(
                        id       = it.uid,
                        name     = it.displayName ?: "",
                        email    = it.email ?: "",
                        photoUrl = it.photoUrl?.toString()
                    )
                }
            )
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun login(email: String, password: String): Resource<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user!!
            Resource.Success(
                User(
                    id       = user.uid,
                    name     = user.displayName ?: "",
                    email    = user.email ?: "",
                    photoUrl = user.photoUrl?.toString()
                )
            )
        } catch (e: Exception) {
            Resource.Error(mapFirebaseError(e))
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        name: String
    ): Resource<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user!!

            // Display name güncelle
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            // Firestore'a kullanıcı kaydı oluştur
            val userMap = mapOf(
                "id"        to firebaseUser.uid,
                "name"      to name,
                "email"     to email,
                "rating"    to 0f,
                "reviewCount" to 0,
                "completedJobsCount" to 0,
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(userMap)
                .await()

            Resource.Success(User(id = firebaseUser.uid, name = name, email = email))
        } catch (e: Exception) {
            Resource.Error(mapFirebaseError(e))
        }
    }

    override suspend fun logout() {
        auth.signOut()
    }

    override fun isLoggedIn(): Boolean = auth.currentUser != null

    /** Firebase hata mesajlarını Türkçeye çevirir. */
    private fun mapFirebaseError(e: Exception): String {
        val msg = e.message ?: return "Bilinmeyen hata"
        return when {
            msg.contains("email address is already in use")  -> "Bu e-posta zaten kullanılıyor"
            msg.contains("password is invalid")              -> "Şifre hatalı"
            msg.contains("no user record")                   -> "Bu e-posta ile kayıtlı kullanıcı bulunamadı"
            msg.contains("network error")                    -> "İnternet bağlantısı yok"
            msg.contains("badly formatted")                  -> "Geçersiz e-posta formatı"
            else                                             -> msg
        }
    }
}
