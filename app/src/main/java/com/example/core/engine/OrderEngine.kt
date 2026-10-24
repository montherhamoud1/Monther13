package com.example.core.engine

import com.example.core.constants.AppConstants
import com.example.domain.model.*
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Phase 4: Central Order Engine & POS Transaction Processor
 * Enforces controlled state transitions, multi-mode pricing, modifiers/addons aggregation,
 * coupon & discount validation, mixed-tender payments, bill splitting, order merging,
 * table transfers, refunds/returns, shift reconciliation, and idempotency.
 */
object OrderEngine {

    // --- 1. Controlled State Machine ---
    val VALID_TRANSITIONS: Map<OrderStatus, Set<OrderStatus>> = mapOf(
        OrderStatus.DRAFT to setOf(
            OrderStatus.HELD,
            OrderStatus.PENDING,
            OrderStatus.PREPARING,
            OrderStatus.COMPLETED,
            OrderStatus.CANCELLED
        ),
        OrderStatus.HELD to setOf(
            OrderStatus.DRAFT,
            OrderStatus.PENDING,
            OrderStatus.PREPARING,
            OrderStatus.COMPLETED,
            OrderStatus.CANCELLED
        ),
        OrderStatus.PENDING to setOf(
            OrderStatus.HELD,
            OrderStatus.PREPARING,
            OrderStatus.READY,
            OrderStatus.COMPLETED,
            OrderStatus.CANCELLED
        ),
        OrderStatus.PREPARING to setOf(
            OrderStatus.READY,
            OrderStatus.SERVED,
            OrderStatus.OUT_FOR_DELIVERY,
            OrderStatus.COMPLETED,
            OrderStatus.CANCELLED
        ),
        OrderStatus.READY to setOf(
            OrderStatus.SERVED,
            OrderStatus.OUT_FOR_DELIVERY,
            OrderStatus.COMPLETED,
            OrderStatus.CANCELLED
        ),
        OrderStatus.SERVED to setOf(
            OrderStatus.COMPLETED,
            OrderStatus.REFUNDED,
            OrderStatus.RETURNED
        ),
        OrderStatus.OUT_FOR_DELIVERY to setOf(
            OrderStatus.COMPLETED,
            OrderStatus.CANCELLED,
            OrderStatus.RETURNED
        ),
        OrderStatus.COMPLETED to setOf(
            OrderStatus.REFUNDED,
            OrderStatus.RETURNED
        ),
        OrderStatus.RETURNED to setOf(
            OrderStatus.REFUNDED
        ),
        OrderStatus.CANCELLED to emptySet(), // Terminal state
        OrderStatus.REFUNDED to emptySet()   // Terminal state
    )

    sealed class OrderTransitionResult {
        data class Success(val updatedOrder: Order) : OrderTransitionResult()
        data class InvalidTransition(
            val from: OrderStatus,
            val to: OrderStatus,
            val message: String
        ) : OrderTransitionResult()
        data class PermissionDenied(
            val requiredPermission: String,
            val message: String
        ) : OrderTransitionResult()
    }

    fun canTransition(from: OrderStatus, to: OrderStatus): Boolean {
        if (from == to) return true
        return VALID_TRANSITIONS[from]?.contains(to) == true
    }

    fun getAllowedNextStates(current: OrderStatus): List<OrderStatus> {
        return VALID_TRANSITIONS[current]?.toList() ?: emptyList()
    }

