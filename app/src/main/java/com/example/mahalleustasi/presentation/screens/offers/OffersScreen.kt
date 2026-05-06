package com.example.mahalleustasi.presentation.screens.offers

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mahalleustasi.domain.model.Offer
import com.example.mahalleustasi.domain.model.OfferStatus
import com.example.mahalleustasi.ui.theme.BrandOrange80
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
    val tabs = listOf("Verdiğim Teklifler", "Aldığım Teklifler")
    val context = LocalContext.current

    // Teklif kabul sonrası Chat'e git
    LaunchedEffect(uiState.navigateToChatId) {
        uiState.navigateToChatId?.let { chatId ->
            onNavigateToChat(chatId)
            viewModel.clearNavigation()
        }
    }

    // Başarı mesajı toast
    LaunchedEffect(uiState.actionSuccess) {
        uiState.actionSuccess?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearNavigation()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tekliflerim", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Tab Seçici ──────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                contentColor = BrandOrange80
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            // ── İçerik ─────────────────────────────────────────────────────
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandOrange80)
                    }
                }
                selectedTab == 0 -> {
                    SentOffersTab(
                        offers = uiState.sentOffers,
                        onJobClick = onNavigateToJobDetail
                    )
                }
                selectedTab == 1 -> {
                    ReceivedOffersTab(
                        offers = uiState.receivedOffers,
                        onAccept = { viewModel.acceptOffer(it) },
                        onReject = { viewModel.rejectOffer(it.id) },
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
    onJobClick: (String) -> Unit
) {
    if (offers.isEmpty()) {
        EmptyState(icon = "📤", title = "Henüz teklif vermediniz", sub = "Çevrenizdeki ilanlara teklif verin!")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(offers) { offer ->
                SentOfferCard(offer = offer, onClick = { onJobClick(offer.jobId) })
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
        EmptyState(icon = "📥", title = "İlanlarınıza henüz teklif gelmedi", sub = "İlanlarınıza teklif geldiğinde burada görünecek.")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(offers) { offer ->
                ReceivedOfferCard(
                    offer = offer,
                    onAccept = { onAccept(offer) },
                    onReject = { onReject(offer) },
                    onJobClick = { onJobClick(offer.jobId) }
                )
            }
        }
    }
}

// ─── Verdiğim Teklif Kartı ─────────────────────────────────────────────────────
@Composable
private fun SentOfferCard(offer: Offer, onClick: () -> Unit) {
    val date = SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date(offer.createdAt))

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                StatusChip(offer.status)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "İlan: ${offer.jobId.take(8)}...",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(4.dp))
            Text(offer.description, maxLines = 2, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Teklifiniz:", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "${offer.price} TL",
                    color = BrandOrange80,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

// ─── Aldığım Teklif Kartı ─────────────────────────────────────────────────────
@Composable
private fun ReceivedOfferCard(
    offer: Offer,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onJobClick: () -> Unit
) {
    val date = SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date(offer.createdAt))
    val isPending = offer.status == OfferStatus.PENDING

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = offer.offeredByUserName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                StatusChip(offer.status)
            }

            Spacer(Modifier.height(4.dp))
            Text(date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            Text(offer.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${offer.price} TL",
                    color = BrandOrange80,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )

                // Sadece PENDING tekliflerde Kabul/Red butonları göster
                if (isPending) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onReject,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reddet", fontSize = 12.sp)
                        }

                        Button(
                            onClick = onAccept,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(Modifier.width(4.dp))
                            Text("Kabul Et", color = Color.White, fontSize = 12.sp)
                        }
                    }
                } else {
                    // Kabul edilmişse "Sohbete Git" butonu
                    if (offer.status == OfferStatus.ACCEPTED) {
                        TextButton(onClick = onJobClick) {
                            Text("İlana Git →")
                        }
                    }
                }
            }
        }
    }
}

// ─── Status Chip ──────────────────────────────────────────────────────────────
@Composable
fun StatusChip(status: OfferStatus) {
    val (color, label) = when (status) {
        OfferStatus.PENDING  -> Color(0xFFFFA000) to "Beklemede"
        OfferStatus.ACCEPTED -> Color(0xFF4CAF50) to "Kabul Edildi"
        OfferStatus.REJECTED -> Color(0xFFF44336) to "Reddedildi"
    }
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = label,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─── Boş Durum ────────────────────────────────────────────────────────────────
@Composable
private fun EmptyState(icon: String, title: String, sub: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(icon, fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(sub, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
    }
}
