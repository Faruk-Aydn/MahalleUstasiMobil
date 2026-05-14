package com.example.mahalleustasi.presentation.screens.offers

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mahalleustasi.domain.model.Offer
import com.example.mahalleustasi.domain.model.OfferStatus
import com.example.mahalleustasi.ui.theme.BrandOrange80
import com.example.mahalleustasi.ui.theme.ForestGreen40
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffersScreen(
    onNavigateToJobDetail: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    viewModel: OffersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(uiState.navigateToChatId) {
        uiState.navigateToChatId?.let { chatId ->
            onNavigateToChat(chatId)
            viewModel.clearNavigation()
        }
    }

    LaunchedEffect(uiState.actionSuccess) {
        uiState.actionSuccess?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearNavigation()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tekliflerim", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            "${uiState.sentOffers.size} gönderilen · ${uiState.receivedOffers.size} alınan",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // ── Tab Row ─────────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor   = MaterialTheme.colorScheme.surface,
                contentColor     = BrandOrange80
            ) {
                listOf(
                    "📤 Verdiğim (${uiState.sentOffers.size})",
                    "📥 Aldığım (${uiState.receivedOffers.size})"
                ).forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick  = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            // ── İçerik ──────────────────────────────────────────────────────
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandOrange80)
                    }
                }
                selectedTab == 0 -> {
                    SentOffersTab(
                        offers     = uiState.sentOffers,
                        jobTitles  = uiState.jobTitles,
                        onJobClick = onNavigateToJobDetail
                    )
                }
                selectedTab == 1 -> {
                    ReceivedOffersTab(
                        offers     = uiState.receivedOffers,
                        onAccept   = { viewModel.acceptOffer(it) },
                        onReject   = { viewModel.rejectOffer(it.id) },
                        onJobClick = onNavigateToJobDetail
                    )
                }
            }
        }
    }
}

// ─── Verdiğim Teklifler Sekmesi ────────────────────────────────────────────────
@Composable
private fun SentOffersTab(
    offers: List<Offer>,
    jobTitles: Map<String, String>,
    onJobClick: (String) -> Unit
) {
    if (offers.isEmpty()) {
        EmptyState(icon = "📤", title = "Henüz teklif vermediniz", sub = "Çevrenizdeki ilanlara teklif verin!")
    } else {
        LazyColumn(
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(offers) { offer ->
                SentOfferCard(
                    offer    = offer,
                    jobTitle = jobTitles[offer.jobId] ?: "İlan yükleniyor...",
                    onClick  = { onJobClick(offer.jobId) }
                )
            }
        }
    }
}

// ─── Aldığım Teklifler Sekmesi ─────────────────────────────────────────────────
@Composable
private fun ReceivedOffersTab(
    offers: List<Offer>,
    onAccept: (Offer) -> Unit,
    onReject: (Offer) -> Unit,
    onJobClick: (String) -> Unit
) {
    if (offers.isEmpty()) {
        EmptyState(
            icon  = "📥",
            title = "Henüz teklif gelmedi",
            sub   = "İlanlarınıza teklif geldiğinde burada görünecek."
        )
    } else {
        LazyColumn(
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(offers) { offer ->
                ReceivedOfferCard(
                    offer    = offer,
                    onAccept = { onAccept(offer) },
                    onReject = { onReject(offer) },
                    onJobClick = { onJobClick(offer.jobId) }
                )
            }
        }
    }
}

// ─── Verdiğim Teklif Kartı (YENİ: İlan başlığı gösteriliyor) ─────────────────
@Composable
private fun SentOfferCard(offer: Offer, jobTitle: String, onClick: () -> Unit) {
    val date = SimpleDateFormat("dd MMM yyyy", Locale("tr")).format(Date(offer.createdAt))

    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // İlan başlığı — Artık ID değil gerçek isim!
                Text(
                    text     = jobTitle,
                    fontWeight = FontWeight.Bold,
                    style    = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                OfferStatusChip(offer.status)
            }

            Spacer(Modifier.height(4.dp))
            Text(date, style = MaterialTheme.typography.labelSmall, color = Color.Gray)

            if (offer.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    offer.description,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Teklifiniz:", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Text(
                    text       = "${offer.price.toLong()} ₺",
                    color      = BrandOrange80,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 20.sp
                )
            }
        }
    }
}

// ─── Aldığım Teklif Kartı (YENİ: Profesyonel avatar & layout) ─────────────────
@Composable
private fun ReceivedOfferCard(
    offer: Offer,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onJobClick: () -> Unit
) {
    val date      = SimpleDateFormat("dd MMM yyyy", Locale("tr")).format(Date(offer.createdAt))
    val isPending = offer.status == OfferStatus.PENDING
    val initials  = offer.offeredByUserName.trim().split(" ")
        .take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ── Header: Avatar + İsim + Durum ─────────────────────────────
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier.fillMaxWidth()
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(BrandOrange80.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text     = initials.ifEmpty { "?" },
                        color    = BrandOrange80,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = offer.offeredByUserName,
                        fontWeight = FontWeight.Bold,
                        style      = MaterialTheme.typography.bodyLarge
                    )
                    Text(date, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                OfferStatusChip(offer.status)
            }

            Spacer(Modifier.height(12.dp))

            // Teklif metni
            if (offer.description.isNotBlank()) {
                Text(
                    "\"${offer.description}\"",
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(12.dp))

            // ── Alt: Fiyat + Butonlar ─────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text("Teklif Fiyatı", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(
                        text       = "${offer.price.toLong()} ₺",
                        color      = BrandOrange80,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 22.sp
                    )
                }

                if (isPending) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onReject,
                            colors  = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            shape   = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reddet", fontSize = 13.sp)
                        }
                        Button(
                            onClick = onAccept,
                            colors  = ButtonDefaults.buttonColors(containerColor = ForestGreen40),
                            shape   = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(Modifier.width(4.dp))
                            Text("Kabul Et", color = Color.White, fontSize = 13.sp)
                        }
                    }
                } else if (offer.status == OfferStatus.ACCEPTED) {
                    FilledTonalButton(
                        onClick = onJobClick,
                        shape   = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("İlana Git")
                    }
                }
            }
        }
    }
}

// ─── Durum Chip ───────────────────────────────────────────────────────────────
@Composable
fun OfferStatusChip(status: OfferStatus) {
    val (color, label) = when (status) {
        OfferStatus.PENDING  -> Color(0xFFFFA000) to "Beklemede"
        OfferStatus.ACCEPTED -> Color(0xFF4CAF50) to "Kabul Edildi"
        OfferStatus.REJECTED -> Color(0xFFF44336) to "Reddedildi"
    }
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(20.dp)) {
        Text(
            text     = label,
            color    = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style    = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

// Eski StatusChip - geriye uyumluluk için
@Composable
fun StatusChip(status: OfferStatus) = OfferStatusChip(status)

// ─── Boş Durum ────────────────────────────────────────────────────────────────
@Composable
private fun EmptyState(icon: String, title: String, sub: String) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(icon, fontSize = 52.sp)
        Spacer(Modifier.height(16.dp))
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(sub, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
    }
}
