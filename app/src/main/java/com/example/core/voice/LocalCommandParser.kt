package com.example.core.voice

import com.example.core.database.dao.VoiceIndexedProduct

class LocalCommandParser {

    private val wordMap = mapOf(
        // English Base
        "zero" to 0.0, "one" to 1.0, "two" to 2.0, "three" to 3.0, "four" to 4.0, 
        "five" to 5.0, "six" to 6.0, "seven" to 7.0, "eight" to 8.0, "nine" to 9.0, "ten" to 10.0,
        // English Scales & Fractions
        "half" to 0.5, "quarter" to 0.25, "double" to 2.0, "triple" to 3.0,
        // Gujarati Base
        "shunya" to 0.0, "ek" to 1.0, "be" to 2.0, "tran" to 3.0, "char" to 4.0, "chaar" to 4.0,
        "panch" to 5.0, "paanch" to 5.0, "chha" to 6.0, "che" to 6.0, "saat" to 7.0, "aath" to 8.0,
        "nav" to 9.0, "das" to 10.0,
        // Gujarati Teens
        "agiyar" to 11.0, "bar" to 12.0, "baar" to 12.0, "ter" to 13.0, "chaud" to 14.0,
        "pandar" to 15.0, "sol" to 16.0, "satar" to 17.0, "adhar" to 18.0, "oganis" to 19.0,
        // Gujarati Tens
        "vis" to 20.0, "vees" to 20.0, "tris" to 30.0, "chalis" to 40.0, "pachas" to 50.0,
        "saath" to 60.0, "siter" to 70.0, "ensi" to 80.0, "nevu" to 90.0,
        // Gujarati Multipliers
        "so" to 100.0, "hajar" to 1000.0, "hazaar" to 1000.0, "ardhu" to 0.5, "pa" to 0.25,
        "bamnu" to 2.0, "tramnu" to 3.0, "tranganu" to 3.0,
        // Hindi Base
        "do" to 2.0, "teen" to 3.0, "chhah" to 6.0, "nau" to 9.0,
        // Hindi Teens
        "gyarah" to 11.0, "barah" to 12.0, "terah" to 13.0, "chaudah" to 14.0, "pandrah" to 15.0,
        "solah" to 16.0, "satrah" to 17.0, "atharah" to 18.0, "unnis" to 19.0,
        // Hindi Tens
        "bees" to 20.0, "tees" to 30.0, "sattar" to 70.0, "assi" to 80.0, "nabbe" to 90.0, "navve" to 90.0,
        // Hindi Multipliers
        "sau" to 100.0, "aadha" to 0.5, "pao" to 0.25, "dugna" to 2.0, "doguna" to 2.0, "tiguna" to 3.0
    )

    private val scaleWords = setOf(
        "so", "sau", "hundred", "hajar", "hazaar", "thousand"
    )

    private val multiplierWords = setOf(
        "double", "triple", "bamnu", "tramnu", "tranganu", "dugna", "doguna", "tiguna"
    )

    private val priceKeywords = setOf("price", "rate", "cost", "rupees", "rs")
    private val alertKeywords = setOf("minimum", "min", "min stock", "min quantity", "min_stock", "alert")

    private val actionWords = mapOf(
        "plus" to "ADD", "add" to "ADD",
        "minus" to "REMOVE", "remove" to "REMOVE",
        "delete" to "DELETE", "hata" to "DELETE", "hatao" to "DELETE",
        "hatay" to "DELETE", "hatavo" to "DELETE", "hataiye" to "DELETE"
    )

    fun parseNumberText(text: String): Double? {
        val direct = text.toDoubleOrNull()
        if (direct != null) return direct

        val clean = text.trim().lowercase()
        if (clean.isEmpty()) return null

        val tokens = clean.split("[\\s\\-_]+".toRegex()).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null

        var totalSum = 0.0
        var currentSection = 0.0
        var parsedAny = false

        for (token in tokens) {
            val directNum = token.toDoubleOrNull()
            if (directNum != null) {
                if (currentSection > 0.0) {
                    totalSum += currentSection
                }
                currentSection = directNum
                parsedAny = true
                continue
            }

            val value = wordMap[token]
            if (value != null) {
                parsedAny = true
                if (scaleWords.contains(token)) {
                    if (currentSection == 0.0) {
                        currentSection = 1.0
                    }
                    currentSection *= value
                } else if (multiplierWords.contains(token)) {
                    if (currentSection > 0.0) {
                        currentSection *= value
                    } else {
                        currentSection = value
                    }
                } else {
                    currentSection += value
                }
            }
        }
        totalSum += currentSection
        return if (parsedAny) totalSum else null
    }

    suspend fun parseCommand(
        transcript: String,
        mode: OperationMode,
        inventory: List<VoiceIndexedProduct>
    ): VoiceCommandIntent {
        val input = transcript.trim().lowercase()

        // 1. Process Segment Chaining Connectors (e.g. "next", "pachi", "aur", "badme")
        // Language Delimiters: next, pachi, pahi, ane, aur, badme
        val segments = input.split("\\b(?:next|pachi|pahi|ane|aur|badme)\\b".toRegex(RegexOption.IGNORE_CASE))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (segments.size > 1) {
            val childIntents = segments.map { parseSingleSegment(it, mode, inventory) }
            return VoiceCommandIntent.CompositeIntent(childIntents)
        }

        return parseSingleSegment(input, mode, inventory)
    }

