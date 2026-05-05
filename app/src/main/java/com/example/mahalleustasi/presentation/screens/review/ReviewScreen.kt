package com.example.mahalleustasi.presentation.screens.review

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mahalleustasi.ui.theme.BrandOrange80
import com.example.mahalleustasi.ui.theme.ForestGreen20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    onReviewSubmitted: () -> Unit,
    onSkip: () -> Unit,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var rating by remember { mutableFloatStateOf(0f) }
    var comment by remember { mutableStateOf("") }

    // Yorum gönderilince ana sayfaya dön
    LaunchedEffect(uiState.isSubmitted) {
        if (uiState.isSubmitted) {
            onReviewSubmitted()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Değerlendirme", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ForestGreen20,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Başlık ─────────────────────────────────────────────────────
            Text("⭐", fontSize = 56.sp)
            Text(
                text = "${viewModel.revieweeName} için değerlendirme",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Deneyiminizi paylaşarak toplulukta güveni artırın.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            // ── Yıldız Seçici ──────────────────────────────────────────────
            Text("Puan:", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.Center) {
                (1..5).forEach { star ->
                    IconButton(onClick = { rating = star.toFloat() }) {
                        Icon(
                            imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = "$star yıldız",
                            tint = if (star <= rating) BrandOrange80 else Color.Gray,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            if (rating > 0f) {
                val ratingText = when (rating.toInt()) {
                    1 -> "Çok Kötü"
                    2 -> "Kötü"
                    3 -> "Orta"
                    4 -> "İyi"
                    5 -> "Mükemmel!"
                    else -> ""
                }
                Text(ratingText, color = BrandOrange80, fontWeight = FontWeight.Bold)
            }

            // ── Yorum Alanı ────────────────────────────────────────────────
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                label = { Text("Yorumunuz (opsiyonel)") },
                placeholder = { Text("İşin kalitesi, dakikliği, iletişimi hakkında ne düşünüyorsunuz?") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandOrange80,
                    focusedLabelColor = BrandOrange80
                )
            )

            // ── Hata Mesajı ────────────────────────────────────────────────
            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.weight(1f))

            // ── Gönder Butonu ──────────────────────────────────────────────
            Button(
                onClick = { viewModel.submitReview(rating, comment) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = rating > 0f && !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange80),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Değerlendirmeyi Gönder", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            TextButton(onClick = onSkip) {
                Text("Atla", color = Color.Gray)
            }
        }
    }
}
