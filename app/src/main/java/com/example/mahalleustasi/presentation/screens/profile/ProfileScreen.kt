package com.example.mahalleustasi.presentation.screens.profile

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mahalleustasi.domain.model.AiTrustAnalysis
import com.example.mahalleustasi.domain.model.Review
import com.example.mahalleustasi.ui.theme.BrandOrange80
import com.example.mahalleustasi.ui.theme.ForestGreen20
import com.example.mahalleustasi.ui.theme.ForestGreen40
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    if (viewModel.isOwnProfile) {
                        IconButton(onClick = {
                            viewModel.logout()
                            onLogout()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor            = ForestGreen20,
                    titleContentColor         = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor    = Color.White
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ── Profil Header ──────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(ForestGreen20, ForestGreen40)))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(88.dp).clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person, null,
                                modifier = Modifier.size(52.dp), tint = Color.White
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text       = uiState.profileUser?.name ?: currentUser?.name ?: "Kullanıcı",
                            style      = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                        Text(
                            text  = uiState.profileUser?.email ?: currentUser?.email ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                            val user = uiState.profileUser
                            ProfileStat(
                                label = "Puan",
                                value = if ((user?.rating ?: 0f) > 0) "%.1f ⭐".format(user?.rating ?: 0f) else "—"
                            )
                            ProfileStat(label = "Yorum", value = "${user?.reviewCount ?: 0}")
                            ProfileStat(label = "İş", value = "${user?.completedJobsCount ?: 0}")
                        }
                    }
                }
            }

            // ── AI Güven Analizi Kartı ─────────────────────────────────────
            item {
                Spacer(Modifier.height(12.dp))
                AiTrustCard(
                    analysis  = uiState.aiAnalysis,
                    isLoading = uiState.isAiLoading,
                    onRefresh = { viewModel.refreshAiAnalysis() },
                    modifier  = Modifier.padding(horizontal = 16.dp)
                )
            }

            // ── Sekme Seçici ──────────────────────────────────────────────
            item {
                Spacer(Modifier.height(12.dp))
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor   = MaterialTheme.colorScheme.surface,
                    contentColor     = ForestGreen40
                ) {
                    listOf(
                        "🔧 Yaptığı İşler (${uiState.workerReviews.size})",
                        "🤝 Yaptırdıkları (${uiState.clientReviews.size})"
                    ).forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick  = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize   = 12.sp
                                )
                            }
                        )
                    }
                }
            }

            // ── Review Listesi ─────────────────────────────────────────────
            val reviews = if (selectedTab == 0) uiState.workerReviews else uiState.clientReviews
            if (reviews.isEmpty()) {
                item {
                    Column(
                        modifier            = Modifier.fillMaxWidth().padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(if (selectedTab == 0) "🔧" else "🤝", fontSize = 40.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Henüz yorum yok",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (selectedTab == 0) "Tamamlanan işler burada görünür"
                            else "Yaptırılan işler burada görünür",
                            style     = MaterialTheme.typography.bodySmall,
                            color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(reviews) { review -> ReviewCard(review = review) }
            }
        }
    }
}

// ─── AI Trust Card ─────────────────────────────────────────────────────────────
@Composable
fun AiTrustCard(
    analysis: AiTrustAnalysis?,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Başlık
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome, null,
                        tint     = BrandOrange80,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "AI Güven Analizi",
                        fontWeight = FontWeight.Bold,
                        style      = MaterialTheme.typography.titleSmall,
                        color      = BrandOrange80
                    )
                }
                IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Refresh, "Yenile",
                        modifier = Modifier.size(18.dp),
                        tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            when {
                isLoading -> {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = BrandOrange80)
                        Text("Yorumlar analiz ediliyor...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
                analysis != null -> {
                    // Güven Skoru
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        analysis.score >= 75 -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                        analysis.score >= 50 -> BrandOrange80.copy(alpha = 0.15f)
                                        else                 -> Color(0xFFF44336).copy(alpha = 0.15f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text       = "${analysis.score}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize   = 22.sp,
                                    color      = when {
                                        analysis.score >= 75 -> Color(0xFF4CAF50)
                                        analysis.score >= 50 -> BrandOrange80
                                        else                 -> Color(0xFFF44336)
                                    }
                                )
                                Text("/100", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text  = analysis.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Güçlü Yönler
                    if (analysis.strengths.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text("💪 Güçlü Yönler", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        analysis.strengths.forEach { strength ->
                            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
                                Text("• ", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                Text(strength, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // Zayıf Yönler
                    if (analysis.weaknesses.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("⚠️ Dikkat Edilmesi Gerekenler", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        analysis.weaknesses.forEach { weakness ->
                            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
                                Text("• ", color = BrandOrange80, fontWeight = FontWeight.Bold)
                                Text(weakness, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                else -> {
                    Text(
                        "Analiz başlatmak için yorumları bekleyin...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

// ─── Review Kartı ─────────────────────────────────────────────────────────────
@Composable
private fun ReviewCard(review: Review) {
    val date     = SimpleDateFormat("dd MMM yyyy", Locale("tr")).format(Date(review.createdAt))
    val initials = review.reviewerName.trim().split(" ")
        .take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            // Avatar
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(ForestGreen40.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(initials.ifEmpty { "?" }, color = ForestGreen40, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(review.reviewerName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(review.rating.toInt()) {
                            Icon(Icons.Default.Star, null, tint = BrandOrange80, modifier = Modifier.size(14.dp))
                        }
                        Text(" %.1f".format(review.rating), fontWeight = FontWeight.Bold, color = BrandOrange80, fontSize = 12.sp)
                    }
                }
                Text(date, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                if (review.comment.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "\"${review.comment}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
    }
}
