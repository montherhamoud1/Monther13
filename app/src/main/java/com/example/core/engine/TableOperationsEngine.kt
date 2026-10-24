package com.example.core.engine

import com.example.domain.model.*
import java.security.MessageDigest
import java.util.UUID

/**
 * Phase 5: Smart Tables, Secure QR Ordering, Waiter Operations & Reservations Engine
 * Connects Floor/Zone/Table management, Waiter Handheld operations, Customer QR Table sessions,
 * and Reservations directly to the central OrderEngine and MenuEngine.
 */
object TableOperationsEngine {

    private const val QR_SECRET_SALT = "RESTOOPS_ENTERPRISE_HMAC_SALT_2026_V5"

    // --- 1. Secure Opaque QR Token Engine (Never exposes raw internal IDs) ---

    data class ValidatedQrContext(
        val qrToken: String,
        val company: Company,
        val branch: Branch,
        val table: RestaurantTable,
        val floorName: String,
        val zoneName: String
    )

    sealed class QrValidationResult {
        data class Success(val context: ValidatedQrContext) : QrValidationResult()
        data class InvalidToken(val messageEn: String, val messageAr: String) : QrValidationResult()
        data class TableUnavailable(val tableNumber: String, val status: TableStatus, val messageEn: String) : QrValidationResult()
    }

    /**
     * Generates a cryptographic SHA-256 based opaque QR token for a table.
     * Raw IDs (companyId, branchId, tableId) are hashed with a secret salt so they are
     * never exposed in plain text in the QR code or URL.
     */
    fun generateSecureQrToken(
        companyId: String,
        branchId: String,
        tableId: String,
        tableNumber: String,
        nonce: String = "v5"
    ): String {
        val rawPayload = "$companyId|$branchId|$tableId|$tableNumber|$nonce|$QR_SECRET_SALT"
        val digest = MessageDigest.getInstance("SHA-256").digest(rawPayload.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }
        val tokenSegment = hex.take(16).uppercase()
        val checksum = hex.substring(16, 22).uppercase()
        return "QR-SIG-$tokenSegment-$checksum"
    }

    /**
     * Builds the public customer-facing QR URL containing only the opaque token.
     */
    fun buildPublicQrUrl(qrToken: String): String {
        return "https://qr.restoops.enterprise/t/$qrToken"
    }

    /**
     * Validates a scanned QR token (or full QR URL), resolves the restaurant company,
     * branch, floor, zone, and table without exposing internal database IDs.
     */
    fun validateQrToken(
        scannedInput: String,
        company: Company,
        branches: List<Branch>,
        tables: List<RestaurantTable>
    ): QrValidationResult {
        val cleanToken = scannedInput.trim()
            .removePrefix("https://qr.restoops.enterprise/t/")
            .trim()

        if (cleanToken.isBlank() || !cleanToken.startsWith("QR-SIG-")) {
            return QrValidationResult.InvalidToken(
                messageEn = "Malformed or unrecognized QR security token.",
                messageAr = "رمز QR غير صالح أو غير معروف أمنياً."
            )
        }

        val matchedTable = tables.find { it.qrToken.equals(cleanToken, ignoreCase = true) }
            ?: return QrValidationResult.InvalidToken(
                messageEn = "QR token is expired or does not match any active table.",
                messageAr = "انتهت صلاحية رمز QR أو أنه لا يطابق أي طاولة نشطة."
            )

        if (matchedTable.status == TableStatus.UNAVAILABLE || matchedTable.status == TableStatus.CLEANING) {
            return QrValidationResult.TableUnavailable(
                tableNumber = matchedTable.tableNumber,
                status = matchedTable.status,
                messageEn = "Table ${matchedTable.tableNumber} is currently ${matchedTable.status.titleEn} and cannot accept orders right now."
            )
        }

        val matchedBranch = branches.find { it.id == matchedTable.branchId }
            ?: branches.firstOrNull()
            ?: return QrValidationResult.InvalidToken(
                messageEn = "Branch associated with this table could not be resolved.",
                messageAr = "تعذر تحديد الفرع المرتبط بهذه الطاولة."
            )

        return QrValidationResult.Success(
            ValidatedQrContext(
                qrToken = matchedTable.qrToken,
                company = company,
                branch = matchedBranch,
                table = matchedTable,
                floorName = matchedTable.floorName,
                zoneName = matchedTable.section
            )
        )
    }

