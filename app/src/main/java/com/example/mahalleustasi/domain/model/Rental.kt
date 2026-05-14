package com.example.mahalleustasi.domain.model

import com.example.mahalleustasi.domain.model.JobLocation

data class Rental(
    val id: String             = "",
    val title: String          = "",
    val description: String    = "",
    val dailyPrice: Double     = 0.0,
    val depositAmount: Double  = 0.0,
    val category: RentalCategory = RentalCategory.OTHER,
    val photoUrls: List<String>  = emptyList(),
    val ownerId: String        = "",
    val ownerName: String      = "",
    val location: JobLocation  = JobLocation(),
    val isAvailable: Boolean   = true,
    val viewCount: Int         = 0,
    val createdAt: Long        = System.currentTimeMillis()
)

enum class RentalCategory(val displayName: String, val emoji: String) {
    TOOLS("Alet & Ekipman", "🔨"),
    ELECTRONICS("Elektronik", "📱"),
    VEHICLES("Araç", "🚗"),
    FURNITURE("Mobilya", "🪑"),
    SPORTS("Spor Ekipmanı", "⚽"),
    PARTY("Parti & Etkinlik", "🎉"),
    OTHER("Diğer", "📦")
}