    fun transitionOrder(
        order: Order,
        targetStatus: OrderStatus,
        reason: String = "",
        approvedBy: String? = null,
        now: Long = System.currentTimeMillis()
    ): OrderTransitionResult {
        if (!canTransition(order.status, targetStatus)) {
            return OrderTransitionResult.InvalidTransition(
                from = order.status,
                to = targetStatus,
                message = "Invalid state transition from ${order.status.code} to ${targetStatus.code}."
            )
        }

        if (targetStatus == OrderStatus.CANCELLED && reason.isBlank()) {
            return OrderTransitionResult.InvalidTransition(
                from = order.status,
                to = targetStatus,
                message = "Cancellation reason is required to void or cancel an order."
            )
        }

        if ((targetStatus == OrderStatus.REFUNDED || targetStatus == OrderStatus.RETURNED) && reason.isBlank()) {
            return OrderTransitionResult.InvalidTransition(
                from = order.status,
                to = targetStatus,
                message = "Refund/return reason is required."
            )
        }

        val updatedPaymentStatus = when (targetStatus) {
            OrderStatus.REFUNDED -> "REFUNDED"
            OrderStatus.COMPLETED -> if (order.paymentStatus == "UNPAID") "PAID" else order.paymentStatus
            else -> order.paymentStatus
        }

        val updated = order.copy(
            status = targetStatus,
            paymentStatus = updatedPaymentStatus,
            updatedAt = now,
            completedAt = if (targetStatus == OrderStatus.COMPLETED) now else order.completedAt,
            cancelledReason = if (targetStatus == OrderStatus.CANCELLED) reason else order.cancelledReason,
            refundReason = if (targetStatus == OrderStatus.REFUNDED || targetStatus == OrderStatus.RETURNED) reason else order.refundReason,
            refundedAmount = if (targetStatus == OrderStatus.REFUNDED && order.refundedAmount == 0.0) order.totalAmount else order.refundedAmount,
            approvedByManager = approvedBy ?: order.approvedByManager
        )

        return OrderTransitionResult.Success(updated)
    }

    // --- 2. POS Cart Line & Financial Calculation ---
    data class PosCartItem(
        val uid: String = UUID.randomUUID().toString(),
        val product: Product,
        val selectedVariant: ProductVariant? = product.variants.find { it.isDefault } ?: product.variants.firstOrNull(),
        val selectedModifiers: List<ModifierOption> = emptyList(),
        val selectedAddons: Map<ProductAddon, Int> = emptyMap(),
        val quantity: Int = 1,
        val baseResolvedPrice: Double = product.price + (selectedVariant?.priceAdjustment ?: 0.0),
        val itemDiscountAmount: Double = 0.0,
        val notes: String = ""
    ) {
        val modifiersDelta: Double
            get() = roundCurrency(selectedModifiers.sumOf { it.priceAdjustment })

        val addonsDelta: Double
            get() = roundCurrency(selectedAddons.entries.sumOf { it.key.price * it.value })

        val unitPriceWithCustomizations: Double
            get() = roundCurrency(baseResolvedPrice + modifiersDelta + addonsDelta)

        val grossTotal: Double
            get() = roundCurrency(unitPriceWithCustomizations * quantity)

        val netTotal: Double
            get() = roundCurrency((grossTotal - itemDiscountAmount).coerceAtLeast(0.0))

        val modifiersSummaryText: String
            get() = selectedModifiers.joinToString(", ") { it.nameEn }

        val addonsSummaryText: String
            get() = selectedAddons.entries.filter { it.value > 0 }
                .joinToString(", ") { "${it.value}x ${it.key.nameEn}" }

        fun toOrderItem(orderId: String): OrderItem {
            return OrderItem(
                id = uid,
                orderId = orderId,
                productId = product.id,
                productNameEn = product.nameEn,
                productNameAr = product.nameAr,
                unitPrice = unitPriceWithCustomizations,
                quantity = quantity,
                totalPrice = netTotal,
                notes = notes,
                kitchenStatus = if (product.isPreparationItem) "PENDING" else "READY",
                variantId = selectedVariant?.id,
                variantName = selectedVariant?.nameEn,
                modifiersSummary = modifiersSummaryText,
                modifiersDelta = modifiersDelta,
                addonsSummary = addonsSummaryText,
                addonsDelta = addonsDelta,
                discountAmount = itemDiscountAmount
            )
        }
    }

