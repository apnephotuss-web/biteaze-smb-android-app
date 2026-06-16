package com.example.ui.screens.more

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import android.content.SharedPreferences
import com.example.ui.screens.billing.BillingViewModel
import com.example.ui.screens.billing.NumberingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onNavigateToExpenses: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val billingViewModel: BillingViewModel = viewModel(
        factory = BillingViewModel.Factory(context.applicationContext as android.app.Application)
    )

    val prefs: SharedPreferences = remember { context.getSharedPreferences("syncpos_settings", Context.MODE_PRIVATE) }
    var activePage by remember { mutableStateOf("menu") }
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    var businessCategory by remember { mutableStateOf(prefs.getString("business_category", "F&B") ?: "F&B") }
    var expiryWarningDays by remember { mutableStateOf(prefs.getInt("expiry_warning_days", 7)) }
    var defaultBillingView by remember { mutableStateOf(prefs.getString("default_billing_view", "Ongoing Orders") ?: "Ongoing Orders") }

    val state by billingViewModel.uiState.collectAsStateWithLifecycle()

    if (activePage == "directory") {
        // Full screen Customer Directory Screen
        CustomerDirectoryScreen(
            customers = state.customers,
            onUpdateCustomer = { c -> billingViewModel.updateCustomer(c) },
            onBack = { activePage = "menu" }
        )
    } else {
        // Main More Settings & Menu
        Scaffold(
            containerColor = Color(0xFFF8FAFC)
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "MORE",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1E293B),
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Business Category Tile (Removed to enforce permanent selection at signup)
                
                Spacer(modifier = Modifier.height(16.dp))

                // Settings Tile
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp), clip = false)
                        .clickable { showSettingsDialog = true },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFF7ED)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Order ID Settings",
                                tint = Color(0xFFF97316),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Order ID Settings",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Customize how active ticket IDs are generated",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Default Billing Screen setting card tile
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp), clip = false)
                        .clickable {
                            val newMode = if (defaultBillingView == "New Order") "Ongoing Orders" else "New Order"
                            defaultBillingView = newMode
                            prefs.edit().putString("default_billing_view", newMode).apply()
                            prefs.edit().putString("fb_default_billing_view", if (newMode == "New Order") "New Order" else "Dashboard").apply()
                        },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFF7ED)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Launch,
                                contentDescription = "Default Billing Screen Settings",
                                tint = Color(0xFFF97316),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Default Billing Screen",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Open: $defaultBillingView",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Switch(
                            checked = (defaultBillingView == "New Order"),
                            onCheckedChange = { isChecked ->
                                val newMode = if (isChecked) "New Order" else "Ongoing Orders"
                                defaultBillingView = newMode
                                prefs.edit().putString("default_billing_view", newMode).apply()
                                prefs.edit().putString("fb_default_billing_view", if (newMode == "New Order") "New Order" else "Dashboard").apply()
                            },
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFF97316)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Customer Directory Tile
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp), clip = false)
                        .clickable { activePage = "directory" },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFF7ED)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = "Customer Directory",
                                tint = Color(0xFFF97316),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Customer Directory",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "View returning customers, details & billing addresses",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Service-Based Settings
                if (businessCategory == "Service-Based") {
                    var showOngoing by remember { mutableStateOf(prefs.getBoolean("show_ongoing_orders", true)) }
                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp), clip = false),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable {
                                    showOngoing = !showOngoing
                                    prefs.edit().putBoolean("show_ongoing_orders", showOngoing).apply()
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFFF7ED)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatListBulleted,
                                    contentDescription = "Ongoing Orders Visibility",
                                    tint = Color(0xFFF97316),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Show 'Ongoing' Orders Button",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "Display ongoing orders shortcut during billing",
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Switch(
                                checked = showOngoing,
                                onCheckedChange = { isChecked ->
                                    showOngoing = isChecked
                                    prefs.edit().putBoolean("show_ongoing_orders", isChecked).apply()
                                },
                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFF97316)
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Expense Ledger Tile
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp), clip = false)
                        .clickable { onNavigateToExpenses() },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFF7ED)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "Expense Ledger",
                                tint = Color(0xFFF97316),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Expense Ledger",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Track business and operational expenditures",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cloud Backup Tile
                val scope = rememberCoroutineScope()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val app = context.applicationContext as com.example.SyncPosApplication
                            val syncManager = com.example.core.sync.CloudSyncManager(app.repository)
                            scope.launch {
                                try {
                                    Toast.makeText(context, "Cloud sync started...", Toast.LENGTH_SHORT).show()
                                    syncManager.backupDataToCloud()
                                    Toast.makeText(context, "Cloud backup completed successfully", Toast.LENGTH_SHORT).show()
                                } catch(e: Exception) {
                                    Toast.makeText(context, "Sync failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFF7ED)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Cloud Backup",
                                tint = Color(0xFFF97316),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cloud Backup",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Manually push local database to cloud storage",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sign Out Tile
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val authManager = com.example.core.auth.FirebaseAuthManager(context)
                            authManager.signOut()
                            onSignOut()
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFFEE2E2))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFEAEA)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Sign Out",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sign Out",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                            Text(
                                text = "Log out from your sync account",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFFEF4444)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showSettingsDialog) {
        var selectedMode by remember { mutableStateOf(billingViewModel.numberingMode) }
        var tempPrefix by remember { mutableStateOf(billingViewModel.customPrefix) }
        var tempNextNumberStr by remember { mutableStateOf(billingViewModel.customNextNumber.toString()) }

        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color(0xFFF97316)
                    )
                    Text(
                        text = "ORDER ID SETTINGS",
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1E293B),
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Choose how active ticket IDs are generated representing daily, monthly, yearly resets, or custom formats.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMode = NumberingMode.DAILY }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (selectedMode == NumberingMode.DAILY),
                                onClick = { selectedMode = NumberingMode.DAILY },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFF97316))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Reset Daily", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 14.sp)
                                Text("Starts from 1 each calendar day (Format: D-YYYYMMDD-X)", fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMode = NumberingMode.MONTHLY }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (selectedMode == NumberingMode.MONTHLY),
                                onClick = { selectedMode = NumberingMode.MONTHLY },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFF97316))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Reset Monthly", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 14.sp)
                                Text("Starts from 1 each calendar month (Format: M-YYYYMM-X)", fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMode = NumberingMode.YEARLY }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (selectedMode == NumberingMode.YEARLY),
                                onClick = { selectedMode = NumberingMode.YEARLY },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFF97316))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Reset Yearly", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 14.sp)
                                Text("Starts from 1 each calendar year (Format: Y-YYYY-X)", fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMode = NumberingMode.CUSTOM }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (selectedMode == NumberingMode.CUSTOM),
                                onClick = { selectedMode = NumberingMode.CUSTOM },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFF97316))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Custom Format", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 14.sp)
                                Text("Set a custom prefix pattern and the next auto-incrementing serial number", fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                        }
                    }

                    if (selectedMode == NumberingMode.CUSTOM) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = tempPrefix,
                                onValueChange = { tempPrefix = it },
                                label = { Text("Prefix format") },
                                placeholder = { Text("e.g. INV-") },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFF97316))
                            )

                            OutlinedTextField(
                                value = tempNextNumberStr,
                                onValueChange = { tempNextNumberStr = it },
                                label = { Text("Next Number") },
                                placeholder = { Text("e.g. 1") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.8f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFF97316))
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        billingViewModel.numberingMode = selectedMode
                        if (selectedMode == NumberingMode.CUSTOM) {
                            billingViewModel.customPrefix = tempPrefix
                            billingViewModel.customNextNumber = tempNextNumberStr.toIntOrNull() ?: 1
                        }
                        showSettingsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Settings", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun CustomerDirectoryScreen(
    customers: List<com.example.core.database.entity.CustomerEntity>,
    onUpdateCustomer: (com.example.core.database.entity.CustomerEntity) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    var searchQuery by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf("All Profiles") }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var selectedCustomerForPayment by remember { mutableStateOf<com.example.core.database.entity.CustomerEntity?>(null) }
    var paymentAmountInput by remember { mutableStateOf("") }

    val filteredCustomers = remember(customers, searchQuery, activeTab) {
        val base = if (activeTab == "Active Credit") customers.filter { it.creditBalance > 0 } else customers
        if (searchQuery.isBlank()) {
            base
        } else {
            base.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.phone.contains(searchQuery, ignoreCase = true) ||
                        (it.address?.contains(searchQuery, ignoreCase = true) ?: false)
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Customer Directory",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                }
                HorizontalDivider(color = Color(0xFFE2E8F0))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Search Input Row
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, phone or address...", color = Color(0xFF94A3B8), fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF64748B))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFF97316),
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true
            )

            // Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf("All Profiles", "Active Credit").forEach { tab ->
                    val isSelected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF1E293B) else Color.White)
                            .border(1.dp, if (isSelected) Color(0xFF1E293B) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .clickable { activeTab = tab }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFF475569)
                        )
                    }
                }
            }

            if (filteredCustomers.isEmpty()) {
                // Highly polished empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFFFFF7ED),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PeopleOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFF97316),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "No Customers Recorded" else "No matching profiles found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF334155)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "Returning profiles will appear here as they are added during orders." else "Try adjusting your search criteria.",
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else {
                // Tabular customer table
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp), clip = false),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Table Headers
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DISPLAY NAME",
                                modifier = Modifier.weight(1.1f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "CONTACT PHONE",
                                modifier = Modifier.weight(1.1f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "BILLING ADDRESS",
                                modifier = Modifier.weight(1.3f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "CREDIT",
                                modifier = Modifier.weight(1.0f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp,
                                textAlign = TextAlign.End
                            )
                            Box(modifier = Modifier.width(60.dp)) // Action header placeholder
                        }
                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        // Scrollable Tabular Rows
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(filteredCustomers) { c ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Name
                                    Text(
                                        text = c.name,
                                        modifier = Modifier.weight(1.1f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    // Phone
                                    Text(
                                        text = c.phone,
                                        modifier = Modifier.weight(1.1f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF475569),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    // Address
                                    Text(
                                        text = if (c.address.isNullOrBlank()) "N/A" else c.address,
                                        modifier = Modifier.weight(1.3f),
                                        fontSize = 12.sp,
                                        color = if (c.address.isNullOrBlank()) Color(0xFF94A3B8) else Color(0xFF475569),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    // Credit
                                    Text(
                                        text = "Rs. ${String.format("%.2f", c.creditBalance)}",
                                        modifier = Modifier.weight(1.0f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (c.creditBalance > 0) Color(0xFFF97316) else Color(0xFF94A3B8),
                                        textAlign = TextAlign.End
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Actions
                                    if (c.creditBalance > 0) {
                                        TextButton(
                                            onClick = {
                                                selectedCustomerForPayment = c
                                                paymentAmountInput = String.format("%.2f", c.creditBalance).replace(',', '.')
                                                showPaymentDialog = true
                                            },
                                            modifier = Modifier.width(60.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Settle", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    } else {
                                        IconButton(
                                            onClick = {
                                                val clip = ClipData.newPlainText("phone", c.phone)
                                                clipboardManager.setPrimaryClip(clip)
                                                Toast.makeText(context, "Copied phone: ${c.phone}", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color(0xFFFFF7ED), RoundedCornerShape(10.dp))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.ContentCopy,
                                                contentDescription = "Copy phone details",
                                                tint = Color(0xFFF97316),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(24.dp))
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                            }
                        }
                    }
                }
            }
        }
        
        if (showPaymentDialog) {
            AlertDialog(
                onDismissRequest = { showPaymentDialog = false },
                title = { Text("Settle Credit", fontWeight = FontWeight.Bold, color = Color(0xFF16A34A)) },
                text = {
                    Column {
                        Text("Enter the amount paid by ${selectedCustomerForPayment?.name ?: "Customer"}:", color = Color(0xFF475569), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = paymentAmountInput,
                            onValueChange = { paymentAmountInput = it },
                            label = { Text("Amount Paid") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val paidAmt = paymentAmountInput.toDoubleOrNull() ?: 0.0
                            val c = selectedCustomerForPayment
                            if (c != null && paidAmt > 0) {
                                val newBalance = (c.creditBalance - paidAmt).coerceAtLeast(0.0)
                                onUpdateCustomer(c.copy(creditBalance = newBalance))
                            }
                            showPaymentDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                    ) {
                        Text("Confirm Payment")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPaymentDialog = false }) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }
                }
            )
        }
    }
}
