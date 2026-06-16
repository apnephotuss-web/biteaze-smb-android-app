package com.example.ui.screens.inventory

import android.Manifest
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.database.entity.ProductEntity
import com.example.core.voice.OperationMode
import com.example.core.voice.ParsedProductInput
import com.example.core.voice.VoiceCommandIntent
import com.example.ui.components.VoiceTriggerFAB
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.CanvasBackground
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.UUID
import kotlinx.coroutines.launch

// Aesthetic Palette
private val Slate50 = Color(0xFFF8FAFC)
private val Slate100 = Color(0xFFEDF2F7)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate800 = Color(0xFF1E293B)
private val OrangeAccent = Color(0xFFF97316)
private val IndigoAccent = Color(0xFFF97316)
private val LightIndigo = Color(0xFFFFF7ED)
private val LightRed = Color(0xFFFEF2F2)
private val RedAccent = Color(0xFFDC2626)
private val LightGreen = Color(0xFFF0FDF4)
private val GreenAccent = Color(0xFF16A34A)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel = viewModel(
        factory = InventoryViewModel.Factory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val products by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val addons by viewModel.addons.collectAsStateWithLifecycle()
    val variations by viewModel.variations.collectAsStateWithLifecycle()
    
    val viewScope = rememberCoroutineScope()
    
    var activeTab by remember { mutableStateOf("ITEMS") }
    var showAddModal by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var voicePrefills by remember { mutableStateOf<ParsedProductInput?>(null) }

    var showAddonModal by remember { mutableStateOf(false) }
    var addonToEdit by remember { mutableStateOf<com.example.core.database.entity.AddonEntity?>(null) }

    var showVariationModal by remember { mutableStateOf(false) }
    var variationToEdit by remember { mutableStateOf<com.example.core.database.entity.VariationEntity?>(null) }
    var showStaffModal by remember { mutableStateOf(false) }
    
    val currentCategory by viewModel.businessCategory.collectAsStateWithLifecycle()
    val expiryWarningDays by viewModel.expiryWarningDays.collectAsStateWithLifecycle()
    val isGrocery = currentCategory == "Grocery"
    
    // Automatically switch to ITEMS tab if Grocery is selected while on another tab
    LaunchedEffect(isGrocery, activeTab) {
        if (isGrocery && activeTab != "ITEMS" && activeTab != "LOOSE ITEMS") {
            activeTab = "ITEMS"
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    
    val context = LocalContext.current

    val productsFiltered = remember(products, searchQuery) {
        if (searchQuery.isBlank()) {
            products
        } else {
            products.filter { prod ->
                prod.name.contains(searchQuery, ignoreCase = true) ||
                prod.category.contains(searchQuery, ignoreCase = true) ||
                prod.sku.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val addonsFiltered = remember(addons, searchQuery) {
        if (searchQuery.isBlank()) {
            addons
        } else {
            addons.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    val variationsFiltered = remember(variations, searchQuery) {
        if (searchQuery.isBlank()) {
            variations
        } else {
            variations.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    // Passive ripple animation for voice recording trigger
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
    val alphaPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Scaffold(
        containerColor = Slate50,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Area A: Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Block
                Column {
                    Text(
                        text = if (currentCategory == "Service-Based") "SERVICES" else "INVENTORY",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate800,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (currentCategory == "Service-Based") "MANAGE SERVICES" else "MANAGE CATALOG & STOCK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 1.sp
                    )
                }

                // Right Block (Action Buttons Row)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Voice search/command
                    VoiceTriggerFAB(mode = OperationMode.INVENTORY_MODE) { intent ->
                        when(intent) {
                            is VoiceCommandIntent.QuickRegisterProduct -> {
                                voicePrefills = ParsedProductInput(
                                    item = intent.name,
                                    sku = intent.sku,
                                    price = intent.price,
                                    stock = intent.stock,
                                    unit = intent.unit,
                                    category = intent.category
                                )
                                editingProduct = null
                                showAddModal = true
                                Toast.makeText(context, "Prefilled voice input!", Toast.LENGTH_SHORT).show()
                            }
                            is VoiceCommandIntent.UpdateProductStock -> {
                                val prod = products.find { it.id == intent.productCode }
                                if (prod != null) {
                                    if (intent.absoluteValue != null) {
                                        viewModel.updateProductStock(prod.id, intent.absoluteValue)
                                    } else if (intent.addDelta != null) {
                                        viewModel.updateProductStock(prod.id, prod.stock + intent.addDelta)
                                    }
                                    Toast.makeText(context, "Stock updated to ${intent.absoluteValue ?: (prod.stock + (intent.addDelta ?: 0.0))}", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Product not found", Toast.LENGTH_SHORT).show()
                                }
                            }
                            is VoiceCommandIntent.UpdateProductMetadata -> {
                                val prod = products.find { it.id == intent.productCode }
                                if (prod != null) {
                                    if (intent.price != null) {
                                        viewModel.saveProduct(prod.copy(price = intent.price, lastUpdated = System.currentTimeMillis()))
                                        Toast.makeText(context, "${prod.name} price updated to ${intent.price}", Toast.LENGTH_SHORT).show()
                                    }
                                    if (intent.minStock != null) {
                                        viewModel.updateProductMinStock(prod.id, intent.minStock)
                                        Toast.makeText(context, "${prod.name} safety stock limit set to ${intent.minStock}", Toast.LENGTH_SHORT).show()
                                    }
                                    if (intent.unit != null) {
                                        viewModel.updateProductUnit(prod.id, intent.unit)
                                        Toast.makeText(context, "${prod.name} unit updated to ${intent.unit}", Toast.LENGTH_SHORT).show()
                                    }
                                    if (intent.isFavorite != null) {
                                        viewModel.updateProductFavorite(prod.id, intent.isFavorite)
                                        Toast.makeText(context, "${prod.name} favorite set to ${intent.isFavorite}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Product not found", Toast.LENGTH_SHORT).show()
                                }
                            }
                            is VoiceCommandIntent.Unrecognized -> {
                                Toast.makeText(context, "Unrecognized command: \${intent.rawText}", Toast.LENGTH_LONG).show()
                            }
                            else -> {}
                        }
                    }

                    // ADD ITEM CTA
                    Button(
                        onClick = {
                            when (activeTab) {
                                "ITEMS" -> {
                                    editingProduct = null
                                    voicePrefills = null
                                    showAddModal = true
                                }
                                "ADDONS" -> {
                                    addonToEdit = null
                                    showAddonModal = true
                                }
                                "VARIATIONS" -> {
                                    variationToEdit = null
                                    showVariationModal = true
                                }
                                "STAFF" -> {
                                    showStaffModal = true
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(48.dp),
                        contentPadding = PaddingValues(horizontal = if (currentCategory == "F&B") 12.dp else 20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(18.dp))
                            Text(
                                text = when (activeTab) {
                                    "ITEMS" -> "ADD ITEM"
                                    "LOOSE ITEMS" -> "ADD LOOSE ITEM"
                                    "ADDONS" -> "ADD ADD-ON"
                                    "STAFF" -> "ADD STAFF"
                                    else -> "ADD VARIATION"
                                },
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 0.25.sp,
                                fontSize = if (currentCategory == "F&B") 10.sp else 13.sp
                            )
                        }
                    }
                }
            }

            // Segmented Navigation Tab Controller
            val availableTabs = when (currentCategory) {
                "Grocery" -> listOf("ITEMS", "LOOSE ITEMS")
                "Service-Based" -> listOf("ITEMS", "STAFF")
                else -> listOf("ITEMS", "ADDONS", "VARIATIONS")
            }
            if (availableTabs.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .background(Slate100, RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    availableTabs.forEach { tab ->
                        val isSelected = activeTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color.White else Color.Transparent)
                                .clickable { 
                                    activeTab = tab 
                                    searchQuery = "" // Clear search queries on tab switch
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = when (tab) {
                                        "ITEMS" -> Icons.Default.Category
                                        "LOOSE ITEMS" -> Icons.Default.Widgets
                                        "ADDONS" -> Icons.Default.AddCircleOutline
                                        "STAFF" -> Icons.Default.People
                                        else -> Icons.Default.Checklist
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) IndigoAccent else Slate500,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = tab,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) IndigoAccent else Slate500,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, SKU, or category...", color = Slate500, fontSize = 13.sp) },
                prefix = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp).padding(end = 4.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangeAccent,
                    unfocusedBorderColor = Slate100,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = OrangeAccent
                )
            )

            // Area B: Persistent Product Inventory Grid / Table nested inside elegant Rounded Surface Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp), clip = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState())
                ) {
                    val currentProducts = remember(productsFiltered, activeTab, isGrocery) {
                        if (isGrocery) {
                            if (activeTab == "ITEMS") {
                                productsFiltered.filter { (it.metadata as? com.example.core.database.entity.IndustryMetadata.Grocery)?.isFractional != true }
                            } else if (activeTab == "LOOSE ITEMS") {
                                productsFiltered.filter { (it.metadata as? com.example.core.database.entity.IndustryMetadata.Grocery)?.isFractional == true }
                            } else {
                                productsFiltered
                            }
                        } else {
                            productsFiltered
                        }
                    }

                    Column(
                        modifier = Modifier
                            .widthIn(min = 840.dp)
                            .fillMaxSize()
                    ) {
                        // Condition check for active tab table render
                        if (activeTab == "ITEMS" || activeTab == "LOOSE ITEMS") {
                            // Table Headers
                            TableHeaderRow(isServiceBased = currentCategory == "Service-Based")
                            HorizontalDivider(color = Slate100, thickness = 1.dp)

                            if (currentProducts.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Category,
                                            contentDescription = null,
                                            tint = Slate400,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = if (activeTab == "LOOSE ITEMS") "No loose products in inventory" else "No products in inventory",
                                            fontWeight = FontWeight.Bold,
                                            color = Slate500
                                        )
                                        Text(
                                            text = if (activeTab == "LOOSE ITEMS") "Click ADD LOOSE ITEM to get started" else "Click ADD ITEM to get started",
                                            fontSize = 12.sp,
                                            color = Slate400
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    itemsIndexed(currentProducts, key = { _, v -> v.id }) { index, product ->
                                        val bgColor = if (index % 2 == 1) Color(0xFFFFF7ED).copy(alpha = 0.45f) else Color.White
                                        TableProductRow(
                                            product = product,
                                            backgroundColor = bgColor,
                                            isGrocery = isGrocery,
                                            isServiceBased = currentCategory == "Service-Based",
                                            expiryWarningDays = expiryWarningDays,
                                            onEdit = {
                                                if (product.id.contains("__var__")) {
                                                    val parentId = product.id.substringBefore("__var__")
                                                    viewScope.launch {
                                                        val parent = viewModel.getProductById(parentId)
                                                        editingProduct = parent
                                                        voicePrefills = null
                                                        showAddModal = true
                                                    }
                                                } else {
                                                    editingProduct = product
                                                    voicePrefills = null
                                                    showAddModal = true
                                                }
                                            },
                                            onDelete = {
                                                if (product.id.contains("__var__")) {
                                                    val parentId = product.id.substringBefore("__var__")
                                                    val varId = product.id.substringAfter("__var__")
                                                    viewScope.launch {
                                                        val parent = viewModel.getProductById(parentId)
                                                        if (parent != null) {
                                                             val meta = parent.metadata as? com.example.core.database.entity.IndustryMetadata.Grocery
                                                             if (meta != null) {
                                                                 val updatedVariations = meta.variations.filter { it.id != varId }
                                                                 val updatedParent = parent.copy(
                                                                     metadata = meta.copy(variations = updatedVariations),
                                                                     lastUpdated = System.currentTimeMillis()
                                                                 )
                                                                 viewModel.saveProduct(updatedParent)
                                                                 Toast.makeText(context, "Variation deleted", Toast.LENGTH_SHORT).show()
                                                             }
                                                        }
                                                    }
                                                } else {
                                                    viewModel.deleteProduct(product.id)
                                                    Toast.makeText(context, "${product.name} deleted", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            onStockChange = { qty ->
                                                 if (product.id.contains("__var__")) {
                                                     val parentId = product.id.substringBefore("__var__")
                                                     val varId = product.id.substringAfter("__var__")
                                                     viewScope.launch {
                                                         val parent = viewModel.getProductById(parentId)
                                                         if (parent != null) {
                                                             val meta = parent.metadata as? com.example.core.database.entity.IndustryMetadata.Grocery
                                                             if (meta != null) {
                                                                 val updatedVars = meta.variations.map { v ->
                                                                     if (v.id == varId) v.copy(stock = qty) else v
                                                                 }
                                                                 viewModel.saveProduct(parent.copy(metadata = meta.copy(variations = updatedVars)))
                                                             }
                                                         }
                                                     }
                                                 } else {
                                                     viewModel.updateProductStock(product.id, qty)
                                                 }
                                             },
                                            onMinQtyChange = { qty ->
                                                val realId = if (product.id.contains("__var__")) product.id.substringBefore("__var__") else product.id
                                                viewModel.updateProductMinStock(realId, qty)
                                            },
                                            onUnitChange = { unit ->
                                                val realId = if (product.id.contains("__var__")) product.id.substringBefore("__var__") else product.id
                                                viewModel.updateProductUnit(realId, unit)
                                            },
                                            onFavChange = { isFav ->
                                                val realId = if (product.id.contains("__var__")) product.id.substringBefore("__var__") else product.id
                                                viewModel.updateProductFavorite(realId, isFav)
                                            }
                                        )
                                        HorizontalDivider(color = Slate100, thickness = 1.dp)
                                    }
                                }
                            }
                        } else if (activeTab == "ADDONS") {
                            TableHeaderAddonRow()
                            HorizontalDivider(color = Slate100, thickness = 1.dp)

                            if (addonsFiltered.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.AddCircleOutline,
                                            contentDescription = null,
                                            tint = Slate400,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("No add-ons configured", fontWeight = FontWeight.Bold, color = Slate500)
                                        Text("Click ADD ADD-ON to create modifier upcharges", fontSize = 12.sp, color = Slate400)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    itemsIndexed(addonsFiltered, key = { _, v -> v.id }) { index, addon ->
                                        val bgColor = if (index % 2 == 1) Color(0xFFFFF7ED).copy(alpha = 0.45f) else Color.White
                                        TableAddonRow(
                                            addon = addon,
                                            backgroundColor = bgColor,
                                            onEdit = {
                                                addonToEdit = addon
                                                showAddonModal = true
                                            },
                                            onDelete = {
                                                viewModel.deleteAddon(addon.id)
                                                Toast.makeText(context, "${addon.name} deleted", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        HorizontalDivider(color = Slate100, thickness = 1.dp)
                                    }
                                }
                            }
                        } else if (activeTab == "VARIATIONS") {
                            TableHeaderVariationRow()
                            HorizontalDivider(color = Slate100, thickness = 1.dp)

                            if (variationsFiltered.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Checklist,
                                            contentDescription = null,
                                            tint = Slate400,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("No variations configured", fontWeight = FontWeight.Bold, color = Slate500)
                                        Text("Click ADD VARIATION to create menu customization options", fontSize = 12.sp, color = Slate400)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    itemsIndexed(variationsFiltered, key = { _, v -> v.id }) { index, variation ->
                                        val bgColor = if (index % 2 == 1) Color(0xFFFFF7ED).copy(alpha = 0.45f) else Color.White
                                        TableVariationRow(
                                            variation = variation,
                                            backgroundColor = bgColor,
                                            onEdit = {
                                                variationToEdit = variation
                                                showVariationModal = true
                                            },
                                            onDelete = {
                                                viewModel.deleteVariation(variation.id)
                                                Toast.makeText(context, "${variation.name} deleted", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        HorizontalDivider(color = Slate100, thickness = 1.dp)
                                    }
                                }
                            }
                        } else if (activeTab == "STAFF") {
                            TableHeaderStaffRow()
                            HorizontalDivider(color = Slate100, thickness = 1.dp)

                            val staffList by viewModel.staff.collectAsStateWithLifecycle()
                            val staffFiltered = remember(staffList, searchQuery) {
                                if (searchQuery.isBlank()) staffList else staffList.filter { it.name.contains(searchQuery, ignoreCase = true) || it.role.contains(searchQuery, ignoreCase = true) }
                            }

                            if (staffFiltered.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.People,
                                            contentDescription = null,
                                            tint = Slate400,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("No staff members found", fontWeight = FontWeight.Bold, color = Slate500)
                                        Text("Click ADD STAFF to create a new staff profile", fontSize = 12.sp, color = Slate400)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    itemsIndexed(staffFiltered, key = { _, s -> s.id }) { index, s ->
                                        val bgColor = if (index % 2 == 1) Color(0xFFFFF7ED).copy(alpha = 0.45f) else Color.White
                                        TableStaffRow(
                                            staff = s,
                                            backgroundColor = bgColor,
                                            onDelete = {
                                                viewModel.deleteStaff(s.id)
                                                Toast.makeText(context, "${s.name} deleted", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        HorizontalDivider(color = Slate100, thickness = 1.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active listing voice popup indicator

        // Add/Edit Popup modal bottom sheet dialogue
        if (showAddModal) {
            InventoryAddEditDialog(
                editingProduct = editingProduct,
                prefills = voicePrefills,
                categories = categories,
                allAddons = addons,
                allVariations = variations,
                isGrocery = isGrocery,
                businessCategory = currentCategory,
                defaultSellAsLoose = (activeTab == "LOOSE ITEMS"),
                onDismiss = { showAddModal = false },
                onSave = { entity ->
                    viewModel.saveProduct(entity)
                    showAddModal = false
                    Toast.makeText(context, if (editingProduct == null) "Item Added" else "Item Updated", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showAddonModal) {
            AddonAddEditDialog(
                addon = addonToEdit,
                onDismiss = { showAddonModal = false },
                onSave = { entity ->
                    viewModel.saveAddon(entity)
                    showAddonModal = false
                    Toast.makeText(context, if (addonToEdit == null) "Add-on Added" else "Add-on Updated", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showVariationModal) {
            VariationAddEditDialog(
                variation = variationToEdit,
                isFnB = (currentCategory == "F&B"),
                onDismiss = { showVariationModal = false },
                onSave = { entity ->
                    viewModel.saveVariation(entity)
                    showVariationModal = false
                    Toast.makeText(context, if (variationToEdit == null) "Variation Added" else "Variation Updated", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showStaffModal) {
            StaffAddDialog(
                onDismiss = { showStaffModal = false },
                onSave = { entity ->
                    viewModel.saveStaff(entity)
                    showStaffModal = false
                    Toast.makeText(context, "Staff member added", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun TableHeaderRow(isServiceBased: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate50)
            .padding(vertical = 14.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(50.dp)) {
            Text("FAV", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500, letterSpacing = 1.sp)
        }
        Box(modifier = Modifier.width(200.dp)) {
            Text("ITEM name & category", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500, letterSpacing = 1.sp)
        }
        if (!isServiceBased) {
            Box(modifier = Modifier.width(110.dp)) {
                Text("STOCK QTY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500, letterSpacing = 1.sp)
            }
            Box(modifier = Modifier.width(110.dp)) {
                Text("UNIT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500, letterSpacing = 1.sp)
            }
            Box(modifier = Modifier.width(100.dp)) {
                Text("MIN QTY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500, letterSpacing = 1.sp)
            }
        }
        Box(modifier = Modifier.width(100.dp)) {
            Text("PRICE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500, letterSpacing = 1.sp)
        }
        if (!isServiceBased) {
            Box(modifier = Modifier.width(80.dp)) {
                Text("TAX SLAB", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500, letterSpacing = 1.sp)
            }
        }
        Box(modifier = Modifier.width(90.dp)) {
            Text("ACTIONS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun TableProductRow(
    product: ProductEntity,
    backgroundColor: Color = Color.White,
    isGrocery: Boolean = false,
    isServiceBased: Boolean = false,
    expiryWarningDays: Int = 30,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStockChange: (Double) -> Unit,
    onMinQtyChange: (Double) -> Unit,
    onUnitChange: (String) -> Unit,
    onFavChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // FAV Amber Star Column
        Box(modifier = Modifier.width(50.dp)) {
            IconButton(
                onClick = { onFavChange(!product.isFavorite) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (product.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = "Toggle Favorite",
                    tint = if (product.isFavorite) Color(0xFFF59E0B) else Slate400,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // ITEM Name & Category
        Box(modifier = Modifier.width(200.dp).padding(end = 12.dp)) {
            Column {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Slate800,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(LightIndigo)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = product.category.uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = IndigoAccent
                        )
                    }
                    
                    if (isGrocery) {
                        val meta = product.metadata as? com.example.core.database.entity.IndustryMetadata.Grocery
                        if (meta != null && meta.batches.isNotEmpty()) {
                            val now = System.currentTimeMillis()
                            val thresholdMs = expiryWarningDays * 24L * 60 * 60 * 1000
                            val expiredCount = meta.batches.count { it.expiryDate <= now }
                            val warningCount = meta.batches.count { it.expiryDate > now && (it.expiryDate - now) <= thresholdMs }
                            
                            if (expiredCount > 0 || warningCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (expiredCount > 0) Color(0xFFFEE2E2) else Color(0xFFFEF3C7))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (expiredCount > 0) "$expiredCount Expired!" else "$warningCount near Expiry",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = if (expiredCount > 0) Color(0xFFDC2626) else Color(0xFFD97706)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!isServiceBased) {
            // STOCK QTY
            Box(modifier = Modifier.width(110.dp)) {
                val lowLimit = if (product.minStock > 0.0) product.minStock else 8.0
                val isLowStock = product.stock <= lowLimit
                
                val hasBatches = isGrocery && (product.metadata as? com.example.core.database.entity.IndustryMetadata.Grocery)?.batches?.isNotEmpty() == true
                
                if (hasBatches) {
                    Text(
                        text = product.stock.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLowStock) Color(0xFFEF4444) else Slate800,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                } else {
                    InlineEditableNumberCell(
                        value = product.stock,
                        onValueChange = onStockChange,
                        isWarning = isLowStock,
                        useColorSchemes = true
                    )
                }
            }

            // UNIT
            Box(modifier = Modifier.width(110.dp)) {
                InlineUnitDropdownSelector(
                    unit = product.unit,
                    onUnitSelect = onUnitChange
                )
            }

            // MIN QTY
            Box(modifier = Modifier.width(100.dp)) {
                InlineEditableNumberCell(
                    value = product.minStock,
                    onValueChange = onMinQtyChange,
                    isWarning = false,
                    useColorSchemes = false
                )
            }
        }

        // PRICE View
        Box(modifier = Modifier.width(100.dp)) {
            val priceStr = if (product.price % 1.0 == 0.0) product.price.toInt().toString() else String.format("%.2f", product.price)
            val formatStr = if (isServiceBased) "Rs. $priceStr" else "Rs. $priceStr/${product.unit}"
            Text(
                text = formatStr,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = Slate800,
                letterSpacing = (-0.5).sp
            )
        }

        if (!isServiceBased) {
            // TAX SLAB
            Box(modifier = Modifier.width(80.dp)) {
                Text(
                    text = "${product.taxRate.toInt()}%",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = Slate500
                )
            }
        }

        // ACTIONS column
        Box(modifier = Modifier.width(90.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit Product",
                        tint = Slate400,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                // Keep a secure Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete Product",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// Inline Editable Badge that shifts instantly on tap
@Composable
fun InlineEditableNumberCell(
    value: Double,
    onValueChange: (Double) -> Unit,
    isWarning: Boolean,
    useColorSchemes: Boolean
) {
    var isEditing by remember { mutableStateOf(false) }
    var rawText by remember(value) { mutableStateOf(if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()) }
    val focusRequester = remember { FocusRequester() }

    // Pulsing background when low count limit warning
    val lowStockTransition = rememberInfiniteTransition(label = "stock_warning")
    val transitionAlpha by lowStockTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    if (isEditing) {
        BasicTextField(
            value = rawText,
            onValueChange = { rawText = it },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    val finalVal = rawText.toDoubleOrNull() ?: value
                    onValueChange(finalVal)
                    isEditing = false
                }
            ),
            modifier = Modifier
                .width(54.dp)
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(2.dp, OrangeAccent, RoundedCornerShape(8.dp))
                .padding(vertical = 6.dp, horizontal = 4.dp)
                .focusRequester(focusRequester)
                .onFocusChanged {
                    if (!it.isFocused) {
                        val finalVal = rawText.toDoubleOrNull() ?: value
                        onValueChange(finalVal)
                        isEditing = false
                    }
                },
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = Slate800
            ),
            singleLine = true
        )
        
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    } else {
        val backgroundTint = when {
            !useColorSchemes -> Color(0xFFF1F5F9)
            isWarning -> LightRed.copy(alpha = transitionAlpha)
            else -> LightGreen
        }
        val textColor = when {
            !useColorSchemes -> Slate500
            isWarning -> RedAccent
            else -> GreenAccent
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundTint)
                .clickable { isEditing = true }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (value % 1.0 == 0.0) value.toInt().toString() else String.format("%.3f", value),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = textColor
            )
        }
    }
}

@Composable
fun InlineUnitDropdownSelector(
    unit: String,
    onUnitSelect: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val units = listOf("pcs", "kg", "g", "liters", "ml", "box", "pack")

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(1.dp, Slate100, RoundedCornerShape(8.dp))
                .clickable { isExpanded = true }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = unit,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Slate800
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Units menu",
                tint = Slate500,
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            units.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 13.sp) },
                    onClick = {
                        onUnitSelect(option)
                        isExpanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryAddEditDialog(
    editingProduct: ProductEntity?,
    prefills: ParsedProductInput?,
    categories: List<String>,
    allAddons: List<com.example.core.database.entity.AddonEntity>,
    allVariations: List<com.example.core.database.entity.VariationEntity>,
    isGrocery: Boolean,
    businessCategory: String,
    defaultSellAsLoose: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    val context = LocalContext.current
    
    // Local State prefills (from editing entity or speech input parsed details)
    var itemName by remember { mutableStateOf(prefills?.item ?: editingProduct?.name ?: "") }
    var priceStr by remember { mutableStateOf(prefills?.price?.toString() ?: editingProduct?.price?.toString() ?: "") }
    var trackStock by remember { mutableStateOf(prefills?.stock != null || (editingProduct?.trackStock ?: false)) }
    var stockStr by remember { mutableStateOf(prefills?.stock?.toString() ?: (if (editingProduct?.trackStock == true) editingProduct.stock.toString() else "")) }
    var unitStr by remember { mutableStateOf(prefills?.unit ?: editingProduct?.unit ?: (if (defaultSellAsLoose) "kg" else "pcs")) }
    var minQtyStr by remember { mutableStateOf(prefills?.minCount?.toString() ?: (if (editingProduct?.trackStock == true) editingProduct.minStock.toString() else "5")) }
    var categoryStr by remember { mutableStateOf(prefills?.category ?: editingProduct?.category ?: "") }
    var taxRateStr by remember { mutableStateOf(prefills?.taxRate?.toInt()?.toString() ?: editingProduct?.taxRate?.toInt()?.toString() ?: "0") }
    var isFavorite by remember { mutableStateOf(prefills?.isFavorite ?: editingProduct?.isFavorite ?: false) }
    var sellAsLoose by remember { mutableStateOf((editingProduct?.metadata as? com.example.core.database.entity.IndustryMetadata.Grocery)?.isFractional ?: defaultSellAsLoose) }
    
    var currentBatches by remember { 
        mutableStateOf(
            (editingProduct?.metadata as? com.example.core.database.entity.IndustryMetadata.Grocery)?.batches ?: emptyList()
        )
    }
    
    var customGroceryVariations by remember {
        mutableStateOf(
            (editingProduct?.metadata as? com.example.core.database.entity.IndustryMetadata.Grocery)?.variations ?: emptyList()
        )
    }

    var newVarName by remember { mutableStateOf("") }
    var newVarPriceStr by remember { mutableStateOf("") }
    var newVarStockStr by remember { mutableStateOf("") }
    
    // Auto-calculate stock if grocery batches are present
    LaunchedEffect(currentBatches) {
        if (isGrocery && currentBatches.isNotEmpty()) {
            stockStr = currentBatches.sumOf { it.quantity }.toString()
        }
    }

    var selectedAddons by remember { mutableStateOf(editingProduct?.addonIds?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()) }
    var selectedVariations by remember { mutableStateOf(editingProduct?.variationIds?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()) }

    // Custom pricing states for F&B
    val existingFbMeta = editingProduct?.metadata as? com.example.core.database.entity.IndustryMetadata.FoodAndBeverage
    var customAddonPrices by remember {
        mutableStateOf<Map<String, Double>>(
            existingFbMeta?.customAddonPrices ?: emptyMap()
        )
    }
    var customVariationPrices by remember {
        mutableStateOf<Map<String, Double>>(
            existingFbMeta?.customVariationPrices ?: emptyMap()
        )
    }
    var selectedVariationSetId by remember {
        mutableStateOf(
            if (businessCategory == "F&B") {
                editingProduct?.variationIds ?: ""
            } else ""
        )
    }

    var autocompleteExpanded by remember { mutableStateOf(false) }

    val filteredCategorySuggestions = remember(categoryStr, categories) {
        if (categoryStr.isBlank()) emptyList()
        else categories.filter { it.contains(categoryStr, ignoreCase = true) && !it.equals(categoryStr, ignoreCase = true) }
    }

    val dialogFocusManager = LocalFocusManager.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxSize()
        ) {
            Column {
                // Header Drawer Block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate50)
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = null,
                                tint = OrangeAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = if (editingProduct == null) "New Item" else "Edit Item",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = Slate800
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .background(Color(0xFFEDF2F7), shape = CircleShape)
                                .size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close overlay",
                                tint = Slate500,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Form Details
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .weight(1f)
                        .padding(horizontal = 24.dp)
                ) {
                    // Item Title Input
                    Text("Product Title", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate500)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        placeholder = { Text("e.g. Multigrain Bread", color = Slate400, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeAccent,
                            unfocusedBorderColor = Slate100,
                            cursorColor = OrangeAccent
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Financials & Quantities Grid (2 Columns, 2 Rows Grid in standard Rows & Columns)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Price (Col 1)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Price (Rs.)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate500)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = priceStr,
                                onValueChange = { priceStr = it },
                                placeholder = { Text("0.00", color = Slate400) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangeAccent,
                                    unfocusedBorderColor = Slate100,
                                    cursorColor = OrangeAccent
                                )
                            )
                        }
                        if (businessCategory != "Service-Based" && trackStock) {
                            // Stock (Col 2)
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Stock Count", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate500)
                                    if (isGrocery && currentBatches.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = Slate400, modifier = Modifier.size(12.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = stockStr,
                                    onValueChange = { stockStr = it },
                                    placeholder = { Text("0", color = Slate400) },
                                    readOnly = isGrocery && currentBatches.isNotEmpty(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = OrangeAccent,
                                        unfocusedBorderColor = Slate100,
                                        cursorColor = OrangeAccent,
                                        disabledBorderColor = Slate100,
                                        disabledTextColor = Slate500
                                    )
                                )
                            }
                        } else if (businessCategory != "Service-Based") {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    if (businessCategory != "Service-Based") {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Slate50)
                                .clickable { trackStock = !trackStock }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Track Stock Level", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate800)
                                Text("Enable physical stock countdown and threshold alerts", fontSize = 11.sp, color = Slate500)
                            }
                            Switch(
                                checked = trackStock,
                                onCheckedChange = { trackStock = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = OrangeAccent,
                                    uncheckedThumbColor = Slate400,
                                    uncheckedTrackColor = Slate100
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (businessCategory != "Service-Based") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Unit Selector Dropdown (Col 1)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Unit of Sale", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate500)
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                var unitDropdownExpanded by remember { mutableStateOf(false) }
                                val unitOptions = listOf("pcs", "kg", "g", "liters", "ml", "box", "pack")
                                
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = unitStr,
                                        onValueChange = {},
                                        readOnly = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { unitDropdownExpanded = true },
                                        enabled = false, // Keep click trigger through disabled + clickable wrapper
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            disabledTextColor = Slate800,
                                            disabledBorderColor = Slate100,
                                            disabledTrailingIconColor = Slate500
                                        ),
                                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
                                    )
                                    // Surface overlays the focus field so click matches perfectly
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clickable { unitDropdownExpanded = true }
                                    )

                                    DropdownMenu(
                                        expanded = unitDropdownExpanded,
                                        onDismissRequest = { unitDropdownExpanded = false }
                                    ) {
                                        unitOptions.forEach { opt ->
                                            DropdownMenuItem(
                                                text = { Text(opt) },
                                                onClick = {
                                                    unitStr = opt
                                                    unitDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            // Min Qty Trigger Warning Stock (Col 2)
                            if (trackStock) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Minimum Stock Threshold", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate500)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = minQtyStr,
                                        onValueChange = { minQtyStr = it },
                                        placeholder = { Text("5", color = Slate400) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = OrangeAccent,
                                            unfocusedBorderColor = Slate100,
                                            cursorColor = OrangeAccent
                                        )
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Category with Autocomplete Dropdown suggestions
                    Text(if (businessCategory == "Service-Based") "Service Category" else "Category Selector", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate500)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = categoryStr,
                            onValueChange = {
                                categoryStr = it
                                autocompleteExpanded = true
                            },
                            placeholder = { Text("e.g. Consulting, Repair...", color = Slate400, fontSize = 13.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { autocompleteExpanded = it.isFocused },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeAccent,
                                unfocusedBorderColor = Slate100,
                                cursorColor = OrangeAccent
                            )
                        )

                        if (autocompleteExpanded && filteredCategorySuggestions.isNotEmpty()) {
                            DropdownMenu(
                                expanded = autocompleteExpanded,
                                onDismissRequest = { autocompleteExpanded = false },
                                properties = PopupProperties(focusable = false),
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                filteredCategorySuggestions.forEach { suggest ->
                                    DropdownMenuItem(
                                        text = { Text(suggest) },
                                        onClick = {
                                            categoryStr = suggest
                                            autocompleteExpanded = false
                                            dialogFocusManager.clearFocus()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (businessCategory != "Service-Based") {
                        // Styled Segmented Tax Percentage Slabs Row
                        Text("Tax Slabs Selectors", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate500)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val taxSlabChps = listOf(0, 5, 12, 18, 28)
                            taxSlabChps.forEach { slab ->
                                val isSelected = taxRateStr.toIntOrNull() == slab
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) LightIndigo else Color.White)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) IndigoAccent else Slate100,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { taxRateStr = slab.toString() }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$slab%",
                                        color = if (isSelected) IndigoAccent else Slate800,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (isGrocery) {
                        Text("Inventory Batches & Expirations", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate800)
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        var showBatchForm by remember { mutableStateOf(false) }
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Slate50, RoundedCornerShape(12.dp))
                                .border(1.dp, Slate100, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            if (currentBatches.isEmpty()) {
                                Text("No batches recorded.", fontSize = 11.sp, color = Slate500, modifier = Modifier.padding(bottom = 8.dp))
                            } else {
                                currentBatches.forEach { batch ->
                                    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(batch.expiryDate))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Qty: ${batch.quantity}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate800)
                                            Text("Exp: $dateStr", fontSize = 11.sp, color = RedAccent)
                                        }
                                        IconButton(onClick = { currentBatches = currentBatches.filter { it.id != batch.id } }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Slate400, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                Divider(color = Slate100, modifier = Modifier.padding(vertical = 8.dp))
                            }
                            
                            if (showBatchForm) {
                                var newQty by remember { mutableStateOf("") }
                                var newDate by remember { mutableStateOf("") }
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = newQty,
                                        onValueChange = { newQty = it },
                                        placeholder = { Text("Qty", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    OutlinedTextField(
                                        value = newDate,
                                        onValueChange = { newDate = it },
                                        placeholder = { Text("YYYY-MM-DD", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1.5f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    IconButton(
                                        onClick = {
                                            val q = newQty.toDoubleOrNull()
                                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                            val time = try { sdf.parse(newDate)?.time } catch(e: Exception) { null }
                                            if (q != null && time != null) {
                                                currentBatches = currentBatches + com.example.core.database.entity.IndustryMetadata.BatchInfo(
                                                    id = java.util.UUID.randomUUID().toString(),
                                                    quantity = q,
                                                    expiryDate = time
                                                )
                                                showBatchForm = false
                                            } else {
                                                Toast.makeText(context, "Invalid quantity or date format", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.background(GreenAccent, RoundedCornerShape(8.dp))
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.White)
                                    }
                                }
                            } else {
                                TextButton(onClick = { showBatchForm = true }) {
                                    Text("+ Add Batch / Restock", color = GreenAccent, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (!isGrocery) {
                        if (businessCategory == "F&B") {
                            Text("Menu Item Add-ons", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate800)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Dropdown choosing Add-ons
                            var addonDropdownExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { addonDropdownExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Slate100),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate500)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Add-on...", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                                DropdownMenu(
                                    expanded = addonDropdownExpanded,
                                    onDismissRequest = { addonDropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.9f).background(Color.White)
                                ) {
                                    val unselectedAddons = allAddons.filter { it.id !in selectedAddons }
                                    if (unselectedAddons.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No more available add-ons to select", color = Slate400, fontSize = 13.sp) },
                                            onClick = {}
                                        )
                                    } else {
                                        unselectedAddons.forEach { addon ->
                                            DropdownMenuItem(
                                                text = { Text("${addon.name} (+Rs. ${addon.price})", fontSize = 13.sp, color = Slate800) },
                                                onClick = {
                                                    selectedAddons = selectedAddons + addon.id
                                                    if (!customAddonPrices.containsKey(addon.id)) {
                                                        customAddonPrices = customAddonPrices + (addon.id to addon.price)
                                                    }
                                                    addonDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Selected Addons Overrides list
                            if (selectedAddons.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    selectedAddons.forEach { addonId ->
                                        val addon = allAddons.find { it.id == addonId }
                                        if (addon != null) {
                                            val currentPrice = customAddonPrices[addonId] ?: addon.price
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Slate50, RoundedCornerShape(12.dp))
                                                    .border(1.dp, Slate100, RoundedCornerShape(12.dp))
                                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Text(
                                                    text = addon.name,
                                                    modifier = Modifier.weight(1.5f),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Slate800
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.weight(2f)
                                                ) {
                                                    Text("Price Override (Rs.)", fontSize = 11.sp, color = Slate500)
                                                    OutlinedTextField(
                                                        value = if (currentPrice == 0.0) "" else currentPrice.toString(),
                                                        onValueChange = { newVal ->
                                                            val price = newVal.toDoubleOrNull() ?: 0.0
                                                            customAddonPrices = customAddonPrices + (addonId to price)
                                                        },
                                                        placeholder = { Text(addon.price.toString(), fontSize = 12.sp) },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                        modifier = Modifier.width(90.dp),
                                                        shape = RoundedCornerShape(8.dp),
                                                        singleLine = true,
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedBorderColor = OrangeAccent,
                                                            unfocusedBorderColor = Slate100,
                                                            focusedTextColor = Slate800,
                                                            unfocusedTextColor = Slate800
                                                        )
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { selectedAddons = selectedAddons - addonId },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text("Menu Item Variation Set (Optional)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate800)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Variation Set Selection Dropdown
                            var varSetDropdownExpanded by remember { mutableStateOf(false) }
                            val selectedSet = allVariations.find { it.id == selectedVariationSetId }

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { varSetDropdownExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Slate100),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate500)
                                ) {
                                    Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (selectedSet != null) "Set: ${selectedSet.name}" else "Select Variation Set...",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                DropdownMenu(
                                    expanded = varSetDropdownExpanded,
                                    onDismissRequest = { varSetDropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.9f).background(Color.White)
                                ) {
                                    if (allVariations.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No variation sets created yet", color = Slate400, fontSize = 13.sp) },
                                            onClick = {}
                                        )
                                    } else {
                                        DropdownMenuItem(
                                            text = { Text("None (Clear Selection)", color = Color.Red, fontSize = 13.sp) },
                                            onClick = {
                                                selectedVariationSetId = ""
                                                varSetDropdownExpanded = false
                                            }
                                        )
                                        allVariations.forEach { variationSet ->
                                            DropdownMenuItem(
                                                text = { Text("${variationSet.name} (${variationSet.options})", fontSize = 13.sp, color = Slate800) },
                                                onClick = {
                                                    selectedVariationSetId = variationSet.id
                                                    varSetDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Show options pricing fields under selected variation set
                            if (selectedSet != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val optionsList = selectedSet.options.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    optionsList.forEach { optionName ->
                                        val currentOptionPrice = customVariationPrices[optionName] ?: 0.0
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Slate50, RoundedCornerShape(12.dp))
                                                .border(1.dp, Slate100, RoundedCornerShape(12.dp))
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = optionName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Slate800
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text("Price (+Rs.)", fontSize = 11.sp, color = Slate500)
                                                OutlinedTextField(
                                                    value = if (currentOptionPrice == 0.0) "" else currentOptionPrice.toString(),
                                                    onValueChange = { newVal ->
                                                        val price = newVal.toDoubleOrNull() ?: 0.0
                                                        customVariationPrices = customVariationPrices + (optionName to price)
                                                    },
                                                    placeholder = { Text("0.00", fontSize = 12.sp) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    modifier = Modifier.width(90.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    singleLine = true,
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = OrangeAccent,
                                                        unfocusedBorderColor = Slate100,
                                                        focusedTextColor = Slate800,
                                                        unfocusedTextColor = Slate800
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (businessCategory != "Service-Based") {
                            Text("Variations & Add-ons", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate800)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Column 1: Available Add-ons
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Slate50, RoundedCornerShape(12.dp))
                                        .border(1.dp, Slate100, RoundedCornerShape(12.dp))
                                        .padding(8.dp)
                                ) {
                                    Text("Available Add-ons", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    if (allAddons.isEmpty()) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("No Add-ons", fontSize = 11.sp, color = Slate400)
                                        }
                                    } else {
                                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                                            items(allAddons) { addon ->
                                                val isChecked = selectedAddons.contains(addon.id)
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            selectedAddons = if (isChecked) selectedAddons - addon.id else selectedAddons + addon.id
                                                        }
                                                        .padding(vertical = 2.dp)
                                                ) {
                                                    Checkbox(
                                                        checked = isChecked,
                                                        onCheckedChange = {
                                                            selectedAddons = if (isChecked) selectedAddons - addon.id else selectedAddons + addon.id
                                                        },
                                                        colors = CheckboxDefaults.colors(checkedColor = IndigoAccent),
                                                        modifier = Modifier.scale(0.8f)
                                                    )
                                                    Column {
                                                        Text(addon.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Slate800, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        Text("+Rs. ${addon.price}", fontSize = 9.sp, color = GreenAccent, fontFamily = FontFamily.Monospace)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Column 2: Available Variations
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Slate50, RoundedCornerShape(12.dp))
                                        .border(1.dp, Slate100, RoundedCornerShape(12.dp))
                                        .padding(8.dp)
                                ) {
                                    Text("Available Variations", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    if (allVariations.isEmpty()) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("No Variations", fontSize = 11.sp, color = Slate400)
                                        }
                                    } else {
                                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                                            items(allVariations) { variation ->
                                                val isChecked = selectedVariations.contains(variation.id)
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            selectedVariations = if (isChecked) selectedVariations - variation.id else selectedVariations + variation.id
                                                        }
                                                        .padding(vertical = 2.dp)
                                                ) {
                                                    Checkbox(
                                                        checked = isChecked,
                                                        onCheckedChange = {
                                                            selectedVariations = if (isChecked) selectedVariations - variation.id else selectedVariations + variation.id
                                                        },
                                                        colors = CheckboxDefaults.colors(checkedColor = IndigoAccent),
                                                        modifier = Modifier.scale(0.8f)
                                                    )
                                                    Column {
                                                        Text(variation.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Slate800, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        Text("+Rs. ${variation.price}", fontSize = 9.sp, color = GreenAccent, fontFamily = FontFamily.Monospace)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        // Grocery options
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate50),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Scale,
                                        contentDescription = null,
                                        tint = Slate400,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column {
                                        Text("Sell as Loose Items", fontWeight = FontWeight.Bold, color = Slate800, fontSize = 13.sp)
                                        Text("Enable custom weight/fractional quantity input", fontSize = 11.sp, color = Slate500)
                                    }
                                }
                                Checkbox(
                                    checked = sellAsLoose,
                                    onCheckedChange = { sellAsLoose = it },
                                    colors = CheckboxDefaults.colors(checkedColor = GreenAccent, checkmarkColor = Color.White)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate50),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Product Variations (Individual)", fontWeight = FontWeight.Bold, color = Slate800, fontSize = 13.sp)
                                Text("Create variations for this item (e.g. 500g, 1kg)", fontSize = 11.sp, color = Slate500)
                                Spacer(modifier = Modifier.height(10.dp))

                                if (customGroceryVariations.isEmpty()) {
                                    Text("No variations added yet.", fontSize = 11.sp, color = Slate400, modifier = Modifier.padding(vertical = 4.dp))
                                } else {
                                    customGroceryVariations.forEach { variation ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.White, RoundedCornerShape(8.dp))
                                                .border(1.dp, Slate100, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(variation.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Slate800)
                                                Text("Price: Rs. ${variation.price} | Stock: ${variation.stock}", fontSize = 10.sp, color = Slate500)
                                            }
                                            IconButton(
                                                onClick = {
                                                    customGroceryVariations = customGroceryVariations.filter { it.id != variation.id }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Delete,
                                                    contentDescription = "Delete Variation",
                                                    tint = Color.Red.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = Slate100)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Add Custom Variation", fontWeight = FontWeight.Bold, color = Slate800, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newVarName,
                                        onValueChange = { newVarName = it },
                                        placeholder = { Text("Name (e.g. 500g)", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1.5f),
                                        shape = RoundedCornerShape(8.dp),
                                        textStyle = TextStyle(fontSize = 12.sp)
                                    )
                                    OutlinedTextField(
                                        value = newVarPriceStr,
                                        onValueChange = { newVarPriceStr = it },
                                        placeholder = { Text("Price", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(8.dp),
                                        textStyle = TextStyle(fontSize = 12.sp)
                                    )
                                    OutlinedTextField(
                                        value = newVarStockStr,
                                        onValueChange = { newVarStockStr = it },
                                        placeholder = { Text("Stock", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(8.dp),
                                        textStyle = TextStyle(fontSize = 12.sp)
                                    )
                                    IconButton(
                                        onClick = {
                                            val price = newVarPriceStr.toDoubleOrNull()
                                            val stock = newVarStockStr.toDoubleOrNull() ?: 0.0
                                            if (newVarName.isNotBlank() && price != null) {
                                                customGroceryVariations = customGroceryVariations + com.example.core.database.entity.IndustryMetadata.GroceryVariation(
                                                    id = java.util.UUID.randomUUID().toString(),
                                                    name = newVarName,
                                                    price = price,
                                                    stock = stock
                                                )
                                                newVarName = ""
                                                newVarPriceStr = ""
                                                newVarStockStr = ""
                                            } else {
                                                Toast.makeText(context, "Please enter Name and Price", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.background(OrangeAccent, RoundedCornerShape(8.dp))
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Recommended / Favorite Solid Star check
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate50),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = if (isFavorite) Color(0xFFF59E0B) else Slate400,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text("Recommended Item", fontWeight = FontWeight.Bold, color = Slate800, fontSize = 13.sp)
                                    Text("Featured Star listings will list first", fontSize = 11.sp, color = Slate500)
                                }
                            }
                            Checkbox(
                                checked = isFavorite,
                                onCheckedChange = { isFavorite = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = OrangeAccent,
                                    checkmarkColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Interactive Footer Row (Mic + CTA Button)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Command Intake mic layout
                    VoiceTriggerFAB(mode = OperationMode.INVENTORY_MODE) { intent ->
                        if (intent is VoiceCommandIntent.QuickRegisterProduct) {
                            intent.name.let { if (it.isNotBlank()) itemName = it }
                            intent.price?.let { priceStr = it.toString() }
                            intent.stock?.let { stockStr = it.toString() }
                            intent.unit?.let { unitStr = it }
                            intent.category?.let { categoryStr = it }
                        }
                    }

                    // Solid CTA Add/Update trigger
                    Button(
                        onClick = {
                            val finalItem = ProductEntity(
                                id = editingProduct?.id ?: UUID.randomUUID().toString(),
                                name = itemName.ifBlank { "Unknown Item" },
                                sku = editingProduct?.sku ?: itemName.lowercase().replace(" ", "-"),
                                price = priceStr.toDoubleOrNull() ?: 0.0,
                                taxRate = taxRateStr.toDoubleOrNull() ?: 0.0,
                                category = categoryStr.ifBlank { "Uncategorized" },
                                stock = if (trackStock) (stockStr.toDoubleOrNull() ?: 0.0) else 0.0,
                                minStock = if (trackStock) (minQtyStr.toDoubleOrNull() ?: 5.0) else 0.0,
                                unit = unitStr,
                                isFavorite = isFavorite,
                                addonIds = if (!isGrocery) selectedAddons.joinToString(",") else "",
                                variationIds = if (!isGrocery) {
                                    if (businessCategory == "F&B") {
                                        selectedVariationSetId
                                    } else {
                                        selectedVariations.joinToString(",")
                                    }
                                } else "",
                                trackStock = trackStock,
                                metadata = if (isGrocery) {
                                    com.example.core.database.entity.IndustryMetadata.Grocery(
                                        batches = currentBatches,
                                        isFractional = sellAsLoose,
                                        variations = customGroceryVariations
                                    )
                                } else if (businessCategory == "F&B") {
                                    com.example.core.database.entity.IndustryMetadata.FoodAndBeverage(
                                        modifierGroupIds = emptyList(),
                                        kitchenRouting = "Kitchen",
                                        customAddonPrices = customAddonPrices,
                                        customVariationPrices = customVariationPrices
                                    )
                                } else {
                                    editingProduct?.metadata ?: com.example.core.database.entity.IndustryMetadata.Generic()
                                }
                            )
                            onSave(finalItem)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Text(
                            text = if (editingProduct == null) "ADD ITEM" else "UPDATE ITEM",
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 14.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddonAddEditDialog(
    addon: com.example.core.database.entity.AddonEntity?,
    onDismiss: () -> Unit,
    onSave: (com.example.core.database.entity.AddonEntity) -> Unit
) {
    var name by remember { mutableStateOf(addon?.name ?: "") }
    var priceStr by remember { mutableStateOf(addon?.price?.toString() ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Slate100),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = if (addon == null) "NEW ADD-ON" else "EDIT ADD-ON",
                    fontWeight = FontWeight.Black,
                    color = Slate800,
                    fontSize = 18.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text("Name", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate500)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. Extra Cheese", color = Slate400, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeAccent,
                        unfocusedBorderColor = Slate100,
                        cursorColor = OrangeAccent
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Price Adjustment (+Rs.)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate500)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    placeholder = { Text("0.00", color = Slate400, fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeAccent,
                        unfocusedBorderColor = Slate100,
                        cursorColor = OrangeAccent
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Slate500)
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(
                                    com.example.core.database.entity.AddonEntity(
                                        id = addon?.id ?: UUID.randomUUID().toString(),
                                        name = name,
                                        price = priceStr.toDoubleOrNull() ?: 0.0
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("Save Change", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun VariationAddEditDialog(
    variation: com.example.core.database.entity.VariationEntity?,
    isFnB: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (com.example.core.database.entity.VariationEntity) -> Unit
) {
    var name by remember { mutableStateOf(variation?.name ?: "") }
    
    // For FnB category, we manage an expandable list of options. Otherwise, we maintain simple comma separated options.
    var optionList by remember {
        mutableStateOf(
            if (variation?.options.isNullOrBlank()) {
                listOf("")
            } else {
                variation!!.options.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }
        )
    }
    var optionsText by remember { mutableStateOf(variation?.options ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Slate100),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = if (variation == null) "NEW VARIATION SET" else "EDIT VARIATION SET",
                    fontWeight = FontWeight.Black,
                    color = Slate800,
                    fontSize = 18.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text("Variation Set Name", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate500)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. Size / Crust / Crust Options / Ice Level", color = Slate400, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeAccent,
                        unfocusedBorderColor = Slate100,
                        cursorColor = OrangeAccent
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isFnB) {
                    Text("Variation Options", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate500)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Render dynamically, with scrolling safety
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        optionList.forEachIndexed { index, option ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = option,
                                    onValueChange = { newValue ->
                                        optionList = optionList.toMutableList().apply {
                                            this[index] = newValue
                                        }
                                    },
                                    placeholder = { Text("Option ${index + 1} (e.g. Small / Large)", color = Slate400, fontSize = 13.sp) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = OrangeAccent,
                                        unfocusedBorderColor = Slate100,
                                        cursorColor = OrangeAccent
                                    )
                                )
                                
                                if (optionList.size > 1) {
                                    IconButton(
                                        onClick = {
                                            optionList = optionList.toMutableList().apply {
                                                removeAt(index)
                                            }
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove option",
                                            tint = Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Add option CTA button
                    OutlinedButton(
                        onClick = {
                            optionList = optionList + ""
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeAccent),
                        border = BorderStroke(1.dp, OrangeAccent),
                        modifier = Modifier.align(Alignment.End),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add option", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Option", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("Options (comma-separated)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate500)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = optionsText,
                        onValueChange = { optionsText = it },
                        placeholder = { Text("e.g. Small, Medium, Large", color = Slate400, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeAccent,
                            unfocusedBorderColor = Slate100,
                            cursorColor = OrangeAccent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Slate500)
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val savedOptions = if (isFnB) {
                                    optionList.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(",")
                                } else {
                                    optionsText
                                }
                                onSave(
                                    com.example.core.database.entity.VariationEntity(
                                        id = variation?.id ?: UUID.randomUUID().toString(),
                                        name = name,
                                        options = savedOptions,
                                        price = 0.0
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("Save Change", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TableHeaderAddonRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate50)
            .padding(vertical = 14.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(500.dp)) {
            Text("ADD-ON NAME", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500, letterSpacing = 1.sp)
        }
        Box(modifier = Modifier.width(250.dp)) {
            Text("PRICE ADJUSTMENT (+Rs.)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500, letterSpacing = 1.sp)
        }
        Box(modifier = Modifier.width(90.dp)) {
            Text("ACTIONS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun TableAddonRow(
    addon: com.example.core.database.entity.AddonEntity,
    backgroundColor: Color = Color.White,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(500.dp)) {
            Text(
                text = addon.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Slate800
            )
        }
        Box(modifier = Modifier.width(250.dp)) {
            Text(
                text = "+Rs. ${addon.price}",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = GreenAccent
            )
        }
        Box(modifier = Modifier.width(90.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit Addon",
                        tint = Slate400,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete Addon",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TableTableHeaderVariationRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate50)
            .padding(vertical = 14.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(350.dp)) {
            Text("VARIATION SET NAME", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500, letterSpacing = 1.sp)
        }
        Box(modifier = Modifier.width(400.dp)) {
            Text("OPTIONS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500, letterSpacing = 1.sp)
        }
        Box(modifier = Modifier.width(90.dp)) {
            Text("ACTIONS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun TableHeaderVariationRow() {
    TableTableHeaderVariationRow()
}

@Composable
fun TableVariationRow(
    variation: com.example.core.database.entity.VariationEntity,
    backgroundColor: Color = Color.White,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(350.dp)) {
            Text(
                text = variation.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Slate800
            )
        }
        Box(modifier = Modifier.width(400.dp)) {
            Text(
                text = variation.options,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Slate500
            )
        }
        Box(modifier = Modifier.width(90.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit Variation",
                        tint = Slate400,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete Variation",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TableHeaderStaffRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate50)
            .padding(vertical = 14.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(350.dp)) {
            Text("STAFF NAME", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500, letterSpacing = 1.sp)
        }
        Box(modifier = Modifier.width(400.dp)) {
            Text("ROLE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500, letterSpacing = 1.sp)
        }
        Box(modifier = Modifier.width(90.dp)) {
            Text("ACTIONS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun TableStaffRow(
    staff: com.example.core.database.entity.StaffEntity,
    backgroundColor: Color = Color.White,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(350.dp)) {
            Text(
                text = staff.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Slate800
            )
        }
        Box(modifier = Modifier.width(400.dp)) {
            Text(
                text = staff.role,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Slate500
            )
        }
        Box(modifier = Modifier.width(90.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete Staff",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StaffAddDialog(
    onDismiss: () -> Unit,
    onSave: (com.example.core.database.entity.StaffEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Slate100),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "NEW STAFF MEMBER",
                    fontWeight = FontWeight.Black,
                    color = Slate800,
                    fontSize = 18.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text("Name", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate500)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. Rahul Sharma", color = Slate400, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("staff_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeAccent,
                        unfocusedBorderColor = Slate100,
                        cursorColor = OrangeAccent
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Role/Specialty", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate500)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    placeholder = { Text("e.g. Hair Stylist", color = Slate400, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("staff_role_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeAccent,
                        unfocusedBorderColor = Slate100,
                        cursorColor = OrangeAccent
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("CANCEL", fontWeight = FontWeight.Bold, color = Slate500)
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank() && role.isNotBlank()) {
                                onSave(
                                    com.example.core.database.entity.StaffEntity(
                                        id = "staff-${System.currentTimeMillis()}",
                                        name = name.trim(),
                                        role = role.trim(),
                                        commissionRate = 12.0,
                                        isActive = true
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(46.dp).testTag("staff_save_btn"),
                        enabled = name.isNotBlank() && role.isNotBlank()
                    ) {
                        Text("SAVE STAFF", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
