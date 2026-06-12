package com.example.core.database.entity

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
sealed interface IndustryMetadata {
    @Serializable
    data class BatchInfo(
        val id: String,
        val quantity: Double,
        val expiryDate: Long
    )

    @Serializable
    data class GroceryVariation(
        val id: String,
        val name: String,
        val price: Double,
        val stock: Double,
        val batches: List<BatchInfo> = emptyList()
    )

    @Serializable
    @SerialName("grocery")
    data class Grocery(
        val batches: List<BatchInfo> = emptyList(),
        val isFractional: Boolean = false,
        val variations: List<GroceryVariation> = emptyList()
    ) : IndustryMetadata

    @Serializable
    @SerialName("apparel")
    data class Apparel(
        val size: String = "",
        val color: String = "",
        val material: String = "",
        val skuMatrix: Map<String, String> = emptyMap()
    ) : IndustryMetadata

    @Serializable
    @SerialName("electronics")
    data class Electronics(
        val serialNumber: String = "",
        val warrantyMonths: Int = 0,
        val requiresImei: Boolean = false
    ) : IndustryMetadata

    @Serializable
    @SerialName("food_and_beverage")
    data class FoodAndBeverage(
        val modifierGroupIds: List<String> = emptyList(),
        val kitchenRouting: String = "Kitchen",
        val customAddonPrices: Map<String, Double> = emptyMap(), // addonId -> customPrice
        val customVariationPrices: Map<String, Double> = emptyMap() // optionName -> customPrice
    ) : IndustryMetadata

    @Serializable
    @SerialName("services")
    data class Services(
        val providerName: String = "",
        val durationMinutes: Int = 0,
        val isOffsite: Boolean = false
    ) : IndustryMetadata

    @Serializable
    @SerialName("generic")
    data class Generic(
        val notes: String = ""
    ) : IndustryMetadata
}
