package com.example.core.engine

import com.example.domain.model.*
import java.util.Locale
import java.util.UUID

/**
 * Phase 6 Central Kitchen Display System (KDS), Station Routing & Real-Time Workflow Engine.
 *
 * Implements:
 * Order -> Automatic Station Routing -> Station Ticket -> Preparation -> Ready -> Packing / Expediter -> Completion
 */
object KitchenEngine {

    data class RoutingOutcome(
        val stationTickets: List<KitchenTicket>,
        val routedStationsSummary: String
    )

    data class TicketTransitionOutcome(
        val updatedTicket: KitchenTicket,
        val spawnedConsolidationTicket: KitchenTicket? = null,
        val updatedOrderStatus: OrderStatus? = null,
        val updatedOrderItemStatuses: Map<String, String> = emptyMap(), // orderItemId -> kitchenStatus
        val workflowMessageEn: String,
        val workflowMessageAr: String
    )

    /**
     * Resolves which KitchenStation a specific OrderItem / Product should route to
     * based on active StationRoutingRules (Product match -> Category match -> Parent Category match -> SKU fallback).
     */
    fun resolveStationForItem(
        item: OrderItem,
        product: Product?,
        categories: List<Category>,
        stations: List<KitchenStation>,
        routingRules: List<StationRoutingRule>,
        orderType: OrderType
    ): KitchenStation {
        val activeStations = stations.filter { it.isActive }.ifEmpty { stations }
        val activeRules = routingRules.filter { it.isActive }

        // 1. Exact product + orderType rule
        val exactProductOrderRule = activeRules.firstOrNull {
            it.productId == item.productId && it.orderType == orderType
        }
        if (exactProductOrderRule != null) {
            activeStations.find { it.id == exactProductOrderRule.targetStationId || it.stationType == exactProductOrderRule.targetStationType }
                ?.let { return it }
        }

        // 2. Exact product rule
        val exactProductRule = activeRules.firstOrNull {
            it.productId == item.productId && it.orderType == null
        }
        if (exactProductRule != null) {
            activeStations.find { it.id == exactProductRule.targetStationId || it.stationType == exactProductRule.targetStationType }
                ?.let { return it }
        }

        // 3. Category or Parent Category rule
        val categoryId = product?.categoryId
        if (categoryId != null) {
            val categoryObj = categories.find { it.id == categoryId }
            val parentCategoryId = categoryObj?.parentId

            val catRule = activeRules.firstOrNull {
                it.productId == null && (it.categoryId == categoryId || (parentCategoryId != null && it.categoryId == parentCategoryId))
            }
            if (catRule != null) {
                activeStations.find { it.id == catRule.targetStationId || it.stationType == catRule.targetStationType }
                    ?.let { return it }
            }
        }

        // 4. Intelligent fallback by SKU / Category naming
        val sku = product?.sku?.uppercase(Locale.US) ?: ""
        val catId = categoryId?.lowercase(Locale.US) ?: ""
        val nameLower = item.productNameEn.lowercase(Locale.US)

        val inferredType = when {
            sku.startsWith("PIZ") || catId.contains("pizza") || nameLower.contains("pizza") || nameLower.contains("flatbread") ->
                KitchenStationType.PIZZA
            sku.startsWith("SID") || catId.contains("fries") || catId.contains("sides") || nameLower.contains("fries") || nameLower.contains("rings") || nameLower.contains("chicken") ->
                KitchenStationType.FRYER
            sku.startsWith("BEV") || catId.contains("bev") || nameLower.contains("lemonade") || nameLower.contains("brew") || nameLower.contains("espresso") || nameLower.contains("drink") ->
                KitchenStationType.DRINKS
            sku.startsWith("DES") || catId.contains("dessert") || nameLower.contains("pudding") || nameLower.contains("cake") || nameLower.contains("gelato") ->
                KitchenStationType.DESSERT
            else -> KitchenStationType.GRILL
        }

        return activeStations.find { it.stationType == inferredType }
            ?: activeStations.firstOrNull()
            ?: KitchenStation(
                id = "st_grill",
                kitchenId = "kit_main",
                prepAreaId = "prep_hot",
                prepAreaName = "Hot Line Area",
                branchId = "branch_01",
                code = "ST-GRILL",
                stationType = KitchenStationType.GRILL,
                nameEn = "Grill Station",
                nameAr = "محطة الشواء والجريل"
            )
    }

