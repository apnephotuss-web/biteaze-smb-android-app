package com.example.ui.screens.billing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.database.entity.ProductEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

sealed class POSSubScreen {
    object Dashboard : POSSubScreen()
    object ActivePOS : POSSubScreen()
    object Calendar : POSSubScreen()
}

enum class VoiceAssistantState {
    IDLE, LISTENING, PROCESSING
}

@Composable
fun BillingPOSScreen(
    viewModel: BillingViewModel = viewModel(
        factory = BillingViewModel.Factory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val businessCategory by viewModel.businessCategory.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    var subScreen by remember(businessCategory) { 
        val prefs = context.getSharedPreferences("syncpos_settings", android.content.Context.MODE_PRIVATE)
        val defaultVal = if (businessCategory == "Service-Based" || businessCategory == "Grocery") "New Order" else "Ongoing Orders"
        val defaultView = prefs.getString("default_billing_view", defaultVal) ?: defaultVal
        if (defaultView == "New Order") {
            mutableStateOf<POSSubScreen>(POSSubScreen.ActivePOS)
        } else {
            mutableStateOf<POSSubScreen>(POSSubScreen.Dashboard)
        }
    }
    
    var activeOrderId by remember(businessCategory) { 
        val prefs = context.getSharedPreferences("syncpos_settings", android.content.Context.MODE_PRIVATE)
        val defaultVal = if (businessCategory == "Service-Based" || businessCategory == "Grocery") "New Order" else "Ongoing Orders"
        val defaultView = prefs.getString("default_billing_view", defaultVal) ?: defaultVal
        if (defaultView == "New Order") {
            mutableStateOf("new")
        } else {
            mutableStateOf("")
        }
    }

    LaunchedEffect(businessCategory) {
        val prefs = context.getSharedPreferences("syncpos_settings", android.content.Context.MODE_PRIVATE)
        val defaultVal = if (businessCategory == "Service-Based" || businessCategory == "Grocery") "New Order" else "Ongoing Orders"
        val defaultView = prefs.getString("default_billing_view", defaultVal) ?: defaultVal
        if (defaultView == "New Order") {
            viewModel.loadOrder("new")
        }
    }

    when (subScreen) {
        is POSSubScreen.Dashboard -> {
            BillingDashboardScreen(
                onNewOrderClick = {
                    activeOrderId = "new"
                    viewModel.loadOrder("new")
                    subScreen = POSSubScreen.ActivePOS
                },
                onNewOrderClickWithTable = { tableId ->
                    activeOrderId = "new"
                    viewModel.loadOrder("new", tableNumber = tableId)
                    subScreen = POSSubScreen.ActivePOS
                },
                onResumeOrderClick = { orderId ->
                    activeOrderId = orderId
                    viewModel.loadOrder(orderId)
                    subScreen = POSSubScreen.ActivePOS
                },
                viewModel = viewModel
            )
        }
        is POSSubScreen.ActivePOS -> {
            ActivePOSConsole(
                orderId = activeOrderId,
                onNavigateBack = {
                    if (businessCategory == "Grocery" || businessCategory == "Service-Based") {
                        // Grocery and Service-Based don't use the ongoing orders dashboard; reset directly to a new clean order session
                        viewModel.clearCart()
                        activeOrderId = "new"
                        viewModel.loadOrder("new")
                    } else {
                        subScreen = POSSubScreen.Dashboard
                    }
                },
                onNavigateToDashboard = {
                    subScreen = POSSubScreen.Dashboard
                },
                viewModel = viewModel
            )
        }
        is POSSubScreen.Calendar -> {
            ServiceCalendarView(
                onBookSlotClick = { startTime ->
                    activeOrderId = "new"
                    viewModel.loadOrderForTimeSlot("new", startTime)
                    subScreen = POSSubScreen.ActivePOS
                },
                onResumeOrderClick = { orderId ->
                    activeOrderId = orderId
                    viewModel.loadOrder(orderId)
                    subScreen = POSSubScreen.ActivePOS
                },
                onNavigateBack = {
                    subScreen = POSSubScreen.Dashboard
                },
                viewModel = viewModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivePOSConsole(
    orderId: String,
    onNavigateBack: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    viewModel: BillingViewModel
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()
    val isTablet = configuration.screenWidthDp >= 600

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val businessCategory by viewModel.businessCategory.collectAsStateWithLifecycle()

    // Dialog state controllers
    var showCustomerDialog by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var showVoiceHelpDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // POS Top Header: Slim 56dp header, white design card, clean shadow
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(4.dp, shape = RoundedCornerShape(0.dp)),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Compact Circle Back Button
                        if (businessCategory != "Service-Based" && businessCategory != "Grocery") {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFF475569),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Main Heading
                        Text(
                            text = if (orderId == "new") "NEW ORDER" else "EDIT ORDER",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.W900,
                            letterSpacing = (-0.5).sp,
                            color = Color(0xFF1E293B)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
        },
        containerColor = Color(0xFFF8FAFC) // Slate background off-white #F8FAFC
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isTablet) {
                // Tablet Mode: Side by Side Dual Panel
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1.8f)) {
                        ProductCatalogPane(
                            state = state,
                            viewModel = viewModel,
                            onTriggerVoiceHelp = { showVoiceHelpDialog = true },
                            onSaveAndHold = {
                                viewModel.saveAndHoldOrder {
                                    Toast.makeText(context, "Transaction Parked / Draft Held!", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                }
                            },
                            onSaveAndBill = {
                                viewModel.saveAndPrintCompletedOrder {
                                    showReceiptDialog = true
                                }
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(Color(0xFFF1F5F9))
                    )

                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .background(Color.White)
                    ) {
                        CheckoutSummaryPane(
                            context = context,
                            state = state,
                            viewModel = viewModel,
                            isMobile = false,
                            onCloseDrawer = {},
                            onEditCustomer = { showCustomerDialog = true },
                            onEditDiscount = { showDiscountDialog = true },
                            onHoldClick = {
                                viewModel.saveAndHoldOrder {
                                    Toast.makeText(context, "Transaction Parked / Draft Held!", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                }
                            },
                            onPrintClick = {
                                viewModel.saveAndPrintCompletedOrder {
                                    showReceiptDialog = true
                                }
                            }
                        )
                    }
                }
            } else {
                // Mobile Mode: Catalog space and bottom sticky overlay
                var showMobileCart by remember(orderId) { mutableStateOf(orderId != "new" && orderId.isNotBlank()) }

                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        ProductCatalogPane(
                            state = state,
                            viewModel = viewModel,
                            onTriggerVoiceHelp = { showVoiceHelpDialog = true },
                            onSaveAndHold = {
                                viewModel.saveAndHoldOrder {
                                    Toast.makeText(context, "Draft Saved & Held!", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                }
                            },
                            onSaveAndBill = {
                                viewModel.saveAndPrintCompletedOrder {
                                    showReceiptDialog = true
                                }
                            }
                        )
                    }

                    // Standard Mobile Bottom Sticky Safety Bar
                    val totalCartQty = state.cart.sumOf { it.quantity }
                    val discountAmount = when (state.discountType) {
                        DiscountType.PERCENT -> state.subtotal * (state.discountValue / 100.0)
                        DiscountType.FIXED -> state.discountValue
                        else -> 0.0
                    }
                    val finalPayable = (state.subtotal + state.calculatedTax - discountAmount).coerceAtLeast(0.0)

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 8.dp,
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
                        ) {
                            if (businessCategory == "Service-Based" || businessCategory == "Grocery") {
                                androidx.compose.material3.Button(
                                    onClick = onNavigateToDashboard,
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF97316),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .height(34.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FormatListBulleted, 
                                            contentDescription = "Ongoing Orders",
                                            modifier = Modifier.size(14.dp),
                                            tint = Color.White
                                        )
                                        Text("ONGOING", fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.5.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "$totalCartQty ITEMS ADDED".uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF94A3B8),
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Rs. ${String.format("%.2f", finalPayable)}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                                Button(
                                    onClick = { showMobileCart = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)), // BrandAccent Bright Orange
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = null, tint = Color.White)
                                        Text("VIEW CART", fontWeight = FontWeight.Black, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Slide Mobile active Cart Drawer (Full Screen version)
                if (showMobileCart) {
                    Dialog(
                        onDismissRequest = { showMobileCart = false },
                        properties = androidx.compose.ui.window.DialogProperties(
                            usePlatformDefaultWidth = false
                        )
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFFF8FAFC)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                CheckoutSummaryPane(
                                    context = context,
                                    state = state,
                                    viewModel = viewModel,
                                    isMobile = true,
                                    onCloseDrawer = { showMobileCart = false },
                                    onEditCustomer = { showCustomerDialog = true },
                                    onEditDiscount = { showDiscountDialog = true },
                                    onHoldClick = {
                                        viewModel.saveAndHoldOrder {
                                            showMobileCart = false
                                            Toast.makeText(context, "Draft Saved & Held!", Toast.LENGTH_SHORT).show()
                                            onNavigateBack()
                                        }
                                    },
                                    onPrintClick = {
                                        viewModel.saveAndPrintCompletedOrder {
                                            showMobileCart = false
                                            showReceiptDialog = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showDiscountDialog) {
            var selectedType by remember { mutableStateOf(state.discountType ?: DiscountType.PERCENT) }
            var tempValStr by remember { mutableStateOf(if (state.discountValue > 0) state.discountValue.toString() else "") }

            AlertDialog(
                onDismissRequest = { showDiscountDialog = false },
                title = { Text("APPLY DISCOUNT", fontWeight = FontWeight.Black, color = Color(0xFFF97316)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Apply discount percentages or absolute deduction amounts across current order item aggregate.")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { selectedType = DiscountType.PERCENT },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedType == DiscountType.PERCENT) Color(0xFFF97316) else Color(0xFFF1F5F9),
                                    contentColor = if (selectedType == DiscountType.PERCENT) Color.White else Color(0xFF475569)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Percentage %", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { selectedType = DiscountType.FIXED },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedType == DiscountType.FIXED) Color(0xFFF97316) else Color(0xFFF1F5F9),
                                    contentColor = if (selectedType == DiscountType.FIXED) Color.White else Color(0xFF475569)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Flat (Rs.)", fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedTextField(
                            value = tempValStr,
                            onValueChange = { tempValStr = it },
                            label = { Text("Value Amount") },
                            placeholder = { Text(if (selectedType == DiscountType.PERCENT) "e.g. 10%" else "e.g. 150") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val v = tempValStr.toDoubleOrNull() ?: 0.0
                            viewModel.setDiscount(selectedType, v)
                            showDiscountDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)), // Brand Accent
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Apply", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            viewModel.setDiscount(null, 0.0)
                            showDiscountDialog = false
                        }
                    ) {
                        Text("Remove Discount", color = Color(0xFFF97316), fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = Color.White
            )
        }

        if (showCustomerDialog) {
            var tempName by remember { mutableStateOf(state.customerName ?: "") }
            var tempPhone by remember { mutableStateOf(state.customerPhone ?: "") }
            var tempAddress by remember { mutableStateOf(state.customerAddress ?: "") }
            var searchQuery by remember { mutableStateOf("") }
            var isCreatingNew by remember { mutableStateOf(false) }

            val filteredCustomers = remember(state.customers, searchQuery) {
                if (searchQuery.isBlank()) state.customers else state.customers.filter {
                    it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery, ignoreCase = true)
                }
            }

            AlertDialog(
                onDismissRequest = { showCustomerDialog = false },
                title = { Text("CUSTOMER RECORD", fontWeight = FontWeight.Black, color = Color(0xFFF97316)) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Select an existing profile or add a new customer.", color = Color(0xFF64748B), fontSize = 13.sp)
                        
                        if (state.customers.isNotEmpty() && !isCreatingNew) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search by name or phone") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Recent Customers", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF475569))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                filteredCustomers.take(5).forEach { c ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.setCustomerInfo(c.name, c.phone, c.address)
                                                showCustomerDialog = false
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(c.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(c.phone, fontSize = 12.sp, color = Color(0xFF64748B))
                                                if (!c.address.isNullOrBlank()) {
                                                    Text(c.address, fontSize = 11.sp, color = Color(0xFF94A3B8))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            TextButton(onClick = { isCreatingNew = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                Text("+ Create New Customer", color = Color(0xFFF97316), fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedTextField(
                                value = tempName,
                                onValueChange = { tempName = it },
                                label = { Text("Display Name") },
                                placeholder = { Text("e.g. John Doe") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = tempPhone,
                                onValueChange = { tempPhone = it },
                                label = { Text("Contact Phone") },
                                placeholder = { Text("e.g. 9876543210") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = tempAddress,
                                onValueChange = { tempAddress = it },
                                label = { Text("Billing Address") },
                                placeholder = { Text("e.g. 123 Main St, City") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            if (state.customers.isNotEmpty()) {
                                TextButton(onClick = { isCreatingNew = false }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                    Text("Pick Existing Customer", color = Color(0xFF64748B))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (state.customers.isEmpty() || isCreatingNew) {
                        Button(
                            onClick = {
                                viewModel.setCustomerInfo(tempName, tempPhone, tempAddress)
                                showCustomerDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)), // Brand Orange Accent
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Details", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomerDialog = false }) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = Color.White
            )
        }

        // Voice Command Assistant Help Modal
        if (showVoiceHelpDialog) {
            VoiceHelpFullScreenDialog(onDismiss = { showVoiceHelpDialog = false })
        }

        // Part 5: Component — Custom Receipt / Invoice Visualizer Modal Dialog OVERLAY
        if (showReceiptDialog) {
            val discountAmount = when (state.discountType) {
                DiscountType.PERCENT -> state.subtotal * (state.discountValue / 100.0)
                DiscountType.FIXED -> state.discountValue
                else -> 0.0
            }
            val payable = (state.subtotal + state.calculatedTax - discountAmount).coerceAtLeast(0.0)

            // Dimming Modal overlay underlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99020617)) // bg-slate-950, opacity 60%
                    .clickable { /* Block clicks */ },
                contentAlignment = Alignment.Center
            ) {
                // Receipt Paper Container: Minimalist ticket card (bg-white, extra-deep corner radius 40dp)
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(16.dp)
                        .shadow(16.dp, shape = RoundedCornerShape(40.dp)),
                    shape = RoundedCornerShape(40.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        // Title header
                        Text(
                            text = "RECEIPT PREVIEW",
                            fontWeight = FontWeight.W900,
                            letterSpacing = 1.sp,
                            fontSize = 16.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Inner thermal paper replica layout
                        Surface(
                            color = Color(0xFFF8FAFC), // Gray thermal paper replica
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "SyncPOS INVOICE",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = "Terminal Active: #001",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF64748B)
                                        )
                                        Text(
                                            text = Date().toString().take(19),
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF64748B)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Ref Code: Order #${state.displayId}",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Customer Metadata Segment
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                                                .padding(6.dp)
                                        ) {
                                            if (!state.customerName.isNullOrEmpty()) {
                                                Text(
                                                    text = "Name: ${state.customerName}",
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color(0xFF0F172A)
                                                )
                                            }
                                            if (!state.customerPhone.isNullOrEmpty()) {
                                                Text(
                                                    text = "Mbl: ${state.customerPhone}",
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color(0xFF0F172A)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
                                            drawPath(
                                                path = Path().apply {
                                                    moveTo(0f, 0f)
                                                    lineTo(size.width, 0f)
                                                },
                                                color = Color(0xFFE2E8F0),
                                                style = Stroke(
                                                    width = 2f,
                                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }

                                // Single list items
                                items(state.cart) { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else String.format("%.3f", item.quantity)}x ${item.product.name}",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF0F172A),
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Rs.${String.format("%.2f", item.product.price * item.quantity)}",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                }

                                item {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
                                            drawPath(
                                                path = Path().apply {
                                                    moveTo(0f, 0f)
                                                    lineTo(size.width, 0f)
                                                },
                                                color = Color(0xFFE2E8F0),
                                                style = Stroke(
                                                    width = 2f,
                                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Dashed Calculations Block Segment
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Subtotal:", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF64748B))
                                            Text("Rs.${String.format("%.2f", state.subtotal)}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF0F172A))
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Taxes Added:", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF4F46E5))
                                            Text("+Rs.${String.format("%.2f", state.calculatedTax)}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF4F46E5))
                                        }
                                        val discountAmount = when (state.discountType) {
                                            DiscountType.PERCENT -> state.subtotal * (state.discountValue / 100.0)
                                            DiscountType.FIXED -> state.discountValue
                                            else -> 0.0
                                        }
                                        if (discountAmount > 0) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Green Discount:", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF10B981))
                                                Text("-Rs.${String.format("%.2f", discountAmount)}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF10B981))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Grand Total: Rs.${String.format("%.2f", payable)}",
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Black,
                                            color = Color.Black
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Paid via [${state.paymentMode.name}]",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Thank you for your visit!",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Actions Controls Bar
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    showReceiptDialog = false
                                    viewModel.clearCart()
                                    onNavigateBack()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = Color(0xFF475569)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Close", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    Toast.makeText(context, "Initializing print spooler...", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1.3f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)), // Rich orange button with printer layout
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Print, contentDescription = null, tint = Color.White)
                                    Text("Print Receipt", fontWeight = FontWeight.Black, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCatalogPane(
    state: BillingUiState,
    viewModel: BillingViewModel,
    onTriggerVoiceHelp: () -> Unit,
    onSaveAndHold: (() -> Unit)? = null,
    onSaveAndBill: (() -> Unit)? = null
) {
    var customizingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var fractionalQuantityProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var staffSelectionProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var selectedStaffForCustomization by remember { mutableStateOf<String?>(null) }

    val businessCategory by viewModel.businessCategory.collectAsStateWithLifecycle()
    var showQuickAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search & Voice-Assistant Console layout
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rounded field overlay
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search catalog...", color = Color(0xFF94A3B8), fontSize = 13.sp) },
                prefix = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF94A3B8), modifier = Modifier.padding(end = 4.dp).size(18.dp))
                },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFF97316),
                    unfocusedBorderColor = Color(0xFFF1F5F9),
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC)
                ),
                singleLine = true
            )

            // Quick Add button for Service based or in general
            IconButton(
                onClick = { showQuickAddDialog = true },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF97316))
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Quick Add", tint = Color.White)
            }

            com.example.ui.components.VoiceTriggerFAB(
                mode = com.example.core.voice.OperationMode.BILLING_MODE,
                onIntentRecognized = { intent -> viewModel.handleVoiceIntent(intent, onSaveAndHold, onSaveAndBill) }
            )

            // Help Button (?): gray square box to trigger instruction popup
            IconButton(
                onClick = onTriggerVoiceHelp,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE2E8F0))
            ) {
                Text("?", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF475569))
            }
        }

        // Split Category Rail & High-Density Catalog Grid
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Column Vertical Rail (Width: ~72dp)
            Column(
                modifier = Modifier
                    .width(72.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val cats = listOf("All") + state.categories
                cats.forEach { cat ->
                    val isSelected = (cat == "All" && state.selectedCategory == null) ||
                            (cat != "All" && state.selectedCategory?.lowercase() == cat.lowercase())

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF1E293B) else Color.White) // active black, inactive white
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                viewModel.selectCategory(if (cat == "All") null else cat)
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            color = if (isSelected) Color.White else Color(0xFF334155),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Favorite Toggle as vertical button
                IconButton(
                    onClick = { viewModel.toggleFavorites() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (state.showOnlyFavorites) Color(0xFFFEE2E2) else Color(0xFFF1F5F9))
                ) {
                    Icon(
                        imageVector = if (state.showOnlyFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Fav Filter",
                        tint = if (state.showOnlyFavorites) Color(0xFFEF4444) else Color(0xFF64748B)
                    )
                }
            }

            // Right Column Grid (Products Panel High Density Grid)
            if (state.filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No products in filtered catalog.", color = Color(0xFF94A3B8), fontSize = 13.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1), // Exactly 1 item per row (single column wide rectangles)
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.filteredProducts) { item ->
                        val inCartQty = state.cart.filter { it.product.id == item.id }.sumOf { it.quantity }
                        ProductGridCard(
                            product = item,
                            currentQtyInCart = inCartQty,
                            onAdd = {
                                if ((item.metadata as? com.example.core.database.entity.IndustryMetadata.Grocery)?.isFractional == true) {
                                    fractionalQuantityProduct = item
                                } else if (!item.addonIds.isNullOrBlank() || !item.variationIds.isNullOrBlank()) {
                                    customizingProduct = item
                                } else {
                                    viewModel.addToCart(item)
                                }
                            },
                            onSubtract = {
                                state.cart.find { it.product.id == item.id }?.let { cartItem ->
                                    viewModel.removeFromCart(cartItem.cartKey)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    customizingProduct?.let { prod ->
        ProductModifierDialog(
            product = prod,
            allAddons = state.addons,
            allVariations = state.variations,
            onDismiss = { 
                customizingProduct = null
                selectedStaffForCustomization = null
            },
            onAddToCart = { varId, addonIds ->
                if (businessCategory == "Service-Based" && selectedStaffForCustomization != null) {
                    viewModel.addCustomToCartWithStaff(prod, varId, addonIds, selectedStaffForCustomization!!)
                } else {
                    viewModel.addCustomToCart(prod, varId, addonIds)
                }
                customizingProduct = null
                selectedStaffForCustomization = null
            }
        )
    }

    staffSelectionProduct?.let { prod ->
        StaffSelectionDialog(
            product = prod,
            staffList = state.staffList,
            onDismiss = { staffSelectionProduct = null },
            onStaffSelected = { staff ->
                if (!prod.addonIds.isNullOrBlank() || !prod.variationIds.isNullOrBlank()) {
                    selectedStaffForCustomization = staff.id
                    customizingProduct = prod
                } else {
                    viewModel.addToCartWithStaff(prod, staff.id)
                }
                staffSelectionProduct = null
            }
        )
    }

    fractionalQuantityProduct?.let { prod ->
        var qtyStr by remember { mutableStateOf("") }
        var selectedUnit by remember(prod.id) { mutableStateOf(prod.unit) }
        val unitOptions = listOf("pcs", "kg", "g", "liters", "ml", "box", "pack")
        
        androidx.compose.ui.window.Dialog(onDismissRequest = { fractionalQuantityProduct = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "ENTER QUANTITY",
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = prod.name,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B),
                        fontSize = 20.sp
                    )
                    val convertedPrice = if (selectedUnit != prod.unit) {
                        val factor = getQuantityConversionFactor(prod.unit, selectedUnit)
                        prod.price / factor
                    } else {
                        prod.price
                    }
                    Text(
                        text = "Price: Rs. ${if (convertedPrice % 1.0 == 0.0) convertedPrice.toInt() else String.format("%.2f", convertedPrice)} per $selectedUnit",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF97316),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Select Unit Type",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    var dropdownExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedUnit,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Expand Units",
                                    modifier = Modifier.clickable { dropdownExpanded = true }
                               )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { dropdownExpanded = true },
                            shape = RoundedCornerShape(12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { dropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            unitOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt) },
                                    onClick = {
                                        selectedUnit = opt
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = qtyStr,
                        onValueChange = { qtyStr = it },
                        label = { Text("Quantity") },
                        suffix = { Text(selectedUnit, fontWeight = FontWeight.Bold, color = Color(0xFFF97316)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            val q = qtyStr.toDoubleOrNull()
                            if (q != null && q > 0) {
                                viewModel.addFractionalQuantityToCart(prod, q, selectedUnit)
                                fractionalQuantityProduct = null
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add to Cart", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showQuickAddDialog) {
        var quickName by remember { mutableStateOf("") }
        var quickPrice by remember { mutableStateOf("") }
        var saveToInventory by remember { mutableStateOf(false) }
        val context = LocalContext.current
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            delay(150)
            focusRequester.requestFocus()
        }

        androidx.compose.ui.window.Dialog(onDismissRequest = { showQuickAddDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "QUICK BILL ITEM",
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = quickName,
                        onValueChange = { quickName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        label = { Text("Item/Service Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF97316),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = quickPrice,
                        onValueChange = { quickPrice = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Price (Rs.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF97316),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { saveToInventory = !saveToInventory }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = saveToInventory,
                            onCheckedChange = { saveToInventory = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFF97316))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save in Menu", fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showQuickAddDialog = false },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = Color(0xFF475569)),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        Button(
                            onClick = {
                                if (quickName.isNotBlank() && quickPrice.toDoubleOrNull() != null) {
                                    val price = quickPrice.toDouble()
                                    val newProduct = ProductEntity(
                                        id = "custom-${UUID.randomUUID()}",
                                        name = quickName.trim(),
                                        category = if (businessCategory == "Service-Based") "Service-Based" else "General",
                                        sku = "QCK-${System.currentTimeMillis()}",
                                        price = price,
                                        stock = 9999.0, // Indefinite supply for quick items
                                        minStock = 0.0,
                                        unit = "pcs",
                                        taxRate = 0.0
                                    )
                                    if (saveToInventory) {
                                        viewModel.saveProductGlobal(newProduct)
                                        Toast.makeText(context, "Item Saved to Inventory", Toast.LENGTH_SHORT).show()
                                    }
                                    viewModel.addQuickItemToCart(newProduct)
                                    showQuickAddDialog = false
                                } else {
                                    Toast.makeText(context, "Please enter valid details", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("Add Item", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductGridCard(
    product: ProductEntity,
    currentQtyInCart: Double,
    onAdd: () -> Unit,
    onSubtract: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isOutOfStock = product.trackStock && product.stock <= 0.0
    val isLowStock = product.trackStock && !isOutOfStock && product.stock <= product.minStock
    val hasCartQty = currentQtyInCart > 0.0

    // Orange highlighted frame and soft orange tint wrap if item is in cart
    val borderStrokeColor = if (hasCartQty) Color(0xFFF97316) else if (isLowStock) Color(0xFFFECACA) else Color(0xFFE2E8F0)
    val cardBackground = if (hasCartQty) Color(0xFFFFF7ED) else if (isOutOfStock) Color(0xFFF8FAFC) else if (isLowStock) Color(0xFFFEF2F2) else Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clickable(enabled = !isOutOfStock) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onAdd()
            }
            .shadow(
                elevation = 1.dp,
                shape = RoundedCornerShape(14.dp),
                clip = false,
                ambientColor = Color(0xFFF1F5F9),
                spotColor = Color(0xFF64748B).copy(alpha = 0.05f)
            ),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, borderStrokeColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left block: Name & Cost (No category name!)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFFF97316),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Rs. ${if (product.price % 1.0 == 0.0) product.price.toInt() else String.format("%.2f", product.price)}/${product.unit}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF1E293B)
                    )
                    
                    if (isOutOfStock) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFEE2E2))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("OUT OF STOCK", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        }
                    }
                }
            }

            // Right block: Compact Add/Subtract pill controller with single place quantity display
            if (!isOutOfStock) {
                if (currentQtyInCart > 0.0) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFF97316), RoundedCornerShape(20.dp))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                      ) {
                        // Minus Button
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFF7ED))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSubtract()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "−",
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFF97316),
                                fontSize = 14.sp
                            )
                        }

                        // Single display place for quantity inside the item box counter pill!
                        Text(
                            text = if (currentQtyInCart % 1.0 == 0.0) currentQtyInCart.toInt().toString() else String.format("%.3f", currentQtyInCart),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E293B)
                        )

                        // Plus Button
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF97316))
                                .clickable {
                                    if (!product.trackStock || currentQtyInCart < product.stock) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onAdd()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    // Click indicating plus action button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF7ED))
                            .border(1.dp, Color(0xFFF97316).copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            fontSize = 18.sp,
                            color = Color(0xFFF97316),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CheckoutSummaryPane(
    context: Context,
    state: BillingUiState,
    viewModel: BillingViewModel,
    isMobile: Boolean,
    onCloseDrawer: () -> Unit,
    onEditCustomer: () -> Unit,
    onEditDiscount: () -> Unit,
    onHoldClick: () -> Unit,
    onPrintClick: () -> Unit
) {
    val businessCategory by viewModel.businessCategory.collectAsStateWithLifecycle()
    var showStaffSelectDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Drawer Header Row with Uppercase tracking
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CURRENT ORDER",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = Color(0xFF1E293B)
            )

            if (isMobile) {
                IconButton(onClick = onCloseDrawer) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close Drawer", tint = Color(0xFF64748B))
                }
            }
        }

        // Active Cart Listing Slip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (state.cart.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Rounded.ShoppingCart, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("NO ITEMS IN CART", fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.cart) { item ->
                        CartItemRow(item = item, viewModel = viewModel)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // We wrap the entire bottom details panel in a highly responsive vertical scrollable container so it is visible and never overflows the screen!
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
        ) {
            // Invoice Cost Presentation Section inside a low-contrast envelope block
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Subtotal Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Items Subtotal", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                        Text("Rs. ${String.format("%.2f", state.subtotal)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    // Taxes (Applied rate slabs) (Orange line)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("+ Taxes (Applied rate slabs)", fontSize = 12.sp, color = Color(0xFFF97316), fontWeight = FontWeight.SemiBold)
                        Text("Rs. ${String.format("%.2f", state.calculatedTax)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF97316))
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    // Discount line (Emerald Green)
                    val activeDiscountVal = when (state.discountType) {
                        DiscountType.PERCENT -> state.subtotal * (state.discountValue / 100.0)
                        DiscountType.FIXED -> state.discountValue
                        else -> 0.0
                    }
                    if (activeDiscountVal > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("- Successful Reduction (Active discounts)", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold)
                            Text("- Rs. ${String.format("%.2f", activeDiscountVal)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Dashed separator Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .padding(vertical = 4.dp)
                    ) {
                        drawPath(
                            path = Path().apply {
                                moveTo(0f, 0f)
                                lineTo(size.width, 0f)
                            },
                            color = Color(0xFFE2E8F0),
                            style = Stroke(
                                width = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Grand total row
                    val grandPayableTotal = (state.subtotal + state.calculatedTax - activeDiscountVal).coerceAtLeast(0.0)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PAYABLE TOTAL", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFF0F172A))
                        Text(
                            text = "Rs. ${String.format("%.2f", grandPayableTotal)}",
                            fontWeight = FontWeight.W900,
                            fontSize = 24.sp, // Made slightly more compact to save important space
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = (-0.5).sp,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Compact Payment Method Picker (CASH, CARD, UPI, OTHERS)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PaymentMode.values().forEach { mode ->
                    val isSelected = state.paymentMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color(0xFF0F172A) else Color(0xFFF1F5F9)) // solid charcoal/black shape vs light grey
                            .border(1.dp, if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                            .clickable { viewModel.setPaymentMode(mode) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) Color.White else Color(0xFF475569) // white bold status vs grey text
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Additional Configurations Grid Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Apply Discount Button
                Button(
                    onClick = onEditDiscount,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF7ED), contentColor = Color(0xFFF97316)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    val reductionLabel = if (state.discountType != null) {
                        if (state.discountType == DiscountType.PERCENT) "${state.discountValue}% Off" else "Rs.${state.discountValue} Off"
                    } else "Discount"

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(imageVector = Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = reductionLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Customer Profile Select Button
                Button(
                    onClick = onEditCustomer,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF7ED), contentColor = Color(0xFFF97316)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = state.customerName ?: "Customer",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Staff Assignment Button (Service-Based custom)
                Button(
                    onClick = { showStaffSelectDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF7ED), contentColor = Color(0xFFF97316)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = state.assignedStaffName ?: "Staff",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Double Primary Decision Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Option A: Save & Hold Button
                if (businessCategory != "Grocery") {
                    Button(
                        onClick = {
                            if (state.cart.isNotEmpty()) onHoldClick()
                            else Toast.makeText(context, "Add items to cart", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF1F5F9), // Slate 100
                            contentColor = Color(0xFF1E293B)  // Slate 900
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Rounded.Pause, contentDescription = "Hold", modifier = Modifier.size(20.dp))
                            Text("HOLD TICKET", fontWeight = FontWeight.Black, fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }

                // Option B: Save & Print Button / Checkout
                Button(
                    onClick = {
                        if (state.cart.isNotEmpty()) {
                            if (state.paymentMode == PaymentMode.CREDIT && state.customerPhone.isNullOrBlank()) {
                                Toast.makeText(context, "Please select a Customer for Credit checkout", Toast.LENGTH_SHORT).show()
                            } else {
                                onPrintClick()
                            }
                        } else {
                            Toast.makeText(context, "Add items to cart", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF97316),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .shadow(elevation = 4.dp, shape = RoundedCornerShape(14.dp), clip = false),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = "Checkout", tint = Color.White, modifier = Modifier.size(20.dp))
                        Text("CHECKOUT", fontWeight = FontWeight.Black, color = Color.White, fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
        }

        if (showStaffSelectDialog) {
            Dialog(onDismissRequest = { showStaffSelectDialog = false }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "ASSIGN STAFF MEMBER",
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E293B),
                            fontSize = 18.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Select a staff member responsible for this order:",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 240.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (state.assignedStaffId == null) Color(0xFFFFF7ED) else Color(0xFFF8FAFC))
                                        .clickable {
                                            viewModel.setAssignedStaff(null, null)
                                            showStaffSelectDialog = false
                                        }
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "None (Unassigned)",
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.assignedStaffId == null) Color(0xFFF97316) else Color(0xFF1E293B)
                                    )
                                    if (state.assignedStaffId == null) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = Color(0xFFF97316),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            items(state.staffList) { staff ->
                                val isSelected = state.assignedStaffId == staff.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Color(0xFFFFF7ED) else Color(0xFFF8FAFC))
                                        .clickable {
                                            viewModel.setAssignedStaff(staff.id, staff.name)
                                            showStaffSelectDialog = false
                                        }
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = staff.name,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFFF97316) else Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = staff.role,
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = Color(0xFFF97316),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { showStaffSelectDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = Color(0xFF475569)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("CLOSE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    viewModel: BillingViewModel
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                Text(
                    text = item.product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF1E293B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Render selected variation and addons text if any
                val variationText = item.selectedVariation?.let { "• ${it.name}" } ?: ""
                val addonsText = if (item.selectedAddons.isNotEmpty()) "• Add-ons: " + item.selectedAddons.joinToString { it.name } else ""
                val customizationSummary = listOf(variationText, addonsText).filter { it.isNotBlank() }.joinToString(" ")
                if (customizationSummary.isNotBlank()) {
                    Text(
                        text = customizationSummary,
                        fontSize = 10.sp,
                        color = Color(0xFF4F46E5),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "Rs. ${if (item.singleUnitPrice % 1.0 == 0.0) item.singleUnitPrice.toInt() else String.format("%.2f", item.singleUnitPrice)}/${item.product.unit}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF64748B)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Calculated sub-cost (qty * price) in Monospace
                Text(
                    text = "Rs. ${if ((item.singleUnitPrice * item.quantity) % 1.0 == 0.0) (item.singleUnitPrice * item.quantity).toInt() else String.format("%.2f", item.singleUnitPrice * item.quantity)}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF0F172A)
                )

                // Quantity modifier controls matching the catalog page design exactly
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFF97316), RoundedCornerShape(20.dp))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Minus Button
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF7ED))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.removeFromCart(item.cartKey)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "−",
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFF97316),
                            fontSize = 14.sp
                        )
                    }

                    Text(
                        text = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else String.format("%.3f", item.quantity),
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = Color(0xFF1E293B)
                    )

                    // Plus Button
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF97316))
                            .clickable {
                                if (!item.product.trackStock || item.quantity < item.product.stock) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.addToCart(item.cartKey)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ProductModifierDialog(
    product: ProductEntity,
    allAddons: List<com.example.core.database.entity.AddonEntity>,
    allVariations: List<com.example.core.database.entity.VariationEntity>,
    onDismiss: () -> Unit,
    onAddToCart: (variationId: String?, addonIds: List<String>) -> Unit
) {
    val assocAddonIds = product.addonIds?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
    val assocVariationIds = product.variationIds?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()

    val fbMeta = product.metadata as? com.example.core.database.entity.IndustryMetadata.FoodAndBeverage

    val productAddons = remember(product, allAddons, fbMeta) {
        allAddons.filter { it.id in assocAddonIds }.map { addon ->
            val customPrice = fbMeta?.customAddonPrices?.get(addon.id)
            if (customPrice != null) addon.copy(price = customPrice) else addon
        }
    }

    val productVariations = remember(product, allVariations, fbMeta) {
        if (fbMeta != null && !product.variationIds.isNullOrBlank()) {
            val varSetId = product.variationIds.split(",").firstOrNull { it.isNotBlank() }
            val variationSet = allVariations.find { it.id == varSetId }
            if (variationSet != null) {
                variationSet.options.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .map { option ->
                        val customPrice = fbMeta.customVariationPrices[option] ?: 0.0
                        com.example.core.database.entity.VariationEntity(
                            id = option,
                            name = option,
                            price = customPrice
                        )
                    }
            } else {
                allVariations.filter { it.id in assocVariationIds }
            }
        } else {
            allVariations.filter { it.id in assocVariationIds }
        }
    }

    var selectedVariationId by remember(productVariations) { mutableStateOf<String?>(productVariations.firstOrNull()?.id) }
    var selectedAddonIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val basePrice = product.price
    val selectedVarPrice = productVariations.find { it.id == selectedVariationId }?.price ?: 0.0
    val selectedAddonsPrice = productAddons.filter { it.id in selectedAddonIds }.sumOf { it.price }
    val totalPrice = basePrice + selectedVarPrice + selectedAddonsPrice

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Header Block
                Text(
                    text = "CUSTOMIZE ITEM",
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = product.name,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B),
                    fontSize = 20.sp
                )
                Text(
                    text = "Base Price: Rs. ${product.price.toInt()}",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Variations mutual exclusion pills block
                if (productVariations.isNotEmpty()) {
                    Text(
                        text = "CHOOSE VARIATION",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569),
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        productVariations.forEach { variation ->
                            val isSelected = selectedVariationId == variation.id
                            val borderCol = if (isSelected) Color(0xFFF97316) else Color(0xFFCBD5E1)
                            val bgCol = if (isSelected) Color(0xFFFFF7ED) else Color.White
                            val textCol = if (isSelected) Color(0xFFF97316) else Color(0xFF334155)

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(bgCol)
                                    .border(1.dp, borderCol, RoundedCornerShape(20.dp))
                                    .clickable { selectedVariationId = variation.id }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = variation.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textCol
                                    )
                                    if (variation.price > 0) {
                                        Text(
                                            text = "(+Rs. ${variation.price.toInt()})",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (isSelected) Color(0xFFF97316) else Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Addons checkboxes block
                if (productAddons.isNotEmpty()) {
                    Text(
                        text = "ADD-ONS (Optional)",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569),
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(productAddons) { addon ->
                            val isChecked = selectedAddonIds.contains(addon.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isChecked) Color(0xFFF8FAFC) else Color.White)
                                    .clickable {
                                        selectedAddonIds = if (isChecked) selectedAddonIds - addon.id else selectedAddonIds + addon.id
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        selectedAddonIds = if (isChecked) selectedAddonIds - addon.id else selectedAddonIds + addon.id
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFF97316)),
                                    modifier = Modifier.scale(0.9f)
                                )
                                Text(
                                    text = addon.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "+Rs. ${addon.price.toInt()}",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Divider and Dynamic Total summary
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Customize Total Price",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF475569)
                    )
                    Text(
                        text = "Rs. ${totalPrice.toInt()}",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF0F172A)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Footer CTA Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onAddToCart(selectedVariationId, selectedAddonIds.toList())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("Add to Cart", color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun StaffSelectionDialog(
    product: ProductEntity,
    staffList: List<com.example.core.database.entity.StaffEntity>,
    onDismiss: () -> Unit,
    onStaffSelected: (com.example.core.database.entity.StaffEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Staff Member", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 18.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Who is executing \"${product.name}\"? This is required for staff commission payouts.",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                if (staffList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No active staff found.", color = Color(0xFF94A3B8), fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.heightIn(max = 240.dp)
                    ) {
                        items(staffList) { staff ->
                            Card(
                                onClick = { onStaffSelected(staff) },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("staff_select_${staff.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = staff.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "${staff.role} | Payout: ${staff.commissionRate.toInt()}%",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Select",
                                        tint = Color(0xFFF97316)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceCalendarView(
    onBookSlotClick: (Long) -> Unit,
    onResumeOrderClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: BillingViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedDayOffset by remember { mutableStateOf(0) } // 0: Today, 1: Tomorrow, 2: Day After
    
    val dayNames = listOf("Today", "Tomorrow", "Day After")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF0F172A)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Service Appointments",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Double-booking prevention engine",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            dayNames.forEachIndexed { index, name ->
                val isSelected = selectedDayOffset == index
                val labelCal = Calendar.getInstance()
                labelCal.add(Calendar.DAY_OF_YEAR, index)
                val dayStr = "${labelCal.get(Calendar.DAY_OF_MONTH)} ${labelCal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())}"
                
                Card(
                    onClick = { selectedDayOffset = index },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFFF97316) else Color.White
                    ),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFFF97316) else Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("day_tab_$index")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isSelected) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = dayStr,
                            fontSize = 12.sp,
                            color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color(0xFF64748B)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            val hoursList = (9..20).toList()
            
            items(hoursList) { hour ->
                val displayHourStr = when {
                    hour < 12 -> "${hour}:00 AM"
                    hour == 12 -> "12:00 PM"
                    else -> "${hour - 12}:00 PM"
                }

                val slotCal = Calendar.getInstance()
                slotCal.add(Calendar.DAY_OF_YEAR, selectedDayOffset)
                slotCal.set(Calendar.HOUR_OF_DAY, hour)
                slotCal.set(Calendar.MINUTE, 0)
                slotCal.set(Calendar.SECOND, 0)
                slotCal.set(Calendar.MILLISECOND, 0)
                val slotStartMillis = slotCal.timeInMillis

                val matchingBooking = state.orders.find { order ->
                    if (order.scheduledStartTime == null) return@find false
                    val ordCal = Calendar.getInstance()
                    ordCal.timeInMillis = order.scheduledStartTime
                    
                    val bookingDayMatches = ordCal.get(Calendar.DAY_OF_YEAR) == slotCal.get(Calendar.DAY_OF_YEAR) &&
                            ordCal.get(Calendar.YEAR) == slotCal.get(Calendar.YEAR)
                    val bookingHourMatches = ordCal.get(Calendar.HOUR_OF_DAY) == hour
                    
                    bookingDayMatches && bookingHourMatches
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayHourStr,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .width(80.dp)
                            .padding(end = 12.dp)
                    )

                    if (matchingBooking != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                            border = BorderStroke(1.dp, Color(0xFFFFEDD5)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("slot_booked_$hour")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFF97316), CircleShape)
                                                .size(8.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "RESERVED - Double Booking Guarded",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color(0xFFC2410C)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Client: ${matchingBooking.customerName ?: "Walk-in Guest"}",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp,
                                        color = Color(0xFFC2410C)
                                    )
                                    if (!matchingBooking.customerPhone.isNullOrBlank()) {
                                        Text(
                                            text = "Phone: ${matchingBooking.customerPhone}",
                                            fontSize = 13.sp,
                                            color = Color(0xFFC2410C).copy(alpha = 0.8f)
                                        )
                                    }
                                    Text(
                                        text = "Duration: ${matchingBooking.estimatedDuration ?: 60} mins | Bill: Rs. ${matchingBooking.totalAmount.toInt()}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF2563EB)
                                    )
                                }
                                Button(
                                    onClick = { onResumeOrderClick(matchingBooking.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Open", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        val isPastSlot = slotStartMillis < System.currentTimeMillis() - 3600000L
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPastSlot) Color(0xFFF1F5F9) else Color.White
                            ),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(12.dp),
                            onClick = { onBookSlotClick(slotStartMillis) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("slot_empty_$hour")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (isPastSlot) "Past Slot" else "Available Slot",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isPastSlot) Color(0xFF94A3B8) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Tap to book service",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Book",
                                    tint = if (isPastSlot) Color(0xFF94A3B8) else Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceHelpFullScreenDialog(onDismiss: () -> Unit) {
    var selectedLang by remember { mutableStateOf("English") }
    val langs = listOf("English", "Hindi", "Gujarati")

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF8FAFC)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                TopAppBar(
                    title = { Text("Voice Commands", fontWeight = FontWeight.Bold, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E293B))
                )

                // Language Selectors
                androidx.compose.material3.ScrollableTabRow(
                    selectedTabIndex = langs.indexOf(selectedLang),
                    containerColor = Color.White,
                    edgePadding = 16.dp,
                    indicator = { tabPositions ->
                        if (selectedLang in langs) {
                            androidx.compose.material3.TabRowDefaults.Indicator(
                                modifier = with(androidx.compose.material3.TabRowDefaults) {
                                    Modifier.tabIndicatorOffset(tabPositions[langs.indexOf(selectedLang)])
                                },
                                color = Color(0xFFF97316),
                                height = 3.dp
                            )
                        }
                    }
                ) {
                    langs.forEach { lang ->
                        androidx.compose.material3.Tab(
                            selected = selectedLang == lang,
                            onClick = { selectedLang = lang },
                            text = {
                                Text(
                                    text = lang,
                                    fontWeight = if (selectedLang == lang) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedLang == lang) Color(0xFFF97316) else Color(0xFF64748B)
                                )
                            }
                        )
                    }
                }

                // Instructions Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFF7ED)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Mic, contentDescription = null, tint = Color(0xFFF97316), modifier = Modifier.size(24.dp))
                        }
                        Text(
                            text = when (selectedLang) {
                                "Hindi" -> "हाथ-मुक्त वॉयस कमांड का उपयोग करें। बस माइक आइकन को टैप करें।"
                                "Gujarati" -> "હેન્ડ્સ-ફ્રી વૉઇસ કમાન્ડનો ઉપયોગ કરો. ફક્ત માઇક આઇકોન પર ટેપ કરો."
                                else -> "Use hands-free voice commands to quickly build and modify tickets. Simply tap the mic icon to activate the speech stream."
                            },
                            fontSize = 14.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    VoiceHelpSection(
                        step = "1",
                        title = when (selectedLang) {
                            "Hindi" -> "आइटम जोड़ना और संशोधित करना (ADDING ITEMS)"
                            "Gujarati" -> "વસ્તુઓ ઉમેરવી (ADDING ITEMS)"
                            else -> "ADDING & MODIFYING ITEMS"
                        },
                        examples = when (selectedLang) {
                            "Hindi" -> listOf("• \"Samosa plus five\" (समोसा प्लस पांच)", "• \"Add three coffee\" (तीन कॉफी जोड़ें)")
                            "Gujarati" -> listOf("• \"Samosa plus five\" (સમોસા પ્લસ પાંચ)", "• \"Add three coffee\" (ત્રણ કોફી ઉમેરો)")
                            else -> listOf("• \"Samosa plus five\"", "• \"Add three coffee\"")
                        }
                    )

                    VoiceHelpSection(
                        step = "2",
                        title = when (selectedLang) {
                            "Hindi" -> "आइटम घटाना और हटाना (REMOVING ITEMS)"
                            "Gujarati" -> "વસ્તુઓ દૂર કરવી (REMOVING ITEMS)"
                            else -> "SUBTRACTING & REMOVING"
                        },
                        examples = when (selectedLang) {
                            "Hindi" -> listOf("• \"Naan minus one\" (नान माइनस एक)", "• \"Remove five bread\" (पांच ब्रेड हटा दें)")
                            "Gujarati" -> listOf("• \"Naan minus one\" (નાન માઇનસ એક)", "• \"Remove five bread\" (પાંચ બ્રેડ દૂર કરો)")
                            else -> listOf("• \"Naan minus one\"", "• \"Remove five bread\"")
                        }
                    )

                    VoiceHelpSection(
                        step = "3",
                        title = when (selectedLang) {
                            "Hindi" -> "भुगतान और चेकआउट (PAYMENTS)"
                            "Gujarati" -> "ચુકવણી (PAYMENTS)"
                            else -> "PAYMENTS & CHECKOUT"
                        },
                        examples = when (selectedLang) {
                            "Hindi" -> listOf("• \"Pay by cash\" (नकद द्वारा भुगतान करें)", "• \"Hold order\" (ऑर्डर होल्ड करें)")
                            "Gujarati" -> listOf("• \"Pay by cash\" (રોકડ દ્વારા ચૂકવણી)", "• \"Hold order\" (ઓર્ડર હોલ્ડ કરો)")
                            else -> listOf("• \"Pay by cash\" / \"Pay by card\"", "• \"Hold order\"")
                        }
                    )

                    VoiceHelpSection(
                        step = "4",
                        title = when (selectedLang) {
                            "Hindi" -> "कार्ट खाली करें (CLEAR CART)"
                            "Gujarati" -> "કાર્ટ સાફ કરો (CLEAR CART)"
                            else -> "CLEAR CART"
                        },
                        examples = when (selectedLang) {
                            "Hindi" -> listOf("• \"Clear order\" (ऑर्डर खाली करें)")
                            "Gujarati" -> listOf("• \"Clear order\" (ઓર્ડર સાફ કરો)")
                            else -> listOf("• \"Clear order\" / \"Empty current cart\"")
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = when (selectedLang) { "Hindi" -> "शुरू करें"; "Gujarati" -> "શરૂ કરો"; else -> "Get Started" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceHelpSection(step: String, title: String, examples: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(24.dp).background(Color(0xFF1E293B), CircleShape), contentAlignment = Alignment.Center) {
                Text(step, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFF1E293B))
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp)
                .background(Color(0xFFFFF7ED), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFFED7AA), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            examples.forEach { example ->
                Text(example, fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = Color(0xFF7C2D12))
            }
        }
    }
}
