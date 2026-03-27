package com.example.mahalleustasi.domain.usecase.auth

import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.User
import com.example.mahalleustasi.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Resource<User> {
        if (email.isBlank())    return Resource.Error("E-posta boş olamaz")
        if (password.isBlank()) return Resource.Error("Şifre boş olamaz")
        return repository.login(email.trim(), password)
    }
}
