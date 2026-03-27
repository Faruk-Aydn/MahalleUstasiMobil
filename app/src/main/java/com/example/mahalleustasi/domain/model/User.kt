package com.example.mahalleustasi.domain.model

data class User(
    val id: String         = "",
    val name: String       = "",
    val email: String      = "",
    val photoUrl: String?  = null,
    val rating: Float      = 0f,
    val reviewCount: Int   = 0,
    val completedJobsCount: Int = 0,
    val createdAt: Long    = System.currentTimeMillis()
)
