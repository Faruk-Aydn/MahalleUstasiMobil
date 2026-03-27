package com.example.mahalleustasi.domain.repository

import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Firebase Auth oturum değişikliklerini dinler. */
    val currentUser: Flow<User?>

    suspend fun login(email: String, password: String): Resource<User>

    suspend fun register(email: String, password: String, name: String): Resource<User>

    suspend fun logout()

    fun isLoggedIn(): Boolean
}