    /**
     * Creates an authenticated customer table ordering session after QR validation.
     */
    fun createCustomerSession(
        validatedContext: ValidatedQrContext,
        guestName: String = "Table Guest",
        partySize: Int = 2,
        existingOrderId: String? = validatedContext.table.currentOrderId
    ): CustomerTableSession {
        return CustomerTableSession(
            sessionId = "csess_${UUID.randomUUID().toString().take(8)}",
            qrToken = validatedContext.qrToken,
            companyId = validatedContext.company.id,
            companyName = validatedContext.company.nameEn,
            branchId = validatedContext.branch.id,
            branchName = validatedContext.branch.nameEn,
            tableId = validatedContext.table.id,
            tableNumber = validatedContext.table.tableNumber,
            zoneName = validatedContext.zoneName,
            guestName = guestName.ifBlank { "Guest at ${validatedContext.table.tableNumber}" },
            partySize = partySize.coerceIn(1, validatedContext.table.combinedCapacity.coerceAtLeast(12)),
            activeOrderId = existingOrderId,
            billRequested = validatedContext.table.status == TableStatus.WAITING_PAYMENT || validatedContext.table.status == TableStatus.BILLED
        )
    }

    // --- 2. Smart Table Combinations & Movement Engine ---

    data class TableCombinationResult(
        val updatedPrimaryTable: RestaurantTable,
        val updatedSecondaryTables: List<RestaurantTable>,
        val totalCombinedSeats: Int
    )

    /**
     * Combines a primary table with one or more secondary tables into a joined seating group.
     */
    fun combineTables(
        primaryTable: RestaurantTable,
        secondaryTables: List<RestaurantTable>
    ): TableCombinationResult? {
        val validSecondaries = secondaryTables.filter { it.id != primaryTable.id }
        if (validSecondaries.isEmpty()) return null

        val allIds = (listOf(primaryTable.id) + primaryTable.combinedWithTableIds + validSecondaries.map { it.id }).distinct()
        val secondaryOnlyIds = allIds.filter { it != primaryTable.id }
        val totalCapacity = primaryTable.capacity + validSecondaries.sumOf { it.capacity }

        val updatedPrimary = primaryTable.copy(
            combinedWithTableIds = secondaryOnlyIds,
            combinedCapacity = totalCapacity
        )

        val updatedSecondaries = validSecondaries.map { sec ->
            sec.copy(
                combinedWithTableIds = listOf(primaryTable.id),
                combinedCapacity = totalCapacity,
                status = primaryTable.status,
                currentOrderId = primaryTable.currentOrderId,
                assignedWaiterId = primaryTable.assignedWaiterId,
                assignedWaiterName = primaryTable.assignedWaiterName
            )
        }

        return TableCombinationResult(
            updatedPrimaryTable = updatedPrimary,
            updatedSecondaryTables = updatedSecondaries,
            totalCombinedSeats = totalCapacity
        )
    }

    /**
     * Splits combined tables back into independent tables with their original capacities.
     */
    fun uncombineTables(
        primaryTable: RestaurantTable,
        allBranchTables: List<RestaurantTable>
    ): List<RestaurantTable> {
        val linkedIds = primaryTable.combinedWithTableIds.toSet()
        return allBranchTables.map { tbl ->
            when {
                tbl.id == primaryTable.id -> tbl.copy(
                    combinedWithTableIds = emptyList(),
                    combinedCapacity = tbl.capacity
                )
                tbl.id in linkedIds || primaryTable.id in tbl.combinedWithTableIds -> tbl.copy(
                    combinedWithTableIds = emptyList(),
                    combinedCapacity = tbl.capacity,
                    status = if (tbl.currentOrderId == primaryTable.currentOrderId) TableStatus.AVAILABLE else tbl.status,
                    currentOrderId = if (tbl.currentOrderId == primaryTable.currentOrderId) null else tbl.currentOrderId
                )
                else -> tbl
            }
        }
    }

