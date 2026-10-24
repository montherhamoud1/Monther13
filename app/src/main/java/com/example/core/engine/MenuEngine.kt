package com.example.core.engine

import com.example.domain.model.*

object MenuEngine {

    /**
     * Calculates the true manufacturing & portion cost for a product recipe,
     * including ingredient yields, trim waste percentages, packaging, and labor overhead.
     */
    fun calculateRecipeCost(
        recipe: Recipe,
        ingredientsMap: Map<String, Ingredient>,
        sellingPrice: Double
    ): ProductCostSummary {
        var rawCost = 0.0

        for (item in recipe.items) {
            val ingredient = ingredientsMap[item.ingredientId]
            if (ingredient != null) {
                // Effective yield factor considering trim/cooking waste: yield * (1 - waste)
                val effectiveFactor = (ingredient.yieldPercentage * (1.0 - ingredient.wastePercentage))
                    .coerceIn(0.05, 1.0)
                val lineCost = (item.quantity * ingredient.costPerUnit) / effectiveFactor
                rawCost += lineCost
            }
        }

        val totalRawPerPortion = rawCost / recipe.yieldCount.coerceAtLeast(1.0)
        val totalCost = totalRawPerPortion + recipe.packagingCost + recipe.laborCost
        val grossProfit = sellingPrice - totalCost
        val marginPercentage = if (sellingPrice > 0.0) (grossProfit / sellingPrice) * 100.0 else 0.0
        val cogsPercentage = if (sellingPrice > 0.0) (totalCost / sellingPrice) * 100.0 else 0.0

        return ProductCostSummary(
            productId = recipe.productId,
            sellingPrice = sellingPrice,
            rawIngredientsCost = totalRawPerPortion,
            packagingCost = recipe.packagingCost,
            laborCost = recipe.laborCost,
            totalCost = totalCost,
            grossProfit = grossProfit,
            grossMarginPercentage = marginPercentage,
            cogsPercentage = cogsPercentage
        )
    }

    /**
     * Unified Price Resolution:
     * Resolves the final price based on the hierarchy:
     * 1. Exact Match: (ProductId, VariantId, BranchId, Channel, CustomerTier)
     * 2. Branch & Channel Match: (ProductId, VariantId, BranchId, Channel)
     * 3. Channel Match: (ProductId, VariantId, Channel)
     * 4. Branch Specific Match: (ProductId, VariantId, BranchId)
     * 5. Fallback: Base Product Price + Variant Price Adjustment
     */
    fun resolveProductPrice(
        product: Product,
        variant: ProductVariant? = null,
        branchId: String? = null,
        channel: SalesChannel = SalesChannel.POS,
        customerTier: String? = null,
        rules: List<ChannelPriceRule> = emptyList()
    ): Double {
        // Find matching rules for this product
        val candidateRules = rules.filter { it.productId == product.id }

        // 1. Check exact rule
        val exactRule = candidateRules.find {
            (it.variantId == null || it.variantId == variant?.id) &&
            (it.branchId == null || it.branchId == branchId) &&
            (it.channel == null || it.channel == channel) &&
            (it.customerTier == null || it.customerTier == customerTier)
        }

        if (exactRule != null) {
            val variantAdj = if (exactRule.variantId != null) 0.0 else (variant?.priceAdjustment ?: 0.0)
            return exactRule.customPrice + variantAdj
        }

        // 2. Channel rule
        val channelRule = candidateRules.find {
            it.channel == channel && (it.variantId == null || it.variantId == variant?.id)
        }
        if (channelRule != null) {
            val variantAdj = if (channelRule.variantId != null) 0.0 else (variant?.priceAdjustment ?: 0.0)
            return channelRule.customPrice + variantAdj
        }

        // Base price fallback
        val basePrice = product.price
        val variantAdjustment = variant?.priceAdjustment ?: 0.0
        return basePrice + variantAdjustment
    }

    /**
     * Generates a channel-specific unified menu consumed identically across:
     * POS, Waiter App, QR Table Ordering, Customer Web Store, and Mobile App.
     */
    fun getUnifiedMenuForChannel(
        categories: List<Category>,
        products: List<Product>,
        channel: SalesChannel,
        branchId: String,
        priceRules: List<ChannelPriceRule>
    ): List<ChannelMenuCategory> {
        // Filter categories available for this branch
        val activeCategories = categories.filter { cat ->
            cat.isActive && (cat.branchAvailability.isEmpty() || cat.branchAvailability.contains(branchId))
        }.sortedBy { it.sortOrder }

        return activeCategories.map { category ->
            val categoryProducts = products.filter { prod ->
                prod.categoryId == category.id &&
                prod.isAvailable &&
                prod.isSellable &&
                (prod.branchAvailability.isEmpty() || prod.branchAvailability.contains(branchId))
            }.map { prod ->
                val resolvedPrice = resolveProductPrice(
                    product = prod,
                    branchId = branchId,
                    channel = channel,
                    rules = priceRules
                )
                ChannelMenuItem(
                    product = prod,
                    resolvedPrice = resolvedPrice,
                    channel = channel
                )
            }

            ChannelMenuCategory(
                category = category,
                items = categoryProducts
            )
        }.filter { it.items.isNotEmpty() }
    }

