package com.example.mahalleustasi.domain.usecase.auth

import com.example.mahalleustasi.domain.model.User
import com.example.mahalleustasi.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    /** Flow ile oturum durumunu dinler. null → giriş yok. */
    operator fun invoke(): Flow<User?> = repository.currentUser

    fun isLoggedIn(): Boolean = repository.isLoggedIn()
}
