package com.example.mahalleustasi.presentation.screens.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
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
import com.example.mahalleustasi.presentation.components.HomeMapComponent
import com.example.mahalleustasi.presentation.components.JobCard
import com.example.mahalleustasi.ui.theme.BrandOrange80
import com.example.mahalleustasi.ui.theme.ForestGreen20
import com.example.mahalleustasi.ui.theme.ForestGreen40

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCreateJob: () -> Unit,
    onNavigateToProfile:   (String) -> Unit,
    onNavigateToJobDetail: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf<JobCategory?>(null) }
    var isMapView by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (hasLocationPermission) {
                viewModel.fetchUserLocation()
            }
        }
    )
    
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            // Bildirim izni durumu
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            viewModel.fetchUserLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick        = onNavigateToCreateJob,
                containerColor = BrandOrange80,
                contentColor   = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "İlan Ver")
            }
        }
    ) { innerPadding ->
        if (isMapView) {
            // HARİTA MODU: LazyColumn kullanılmaz, böylece harita kaydırmaları engellenmez.
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                HomeCategoryFilters(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
                HomeTitleAndToggle(
                    jobCount = uiState.jobs.size,
                    isMapView = isMapView,
                    onToggle = { isMapView = it }
                )
                
                val filtered = if (selectedCategory == null) uiState.jobs
                               else uiState.jobs.filter { it.category == selectedCategory }
                               
                HomeMapComponent(
                    jobs = filtered,
                    currentUserId = viewModel.currentUserId,
                    userLocation = uiState.userLocation,
                    hasLocationPermission = hasLocationPermission,
                    onJobClick = onNavigateToJobDetail,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // LİSTE MODU: Standart kaydırılabilir sayfa
            LazyColumn(
                modifier        = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding  = PaddingValues(bottom = 80.dp)
            ) {
                item { HomeHeaderBanner(onNavigateToProfile) }
                item { 
                    HomeCategoryFilters(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it }
                    ) 
                }
                item { 
                    HomeTitleAndToggle(
                        jobCount = uiState.jobs.size,
                        isMapView = isMapView,
                        onToggle = { isMapView = it }
                    ) 
                }

                if (uiState.jobs.isEmpty()) {
                    item {
                        EmptyJobsView(onCreateJob = onNavigateToCreateJob)
                    }
                } else {
                    val filtered = if (selectedCategory == null) uiState.jobs
                                   else uiState.jobs.filter { it.category == selectedCategory }
                    
                    items(filtered) { job ->
                        JobCard(
                            job     = job,
                            onClick = { onNavigateToJobDetail(job.id) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeaderBanner(onNavigateToProfile: (String) -> Unit) {
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
                    IconButton(onClick = { onNavigateToProfile("me") }) {
                        Icon(Icons.Default.Person, null, tint = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeCategoryFilters(
    selectedCategory: JobCategory?,
    onCategorySelected: (JobCategory?) -> Unit
) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick  = { onCategorySelected(null) },
                label    = { Text("Tümü") }
            )
        }
        items(JobCategory.entries) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick  = {
                    onCategorySelected(if (selectedCategory == category) null else category)
                },
                label = { Text(category.displayName) }
            )
        }
    }
}

@Composable
private fun HomeTitleAndToggle(
    jobCount: Int,
    isMapView: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text       = "Son İlanlar",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text  = "$jobCount ilan",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
        
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (isMapView) BrandOrange80 else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isMapView) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = { onToggle(!isMapView) }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isMapView) Icons.Default.List else Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isMapView) "Listeye Dön" else "Haritada Gör",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
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
