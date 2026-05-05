package com.example.mahalleustasi.domain.repository

import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserById(userId: String): Flow<Resource<User>>
    suspend fun updateUser(user: User): Resource<Unit>
    suspend fun createUser(user: User): Resource<Unit>
}
