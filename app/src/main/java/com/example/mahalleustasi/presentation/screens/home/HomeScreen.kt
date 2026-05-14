package com.example.mahalleustasi.presentation.screens.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mahalleustasi.domain.model.JobCategory
import com.example.mahalleustasi.presentation.components.HomeMapComponent
import com.example.mahalleustasi.presentation.components.JobCard
import com.example.mahalleustasi.ui.theme.BrandOrange80
import com.example.mahalleustasi.ui.theme.ForestGreen20
import com.example.mahalleustasi.ui.theme.ForestGreen40

private val CategoryEmojis = mapOf(
    JobCategory.REPAIR       to "🔧",
    JobCategory.CLEANING     to "🧹",
    JobCategory.MOVING       to "📦",
    JobCategory.TUTORING     to "📚",
    JobCategory.GARDENING    to "🌱",
    JobCategory.TECH_SUPPORT to "💻",
    JobCategory.OTHER        to "✨"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCreateJob: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToJobDetail: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isMapView by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocationPermission) viewModel.fetchUserLocation()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) viewModel.fetchUserLocation()
        else locationPermissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    // Filtre Bottom Sheet
    if (uiState.showFilterSheet) {
        FilterBottomSheet(
            filterState = uiState.filterState,
            onDismiss   = { viewModel.toggleFilterSheet() },
            onSortChange = { viewModel.setSortOrder(it) },
            onMaxBudgetChange = { viewModel.setMaxBudget(it) },
            onClearFilters = { viewModel.clearFilters() }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick        = onNavigateToCreateJob,
                containerColor = BrandOrange80,
                contentColor   = Color.White
            ) {
                Icon(Icons.Default.Add, "İlan Ver")
            }
        }
    ) { innerPadding ->
        if (isMapView) {
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                HomeCategoryFilters(
                    filter            = uiState.filterState,
                    onCategoryToggle  = { viewModel.toggleCategory(it) }
                )
                HomeTitleBar(
                    jobCount  = uiState.filteredJobs.size,
                    isMapView = isMapView,
                    hasFilter = uiState.filterState.selectedCategories.isNotEmpty() || uiState.filterState.maxBudget > 0,
                    onToggle  = { isMapView = it },
                    onFilter  = { viewModel.toggleFilterSheet() }
                )
                HomeMapComponent(
                    jobs                 = uiState.filteredJobs,
                    currentUserId        = viewModel.currentUserId,
                    userLocation         = uiState.userLocation,
                    hasLocationPermission = hasLocationPermission,
                    onJobClick           = onNavigateToJobDetail,
                    modifier             = Modifier.fillMaxSize()
                )
            }
        } else {
            LazyColumn(
                modifier       = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                item {
                    HomeHeaderBanner(
                        userName          = uiState.currentUserName,
                        searchQuery       = uiState.filterState.searchQuery,
                        onSearchChange    = { viewModel.setSearchQuery(it) },
                        onProfileClick    = { onNavigateToProfile("me") },
                        onSearchSubmit    = { focusManager.clearFocus() }
                    )
                }
                item {
                    HomeCategoryFilters(
                        filter           = uiState.filterState,
                        onCategoryToggle = { viewModel.toggleCategory(it) }
                    )
                }
                item {
                    HomeTitleBar(
                        jobCount  = uiState.filteredJobs.size,
                        isMapView = isMapView,
                        hasFilter = uiState.filterState.selectedCategories.isNotEmpty() || uiState.filterState.maxBudget > 0,
                        onToggle  = { isMapView = it },
                        onFilter  = { viewModel.toggleFilterSheet() }
                    )
                }

                if (uiState.isLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = BrandOrange80)
                        }
                    }
                } else if (uiState.filteredJobs.isEmpty()) {
                    item { EmptyJobsView(onCreateJob = onNavigateToCreateJob) }
                } else {
                    items(uiState.filteredJobs) { job ->
                        JobCard(
                            job      = job,
                            onClick  = { onNavigateToJobDetail(job.id) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Premium Header Banner ─────────────────────────────────────────────────────
@Composable
private fun HomeHeaderBanner(
    userName: String,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onProfileClick: () -> Unit,
    onSearchSubmit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(Color(0xFF1B5E20), Color(0xFF2E7D32), Color(0xFF388E3C)))
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
                        text  = if (userName.isNotBlank()) "Merhaba, $userName 👋" else "Merhaba 👋",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Text(
                        text       = "Mahalle Ustası",
                        style      = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color.White
                    )
                }
                // Avatar butonu
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable(onClick = onProfileClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Arama Kutusu
            TextField(
                value         = searchQuery,
                onValueChange = onSearchChange,
                placeholder   = { Text("İlan ara...", color = Color.White.copy(alpha = 0.6f)) },
                leadingIcon   = { Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.7f)) },
                trailingIcon  = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.7f))
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor   = Color.White.copy(alpha = 0.2f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.15f),
                    focusedTextColor        = Color.White,
                    unfocusedTextColor      = Color.White,
                    cursorColor             = Color.White,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape              = RoundedCornerShape(14.dp),
                modifier           = Modifier.fillMaxWidth(),
                singleLine         = true,
                keyboardOptions    = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions    = KeyboardActions(onSearch = { onSearchSubmit() })
            )
        }
    }
}

