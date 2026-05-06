package com.example.mahalleustasi.domain.usecase.auth

import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.User
import com.example.mahalleustasi.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Resource<User> {
        if (name.isBlank()) return Resource.Error("Ad Soyad boş olamaz.")
        if (email.isBlank()) return Resource.Error("E-posta boş olamaz.")
        if (password.length < 6) return Resource.Error("Şifre en az 6 karakter olmalıdır.")
        if (password != confirmPassword) return Resource.Error("Şifreler eşleşmiyor.")

        return authRepository.register(email = email, password = password, name = name)
    }
}
