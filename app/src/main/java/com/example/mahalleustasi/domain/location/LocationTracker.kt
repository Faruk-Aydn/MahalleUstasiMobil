package com.example.mahalleustasi.domain.location

import android.location.Location

/**
 * Konum servislerini soyutlayan arayüz.
 * MVVM mimarisine uygun olarak, konum alma işlemlerini domain katmanında temsil eder.
 */
interface LocationTracker {
    suspend fun getCurrentLocation(): Location?
    suspend fun getCoordinatesFromAddress(address: String): Pair<Double, Double>?
}
