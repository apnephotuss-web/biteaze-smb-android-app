package com.example.ui.screens.billing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.SyncPosApplication
import com.example.core.database.entity.OrderEntity
import com.example.core.database.entity.OrderItemEntity
import com.example.core.database.entity.ProductEntity
import com.example.core.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class PaymentMode { CASH, CARD, UPI, CREDIT }
enum class NumberingMode { DAILY, MONTHLY, YEARLY, CUSTOM }
enum class DiscountType { PERCENT, FIXED }

data class CartItem(
    val cartKey: String,
    val product: ProductEntity,
    val quantity: Double,
    val selectedVariation: com.example.core.database.entity.VariationEntity? = null,
    val selectedAddons: List<com.example.core.database.entity.AddonEntity> = emptyList(),
    val singleUnitPrice: Double,
    val discountValue: Double = 0.0,
    val discountType: String? = null, // "PERCENT", "FIXED", null
    val assignedStaffId: String? = null,
    val assignedStaffName: String? = null
)

data class CombinedDirectoryAndModifiers(
    val customers: List<com.example.core.database.entity.CustomerEntity>,
    val orders: List<OrderEntity>,
    val addons: List<com.example.core.database.entity.AddonEntity>,
    val variations: List<com.example.core.database.entity.VariationEntity>,
    val staffList: List<com.example.core.database.entity.StaffEntity> = emptyList()
)

data class FilterState(
    val selectedCategory: String? = null,
    val showOnlyFavorites: Boolean = false,
    val searchQuery: String = ""
)

data class POSContext(
    val activeOrderId: String? = null,
    val displayId: String = "",
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val discountType: DiscountType? = null,
    val discountValue: Double = 0.0,
    val customerPhone: String? = null,
    val customerName: String? = null,
    val customerAddress: String? = null,
    val scheduledStartTime: Long? = null,
    val estimatedDuration: Int? = null,
    val tableNumber: String? = null,
    val assignedStaffId: String? = null,
    val assignedStaffName: String? = null
)

data class BillingUiState(
    val products: List<ProductEntity> = emptyList(),
    val filteredProducts: List<ProductEntity> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val showOnlyFavorites: Boolean = false,
    val cartMap: Map<String, Double> = emptyMap(),
    val cart: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0,
    val calculatedTax: Double = 0.0,
    val paymentMode: PaymentMode = PaymentMode.CASH,
    val activeOrderId: String? = null,
    val displayId: String = "",
    val tableNumber: String? = null,
    val heldOrders: List<OrderEntity> = emptyList(),
    val searchQuery: String = "",
    val discountType: DiscountType? = null,
    val discountValue: Double = 0.0,
    val customerPhone: String? = null,
    val customerName: String? = null,
    val customerAddress: String? = null,
    val customers: List<com.example.core.database.entity.CustomerEntity> = emptyList(),
    val addons: List<com.example.core.database.entity.AddonEntity> = emptyList(),
    val variations: List<com.example.core.database.entity.VariationEntity> = emptyList(),
    val orders: List<OrderEntity> = emptyList(),
    val staffList: List<com.example.core.database.entity.StaffEntity> = emptyList(),
    val assignedStaffId: String? = null,
    val assignedStaffName: String? = null
)

data class CombinedPosPartial(
    val products: List<ProductEntity>,
    val categories: List<String>,
    val filter: FilterState,
    val cartMap: Map<String, Double>,
    val ctx: POSContext
)

data class CombinedDirectoryPartial(
    val customers: List<com.example.core.database.entity.CustomerEntity>,
    val orders: List<OrderEntity>
)

class BillingViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    suspend fun getOrderWithItems(orderId: String) = repository.getOrderWithItems(orderId)

    private val prefs = application.getSharedPreferences("syncpos_settings", android.content.Context.MODE_PRIVATE)

    val businessCategory = MutableStateFlow(prefs.getString("business_category", "F&B") ?: "F&B")

    private val preferenceListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == "business_category") {
            businessCategory.value = sharedPreferences.getString("business_category", "F&B") ?: "F&B"
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener)
    }

    var numberingMode: NumberingMode
        get() = NumberingMode.valueOf(prefs.getString("numbering_mode", NumberingMode.DAILY.name) ?: NumberingMode.DAILY.name)
        set(value) { prefs.edit().putString("numbering_mode", value.name).apply() }

    var customPrefix: String
        get() = prefs.getString("custom_prefix", "ORD-") ?: "ORD-"
        set(value) { prefs.edit().putString("custom_prefix", value).apply() }

    var customNextNumber: Int
        get() = prefs.getInt("custom_next_number", 1)
        set(value) { prefs.edit().putInt("custom_next_number", value).apply() }

    suspend fun generateNextDisplayId(): String {
        val allOrders = repository.getAllOrders().firstOrNull() ?: emptyList()
        val calendar = java.util.Calendar.getInstance()
        val currentYear = calendar.get(java.util.Calendar.YEAR)
        val currentMonth = calendar.get(java.util.Calendar.MONTH)
        val currentDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)

        return when (numberingMode) {
            NumberingMode.DAILY -> {
                val dailyCount = allOrders.count { ord ->
                    val ordCal = java.util.Calendar.getInstance().apply { timeInMillis = ord.createdAt }
                    ordCal.get(java.util.Calendar.YEAR) == currentYear &&
                    ordCal.get(java.util.Calendar.MONTH) == currentMonth &&
                    ordCal.get(java.util.Calendar.DAY_OF_MONTH) == currentDay
                }
                "D-${java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())}-${dailyCount + 1}"
            }
            NumberingMode.MONTHLY -> {
                val monthlyCount = allOrders.count { ord ->
                    val ordCal = java.util.Calendar.getInstance().apply { timeInMillis = ord.createdAt }
                    ordCal.get(java.util.Calendar.YEAR) == currentYear &&
                    ordCal.get(java.util.Calendar.MONTH) == currentMonth
                }
                "M-${java.text.SimpleDateFormat("yyyyMM", java.util.Locale.getDefault()).format(java.util.Date())}-${monthlyCount + 1}"
            }
            NumberingMode.YEARLY -> {
                val yearlyCount = allOrders.count { ord ->
                    val ordCal = java.util.Calendar.getInstance().apply { timeInMillis = ord.createdAt }
                    ordCal.get(java.util.Calendar.YEAR) == currentYear
                }
                "Y-${currentYear}-${yearlyCount + 1}"
            }
            NumberingMode.CUSTOM -> {
                val prefix = customPrefix
                val num = customNextNumber
                "$prefix$num"
            }
        }
    }

    suspend fun insertOrderWithEmptyItems(order: OrderEntity) {
        repository.saveOrderWithItems(order, emptyList(), updateStock = false)
    }

    private val _filterState = MutableStateFlow(FilterState())
    private val _cartMap = MutableStateFlow<Map<String, Double>>(emptyMap())
    private val _posContext = MutableStateFlow(POSContext())
    private val _tempProducts = MutableStateFlow<List<ProductEntity>>(emptyList())

    // Filter held orders reactively
    val heldOrders: Flow<List<OrderEntity>> = repository.getAllOrders().map { list ->
        list.filter { it.status.uppercase() == "HELD" }
    }

    private val combinedProductsFlow: Flow<List<ProductEntity>> = combine(
        repository.getAllProducts(),
        businessCategory,
        _tempProducts
    ) { prod, busCat, tempProds ->
        // Filter out items from tempProds if they are now present in the main database catalog
        val cleanTemp = tempProds.filter { tp -> prod.none { p -> p.id == tp.id } }
        val allProds = (prod + cleanTemp).distinctBy { it.id }
        val isGrocery = busCat == "Grocery"
        if (isGrocery) {
            allProds.flatMap { product ->
                val meta = product.metadata as? com.example.core.database.entity.IndustryMetadata.Grocery
                if (meta != null && meta.variations.isNotEmpty()) {
                    meta.variations.map { v ->
                        product.copy(
                            id = "${product.id}__var__${v.id}",
                            name = "${product.name} ${v.name}",
                            price = v.price,
                            stock = v.stock,
                            metadata = meta.copy(batches = v.batches, variations = emptyList())
                        )
                    }
                } else {
                    listOf(product)
                }
            }
        } else {
            allProds
        }
    }

    val uiState: StateFlow<BillingUiState> = combine(
        combine(
            combinedProductsFlow,
            repository.getAllCategories(),
            _filterState,
            _cartMap,
            _posContext
        ) { prod, cat, fil, cmap, c ->
            CombinedPosPartial(prod, cat, fil, cmap, c)
        },
        combine(
            repository.getAllCustomers(),
            repository.getAllOrders(),
            repository.getAllAddons(),
            repository.getAllVariations(),
            repository.getAllStaff()
        ) { cust, ord, addList, varList, staffL ->
            CombinedDirectoryAndModifiers(cust, ord, addList, varList, staffL)
        }
    ) { partial, dirMod ->
        val products = partial.products
        val categories = partial.categories
        val filter = partial.filter
        val cartMap = partial.cartMap
        val ctx = partial.ctx
        val customersList = dirMod.customers
        val ordersList = dirMod.orders
        val addonsList = dirMod.addons
        val variationsList = dirMod.variations

        // 1. Filter products by category, favorites, and search query
        val filtered = products.filter { prod ->
            val matchesCategory = filter.selectedCategory == null || prod.category.equals(filter.selectedCategory, ignoreCase = true)
            val matchesFavorite = !filter.showOnlyFavorites || prod.isFavorite
            val matchesSearch = filter.searchQuery.isBlank() || 
                    prod.name.contains(filter.searchQuery, ignoreCase = true) || 
                    prod.sku.contains(filter.searchQuery, ignoreCase = true)
            matchesCategory && matchesFavorite && matchesSearch
        }

        // 2. Assemble Cart detail rows
        val cartItems = cartMap.mapNotNull { (cartKey, qty) ->
            if (qty <= 0.0) return@mapNotNull null
            val parts = cartKey.split(":")
            val productId = parts.getOrNull(0) ?: return@mapNotNull null
            val prod = products.find { it.id == productId } ?: return@mapNotNull null
            
            val selectedVariationId = parts.getOrNull(1)
            val selectedVariation = if (!selectedVariationId.isNullOrBlank()) {
                val fbMeta = prod.metadata as? com.example.core.database.entity.IndustryMetadata.FoodAndBeverage
                if (fbMeta != null && fbMeta.customVariationPrices.containsKey(selectedVariationId)) {
                    com.example.core.database.entity.VariationEntity(
                        id = selectedVariationId,
                        name = selectedVariationId,
                        price = fbMeta.customVariationPrices[selectedVariationId] ?: 0.0
                    )
                } else {
                    variationsList.find { it.id == selectedVariationId }
                }
            } else null
            
            val addonIdsStr = parts.getOrNull(2)
            val selectedAddons = if (!addonIdsStr.isNullOrBlank()) {
                val ids = addonIdsStr.split(",").toSet()
                addonsList.filter { it.id in ids }
            } else emptyList()
            
            val customUnit = parts.getOrNull(3)
            val finalProduct = if (!customUnit.isNullOrBlank() && customUnit != prod.unit) {
                val factor = getQuantityConversionFactor(prod.unit, customUnit)
                val rate = if (factor > 0.0) factor else 1.0
                prod.copy(
                    unit = customUnit,
                    price = prod.price / rate,
                    stock = prod.stock * rate,
                    minStock = prod.minStock * rate
                )
            } else {
                prod
            }
            
            val singlePrice = finalProduct.price + (selectedVariation?.price ?: 0.0) + selectedAddons.sumOf { it.price }

            val staffId = parts.getOrNull(4)
            val assignedStaff = staffId?.let { sId -> dirMod.staffList.find { it.id == sId } }

            CartItem(
                cartKey = cartKey,
                product = finalProduct,
                quantity = qty,
                selectedVariation = selectedVariation,
                selectedAddons = selectedAddons,
                singleUnitPrice = singlePrice,
                assignedStaffId = staffId,
                assignedStaffName = assignedStaff?.name
            )
        }

        // 3. Compute totals
        var subtotal = 0.0
        var totalTax = 0.0
        for (item in cartItems) {
            val itemSum = item.singleUnitPrice * item.quantity
            subtotal += itemSum
            // Handle line-level and global discounts for line tax calculation
            val lineDiscountValue = when (item.discountType) {
                "PERCENT" -> itemSum * (item.discountValue / 100.0)
                "FIXED" -> item.discountValue
                else -> 0.0
            }.coerceAtLeast(0.0)
            val finalLineSum = (itemSum - lineDiscountValue).coerceAtLeast(0.0)
            totalTax += calculateMultiSlabTax(finalLineSum, item.product.taxRate)
        }

        val ongoingHeld = ordersList.filter { it.status.uppercase() == "HELD" }

        BillingUiState(
            products = products,
            filteredProducts = filtered,
            categories = categories,
            selectedCategory = filter.selectedCategory,
            showOnlyFavorites = filter.showOnlyFavorites,
            cartMap = cartMap,
            cart = cartItems,
            subtotal = subtotal,
            calculatedTax = totalTax,
            paymentMode = ctx.paymentMode,
            activeOrderId = ctx.activeOrderId,
            displayId = ctx.displayId,
            tableNumber = ctx.tableNumber,
            heldOrders = ongoingHeld,
            searchQuery = filter.searchQuery,
            discountType = ctx.discountType,
            discountValue = ctx.discountValue,
            customerPhone = ctx.customerPhone,
            customerName = ctx.customerName,
            customerAddress = ctx.customerAddress,
            customers = customersList,
            addons = addonsList,
            variations = variationsList,
            orders = ordersList,
            staffList = dirMod.staffList,
            assignedStaffId = ctx.assignedStaffId,
            assignedStaffName = ctx.assignedStaffName
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BillingUiState()
    )

    fun loadOrder(orderId: String, tableNumber: String? = null) {
        if (orderId == "new" || orderId.isBlank()) {
            viewModelScope.launch {
                val nextDisplayId = generateNextDisplayId()
                _posContext.value = POSContext(
                    activeOrderId = UUID.randomUUID().toString(),
                    displayId = nextDisplayId,
                    paymentMode = PaymentMode.CASH,
                    discountType = null,
                    discountValue = 0.0,
                    customerPhone = null,
                    customerName = null,
                    scheduledStartTime = null,
                    estimatedDuration = null,
                    tableNumber = tableNumber
                )
                _cartMap.value = emptyMap()
            }
        } else {
            viewModelScope.launch {
                val orderWithItems = repository.getOrderWithItems(orderId)
                if (orderWithItems != null) {
                    val payM = when (orderWithItems.order.paymentMode.uppercase()) {
                        "CASH" -> PaymentMode.CASH
                        "CARD" -> PaymentMode.CARD
                        "UPI" -> PaymentMode.UPI
                        "CREDIT", "OTHERS" -> PaymentMode.CREDIT
                        else -> PaymentMode.CASH
                    }
                    val discT = when (orderWithItems.order.cartDiscountType) {
                        "PERCENT" -> DiscountType.PERCENT
                        "FIXED" -> DiscountType.FIXED
                        else -> null
                    }
                    _posContext.value = POSContext(
                        activeOrderId = orderWithItems.order.id,
                        displayId = orderWithItems.order.displayId.ifBlank { orderWithItems.order.id.takeLast(4) },
                        paymentMode = payM,
                        discountType = discT,
                        discountValue = orderWithItems.order.cartDiscountValue,
                        customerPhone = orderWithItems.order.customerPhone,
                        customerName = orderWithItems.order.customerName,
                        scheduledStartTime = orderWithItems.order.scheduledStartTime,
                        estimatedDuration = orderWithItems.order.estimatedDuration,
                        tableNumber = orderWithItems.order.tableNumber,
                        assignedStaffId = orderWithItems.order.assignedStaffId,
                        assignedStaffName = orderWithItems.order.assignedStaffName
                    )
                    
                    // Reassemble cartMap with staffId encoding (staffId is stored in orderItems.staffId!)
                    val map = orderWithItems.items.associate { item ->
                        val staffIdEncoded = item.staffId ?: ""
                        val variationStr = item.selectedVariationId ?: ""
                        val addonStr = item.selectedAddonIds ?: ""
                        val key = "${item.productId}:${variationStr}:${addonStr}::${staffIdEncoded}"
                        key to item.quantity
                    }
                    _cartMap.value = map
                } else {
                    _posContext.value = POSContext(
                        activeOrderId = orderId,
                        displayId = orderId.takeLast(4)
                    )
                }
            }
        }
    }

    fun loadOrderForTimeSlot(orderId: String, startTime: Long) {
        viewModelScope.launch {
            val nextDisplayId = generateNextDisplayId()
            _posContext.value = POSContext(
                activeOrderId = if (orderId == "new" || orderId.isBlank()) UUID.randomUUID().toString() else orderId,
                displayId = nextDisplayId,
                paymentMode = PaymentMode.CASH,
                discountType = null,
                discountValue = 0.0,
                customerPhone = null,
                customerName = null,
                scheduledStartTime = startTime,
                estimatedDuration = 60 // default 60 mins
            )
            _cartMap.value = emptyMap()
        }
    }

    fun updateSearchQuery(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    fun selectCategory(category: String?) {
        _filterState.update { it.copy(selectedCategory = category) }
    }

    fun toggleFavorites() {
        _filterState.update { it.copy(showOnlyFavorites = !it.showOnlyFavorites) }
    }

    fun addToCart(cartKey: String) {
        val current = _cartMap.value.toMutableMap()
        val currentQty = current[cartKey] ?: 0.0
        
        // Find product to check overall stock
        val productId = cartKey.split(":").firstOrNull() ?: return
        val product = uiState.value.products.find { it.id == productId } ?: return

        // Stock limit validation (compare total quantity in cart of this product vs its stock)
        val totalProductQtyInCart = _cartMap.value.filter { it.key.startsWith("$productId:") || it.key == productId }.values.sum()
        if (businessCategory.value == "Service-Based" || totalProductQtyInCart < product.stock) {
            current[cartKey] = currentQty + 1.0
            _cartMap.value = current
        }
    }

    fun addToCart(product: ProductEntity) {
        addToCart(product.id)
    }

    fun addQuickItemToCart(product: ProductEntity) {
        _tempProducts.value = _tempProducts.value + product
        val current = _cartMap.value.toMutableMap()
        val currentQty = current[product.id] ?: 0.0
        current[product.id] = currentQty + 1.0
        _cartMap.value = current
    }

    fun saveProductGlobal(product: ProductEntity) {
        viewModelScope.launch {
            repository.insertProduct(product)
        }
    }

    fun addCustomToCart(product: ProductEntity, variationId: String?, addonIds: List<String>) {
        val addonStr = if (addonIds.isNotEmpty()) addonIds.sorted().joinToString(",") else ""
        val cartKey = if (variationId.isNullOrEmpty() && addonStr.isEmpty()) {
            product.id
        } else {
            "${product.id}:${variationId ?: ""}:${addonStr}"
        }
        
        val current = _cartMap.value.toMutableMap()
        val currentQty = current[cartKey] ?: 0.0
        
        val totalProductQtyInCart = _cartMap.value.filter { it.key.startsWith("${product.id}:") || it.key == product.id }.values.sum()
        if (totalProductQtyInCart < product.stock) {
            current[cartKey] = currentQty + 1.0
            _cartMap.value = current
        }
    }

    fun addToCartWithStaff(product: ProductEntity, staffId: String) {
        val cartKey = "${product.id}::::${staffId}"
        val current = _cartMap.value.toMutableMap()
        val currentQty = current[cartKey] ?: 0.0
        
        val totalProductQtyInCart = _cartMap.value.filter { it.key.startsWith("${product.id}:") || it.key == product.id }.values.sum()
        if (totalProductQtyInCart < product.stock) {
            current[cartKey] = currentQty + 1.0
            _cartMap.value = current
        }
    }

    fun addCustomToCartWithStaff(product: ProductEntity, variationId: String?, addonIds: List<String>, staffId: String) {
        val addonStr = if (addonIds.isNotEmpty()) addonIds.sorted().joinToString(",") else ""
        val cartKey = "${product.id}:${variationId ?: ""}:${addonStr}::${staffId}"
        val current = _cartMap.value.toMutableMap()
        val currentQty = current[cartKey] ?: 0.0
        
        val totalProductQtyInCart = _cartMap.value.filter { it.key.startsWith("${product.id}:") || it.key == product.id }.values.sum()
        if (totalProductQtyInCart < product.stock) {
            current[cartKey] = currentQty + 1.0
            _cartMap.value = current
        }
    }

    fun addFractionalQuantityToCart(product: ProductEntity, quantity: Double, unit: String, overwrite: Boolean = true) {
        val cartKey = "${product.id}:::${unit}"
        val current = _cartMap.value.toMutableMap()
        
        // Find existing custom-unit stock
        var maxStock = product.stock
        if (unit != product.unit) {
            val factor = getQuantityConversionFactor(product.unit, unit)
            if (factor > 0) {
                maxStock = product.stock * factor
            }
        }

        // Ensure not exceeding stock
        val currentQty = if (overwrite) 0.0 else (current[cartKey] ?: 0.0)
        val newQty = (currentQty + quantity).coerceAtMost(maxStock)
        
        if (newQty > 0) {
            current[cartKey] = newQty
            _cartMap.value = current
        }
    }

    fun addFractionalQuantityToCart(product: ProductEntity, quantity: Double, overwrite: Boolean = true) {
        addFractionalQuantityToCart(product, quantity, product.unit, overwrite)
    }

    fun updateProductUnit(productId: String, unit: String) {
        viewModelScope.launch {
            val realId = if (productId.contains("__var__")) productId.substringBefore("__var__") else productId
            val existing = repository.getProductById(realId) ?: return@launch
            repository.updateProduct(existing.copy(unit = unit, lastUpdated = System.currentTimeMillis()))
        }
    }

    fun removeFromCart(cartKey: String) {
        val current = _cartMap.value.toMutableMap()
        val currentQty = current[cartKey] ?: 0.0
        if (currentQty <= 1.0) {
            current.remove(cartKey)
        } else {
            current[cartKey] = currentQty - 1.0
        }
        _cartMap.value = current
    }

    fun removeFromCart(product: ProductEntity) {
        removeFromCart(product.id)
    }

    fun clearCart() {
        _cartMap.value = emptyMap()
    }

    fun setPaymentMode(mode: PaymentMode) {
        _posContext.update { it.copy(paymentMode = mode) }
    }

    fun setTableString(tableNumber: String?) {
        _posContext.update { it.copy(tableNumber = tableNumber) }
    }

    fun setDiscount(type: DiscountType?, value: Double) {
        _posContext.update { it.copy(discountType = type, discountValue = value) }
    }

    fun setCustomerInfo(name: String?, phone: String?, address: String? = null) {
        val finalName = name?.ifBlank { null }
        val finalPhone = phone?.ifBlank { null }
        val finalAddress = address?.ifBlank { null }
        _posContext.update { it.copy(customerName = finalName, customerPhone = finalPhone, customerAddress = finalAddress) }
        
        viewModelScope.launch {
            if (finalName != null && finalPhone != null) {
                val existing = repository.getCustomerByPhone(finalPhone)
                if (existing == null) {
                    repository.insertCustomer(com.example.core.database.entity.CustomerEntity(
                        id = UUID.randomUUID().toString(),
                        name = finalName,
                        phone = finalPhone,
                        address = finalAddress,
                        createdAt = System.currentTimeMillis()
                    ))
                } else {
                    repository.insertCustomer(existing.copy(name = finalName, address = finalAddress ?: existing.address))
                }
            }
        }
    }

    fun updateCustomer(customer: com.example.core.database.entity.CustomerEntity) {
        viewModelScope.launch {
            repository.updateCustomer(customer)
        }
    }

    fun setAssignedStaff(staffId: String?, staffName: String?) {
        _posContext.update { it.copy(assignedStaffId = staffId, assignedStaffName = staffName) }
    }

    fun saveAndHoldOrder(onComplete: () -> Unit) {
        val state = uiState.value
        val ctx = _posContext.value
        val orderId = ctx.activeOrderId ?: UUID.randomUUID().toString()
        if (state.cart.isEmpty()) return

        val discountAmount = when (ctx.discountType) {
            DiscountType.PERCENT -> state.subtotal * (ctx.discountValue / 100.0)
            DiscountType.FIXED -> ctx.discountValue
            else -> 0.0
        }
        val finalAmount = (state.subtotal + state.calculatedTax - discountAmount).coerceAtLeast(0.0)

        viewModelScope.launch {
            // Guarantee that all cart products exist in the database (or are saved as isDeleted = true)
            state.cart.forEach { item ->
                val existing = repository.getProductById(item.product.id)
                if (existing == null) {
                    repository.insertProduct(item.product.copy(isDeleted = true))
                }
            }

            val order = OrderEntity(
                id = orderId,
                createdAt = System.currentTimeMillis(),
                totalAmount = finalAmount,
                subtotal = state.subtotal,
                taxAmount = state.calculatedTax,
                paymentMethod = ctx.paymentMode.name,
                paymentMode = ctx.paymentMode.name,
                status = "HELD",
                shiftId = "shift-1",
                customerPhone = ctx.customerPhone,
                customerName = ctx.customerName,
                cartDiscountValue = ctx.discountValue,
                cartDiscountType = ctx.discountType?.name,
                displayId = ctx.displayId.ifBlank { orderId.takeLast(4) },
                scheduledStartTime = ctx.scheduledStartTime,
                estimatedDuration = ctx.estimatedDuration,
                tableNumber = ctx.tableNumber,
                assignedStaffId = ctx.assignedStaffId,
                assignedStaffName = ctx.assignedStaffName
            )

            val orderItems = state.cart.map { item ->
                val itemSubtotal = item.singleUnitPrice * item.quantity
                val lineDiscountValue = when (item.discountType) {
                    "PERCENT" -> itemSubtotal * (item.discountValue / 100.0)
                    "FIXED" -> item.discountValue
                    else -> 0.0
                }.coerceAtLeast(0.0)
                val finalLineSum = (itemSubtotal - lineDiscountValue).coerceAtLeast(0.0)
                val calculatedLineTax = calculateMultiSlabTax(finalLineSum, item.product.taxRate)

                val originalProduct = uiState.value.products.find { it.id == item.product.id }
                val (dbQty, dbPrice) = if (originalProduct != null && originalProduct.unit != item.product.unit) {
                    val factor = getQuantityConversionFactor(originalProduct.unit, item.product.unit)
                    val rate = if (factor > 0) factor else 1.0
                    Pair(item.quantity / rate, item.singleUnitPrice * rate)
                } else {
                    Pair(item.quantity, item.singleUnitPrice)
                }

                OrderItemEntity(
                    orderId = orderId,
                    productId = item.product.id,
                    quantity = dbQty,
                    price = dbPrice,
                    selectedVariationId = item.selectedVariation?.id,
                    selectedAddonIds = item.selectedAddons.map { it.id }.takeIf { it.isNotEmpty() }?.joinToString(","),
                    discountValue = item.discountValue,
                    discountType = item.discountType,
                    taxAmount = calculatedLineTax,
                    staffId = item.assignedStaffId,
                    metadata = originalProduct?.metadata ?: item.product.metadata
                )
            }

            repository.saveOrderWithItems(order, orderItems, updateStock = false)
            if (numberingMode == NumberingMode.CUSTOM && ctx.displayId == "$customPrefix$customNextNumber") {
                customNextNumber = customNextNumber + 1
            }
            clearCart()
            _posContext.value = POSContext()
            onComplete()
        }
    }

    fun saveAndPrintCompletedOrder(onComplete: () -> Unit) {
        val state = uiState.value
        val ctx = _posContext.value
        val orderId = ctx.activeOrderId ?: UUID.randomUUID().toString()
        if (state.cart.isEmpty()) return

        val discountAmount = when (ctx.discountType) {
            DiscountType.PERCENT -> state.subtotal * (ctx.discountValue / 100.0)
            DiscountType.FIXED -> ctx.discountValue
            else -> 0.0
        }
        val finalAmount = (state.subtotal + state.calculatedTax - discountAmount).coerceAtLeast(0.0)

        viewModelScope.launch {
            // Guarantee that all cart products exist in the database (or are saved as isDeleted = true)
            state.cart.forEach { item ->
                val existing = repository.getProductById(item.product.id)
                if (existing == null) {
                    repository.insertProduct(item.product.copy(isDeleted = true))
                }
            }

            val order = OrderEntity(
                id = orderId,
                createdAt = System.currentTimeMillis(),
                totalAmount = finalAmount,
                subtotal = state.subtotal,
                taxAmount = state.calculatedTax,
                paymentMethod = ctx.paymentMode.name,
                paymentMode = ctx.paymentMode.name,
                status = "COMPLETED",
                shiftId = "shift-1",
                customerPhone = ctx.customerPhone,
                customerName = ctx.customerName,
                cartDiscountValue = ctx.discountValue,
                cartDiscountType = ctx.discountType?.name,
                displayId = ctx.displayId.ifBlank { orderId.takeLast(4) },
                scheduledStartTime = ctx.scheduledStartTime,
                estimatedDuration = ctx.estimatedDuration,
                tableNumber = ctx.tableNumber,
                assignedStaffId = ctx.assignedStaffId,
                assignedStaffName = ctx.assignedStaffName
            )

            val orderItems = state.cart.map { item ->
                val itemSubtotal = item.singleUnitPrice * item.quantity
                val lineDiscountValue = when (item.discountType) {
                    "PERCENT" -> itemSubtotal * (item.discountValue / 100.0)
                    "FIXED" -> item.discountValue
                    else -> 0.0
                }.coerceAtLeast(0.0)
                val finalLineSum = (itemSubtotal - lineDiscountValue).coerceAtLeast(0.0)
                val calculatedLineTax = calculateMultiSlabTax(finalLineSum, item.product.taxRate)

                val originalProduct = uiState.value.products.find { it.id == item.product.id }
                val (dbQty, dbPrice) = if (originalProduct != null && originalProduct.unit != item.product.unit) {
                    val factor = getQuantityConversionFactor(originalProduct.unit, item.product.unit)
                    val rate = if (factor > 0) factor else 1.0
                    Pair(item.quantity / rate, item.singleUnitPrice * rate)
                } else {
                    Pair(item.quantity, item.singleUnitPrice)
                }

                OrderItemEntity(
                    orderId = orderId,
                    productId = item.product.id,
                    quantity = dbQty,
                    price = dbPrice,
                    selectedVariationId = item.selectedVariation?.id,
                    selectedAddonIds = item.selectedAddons.map { it.id }.takeIf { it.isNotEmpty() }?.joinToString(","),
                    discountValue = item.discountValue,
                    discountType = item.discountType,
                    taxAmount = calculatedLineTax,
                    staffId = item.assignedStaffId,
                    metadata = originalProduct?.metadata ?: item.product.metadata
                )
            }

            repository.saveOrderWithItems(order, orderItems, updateStock = true)
            
            if (ctx.paymentMode == PaymentMode.CREDIT && !ctx.customerPhone.isNullOrBlank()) {
                val existingCustomer = repository.getCustomerByPhone(ctx.customerPhone)
                if (existingCustomer != null) {
                    repository.updateCustomer(existingCustomer.copy(creditBalance = existingCustomer.creditBalance + finalAmount))
                }
            }

            if (numberingMode == NumberingMode.CUSTOM && ctx.displayId == "$customPrefix$customNextNumber") {
                customNextNumber = customNextNumber + 1
            }
            onComplete()
        }
    }

    private fun calculateMultiSlabTax(subtotal: Double, baseRate: Double): Double {
        if (subtotal <= 0) return 0.0
        return when {
            subtotal <= 50.0 -> subtotal * (baseRate / 100.0)
            subtotal <= 200.0 -> {
                val part1 = 50.0 * (baseRate / 100.0)
                val part2 = (subtotal - 50.0) * ((baseRate + 3.0) / 100.0)
                part1 + part2
            }
            else -> {
                val part1 = 50.0 * (baseRate / 100.0)
                val part2 = 150.0 * ((baseRate + 3.0) / 100.0)
                val part3 = (subtotal - 200.0) * ((baseRate + 6.0) / 100.0)
                part1 + part2 + part3
            }
        }
    }

    // Hands-free Voice Billing Parser
    fun handleVoiceCommand(transcript: String) {
        val text = transcript.lowercase()
        val current = _cartMap.value.toMutableMap()
        var matched = false

        val directPattern = "([a-zA-Z0-9\\s&&[^\\b(plus|minus|add|remove)\\b]]+?)\\s*(plus|minus|add|remove)\\s*(one|two|three|four|five|six|seven|eight|nine|ten|\\d+)".toRegex()
        val directMatches = directPattern.findAll(text)

        for (match in directMatches) {
            val prodNameQuery = match.groupValues[1].trim()
            val action = match.groupValues[2]
            val qtyStr = match.groupValues[3]

            val qty = parseQuantity(qtyStr)
            val product = uiState.value.products.find { prod ->
                prod.name.lowercase().contains(prodNameQuery) ||
                prodNameQuery.contains(prod.name.lowercase())
            }

            if (product != null) {
                val currentQty = current[product.id] ?: 0.0
                if (action == "plus" || action == "add") {
                    val targetQty = (currentQty + qty).coerceAtMost(product.stock)
                    current[product.id] = targetQty
                } else if (action == "minus" || action == "remove") {
                    val targetQty = currentQty - qty
                    if (targetQty <= 0.0) {
                        current.remove(product.id)
                    } else {
                        current[product.id] = targetQty
                    }
                }
                matched = true
            }
        }

        if (!matched) {
            val prefixPattern = "(add|remove|plus|minus)\\s*(one|two|three|four|five|six|seven|eight|nine|ten|\\d+)\\s+([a-zA-Z0-9\\s]+)".toRegex()
            val prefixMatches = prefixPattern.findAll(text)
            for (match in prefixMatches) {
                val action = match.groupValues[1]
                val qtyStr = match.groupValues[2]
                val prodNameQuery = match.groupValues[3].trim()

                val qty = parseQuantity(qtyStr)
                val product = uiState.value.products.find { prod ->
                    prod.name.lowercase().contains(prodNameQuery) ||
                    prodNameQuery.contains(prod.name.lowercase())
                }

                if (product != null) {
                    val currentQty = current[product.id] ?: 0.0
                    if (action == "add" || action == "plus") {
                        val targetQty = (currentQty + qty).coerceAtMost(product.stock)
                        current[product.id] = targetQty
                    } else {
                        val targetQty = currentQty - qty
                        if (targetQty <= 0.0) {
                            current.remove(product.id)
                        } else {
                            current[product.id] = targetQty
                        }
                    }
                    matched = true
                }
            }
        }

        if (matched) {
            _cartMap.value = current
        }
    }

    private fun parseQuantity(qtyStr: String): Int {
        return when (qtyStr) {
            "one" -> 1
            "two" -> 2
            "three" -> 3
            "four" -> 4
            "five" -> 5
            "six" -> 6
            "seven" -> 7
            "eight" -> 8
            "nine" -> 9
            "ten" -> 10
            else -> qtyStr.toIntOrNull() ?: 1
        }
    }

    fun handleVoiceIntent(
        intent: com.example.core.voice.VoiceCommandIntent,
        onSaveAndHold: (() -> Unit)? = null,
        onSaveAndBill: (() -> Unit)? = null
    ) {
        val inventory = uiState.value.products
        when (intent) {
            is com.example.core.voice.VoiceCommandIntent.CompositeIntent -> {
                intent.intents.forEach { subIntent ->
                    handleVoiceIntent(subIntent, onSaveAndHold, onSaveAndBill)
                }
            }
            is com.example.core.voice.VoiceCommandIntent.AddCartItem -> {
                val current = _cartMap.value.toMutableMap()
                val currentQty = current[intent.productCode] ?: 0.0
                val prod = inventory.find { it.id == intent.productCode } ?: return
                if (businessCategory.value == "Service-Based" || currentQty + intent.quantity <= prod.stock) {
                    current[intent.productCode] = currentQty + intent.quantity
                } else {
                    current[intent.productCode] = prod.stock
                }
                _cartMap.value = current
            }
            is com.example.core.voice.VoiceCommandIntent.RemoveCartItem -> {
                val current = _cartMap.value.toMutableMap()
                val currentQty = current[intent.productCode] ?: 0.0
                if (currentQty > 0.0) {
                    val delta = intent.quantity ?: 1.0
                    val newQty = currentQty - delta
                    if (newQty > 0.0) {
                        current[intent.productCode] = newQty
                    } else {
                        current.remove(intent.productCode)
                    }
                    _cartMap.value = current
                }
            }
            is com.example.core.voice.VoiceCommandIntent.ApplyCartDiscount -> {
                setDiscount(if (intent.type == "PERCENT") DiscountType.PERCENT else DiscountType.FIXED, intent.amount)
            }
            is com.example.core.voice.VoiceCommandIntent.CheckoutCart -> {
                if (intent.paymentMode == "HOLD") {
                    if (businessCategory.value != "Grocery") {
                        setPaymentMode(PaymentMode.CREDIT)
                        onSaveAndHold?.invoke()
                    }
                } else if (intent.paymentMode == "BILL") {
                    setPaymentMode(PaymentMode.CASH)
                    onSaveAndBill?.invoke()
                } else {
                    val mode = when (intent.paymentMode) {
                        "CARD" -> PaymentMode.CARD
                        "UPI" -> PaymentMode.UPI
                        else -> PaymentMode.CASH
                    }
                    setPaymentMode(mode)
                }
            }
            is com.example.core.voice.VoiceCommandIntent.ClearCart -> {
                clearCart()
            }
            else -> {}
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repository = (application as SyncPosApplication).repository
            return BillingViewModel(application, repository) as T
        }
    }
}

fun getQuantityConversionFactor(fromUnit: String, toUnit: String): Double {
    val from = fromUnit.lowercase().trim()
    val to = toUnit.lowercase().trim()
    if (from == to) return 1.0

    val fromNormalized = when (from) {
        "kilogram", "kilograms", "kg" -> "kg"
        "gram", "grams", "g" -> "g"
        "liter", "liters", "l" -> "l"
        "milliliter", "milliliters", "ml" -> "ml"
        else -> from
    }

    val toNormalized = when (to) {
        "kilogram", "kilograms", "kg" -> "kg"
        "gram", "grams", "g" -> "g"
        "liter", "liters", "l" -> "l"
        "milliliter", "milliliters", "ml" -> "ml"
        else -> to
    }

    if (fromNormalized == toNormalized) return 1.0

    // kg and g
    if (fromNormalized == "kg" && toNormalized == "g") return 1000.0
    if (fromNormalized == "g" && toNormalized == "kg") return 0.001

    // liters and ml
    if (fromNormalized == "l" && toNormalized == "ml") return 1000.0
    if (fromNormalized == "ml" && toNormalized == "l") return 0.001

    return 1.0
}
