package com.example.mahalleustasi

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Uygulama sınıfı.
 * @HiltAndroidApp → Hilt'in kod üretimini tetikler ve uygulama düzeyinde
 * bağımlılık grafiğini oluşturur.
 */
@HiltAndroidApp
class MahalleUstasiApp : Application()
