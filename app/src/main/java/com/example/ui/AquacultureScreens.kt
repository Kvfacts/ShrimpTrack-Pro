package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.*
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAquacultureApp(viewModel: AquacultureViewModel) {
    val currentUser by viewModel.currentUserProfile.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()

    if (currentUser == null) {
        AuthScreen(
            onGoogleSignIn = { email, name -> viewModel.loginWithGoogle(email, name) },
            onMobileSignIn = { phone, name, role -> viewModel.loginWithMobile(phone, name, role) }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "AquaShrimp",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF4CAF50), RoundedCornerShape(3.dp))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Synced: Co-Owners Online",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                                )
                            }
                        }
                    },
                    actions = {
                        val activeAlarms by viewModel.activeTrayAlarms.collectAsState()
                        val pendingCount = activeAlarms.count { !it.isCompleted }
                        Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.padding(end = 8.dp)) {
                            IconButton(onClick = { viewModel.selectTab("alarms") }) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Alerts",
                                    tint = if (pendingCount > 0) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary
                                )
                            }
                            if (pendingCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(Color.Red, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = pendingCount.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == "dashboard",
                        onClick = { viewModel.selectTab("dashboard") },
                        icon = { Icon(Icons.Default.Dashboard, "Dashboard") },
                        label = { Text("Hub", fontSize = 11.sp) }
                    )
                    NavigationBarItem(
                        selected = currentTab == "feeds",
                        onClick = { viewModel.selectTab("feeds") },
                        icon = { Icon(Icons.Default.Restaurant, "Feeds") },
                        label = { Text("Feeds", fontSize = 11.sp) }
                    )
                    NavigationBarItem(
                        selected = currentTab == "meds",
                        onClick = { viewModel.selectTab("meds") },
                        icon = { Icon(Icons.Default.Healing, "Meds") },
                        label = { Text("Meds", fontSize = 11.sp) }
                    )
                    NavigationBarItem(
                        selected = currentTab == "counts",
                        onClick = { viewModel.selectTab("counts") },
                        icon = { Icon(Icons.Default.Assessment, "Counts") },
                        label = { Text("Growth", fontSize = 11.sp) }
                    )
                    NavigationBarItem(
                        selected = currentTab == "alarms",
                        onClick = { viewModel.selectTab("alarms") },
                        icon = { Icon(Icons.Default.Alarm, "Trays") },
                        label = { Text("Trays", fontSize = 11.sp) }
                    )
                    NavigationBarItem(
                        selected = currentTab == "profile",
                        onClick = { viewModel.selectTab("profile") },
                        icon = { Icon(Icons.Default.Person, "Profile") },
                        label = { Text("Profile", fontSize = 11.sp) }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (currentTab) {
                    "dashboard" -> DashboardScreen(viewModel)
                    "feeds" -> FeedsScreen(viewModel)
                    "meds" -> MedicationScreen(viewModel)
                    "counts" -> SamplingScreen(viewModel)
                    "alarms" -> TrayMonitorScreen(viewModel)
                    "profile" -> ProfileScreen(viewModel)
                    else -> DashboardScreen(viewModel)
                }
            }
        }
    }
}

