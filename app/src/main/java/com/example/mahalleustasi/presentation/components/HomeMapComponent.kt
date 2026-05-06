package com.example.mahalleustasi.presentation.components

import android.location.Location
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mahalleustasi.domain.model.Job
import com.example.mahalleustasi.presentation.components.JobCard
import com.example.mahalleustasi.presentation.util.MapIconGenerator
import com.example.mahalleustasi.ui.theme.BrandOrange80
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun HomeMapComponent(
    jobs: List<Job>,
    currentUserId: String,
    userLocation: LatLng? = null,
    hasLocationPermission: Boolean = false,
    onJobClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val defaultLocation = LatLng(40.9901, 29.0292)
    val firstJobLocation = jobs.firstOrNull { it.location.lat != 0.0 }?.let {
        LatLng(it.location.lat, it.location.lng)
    } ?: defaultLocation

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(firstJobLocation, 13.5f)
    }

    var selectedJobs by remember { mutableStateOf<List<Job>>(emptyList()) }
    var isMapLoaded by remember { mutableStateOf(false) }
    var showMap by remember { mutableStateOf(false) }

    // Ana ipliği (UI thread) kilitlememek için harita bileşenini yüklemeden önce
    // bekleme ekranının en az bir frame çizilmesine izin ver.
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(50)
        showMap = true
    }

    // Kullanıcıya en yakın ilanları hesapla
    val nearbyJobs = remember(jobs, userLocation) {
        if (userLocation == null) emptyList<Pair<Job, Float>>()
        else {
            jobs.filter { it.location.lat != 0.0 }.map { job ->
                val results = FloatArray(1)
                Location.distanceBetween(
                    userLocation.latitude, userLocation.longitude,
                    job.location.lat, job.location.lng,
                    results
                )
                job to results[0]
            }.sortedBy { it.second }.take(5)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (showMap) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    mapToolbarEnabled = false,
                    compassEnabled = false
                ),
                properties = MapProperties(
                    isBuildingEnabled = false,
                    isIndoorEnabled = false,
                    isMyLocationEnabled = hasLocationPermission
                ),
                onMapLoaded = { isMapLoaded = true },
                onMapClick = { selectedJobs = emptyList() }
            ) {
                // İlanları lokasyonlarına göre grupla
                val jobsByLocation = jobs.filter { it.location.lat != 0.0 }
                    .groupBy { LatLng(it.location.lat, it.location.lng) }

                jobsByLocation.forEach { (position, jobsAtLocation) ->
                    
                    val hasMyJobOrAccepted = jobsAtLocation.any { it.postedByUserId == currentUserId || it.acceptedOfferId != null }
                    // Kategori ikonunu belirle (Grupta birden fazla varsa ilk ilanın kategorisi)
                    val categoryIcon = MapIconGenerator.getCategoryIcon(context, jobsAtLocation.first().category)

                    if (hasMyJobOrAccepted) {
                        Marker(
                            state = MarkerState(position = position),
                            title = if (jobsAtLocation.size > 1) "${jobsAtLocation.size} İlan" else jobsAtLocation.first().title,
                            snippet = "Tam Konum (Tıkla)",
                            icon = categoryIcon,
                            onClick = {
                                selectedJobs = jobsAtLocation
                                true
                            }
                        )
                    } else {
                        // Gizlilik Çemberi
                        Circle(
                            center = position,
                            radius = 200.0,
                            fillColor = Color(0x33FF9800), // Daha soft turuncu saydamlık
                            strokeColor = BrandOrange80,
                            strokeWidth = 3f,
                            clickable = true,
                            onClick = {
                                selectedJobs = jobsAtLocation
                            }
                        )
                        // Merkezde kategori ikonu (Radar efekti nokta yerine özel pin)
                        Marker(
                            state = MarkerState(position = position),
                            title = if (jobsAtLocation.size > 1) "${jobsAtLocation.size} İlan" else jobsAtLocation.first().title,
                            snippet = "Yaklaşık Konum",
                            icon = categoryIcon,
                            onClick = {
                                selectedJobs = jobsAtLocation
                                true
                            }
                        )
                    }
                }
            }
        }

        // Seçili ilan(lar) için alttan çıkan zarif kart veya kaydırılabilir liste
        AnimatedVisibility(
            visible = selectedJobs.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (selectedJobs.size == 1) {
                    JobCard(
                        job = selectedJobs.first(),
                        onClick = { onJobClick(selectedJobs.first().id) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )
                } else if (selectedJobs.size > 1) {
                    // Birden fazla ilan varsa HorizontalPager (veya Row) kullan
                    androidx.compose.foundation.pager.HorizontalPager(
                        state = androidx.compose.foundation.pager.rememberPagerState { selectedJobs.size },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        pageSpacing = 8.dp
                    ) { page ->
                        JobCard(
                            job = selectedJobs[page],
                            onClick = { onJobClick(selectedJobs[page].id) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Kapat butonu
                SmallFloatingActionButton(
                    onClick = { selectedJobs = emptyList() },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 16.dp, top = 8.dp)
                        .size(32.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat", modifier = Modifier.size(16.dp))
                }
            }
        }

        // Yakınımdaki İlanlar Önerisi (Seçili ilan yoksa ve konum varsa gösterilir)
        AnimatedVisibility(
            visible = selectedJobs.isEmpty() && nearbyJobs.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = BrandOrange80,
                    shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.NearMe, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Yakınındaki İlanlar",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(nearbyJobs) { (job, distanceMeters) ->
                        val distanceText = if (distanceMeters < 1000) {
                            "${distanceMeters.toInt()} m"
                        } else {
                            String.format("%.1f km", distanceMeters / 1000f)
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp,
                            onClick = { onJobClick(job.id) },
                            modifier = Modifier.width(260.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        job.category.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandOrange80,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        distanceText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    job.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Harita Yükleniyor Ekranı
        AnimatedVisibility(
            visible = !isMapLoaded,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = BrandOrange80)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Harita Hazırlanıyor...",
                        fontWeight = FontWeight.Bold,
                        color = BrandOrange80
                    )
                }
            }
        }
    }
}