    data class TableMoveResult(
        val updatedSourceTable: RestaurantTable,
        val updatedTargetTable: RestaurantTable,
        val updatedOrder: Order?
    )

    /**
     * Moves seated guests and their active order from sourceTable to targetTable.
     * Source table automatically transitions to CLEANING so staff can sanitize it.
     */
    fun moveTable(
        sourceTable: RestaurantTable,
        targetTable: RestaurantTable,
        activeOrder: Order?
    ): TableMoveResult? {
        if (sourceTable.id == targetTable.id) return null
        if (targetTable.status !in listOf(TableStatus.AVAILABLE, TableStatus.RESERVED)) return null

        val movedOrder = activeOrder?.copy(
            tableId = targetTable.id,
            notes = listOf(activeOrder.notes, "Moved from ${sourceTable.tableNumber} to ${targetTable.tableNumber}")
                .filter { it.isNotBlank() }
                .joinToString(" | "),
            updatedAt = System.currentTimeMillis()
        )

        val newSource = sourceTable.copy(
            status = TableStatus.CLEANING,
            currentOrderId = null,
            guestCount = 0
        )

        val newTarget = targetTable.copy(
            status = TableStatus.OCCUPIED,
            currentOrderId = movedOrder?.id ?: sourceTable.currentOrderId,
            guestCount = sourceTable.guestCount.coerceAtLeast(2),
            assignedWaiterId = sourceTable.assignedWaiterId ?: targetTable.assignedWaiterId,
            assignedWaiterName = sourceTable.assignedWaiterName ?: targetTable.assignedWaiterName
        )

        return TableMoveResult(
            updatedSourceTable = newSource,
            updatedTargetTable = newTarget,
            updatedOrder = movedOrder
        )
    }

    // --- 3. Unified Order Creation for Waiter, QR & Table Flows (via OrderEngine) ---

    /**
     * Builds or appends to a table's Order using the central OrderEngine.
     * Used identically by POS Dine-In, Waiter Handheld App, and Customer QR Table Ordering.
     */
    fun buildTableOrderFromCart(
        existingOrder: Order?,
        cartItems: List<OrderEngine.PosCartItem>,
        branch: Branch,
        table: RestaurantTable,
        channel: SalesChannel, // SalesChannel.WAITER_APP or SalesChannel.QR_TABLE or SalesChannel.POS
        serverOrGuestId: String,
        serverOrGuestName: String,
        customerName: String? = null,
        customerPhone: String? = null,
        orderNotes: String = "",
        taxProfile: TaxProfile = TaxProfile("tax_standard_10", "VAT 10%", "ضريبة 10%", branch.taxRate, branch.taxInclusive),
        applyServiceFee: Boolean = true,
        idempotencyKey: String = "idem_tbl_${table.id}_${UUID.randomUUID()}"
    ): Pair<Order, List<OrderItem>> {
        val now = System.currentTimeMillis()
        val orderId = existingOrder?.id ?: "ord_${UUID.randomUUID().toString().take(8)}"
        val orderNumber = existingOrder?.orderNumber ?: "ORD-${(1050..9999).random()}"

        val newOrderItems = cartItems.map { it.toOrderItem(orderId) }
        val combinedItems = if (existingOrder != null && existingOrder.items.isNotEmpty()) {
            val merged = mutableListOf<OrderItem>()
            (existingOrder.items + newOrderItems).forEach { item ->
                val idx = merged.indexOfFirst {
                    it.productId == item.productId &&
                        it.variantId == item.variantId &&
                        it.modifiersSummary == item.modifiersSummary &&
                        it.addonsSummary == item.addonsSummary &&
                        it.notes == item.notes
                }
                if (idx >= 0) {
                    val prev = merged[idx]
                    val newQty = prev.quantity + item.quantity
                    merged[idx] = prev.copy(
                        quantity = newQty,
                        totalPrice = (prev.unitPrice * newQty) - prev.discountAmount
                    )
                } else {
                    merged.add(item.copy(orderId = orderId))
                }
            }
            merged
        } else {
            newOrderItems
        }

        // Convert combined items back to PosCartItem for exact OrderEngine financial calculation
        val syntheticCart = combinedItems.map { item ->
            OrderEngine.PosCartItem(
                uid = item.id,
                product = Product(
                    id = item.productId,
                    categoryId = "cat_burgers",
                    sku = "SKU",
                    nameEn = item.productNameEn,
                    nameAr = item.productNameAr,
                    price = item.unitPrice
                ),
                quantity = item.quantity,
                baseResolvedPrice = item.unitPrice,
                itemDiscountAmount = item.discountAmount,
                notes = item.notes
            )
        }

        val financials = OrderEngine.calculateOrderFinancials(
            cartItems = syntheticCart,
            orderType = if (channel == SalesChannel.QR_TABLE) OrderType.QR_ONLINE else OrderType.DINE_IN,
            taxProfile = taxProfile,
            applyDineInServiceFee = applyServiceFee
        )

        val finalNotes = listOfNotNull(
            existingOrder?.notes?.takeIf { it.isNotBlank() },
            orderNotes.takeIf { it.isNotBlank() }
        ).distinct().joinToString(" | ")

        val order = Order(
            id = orderId,
            branchId = branch.id,
            terminalId = when (channel) {
                SalesChannel.QR_TABLE -> "QR-SELF-ORDER"
                SalesChannel.WAITER_APP -> "WAITER-PAD-01"
                else -> "POS-01"
            },
            orderNumber = orderNumber,
            orderType = if (channel == SalesChannel.QR_TABLE) OrderType.QR_ONLINE else OrderType.DINE_IN,
            channel = channel,
            tableId = table.id,
            cashierId = serverOrGuestId,
            cashierName = serverOrGuestName,
            customerName = customerName ?: existingOrder?.customerName,
            customerPhone = customerPhone ?: existingOrder?.customerPhone,
            status = OrderStatus.PREPARING,
            subtotal = financials.grossItemsSubtotal,
            discountAmount = financials.totalDiscount,
            serviceFee = financials.serviceFee,
            taxAmount = financials.taxAmount,
            totalAmount = financials.totalPayable,
            paymentStatus = existingOrder?.paymentStatus ?: "UNPAID",
            notes = finalNotes,
            idempotencyKey = idempotencyKey,
            createdAt = existingOrder?.createdAt ?: now,
            updatedAt = now,
            items = combinedItems,
            payments = existingOrder?.payments ?: emptyList()
        )

        return order to combinedItems
    }

