package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.engine.ChannelMenuItem
import com.example.core.engine.MenuEngine
import com.example.core.engine.OrderEngine
import com.example.core.engine.TableOperationsEngine
import com.example.core.i18n.AppLanguage
import com.example.designsystem.*
import com.example.domain.model.*
import com.example.ui.theme.*
import java.util.Locale
import java.util.UUID

/**
 * Tab 2: Touch-Friendly Waiter Handheld Interface
 * Supports: Waiter Login/Switcher, Floor Map / Table List toggle, Open Table,
 * Take Order, Modify Order, Send to Kitchen, View Order Status, Request Bill,
 * Move Table, and Merge Tables.
 */
@Composable
fun WaiterHandheldTab(
    branch: Branch,
    tables: List<RestaurantTable>,
    orders: List<Order>,
    products: List<Product>,
    categories: List<Category>,
    modifierGroups: List<ModifierGroup>,
    addons: List<ProductAddon>,
    priceRules: List<ChannelPriceRule>,
    taxProfiles: List<TaxProfile>,
    employees: List<Employee>,
    currentUser: User,
    language: AppLanguage,
    onOpenTable: (tableId: String, guestCount: Int, waiterId: String, waiterName: String) -> Unit,
    onPlaceOrUpdateOrder: (Order, List<OrderItem>) -> Unit,
    onRequestBill: (tableId: String, requestedBy: String) -> Unit,
    onMoveTable: (sourceTableId: String, targetTableId: String) -> Unit,
    onMergeOrders: (primaryOrderId: String, secondaryOrderId: String) -> Unit,
    onCombineTables: (primaryTableId: String, secondaryTableIds: List<String>) -> Unit,
    onUpdateTableStatus: (tableId: String, status: TableStatus) -> Unit,
    onShowFeedback: (String) -> Unit
) {
    // Waiter authentication / active server session state
    var activeWaiterName by remember { mutableStateOf("Sarah Nader") }
    var activeWaiterId by remember { mutableStateOf("user_04") }
    var isWaiterLoggedIn by remember { mutableStateOf(true) }
    var pinInput by remember { mutableStateOf("") }

    // View mode: FLOOR_MAP or TABLE_LIST
    var waiterViewMode by remember { mutableStateOf("FLOOR_MAP") }
    var selectedTableId by remember { mutableStateOf(tables.firstOrNull()?.id ?: "t2") }
    val selectedTable = tables.find { it.id == selectedTableId } ?: tables.firstOrNull()

    // Active order for the selected table
    val tableOrder = remember(selectedTable, orders) {
        selectedTable?.let { tbl ->
            orders.find { it.id == tbl.currentOrderId }
                ?: orders.firstOrNull {
                    it.tableId == tbl.id && it.status !in listOf(
                        OrderStatus.COMPLETED,
                        OrderStatus.CANCELLED,
                        OrderStatus.REFUNDED
                    )
                }
        }
    }

    // Waiter order pad cart
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var waiterCart by remember { mutableStateOf<List<OrderEngine.PosCartItem>>(emptyList()) }
    var orderNotes by remember { mutableStateOf("") }
    var guestCountInput by remember(selectedTable) {
        mutableIntStateOf((selectedTable?.guestCount ?: 2).coerceAtLeast(1))
    }

    // Dialog states for Waiter Move / Merge / Item Modifiers
    var showMoveModal by remember { mutableStateOf(false) }
    var showMergeModal by remember { mutableStateOf(false) }
    var customizingProduct by remember { mutableStateOf<ChannelMenuItem?>(null) }

    // Resolve menu for WAITER_APP channel
    val resolvedMenu = remember(products, categories, priceRules, branch) {
        MenuEngine.getUnifiedMenuForChannel(
            categories = categories,
            products = products,
            channel = SalesChannel.WAITER_APP,
            branchId = branch.id,
            priceRules = priceRules
        ).flatMap { it.items }
    }

    val filteredProducts = remember(resolvedMenu, selectedCategoryId) {
        resolvedMenu.filter { item ->
            selectedCategoryId == null || item.product.categoryId == selectedCategoryId
        }
    }

    if (!isWaiterLoggedIn) {
        // Waiter PIN Login Card
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            RestoCard(modifier = Modifier.widthIn(max = 420.dp)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = "Waiter Terminal Login",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Waiter Handheld Terminal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select floor server profile or enter quick PIN to unlock table operations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val servers = listOf(
                        "user_04" to "Sarah Nader (Dining Captain)",
                        "user_03" to "Youssef Zaid (Senior Server)",
                        "user_02" to "Karim Mansour (Floor Manager)"
                    )
                    servers.forEach { (id, label) ->
                        OutlinedButton(
                            onClick = {
                                activeWaiterId = id
                                activeWaiterName = label.substringBefore(" (")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(
                                width = if (activeWaiterId == id) 2.dp else 1.dp,
                                color = if (activeWaiterId == id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Text(label)
                        }
                    }

                    RestoTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        label = "Server PIN (Default: 1234)",
                        placeholder = "1234"
                    )

                    RestoButton(
                        onClick = {
                            isWaiterLoggedIn = true
                            pinInput = ""
                            onShowFeedback("Waiter terminal unlocked for $activeWaiterName")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("waiter_login_submit_btn")
                    ) {
                        Text("Unlock Waiter Handheld")
                    }
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("waiter_handheld_tab"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Waiter Session Bar
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = RestoEmerald600.copy(alpha = 0.15f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonPin,
                            contentDescription = "Active Waiter",
                            tint = RestoEmerald600,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Server: $activeWaiterName",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${branch.nameEn} • ${tables.count { it.status == TableStatus.OCCUPIED }} Occupied Tables",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RestoFilterChip(
                        selected = waiterViewMode == "FLOOR_MAP",
                        onClick = { waiterViewMode = "FLOOR_MAP" },
                        label = "Floor Map"
                    )
                    RestoFilterChip(
                        selected = waiterViewMode == "TABLE_LIST",
                        onClick = { waiterViewMode = "TABLE_LIST" },
                        label = "Table List"
                    )
                    RestoButton(
                        onClick = { isWaiterLoggedIn = false },
                        variant = RestoButtonVariant.OUTLINE,
                        modifier = Modifier.testTag("waiter_switch_server_btn")
                    ) {
                        Text("Switch Server")
                    }
                }
            }
        }

        // Split View: Left = Floor Map / Table List + Active Table Status | Right = Waiter Order Pad
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 780.dp

            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WaiterTableSelectionSection(
                            tables = tables,
                            orders = orders,
                            selectedTable = selectedTable,
                            viewMode = waiterViewMode,
                            guestCountInput = guestCountInput,
                            onGuestCountChange = { guestCountInput = it },
                            onSelectTable = { selectedTableId = it.id },
                            onOpenTable = { tbl ->
                                onOpenTable(tbl.id, guestCountInput, activeWaiterId, activeWaiterName)
                            },
                            onRequestBill = { tbl ->
                                onRequestBill(tbl.id, "Waiter $activeWaiterName")
                            },
                            onShowMoveModal = { showMoveModal = true },
                            onShowMergeModal = { showMergeModal = true },
                            onCleanComplete = { tbl ->
                                onUpdateTableStatus(tbl.id, TableStatus.AVAILABLE)
                            }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight()
                    ) {
                        WaiterOrderPadSection(
                            selectedTable = selectedTable,
                            existingOrder = tableOrder,
                            categories = categories,
                            selectedCategoryId = selectedCategoryId,
                            onSelectCategory = { selectedCategoryId = it },
                            resolvedProducts = filteredProducts,
                            waiterCart = waiterCart,
                            orderNotes = orderNotes,
                            onOrderNotesChange = { orderNotes = it },
                            onTapProduct = { resolved ->
                                if (resolved.product.variants.isNotEmpty() || resolved.product.modifierGroupIds.isNotEmpty()) {
                                    customizingProduct = resolved
                                } else {
                                    waiterCart = addOrIncrementCartItem(waiterCart, resolved, null, emptyList(), emptyList(), "")
                                }
                            },
                            onUpdateCartQty = { uid, delta ->
                                waiterCart = waiterCart.mapNotNull { item ->
                                    if (item.uid == uid) {
                                        val next = item.quantity + delta
                                        if (next <= 0) null else item.copy(quantity = next)
                                    } else item
                                }
                            },
                            onClearCart = { waiterCart = emptyList() },
                            onSendToKitchen = {
                                if (selectedTable == null) return@WaiterOrderPadSection
                                if (waiterCart.isEmpty()) {
                                    onShowFeedback("Add at least one item before sending to kitchen")
                                    return@WaiterOrderPadSection
                                }
                                val (order, items) = TableOperationsEngine.buildTableOrderFromCart(
                                    existingOrder = tableOrder,
                                    cartItems = waiterCart,
                                    branch = branch,
                                    table = selectedTable,
                                    channel = SalesChannel.WAITER_APP,
                                    serverOrGuestId = activeWaiterId,
                                    serverOrGuestName = activeWaiterName,
                                    orderNotes = orderNotes
                                )
                                onPlaceOrUpdateOrder(order, items)
                                waiterCart = emptyList()
                                orderNotes = ""
                            }
                        )
                    }
                }
            } else {
                // Compact Handheld Layout (Vertical Scroll)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WaiterTableSelectionSection(
                        tables = tables,
                        orders = orders,
                        selectedTable = selectedTable,
                        viewMode = waiterViewMode,
                        guestCountInput = guestCountInput,
                        onGuestCountChange = { guestCountInput = it },
                        onSelectTable = { selectedTableId = it.id },
                        onOpenTable = { tbl ->
                            onOpenTable(tbl.id, guestCountInput, activeWaiterId, activeWaiterName)
                        },
                        onRequestBill = { tbl ->
                            onRequestBill(tbl.id, "Waiter $activeWaiterName")
                        },
                        onShowMoveModal = { showMoveModal = true },
                        onShowMergeModal = { showMergeModal = true },
                        onCleanComplete = { tbl ->
                            onUpdateTableStatus(tbl.id, TableStatus.AVAILABLE)
                        }
                    )

                    WaiterOrderPadSection(
                        selectedTable = selectedTable,
                        existingOrder = tableOrder,
                        categories = categories,
                        selectedCategoryId = selectedCategoryId,
                        onSelectCategory = { selectedCategoryId = it },
                        resolvedProducts = filteredProducts,
                        waiterCart = waiterCart,
                        orderNotes = orderNotes,
                        onOrderNotesChange = { orderNotes = it },
                        onTapProduct = { resolved ->
                            if (resolved.product.variants.isNotEmpty() || resolved.product.modifierGroupIds.isNotEmpty()) {
                                customizingProduct = resolved
                            } else {
                                waiterCart = addOrIncrementCartItem(waiterCart, resolved, null, emptyList(), emptyList(), "")
                            }
                        },
                        onUpdateCartQty = { uid, delta ->
                            waiterCart = waiterCart.mapNotNull { item ->
                                if (item.uid == uid) {
                                    val next = item.quantity + delta
                                    if (next <= 0) null else item.copy(quantity = next)
                                } else item
                            }
                        },
                        onClearCart = { waiterCart = emptyList() },
                        onSendToKitchen = {
                            if (selectedTable == null) return@WaiterOrderPadSection
                            if (waiterCart.isEmpty()) {
                                onShowFeedback("Add at least one item before sending to kitchen")
                                return@WaiterOrderPadSection
                            }
                            val (order, items) = TableOperationsEngine.buildTableOrderFromCart(
                                existingOrder = tableOrder,
                                cartItems = waiterCart,
                                branch = branch,
                                table = selectedTable,
                                channel = SalesChannel.WAITER_APP,
                                serverOrGuestId = activeWaiterId,
                                serverOrGuestName = activeWaiterName,
                                orderNotes = orderNotes
                            )
                            onPlaceOrUpdateOrder(order, items)
                            waiterCart = emptyList()
                            orderNotes = ""
                        },
                        modifier = Modifier.heightIn(min = 520.dp)
                    )
                }
            }
        }
    }

    // Move Table Dialog
    if (showMoveModal && selectedTable != null) {
        val availableTargets = tables.filter {
            it.id != selectedTable.id && it.status in listOf(TableStatus.AVAILABLE, TableStatus.RESERVED)
        }
        RestoModalDialog(
            onDismissRequest = { showMoveModal = false },
            title = "Move Guests from Table ${selectedTable.tableNumber}"
        ) {
            Text(
                text = "Select an available destination table. Active order and server assignment will transfer automatically, and ${selectedTable.tableNumber} will be marked for Cleaning.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (availableTargets.isEmpty()) {
                Text("No available tables to move to right now.", color = RestoRose600)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableTargets.forEach { target ->
                        OutlinedButton(
                            onClick = {
                                onMoveTable(selectedTable.id, target.id)
                                selectedTableId = target.id
                                showMoveModal = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("move_target_${target.tableNumber}")
                        ) {
                            Text("Move to ${target.tableNumber} (${target.section} • ${target.combinedCapacity} Seats)")
                        }
                    }
                }
            }
        }
    }

    // Merge Table Orders Dialog
    if (showMergeModal && selectedTable != null) {
        val otherOccupiedTables = tables.filter {
            it.id != selectedTable.id && it.currentOrderId != null
        }
        RestoModalDialog(
            onDismissRequest = { showMergeModal = false },
            title = "Merge Tables / Active Orders into ${selectedTable.tableNumber}"
        ) {
            Text(
                text = "Combine seating or merge another occupied table's active order into ${selectedTable.tableNumber}:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (tableOrder != null && otherOccupiedTables.isNotEmpty()) {
                    Text("Merge Active Order From:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    otherOccupiedTables.forEach { secTbl ->
                        val secOrderId = secTbl.currentOrderId ?: return@forEach
                        RestoButton(
                            onClick = {
                                onMergeOrders(tableOrder.id, secOrderId)
                                onCombineTables(selectedTable.id, listOf(secTbl.id))
                                showMergeModal = false
                            },
                            variant = RestoButtonVariant.PRIMARY,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Merge Order & Combine with ${secTbl.tableNumber}")
                        }
                    }
                }
                Text("Or Combine Physical Seating Group With:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                tables.filter { it.id != selectedTable.id }.forEach { candidate ->
                    OutlinedButton(
                        onClick = {
                            onCombineTables(selectedTable.id, listOf(candidate.id))
                            showMergeModal = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Join ${selectedTable.tableNumber} + ${candidate.tableNumber} (${selectedTable.capacity + candidate.capacity} Seats)")
                    }
                }
            }
        }
    }

    // Item Variant & Modifier Customization Modal
    customizingProduct?.let { resolved ->
        TableItemCustomizerModal(
            resolvedProduct = resolved,
            modifierGroups = modifierGroups,
            addons = addons,
            onDismiss = { customizingProduct = null },
            onConfirmItem = { variant, mods, itemAddons, notes ->
                waiterCart = addOrIncrementCartItem(waiterCart, resolved, variant, mods, itemAddons, notes)
                customizingProduct = null
            }
        )
    }
}

