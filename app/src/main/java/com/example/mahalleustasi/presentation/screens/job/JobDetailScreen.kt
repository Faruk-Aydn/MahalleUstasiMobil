package com.example.mahalleustasi.presentation.screens.job

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mahalleustasi.domain.model.Job
import com.example.mahalleustasi.domain.model.JobStatus
import com.example.mahalleustasi.domain.model.Offer
import com.example.mahalleustasi.domain.model.OfferStatus
import com.example.mahalleustasi.ui.theme.BrandOrange80
import java.text.SimpleDateFormat
import java.util.*
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToReview: (jobId: String, revieweeId: String, revieweeName: String, role: String) -> Unit,
    viewModel: JobDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showOfferDialog by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Teklif başarıyla gönderildi
    LaunchedEffect(uiState.offerSuccess) {
        if (uiState.offerSuccess) {
            Toast.makeText(context, "Teklifiniz başarıyla gönderildi!", Toast.LENGTH_SHORT).show()
            showOfferDialog = false
            viewModel.resetOfferSuccess()
        }
    }

    // Chat'e yönlendir
    LaunchedEffect(uiState.navigateToChatId) {
        uiState.navigateToChatId?.let { chatId ->
            onNavigateToChat(chatId)
            viewModel.clearNavigation()
        }
    }

    // Review'a yönlendir
    LaunchedEffect(uiState.navigateToReview) {
        uiState.navigateToReview?.let { args ->
            onNavigateToReview(args.jobId, args.revieweeId, args.revieweeName, args.role)
            viewModel.clearNavigation()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("İlan Detayı", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading && uiState.job == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = BrandOrange80
                    )
                }
                uiState.error != null && uiState.job == null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.error)
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                        Button(onClick = viewModel::loadJobDetail) { Text("Tekrar Dene") }
                    }
                }
                uiState.job != null -> {
                    JobDetailContent(
                        job = uiState.job!!,
                        offers = uiState.offers,
                        isOwner = uiState.isOwner,
                        isAcceptedWorker = uiState.isAcceptedWorker,
                        isLoading = uiState.isLoading,
                        aiAnalysis = uiState.aiAnalysis,
                        isAiLoading = uiState.isAiLoading,
                        onOfferClick = { showOfferDialog = true },
                        onAcceptOffer = { viewModel.acceptOffer(it) },
                        onRejectOffer = { viewModel.rejectOffer(it.id) },
                        onCompleteJob = { showCompleteDialog = true },
                        onOpenChat = { viewModel.openChat() },
                        onReviewAsWorker = { viewModel.navigateToReviewAsClient() }
                    )
                }
            }

            // Teklif Ver Dialog
            if (showOfferDialog) {
                OfferSubmissionDialog(
                    onDismiss = { showOfferDialog = false },
                    onSubmit = { price, desc -> viewModel.submitOffer(price, desc) },
                    isLoading = uiState.isOfferLoading
                )
            }

            // İşi Tamamla Onay Dialog
            if (showCompleteDialog) {
                AlertDialog(
                    onDismissRequest = { showCompleteDialog = false },
                    title = { Text("İşi Tamamla", fontWeight = FontWeight.Bold) },
                    text = { Text("İşi tamamlandı olarak işaretlemek istiyor musunuz? Ardından karşılıklı değerlendirme yapabilirsiniz.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showCompleteDialog = false
                                viewModel.completeJob()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("Evet, Tamamlandı", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCompleteDialog = false }) {
                            Text("İptal")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun JobDetailContent(
    job: Job,
    offers: List<Offer>,
    isOwner: Boolean,
    isAcceptedWorker: Boolean,
    isLoading: Boolean,
    aiAnalysis: com.example.mahalleustasi.domain.model.AiTrustAnalysis?,
    isAiLoading: Boolean,
    onOfferClick: () -> Unit,
    onAcceptOffer: (Offer) -> Unit,
    onRejectOffer: (Offer) -> Unit,
    onCompleteJob: () -> Unit,
    onOpenChat: () -> Unit,
    onReviewAsWorker: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Fotoğraf ──────────────────────────────────────────────────────
        if (job.photoUrls.isNotEmpty()) {
            AsyncImage(
                model = job.photoUrls.first(),
                contentDescription = "İlan Fotoğrafı",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
        }

        // ── Kategori Chip ──────────────────────────────────────────────────
        Surface(
            color = BrandOrange80.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = job.category.displayName,
                color = BrandOrange80,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // ── Durum Chip ────────────────────────────────────────────────────
        val statusColor = when (job.status) {
            JobStatus.OPEN -> Color(0xFF4CAF50)
            JobStatus.IN_PROGRESS -> Color(0xFF2196F3)
            JobStatus.WAITING_CONFIRMATION -> BrandOrange80
            JobStatus.COMPLETED -> Color.Gray
            JobStatus.CANCELLED -> Color(0xFFF44336)
        }
        Surface(
            color = statusColor.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = job.status.displayName,
                color = statusColor,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // ── Başlık ────────────────────────────────────────────────────────
        Text(
            text = job.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // ── Tarih & Konum ─────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val date = SimpleDateFormat("dd MMM yyyy", Locale("tr")).format(Date(job.createdAt))
            Text(
                text = "📅 $date",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            if (job.location.address.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Text(
                        text = job.location.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // ── İlan Veren ────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(job.postedByUserName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    if (isOwner) "Sizin İlanınız" else "İlan Sahibi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // ── AI Güven Analizi ──────────────────────────────────────────────
        if (!isOwner) {
            AiTrustSection(analysis = aiAnalysis, isLoading = isAiLoading)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }

        // ── Açıklama ──────────────────────────────────────────────────────
        Text("Açıklama", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = job.description, style = MaterialTheme.typography.bodyLarge, lineHeight = 24.sp)

        // ── Bütçe ────────────────────────────────────────────────────────
        job.budget?.let {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tahmini Bütçe", fontWeight = FontWeight.Medium)
                    Text(it, color = BrandOrange80, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Teklifler Bölümü ──────────────────────────────────────────────
        Text(
            text = if (isOwner) "Gelen Teklifler (${offers.size})" else "Diğer Teklifler (${offers.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (offers.isEmpty()) {
            Text(
                text = if (isOwner) "Henüz teklif gelmedi. Paylaşarak daha fazla usta çekin!"
                       else "Henüz teklif yok. İlk teklifinizi verin!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        } else {
            offers.forEach { offer ->
                if (isOwner) {
                    // Sahip görünümü: Kabul/Red butonları
                    OwnerOfferItem(
                        offer = offer,
                        onAccept = { onAcceptOffer(offer) },
                        onReject = { onRejectOffer(offer) }
                    )
                } else {
                    // Diğer kullanıcılar sadece görür
                    OfferItem(offer = offer)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Alt Butonlar (Dinamik) ─────────────────────────────────────────
        when {
            (isOwner || isAcceptedWorker) && (job.status == JobStatus.IN_PROGRESS || job.status == JobStatus.WAITING_CONFIRMATION) -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Mesajlaşma Butonu
                    Button(
                        onClick = onOpenChat,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Sohbete Git", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    // Dinamik Tamamlama Butonu
                    val buttonText = when {
                        isAcceptedWorker && job.status == JobStatus.IN_PROGRESS -> "İşi Bitirdim / Onaya Gönder"
                        isOwner && job.status == JobStatus.WAITING_CONFIRMATION -> "İşi Onayla ve Tamamla"
                        isOwner && job.status == JobStatus.IN_PROGRESS -> "Ustanın Bitirmesi Bekleniyor"
                        isAcceptedWorker && job.status == JobStatus.WAITING_CONFIRMATION -> "Onay Bekleniyor..."
                        else -> "İşi Tamamla"
                    }
                    
                    val isEnabled = (isAcceptedWorker && job.status == JobStatus.IN_PROGRESS) || 
                                    (isOwner && job.status == JobStatus.WAITING_CONFIRMATION)

                    Button(
                        onClick = onCompleteJob,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEnabled) Color(0xFF4CAF50) else Color.Gray.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading && isEnabled
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                if (isEnabled) Icons.Default.CheckCircle else Icons.Default.HourglassEmpty, 
                                null, 
                                tint = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(buttonText, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    if (isOwner && job.status == JobStatus.WAITING_CONFIRMATION) {
                        Text(
                            "Usta işi bitirdiğini bildirdi. Lütfen kontrol edip onaylayın.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            !isOwner && job.status == JobStatus.OPEN -> {
                // Teklif Ver butonu
                Button(
                    onClick = onOfferClick,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange80),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Teklif Ver", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            job.status == JobStatus.COMPLETED -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Tamamlanan ilan bilgi kutusu
                    Surface(
                        color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "İş Başarıyla Tamamlandı",
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Mesajlaşma Butonu (Tamamlansa bile geçmişe bakabilsinler)
                    if (isOwner || isAcceptedWorker) {
                        OutlinedButton(
                            onClick = onOpenChat,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Geçmiş Sohbeti Gör", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Değerlendirme Butonu
                    if (isAcceptedWorker) {
                        Button(
                            onClick = onReviewAsWorker,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandOrange80),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Star, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("İş Sahibini Değerlendir", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ─── Sahip için Teklif Kartı (Kabul/Red butonları ile) ────────────────────────
@Composable
private fun OwnerOfferItem(
    offer: Offer,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(offer.offeredByUserName, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "${offer.price} TL",
                    color = BrandOrange80,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(offer.description, style = MaterialTheme.typography.bodyMedium)

            // Kabul/Red sadece PENDING tekliflerde
            if (offer.status == OfferStatus.PENDING) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reddet")
                    }
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Kabul Et", color = Color.White)
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                val statusColor = if (offer.status == OfferStatus.ACCEPTED) Color(0xFF4CAF50) else Color(0xFFF44336)
                val statusText = if (offer.status == OfferStatus.ACCEPTED) "✅ Kabul Edildi" else "❌ Reddedildi"
                Text(statusText, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

// ─── Normal Teklif Kartı ───────────────────────────────────────────────────────
@Composable
fun OfferItem(offer: Offer) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(offer.offeredByUserName, fontWeight = FontWeight.Bold)
                }
                Text(text = "${offer.price} TL", color = BrandOrange80, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(8.dp))
            Text(offer.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ─── Teklif Ver Dialog ────────────────────────────────────────────────────────
@Composable
fun OfferSubmissionDialog(
    onDismiss: () -> Unit,
    onSubmit: (Double, String) -> Unit,
    isLoading: Boolean
) {
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Teklif Ver", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = price,
                    onValueChange = { if (it.all { char -> char.isDigit() }) price = it },
                    label = { Text("Fiyat (TL)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Örn: 300") }
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Açıklama / Mesaj") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    placeholder = { Text("Kendinizi kısaca tanıtın, ne zaman gelebilirsiniz?") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val priceDouble = price.toDoubleOrNull() ?: 0.0
                    onSubmit(priceDouble, description)
                },
                enabled = !isLoading && price.isNotBlank() && description.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange80)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Gönder", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}

@Composable
fun AiTrustSection(
    analysis: com.example.mahalleustasi.domain.model.AiTrustAnalysis?,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = BrandOrange80.copy(alpha = 0.05f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandOrange80.copy(alpha = 0.2f))
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