    /**
     * Validates modifier selections based on required flags and min/max constraints.
     */
    fun validateModifierSelection(
        group: ModifierGroup,
        selectedOptionIds: List<String>
    ): ModifierValidationResult {
        val count = selectedOptionIds.size
        if (group.isRequired && count < group.minSelections) {
            return ModifierValidationResult(
                isValid = false,
                errorMessageEn = "Please select at least ${group.minSelections} option(s) for ${group.nameEn}",
                errorMessageAr = "يرجى اختيار ${group.minSelections} خيار على الأقل لـ ${group.nameAr}"
            )
        }
        if (count > group.maxSelections) {
            return ModifierValidationResult(
                isValid = false,
                errorMessageEn = "Maximum ${group.maxSelections} selection(s) allowed for ${group.nameEn}",
                errorMessageAr = "الحد الأقصى المسموح به هو ${group.maxSelections} خيار لـ ${group.nameAr}"
            )
        }
        return ModifierValidationResult(isValid = true)
    }

    /**
     * Computes total combo price factoring in base combo price, premium component upgrades,
     * and channel price rules.
     */
    fun calculateComboPrice(
        combo: ComboProduct,
        componentSelections: Map<String, Product>, // componentId -> selected Product
        channel: SalesChannel = SalesChannel.POS,
        priceRules: List<ChannelPriceRule> = emptyList()
    ): Double {
        var total = combo.comboBasePrice

        // Check if there is a channel override for the combo product
        val comboChannelRule = priceRules.find { it.productId == combo.productId && it.channel == channel }
        if (comboChannelRule != null) {
            total = comboChannelRule.customPrice
        }

        for (comp in combo.components) {
            total += comp.extraCharge
            val selectedProd = componentSelections[comp.id]
            if (selectedProd != null && selectedProd.price > 0.0) {
                // If a component has an individual premium surcharge
                val defaultProdId = comp.defaultProductId
                if (defaultProdId != null && selectedProd.id != defaultProdId) {
                    // Difference if higher than default
                    val diff = (selectedProd.price - 5.0).coerceAtLeast(0.0)
                    total += diff * 0.5 // combo discounted upgrade price
                }
            }
        }
        return total
    }

    /**
     * Calculates tax amounts according to inclusive or exclusive profile rules.
     */
    fun calculateTax(
        amount: Double,
        taxProfile: TaxProfile
    ): TaxCalculationResult {
        return if (taxProfile.isInclusive) {
            val net = amount / (1.0 + taxProfile.rate)
            val tax = amount - net
            TaxCalculationResult(
                netAmount = net,
                taxAmount = tax,
                grossAmount = amount,
                isInclusive = true,
                taxRate = taxProfile.rate
            )
        } else {
            val tax = amount * taxProfile.rate
            val gross = amount + tax
            TaxCalculationResult(
                netAmount = amount,
                taxAmount = tax,
                grossAmount = gross,
                isInclusive = false,
                taxRate = taxProfile.rate
            )
        }
    }

    /**
     * Evaluates schedule availability for a product based on day of week and current HH:mm.
     */
    fun isAvailableInSchedule(
        schedule: ProductSchedule?,
        dayOfWeek: Int = 1, // 1 = Monday / Sunday
        currentTime: String = "12:00"
    ): Boolean {
        if (schedule == null) return true
        if (!schedule.isAvailable || schedule.isOutOfStock) return false
        if (schedule.daysOfWeek.isNotEmpty() && !schedule.daysOfWeek.contains(dayOfWeek)) {
            return false
        }
        if (currentTime < schedule.startTime || currentTime > schedule.endTime) {
            return false
        }
        return true
    }
}

data class ModifierValidationResult(
    val isValid: Boolean,
    val errorMessageEn: String = "",
    val errorMessageAr: String = ""
)

data class TaxCalculationResult(
    val netAmount: Double,
    val taxAmount: Double,
    val grossAmount: Double,
    val isInclusive: Boolean,
    val taxRate: Double
)

data class ChannelMenuItem(
    val product: Product,
    val resolvedPrice: Double,
    val channel: SalesChannel
)

data class ChannelMenuCategory(
    val category: Category,
    val items: List<ChannelMenuItem>
)