@Composable
private fun WaiterTableSelectionSection(
    tables: List<RestaurantTable>,
    orders: List<Order>,
    selectedTable: RestaurantTable?,
    viewMode: String,
    guestCountInput: Int,
    onGuestCountChange: (Int) -> Unit,
    onSelectTable: (RestaurantTable) -> Unit,
    onOpenTable: (RestaurantTable) -> Unit,
    onRequestBill: (RestaurantTable) -> Unit,
    onShowMoveModal: () -> Unit,
    onShowMergeModal: () -> Unit,
    onCleanComplete: (RestaurantTable) -> Unit
) {
    RestoCard {
        Text(
            text = if (viewMode == "FLOOR_MAP") "Interactive Waiter Floor Map" else "Branch Table List",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (viewMode == "FLOOR_MAP") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                tables.forEach { tbl ->
                    val (statusColor, _) = tableStatusVisuals(tbl.status)
                    val isSelected = selectedTable?.id == tbl.id
                    Surface(
                        modifier = Modifier
                            .width(135.dp)
                            .clickable { onSelectTable(tbl) }
                            .testTag("waiter_table_chip_${tbl.tableNumber}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) statusColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, statusColor)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(tbl.tableNumber, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${tbl.combinedCapacity}p",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = tbl.status.titleEn,
                                fontSize = 11.sp,
                                color = statusColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = tbl.section,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                tables.forEach { tbl ->
                    val (statusColor, badgeVariant) = tableStatusVisuals(tbl.status)
                    val isSelected = selectedTable?.id == tbl.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTable(tbl) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) statusColor.copy(alpha = 0.12f) else Color.Transparent,
                        border = BorderStroke(1.dp, if (isSelected) statusColor else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${tbl.tableNumber} • ${tbl.section}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Capacity: ${tbl.combinedCapacity} seats • Server: ${tbl.assignedWaiterName ?: "Unassigned"}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            RestoBadge(text = tbl.status.titleEn, variant = badgeVariant)
                        }
                    }
                }
            }
        }

        // Selected Table Action Console
        selectedTable?.let { tbl ->
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            val activeOrder = orders.find { it.id == tbl.currentOrderId }
            val (_, badgeVariant) = tableStatusVisuals(tbl.status)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Table ${tbl.tableNumber} (${tbl.section})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (tbl.combinedWithTableIds.isNotEmpty()) {
                            "Joined Group (+${tbl.combinedWithTableIds.size} tables) • ${tbl.combinedCapacity} Seats"
                        } else {
                            "${tbl.capacity} Seats • Floor: ${tbl.floorName}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                RestoBadge(text = tbl.status.titleEn, variant = badgeVariant)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Guest Count & Open Table Bar
            if (tbl.status in listOf(TableStatus.AVAILABLE, TableStatus.RESERVED)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Guests:", style = MaterialTheme.typography.labelMedium)
                        OutlinedButton(
                            onClick = { onGuestCountChange((guestCountInput - 1).coerceAtLeast(1)) },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(34.dp)
                        ) { Text("-") }
                        Text("$guestCountInput", fontWeight = FontWeight.Bold)
                        OutlinedButton(
                            onClick = { onGuestCountChange((guestCountInput + 1).coerceAtMost(20)) },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(34.dp)
                        ) { Text("+") }
                    }

                    RestoButton(
                        onClick = { onOpenTable(tbl) },
                        variant = RestoButtonVariant.PRIMARY,
                        modifier = Modifier.testTag("waiter_open_table_btn")
                    ) {
                        Text("Open Table & Seat")
                    }
                }
            }

            if (tbl.status == TableStatus.CLEANING) {
                RestoButton(
                    onClick = { onCleanComplete(tbl) },
                    variant = RestoButtonVariant.SECONDARY,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mark Table Sanitized & Available")
                }
            }

            // Waiter Action Buttons: Request Bill, Move Table, Merge Tables
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RestoButton(
                    onClick = { onRequestBill(tbl) },
                    variant = RestoButtonVariant.SECONDARY,
                    enabled = tbl.status == TableStatus.OCCUPIED || activeOrder != null,
                    modifier = Modifier.testTag("waiter_request_bill_btn")
                ) {
                    Text("Request Bill")
                }
                RestoButton(
                    onClick = onShowMoveModal,
                    variant = RestoButtonVariant.OUTLINE,
                    modifier = Modifier.testTag("waiter_move_table_btn")
                ) {
                    Text("Move Table")
                }
                RestoButton(
                    onClick = onShowMergeModal,
                    variant = RestoButtonVariant.OUTLINE,
                    modifier = Modifier.testTag("waiter_merge_tables_btn")
                ) {
                    Text("Merge Tables")
                }
            }

            // Active Order Status Summary
            if (activeOrder != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Active Ticket: ${activeOrder.orderNumber}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            RestoBadge(
                                text = "KDS: ${activeOrder.status.titleEn}",
                                variant = BadgeVariant.INFO
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        activeOrder.items.forEach { item ->
                            Text(
                                text = "• ${item.quantity}x ${item.productNameEn} (${item.kitchenStatus}) — $${String.format(Locale.US, "%.2f", item.totalPrice)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Running Total: $${String.format(Locale.US, "%.2f", activeOrder.totalAmount)} (${activeOrder.paymentStatus})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WaiterOrderPadSection(
    selectedTable: RestaurantTable?,
    existingOrder: Order?,
    categories: List<Category>,
    selectedCategoryId: String?,
    onSelectCategory: (String?) -> Unit,
    resolvedProducts: List<ChannelMenuItem>,
    waiterCart: List<OrderEngine.PosCartItem>,
    orderNotes: String,
    onOrderNotesChange: (String) -> Unit,
    onTapProduct: (ChannelMenuItem) -> Unit,
    onUpdateCartQty: (String, Int) -> Unit,
    onClearCart: () -> Unit,
    onSendToKitchen: () -> Unit,
    modifier: Modifier = Modifier
) {
    RestoCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (existingOrder != null) {
                        "Modify Order ${existingOrder.orderNumber} (${selectedTable?.tableNumber ?: ""})"
                    } else {
                        "Take New Order (${selectedTable?.tableNumber ?: "Select Table"})"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap items to add courses/modifiers and dispatch directly to Kitchen KDS",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (waiterCart.isNotEmpty()) {
                TextButton(onClick = onClearCart) {
                    Text("Clear Pad")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Category Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RestoFilterChip(
                selected = selectedCategoryId == null,
                onClick = { onSelectCategory(null) },
                label = "All Menu"
            )
            categories.filter { it.parentId == null }.forEach { cat ->
                RestoFilterChip(
                    selected = selectedCategoryId == cat.id,
                    onClick = { onSelectCategory(cat.id) },
                    label = cat.nameEn
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Menu Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 135.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .heightIn(min = 180.dp, max = 320.dp)
                .fillMaxWidth()
        ) {
            items(resolvedProducts) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTapProduct(item) }
                        .testTag("waiter_menu_item_${item.product.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = item.product.nameEn,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", item.resolvedPrice)}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(8.dp))

        // Current New Items Pad
        if (waiterCart.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 140.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                waiterCart.forEach { cartItem ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${cartItem.quantity}x ${cartItem.product.nameEn}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            val modSummary = listOf(cartItem.modifiersSummaryText, cartItem.addonsSummaryText)
                                .filter { it.isNotBlank() }
                                .joinToString(", ")
                            if (modSummary.isNotBlank() || cartItem.notes.isNotBlank()) {
                                Text(
                                    text = listOf(modSummary, cartItem.notes).filter { it.isNotBlank() }.joinToString(" • "),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onUpdateCartQty(cartItem.uid, -1) },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(28.dp)
                            ) { Text("-") }
                            Text("${cartItem.quantity}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            OutlinedButton(
                                onClick = { onUpdateCartQty(cartItem.uid, 1) },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(28.dp)
                            ) { Text("+") }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        RestoTextField(
            value = orderNotes,
            onValueChange = onOrderNotesChange,
            label = "Kitchen / Table Notes",
            placeholder = "Allergies, course pacing, VIP instructions..."
        )

        Spacer(modifier = Modifier.height(10.dp))

        val padSubtotal = waiterCart.sumOf { it.netTotal }
        RestoButton(
            onClick = onSendToKitchen,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("waiter_send_kitchen_btn")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (existingOrder != null) {
                    "Send Added Items to Kitchen (+$${String.format(Locale.US, "%.2f", padSubtotal)})"
                } else {
                    "Send Order to Kitchen ($${String.format(Locale.US, "%.2f", padSubtotal)})"
                }
            )
        }
    }
}

/**
 * Tab 3: Customer QR Table Ordering & Cryptographic Token Validator
 * Implements the exact flow:
 * QR -> validate -> identify restaurant -> identify branch -> identify table -> load menu -> create customer session -> create order
 */
@Composable
fun CustomerQrOrderingTab(
    company: Company,
    branch: Branch,
    branches: List<Branch>,
    tables: List<RestaurantTable>,
    orders: List<Order>,
    products: List<Product>,
    categories: List<Category>,
    modifierGroups: List<ModifierGroup>,
    addons: List<ProductAddon>,
    priceRules: List<ChannelPriceRule>,
    taxProfiles: List<TaxProfile>,
    activeSession: CustomerTableSession?,
    onStartQrSession: (scannedToken: String, guestName: String, partySize: Int) -> Unit,
    onClearQrSession: () -> Unit,
    onRotateTableQrToken: (tableId: String) -> Unit,
    onSubmitCustomerOrder: (Order, List<OrderItem>) -> Unit,
    onCustomerRequestBill: (tableId: String, guestName: String) -> Unit,
    onShowFeedback: (String) -> Unit
) {
    var selectedTableForQr by remember(tables) { mutableStateOf(tables.firstOrNull()) }
    var scannedTokenInput by remember(selectedTableForQr) {
        mutableStateOf(selectedTableForQr?.qrToken ?: "")
    }
    var guestNameInput by remember { mutableStateOf("Faisal Al-Rashid") }
    var partySizeInput by remember { mutableIntStateOf(2) }

    // Customer cart state
    var qrCart by remember { mutableStateOf<List<OrderEngine.PosCartItem>>(emptyList()) }
    var qrNotes by remember { mutableStateOf("") }
    var customizingQrProduct by remember { mutableStateOf<ChannelMenuItem?>(null) }

    // Resolve menu for QR_TABLE channel (applies QR-exclusive pricing rules!)
    val qrMenu = remember(products, categories, priceRules, branch) {
        MenuEngine.getUnifiedMenuForChannel(
            categories = categories,
            products = products,
            channel = SalesChannel.QR_TABLE,
            branchId = branch.id,
            priceRules = priceRules
        ).flatMap { it.items }
    }

    // Live order linked to the QR customer session
    val sessionTable = remember(activeSession, tables) {
        activeSession?.let { sess -> tables.find { it.id == sess.tableId } }
    }
    val sessionActiveOrder = remember(sessionTable, orders, activeSession) {
        orders.find { it.id == sessionTable?.currentOrderId }
            ?: orders.firstOrNull {
                it.tableId == activeSession?.tableId && it.status !in listOf(
                    OrderStatus.COMPLETED,
                    OrderStatus.CANCELLED
                )
            }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("qr_ordering_tab")
    ) {
        val isWide = maxWidth >= 800.dp

        if (isWide) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Pane: Secure QR Token Generator & 7-Step Flow Pipeline
                Column(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QrSecurityTokenManagerCard(
                        company = company,
                        branch = branch,
                        tables = tables,
                        selectedTable = selectedTableForQr,
                        scannedTokenInput = scannedTokenInput,
                        guestNameInput = guestNameInput,
                        partySizeInput = partySizeInput,
                        activeSession = activeSession,
                        onSelectTable = { tbl ->
                            selectedTableForQr = tbl
                            scannedTokenInput = tbl.qrToken
                        },
                        onTokenInputChange = { scannedTokenInput = it },
                        onGuestNameChange = { guestNameInput = it },
                        onPartySizeChange = { partySizeInput = it },
                        onRotateToken = { tbl ->
                            onRotateTableQrToken(tbl.id)
                        },
                        onValidateAndStartSession = {
                            onStartQrSession(scannedTokenInput, guestNameInput, partySizeInput)
                        },
                        onEndSession = onClearQrSession
                    )
                }

                // Right Pane: Customer Self-Ordering Experience (Browse, Modifiers, Cart, Submit, Track, Request Bill)
                Column(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CustomerMobileOrderingPreview(
                        activeSession = activeSession,
                        branch = branch,
                        sessionTable = sessionTable,
                        activeOrder = sessionActiveOrder,
                        qrMenu = qrMenu,
                        qrCart = qrCart,
                        qrNotes = qrNotes,
                        onNotesChange = { qrNotes = it },
                        onTapMenuProduct = { resolved ->
                            if (resolved.product.variants.isNotEmpty() || resolved.product.modifierGroupIds.isNotEmpty()) {
                                customizingQrProduct = resolved
                            } else {
                                qrCart = addOrIncrementCartItem(qrCart, resolved, null, emptyList(), emptyList(), "")
                            }
                        },
                        onUpdateCartQty = { uid, delta ->
                            qrCart = qrCart.mapNotNull { item ->
                                if (item.uid == uid) {
                                    val next = item.quantity + delta
                                    if (next <= 0) null else item.copy(quantity = next)
                                } else item
                            }
                        },
                        onSubmitQrOrder = {
                            val sess = activeSession ?: return@CustomerMobileOrderingPreview
                            val tbl = sessionTable ?: tables.find { it.id == sess.tableId } ?: return@CustomerMobileOrderingPreview
                            if (qrCart.isEmpty()) {
                                onShowFeedback("Your table cart is empty")
                                return@CustomerMobileOrderingPreview
                            }
                            val (order, items) = TableOperationsEngine.buildTableOrderFromCart(
                                existingOrder = sessionActiveOrder,
                                cartItems = qrCart,
                                branch = branch,
                                table = tbl,
                                channel = SalesChannel.QR_TABLE,
                                serverOrGuestId = sess.sessionId,
                                serverOrGuestName = "${sess.guestName} (QR)",
                                customerName = sess.guestName,
                                orderNotes = qrNotes
                            )
                            onSubmitCustomerOrder(order, items)
                            qrCart = emptyList()
                            qrNotes = ""
                        },
                        onRequestBill = {
                            val sess = activeSession ?: return@CustomerMobileOrderingPreview
                            onCustomerRequestBill(sess.tableId, "${sess.guestName} (QR Table ${sess.tableNumber})")
                        }
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QrSecurityTokenManagerCard(
                    company = company,
                    branch = branch,
                    tables = tables,
                    selectedTable = selectedTableForQr,
                    scannedTokenInput = scannedTokenInput,
                    guestNameInput = guestNameInput,
                    partySizeInput = partySizeInput,
                    activeSession = activeSession,
                    onSelectTable = { tbl ->
                        selectedTableForQr = tbl
                        scannedTokenInput = tbl.qrToken
                    },
                    onTokenInputChange = { scannedTokenInput = it },
                    onGuestNameChange = { guestNameInput = it },
                    onPartySizeChange = { partySizeInput = it },
                    onRotateToken = { tbl -> onRotateTableQrToken(tbl.id) },
                    onValidateAndStartSession = {
                        onStartQrSession(scannedTokenInput, guestNameInput, partySizeInput)
                    },
                    onEndSession = onClearQrSession
                )

                CustomerMobileOrderingPreview(
                    activeSession = activeSession,
                    branch = branch,
                    sessionTable = sessionTable,
                    activeOrder = sessionActiveOrder,
                    qrMenu = qrMenu,
                    qrCart = qrCart,
                    qrNotes = qrNotes,
                    onNotesChange = { qrNotes = it },
                    onTapMenuProduct = { resolved ->
                        if (resolved.product.variants.isNotEmpty() || resolved.product.modifierGroupIds.isNotEmpty()) {
                            customizingQrProduct = resolved
                        } else {
                            qrCart = addOrIncrementCartItem(qrCart, resolved, null, emptyList(), emptyList(), "")
                        }
                    },
                    onUpdateCartQty = { uid, delta ->
                        qrCart = qrCart.mapNotNull { item ->
                            if (item.uid == uid) {
                                val next = item.quantity + delta
                                if (next <= 0) null else item.copy(quantity = next)
                            } else item
                        }
                    },
                    onSubmitQrOrder = {
                        val sess = activeSession ?: return@CustomerMobileOrderingPreview
                        val tbl = sessionTable ?: tables.find { it.id == sess.tableId } ?: return@CustomerMobileOrderingPreview
                        if (qrCart.isEmpty()) {
                            onShowFeedback("Your table cart is empty")
                            return@CustomerMobileOrderingPreview
                        }
                        val (order, items) = TableOperationsEngine.buildTableOrderFromCart(
                            existingOrder = sessionActiveOrder,
                            cartItems = qrCart,
                            branch = branch,
                            table = tbl,
                            channel = SalesChannel.QR_TABLE,
                            serverOrGuestId = sess.sessionId,
                            serverOrGuestName = "${sess.guestName} (QR)",
                            customerName = sess.guestName,
                            orderNotes = qrNotes
                        )
                        onSubmitCustomerOrder(order, items)
                        qrCart = emptyList()
                        qrNotes = ""
                    },
                    onRequestBill = {
                        val sess = activeSession ?: return@CustomerMobileOrderingPreview
                        onCustomerRequestBill(sess.tableId, "${sess.guestName} (QR Table ${sess.tableNumber})")
                    }
                )
            }
        }
    }

    customizingQrProduct?.let { resolved ->
        TableItemCustomizerModal(
            resolvedProduct = resolved,
            modifierGroups = modifierGroups,
            addons = addons,
            onDismiss = { customizingQrProduct = null },
            onConfirmItem = { variant, mods, itemAddons, notes ->
                qrCart = addOrIncrementCartItem(qrCart, resolved, variant, mods, itemAddons, notes)
                customizingQrProduct = null
            }
        )
    }
}

@Composable
private fun QrSecurityTokenManagerCard(
    company: Company,
    branch: Branch,
    tables: List<RestaurantTable>,
    selectedTable: RestaurantTable?,
    scannedTokenInput: String,
    guestNameInput: String,
    partySizeInput: Int,
    activeSession: CustomerTableSession?,
    onSelectTable: (RestaurantTable) -> Unit,
    onTokenInputChange: (String) -> Unit,
    onGuestNameChange: (String) -> Unit,
    onPartySizeChange: (Int) -> Unit,
    onRotateToken: (RestaurantTable) -> Unit,
    onValidateAndStartSession: () -> Unit,
    onEndSession: () -> Unit
) {
    RestoCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Secure Table QR Generator & Validator",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Opaque SHA-256 tokens — never exposes raw Company, Branch, or Table IDs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.QrCode2,
                contentDescription = "QR Security",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Select Table to Inspect or Scan QR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tables.forEach { tbl ->
                RestoFilterChip(
                    selected = selectedTable?.id == tbl.id,
                    onClick = { onSelectTable(tbl) },
                    label = "${tbl.tableNumber} (${tbl.status.titleEn})"
                )
            }
        }

        selectedTable?.let { tbl ->
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Table ${tbl.tableNumber} Cryptographic Token",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        RestoBadge(text = "OPAQUE SHA-256", variant = BadgeVariant.SUCCESS)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tbl.qrToken,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "URL: ${TableOperationsEngine.buildPublicQrUrl(tbl.qrToken)}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RestoButton(
                        onClick = { onRotateToken(tbl) },
                        variant = RestoButtonVariant.OUTLINE,
                        modifier = Modifier.testTag("rotate_qr_token_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rotate Table QR Token")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // QR Scan & Session Initiator
        RestoTextField(
            value = scannedTokenInput,
            onValueChange = onTokenInputChange,
            label = "Scanned QR Token or URL",
            placeholder = "QR-SIG-..."
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                RestoTextField(
                    value = guestNameInput,
                    onValueChange = onGuestNameChange,
                    label = "Guest Name",
                    placeholder = "Table Guest"
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Party Size", fontSize = 11.sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = { onPartySizeChange((partySizeInput - 1).coerceAtLeast(1)) },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(32.dp)
                    ) { Text("-") }
                    Text("$partySizeInput", fontWeight = FontWeight.Bold)
                    OutlinedButton(
                        onClick = { onPartySizeChange((partySizeInput + 1).coerceAtMost(16)) },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(32.dp)
                    ) { Text("+") }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RestoButton(
                onClick = onValidateAndStartSession,
                modifier = Modifier
                    .weight(1f)
                    .testTag("validate_qr_start_session_btn")
            ) {
                Text("Scan & Validate QR Session")
            }
            if (activeSession != null) {
                RestoButton(
                    onClick = onEndSession,
                    variant = RestoButtonVariant.OUTLINE
                ) {
                    Text("End Session")
                }
            }
        }

        // 7-Step QR Flow Pipeline Verification Display
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "QR Resolution Pipeline Status:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        val steps = listOf(
            "1. Validate Opaque QR Signature" to (activeSession != null),
            "2. Identify Restaurant Tenant (${activeSession?.companyName ?: company.nameEn})" to (activeSession != null),
            "3. Identify Operational Branch (${activeSession?.branchName ?: branch.nameEn})" to (activeSession != null),
            "4. Identify Table & Zone (${activeSession?.tableNumber ?: "Pending"} • ${activeSession?.zoneName ?: ""})" to (activeSession != null),
            "5. Load QR Channel Menu & Pricing" to (activeSession != null),
            "6. Create Authenticated Customer Session (${activeSession?.sessionId ?: "Inactive"})" to (activeSession != null),
            "7. Submit Order to Central Order Engine" to (activeSession?.activeOrderId != null)
        )
        steps.forEach { (label, completed) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Icon(
                    imageVector = if (completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (completed) RestoEmerald600 else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = if (completed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CustomerMobileOrderingPreview(
    activeSession: CustomerTableSession?,
    branch: Branch,
    sessionTable: RestaurantTable?,
    activeOrder: Order?,
    qrMenu: List<ChannelMenuItem>,
    qrCart: List<OrderEngine.PosCartItem>,
    qrNotes: String,
    onNotesChange: (String) -> Unit,
    onTapMenuProduct: (ChannelMenuItem) -> Unit,
    onUpdateCartQty: (String, Int) -> Unit,
    onSubmitQrOrder: () -> Unit,
    onRequestBill: () -> Unit
) {
    RestoCard {
        if (activeSession == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneIphone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(42.dp)
                )
                Text(
                    text = "Scan Table QR to Launch Customer Ordering",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Click 'Scan & Validate QR Session' to validate the table's opaque token and open the guest digital menu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Customer Session Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${activeSession.companyName} • Table ${activeSession.tableNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Welcome, ${activeSession.guestName} (${activeSession.partySize} Guests) • ${activeSession.branchName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                RestoBadge(
                    text = if (sessionTable?.status == TableStatus.WAITING_PAYMENT || activeSession.billRequested) {
                        "BILL REQUESTED"
                    } else {
                        "LIVE QR SESSION"
                    },
                    variant = if (activeSession.billRequested) BadgeVariant.WARNING else BadgeVariant.SUCCESS
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live Order Tracker Banner (if customer has an active order at the table)
            if (activeOrder != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RestoEmerald600.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, RestoEmerald600.copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("qr_live_order_tracker")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Live Order Tracking: ${activeOrder.orderNumber}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            RestoBadge(text = activeOrder.status.titleEn, variant = BadgeVariant.INFO)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        activeOrder.items.forEach { item ->
                            Text(
                                text = "• ${item.quantity}x ${item.productNameEn} [${item.kitchenStatus}] — $${String.format(Locale.US, "%.2f", item.totalPrice)}",
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total: $${String.format(Locale.US, "%.2f", activeOrder.totalAmount)}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            RestoButton(
                                onClick = onRequestBill,
                                variant = RestoButtonVariant.SECONDARY,
                                modifier = Modifier.testTag("qr_customer_request_bill_btn")
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Request Bill")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Digital Menu Items
            Text(
                text = "Browse Table Menu (QR Channel Pricing Applied)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                qrMenu.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTapMenuProduct(item) }
                            .testTag("qr_menu_item_${item.product.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.product.nameEn, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    text = item.product.descriptionEn,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            RestoButton(
                                onClick = { onTapMenuProduct(item) },
                                variant = RestoButtonVariant.OUTLINE
                            ) {
                                Text("+ $${String.format(Locale.US, "%.2f", item.resolvedPrice)}")
                            }
                        }
                    }
                }
            }

            // Customer QR Cart & Submit
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Your Table Cart (${qrCart.sumOf { it.quantity }} items)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            if (qrCart.isEmpty()) {
                Text(
                    text = "Tap any dish above to customize modifiers and add to your table order.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                qrCart.forEach { cItem ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${cItem.quantity}x ${cItem.product.nameEn}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            val mods = listOf(cItem.modifiersSummaryText, cItem.addonsSummaryText)
                                .filter { it.isNotBlank() }
                                .joinToString(", ")
                            if (mods.isNotBlank()) {
                                Text(mods, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onUpdateCartQty(cItem.uid, -1) },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(28.dp)
                            ) { Text("-") }
                            Text("${cItem.quantity}", fontWeight = FontWeight.Bold)
                            OutlinedButton(
                                onClick = { onUpdateCartQty(cItem.uid, 1) },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(28.dp)
                            ) { Text("+") }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                RestoTextField(
                    value = qrNotes,
                    onValueChange = onNotesChange,
                    label = "Special Requests for Kitchen",
                    placeholder = "Extra napkins, sauce on the side..."
                )
                Spacer(modifier = Modifier.height(10.dp))

                val subtotal = qrCart.sumOf { it.netTotal }
                RestoButton(
                    onClick = onSubmitQrOrder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("qr_submit_order_btn")
                ) {
                    Text("Submit Table Order ($${String.format(Locale.US, "%.2f", subtotal)} + Tax)")
                }
            }
        }
    }
}

/**
 * Shared Item Variant & Modifier Customization Dialog for Waiter App & Customer QR Ordering
 */
@Composable
fun TableItemCustomizerModal(
    resolvedProduct: ChannelMenuItem,
    modifierGroups: List<ModifierGroup>,
    addons: List<ProductAddon>,
    onDismiss: () -> Unit,
    onConfirmItem: (
        variant: ProductVariant?,
        selectedModifiers: List<ModifierOption>,
        selectedAddons: List<ProductAddon>,
        notes: String
    ) -> Unit
) {
    val product = resolvedProduct.product
    var selectedVariant by remember {
        mutableStateOf(product.variants.find { it.isDefault } ?: product.variants.firstOrNull())
    }
    val productGroups = remember(product, modifierGroups) {
        modifierGroups.filter { it.id in product.modifierGroupIds }
    }
    val selectedModifiers = remember {
        mutableStateListOf<ModifierOption>().apply {
            productGroups.forEach { grp ->
                grp.options.find { it.isDefault }?.let { add(it) }
            }
        }
    }
    val availableAddons = remember(product, addons) {
        addons.filter { it.id in product.addonIds }
    }
    val selectedAddons = remember { mutableStateListOf<ProductAddon>() }
    var itemNotes by remember { mutableStateOf("") }

    RestoModalDialog(
        onDismissRequest = onDismiss,
        title = "Customize ${product.nameEn}"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (product.variants.isNotEmpty()) {
                Text("Select Size / Variant:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    product.variants.forEach { variant ->
                        RestoFilterChip(
                            selected = selectedVariant?.id == variant.id,
                            onClick = { selectedVariant = variant },
                            label = "${variant.nameEn} (+$${String.format(Locale.US, "%.2f", variant.priceAdjustment)})"
                        )
                    }
                }
            }

            productGroups.forEach { group ->
                Text("${group.nameEn}:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    group.options.forEach { opt ->
                        val isChosen = selectedModifiers.any { it.id == opt.id }
                        RestoFilterChip(
                            selected = isChosen,
                            onClick = {
                                if (group.maxSelections <= 1) {
                                    selectedModifiers.removeAll { it.groupId == group.id }
                                    selectedModifiers.add(opt)
                                } else {
                                    if (isChosen) selectedModifiers.removeAll { it.id == opt.id }
                                    else selectedModifiers.add(opt)
                                }
                            },
                            label = if (opt.priceAdjustment > 0) "${opt.nameEn} (+$${opt.priceAdjustment})" else opt.nameEn
                        )
                    }
                }
            }

            if (availableAddons.isNotEmpty()) {
                Text("Extra Add-ons:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableAddons.forEach { addon ->
                        val isAdded = selectedAddons.any { it.id == addon.id }
                        RestoFilterChip(
                            selected = isAdded,
                            onClick = {
                                if (isAdded) selectedAddons.removeAll { it.id == addon.id }
                                else selectedAddons.add(addon)
                            },
                            label = "${addon.nameEn} (+$${addon.price})"
                        )
                    }
                }
            }

            RestoTextField(
                value = itemNotes,
                onValueChange = { itemNotes = it },
                label = "Item Preparation Note",
                placeholder = "No onions, extra sauce..."
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(modifier = Modifier.width(8.dp))
                RestoButton(
                    onClick = {
                        onConfirmItem(
                            selectedVariant,
                            selectedModifiers.toList(),
                            selectedAddons.toList(),
                            itemNotes
                        )
                    },
                    modifier = Modifier.testTag("confirm_customized_table_item_btn")
                ) {
                    Text("Add to Order")
                }
            }
        }
    }
}

internal fun addOrIncrementCartItem(
    currentCart: List<OrderEngine.PosCartItem>,
    resolved: ChannelMenuItem,
    variant: ProductVariant?,
    modifiers: List<ModifierOption>,
    addons: List<ProductAddon>,
    notes: String
): List<OrderEngine.PosCartItem> {
    val addonMap = addons.associateWith { 1 }
    val existingIdx = currentCart.indexOfFirst {
        it.product.id == resolved.product.id &&
            it.selectedVariant?.id == variant?.id &&
            it.selectedModifiers.map { m -> m.id } == modifiers.map { m -> m.id } &&
            it.selectedAddons.keys.map { a -> a.id } == addons.map { a -> a.id } &&
            it.notes == notes
    }
    return if (existingIdx >= 0) {
        currentCart.toMutableList().also { list ->
            val prev = list[existingIdx]
            list[existingIdx] = prev.copy(quantity = prev.quantity + 1)
        }
    } else {
        currentCart + OrderEngine.PosCartItem(
            uid = "ci_${UUID.randomUUID().toString().take(8)}",
            product = resolved.product,
            quantity = 1,
            baseResolvedPrice = resolved.resolvedPrice + (variant?.priceAdjustment ?: 0.0),
            selectedVariant = variant,
            selectedModifiers = modifiers,
            selectedAddons = addonMap,
            notes = notes
        )
    }
}

internal fun tableStatusVisuals(status: TableStatus): Pair<Color, BadgeVariant> {
    return when (status) {
        TableStatus.AVAILABLE -> RestoEmerald600 to BadgeVariant.SUCCESS
        TableStatus.OCCUPIED -> RestoAmber500 to BadgeVariant.WARNING
        TableStatus.RESERVED -> RestoBlue600 to BadgeVariant.INFO
        TableStatus.WAITING_PAYMENT, TableStatus.BILLED -> RestoRose600 to BadgeVariant.DANGER
        TableStatus.CLEANING -> Color(0xFF8B5CF6) to BadgeVariant.INFO
        TableStatus.UNAVAILABLE -> Color(0xFF64748B) to BadgeVariant.NEUTRAL
    }
}
