package com.example.mahalleustasi.domain.usecase.ai

import android.graphics.Bitmap
import com.example.mahalleustasi.BuildConfig
import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.AiAnalysisResult
import com.example.mahalleustasi.domain.model.JobCategory
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
import javax.inject.Inject

class AnalyzeImageUseCase @Inject constructor() {

    operator fun invoke(bitmap: Bitmap): Flow<Resource<AiAnalysisResult>> = flow {
        emit(Resource.Loading)
        try {
            val generativeModel = GenerativeModel(
                modelName = "gemini-2.0-flash",
                apiKey = BuildConfig.GEMINI_API_KEY
            )

            val prompt = """
                Sen usta ve müşterileri buluşturan "Mahalle Ustası" adlı bir platformda yapay zeka asistanısın. 
                Sana bir arıza veya tamirat gerektiren eşyanın fotoğrafını gönderiyorum. 
                Lütfen bu fotoğrafı incele ve bana **sadece aşağıdaki JSON formatında** dönüş yap. Markdown tagleri veya ekstra metin kullanma.
                
                {
                  "title": "İlan için kısa ve açıklayıcı bir başlık (örnek: Kırık Musluk Tamiri)",
                  "description": "Sorunun ne olduğuna dair detaylı açıklama (örnek: Banyo musluğundan su damlatıyor, contası değişmesi gerekebilir.)",
                  "category": "Şu kategorilerden biri olmalı: REPAIR, CLEANING, MOVING, TUTORING, GARDENING, TECH_SUPPORT, OTHER",
                  "estimatedCost": "Tahmini maliyet aralığı (örnek: 200 - 500 TL)"
                }
            """.trimIndent()

            val response = generativeModel.generateContent(
                content {
                    image(bitmap)
                    text(prompt)
                }
            )

            val responseText = response.text
            if (responseText != null) {
                // Temizleme: Eğer gemini markdown json block olarak döndüyse temizle
                val cleanJson = responseText.replace("```json", "").replace("```", "").trim()
                
                val jsonObject = JSONObject(cleanJson)
                val title = jsonObject.optString("title", "Arıza Tespiti")
                val description = jsonObject.optString("description", "")
                val categoryString = jsonObject.optString("category", "OTHER")
                val estimatedCost = jsonObject.optString("estimatedCost", "")

                val category = try {
                    JobCategory.valueOf(categoryString)
                } catch (e: Exception) {
                    JobCategory.OTHER
                }

                val result = AiAnalysisResult(
                    title = title,
                    description = description,
                    category = category,
                    estimatedCost = estimatedCost
                )
                
                emit(Resource.Success(result))
            } else {
                emit(Resource.Error("Yapay zeka yanıt üretemedi."))
            }

        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Beklenmeyen bir hata oluştu"))
        }
    }
}
