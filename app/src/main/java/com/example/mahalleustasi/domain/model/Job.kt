package com.example.mahalleustasi.domain.model

data class Job(
    val id: String                 = "",
    val title: String              = "",
    val description: String        = "",
    val category: JobCategory      = JobCategory.OTHER,
    val status: JobStatus          = JobStatus.OPEN,
    val location: JobLocation      = JobLocation(),
    val budget: String?            = null,
    val photoUrls: List<String>    = emptyList(),
    val postedByUserId: String     = "",
    val postedByUserName: String   = "",
    val acceptedOfferId: String?   = null,
    val offerCount: Int            = 0,
    val createdAt: Long            = System.currentTimeMillis(),
    val updatedAt: Long            = System.currentTimeMillis()
)

data class JobLocation(
    val address: String = "",
    val lat: Double     = 0.0,
    val lng: Double     = 0.0
)

enum class JobCategory(val displayName: String) {
    REPAIR("Tamirat"),
    CLEANING("Temizlik"),
    MOVING("Nakliye"),
    TUTORING("Özel Ders"),
    GARDENING("Bahçe"),
    TECH_SUPPORT("Teknik Destek"),
    OTHER("Diğer")
}

enum class JobStatus(val displayName: String) {
    OPEN("Açık"),
    IN_PROGRESS("Devam Ediyor"),
    WAITING_CONFIRMATION("Onay Bekliyor"),
    COMPLETED("Tamamlandı"),
    CANCELLED("İptal Edildi")
}
