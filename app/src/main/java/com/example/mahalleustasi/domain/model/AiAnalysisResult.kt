package com.example.mahalleustasi.domain.model

data class AiAnalysisResult(
    val title: String = "",
    val description: String = "",
    val category: JobCategory = JobCategory.OTHER,
    val estimatedCost: String = ""
)