    /**
     * Routes an incoming or updated Order into one or more station-specific KitchenTickets.
     */
    fun routeOrderToStations(
        order: Order,
        items: List<OrderItem>,
        products: List<Product>,
        categories: List<Category>,
        stations: List<KitchenStation>,
        routingRules: List<StationRoutingRule>,
        kitchenEmployees: List<KitchenEmployeeAssignment>,
        tables: List<RestaurantTable>,
        existingTicketsForOrder: List<KitchenTicket> = emptyList()
    ): RoutingOutcome {
        val prepItems = items.filter { item ->
            val prod = products.find { it.id == item.productId }
            prod?.isPreparationItem != false
        }
        if (prepItems.isEmpty()) {
            return RoutingOutcome(emptyList(), "No preparation items")
        }

        val tableNumber = order.tableId?.let { tblId ->
            tables.find { it.id == tblId }?.tableNumber
        }

        val isVipOrRush = order.orderType in listOf(OrderType.PHONE_ORDER, OrderType.PICKUP) ||
            order.notes.contains("VIP", ignoreCase = true) ||
            order.notes.contains("RUSH", ignoreCase = true)

        val priority = when {
            isVipOrRush -> TicketPriority.VIP_RUSH
            order.orderType == OrderType.DELIVERY || prepItems.sumOf { it.quantity } >= 5 -> TicketPriority.HIGH
            else -> TicketPriority.NORMAL
        }

        // Group items by their resolved target KitchenStation
        val groupedByStation = prepItems.groupBy { item ->
            val product = products.find { it.id == item.productId }
            resolveStationForItem(
                item = item,
                product = product,
                categories = categories,
                stations = stations,
                routingRules = routingRules,
                orderType = order.orderType
            )
        }

        val now = System.currentTimeMillis()
        val generatedTickets = groupedByStation.map { (station, stationOrderItems) ->
            val existingTicket = existingTicketsForOrder.find {
                it.stationId == station.id && it.workflowStage == "STATION_PREP"
            }

            val ticketItems = stationOrderItems.map { oi ->
                KitchenTicketItem(
                    id = "kti_${oi.id}_${station.code.lowercase(Locale.US)}",
                    orderItemId = oi.id,
                    productId = oi.productId,
                    productNameEn = oi.productNameEn,
                    productNameAr = oi.productNameAr,
                    variantName = oi.variantName,
                    quantity = oi.quantity,
                    modifiersSummary = oi.modifiersSummary,
                    addonsSummary = oi.addonsSummary,
                    notes = oi.notes,
                    stationType = station.stationType,
                    status = existingTicket?.ticketStatus ?: KdsTicketStatus.NEW
                )
            }

            val summaryText = ticketItems.joinToString("\n") { ti ->
                val variantTag = if (!ti.variantName.isNullOrBlank()) " (${ti.variantName})" else ""
                val modsTag = if (ti.modifiersSummary.isNotBlank()) " [${ti.modifiersSummary}]" else ""
                val addonsTag = if (ti.addonsSummary.isNotBlank()) " +${ti.addonsSummary}" else ""
                val noteTag = if (ti.notes.isNotBlank()) " • Note: ${ti.notes}" else ""
                "${ti.quantity}x ${ti.productNameEn}$variantTag$modsTag$addonsTag$noteTag"
            }

            val assignedChef = kitchenEmployees.firstOrNull {
                it.stationId == station.id && it.isOnDuty
            }?.employeeName ?: kitchenEmployees.firstOrNull {
                it.stationType == station.stationType && it.isOnDuty
            }?.employeeName

            val ticketStatus = existingTicket?.ticketStatus ?: KdsTicketStatus.NEW

            KitchenTicket(
                id = existingTicket?.id ?: "kt_${order.id}_${station.stationType.code.lowercase(Locale.US)}",
                branchId = order.branchId,
                orderId = order.id,
                orderNumber = order.orderNumber,
                stationName = station.nameEn,
                itemsSummary = summaryText,
                status = ticketStatus.code,
                elapsedMinutes = existingTicket?.elapsedMinutes ?: 0,
                isPriority = priority != TicketPriority.NORMAL,
                kitchenId = station.kitchenId,
                prepAreaId = station.prepAreaId,
                stationId = station.id,
                stationType = station.stationType,
                orderType = order.orderType,
                channel = order.channel,
                tableId = order.tableId,
                tableNumber = tableNumber,
                serverOrCashierName = order.cashierName,
                customerName = order.customerName,
                priority = priority,
                ticketStatus = ticketStatus,
                items = ticketItems,
                orderNotes = order.notes,
                warningThresholdMinutes = station.warningThresholdMinutes,
                overdueThresholdMinutes = station.overdueThresholdMinutes,
                workflowStage = "STATION_PREP",
                createdAt = existingTicket?.createdAt ?: now,
                acceptedAt = existingTicket?.acceptedAt,
                preparingAt = existingTicket?.preparingAt,
                readyAt = existingTicket?.readyAt,
                completedAt = existingTicket?.completedAt,
                assignedChefName = assignedChef
            )
        }

        val summary = generatedTickets.joinToString(", ") { "${it.stationName} (${it.items.sumOf { i -> i.quantity }})" }
        return RoutingOutcome(generatedTickets, summary)
    }

