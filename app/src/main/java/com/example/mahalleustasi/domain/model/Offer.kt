package com.example.mahalleustasi.domain.model

data class Offer(
    val id: String                 = "",
    val jobId: String              = "",
    val status: OfferStatus        = OfferStatus.PENDING,
    val price: Double              = 0.0,
    val description: String        = "",
    val offeredByUserId: String    = "",
    val offeredByUserName: String  = "",
    val offeredByPhotoUrl: String? = null,
    val createdAt: Long            = System.currentTimeMillis()
)

enum class OfferStatus(val displayName: String) {
    PENDING("Beklemede"),
    ACCEPTED("Kabul Edildi"),
    REJECTED("Reddedildi")
}
