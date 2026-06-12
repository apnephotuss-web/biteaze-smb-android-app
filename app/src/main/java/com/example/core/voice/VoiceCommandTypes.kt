package com.example.core.voice

data class ParsedProductInput(
    val item: String? = null,
    val sku: String? = null,
    val price: Double? = null,
    val stock: Double? = null,
    val unit: String? = null,
    val minCount: Double? = null,
    val category: String? = null,
    val taxRate: Double? = null,
    val isFavorite: Boolean = false
)

enum class OperationMode { 
    BILLING_MODE, 
    INVENTORY_MODE 
}

sealed interface VoiceState {
    object Idle : VoiceState
    object Listening : VoiceState
    object Processing : VoiceState
    data class Success(val commandText: String) : VoiceState
    data class Error(val errorMessage: String) : VoiceState
}

sealed interface VoiceCommandIntent {
    // Billing Intents
    data class AddCartItem(val productCode: String, val quantity: Double, val customPrice: Double?) : VoiceCommandIntent
    data class RemoveCartItem(val productCode: String, val quantity: Double? = null) : VoiceCommandIntent
    data class ApplyCartDiscount(val amount: Double, val type: String) : VoiceCommandIntent // "PERCENT" or "FIXED"
    data class CheckoutCart(val paymentMode: String) : VoiceCommandIntent // "CASH", "CARD", "UPI", "HOLD"
    object ClearCart : VoiceCommandIntent
    
    // Joint / Composite Intent for chained commands
    data class CompositeIntent(val intents: List<VoiceCommandIntent>) : VoiceCommandIntent
    
    // Inventory New & Edit Intents
    data class QuickRegisterProduct(
        val name: String, 
        val sku: String?, 
        val price: Double?, 
        val stock: Double?, 
        val category: String?, 
        val unit: String?
    ) : VoiceCommandIntent
    data class UpdateProductStock(val productCode: String, val absoluteValue: Double?, val addDelta: Double?) : VoiceCommandIntent
    data class UpdateProductMetadata(
        val productCode: String,
        val price: Double? = null,
        val minStock: Double? = null,
        val unit: String? = null,
        val isFavorite: Boolean? = null
    ) : VoiceCommandIntent
    
    data class Unrecognized(val rawText: String) : VoiceCommandIntent
}