    /**
     * Validates whether a KDS ticket status transition is allowed.
     */
    fun canTransitionTicket(current: KdsTicketStatus, target: KdsTicketStatus): Boolean {
        if (current == target) return true
        return when (current) {
            KdsTicketStatus.NEW -> target in setOf(
                KdsTicketStatus.ACCEPTED,
                KdsTicketStatus.PREPARING,
                KdsTicketStatus.READY,
                KdsTicketStatus.CANCELLED
            )
            KdsTicketStatus.ACCEPTED -> target in setOf(
                KdsTicketStatus.PREPARING,
                KdsTicketStatus.READY,
                KdsTicketStatus.CANCELLED
            )
            KdsTicketStatus.PREPARING -> target in setOf(
                KdsTicketStatus.READY,
                KdsTicketStatus.COMPLETED,
                KdsTicketStatus.CANCELLED
            )
            KdsTicketStatus.READY -> target in setOf(
                KdsTicketStatus.COMPLETED,
                KdsTicketStatus.PREPARING, // recall back to line
                KdsTicketStatus.CANCELLED
            )
            KdsTicketStatus.COMPLETED -> target in setOf(
                KdsTicketStatus.READY,
                KdsTicketStatus.PREPARING // recall bumped ticket
            )
            KdsTicketStatus.CANCELLED -> target in setOf(
                KdsTicketStatus.NEW,
                KdsTicketStatus.PREPARING
            )
        }
    }

