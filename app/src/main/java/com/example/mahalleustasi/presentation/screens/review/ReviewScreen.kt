package com.example.mahalleustasi.presentation.screens.review

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mahalleustasi.ui.theme.BrandOrange80
import com.example.mahalleustasi.ui.theme.ForestGreen20
import com.example.mahalleustasi.ui.theme.ForestGreen40

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
            kotlinx.coroutines.delay(1800) // Teşekkür ekranı göster, sonra çık
            onReviewSubmitted()
        }
    }

    AnimatedContent(
        targetState = uiState.isSubmitted,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "review_transition"
    ) { isSubmitted ->
        if (isSubmitted) {
            // ── Başarı Ekranı ──────────────────────────────────────────────
            ThankYouScreen(revieweeName = viewModel.revieweeName, rating = rating)
        } else {
            // ── Değerlendirme Formu ────────────────────────────────────────
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
                        .verticalScroll(rememberScrollState())
                ) {
                    // ── Gradient Başlık Kartı ──────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(ForestGreen20, ForestGreen40)))
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👤", fontSize = 36.sp)
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = viewModel.revieweeName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "nasıl bir deneyim yaşadınız?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // ── Form Bölümü ───────────────────────────────────────
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {

                        // ── Yıldız Seçici (Animasyonlu) ───────────────────
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Puan verin",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(12.dp))

                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                (1..5).forEach { star ->
                                    val isSelected = star <= rating
                                    val scale by animateFloatAsState(
                                        targetValue = if (isSelected) 1.25f else 1f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                        label = "star_scale_$star"
                                    )

                                    IconButton(
                                        onClick = { rating = star.toFloat() },
                                        modifier = Modifier.scale(scale)
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                            contentDescription = "$star yıldız",
                                            tint = if (isSelected) BrandOrange80 else MaterialTheme.colorScheme.outlineVariant,
                                            modifier = Modifier.size(44.dp)
                                        )
                                    }
                                }
                            }

                            // Puan etiketi
                            AnimatedVisibility(visible = rating > 0f) {
                                val ratingText = when (rating.toInt()) {
                                    1 -> "😞 Çok Kötü"
                                    2 -> "😐 Kötü"
                                    3 -> "🙂 Orta"
                                    4 -> "😊 İyi"
                                    5 -> "🤩 Mükemmel!"
                                    else -> ""
                                }
                                Surface(
                                    color = BrandOrange80.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        ratingText,
                                        color = BrandOrange80,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // ── Yorum Alanı ───────────────────────────────────
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

                        // ── Hata Mesajı ───────────────────────────────────
                        uiState.error?.let {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    it,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        // ── Gönder Butonu ─────────────────────────────────
                        Button(
                            onClick = { viewModel.submitReview(rating, comment) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = rating > 0f && !uiState.isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandOrange80),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.Star, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Değerlendirmeyi Gönder", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                        TextButton(
                            onClick = onSkip,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Şimdi Değil", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

// ── Animasyonlu Teşekkür Ekranı ─────────────────────────────────────────────
@Composable
private fun ThankYouScreen(revieweeName: String, rating: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emoji_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ForestGreen20, ForestGreen40))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                "🎉",
                fontSize = 72.sp,
                modifier = Modifier.scale(scale)
            )
            Text(
                "Teşekkürler!",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                "$revieweeName için değerlendirmeniz\ngönderildi.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            // Verilen yıldız göstergesi
            Row {
                (1..5).forEach { star ->
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = if (star <= rating) Color(0xFFFFD700) else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
