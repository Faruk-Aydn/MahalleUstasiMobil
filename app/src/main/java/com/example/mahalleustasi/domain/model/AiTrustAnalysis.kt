package com.example.mahalleustasi.domain.model

data class AiTrustAnalysis(
    val score: Int = 0,               // 0-100 arası güven puanı
    val summary: String = "",         // Genel özet metni
    val strengths: List<String> = emptyList(), // Güçlü yönler
    val weaknesses: List<String> = emptyList(), // Zayıf yönler/Eleştiriler
    val analyzedAt: Long = System.currentTimeMillis()
)
