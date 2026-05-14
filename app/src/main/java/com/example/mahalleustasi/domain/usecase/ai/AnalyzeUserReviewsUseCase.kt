package com.example.mahalleustasi.domain.usecase.ai

import com.example.mahalleustasi.BuildConfig
import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.AiTrustAnalysis
import com.example.mahalleustasi.domain.model.Review
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

class AnalyzeUserReviewsUseCase @Inject constructor() {

    operator fun invoke(reviews: List<Review>): Flow<Resource<AiTrustAnalysis>> = flow {
        if (reviews.isEmpty()) {
            emit(Resource.Success(AiTrustAnalysis(
                score = 0,
                summary = "Henüz yeterli yorum bulunmuyor. İlk yorumu siz yapın!",
                strengths = emptyList(),
                weaknesses = emptyList()
            )))
            return@flow
        }

        emit(Resource.Loading)
        try {
            val generativeModel = GenerativeModel(
                modelName = "gemini-2.5-flash-lite", // Stable, ultra-fast, low-cost
                apiKey = BuildConfig.GEMINI_API_KEY
            )

            // Yorumları metin haline getir
            val reviewsText = reviews.joinToString("\n") { "- [Rating: ${it.rating}/5] ${it.comment}" }

            val prompt = """
                Sen "Mahalle Ustası" platformu için uzman bir kullanıcı güven analistisin. 
                Aşağıdaki kullanıcı yorumlarını analiz et ve kullanıcının güvenilirliği hakkında SADECE JSON formatında bir rapor oluştur.
                
                GİRDİ YORUMLAR:
                $reviewsText
                
                ÇIKTI FORMATI (JSON):
                {
                  "score": 0-100 arası tam sayı (Yıldız puanı ve yorum içeriğine göre hesapla),
                  "summary": "Genel güvenilirliği anlatan 1-2 cümlelik profesyonel özet (Türkçe)",
                  "strengths": ["Güçlü yön 1", "Güçlü yön 2"],
                  "weaknesses": ["Zayıf yön 1", "Eksik nokta"]
                }
                
                KURALLAR:
                - Yanıt sadece JSON olmalı. Markdown block (```json) kullanma.
                - Özet metni samimi ama profesyonel olsun.
                - Eğer eleştiri yoksa weaknesses boş liste olsun.
            """.trimIndent()

            val response = generativeModel.generateContent(
                content {
                    text(prompt)
                }
            )

            val responseText = response.text
            if (responseText != null) {
                val cleanJson = responseText.replace("```json", "").replace("```", "").trim()
                val json = JSONObject(cleanJson)
                
                val strengthsList = mutableListOf<String>()
                val strengthsArray = json.optJSONArray("strengths")
                if (strengthsArray != null) {
                    for (i in 0 until strengthsArray.length()) {
                        strengthsList.add(strengthsArray.getString(i))
                    }
                }

                val weaknessesList = mutableListOf<String>()
                val weaknessesArray = json.optJSONArray("weaknesses")
                if (weaknessesArray != null) {
                    for (i in 0 until weaknessesArray.length()) {
                        weaknessesList.add(weaknessesArray.getString(i))
                    }
                }

                val analysis = AiTrustAnalysis(
                    score = json.optInt("score", 0),
                    summary = json.optString("summary", ""),
                    strengths = strengthsList,
                    weaknesses = weaknessesList
                )
                
                emit(Resource.Success(analysis))
            } else {
                emit(Resource.Error("Yapay zeka analiz yapamadı."))
            }

        } catch (e: Exception) {
            emit(Resource.Error("Analiz hatası: ${e.localizedMessage}"))
        }
    }
}
