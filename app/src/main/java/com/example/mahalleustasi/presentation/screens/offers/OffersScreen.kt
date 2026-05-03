package com.example.mahalleustasi.presentation.screens.offers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    viewModel: OffersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tekliflerim", fontWeight = FontWeight.Bold) }
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
                uiState.error != null -> {
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                }
                uiState.myOffers.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📉", fontSize = 48.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("Henüz bir teklif vermediniz.", color = Color.Gray)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.myOffers) { offer ->
                            MyOfferItem(offer, onClick = { onNavigateToJobDetail(offer.jobId) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MyOfferItem(offer: Offer, onClick: () -> Unit) {
    Card(
        onClick = onClick,
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
                val date = SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date(offer.createdAt))
                Text(date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                
                StatusChip(offer.status)
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = "İlan ID: ${offer.jobId.take(8)}...", 
                fontWeight = FontWeight.Bold,
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

@Composable
fun StatusChip(status: OfferStatus) {
    val color = when (status) {
        OfferStatus.PENDING -> Color(0xFFFFA000)
        OfferStatus.ACCEPTED -> Color(0xFF4CAF50)
        OfferStatus.REJECTED -> Color(0xFFF44336)
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = status.displayName,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
