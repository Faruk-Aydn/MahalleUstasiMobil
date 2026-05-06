package com.example.mahalleustasi.domain.model

data class User(
    val id: String         = "",
    val name: String       = "",
    val email: String      = "",
    val photoUrl: String?  = null,
    val rating: Float      = 0f,
    val reviewCount: Int   = 0,
    val completedJobsCount: Int = 0,
    val savedAddresses: List<SavedAddress> = emptyList(),
    val fcmToken: String?  = null,
    val createdAt: Long    = System.currentTimeMillis()
)

data class SavedAddress(
    val id: String = "",
    val title: String = "", // e.g. "Ev", "İş"
    val addressText: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0
)