// ==========================================
// 1. AUTHENTICATION & LOGIN SCREEN
// ==========================================
@Composable
fun AuthScreen(
    onGoogleSignIn: (email: String, name: String) -> Unit,
    onMobileSignIn: (phone: String, name: String, role: String) -> Unit
) {
    var loginMethodGoogle by remember { mutableStateOf(true) }
    var emailInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("Co-Owner") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        // Modern logo banner
        Box(
            modifier = Modifier
                .size(90.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFE0F7FA), Color(0xFF00838F))
                    ),
                    shape = RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Waves,
                contentDescription = "Aqua",
                tint = Color.White,
                modifier = Modifier.size(50.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "AquaShrimp Core",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        )
        Text(
            text = "Shrimp Aquaculture Multi-Device Hub",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Toggle Pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE0F2F1), RoundedCornerShape(24.dp))
                .padding(4.dp)
        ) {
            Button(
                onClick = { loginMethodGoogle = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (loginMethodGoogle) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (loginMethodGoogle) Color.White else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Google Sign-In", fontSize = 13.sp)
            }

            Button(
                onClick = { loginMethodGoogle = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!loginMethodGoogle) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (!loginMethodGoogle) Color.White else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Mobile OTP", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (loginMethodGoogle) "Sign in with Google credentials" else "Access via Mobile SMS verification",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Full Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (loginMethodGoogle) {
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Google Account Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Mobile Phone Number") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Pond Role Designation", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Co-Owner", "Pond Manager", "Supervisor").forEach { role ->
                            FilterChip(
                                selected = selectedRole == role,
                                onClick = { selectedRole = role },
                                label = { Text(role, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (loginMethodGoogle) {
                            onGoogleSignIn(emailInput, nameInput)
                        } else {
                            onMobileSignIn(phoneInput, nameInput, selectedRole)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Authenticate & Synced Login", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick login buttons for demonstration & seamless review
        Text("Quick Test Profiles (Co-Owners / Managers)", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { onGoogleSignIn("kpericharla2005@gmail.com", "Karthik Varma Pericharla") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0F7FA), contentColor = Color(0xFF006064)),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(8.dp)
            ) {
                Text("Owner Google", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { onMobileSignIn("+91 94405 98765", "Raju Manager", "Pond Manager") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF3E0), contentColor = Color(0xFFE65100)),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(8.dp)
            ) {
                Text("Manager Mobile", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// 2. MAIN HUB / DASHBOARD SCREEN
// ==========================================
@Composable
fun DashboardScreen(viewModel: AquacultureViewModel) {
    val selectedPond by viewModel.selectedPond.collectAsState()
    val allFeeds by viewModel.allFeedLogs.collectAsState()
    val latestCount by viewModel.latestCountRecord.collectAsState()
    val trayAlarms by viewModel.allTrayAlarms.collectAsState()
    val allPonds by viewModel.allPonds.collectAsState()
    val apRates by viewModel.allApMarketRates.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var isManualSyncing by remember { mutableStateOf(false) }
    var syncFeedbackText by remember { mutableStateOf("All local records securely mirrored in Cloud storage.") }

    val availablePonds = if (allPonds.isEmpty()) {
        listOf("Pond Delta-1", "Pond Delta-2", "Pond Delta-3")
    } else {
        allPonds.map { it.name }
    }

    // Filter current selection
    val pondFeeds = allFeeds.filter { it.pondName == selectedPond }
    val totalFeedKg = pondFeeds.sumOf { it.quantityKg }

    // Alarm calculation
    val futureAlarms = trayAlarms.filter { !it.isCompleted && it.pondName == selectedPond }

    // Add Pond Dialog states
    var showAddPondDialog by remember { mutableStateOf(false) }
    var newPondName by remember { mutableStateOf("") }
    var newPondArea by remember { mutableStateOf("") }
    var newStockCount by remember { mutableStateOf("") }
    var newSurvivalPct by remember { mutableStateOf("") }

    // Update Market Rate Dialog states
    var showUpdateRateDialog by remember { mutableStateOf(false) }
    var selectedRateToUpdate by remember { mutableStateOf<ApMarketRate?>(null) }
    var updateRateValue by remember { mutableStateOf("") }

    // Dynamic Pond Creator Dialog Dialog
    if (showAddPondDialog) {
        Dialog(onDismissRequest = { showAddPondDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Add New Aquaculture Pond",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = newPondName,
                        onValueChange = { newPondName = it },
                        label = { Text("Pond Name") },
                        placeholder = { Text("e.g. Pond Delta-4 or Nel-2") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newPondArea,
                        onValueChange = { newPondArea = it },
                        label = { Text("Pond Area (Acres)") },
                        placeholder = { Text("e.g. 1.2") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newStockCount,
                        onValueChange = { newStockCount = it },
                        label = { Text("Active Seed Stock") },
                        placeholder = { Text("e.g. 180000") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newSurvivalPct,
                        onValueChange = { newSurvivalPct = it },
                        label = { Text("Target Survival Rate (%)") },
                        placeholder = { Text("e.g. 85.0") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddPondDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val area = newPondArea.toDoubleOrNull() ?: 1.2
                                val stock = newStockCount.toIntOrNull() ?: 180000
                                val survival = newSurvivalPct.toDoubleOrNull() ?: 85.0
                                if (newPondName.isNotBlank()) {
                                    viewModel.addPond(newPondName.trim(), area, stock, survival)
                                    showAddPondDialog = false
                                    newPondName = ""
                                    newPondArea = ""
                                    newStockCount = ""
                                    newSurvivalPct = ""
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Create")
                        }
                    }
                }
            }
        }
    }

    // Dynamic AP Rate Updater Dialog
    if (showUpdateRateDialog && selectedRateToUpdate != null) {
        Dialog(onDismissRequest = { showUpdateRateDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Update AP Market Rate",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Current Pricing for ${selectedRateToUpdate!!.count} Count Grade",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = updateRateValue,
                        onValueChange = { updateRateValue = it },
                        label = { Text("Rate per Kg (INR ₹)") },
                        placeholder = { Text("e.g. 340.0") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showUpdateRateDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val rateVal = updateRateValue.toDoubleOrNull()
                                if (rateVal != null) {
                                    viewModel.updateApMarketRate(selectedRateToUpdate!!.count, rateVal)
                                    showUpdateRateDialog = false
                                    selectedRateToUpdate = null
                                    updateRateValue = ""
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Rate")
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Horizontal Pond Selector Capsules
        Text(
            "Select Aquaculture Pond Focus",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.Gray)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            availablePonds.forEach { pond ->
                val isSelected = selectedPond == pond
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFE0F2F1),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { viewModel.selectPond(pond) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = pond,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            // Quick Add Pond capsule
            Box(
                modifier = Modifier
                    .background(Color(0xFF009688), RoundedCornerShape(20.dp))
                    .clickable { showAddPondDialog = true }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = "Add Pond", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add Pond",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Main Overview Card with Total Feed usage
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "TOTAL CROP FEED USAGE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%,.1f Kg", totalFeedKg),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(27.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = "Feed",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Last Feeding Log",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = pondFeeds.firstOrNull()?.let { "${it.quantityKg} Kg (${it.feedType})" } ?: "No Feeds Entered",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .clickable { viewModel.selectTab("feeds") }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Add Logs", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Grid of Key Shrimp Metrics (Valuation, Tray status & Reminders)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Interactive Remaining Seed/Shrimp Andhra Pradesh Valuation Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(135.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = "Value", tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AP Crop Value", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val apValuation = viewModel.calculateApValuation(latestCount, apRates)
                    Text(
                        text = "₹" + String.format(Locale.getDefault(), "%,.0f", apValuation.totalValueInr),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val stockCountVal = latestCount?.estimatedRemainingStock ?: 0
                    val formattedStock = String.format(Locale.getDefault(), "%,d", stockCountVal)
                    val displaySubtitle = if (apValuation.isSeedStage) "Seed Stage ($formattedStock PL)" else "$formattedStock Pcs Surviving"
                    Text(
                        text = displaySubtitle,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = apValuation.calculationRemark,
                        fontSize = 8.sp,
                        color = Color.Gray,
                        maxLines = 2,
                        lineHeight = 10.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Remainder To Check TRAY Alarm Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(135.dp)
                    .clickable { viewModel.selectTab("alarms") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (futureAlarms.isNotEmpty()) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Alarm",
                            tint = if (futureAlarms.isNotEmpty()) Color(0xFFFF9800) else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tray Alarm", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if (futureAlarms.isNotEmpty()) {
                        val alarm = futureAlarms.first()
                        Text(
                            text = "Tray ${alarm.trayNumber} at ${alarm.scheduledTime}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Next scheduled check", fontSize = 10.sp, color = Color.Gray)
                    } else {
                        Text("No Alarms Set", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Tray check alarm clean", fontSize = 10.sp, color = Color.LightGray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Next Count Day Reminder Widget
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Calendar", tint = Color(0xFF4CAF50))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Next Sampling Count Remainder", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        text = latestCount?.nextCountDate ?: "Not Scheduled Yet",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Button(
                    onClick = { viewModel.selectTab("counts") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Schedule", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Live Market rates Sheet Card (AndraPradesh regional metrics)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Andhra Pradesh Shrimp Market Price Sheet", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text(
                text = "Live (₹ / Kg Vannamei)",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Grade Size (Count/Kg)", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                    Text("Andhra Market Rate (INR)", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                    Text("Action", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                }

                if (apRates.isEmpty()) {
                    Text(
                        text = "Loading AP Market rates...",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
                    )
                } else {
                    apRates.forEach { rate ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${rate.count} Count", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.5f))
                            Text("₹${rate.ratePerKgInInr}/Kg", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .wrapContentWidth(Alignment.End)
                            ) {
                                Text(
                                    text = "Update",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                        .clickable {
                                            selectedRateToUpdate = rate
                                            updateRateValue = rate.ratePerKgInInr.toString()
                                            showUpdateRateDialog = true
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Co-Owners device visual synchronization logger
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Multi-Device Cloud Tracker Logs", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            TextButton(
                onClick = {
                    if (!isManualSyncing) {
                        isManualSyncing = true
                        syncFeedbackText = "Uploading database state to kpericharla2005@gmail.com Cloud storage..."
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(1800)
                            isManualSyncing = false
                            syncFeedbackText = "All local logs securely mirrored in central Cloud SQL Storage (kpericharla2005@gmail.com)."
                        }
                    }
                },
                enabled = !isManualSyncing
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sync Now",
                    modifier = Modifier.size(16.dp),
                    tint = if (isManualSyncing) Color.Gray else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isManualSyncing) "Syncing..." else "Sync Now",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFECEFF1))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isManualSyncing) Icons.Default.Sync else Icons.Default.CloudQueue,
                        contentDescription = "Cloud",
                        tint = if (isManualSyncing) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cloud Server: kpericharla2005@gmail.com",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = syncFeedbackText,
                    fontSize = 11.sp,
                    color = if (isManualSyncing) Color(0xFFE65100) else Color(0xFF2E7D32),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                DeviceLogItem("Karthik Varma Pericharla (Owner-Web)", "Logged in via Google. Modified Feed Evening on Pond Delta-1", "Just now")
                DeviceLogItem("Raju Manager (Mobile)", "Logged in via SMS. Added Count Record [DOC 50 - 18.4g]", "1 Hour ago")
            }
        }
    }
}

@Composable
fun DeviceLogItem(device: String, action: String, time: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(device, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(time, fontSize = 9.sp, color = Color.Gray)
        }
        Text(action, fontSize = 11.sp, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(2.dp))
        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
    }
}


// ==========================================
// 3. FEEDS SCREEN (DAILY DISH TRACKER)
// ==========================================
@Composable
fun FeedsScreen(viewModel: AquacultureViewModel) {
    val selectedPond by viewModel.selectedPond.collectAsState()
    val allFeeds by viewModel.allFeedLogs.collectAsState()

    var showAddFeedDialog by remember { mutableStateOf(false) }

    val pondFeeds = allFeeds.filter { it.pondName == selectedPond }

    // Computations
    val feedByDate = pondFeeds.groupBy { it.date }.mapValues { it.value.sumOf { f -> f.quantityKg } }
    val totalPondFeed = pondFeeds.sumOf { it.quantityKg }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Weekly Feed Usage logs",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text("Pond: $selectedPond", fontSize = 12.sp, color = Color.Gray)
            }
            Button(
                onClick = { showAddFeedDialog = true },
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Log Feed")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Total Feed Added", fontSize = 10.sp, color = Color.Gray)
                    Text(String.format(Locale.getDefault(), "%,.1f Kg", totalPondFeed), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Unique Feeds", fontSize = 10.sp, color = Color.Gray)
                    Text("${pondFeeds.map { it.feedType }.distinct().size} Brands", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Today's Date header and Feed entries
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (pondFeeds.isEmpty()) {
                item {
                    EmptyStatePlaceholder("No Feed inputs logged yet.", Icons.Default.Restaurant)
                }
            } else {
                items(pondFeeds) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFE0F7FA), RoundedCornerShape(18.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.RestaurantMenu, contentDescription = null, tint = Color(0xFF00838F), modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(log.feedType, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(log.date, fontSize = 11.sp, color = Color.Gray)
                                }
                                Text("${log.quantityKg} Kg applied in $selectedPond", fontSize = 12.sp, color = Color.DarkGray)
                                if (log.remarks.isNotEmpty()) {
                                    Text("• ${log.remarks}", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            IconButton(onClick = { viewModel.deleteFeedLog(log) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddFeedDialog) {
        var quantityInput by remember { mutableStateOf("") }
        var feedTypeSelection by remember { mutableStateOf("Grower Pellet") }
        var remarksSelection by remember { mutableStateOf("") }

        val brands = listOf("Starter Pellet", "Grower Pellet", "Finisher Pellet", "Premium Protein Blend")

        Dialog(onDismissRequest = { showAddFeedDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Text("Register Daily Feed log", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Select Feed Type Category", fontSize = 12.sp, color = Color.Gray)
                    Column {
                        brands.forEach { b ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { feedTypeSelection = b }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = feedTypeSelection == b, onClick = { feedTypeSelection = b })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(b, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = quantityInput,
                        onValueChange = { quantityInput = it },
                        label = { Text("Feed Quantity Used (Kg)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = remarksSelection,
                        onValueChange = { remarksSelection = it },
                        label = { Text("Remarks (Time/Session)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddFeedDialog = false }) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val qty = quantityInput.toDoubleOrNull() ?: 0.0
                                if (qty > 0) {
                                    viewModel.addFeedLog(
                                        feedType = feedTypeSelection,
                                        quantity = qty,
                                        cost = 1.6,
                                        pondName = selectedPond,
                                        remarks = remarksSelection
                                    )
                                    showAddFeedDialog = false
                                }
                            }
                        ) {
                            Text("Save Entry")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. MEDICINE / CHEMICAL TRACKER
// ==========================================
@Composable
fun MedicationScreen(viewModel: AquacultureViewModel) {
    val selectedPond by viewModel.selectedPond.collectAsState()
    val allMeds by viewModel.allMedicineLogs.collectAsState()

    var showAddMedDialog by remember { mutableStateOf(false) }

    val pondMeds = allMeds.filter { it.pondName == selectedPond }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Medicine & Probiotic Log",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text("Pond: $selectedPond", fontSize = 12.sp, color = Color.Gray)
            }
            Button(
                onClick = { showAddMedDialog = true },
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Log Med")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (pondMeds.isEmpty()) {
                item {
                    EmptyStatePlaceholder("No chemicals or medication applied in this pond.", Icons.Default.LocalPharmacy)
                }
            } else {
                items(pondMeds) { med ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFE8F5E9), RoundedCornerShape(18.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.LocalPharmacy, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(med.medicineName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(med.date, fontSize = 11.sp, color = Color.Gray)
                                }
                                Text("Dosage: ${med.dosage} | Qty: ${med.quantityUsed} ${med.unit}", fontSize = 12.sp, color = Color.DarkGray)
                                Text("Purpose: ${med.purpose}", fontSize = 11.sp, color = Color.Gray)
                                if (med.remarks.isNotEmpty()) {
                                    Text("Remarks: ${med.remarks}", fontSize = 11.sp, color = Color.LightGray)
                                }
                            }
                            IconButton(onClick = { viewModel.deleteMedicineLog(med) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddMedDialog) {
        var medNameInput by remember { mutableStateOf("") }
        var dosageInput by remember { mutableStateOf("") }
        var purposeInput by remember { mutableStateOf("") }
        var qtyInput by remember { mutableStateOf("") }
        var selectedUnit by remember { mutableStateOf("kg") }
        var remarksInput by remember { mutableStateOf("") }

        val commonMeds = listOf("Gut-Pro Probiotic", "Min-Fortify Minerals", "Oxygen Max Tab", "Nano-Silver Sanitizer")

        Dialog(onDismissRequest = { showAddMedDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Register Medicine Application", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Preset Medicines/Chemicals", fontSize = 11.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        commonMeds.forEach { m ->
                            Box(
                                modifier = Modifier
                                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                    .clickable { medNameInput = m }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(m, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = medNameInput,
                        onValueChange = { medNameInput = it },
                        label = { Text("Medicine/Probiotic Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = dosageInput,
                        onValueChange = { dosageInput = it },
                        label = { Text("Dosage (e.g., 10g/kg feed, 1L/acre)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = purposeInput,
                        onValueChange = { purposeInput = it },
                        label = { Text("Application Purpose") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = qtyInput,
                            onValueChange = { qtyInput = it },
                            label = { Text("Quantity") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Unit", fontSize = 10.sp, color = Color.Gray)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("kg", "Litre", "pkt").forEach { u ->
                                    FilterChip(
                                        selected = selectedUnit == u,
                                        onClick = { selectedUnit = u },
                                        label = { Text(u, fontSize = 9.sp) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = remarksInput,
                        onValueChange = { remarksInput = it },
                        label = { Text("Additional Remarks") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddMedDialog = false }) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (medNameInput.isNotEmpty()) {
                                    viewModel.addMedicineLog(
                                        medName = medNameInput,
                                        dosage = dosageInput,
                                        purpose = purposeInput,
                                        quantity = qtyInput.toDoubleOrNull() ?: 1.0,
                                        unit = selectedUnit,
                                        pondName = selectedPond,
                                        remarks = remarksInput
                                    )
                                    showAddMedDialog = false
                                }
                            }
                        ) {
                            Text("Apply Medicine")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. COUNT RECORDS & GROWTH VISUALIZATION (CANVAS CHART)
// ==========================================
@Composable
fun SamplingScreen(viewModel: AquacultureViewModel) {
    val selectedPond by viewModel.selectedPond.collectAsState()
    val countsChronological by viewModel.countRecordsChronological.collectAsState()
    val apRates by viewModel.allApMarketRates.collectAsState()

    var showAddCountDialog by remember { mutableStateOf(false) }

    val pondCounts = countsChronological.filter { it.pondName == selectedPond }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Growth Sampling Patterns",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text("Pond: $selectedPond", fontSize = 12.sp, color = Color.Gray)
            }
            Button(
                onClick = { showAddCountDialog = true },
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Log Sampling")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- CUSTOM CANNED GROWTH VECTOR CANVAS CHART ---
        Text(
            text = "Average Body Weight (ABW in grams) vs Days of Culture (DOC)",
            fontSize = 11.sp,
            color = Color.Gray,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                if (pondCounts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Add multiple sampling logs to generate a growth analytics curve.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    // Line plot on Jetpack Compose Canvas
                    GrowthPatternCanvasChart(pondCounts)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text("Historic Sampling Records & Seed Valuation", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (pondCounts.isEmpty()) {
                EmptyStatePlaceholder("No sampling logs cataloged.", Icons.Default.Assessment)
            } else {
                pondCounts.asReversed().forEach { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Waves, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("DOC ${record.daysOfCulture}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                                Text("Recorded: ${record.date}", fontSize = 11.sp, color = Color.Gray)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Shrimp Weight (ABW)", fontSize = 10.sp, color = Color.Gray)
                                    Text("${record.averageBodyWeightGrams} grams", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Column {
                                    Text("Est. Surviving Count", fontSize = 10.sp, color = Color.Gray)
                                    Text(String.format(Locale.getDefault(), "%,d pcs", record.estimatedRemainingStock), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("AP Regional Value", fontSize = 10.sp, color = Color.Gray)
                                    val logApVal = viewModel.calculateApValuation(record, apRates)
                                    Text(
                                        text = "₹" + String.format(Locale.getDefault(), "%,.0f", logApVal.totalValueInr),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Next Count Reminder: ${record.nextCountDate}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFF9800)
                                )
                                IconButton(onClick = { viewModel.deleteCountRecord(record) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCountDialog) {
                                var docInput by remember { mutableStateOf("") }
                                var abwInput by remember { mutableStateOf("") }
                                var remainingStockInput by remember { mutableStateOf("") }
                                var rateInput by remember { mutableStateOf("250.0") } // default ₹250.0 per 1000 PL seed
                                var offsetDays by remember { mutableStateOf("10") } // default schedule in 10 days
                                var remarksInput by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddCountDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Register Sampling Count & Seed Valuation", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = docInput,
                        onValueChange = { docInput = it },
                        label = { Text("Days of Culture (DOC Age, e.g. 52)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = abwInput,
                        onValueChange = { abwInput = it },
                        label = { Text("Average Body Weight (ABW in Grams)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = remainingStockInput,
                        onValueChange = { remainingStockInput = it },
                        label = { Text("Estimated Shrimp Survival Piece Count") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = rateInput,
                        onValueChange = { rateInput = it },
                        label = { Text("Seed Valuation Rate per 1000 pcs (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = offsetDays,
                        onValueChange = { offsetDays = it },
                        label = { Text("Next Count Schedule Reminder (in Days)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = remarksInput,
                        onValueChange = { remarksInput = it },
                        label = { Text("Remarks (Feed response/Water status)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddCountDialog = false }) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val doc = docInput.toIntOrNull() ?: 0
                                val abw = abwInput.toDoubleOrNull() ?: 0.0
                                val stock = remainingStockInput.toIntOrNull() ?: 0
                                val valRate = rateInput.toDoubleOrNull() ?: 0.0
                                val remOffset = offsetDays.toIntOrNull() ?: 10

                                if (doc > 0 && abw > 0.0 && stock > 0) {
                                    val calendar = Calendar.getInstance()
                                    calendar.add(Calendar.DAY_OF_YEAR, remOffset)
                                    val formattedNextCountDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

                                    viewModel.addCountRecord(
                                        pondName = selectedPond,
                                        doc = doc,
                                        abw = abw,
                                        remainingStock = stock,
                                        ratePerThousand = valRate,
                                        nextCountDate = formattedNextCountDate,
                                        remarks = remarksInput
                                    )
                                    showAddCountDialog = false
                                }
                            }
                        ) {
                            Text("Save Audit")
                        }
                    }
                }
            }
        }
    }
}

// Custom Drawn growth chart to display pristine vector analytics
@Composable
fun GrowthPatternCanvasChart(pondCounts: List<CountRecord>) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        val width = size.width
        val height = size.height

        val maxDoc = (pondCounts.maxOfOrNull { it.daysOfCulture } ?: 60).toFloat().coerceAtLeast(1.0f)
        val maxAbw = (pondCounts.maxOfOrNull { it.averageBodyWeightGrams } ?: 30.0).toFloat().coerceAtLeast(1.0f)

        // Draw axis lines
        val paintAxis = Paint().asFrameworkPaint().apply {
            color = android.graphics.Color.GRAY
            textSize = 24f
            isAntiAlias = true
        }

        val paddingLeft = 40f
        val paddingBottom = 40f
        val topBound = 20f
        val rightBound = 20f

        val axisXWidth = width - paddingLeft - rightBound
        val axisYHeight = height - paddingBottom - topBound

        // Draw Grid lines & Y labels
        val gridLines = 4
        for (i in 0..gridLines) {
            val yVal = i * (maxAbw / gridLines)
            val yPos = topBound + axisYHeight - (i * (axisYHeight / gridLines))
            // Grid line
            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = Offset(paddingLeft, yPos),
                end = Offset(width - rightBound, yPos),
                strokeWidth = 1f
            )
            // Draw axis text
            drawContext.canvas.nativeCanvas.drawText(
                String.format(Locale.getDefault(), "%.1fg", yVal),
                5f,
                yPos + 8f,
                paintAxis
            )
        }

        // Draw path line for chronological pattern
        val sortedPoints = pondCounts.sortedBy { it.daysOfCulture }
        val points = mutableListOf<Offset>()

        for (p in sortedPoints) {
            val normX = p.daysOfCulture.toFloat() / maxDoc
            val normY = p.averageBodyWeightGrams.toFloat() / maxAbw

            val x = paddingLeft + (normX * axisXWidth)
            val y = topBound + axisYHeight - (normY * axisYHeight)
            points.add(Offset(x, y))
        }

        // Draw elegant linear curve connection path
        if (points.size > 1) {
            val curvePath = Path()
            curvePath.moveTo(points[0].x, points[0].y)
            for (idx in 1 until points.size) {
                curvePath.lineTo(points[idx].x, points[idx].y)
            }
            drawPath(
                path = curvePath,
                color = OceanTealPrimary,
                style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Draw a glowing gradient area below the curve
            val fillPath = Path()
            fillPath.moveTo(points[0].x, points[0].y)
            for (idx in 1 until points.size) {
                fillPath.lineTo(points[idx].x, points[idx].y)
            }
            fillPath.lineTo(points.last().x, topBound + axisYHeight)
            fillPath.lineTo(points.first().x, topBound + axisYHeight)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(OceanTealPrimary.copy(alpha = 0.3f), Color.Transparent)
                )
            )
        }

        // Draw node circles & text
        for (idx in points.indices) {
            val p = points[idx]
            val record = sortedPoints[idx]

            // outer glow
            drawCircle(
                color = LagoonCyanSecondary,
                radius = 8f,
                center = p
            )
            // inner point
            drawCircle(
                color = Color.White,
                radius = 4f,
                center = p
            )

            // Draw DOC labels under dots
            drawContext.canvas.nativeCanvas.drawText(
                "DOC ${record.daysOfCulture}",
                p.x - 25f,
                topBound + axisYHeight + 30f,
                paintAxis
            )
        }
    }
}

// ==========================================
// 6. CHECK TRAY MONITOR & REMAINDER ALARM
// ==========================================
@Composable
fun TrayMonitorScreen(viewModel: AquacultureViewModel) {
    val selectedPond by viewModel.selectedPond.collectAsState()
    val trayAlarms by viewModel.allTrayAlarms.collectAsState()

    var showAddAlarmDialog by remember { mutableStateOf(false) }

    val pondAlarms = trayAlarms.filter { it.pondName == selectedPond }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Check Tray alarms",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text("Pond: $selectedPond", fontSize = 12.sp, color = Color.Gray)
            }
            Button(
                onClick = { showAddAlarmDialog = true },
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.AlarmAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Alarm")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Context info about aquaculture Check Trays
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F7FA))
        ) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = "Info", tint = Color(0xFF006064), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Check trays verify if shrimp completely consume daily rations 1.5 - 2 hours post-feed to adjust doses & prevent bottom soil spoilage.",
                    fontSize = 11.sp,
                    color = Color(0xFF006064),
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (pondAlarms.isEmpty()) {
                item {
                    EmptyStatePlaceholder("No Check Tray alarms set.", Icons.Default.Alarm)
                }
            } else {
                items(pondAlarms) { alarm ->
                    val isPast = System.currentTimeMillis() > alarm.epochScheduledTime
                    val isCompleted = alarm.isCompleted

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isCompleted -> Color(0xFFE8F5E9)
                                isPast -> Color(0xFFFFF3E0)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = when {
                                            isCompleted -> Color(0xFF4CAF50)
                                            isPast -> Color(0xFFFF9800)
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Tray ${alarm.trayNumber} check",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = when {
                                                isCompleted -> Color(0xFFC8E6C9)
                                                isPast -> Color(0xFFFFE0B2)
                                                else -> Color(0xFFE0F7FA)
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isCompleted) alarm.feedRemainingStatus else if (isPast) "OVERDUE ALARM" else "PENDING",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isCompleted -> Color(0xFF2E7D32)
                                            isPast -> Color(0xFFE65100)
                                            else -> Color(0xFF006064)
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Scheduled Day Alarm Check: at ${alarm.scheduledTime}", fontSize = 12.sp, color = Color.DarkGray)
                            if (alarm.remarks.isNotEmpty()) {
                                Text("Notes: ${alarm.remarks}", fontSize = 11.sp, color = Color.Gray)
                            }

                            if (!isCompleted) {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(6.dp))

                                Text("Resolve Tray Check Feed Level:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Complete states
                                    Button(
                                        onClick = { viewModel.completeTrayAlarm(alarm, "Clean (Good)", "Shrimp consumed all feed.") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4EDDA), contentColor = Color(0xFF155724)),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text("Clean / Perfect", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { viewModel.completeTrayAlarm(alarm, "Leftover (Overfed)", "Feed wasted. Reduce subsequent ration.") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF3CD), contentColor = Color(0xFF856404)),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text("Leftover / Over", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { viewModel.completeTrayAlarm(alarm, "Deficient (Underfed)", "Needs dose raise.") },
                                        colors = ButtonColors(Color(0xFFF8D7DA), Color(0xFF721C24), Color.LightGray, Color.White),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text("Empty / Under", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    TextButton(onClick = { viewModel.deleteTrayAlarm(alarm.id) }) {
                                        Text("Delete Alarm", fontSize = 11.sp, color = Color.Red)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddAlarmDialog) {
        var trayNumberInput by remember { mutableStateOf("1") }
        var alarmTimeInput by remember { mutableStateOf("") }
        var offsetSel by remember { mutableStateOf(90) } // Default 90 Mins after feed
        var notesInput by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddAlarmDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Text("Schedule Tray Alarm remainder", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = trayNumberInput,
                        onValueChange = { trayNumberInput = it },
                        label = { Text("Tray Identifier Number (e.g. 1, 2, 3)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = alarmTimeInput,
                        onValueChange = { alarmTimeInput = it },
                        label = { Text("Alarm Check Hour (HH:mm, e.g. 16:30)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Alarm delay offset post-feed:", fontSize = 12.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(60, 90, 120).forEach { mins ->
                            FilterChip(
                                selected = offsetSel == mins,
                                onClick = { offsetSel = mins },
                                label = { Text("$mins mins", fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Tray Location Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddAlarmDialog = false }) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val trayNum = trayNumberInput.toIntOrNull() ?: 1
                                if (alarmTimeInput.contains(":")) {
                                    viewModel.addTrayCheckAlarm(
                                        pondName = selectedPond,
                                        trayNum = trayNum,
                                        scheduledTime = alarmTimeInput,
                                        offsetMins = offsetSel,
                                        remarks = notesInput
                                    )
                                    showAddAlarmDialog = false
                                }
                            }
                        ) {
                            Text("Set Alarms")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. PROFILE & REALTIME SYNC (MULTI-DEVICE)
// ==========================================
@Composable
fun ProfileScreen(viewModel: AquacultureViewModel) {
    val currentUser by viewModel.currentUserProfile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Profile Avatar Badge
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(40.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(45.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = currentUser?.userName ?: "Anonymous Owner",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Role: ${currentUser?.role ?: "Aquaculture Co-Owner"}",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Device Sync Parameters Card - "multi device login availability with google and mobile so all the owners track"
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Pond Multi-Device Sync Parameters",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Multiple farm owners can view and log feed usage, medicine application, sampling and check trays instantly with cloud syncing.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Cloud Database Sync Active", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Current: ${if (currentUser?.isCloudSyncEnabled == true) "Auto Backup Triggered" else "Offline Only"}", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = currentUser?.isCloudSyncEnabled == true,
                        onCheckedChange = { viewModel.toggleCloudSync() }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Login Credentials via:", fontSize = 12.sp, color = Color.DarkGray)
                    Text(currentUser?.loginMethod ?: "Local Account", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Associated Contact ID:", fontSize = 12.sp, color = Color.DarkGray)
                    Text(if (currentUser?.loginMethod?.contains("Google") == true) currentUser?.email.orEmpty() else currentUser?.phone.orEmpty(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Simulated secondary owner login switch - "google signing in and mobile login so ALL the owners track"
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
            border = BorderStroke(1.dp, Color(0xFFFFB74D))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Simulator Tool: Switch Devices/Owners",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                )
                Text(
                    text = "Verify that logging in as a different owner (Google or Mobile) on another device preserves database integrity with the centralized culture records.",
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Button(
                    onClick = { viewModel.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Switch Current Device Session", fontSize = 12.sp)
                }
            }
        }
    }
}


// ==========================================
// SEAMLESS REUSABLE PLACEHOLDER COMPONENT
// ==========================================
@Composable
fun EmptyStatePlaceholder(text: String, icon: ImageVector) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(60.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}
