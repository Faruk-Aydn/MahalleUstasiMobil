package com.example.mahalleustasi.core.util

/**
 * Generic wrapper class for UI state management.
 *
 * Kullanım:
 *   - Resource.Loading  → Yükleniyor göstergesi
 *   - Resource.Success  → Veri başarıyla alındı
 *   - Resource.Error    → Hata mesajı
 */
sealed class Resource<out T> {
    data object Loading : Resource<Nothing>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
}
