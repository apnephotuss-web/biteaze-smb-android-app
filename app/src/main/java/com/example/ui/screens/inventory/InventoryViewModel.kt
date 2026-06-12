package com.example.ui.screens.inventory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.SyncPosApplication
import com.example.core.database.entity.AddonEntity
import com.example.core.database.entity.ProductEntity
import com.example.core.database.entity.VariationEntity
import com.example.core.repository.AppRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.MutableStateFlow

class InventoryViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("syncpos_settings", android.content.Context.MODE_PRIVATE)

    val businessCategory = MutableStateFlow(prefs.getString("business_category", "F&B") ?: "F&B")
    val expiryWarningDays = MutableStateFlow(prefs.getInt("expiry_warning_days", 7))

    private val preferenceListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == "business_category") {
            businessCategory.value = sharedPreferences.getString("business_category", "F&B") ?: "F&B"
        } else if (key == "expiry_warning_days") {
            expiryWarningDays.value = sharedPreferences.getInt("expiry_warning_days", 7)
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener)
    }

    val uiState: StateFlow<List<ProductEntity>> = combine(
        repository.getActiveProducts(),
        businessCategory
    ) { prodList, busCat ->
        val isGrocery = busCat == "Grocery"
        if (isGrocery) {
            prodList.flatMap { product ->
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
            prodList
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    suspend fun getProductById(id: String): ProductEntity? {
        return repository.getProductById(id)
    }
        
    val categories: StateFlow<List<String>> = repository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val addons: StateFlow<List<AddonEntity>> = repository.getAllAddons()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val variations: StateFlow<List<VariationEntity>> = repository.getAllVariations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val staff: StateFlow<List<com.example.core.database.entity.StaffEntity>> = repository.getAllActiveStaff()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveStaff(staff: com.example.core.database.entity.StaffEntity) {
        viewModelScope.launch {
            repository.insertStaff(staff)
        }
    }

    fun deleteStaff(id: String) {
        viewModelScope.launch {
            repository.deleteStaff(id)
        }
    }

    fun saveAddon(addon: AddonEntity) {
        viewModelScope.launch {
            repository.insertAddon(addon)
        }
    }

    fun deleteAddon(id: String) {
        viewModelScope.launch {
            repository.deleteAddon(id)
        }
    }

    fun saveVariation(variation: VariationEntity) {
        viewModelScope.launch {
            repository.insertVariation(variation)
        }
    }

    fun deleteVariation(id: String) {
        viewModelScope.launch {
            repository.deleteVariation(id)
        }
    }

    fun saveProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.insertOrUpdateProduct(product)
        }
    }

    fun updateProductStock(id: String, stock: Double) {
        viewModelScope.launch {
            val existing = repository.getProductById(id) ?: return@launch
            repository.updateProduct(existing.copy(stock = stock, lastUpdated = System.currentTimeMillis()))
        }
    }

    fun updateProductMinStock(id: String, minStock: Double) {
        viewModelScope.launch {
            val existing = repository.getProductById(id) ?: return@launch
            repository.updateProduct(existing.copy(minStock = minStock, lastUpdated = System.currentTimeMillis()))
        }
    }

    fun updateProductUnit(id: String, unit: String) {
        viewModelScope.launch {
            val existing = repository.getProductById(id) ?: return@launch
            repository.updateProduct(existing.copy(unit = unit, lastUpdated = System.currentTimeMillis()))
        }
    }

    fun updateProductFavorite(id: String, isFavorite: Boolean) {
        viewModelScope.launch {
            val existing = repository.getProductById(id) ?: return@launch
            repository.updateProduct(existing.copy(isFavorite = isFavorite, lastUpdated = System.currentTimeMillis()))
        }
    }

    fun deleteProduct(id: String) {
        viewModelScope.launch {
            repository.markDeleted(id)
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repository = (application as SyncPosApplication).repository
            return InventoryViewModel(application, repository) as T
        }
    }
}
