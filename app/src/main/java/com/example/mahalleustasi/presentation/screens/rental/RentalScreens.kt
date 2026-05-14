package com.example.mahalleustasi.presentation.screens.rental

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mahalleustasi.domain.model.Rental
import com.example.mahalleustasi.domain.model.RentalCategory
import com.example.mahalleustasi.ui.theme.BrandOrange80
import com.example.mahalleustasi.ui.theme.ForestGreen20
import com.example.mahalleustasi.ui.theme.ForestGreen40
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable

// ═══════════════════════════════════════════════════════════════════════════════
// RENTAL LIST SCREEN
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalListScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    viewModel: RentalListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick          = onNavigateToCreate,
                containerColor   = BrandOrange80,
                contentColor     = Color.White,
                icon             = { Icon(Icons.Default.Add, null) },
                text             = { Text("Eşya Ekle", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(Color(0xFF4A148C), Color(0xFF7B1FA2), Color(0xFF9C27B0))))
                        .padding(20.dp)
                ) {
                    Column {
                        Text("📦 Eşya Kiralama", style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("Komşularına kiralık, komşularından kirala",
                            style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                        Spacer(Modifier.height(12.dp))
                        // Arama
                        TextField(
                            value         = uiState.searchQuery,
                            onValueChange = { viewModel.setSearch(it) },
                            placeholder   = { Text("Ne arıyorsun?", color = Color.White.copy(alpha = 0.6f)) },
                            leadingIcon   = { Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.7f)) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor   = Color.White.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.15f),
                                focusedTextColor        = Color.White,
                                unfocusedTextColor      = Color.White,
                                cursorColor             = Color.White,
                                focusedIndicatorColor   = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape    = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            // Kategori filtreleri
            item {
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedCategory == null,
                            onClick  = { viewModel.setCategory(null) },
                            label    = { Text("Tümü") }
                        )
                    }
                    items(RentalCategory.entries) { cat ->
                        FilterChip(
                            selected = uiState.selectedCategory == cat,
                            onClick  = { viewModel.setCategory(if (uiState.selectedCategory == cat) null else cat) },
                            label    = { Text("${cat.emoji} ${cat.displayName}") }
                        )
                    }
                }
            }

            // Stats
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${uiState.filteredRentals.size} eşya bulundu",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (uiState.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandOrange80)
                    }
                }
            } else if (uiState.filteredRentals.isEmpty()) {
                item {
                    Column(
                        modifier            = Modifier.fillMaxWidth().padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📭", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Henüz kiralık eşya yok", fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text("İlk ekleyen sen ol!", color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                items(uiState.filteredRentals) { rental ->
                    RentalCard(
                        rental   = rental,
                        onClick  = { onNavigateToDetail(rental.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

// ─── Rental Kartı ─────────────────────────────────────────────────────────────
@Composable
private fun RentalCard(rental: Rental, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick   = onClick,
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (rental.photoUrls.isNotEmpty()) {
                AsyncImage(
                    model = rental.photoUrls.first(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(16.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // Kategori chip
                    Surface(
                        color = Color(0xFF9C27B0).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "${rental.category.emoji} ${rental.category.displayName}",
                            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style      = MaterialTheme.typography.labelSmall,
                            color      = Color(0xFF7B1FA2),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    // Müsaitlik
                    Surface(
                        color = if (rental.isAvailable) Color(0xFF4CAF50).copy(0.12f) else Color.Gray.copy(0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            if (rental.isAvailable) "✓ Müsait" else "✗ Dolu",
                            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style      = MaterialTheme.typography.labelSmall,
                            color      = if (rental.isAvailable) Color(0xFF4CAF50) else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(rental.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(rental.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("👤 ${rental.ownerName}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${rental.dailyPrice.toLong()} ₺/gün",
                            color      = BrandOrange80,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 18.sp
                        )
                        if (rental.depositAmount > 0) {
                            Text("Depozito: ${rental.depositAmount.toLong()} ₺",
                                style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// RENTAL DETAIL SCREEN
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: RentalDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val rental = uiState.rental

    Scaffold(
        topBar = {
            TopAppBar(
                title   = { Text(rental?.title ?: "Yükleniyor...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = Color(0xFF7B1FA2),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading || rental == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandOrange80)
            }
        } else {
            LazyColumn(
                modifier       = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Fotoğraf
                if (rental.photoUrls.isNotEmpty()) {
                    item {
                        AsyncImage(
                            model = rental.photoUrls.first(),
                            contentDescription = "Eşya Fotoğrafı",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Kategori + Durum
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = Color(0xFF9C27B0).copy(0.12f), shape = RoundedCornerShape(8.dp)) {
                            Text("${rental.category.emoji} ${rental.category.displayName}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                color = Color(0xFF7B1FA2), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        Surface(
                            color = if (rental.isAvailable) Color(0xFF4CAF50).copy(0.12f) else Color.Gray.copy(0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                if (rental.isAvailable) "✓ Müsait" else "✗ Şu An Dolu",
                                modifier   = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                color      = if (rental.isAvailable) Color(0xFF4CAF50) else Color.Gray,
                                style      = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Başlık
                item {
                    Text(rental.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                }

                // Fiyat Kartı
                item {
                    Card(
                        shape  = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = BrandOrange80.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier              = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Günlük Ücret", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("${rental.dailyPrice.toLong()} ₺",
                                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = BrandOrange80)
                            }
                            if (rental.depositAmount > 0) {
                                VerticalDivider(modifier = Modifier.height(40.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Depozito", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text("${rental.depositAmount.toLong()} ₺",
                                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                // Açıklama
                item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Açıklama", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(rental.description, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                        }
                    }
                }

                // İlan Sahibi
                item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(Color(0xFF9C27B0).copy(0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    rental.ownerName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    color = Color(0xFF7B1FA2), fontWeight = FontWeight.Bold, fontSize = 18.sp
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("İlan Sahibi", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(rental.ownerName, fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }

                // Sahip kontrolleri
                if (uiState.isOwner) {
                    item {
                        HorizontalDivider()
                        Spacer(Modifier.height(4.dp))
                        Text("Sahip Kontrolleri", style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick  = { viewModel.toggleAvailability() },
                                modifier = Modifier.weight(1f),
                                shape    = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    if (rental.isAvailable) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    null, modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(if (rental.isAvailable) "Pasife Al" else "Aktife Al")
                            }
                        }
                    }
                }

                // Mesaj butonu (sahip değilse)
                if (!uiState.isOwner) {
                    item {
                        Button(
                            onClick  = { /* TODO: Kiralama talebi */ },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            enabled  = rental.isAvailable,
                            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                            shape    = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Send, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (rental.isAvailable) "Kiralama Talebi Gönder" else "Şu An Müsait Değil",
                                color = Color.White, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// RENTAL CREATE SCREEN
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalCreateScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAiAssistant: () -> Unit = {},
    viewModel: RentalCreateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var categoryExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onNavigateBack()
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let { viewModel.onImageSelected(it.toString()) }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title          = { Text("Kiralık Eşya Ekle") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = Color(0xFF7B1FA2),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── AI Assistant Card ──────────────────────────────────────────
            item {
                Surface(
                    onClick = onNavigateToAiAssistant,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF7B1FA2),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("✨", style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI ile Otomatik Doldur",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Eşyanın fotoğrafını çekin, AI başlık, açıklama, fiyat ve kategoriyi belirlesin.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // ── Image Preview and Picker ──────────────────────────────────
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .clickable { imagePickerLauncher.launch("image/*") },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    if (uiState.imageUri != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = uiState.imageUri,
                                contentDescription = "Seçilen Fotoğraf",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { viewModel.onImageSelected(null) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
                                    .size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = Color(0xFF7B1FA2),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Fotoğraf Ekle",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF7B1FA2)
                            )
                            Text(
                                "(Galeriden Seç)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Başlık
            item {
                OutlinedTextField(
                    value         = uiState.title,
                    onValueChange = { viewModel.onTitleChange(it) },
                    label         = { Text("Eşya Başlığı *") },
                    placeholder   = { Text("Örn: Matkap, Bisiklet, Kamera...") },
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp),
                    leadingIcon   = { Icon(Icons.Default.Title, null) },
                    singleLine    = true
                )
            }

            // Kategori Seçimi
            item {
                ExposedDropdownMenuBox(
                    expanded        = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value         = "${uiState.category.emoji} ${uiState.category.displayName}",
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Kategori *") },
                        modifier      = Modifier.fillMaxWidth().menuAnchor(),
                        shape         = RoundedCornerShape(12.dp),
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded       = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        RentalCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text    = { Text("${cat.emoji} ${cat.displayName}") },
                                onClick = {
                                    viewModel.onCategoryChange(cat)
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Günlük Fiyat
            item {
                OutlinedTextField(
                    value         = uiState.dailyPrice,
                    onValueChange = { viewModel.onDailyPriceChange(it) },
                    label         = { Text("Günlük Kiralama Ücreti (₺) *") },
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp),
                    leadingIcon   = { Text("₺", modifier = Modifier.padding(start = 12.dp), fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine    = true
                )
            }

            // Depozito
            item {
                OutlinedTextField(
                    value         = uiState.depositAmount,
                    onValueChange = { viewModel.onDepositChange(it) },
                    label         = { Text("Depozito Miktarı (₺) (İsteğe Bağlı)") },
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp),
                    leadingIcon   = { Icon(Icons.Default.Security, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine    = true
                )
            }

            // Açıklama
            item {
                OutlinedTextField(
                    value         = uiState.description,
                    onValueChange = { viewModel.onDescriptionChange(it) },
                    label         = { Text("Açıklama") },
                    placeholder   = { Text("Eşyanın durumu, özellikleri ve kullanım koşullarını yazın...") },
                    modifier      = Modifier.fillMaxWidth().height(140.dp),
                    shape         = RoundedCornerShape(12.dp),
                    maxLines      = 6
                )
            }

            // Hata mesajı
            if (uiState.error != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            uiState.error!!,
                            modifier = Modifier.padding(12.dp),
                            color    = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Submit
            item {
                Button(
                    onClick  = { viewModel.submit() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled  = !uiState.isLoading,
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Add, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("İlan Yayınla", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
