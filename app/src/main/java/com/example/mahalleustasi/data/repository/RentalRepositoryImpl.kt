package com.example.mahalleustasi.data.repository

import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.Rental
import com.example.mahalleustasi.domain.repository.RentalRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class RentalRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : RentalRepository {

    override fun getRentals(): Flow<Resource<List<Rental>>> = callbackFlow {
        trySend(Resource.Loading)
        val listener = firestore.collection("rentals")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Kiralık ilanlar yüklenemedi"))
                    return@addSnapshotListener
                }
                val rentals = snapshot?.documents?.mapNotNull { it.toObject(Rental::class.java) } ?: emptyList()
                trySend(Resource.Success(rentals))
            }
        awaitClose { listener.remove() }
    }

    override fun getRentalsByUserId(userId: String): Flow<Resource<List<Rental>>> = callbackFlow {
        trySend(Resource.Loading)
        val listener = firestore.collection("rentals")
            .whereEqualTo("ownerId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "İlanlar yüklenemedi"))
                    return@addSnapshotListener
                }
                val rentals = snapshot?.documents?.mapNotNull { it.toObject(Rental::class.java) } ?: emptyList()
                trySend(Resource.Success(rentals))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getRentalById(rentalId: String): Resource<Rental> {
        return try {
            val snapshot = firestore.collection("rentals").document(rentalId).get().await()
            val rental = snapshot.toObject(Rental::class.java)
            if (rental != null) Resource.Success(rental)
            else Resource.Error("Kiralık ilan bulunamadı.")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "İlan yüklenirken hata oluştu.")
        }
    }

    override suspend fun createRental(rental: Rental): Resource<Unit> {
        return try {
            val user = auth.currentUser ?: return Resource.Error("Oturum açmanız gerekiyor.")
            val docRef = firestore.collection("rentals").document()
            val rentalWithId = rental.copy(
                id = docRef.id,
                ownerId = user.uid,
                ownerName = user.displayName ?: user.email?.substringBefore("@") ?: "Kullanıcı",
                createdAt = System.currentTimeMillis()
            )
            docRef.set(rentalWithId).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Kiralık ilan oluşturulamadı.")
        }
    }

    override suspend fun updateAvailability(rentalId: String, isAvailable: Boolean): Resource<Unit> {
        return try {
            firestore.collection("rentals").document(rentalId)
                .update("isAvailable", isAvailable).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Durum güncellenemedi.")
        }
    }

    override suspend fun deleteRental(rentalId: String): Resource<Unit> {
        return try {
            firestore.collection("rentals").document(rentalId).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "İlan silinemedi.")
        }
    }
}