    /**
     * Advances or updates a KitchenTicket status, evaluates whether all station prep tickets
     * for the order are ready to spawn a Packing/Expediter consolidation ticket,
     * and computes synchronized Order & OrderItem status updates.
     */
    fun transitionTicketStatus(
        ticket: KitchenTicket,
        targetStatus: KdsTicketStatus,
        allOrderTickets: List<KitchenTicket>,
        stations: List<KitchenStation>,
        kitchenEmployees: List<KitchenEmployeeAssignment>
    ): TicketTransitionOutcome? {
        if (!canTransitionTicket(ticket.ticketStatus, targetStatus)) {
            return null
        }

        val now = System.currentTimeMillis()
        val updatedItems = ticket.items.map { it.copy(status = targetStatus) }
        val updatedTicket = ticket.copy(
            status = targetStatus.code,
            ticketStatus = targetStatus,
            items = updatedItems,
            acceptedAt = if (targetStatus == KdsTicketStatus.ACCEPTED) (ticket.acceptedAt ?: now) else ticket.acceptedAt,
            preparingAt = if (targetStatus == KdsTicketStatus.PREPARING) (ticket.preparingAt ?: now) else ticket.preparingAt,
            readyAt = if (targetStatus == KdsTicketStatus.READY) (ticket.readyAt ?: now) else ticket.readyAt,
            completedAt = if (targetStatus == KdsTicketStatus.COMPLETED) now else if (targetStatus == KdsTicketStatus.PREPARING) null else ticket.completedAt,
            workflowStage = if (targetStatus == KdsTicketStatus.COMPLETED) "COMPLETED" else ticket.workflowStage
        )

        // Map ticket status to OrderItem.kitchenStatus
        val itemKitchenStatus = when (targetStatus) {
            KdsTicketStatus.NEW -> "PENDING"
            KdsTicketStatus.ACCEPTED, KdsTicketStatus.PREPARING -> "COOKING"
            KdsTicketStatus.READY -> "READY"
            KdsTicketStatus.COMPLETED -> "SERVED"
            KdsTicketStatus.CANCELLED -> "CANCELLED"
        }
        val itemStatusUpdates = updatedTicket.items.associate { it.orderItemId to itemKitchenStatus }

        // Evaluate all tickets for the same order with this ticket updated
        val effectiveOrderTickets = allOrderTickets.map {
            if (it.id == updatedTicket.id) updatedTicket else it
        }

        val prepTickets = effectiveOrderTickets.filter { it.workflowStage == "STATION_PREP" || (it.stationType != KitchenStationType.PACKING && it.stationType != KitchenStationType.EXPEDITER) }
        val existingConsolidationTicket = effectiveOrderTickets.find {
            it.stationType == KitchenStationType.PACKING || it.stationType == KitchenStationType.EXPEDITER
        }

        var spawnedConsolidation: KitchenTicket? = null
        val isPrepStationTicket = updatedTicket.stationType != KitchenStationType.PACKING &&
            updatedTicket.stationType != KitchenStationType.EXPEDITER

        // When all prep station tickets for this order reach READY or COMPLETED, spawn Packing/Expediter ticket!
        if (isPrepStationTicket &&
            targetStatus in setOf(KdsTicketStatus.READY, KdsTicketStatus.COMPLETED) &&
            prepTickets.isNotEmpty() &&
            prepTickets.all { it.ticketStatus in setOf(KdsTicketStatus.READY, KdsTicketStatus.COMPLETED) } &&
            existingConsolidationTicket == null
        ) {
            val requiresPacking = updatedTicket.orderType in setOf(
                OrderType.TAKEOUT,
                OrderType.PICKUP,
                OrderType.DELIVERY,
                OrderType.PHONE_ORDER
            )
            val consolidationType = if (requiresPacking) KitchenStationType.PACKING else KitchenStationType.EXPEDITER
            val consolidationStation = stations.find { it.stationType == consolidationType }
                ?: KitchenStation(
                    id = if (requiresPacking) "st_packing" else "st_expediter",
                    kitchenId = updatedTicket.kitchenId,
                    prepAreaId = "prep_pass",
                    prepAreaName = "Assembly & Pass Area",
                    branchId = updatedTicket.branchId,
                    code = if (requiresPacking) "ST-PACK" else "ST-EXP",
                    stationType = consolidationType,
                    nameEn = consolidationType.titleEn,
                    nameAr = consolidationType.titleAr,
                    warningThresholdMinutes = 5,
                    overdueThresholdMinutes = 10
                )

            val combinedItems = prepTickets.flatMap { it.items }.map {
                it.copy(
                    id = "kti_cons_${it.orderItemId}",
                    stationType = consolidationType,
                    status = KdsTicketStatus.NEW
                )
            }
            val combinedSummary = prepTickets.joinToString("\n") { it.itemsSummary }
            val assignedPassChef = kitchenEmployees.firstOrNull {
                it.stationType == consolidationType && it.isOnDuty
            }?.employeeName

            spawnedConsolidation = KitchenTicket(
                id = "kt_${updatedTicket.orderId}_${consolidationType.code.lowercase(Locale.US)}",
                branchId = updatedTicket.branchId,
                orderId = updatedTicket.orderId,
                orderNumber = updatedTicket.orderNumber,
                stationName = consolidationStation.nameEn,
                itemsSummary = combinedSummary,
                status = KdsTicketStatus.NEW.code,
                elapsedMinutes = 0,
                isPriority = updatedTicket.isPriority,
                kitchenId = consolidationStation.kitchenId,
                prepAreaId = consolidationStation.prepAreaId,
                stationId = consolidationStation.id,
                stationType = consolidationType,
                orderType = updatedTicket.orderType,
                channel = updatedTicket.channel,
                tableId = updatedTicket.tableId,
                tableNumber = updatedTicket.tableNumber,
                serverOrCashierName = updatedTicket.serverOrCashierName,
                customerName = updatedTicket.customerName,
                priority = updatedTicket.priority,
                ticketStatus = KdsTicketStatus.NEW,
                items = combinedItems,
                orderNotes = updatedTicket.orderNotes,
                warningThresholdMinutes = consolidationStation.warningThresholdMinutes,
                overdueThresholdMinutes = consolidationStation.overdueThresholdMinutes,
                workflowStage = consolidationType.code,
                createdAt = now,
                assignedChefName = assignedPassChef
            )
        }

        // Determine synchronized central OrderStatus
        val synchronizedOrderStatus: OrderStatus? = when {
            targetStatus == KdsTicketStatus.CANCELLED && effectiveOrderTickets.all { it.ticketStatus == KdsTicketStatus.CANCELLED } ->
                OrderStatus.CANCELLED
            !isPrepStationTicket && targetStatus == KdsTicketStatus.COMPLETED -> {
                if (updatedTicket.orderType in setOf(OrderType.DINE_IN, OrderType.QR_ONLINE)) {
                    OrderStatus.SERVED
                } else {
                    OrderStatus.READY
                }
            }
            !isPrepStationTicket && targetStatus == KdsTicketStatus.READY ->
                OrderStatus.READY
            isPrepStationTicket && targetStatus == KdsTicketStatus.COMPLETED && existingConsolidationTicket == null && spawnedConsolidation == null ->
                OrderStatus.READY
            isPrepStationTicket && targetStatus == KdsTicketStatus.READY && prepTickets.all { it.ticketStatus in setOf(KdsTicketStatus.READY, KdsTicketStatus.COMPLETED) } ->
                OrderStatus.READY
            targetStatus in setOf(KdsTicketStatus.ACCEPTED, KdsTicketStatus.PREPARING) ->
                OrderStatus.PREPARING
            else -> null
        }

        val msgEn = buildString {
            append("${updatedTicket.orderNumber} [${updatedTicket.stationName}] -> ${targetStatus.titleEn}")
            if (spawnedConsolidation != null) {
                append(" • Dispatched to ${spawnedConsolidation.stationName}")
            }
        }
        val msgAr = buildString {
            append("${updatedTicket.orderNumber} [${updatedTicket.stationType.titleAr}] -> ${targetStatus.titleAr}")
            if (spawnedConsolidation != null) {
                append(" • تم التوجيه إلى ${spawnedConsolidation.stationType.titleAr}")
            }
        }

        return TicketTransitionOutcome(
            updatedTicket = updatedTicket,
            spawnedConsolidationTicket = spawnedConsolidation,
            updatedOrderStatus = synchronizedOrderStatus,
            updatedOrderItemStatuses = itemStatusUpdates,
            workflowMessageEn = msgEn,
            workflowMessageAr = msgAr
        )
    }

    /**
     * Formats seconds into mm:ss for KDS live timers.
     */
    fun formatElapsedTimer(totalSeconds: Long): String {
        val mins = totalSeconds / 60L
        val secs = totalSeconds % 60L
        return String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}