    data class OrderFinancialSummary(
        val grossItemsSubtotal: Double,
        val itemLevelDiscounts: Double,
        val orderLevelDiscount: Double,
        val couponDiscount: Double,
        val totalDiscount: Double,
        val netSubtotalAfterDiscount: Double,
        val serviceFee: Double,
        val deliveryFee: Double,
        val taxAmount: Double,
        val isTaxInclusive: Boolean,
        val totalPayable: Double,
        val appliedCouponCode: String? = null,
        val couponValidationError: String? = null
    )

    fun calculateOrderFinancials(
        cartItems: List<PosCartItem>,
        orderType: OrderType,
        manualDiscountType: DiscountType = DiscountType.PERCENTAGE,
        manualDiscountValue: Double = 0.0,
        coupon: Coupon? = null,
        taxProfile: TaxProfile = TaxProfile("tax_standard_10", "VAT 10%", "ضريبة 10%", AppConstants.DEFAULT_TAX_RATE, isInclusive = false),
        applyDineInServiceFee: Boolean = false,
        serviceFeeRate: Double = AppConstants.DEFAULT_SERVICE_FEE_RATE,
        customDeliveryFee: Double = AppConstants.DEFAULT_DELIVERY_FEE
    ): OrderFinancialSummary {
        val grossSubtotal = roundCurrency(cartItems.sumOf { it.grossTotal })
        val itemDiscounts = roundCurrency(cartItems.sumOf { it.itemDiscountAmount.coerceAtMost(it.grossTotal) })
        val afterItemDiscounts = (grossSubtotal - itemDiscounts).coerceAtLeast(0.0)

        // Manual Order-Level Discount
        val rawOrderDiscount = when (manualDiscountType) {
            DiscountType.PERCENTAGE -> afterItemDiscounts * (manualDiscountValue.coerceIn(0.0, 100.0) / 100.0)
            DiscountType.FIXED_AMOUNT -> manualDiscountValue.coerceAtLeast(0.0)
        }
        val orderDiscount = roundCurrency(rawOrderDiscount.coerceAtMost(afterItemDiscounts))
        val afterManualDiscount = (afterItemDiscounts - orderDiscount).coerceAtLeast(0.0)

        // Coupon Validation & Discount
        var couponDiscount = 0.0
        var couponError: String? = null
        var validCouponCode: String? = null

        if (coupon != null) {
            if (!coupon.isActive) {
                couponError = "Coupon ${coupon.code} is inactive."
            } else if (afterManualDiscount < coupon.minOrderAmount) {
                couponError = "Coupon ${coupon.code} requires minimum subtotal of $${coupon.minOrderAmount}."
            } else {
                val rawCoupon = when (coupon.discountType) {
                    DiscountType.PERCENTAGE -> afterManualDiscount * (coupon.value / 100.0)
                    DiscountType.FIXED_AMOUNT -> coupon.value
                }
                val cappedCoupon = if (coupon.maxDiscountAmount != null) {
                    rawCoupon.coerceAtMost(coupon.maxDiscountAmount)
                } else {
                    rawCoupon
                }
                couponDiscount = roundCurrency(cappedCoupon.coerceAtMost(afterManualDiscount))
                validCouponCode = coupon.code
            }
        }

        val totalDiscount = roundCurrency(itemDiscounts + orderDiscount + couponDiscount)
        val netSubtotal = roundCurrency((grossSubtotal - totalDiscount).coerceAtLeast(0.0))

        // Service Fee (only for DINE_IN when enabled and cart is non-empty)
        val serviceFee = if (cartItems.isNotEmpty() && orderType == OrderType.DINE_IN && applyDineInServiceFee) {
            roundCurrency(netSubtotal * serviceFeeRate)
        } else {
            0.0
        }

        // Delivery Fee (only for DELIVERY when cart is non-empty)
        val deliveryFee = if (cartItems.isNotEmpty() && orderType == OrderType.DELIVERY) {
            roundCurrency(customDeliveryFee.coerceAtLeast(0.0))
        } else {
            0.0
        }

        // Tax Calculation on (netSubtotal + serviceFee)
        val taxableBase = roundCurrency(netSubtotal + serviceFee)
        val taxBreakdown = MenuEngine.calculateTax(taxableBase, taxProfile)
        val totalPayable = roundCurrency(taxBreakdown.grossAmount + deliveryFee)

        return OrderFinancialSummary(
            grossItemsSubtotal = grossSubtotal,
            itemLevelDiscounts = itemDiscounts,
            orderLevelDiscount = orderDiscount,
            couponDiscount = couponDiscount,
            totalDiscount = totalDiscount,
            netSubtotalAfterDiscount = netSubtotal,
            serviceFee = serviceFee,
            deliveryFee = deliveryFee,
            taxAmount = taxBreakdown.taxAmount,
            isTaxInclusive = taxProfile.isInclusive,
            totalPayable = totalPayable,
            appliedCouponCode = validCouponCode,
            couponValidationError = couponError
        )
    }