    private fun parseSingleSegment(
        segment: String,
        mode: OperationMode,
        inventory: List<VoiceIndexedProduct>
    ): VoiceCommandIntent {
        val completionKeywords = listOf("done done", "done", "dan", "dun", "completo", "complete")
        var isCompletion = false
        var cleaned = segment

        for (kw in completionKeywords) {
            if (cleaned.matches(".*\\b$kw\\b.*".toRegex(RegexOption.IGNORE_CASE))) {
                isCompletion = true
                cleaned = cleaned.replace("\\b$kw\\b".toRegex(RegexOption.IGNORE_CASE), "").trim()
            }
        }

        if (cleaned.isEmpty()) {
            if (isCompletion) {
                return VoiceCommandIntent.CheckoutCart("BILL")
            }
            return VoiceCommandIntent.Unrecognized(segment)
        }

        val baseIntent = when (mode) {
            OperationMode.BILLING_MODE -> parseBillingCommand(cleaned, inventory)
            OperationMode.INVENTORY_MODE -> parseInventoryCommand(cleaned, inventory)
        }

        if (isCompletion) {
            return VoiceCommandIntent.CompositeIntent(listOf(baseIntent, VoiceCommandIntent.CheckoutCart("BILL")))
        }

        return baseIntent
    }

    private fun parseBillingCommand(input: String, inventory: List<VoiceIndexedProduct>): VoiceCommandIntent {
        if (input.contains("save and hold") || input.contains("save & hold") || input.contains("park order")) {
            return VoiceCommandIntent.CheckoutCart("HOLD")
        }

        if (input.contains("save and bill") || input.contains("save & bill")) {
            return VoiceCommandIntent.CheckoutCart("BILL")
        }

        if (input.contains("clear all") || input.contains("reset billing cart") || input.contains("clear cart")) {
            return VoiceCommandIntent.ClearCart
        }

        if (input.contains("checkout") || input.contains("complete order") || input.contains("pay")) {
            val mode = if (input.contains("card")) "CARD"
                       else if (input.contains("upi") || input.contains("qr")) "UPI"
                       else if (input.contains("held") || input.contains("hold")) "HOLD"
                       else "CASH"
            return VoiceCommandIntent.CheckoutCart(mode)
        }

        if (input.contains("discount")) {
            val discountMatch = "discount[ a-z]* (?:of )?([0-9a-z\\s]+)".toRegex().find(input)
            val valStr = discountMatch?.groupValues?.get(1)?.trim() ?: ""
            val amount = parseNumberText(valStr) ?: 0.0
            val type = if (input.contains("percent")) "PERCENT" else "FIXED"
            if (amount > 0) return VoiceCommandIntent.ApplyCartDiscount(amount, type)
        }

        // Search for product match using exact contains (longest first) & Levenshtein
        val product = findProductMatch(input, inventory)
        if (product != null) {
            val segmentTokens = input.split("\\s+".toRegex())
            val matchedTokens = product.name.lowercase().split("\\s+".toRegex())

            // Find match boundary inside segment tokens
            var matchedIdx = -1
            for (i in 0..(segmentTokens.size - matchedTokens.size)) {
                var isMatch = true
                for (j in matchedTokens.indices) {
                    val sim = QualityMatcher.calculateSimilarity(segmentTokens[i + j], matchedTokens[j])
                    if (sim < 0.75f) {
                        isMatch = false
                        break
                    }
                }
                if (isMatch) {
                    matchedIdx = i
                    break
                }
            }

            // Window: 3 tokens before, 3 tokens after boundary
            val beforeTokens = if (matchedIdx > 0) {
                segmentTokens.subList(maxOf(0, matchedIdx - 3), matchedIdx)
            } else emptyList()

            val afterTokens = if (matchedIdx != -1 && matchedIdx + matchedTokens.size < segmentTokens.size) {
                segmentTokens.subList(matchedIdx + matchedTokens.size, minOf(segmentTokens.size, matchedIdx + matchedTokens.size + 3))
            } else emptyList()

            val surround = beforeTokens + afterTokens

            // 1. Resolve Action verb
            var action = "ADD"
            for (token in surround) {
                val act = actionWords[token]
                if (act != null) {
                    action = act
                    break
                }
            }

            // 2. Resolve Quantity (implicit operator fallback defaults to 1.0)
            var quantity = 1.0
            for (token in surround) {
                val num = parseNumberText(token)
                if (num != null) {
                    quantity = num
                    break
                }
            }

            // 3. Resolve Custom Price overrides (e.g. "Add Milk with price seventy five")
            var customPrice: Double? = null
            for (i in segmentTokens.indices) {
                if (priceKeywords.contains(segmentTokens[i])) {
                    if (i + 1 < segmentTokens.size) {
                        customPrice = parseNumberText(segmentTokens[i + 1])
                    }
                    break
                }
            }

            return when (action) {
                "REMOVE" -> VoiceCommandIntent.RemoveCartItem(product.id, quantity)
                "DELETE" -> VoiceCommandIntent.RemoveCartItem(product.id, null)
                else -> VoiceCommandIntent.AddCartItem(product.id, quantity, customPrice)
            }
        }

        return VoiceCommandIntent.Unrecognized(input)
    }

