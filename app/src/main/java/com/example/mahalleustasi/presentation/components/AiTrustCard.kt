package com.example.mahalleustasi.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mahalleustasi.domain.model.AiTrustAnalysis
import com.example.mahalleustasi.ui.theme.BrandOrange80

@Composable
fun AiTrustCard(
    analysis: AiTrustAnalysis?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = BrandOrange80.copy(alpha = 0.05f)
        ),
        border = BorderStroke(1.dp, BrandOrange80.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = BrandOrange80,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "AI Güven Analizi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandOrange80
                    )
                }

                if (analysis != null && !isLoading) {
                    Surface(
                        color = when {
                            analysis.score >= 80 -> Color(0xFF4CAF50)
                            analysis.score >= 50 -> BrandOrange80
                            else -> Color(0xFFF44336)
                        },
                        shape = CircleShape
                    ) {
                        Text(
                            "%${analysis.score}",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = BrandOrange80)
                    Spacer(Modifier.width(12.dp))
                    Text("Yapay zeka yorumları inceliyor...", style = MaterialTheme.typography.bodyMedium)
                }
            } else if (analysis != null) {
                Text(
                    text = analysis.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )

                if (analysis.strengths.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    analysis.strengths.forEach { strength ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strength, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                Text("Analiz hazırlanıyor...", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