    // --- 3. Payment & Mixed-Tender Engine with Idempotency ---
    data class PaymentResolutionSummary(
        val deduplicatedPayments: List<Payment>,
        val totalPaid: Double,
        val remainingBalance: Double,
        val totalChangeDue: Double,
        val paymentStatus: String // UNPAID, PARTIAL, PAID
    )

    fun resolvePayments(
        orderTotal: Double,
        payments: List<Payment>
    ): PaymentResolutionSummary {
        // Deduplicate by idempotencyKey to guarantee no double-charging
        val uniquePayments = payments
            .filter { it.status == "SUCCESS" }
            .distinctBy { it.idempotencyKey }

        val totalApplied = roundCurrency(uniquePayments.sumOf { it.amount })
        val totalChange = roundCurrency(uniquePayments.sumOf { it.changeAmount })
        val remaining = roundCurrency((orderTotal - totalApplied).coerceAtLeast(0.0))

        val status = when {
            totalApplied <= 0.0 -> "UNPAID"
            remaining <= 0.009 -> "PAID"
            else -> "PARTIAL"
        }

        return PaymentResolutionSummary(
            deduplicatedPayments = uniquePayments,
            totalPaid = totalApplied,
            remainingBalance = remaining,
            totalChangeDue = totalChange,
            paymentStatus = status
        )
    }

    fun createPaymentLeg(
        orderId: String,
        branchId: String,
        remainingOrderBalance: Double,
        tenderedAmount: Double,
        method: PaymentMethod,
        cashierId: String = "user_01",
        shiftId: String? = null,
        referenceNumber: String = "",
        idempotencyKey: String = UUID.randomUUID().toString()
    ): Payment {
        val cleanTender = roundCurrency(tenderedAmount.coerceAtLeast(0.0))
        val appliedAmount = if (method == PaymentMethod.CASH) {
            cleanTender.coerceAtMost(remainingOrderBalance)
        } else {
            cleanTender.coerceAtMost(remainingOrderBalance)
        }
        val change = if (method == PaymentMethod.CASH && cleanTender > remainingOrderBalance) {
            roundCurrency(cleanTender - remainingOrderBalance)
        } else {
            0.0
        }
        val ref = referenceNumber.ifBlank {
            "${method.name.take(3)}-${UUID.randomUUID().toString().take(6).uppercase()}"
        }
        return Payment(
            id = "pay_${UUID.randomUUID().toString().take(8)}",
            orderId = orderId,
            branchId = branchId,
            amount = appliedAmount,
            paymentMethod = method,
            referenceNumber = ref,
            status = "SUCCESS",
            idempotencyKey = idempotencyKey,
            cashierId = cashierId,
            shiftId = shiftId,
            tenderedAmount = cleanTender,
            changeAmount = change
        )
    }

