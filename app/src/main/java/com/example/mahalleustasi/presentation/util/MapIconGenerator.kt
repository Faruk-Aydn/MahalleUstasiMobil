package com.example.mahalleustasi.presentation.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.example.mahalleustasi.domain.model.JobCategory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface

object MapIconGenerator {

    /**
     * Kategorisine göre harita pininin BitmapDescriptor'ını üretir.
     * Renkli bir daire içine kategoriye özel emoji (süpürge, anahtar vb.) çizer.
     */
    fun getCategoryIcon(context: Context, category: JobCategory): BitmapDescriptor {
        val colorInt = android.graphics.Color.parseColor(getCategoryColorHex(category))
        val emoji = getCategoryEmoji(category)
        
        // Bitmap oluştur (Örn: 70x70 piksel - daha minimal ve zarif)
        val size = 74
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Arka plan dairesi çiz
        val bgPaint = Paint().apply {
            color = colorInt
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint)

        // 2. Beyaz bir iç çerçeve (opsiyonel şıklık)
        val strokePaint = Paint().apply {
            color = android.graphics.Color.WHITE
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - 2f, strokePaint)

        // 3. Emojiyi çiz
        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = size * 0.6f // İkonu daireye oranla biraz daha belirgin yap
            textAlign = Paint.Align.CENTER
        }
        
        // Emojinin dikey ortalanması için offset hesapla
        val textBounds = Rect()
        textPaint.getTextBounds(emoji, 0, emoji.length, textBounds)
        val yOffset = textBounds.height() / 2f

        canvas.drawText(emoji, size / 2f, (size / 2f) + yOffset, textPaint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun getCategoryColorHex(category: JobCategory): String {
        return when (category) {
            JobCategory.REPAIR -> "#FF9800" // Turuncu
            JobCategory.CLEANING -> "#2196F3" // Mavi
            JobCategory.MOVING -> "#9C27B0" // Mor
            JobCategory.TUTORING -> "#4CAF50" // Yeşil
            JobCategory.GARDENING -> "#8BC34A" // Açık Yeşil
            JobCategory.TECH_SUPPORT -> "#607D8B" // Gri/Mavi
            JobCategory.OTHER -> "#F44336" // Kırmızı
        }
    }

    private fun getCategoryEmoji(category: JobCategory): String {
        return when (category) {
            JobCategory.REPAIR -> "🔧"
            JobCategory.CLEANING -> "🧹"
            JobCategory.MOVING -> "🚚"
            JobCategory.TUTORING -> "📚"
            JobCategory.GARDENING -> "🌿"
            JobCategory.TECH_SUPPORT -> "💻"
            JobCategory.OTHER -> "📌"
        }
    }
}
