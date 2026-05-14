package com.example.mahalleustasi.domain.repository

import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.Rental
import kotlinx.coroutines.flow.Flow

interface RentalRepository {
    fun getRentals(): Flow<Resource<List<Rental>>>
    fun getRentalsByUserId(userId: String): Flow<Resource<List<Rental>>>
    suspend fun getRentalById(rentalId: String): Resource<Rental>
    suspend fun createRental(rental: Rental): Resource<Unit>
    suspend fun updateAvailability(rentalId: String, isAvailable: Boolean): Resource<Unit>
    suspend fun deleteRental(rentalId: String): Resource<Unit>
}
