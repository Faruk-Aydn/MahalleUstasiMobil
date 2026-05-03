package com.example.mahalleustasi.presentation.screens.job

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mahalleustasi.domain.model.Job
import com.example.mahalleustasi.ui.theme.BrandOrange80
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: JobDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showOfferDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(uiState.offerSuccess) {
        if (uiState.offerSuccess) {
            Toast.makeText(context, "Teklifiniz başarıyla gönderildi!", Toast.LENGTH_SHORT).show()
            showOfferDialog = false
            viewModel.resetOfferSuccess()
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
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null && uiState.job == null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.error)
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                        Button(onClick = viewModel::loadJobDetail) {
                            Text("Tekrar Dene")
                        }
                    }
                }
                uiState.job != null -> {
                    JobDetailContent(
                        job = uiState.job!!,
                        offers = uiState.offers,
                        onOfferClick = { showOfferDialog = true }
                    )
                }
            }

            if (showOfferDialog) {
                OfferSubmissionDialog(
                    onDismiss = { showOfferDialog = false },
                    onSubmit = { price, desc -> viewModel.submitOffer(price, desc) },
                    isLoading = uiState.isOfferLoading
                )
            }
        }
    }
}

@Composable
private fun JobDetailContent(
    job: Job,
    offers: List<Offer>,
    onOfferClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ... (Kategori, Başlık, Tarih kısımları aynı kalıyor, sadece içerik metoduna parametre ekledik)
        
        // Kategori Chip
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

        // Başlık
        Text(
            text = job.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Tarih ve Lokasyon
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
            job.location?.address?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp), alpha = 0.1f)

        // İlan Veren Bilgisi
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
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
                Text(
                    text = job.postedByUserName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "İlan Sahibi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp), alpha = 0.1f)

        // Açıklama
        Text(
            text = "Açıklama",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = job.description,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 24.sp
        )

        // Bütçe
        job.budget?.let {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tahmini Bütçe", fontWeight = FontWeight.Medium)
                    Text(
                        text = it,
                        color = BrandOrange80,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Teklifler Bölümü
        Text(
            text = "Gelen Teklifler (${offers.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (offers.isEmpty()) {
            Text(
                text = "Henüz teklif gelmedi. İlk teklifi sen ver!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        } else {
            offers.forEach { offer ->
                OfferItem(offer)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Teklif Ver Butonu
        Button(
            onClick = onOfferClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandOrange80),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Teklif Ver", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

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
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(offer.offeredByUserName, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "${offer.price} TL",
                    color = BrandOrange80,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(offer.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

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
                    placeholder = { Text("Ustanıza ne söylemek istersiniz?") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val priceDouble = price.toDoubleOrNull() ?: 0.0
                    onSubmit(priceDouble, description)
                },
                enabled = !isLoading && price.isNotBlank() && description.isNotBlank()
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Gönder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

