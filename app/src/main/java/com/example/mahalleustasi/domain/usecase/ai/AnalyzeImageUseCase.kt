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

    operator fun invoke(bitmap: Bitmap, mode: String = "job"): Flow<Resource<AiAnalysisResult>> = flow {
        emit(Resource.Loading)
        try {
            val generativeModel = GenerativeModel(
                modelName = "gemini-3.1-flash-lite", // Stable, ultra-fast, low-cost
                apiKey = BuildConfig.GEMINI_API_KEY
            )

            val prompt = if (mode == "rental") {
                """
                Sen "Mahalle Ustası" platformu için çalışan bir eşya kiralama asistanısın.
                Türk kullanıcıların kiralamak istedikleri eşyaları fotoğraflayıp ilan oluşturmasına yardımcı oluyorsun.
                
                Fotoğraftaki eşyayı analiz et ve SADECE aşağıdaki JSON formatında yanıt ver.
                Kesinlikle başka metin, açıklama veya markdown ekleme.
                
                KURALLAR:
                - title: Kısa, net, Türkçe. Eşyanın adını tanımla. (Maks 60 karakter)
                - description: Eşyanın ne olduğunu, genel durumunu ve kiralayacak kişiye faydasını anlat. (2-3 cümle, Türkçe)
                - category: Aşağıdaki listeden SADECE biri olmalı (büyük harf, İngilizce adıyla):
                    TOOLS = Alet & Ekipman (matkap, testere vb.)
                    ELECTRONICS = Elektronik (kamera, konsol, hoparlör vb.)
                    VEHICLES = Araç (bisiklet, scooter vb.)
                    FURNITURE = Mobilya (masa, sandalye vb.)
                    SPORTS = Spor Ekipmanı (çadır, raket, ağırlık vb.)
                    PARTY = Parti & Etkinlik
                    OTHER = Diğer
                - estimatedCost: Türkiye piyasasına göre gerçekçi GÜNLÜK kiralama bedeli tahmini. Sadece sayı. Örnek: "150"
                
                ÖRNEK ÇIKTILAR:
                Matkap için:
                {"title":"Darbeli Matkap","description":"Evdeki tadilat işleriniz için ideal, güçlü darbeli matkap. Uç setiyle birlikte verilir.","category":"TOOLS","estimatedCost":"150"}
                
                Çadır için:
                {"title":"3 Kişilik Kamp Çadırı","description":"Su geçirmez, kolay kurulan 3 kişilik kamp çadırı. Hafta sonu kampları için çok uygundur.","category":"SPORTS","estimatedCost":"200"}
                
                Şimdi gönderilen fotoğrafı analiz et ve sadece JSON döndür:
                """.trimIndent()
            } else {
                """
                Sen "Mahalle Ustası" platformu için çalışan bir görüntü analiz asistanısın.
                Türk müşterilerin ev/işyeri arızalarını fotoğraflayıp usta bulmasına yardımcı oluyorsun.
                
                Fotoğraftaki arızayı veya tamirat ihtiyacını analiz et ve SADECE aşağıdaki JSON formatında yanıt ver.
                Kesinlikle başka metin, açıklama veya markdown ekleme.
                
                KURALLAR:
                - title: Kısa, net, Türkçe. Sadece arızayı tanımla. (Maks 60 karakter)
                - description: Sorunu, muhtemel sebebini ve ustaya ne yapması gerektiğini anlat. (2-3 cümle, Türkçe)
                - category: Aşağıdaki listeden SADECE biri olmalı (büyük harf, İngilizce adıyla):
                    REPAIR = Tamirat/Tadilat (boru, musluk, kapı, kilit, pencere, çatı vb.)
                    CLEANING = Temizlik (halı, derin temizlik, cam silme vb.)
                    MOVING = Nakliyat/Taşımacılık
                    TUTORING = Özel Ders/Eğitim
                    GARDENING = Bahçe/Peyzaj
                    TECH_SUPPORT = Elektrik/Elektronik/Teknoloji (kablo, priz, beyaz eşya vb.)
                    OTHER = Diğer
                - estimatedCost: Türkiye piyasasına göre gerçekçi TL tahmini. Örnek: "150 - 300 TL"
                
                ÖRNEK ÇIKTILAR:
                Musluk arızası için:
                {"title":"Mutfak Musluğu Damlıyor","description":"Mutfak lavabo musluğundan sürekli su damlaması var. Conta veya kartuş yıpranmış olabilir. Ustanın musluğu söküp conta/kartuş değişimi yapması gerekiyor.","category":"REPAIR","estimatedCost":"100 - 250 TL"}
                
                Elektrik arızası için:
                {"title":"Priz Çalışmıyor","description":"Odadaki priz elektrik vermiyor. Sigorta atmış ya da iç kablo kopmuş olabilir. Elektrikçinin sigortaları ve kablo bağlantılarını kontrol etmesi gerekiyor.","category":"TECH_SUPPORT","estimatedCost":"80 - 200 TL"}
                
                Şimdi gönderilen fotoğrafı analiz et ve sadece JSON döndür:
                """.trimIndent()
            }

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
                val title = jsonObject.optString("title", if (mode == "rental") "Eşya Tespiti" else "Arıza Tespiti")
                val description = jsonObject.optString("description", "")
                val categoryString = jsonObject.optString("category", "OTHER")
                val estimatedCost = jsonObject.optString("estimatedCost", "")

                val result = AiAnalysisResult(
                    title = title,
                    description = description,
                    category = categoryString,
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