    private fun parseInventoryCommand(input: String, inventory: List<VoiceIndexedProduct>): VoiceCommandIntent {
        if (input.contains("new product") || input.contains("register product")) {
            val nameMatch = "(?:new product|register product) ([a-z0-9\\s]+?)(?: sku| price| unit| stock| category|$)".toRegex().find(input)
            val skuMatch = "sku ([a-z0-9\\-]+)".toRegex().find(input)
            val priceMatch = "price ([0-9a-z\\s]+?)(?: sku| unit| stock| category|$)".toRegex().find(input)
            val stockMatch = "stock ([0-9a-z\\s]+?)(?: sku| price| unit| category|$)".toRegex().find(input)
            val unitMatch = "unit ([a-z]+)".toRegex().find(input)
            val categoryMatch = "category ([a-z0-9\\s]+?)(?: sku| price| unit| stock|$)".toRegex().find(input)

            val name = nameMatch?.groupValues?.get(1)?.trim() ?: "Unknown Product"
            val sku = skuMatch?.groupValues?.get(1)?.trim()
            val price = priceMatch?.groupValues?.get(1)?.let { parseNumberText(it) }
            val stock = stockMatch?.groupValues?.get(1)?.let { parseNumberText(it) }
            val unit = unitMatch?.groupValues?.get(1)?.trim()
            val category = categoryMatch?.groupValues?.get(1)?.trim()

            return VoiceCommandIntent.QuickRegisterProduct(name, sku, price, stock, category, unit)
        }

        val product = findProductMatch(input, inventory)
        if (product != null) {
            val tokens = input.split("\\s+".toRegex())

            // 1. Check for Price Updates
            var hasPriceKeyword = false
            for (kw in priceKeywords) {
                if (input.contains(kw)) {
                    hasPriceKeyword = true
                    break
                }
            }
            if (hasPriceKeyword) {
                for (i in tokens.indices) {
                    if (priceKeywords.contains(tokens[i]) && i + 1 < tokens.size) {
                        val priceVal = parseNumberText(tokens[i + 1])
                        if (priceVal != null) {
                            return VoiceCommandIntent.UpdateProductMetadata(product.id, price = priceVal)
                        }
                    }
                }
            }

            // 2. Check for Safety Limits (Min Stock)
            var hasAlertKeyword = false
            for (kw in alertKeywords) {
                if (input.contains(kw)) {
                    hasAlertKeyword = true
                    break
                }
            }
            if (hasAlertKeyword) {
                for (i in tokens.indices) {
                    if (alertKeywords.contains(tokens[i]) && i + 1 < tokens.size) {
                        val minStockVal = parseNumberText(tokens[i + 1])
                        if (minStockVal != null) {
                            return VoiceCommandIntent.UpdateProductMetadata(product.id, minStock = minStockVal)
                        }
                    }
                }
            }

            // 3. Update physical stock
            var deltaSign = 1
            if (input.contains("remove") || input.contains("minus")) {
                deltaSign = -1
            }

            var numValue: Double? = null
            for (token in tokens) {
                val num = parseNumberText(token)
                if (num != null) {
                    numValue = num
                    break
                }
            }

            if (numValue != null) {
                val delta = numValue * deltaSign
                return if (input.contains("add") || input.contains("plus") || input.contains("remove") || input.contains("minus")) {
                    VoiceCommandIntent.UpdateProductStock(product.id, absoluteValue = null, addDelta = delta)
                } else {
                    VoiceCommandIntent.UpdateProductStock(product.id, absoluteValue = numValue, addDelta = null)
                }
            }
        }

        return VoiceCommandIntent.Unrecognized(input)
    }

    private fun findProductMatch(input: String, inventory: List<VoiceIndexedProduct>): VoiceIndexedProduct? {
        if (input.isBlank()) return null
        val cleanInput = input.lowercase()

        // 1. Search for exact contains (longest product name first to prevent partial substring issues)
        val sorted = inventory.sortedByDescending { it.name.length }
        for (prod in sorted) {
            val name = prod.name.lowercase()
            if (cleanInput.contains(name)) {
                return prod
            }
        }

        // 2. Token similarities with threshold >= 0.75
        val segTokens = cleanInput.split("[\\s\\-_,]+".toRegex()).filter { it.length >= 3 }
        for (prod in sorted) {
            val prodTokens = prod.name.lowercase().split("[\\s\\-_,]+".toRegex()).filter { it.length >= 3 }
            for (pToken in prodTokens) {
                for (sToken in segTokens) {
                    val sim = QualityMatcher.calculateSimilarity(sToken, pToken)
                    if (sim >= 0.75f) {
                        return prod
                    }
                }
            }
        }
        return null
    }
}
