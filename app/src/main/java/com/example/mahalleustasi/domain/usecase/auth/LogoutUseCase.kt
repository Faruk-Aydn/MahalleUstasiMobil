package com.example.mahalleustasi.domain.usecase.auth

import com.example.mahalleustasi.domain.repository.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke() = repository.logout()
}
