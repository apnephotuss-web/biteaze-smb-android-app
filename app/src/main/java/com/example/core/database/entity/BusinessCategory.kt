package com.example.core.database.entity

sealed interface BusinessCategory {
    object FoodAndBeverage : BusinessCategory
    object Grocery : BusinessCategory
    object Apparel : BusinessCategory
    object Electronics : BusinessCategory
    object Services : BusinessCategory
}
