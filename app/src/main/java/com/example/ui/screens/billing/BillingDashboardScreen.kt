package com.example.ui.screens.billing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Description
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.database.entity.OrderEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BillingDashboardScreen(
    onNewOrderClick: () -> Unit,
    onNewOrderClickWithTable: (String) -> Unit = {},
    onResumeOrderClick: (String) -> Unit,
    viewModel: BillingViewModel = viewModel(
        factory = BillingViewModel.Factory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val heldList = state.heldOrders
    val businessCategory by viewModel.businessCategory.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("syncpos_prefs", android.content.Context.MODE_PRIVATE) }
    var tableCount by remember { mutableIntStateOf(prefs.getInt("table_count", 10)) }
    var showTableDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredHeldList = remember(heldList, searchQuery) {
        if (searchQuery.isBlank()) {
            heldList
        } else {
            heldList.filter { 
                (it.customerName ?: "").contains(searchQuery, ignoreCase = true)
            }
        }
    }

    if (showTableDialog) {
        var tempCount by remember { mutableStateOf(tableCount.toString()) }
        AlertDialog(
            onDismissRequest = { showTableDialog = false },
            title = { Text("Edit Table Count", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempCount,
                    onValueChange = { tempCount = it },
                    label = { Text("Number of Tables") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tc = tempCount.toIntOrNull() ?: tableCount
                        tableCount = tc
                        prefs.edit().putInt("table_count", tc).apply()
                        showTableDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316))
                ) { Text("Save", color = Color.White) }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC), // Primary Canvas Background: Soft slate off-white #F8FAFC
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewOrderClick,
                containerColor = Color(0xFFF97316), // Brand Accent: Bright Orange #F97316
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(bottom = 16.dp, end = 8.dp) // Placed wonderfully above bottom navigation
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Billing Session",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // 1. Header Layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Corner displays page title
                Column(modifier = Modifier.padding(end = 12.dp)) {
                    Text(
                        text = "BILLING",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.W900, // Heavy geometric sans-serif aesthetic
                        color = Color(0xFF1E293B), // #1E293B
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp)) // mt-4dp offset equivalent
                    Text(
                        text = "ONGOING ORDERS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B), // #64748B
                        letterSpacing = 2.sp // tracking-widest
                    )
                }
                
                // Search field on the right
                var showSearchField by remember { mutableStateOf(false) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.weight(1f)
                ) {
                    if (showSearchField) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search customer...", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF64748B)) },
                            trailingIcon = {
                                IconButton(onClick = { 
                                    searchQuery = ""
                                    showSearchField = false
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close search", modifier = Modifier.size(16.dp), tint = Color(0xFF64748B))
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFF97316),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                    } else {
                        IconButton(
                            onClick = { showSearchField = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search orders", tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            if (businessCategory == "F&B") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TABLES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 1.sp)
                    TextButton(
                        onClick = { showTableDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("edit", color = Color(0xFFF97316), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp)
                ) {
                    items(tableCount) { index ->
                        val tableId = "${index + 1}"
                        val isOngoing = heldList.any { it.tableNumber == tableId }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isOngoing) Color(0xFFD1FAE5) else Color.White)
                                .border(1.dp, if (isOngoing) Color(0xFFF97316) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                .clickable {
                                    val order = heldList.find { it.tableNumber == tableId }
                                    if (order != null) {
                                        onResumeOrderClick(order.id)
                                    } else {
                                        onNewOrderClickWithTable(tableId)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(tableId, fontWeight = FontWeight.Black, color = Color(0xFFF97316), fontSize = 16.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 2. Main Content (Conditional State Grid)
            if (filteredHeldList.isEmpty()) {
                // Empty State (No Held Bills)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f)
                            .shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp), clip = false),
                        shape = RoundedCornerShape(24.dp), // corner radius 24dp
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)) // bordered #F1F5F9
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Description, // FileText icon look
                                contentDescription = null,
                                tint = Color(0xFF64748B).copy(alpha = 0.3f), // opacity 30%, slate
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "NO MATCHING ORDERS" else "NO ONGOING ORDERS",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569), // #475569
                                letterSpacing = 1.sp, // bold tracking
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "Try refining your search terms." else "All clear! Tap the plus button at the bottom right to start a new billing session.",
                                fontSize = 14.sp,
                                color = Color(0xFF94A3B8), // centered #94A3B8
                                textAlign = TextAlign.Center,
                                modifier = Modifier.widthIn(max = 280.dp) // bounded max-width to avoid letter stretches
                             )
                        }
                    }
                }
            } else {
                // Active State (2x2 Grid Layout / grid-cols-2)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredHeldList) { order ->
                        HeldOrderGridItem(
                            order = order,
                            businessCategory = businessCategory,
                            viewModel = viewModel,
                            onClick = { onResumeOrderClick(order.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeldOrderGridItem(
    order: OrderEntity,
    businessCategory: String,
    viewModel: BillingViewModel,
    onClick: () -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val timeString = timeFormatter.format(Date(order.createdAt))

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val productMap = remember(state.products) { state.products.associateBy { it.id } }
    
    val orderItemsState = produceState<List<com.example.core.database.entity.OrderItemEntity>>(initialValue = emptyList(), order.id) {
        val result = viewModel.getOrderWithItems(order.id)
        value = result?.items ?: emptyList()
    }
    val orderItems = orderItemsState.value

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp), clip = false),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (businessCategory == "Service-Based") {
                // 1. Top of the Card: Customer name instead of Order ID
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = order.customerName ?: "Walk-in Customer",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Created at",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = timeString,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // 2. Middle of the Card: Order items and mobile number (with button to quickly copy the number)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (orderItems.isNotEmpty()) {
                        Text(
                            text = "ITEMS REQUIRED:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 1.sp
                        )
                        orderItems.forEach { item ->
                            val pName = productMap[item.productId]?.name ?: "Service Item"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "$pName x${item.quantity.toInt()}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF334155)
                                )
                                Text(
                                    text = "Rs. ${String.format("%.2f", item.price * item.quantity)}",
                                    fontSize = 14.sp,
                                    color = Color(0xFF64748B),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    if (!order.customerPhone.isNullOrBlank()) {
                        if (orderItems.isNotEmpty()) {
                            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "CUSTOMER PHONE:",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = order.customerPhone,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                            }

                            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                            val context = LocalContext.current

                            IconButton(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(order.customerPhone))
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    Toast.makeText(context, "Copied phone number!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy number",
                                    tint = Color(0xFFF97316),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                // 3. Bottom of the Card: Total amount and a Bill button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL AMOUNT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Rs. ${String.format("%.2f", order.totalAmount)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E293B),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Button(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Text("Bill", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            } else {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (businessCategory == "F&B") {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFEDD5), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "T ${order.tableNumber ?: "-"}",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF97316),
                                fontSize = 16.sp
                            )
                        }
                        
                        Text(
                            text = order.customerName ?: "Walk-in",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .weight(1f, fill = false),
                            textAlign = TextAlign.End
                        )
                    } else {
                        Text(
                            text = "Order #${order.displayId.ifBlank { order.id.takeLast(4) }}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Created at",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = timeString,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                if (businessCategory != "F&B" && (!order.customerName.isNullOrBlank() || !order.customerPhone.isNullOrBlank())) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        if (!order.customerName.isNullOrBlank()) {
                            Text(
                                text = order.customerName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                        }
                        if (!order.customerPhone.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = order.customerPhone,
                                fontSize = 14.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                if (businessCategory == "F&B") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rs. ${String.format("%.2f", order.totalAmount)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E293B),
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Button(
                            onClick = onClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Text("Bill", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "TOTAL DUE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Rs. ${String.format("%.2f", order.totalAmount)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFF97316),
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
