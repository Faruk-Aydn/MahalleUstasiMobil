package com.example.mahalleustasi.presentation.screens.job

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mahalleustasi.domain.model.JobCategory
import com.example.mahalleustasi.ui.theme.BrandOrange80

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobCreateScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAiAssistant: () -> Unit,
    viewModel: JobCreateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            // İzinler verilse de verilmese de ilanı yayınlamaya devam et.
            // Konum alınamazsa varsayılan 0.0 atılacak.
        }
    )

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            Toast.makeText(context, "İlan başarıyla oluşturuldu!", Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
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
                title = { Text("Yeni İlan Ver", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Hata",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // ── AI Assistant Card ──────────────────────────────────────────
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
                        color = MaterialTheme.colorScheme.primary,
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
                            text = "Arızanın fotoğrafını çek, bilgiler dolsun!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = "Veya bilgileri manuel girin",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 8.dp)
            )

            // ── Image Preview and Picker ──────────────────────────────────
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
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
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
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Fotoğraf Ekle",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "(Galeriden Seç)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Başlık") },
                placeholder = { Text("Örn: Mutfak Musluğu Tamiri") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Detaylı Açıklama") },
                placeholder = { Text("Ne yapılması gerekiyor?") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 5
            )

            // Kategori Seçimi (Basit Dropdown mantığı veya Segmented Buttons olabilir, burada DropdownMenu kullanacağız)
            var expandedCategory by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedCategory,
                onExpandedChange = { expandedCategory = !expandedCategory }
            ) {
                OutlinedTextField(
                    value = uiState.category.displayName,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Kategori") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory)
                    },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedCategory,
                    onDismissRequest = { expandedCategory = false }
                ) {
                    JobCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.displayName) },
                            onClick = {
                                viewModel.onCategoryChange(category)
                                expandedCategory = false
                            }
                        )
                    }
                }
            }

            var showAddressSheet by remember { mutableStateOf(false) }

            OutlinedTextField(
                value = if (uiState.useCurrentLocation) "📍 Şu Anki Konumum" else uiState.address,
                onValueChange = { },
                readOnly = true,
                label = { Text("Konum/Adres Detayı") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { showAddressSheet = true }) {
                        Icon(Icons.Default.ArrowDropDown, "Adres Seç")
                    }
                },
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    .also { interactionSource ->
                        LaunchedEffect(interactionSource) {
                            interactionSource.interactions.collect {
                                if (it is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                    showAddressSheet = true
                                }
                            }
                        }
                    }
            )

            if (showAddressSheet) {
                AddressSelectionSheet(
                    savedAddresses = uiState.savedAddresses,
                    onDismiss = { showAddressSheet = false },
                    onSelectCurrentLocation = {
                        viewModel.onUseCurrentLocationToggle(true)
                        showAddressSheet = false
                    },
                    onSelectSavedAddress = { savedAddress ->
                        viewModel.onUseCurrentLocationToggle(false)
                        viewModel.onAddressChange(savedAddress.addressText)
                        showAddressSheet = false
                    },
                    onAddNewAddress = { title, addressText ->
                        viewModel.saveNewAddress(title, addressText)
                        showAddressSheet = false
                    }
                )
            }

            OutlinedTextField(
                value = uiState.budget,
                onValueChange = viewModel::onBudgetChange,
                label = { Text("Tahmini Bütçe (Opsiyonel)") },
                placeholder = { Text("Örn: Tekliflere Açık veya Maks. 500 TL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = viewModel::submitJob,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange80),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("İlanı Yayınla", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressSelectionSheet(
    savedAddresses: List<com.example.mahalleustasi.domain.model.SavedAddress>,
    onDismiss: () -> Unit,
    onSelectCurrentLocation: () -> Unit,
    onSelectSavedAddress: (com.example.mahalleustasi.domain.model.SavedAddress) -> Unit,
    onAddNewAddress: (String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAddDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Adres Seçimi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Şu Anki Konum Seçeneği
            ListItem(
                headlineContent = { Text("Şu Anki Konumum", fontWeight = FontWeight.Bold) },
                supportingContent = { Text("GPS üzerinden anlık olarak bulunur") },
                leadingContent = {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.padding(8.dp))
                    }
                },
                modifier = Modifier.clickable { onSelectCurrentLocation() }
            )
            
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            
            Text(
                text = "Kayıtlı Adreslerim",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (savedAddresses.isEmpty()) {
                Text(
                    text = "Henüz kayıtlı bir adresiniz bulunmuyor.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                savedAddresses.forEach { address ->
                    ListItem(
                        headlineContent = { Text(address.title, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text(address.addressText) },
                        leadingContent = {
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.padding(8.dp))
                            }
                        },
                        modifier = Modifier.clickable { onSelectSavedAddress(address) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Yeni Adres Ekleme Butonu
            OutlinedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Yeni Adres Ekle")
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var addressText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Yeni Adres Ekle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Adres Başlığı") },
                        placeholder = { Text("Örn: Ev, Annemin Evi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = addressText,
                        onValueChange = { addressText = it },
                        label = { Text("Açık Adres") },
                        placeholder = { Text("Örn: Kadıköy, Rıhtım Sk. No: 4") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank() && addressText.isNotBlank()) {
                            onAddNewAddress(title, addressText)
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Kaydet ve Seç")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
}
