package com.example.mahalleustasi.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mahalleustasi.domain.model.JobCategory
import com.example.mahalleustasi.presentation.components.JobCard
import com.example.mahalleustasi.ui.theme.BrandOrange80
import com.example.mahalleustasi.ui.theme.ForestGreen20
import com.example.mahalleustasi.ui.theme.ForestGreen40

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToJobCreate: () -> Unit,
    onNavigateToOffers:    () -> Unit,
    onNavigateToProfile:   (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf<JobCategory?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick           = onNavigateToJobCreate,
                icon              = { Icon(Icons.Default.Add, contentDescription = "İlan Ver") },
                text              = { Text("İlan Ver") },
                containerColor    = BrandOrange80,
                contentColor      = Color.White
            )
        }
    ) { innerPadding ->

        LazyColumn(
            modifier        = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding  = PaddingValues(bottom = 80.dp)
        ) {

            // ── Header Banner ──────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(ForestGreen20, ForestGreen40)
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Column {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text       = "Merhaba 👋",
                                    style      = MaterialTheme.typography.bodyMedium,
                                    color      = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text       = "Mahalle Ustası",
                                    style      = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color      = Color.White
                                )
                            }
                            Row {
                                IconButton(onClick = onNavigateToOffers) {
                                    Icon(Icons.Default.Notifications, null, tint = Color.White)
                                }
                                IconButton(onClick = { onNavigateToProfile("me") }) {
                                    Icon(Icons.Default.Person, null, tint = Color.White)
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Arama alanı (placeholder)
                        Surface(
                            shape  = RoundedCornerShape(12.dp),
                            color  = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text     = "🔍  Ne arıyorsun?",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                color    = Color.White.copy(alpha = 0.8f),
                                style    = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // ── Kategori Filtreleri ────────────────────────────────────────
            item {
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick  = { selectedCategory = null },
                            label    = { Text("Tümü") }
                        )
                    }
                    items(JobCategory.entries) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick  = {
                                selectedCategory = if (selectedCategory == category) null else category
                            },
                            label = { Text(category.displayName) }
                        )
                    }
                }
            }

            // ── Başlık ────────────────────────────────────────────────────
            item {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text       = "Son İlanlar",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text  = "${uiState.jobs.size} ilan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }

            // ── İlan Listesi ──────────────────────────────────────────────
            if (uiState.jobs.isEmpty()) {
                item {
                    EmptyJobsView(onCreateJob = onNavigateToJobCreate)
                }
            } else {
                val filtered = if (selectedCategory == null) uiState.jobs
                               else uiState.jobs.filter { it.category == selectedCategory }
                items(filtered) { job ->
                    JobCard(
                        job     = job,
                        onClick = { /* JobDetail navigasyonu bir sonraki sprintte */ },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyJobsView(onCreateJob: () -> Unit) {
    Column(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.Center
    ) {
        Text(text = "🔧", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            text       = "Henüz ilan yok",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text  = "İlk ilanı veren sen ol!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick        = onCreateJob,
            colors         = ButtonDefaults.buttonColors(containerColor = BrandOrange80),
            shape          = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("İlan Ver", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
