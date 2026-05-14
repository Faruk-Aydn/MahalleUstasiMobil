package com.example.mahalleustasi.domain.model

data class AiAnalysisResult(
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val estimatedCost: String = "",
    val imageUri: String? = null
)