    // --- 4. Split Bill Engine (Equal Split & By Item Split) ---
    fun splitBillEqually(
        order: Order,
        items: List<OrderItem>,
        seatCount: Int
    ): List<SplitSeatShare> {
        val validSeats = seatCount.coerceIn(2, 12)
        val baseSubtotalPerSeat = roundCurrency(order.subtotal / validSeats)
        val baseTaxPerSeat = roundCurrency(order.taxAmount / validSeats)
        val baseServicePerSeat = roundCurrency(order.serviceFee / validSeats)
        val baseDiscountPerSeat = roundCurrency(order.discountAmount / validSeats)
        val baseTotalPerSeat = roundCurrency(order.totalAmount / validSeats)

        return (1..validSeats).map { seat ->
            val isLast = seat == validSeats
            val seatTotal = if (isLast) {
                roundCurrency(order.totalAmount - (baseTotalPerSeat * (validSeats - 1)))
            } else {
                baseTotalPerSeat
            }
            val seatSubtotal = if (isLast) {
                roundCurrency(order.subtotal - (baseSubtotalPerSeat * (validSeats - 1)))
            } else {
                baseSubtotalPerSeat
            }
            val seatTax = if (isLast) {
                roundCurrency(order.taxAmount - (baseTaxPerSeat * (validSeats - 1)))
            } else {
                baseTaxPerSeat
            }
            SplitSeatShare(
                seatNumber = seat,
                label = "Guest #$seat (1/$validSeats Equal Share)",
                items = if (seat == 1) items else emptyList(),
                subtotal = seatSubtotal,
                taxAmount = seatTax,
                serviceFeeShare = baseServicePerSeat,
                discountShare = baseDiscountPerSeat,
                totalDue = seatTotal
            )
        }
    }

    fun splitBillByItems(
        order: Order,
        seatAssignments: Map<Int, List<OrderItem>>
    ): List<SplitSeatShare> {
        val totalItemsSum = seatAssignments.values.flatten().sumOf { it.totalPrice }.coerceAtLeast(0.01)
        return seatAssignments.entries.sortedBy { it.key }.map { (seatNum, seatItems) ->
            val seatItemsSubtotal = roundCurrency(seatItems.sumOf { it.totalPrice })
            val ratio = seatItemsSubtotal / totalItemsSum
            val seatDiscount = roundCurrency(order.discountAmount * ratio)
            val seatService = roundCurrency(order.serviceFee * ratio)
            val seatTax = roundCurrency(order.taxAmount * ratio)
            val seatDelivery = roundCurrency(order.deliveryFee * ratio)
            val seatTotal = roundCurrency((seatItemsSubtotal - seatDiscount + seatService + seatTax + seatDelivery).coerceAtLeast(0.0))

            SplitSeatShare(
                seatNumber = seatNum,
                label = "Guest #$seatNum (${seatItems.sumOf { it.quantity }} items)",
                items = seatItems,
                subtotal = seatItemsSubtotal,
                taxAmount = seatTax,
                serviceFeeShare = seatService,
                discountShare = seatDiscount,
                totalDue = seatTotal
            )
        }
    }

    // --- 5. Merge Orders Engine ---
    data class MergeOrdersResult(
        val mergedOrder: Order,
        val mergedItems: List<OrderItem>
    )