// ─── Kategori Filtreleri ──────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeCategoryFilters(
    filter: HomeFilterState,
    onCategoryToggle: (JobCategory) -> Unit
) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "Tümü" chip
        item {
            FilterChip(
                selected = filter.selectedCategories.isEmpty(),
                onClick  = { /* No-op, clear is via filter button */ },
                label    = { Text("Tümü") }
            )
        }
        items(JobCategory.entries) { category ->
            val emoji = CategoryEmojis[category] ?: "•"
            FilterChip(
                selected = category in filter.selectedCategories,
                onClick  = { onCategoryToggle(category) },
                label    = { Text("$emoji ${category.displayName}") }
            )
        }
    }
}

// ─── Başlık + Harita/Filtre Butonları ─────────────────────────────────────────
@Composable
private fun HomeTitleBar(
    jobCount: Int,
    isMapView: Boolean,
    hasFilter: Boolean,
    onToggle: (Boolean) -> Unit,
    onFilter: () -> Unit
) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column {
            Text("Son İlanlar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "$jobCount ilan",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Filtre butonu
            BadgedBox(badge = {
                if (hasFilter) Badge(containerColor = BrandOrange80)
            }) {
                FilledTonalIconButton(
                    onClick = onFilter,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.FilterList, "Filtrele", modifier = Modifier.size(18.dp))
                }
            }
            // Harita/Liste toggle
            Surface(
                shape        = RoundedCornerShape(24.dp),
                color        = if (isMapView) BrandOrange80 else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isMapView) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick      = { onToggle(!isMapView)  }
            ) {
                Row(
                    modifier          = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isMapView) Icons.Default.List else Icons.Default.Map,
                        null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isMapView) "Listeye Dön" else "Harita",
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─── Boş İlan Durumu ──────────────────────────────────────────────────────────
@Composable
private fun EmptyJobsView(onCreateJob: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🔧", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(16.dp))
        Text("Henüz ilan yok", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "İlk ilanı veren sen ol!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onCreateJob,
            colors  = ButtonDefaults.buttonColors(containerColor = BrandOrange80),
            shape   = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("İlan Ver", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Filtre Bottom Sheet ───────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    filterState: HomeFilterState,
    onDismiss: () -> Unit,
    onSortChange: (SortOrder) -> Unit,
    onMaxBudgetChange: (Int) -> Unit,
    onClearFilters: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var budgetSlider by remember { mutableFloatStateOf(filterState.maxBudget.toFloat()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Filtrele & Sırala", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                TextButton(onClick = {
                    budgetSlider = 0f
                    onClearFilters()
                }) { Text("Temizle", color = MaterialTheme.colorScheme.error) }
            }

            Spacer(Modifier.height(20.dp))

            // Sıralama
            Text("Sıralama", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            SortOrder.entries.forEach { order ->
                Row(
                    modifier          = Modifier.fillMaxWidth().clickable { onSortChange(order) }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = filterState.sortOrder == order,
                        onClick  = { onSortChange(order) },
                        colors   = RadioButtonDefaults.colors(selectedColor = BrandOrange80)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(order.label, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // Maksimum Bütçe
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Maksimum Bütçe", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    if (budgetSlider == 0f) "Sınırsız" else "${budgetSlider.toInt()} ₺",
                    color = BrandOrange80,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            Slider(
                value         = budgetSlider,
                onValueChange = { budgetSlider = it },
                onValueChangeFinished = { onMaxBudgetChange(budgetSlider.toInt()) },
                valueRange    = 0f..10000f,
                steps         = 19,
                colors        = SliderDefaults.colors(thumbColor = BrandOrange80, activeTrackColor = BrandOrange80)
            )

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = BrandOrange80),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Text("Uygula", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