    // --- 4. Reservations Lifecycle Engine ---

    fun validateReservation(
        customerName: String,
        customerPhone: String,
        partySize: Int,
        table: RestaurantTable
    ): String? {
        if (customerName.isBlank()) return "Guest name is required for reservation."
        if (customerPhone.isBlank()) return "Guest phone number is required."
        if (partySize <= 0) return "Party size must be at least 1 guest."
        if (partySize > table.combinedCapacity) {
            return "Party size ($partySize) exceeds Table ${table.tableNumber} capacity (${table.combinedCapacity} seats). Combine tables or select a larger table."
        }
        return null
    }

    fun transitionReservationStatus(
        reservation: Reservation,
        targetStatus: ReservationStatus,
        table: RestaurantTable?
    ): Pair<Reservation, RestaurantTable?> {
        val now = System.currentTimeMillis()
        val updatedRes = reservation.copy(
            status = targetStatus,
            updatedAt = now
        )
        val updatedTable = table?.let { tbl ->
            when (targetStatus) {
                ReservationStatus.CONFIRMED -> tbl.copy(
                    status = if (tbl.status == TableStatus.AVAILABLE) TableStatus.RESERVED else tbl.status,
                    activeReservationId = reservation.id
                )
                ReservationStatus.SEATED -> tbl.copy(
                    status = TableStatus.OCCUPIED,
                    activeReservationId = reservation.id,
                    guestCount = reservation.partySize
                )
                ReservationStatus.CANCELLED, ReservationStatus.NO_SHOW, ReservationStatus.COMPLETED -> tbl.copy(
                    status = if (tbl.status == TableStatus.RESERVED) TableStatus.AVAILABLE else tbl.status,
                    activeReservationId = if (tbl.activeReservationId == reservation.id) null else tbl.activeReservationId
                )
                else -> tbl
            }
        }
        return updatedRes to updatedTable
    }
}