    fun mergeOrders(
        primaryOrder: Order,
        primaryItems: List<OrderItem>,
        secondaryOrder: Order,
        secondaryItems: List<OrderItem>,
        taxRate: Double = AppConstants.DEFAULT_TAX_RATE
    ): MergeOrdersResult? {
        val invalidStatuses = setOf(OrderStatus.CANCELLED, OrderStatus.REFUNDED, OrderStatus.COMPLETED)
        if (primaryOrder.status in invalidStatuses || secondaryOrder.status in invalidStatuses) {
            return null
        }

        // Re-assign secondary items to primaryOrder.id and combine identical product+variant+modifiers+notes
        val combinedItems = mutableListOf<OrderItem>()
        (primaryItems + secondaryItems).forEach { item ->
            val existingIdx = combinedItems.indexOfFirst {
                it.productId == item.productId &&
                    it.variantId == item.variantId &&
                    it.modifiersSummary == item.modifiersSummary &&
                    it.addonsSummary == item.addonsSummary &&
                    it.notes == item.notes
            }
            if (existingIdx >= 0) {
                val existing = combinedItems[existingIdx]
                val newQty = existing.quantity + item.quantity
                val newDiscount = roundCurrency(existing.discountAmount + item.discountAmount)
                val newTotal = roundCurrency((existing.unitPrice * newQty) - newDiscount)
                combinedItems[existingIdx] = existing.copy(
                    quantity = newQty,
                    discountAmount = newDiscount,
                    totalPrice = newTotal
                )
            } else {
                combinedItems.add(item.copy(orderId = primaryOrder.id))
            }
        }

        val newSubtotal = roundCurrency(combinedItems.sumOf { it.totalPrice })
        val combinedDiscount = roundCurrency(primaryOrder.discountAmount + secondaryOrder.discountAmount)
        val combinedServiceFee = roundCurrency(primaryOrder.serviceFee + secondaryOrder.serviceFee)
        val combinedDeliveryFee = maxOf(primaryOrder.deliveryFee, secondaryOrder.deliveryFee)
        val netTaxable = (newSubtotal - combinedDiscount + combinedServiceFee).coerceAtLeast(0.0)
        val newTax = roundCurrency(netTaxable * taxRate)
        val newTotal = roundCurrency(netTaxable + newTax + combinedDeliveryFee)

        val mergedNotes = listOf(
            primaryOrder.notes,
            secondaryOrder.notes,
            "Merged with ${secondaryOrder.orderNumber}"
        ).filter { it.isNotBlank() }.joinToString(" | ")

        val mergedOrder = primaryOrder.copy(
            subtotal = newSubtotal,
            discountAmount = combinedDiscount,
            serviceFee = combinedServiceFee,
            deliveryFee = combinedDeliveryFee,
            taxAmount = newTax,
            totalAmount = newTotal,
            notes = mergedNotes,
            updatedAt = System.currentTimeMillis(),
            items = combinedItems
        )

        return MergeOrdersResult(mergedOrder, combinedItems)
    }

    // --- 6. Security & RBAC Permission Gate ---
    enum class PosSensitiveAction(val permissionCode: String, val labelEn: String) {
        APPLY_HIGH_DISCOUNT("POS:POS_TERMINAL:DISCOUNT", "Apply High Discount (>15%)"),
        CANCEL_ORDER("POS:POS_TERMINAL:CANCEL", "Cancel / Void Order"),
        REFUND_ORDER("POS:POS_TERMINAL:REFUND", "Refund Completed Order"),
        RETURN_ITEMS("POS:POS_TERMINAL:REFUND", "Return Order Items"),
        CASH_DRAWER_PAYOUT("POS:POS_TERMINAL:EDIT", "Cash Out / Safe Drop"),
        CLOSE_SHIFT_VARIANCE("POS:POS_TERMINAL:APPROVE", "Close Shift with Cash Discrepancy")
    }

    fun isAuthorizedForSensitiveAction(
        session: UserSession?,
        userRoleCode: String,
        action: PosSensitiveAction,
        managerPinProvided: String? = null
    ): Boolean {
        if (userRoleCode in listOf("ROLE_ADMIN", "ROLE_MANAGER")) return true
        if (session?.hasPermission(action.permissionCode) == true) return true
        if (managerPinProvided != null && verifyManagerPin(managerPinProvided)) return true
        return false
    }

    fun verifyManagerPin(pin: String): Boolean {
        // Default enterprise supervisor override PINs
        return pin.trim() in setOf("1234", "9999", "0000")
    }

    private fun roundCurrency(value: Double): Double {
        return (value * 100.0).roundToInt() / 100.0
    }
}
