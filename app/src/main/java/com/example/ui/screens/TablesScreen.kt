package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.engine.TableOperationsEngine
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.Translations
import com.example.data.repository.DefaultSeedData
import com.example.designsystem.*
import com.example.domain.model.*
import com.example.ui.theme.*
import java.util.Locale
import java.util.UUID

enum class TablesModuleTab(val key: String) {
    FLOOR_MAP("tables_tab_floor_map"),
    WAITER_APP("tables_tab_waiter_app"),
    QR_ORDERING("tables_tab_qr_ordering"),
    RESERVATIONS("tables_tab_reservations")
}

@Composable
fun TablesScreen(
    tables: List<RestaurantTable>,
    language: AppLanguage,
    onUpdateTableStatus: (tableId: String, status: TableStatus) -> Unit,
    modifier: Modifier = Modifier,
    // Phase 5 Extended Parameters (with defaults for backward compatibility)
    company: Company = Company("comp_01", "AL-HAYA", "Al-Haya Hospitality Group", "مجموعة الحياة", "TX-9948210"),
    branch: Branch = Branch("branch_01", "brand_01", "DOWNTOWN-HQ", "Downtown Flagship Branch", "فرع وسط المدينة", "+966 11 456 7890", "King Fahd Rd"),
    branches: List<Branch> = listOf(branch),
    currentUser: User = User("user_04", "comp_01", "waiter_sarah", "sarah@restoops.com", "Sarah Nader", "ROLE_WAITER", "branch_01"),
    floors: List<RestaurantFloor> = DefaultSeedData.floors,
    zones: List<TableZone> = DefaultSeedData.zones,
    reservations: List<Reservation> = DefaultSeedData.reservations,
    orders: List<Order> = emptyList(),
    products: List<Product> = DefaultSeedData.products,
    categories: List<Category> = DefaultSeedData.categories,
    modifierGroups: List<ModifierGroup> = DefaultSeedData.modifierGroups,
    addons: List<ProductAddon> = DefaultSeedData.addons,
    priceRules: List<ChannelPriceRule> = DefaultSeedData.priceRules,
    taxProfiles: List<TaxProfile> = DefaultSeedData.taxProfiles,
    employees: List<Employee> = emptyList(),
    customers: List<Customer> = DefaultSeedData.customers,
    activeCustomerQrSession: CustomerTableSession? = null,
    onSaveTable: (RestaurantTable) -> Unit = {},
    onOpenTableForGuests: (tableId: String, guestCount: Int, waiterId: String?, waiterName: String?) -> Unit = { id, _, _, _ ->
        onUpdateTableStatus(id, TableStatus.OCCUPIED)
    },
    onCombineTables: (primaryTableId: String, secondaryTableIds: List<String>) -> Unit = { _, _ -> },
    onUncombineTable: (tableId: String) -> Unit = {},
    onMoveTableSession: (sourceTableId: String, targetTableId: String) -> Unit = { _, _ -> },
    onMergeOrders: (primaryOrderId: String, secondaryOrderId: String) -> Unit = { _, _ -> },
    onPlaceOrUpdateOrder: (Order, List<OrderItem>) -> Unit = { _, _ -> },
    onRequestTableBill: (tableId: String, requestedBy: String) -> Unit = { id, _ ->
        onUpdateTableStatus(id, TableStatus.WAITING_PAYMENT)
    },
    onRegenerateTableQrToken: (tableId: String) -> Unit = {},
    onStartCustomerQrSession: (scannedToken: String, guestName: String, partySize: Int) -> Unit = { _, _, _ -> },
    onClearCustomerQrSession: () -> Unit = {},
    onSaveReservation: (Reservation) -> Unit = {},
    onUpdateReservationStatus: (reservationId: String, status: ReservationStatus) -> Unit = { _, _ -> },
    onShowFeedback: (String) -> Unit = {}
) {
    var activeTab by remember { mutableStateOf(TablesModuleTab.FLOOR_MAP) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("tables_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header & Live Floor KPI Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = Translations.get("nav_tables", language),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                val freeCount = tables.count { it.status == TableStatus.AVAILABLE }
                val occupiedCount = tables.count { it.status == TableStatus.OCCUPIED }
                val waitingBillCount = tables.count { it.status == TableStatus.WAITING_PAYMENT || it.status == TableStatus.BILLED }
                Text(
                    text = "$freeCount of ${tables.size} tables free • $occupiedCount occupied • $waitingBillCount awaiting payment • ${reservations.count { it.status == ReservationStatus.CONFIRMED }} confirmed reservations",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 4-Module Navigation Tabs (Floor Map, Waiter Handheld, QR Table Ordering, Reservations)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TablesModuleTab.entries.forEach { tab ->
                RestoFilterChip(
                    selected = activeTab == tab,
                    onClick = { activeTab = tab },
                    label = Translations.get(tab.key, language),
                    modifier = Modifier.testTag("tables_tab_${tab.name.lowercase()}")
                )
            }
        }

        // Active Tab Content
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (activeTab) {
                TablesModuleTab.FLOOR_MAP -> SmartFloorMapTab(
                    branch = branch,
                    floors = floors,
                    zones = zones,
                    tables = tables,
                    orders = orders,
                    reservations = reservations,
                    language = language,
                    onUpdateTableStatus = onUpdateTableStatus,
                    onSaveTable = onSaveTable,
                    onOpenTableForGuests = onOpenTableForGuests,
                    onCombineTables = onCombineTables,
                    onUncombineTable = onUncombineTable,
                    onMoveTableSession = onMoveTableSession,
                    onRequestTableBill = onRequestTableBill,
                    onRegenerateTableQrToken = onRegenerateTableQrToken,
                    onOpenInWaiterPad = {
                        activeTab = TablesModuleTab.WAITER_APP
                    },
                    onOpenInQrPreview = { tbl ->
                        onStartCustomerQrSession(tbl.qrToken, "Guest at ${tbl.tableNumber}", tbl.capacity)
                        activeTab = TablesModuleTab.QR_ORDERING
                    }
                )

                TablesModuleTab.WAITER_APP -> WaiterHandheldTab(
                    branch = branch,
                    tables = tables,
                    orders = orders,
                    products = products,
                    categories = categories,
                    modifierGroups = modifierGroups,
                    addons = addons,
                    priceRules = priceRules,
                    taxProfiles = taxProfiles,
                    employees = employees,
                    currentUser = currentUser,
                    language = language,
                    onOpenTable = { tblId, guests, wId, wName ->
                        onOpenTableForGuests(tblId, guests, wId, wName)
                    },
                    onPlaceOrUpdateOrder = onPlaceOrUpdateOrder,
                    onRequestBill = onRequestTableBill,
                    onMoveTable = onMoveTableSession,
                    onMergeOrders = onMergeOrders,
                    onCombineTables = onCombineTables,
                    onUpdateTableStatus = onUpdateTableStatus,
                    onShowFeedback = onShowFeedback
                )

                TablesModuleTab.QR_ORDERING -> CustomerQrOrderingTab(
                    company = company,
                    branch = branch,
                    branches = branches,
                    tables = tables,
                    orders = orders,
                    products = products,
                    categories = categories,
                    modifierGroups = modifierGroups,
                    addons = addons,
                    priceRules = priceRules,
                    taxProfiles = taxProfiles,
                    activeSession = activeCustomerQrSession,
                    onStartQrSession = onStartCustomerQrSession,
                    onClearQrSession = onClearCustomerQrSession,
                    onRotateTableQrToken = onRegenerateTableQrToken,
                    onSubmitCustomerOrder = onPlaceOrUpdateOrder,
                    onCustomerRequestBill = onRequestTableBill,
                    onShowFeedback = onShowFeedback
                )

                TablesModuleTab.RESERVATIONS -> ReservationsDeskTab(
                    branch = branch,
                    tables = tables,
                    reservations = reservations,
                    customers = customers,
                    language = language,
                    onSaveReservation = onSaveReservation,
                    onUpdateReservationStatus = onUpdateReservationStatus,
                    onShowFeedback = onShowFeedback
                )
            }
        }
    }
}

/**
 * Tab 1: Smart Floor Map, Zones, Table States, Combinations & Movement
 */
@Composable
private fun SmartFloorMapTab(
    branch: Branch,
    floors: List<RestaurantFloor>,
    zones: List<TableZone>,
    tables: List<RestaurantTable>,
    orders: List<Order>,
    reservations: List<Reservation>,
    language: AppLanguage,
    onUpdateTableStatus: (tableId: String, status: TableStatus) -> Unit,
    onSaveTable: (RestaurantTable) -> Unit,
    onOpenTableForGuests: (tableId: String, guestCount: Int, waiterId: String?, waiterName: String?) -> Unit,
    onCombineTables: (primaryTableId: String, secondaryTableIds: List<String>) -> Unit,
    onUncombineTable: (tableId: String) -> Unit,
    onMoveTableSession: (sourceTableId: String, targetTableId: String) -> Unit,
    onRequestTableBill: (tableId: String, requestedBy: String) -> Unit,
    onRegenerateTableQrToken: (tableId: String) -> Unit,
    onOpenInWaiterPad: (RestaurantTable) -> Unit,
    onOpenInQrPreview: (RestaurantTable) -> Unit
) {
    var selectedFloorId by remember { mutableStateOf("ALL") }
    var selectedSection by remember { mutableStateOf("ALL") }
    var selectedStatusFilter by remember { mutableStateOf<TableStatus?>(null) }

    var activeTableForDialog by remember { mutableStateOf<RestaurantTable?>(null) }
    var showAddTableDialog by remember { mutableStateOf(false) }
    var showCombineDialogFor by remember { mutableStateOf<RestaurantTable?>(null) }
    var showMoveDialogFor by remember { mutableStateOf<RestaurantTable?>(null) }

    val sections = listOf(
        "ALL" to if (language == AppLanguage.ARABIC) "جميع الصالات" else "All Zones",
        "Indoor Main" to if (language == AppLanguage.ARABIC) "الصالة الرئيسية" else "Indoor Main",
        "Terrace Garden" to if (language == AppLanguage.ARABIC) "الشرفة الخارجية" else "Terrace Garden",
        "VIP Suite" to if (language == AppLanguage.ARABIC) "جناح كبار الشخصيات" else "VIP Suite"
    )

    val filteredTables = remember(tables, selectedFloorId, selectedSection, selectedStatusFilter) {
        tables.filter { tbl ->
            val floorMatch = selectedFloorId == "ALL" || tbl.floorId == selectedFloorId
            val zoneMatch = selectedSection == "ALL" || tbl.section == selectedSection
            val statusMatch = selectedStatusFilter == null || tbl.status == selectedStatusFilter
            floorMatch && zoneMatch && statusMatch
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Floor Selector + Zone Filters + Add Table Action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RestoFilterChip(
                    selected = selectedFloorId == "ALL",
                    onClick = { selectedFloorId = "ALL" },
                    label = "All Floors (${floors.size})"
                )
                floors.forEach { flr ->
                    RestoFilterChip(
                        selected = selectedFloorId == flr.id,
                        onClick = { selectedFloorId = flr.id },
                        label = if (language == AppLanguage.ARABIC) flr.nameAr else flr.nameEn
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            RestoButton(
                onClick = { showAddTableDialog = true },
                variant = RestoButtonVariant.PRIMARY,
                modifier = Modifier.testTag("add_new_table_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Table")
            }
        }

        // Zone & Status Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            sections.forEach { (id, title) ->
                RestoFilterChip(
                    selected = selectedSection == id,
                    onClick = { selectedSection = id },
                    label = title
                )
            }

            VerticalDivider(modifier = Modifier.height(22.dp))

            val mainStates = listOf(
                TableStatus.AVAILABLE,
                TableStatus.OCCUPIED,
                TableStatus.RESERVED,
                TableStatus.WAITING_PAYMENT,
                TableStatus.CLEANING,
                TableStatus.UNAVAILABLE
            )
            mainStates.forEach { st ->
                RestoFilterChip(
                    selected = selectedStatusFilter == st,
                    onClick = {
                        selectedStatusFilter = if (selectedStatusFilter == st) null else st
                    },
                    label = st.titleEn
                )
            }
        }

        // Tables Interactive Floor Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 195.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredTables) { table ->
                val (statusColor, badgeVariant) = tableStatusVisuals(table.status)
                val linkedOrder = orders.find { it.id == table.currentOrderId }
                val linkedReservation = reservations.find { it.id == table.activeReservationId }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                        .clickable { activeTableForDialog = table }
                        .testTag("table_card_${table.tableNumber}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.5.dp, statusColor.copy(alpha = 0.65f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Table ${table.tableNumber}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${table.section} • ${table.shape}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            RestoBadge(text = table.status.name, variant = badgeVariant)
                        }

                        // Middle Info: Combined Status / Active Order / Reservation / QR Token
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (table.combinedWithTableIds.isNotEmpty()) {
                                Text(
                                    text = "Joined Group (${table.combinedCapacity} Combined Seats)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (linkedOrder != null) {
                                Text(
                                    text = "${linkedOrder.orderNumber} • $${String.format(Locale.US, "%.2f", linkedOrder.totalAmount)} (${linkedOrder.status.titleEn})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            } else if (linkedReservation != null) {
                                Text(
                                    text = "Res: ${linkedReservation.customerName} (${linkedReservation.reservationTime})",
                                    fontSize = 11.sp,
                                    color = RestoBlue600
                                )
                            } else {
                                Text(
                                    text = "QR: ${table.qrToken.take(15)}...",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Footer: Capacity & Assigned Waiter
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "${table.combinedCapacity} Seats",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = table.assignedWaiterName ?: "Unassigned",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    // Comprehensive Table Management & Operations Dialog
    activeTableForDialog?.let { table ->
        RestoModalDialog(
            onDismissRequest = { activeTableForDialog = null },
            title = "Manage Table ${table.tableNumber}"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Floor: ${table.floorName} • Zone: ${table.section} • Capacity: ${table.combinedCapacity} guests",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Secure QR Token: ${table.qrToken}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Update Operational Status (6 Controlled Table States):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                RestoButton(
                    onClick = {
                        onUpdateTableStatus(table.id, TableStatus.AVAILABLE)
                        activeTableForDialog = null
                    },
                    variant = RestoButtonVariant.OUTLINE,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Set as Available (Empty & Clean)")
                }
                RestoButton(
                    onClick = {
                        onOpenTableForGuests(table.id, table.capacity, table.assignedWaiterId, table.assignedWaiterName)
                        activeTableForDialog = null
                    },
                    variant = RestoButtonVariant.PRIMARY,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Seat Guests (Occupied)")
                }
                RestoButton(
                    onClick = {
                        onUpdateTableStatus(table.id, TableStatus.RESERVED)
                        activeTableForDialog = null
                    },
                    variant = RestoButtonVariant.OUTLINE,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mark as Reserved")
                }
                RestoButton(
                    onClick = {
                        onRequestTableBill(table.id, "Floor Manager")
                        activeTableForDialog = null
                    },
                    variant = RestoButtonVariant.SECONDARY,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Issue Bill (Awaiting Payment)")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RestoButton(
                        onClick = {
                            onUpdateTableStatus(table.id, TableStatus.CLEANING)
                            activeTableForDialog = null
                        },
                        variant = RestoButtonVariant.OUTLINE,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cleaning")
                    }
                    RestoButton(
                        onClick = {
                            onUpdateTableStatus(table.id, TableStatus.UNAVAILABLE)
                            activeTableForDialog = null
                        },
                        variant = RestoButtonVariant.OUTLINE,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Unavailable")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                Text(
                    text = "Smart Table Operations & Ordering:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RestoButton(
                        onClick = {
                            showCombineDialogFor = table
                            activeTableForDialog = null
                        },
                        variant = RestoButtonVariant.OUTLINE,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("modal_combine_table_btn")
                    ) {
                        Text("Combine Tables")
                    }
                    if (table.combinedWithTableIds.isNotEmpty()) {
                        RestoButton(
                            onClick = {
                                onUncombineTable(table.id)
                                activeTableForDialog = null
                            },
                            variant = RestoButtonVariant.OUTLINE,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Split Group")
                        }
                    } else {
                        RestoButton(
                            onClick = {
                                showMoveDialogFor = table
                                activeTableForDialog = null
                            },
                            variant = RestoButtonVariant.OUTLINE,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("modal_move_table_btn")
                        ) {
                            Text("Move Table")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RestoButton(
                        onClick = {
                            val target = table
                            activeTableForDialog = null
                            onOpenInWaiterPad(target)
                        },
                        variant = RestoButtonVariant.PRIMARY,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Waiter Order Pad")
                    }
                    RestoButton(
                        onClick = {
                            val target = table
                            activeTableForDialog = null
                            onOpenInQrPreview(target)
                        },
                        variant = RestoButtonVariant.SECONDARY,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Launch QR Menu")
                    }
                }

                RestoButton(
                    onClick = {
                        onRegenerateTableQrToken(table.id)
                        activeTableForDialog = null
                    },
                    variant = RestoButtonVariant.OUTLINE,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Rotate Secure QR Token")
                }
            }
        }
    }

    // Combine Tables Modal
    showCombineDialogFor?.let { primary ->
        val selectedSecondaries = remember { mutableStateListOf<String>() }
        RestoModalDialog(
            onDismissRequest = { showCombineDialogFor = null },
            title = "Combine Tables with ${primary.tableNumber}"
        ) {
            Text(
                text = "Select one or more tables to join with ${primary.tableNumber} (${primary.capacity} seats):",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                tables.filter { it.id != primary.id }.forEach { candidate ->
                    val isChecked = candidate.id in selectedSecondaries
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isChecked) selectedSecondaries.remove(candidate.id)
                                else selectedSecondaries.add(candidate.id)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${candidate.tableNumber} (${candidate.section} • ${candidate.capacity} seats)")
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) selectedSecondaries.add(candidate.id)
                                else selectedSecondaries.remove(candidate.id)
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            RestoButton(
                onClick = {
                    onCombineTables(primary.id, selectedSecondaries.toList())
                    showCombineDialogFor = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("confirm_combine_tables_btn")
            ) {
                Text("Confirm Table Combination")
            }
        }
    }

    // Move Table Modal
    showMoveDialogFor?.let { source ->
        val targets = tables.filter {
            it.id != source.id && it.status in listOf(TableStatus.AVAILABLE, TableStatus.RESERVED)
        }
        RestoModalDialog(
            onDismissRequest = { showMoveDialogFor = null },
            title = "Move Table ${source.tableNumber} Session"
        ) {
            Text(
                text = "Choose destination table for guests and active order:",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                targets.forEach { target ->
                    OutlinedButton(
                        onClick = {
                            onMoveTableSession(source.id, target.id)
                            showMoveDialogFor = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Move to ${target.tableNumber} (${target.section} • ${target.combinedCapacity} Seats)")
                    }
                }
            }
        }
    }

    // Add New Table Dialog
    if (showAddTableDialog) {
        var tableNumber by remember { mutableStateOf("T-0${tables.size + 1}") }
        var capacityText by remember { mutableStateOf("4") }
        var selectedZone by remember { mutableStateOf(zones.firstOrNull() ?: DefaultSeedData.zones.first()) }
        var selectedShape by remember { mutableStateOf("RECTANGLE") }

        RestoModalDialog(
            onDismissRequest = { showAddTableDialog = false },
            title = "Add Smart Floor Table"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RestoTextField(
                    value = tableNumber,
                    onValueChange = { tableNumber = it },
                    label = "Table Number",
                    placeholder = "T-09"
                )
                RestoTextField(
                    value = capacityText,
                    onValueChange = { capacityText = it },
                    label = "Seating Capacity",
                    placeholder = "4"
                )
                Text("Select Zone & Floor:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    zones.forEach { z ->
                        RestoFilterChip(
                            selected = selectedZone.id == z.id,
                            onClick = { selectedZone = z },
                            label = z.nameEn
                        )
                    }
                }
                Text("Table Shape:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ROUND", "RECTANGLE", "BOOTH").forEach { shp ->
                        RestoFilterChip(
                            selected = selectedShape == shp,
                            onClick = { selectedShape = shp },
                            label = shp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                RestoButton(
                    onClick = {
                        val cap = capacityText.toIntOrNull()?.coerceAtLeast(1) ?: 4
                        val id = "tbl_${UUID.randomUUID().toString().take(6)}"
                        val floor = floors.find { it.id == selectedZone.floorId } ?: floors.firstOrNull()
                        val newTable = RestaurantTable(
                            id = id,
                            branchId = branch.id,
                            tableNumber = tableNumber.ifBlank { "T-${tables.size + 1}" },
                            capacity = cap,
                            section = selectedZone.nameEn,
                            status = TableStatus.AVAILABLE,
                            floorId = floor?.id ?: "floor_01",
                            floorName = floor?.nameEn ?: "Ground Floor Dining",
                            zoneId = selectedZone.id,
                            combinedCapacity = cap,
                            assignedWaiterId = "user_04",
                            assignedWaiterName = "Sarah Nader",
                            qrToken = TableOperationsEngine.generateSecureQrToken("comp_01", branch.id, id, tableNumber),
                            shape = selectedShape
                        )
                        onSaveTable(newTable)
                        showAddTableDialog = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_new_table_btn")
                ) {
                    Text("Create Table & Generate Secure QR")
                }
            }
        }
    }
}

/**
 * Tab 4: Reservations Desk
 * Supports: Reservation creation, Customer linking, Table assignment, Date/Time, Party Size,
 * Status transitions (Pending, Confirmed, Seated, Completed, Cancelled, No-Show), and Notes.
 */
@Composable
private fun ReservationsDeskTab(
    branch: Branch,
    tables: List<RestaurantTable>,
    reservations: List<Reservation>,
    customers: List<Customer>,
    language: AppLanguage,
    onSaveReservation: (Reservation) -> Unit,
    onUpdateReservationStatus: (reservationId: String, status: ReservationStatus) -> Unit,
    onShowFeedback: (String) -> Unit
) {
    var selectedStatusFilter by remember { mutableStateOf<ReservationStatus?>(null) }
    var showCreateReservationDialog by remember { mutableStateOf(false) }

    val filteredReservations = remember(reservations, selectedStatusFilter) {
        if (selectedStatusFilter == null) reservations
        else reservations.filter { it.status == selectedStatusFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reservations_desk_tab"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Filter & Create Reservation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RestoFilterChip(
                    selected = selectedStatusFilter == null,
                    onClick = { selectedStatusFilter = null },
                    label = "All (${reservations.size})"
                )
                ReservationStatus.entries.forEach { st ->
                    RestoFilterChip(
                        selected = selectedStatusFilter == st,
                        onClick = { selectedStatusFilter = st },
                        label = st.titleEn
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            RestoButton(
                onClick = { showCreateReservationDialog = true },
                modifier = Modifier.testTag("new_reservation_btn")
            ) {
                Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(Translations.get("tables_new_reservation", language))
            }
        }

        // Reservations List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredReservations, key = { it.id }) { res ->
                val badgeVariant = when (res.status) {
                    ReservationStatus.CONFIRMED -> BadgeVariant.INFO
                    ReservationStatus.SEATED, ReservationStatus.COMPLETED -> BadgeVariant.SUCCESS
                    ReservationStatus.PENDING -> BadgeVariant.WARNING
                    ReservationStatus.CANCELLED, ReservationStatus.NO_SHOW -> BadgeVariant.DANGER
                }

                RestoCard(modifier = Modifier.testTag("reservation_card_${res.id}")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${res.customerName} (${res.partySize} Guests)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                RestoBadge(text = res.confirmationCode, variant = BadgeVariant.NEUTRAL)
                                RestoBadge(text = res.status.titleEn, variant = badgeVariant)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Table ${res.tableNumber} • Date: ${res.reservationDate} at ${res.reservationTime} • Phone: ${res.customerPhone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (res.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Notes: ${res.notes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Reservation Lifecycle Action Buttons: Confirm, Seat, Complete, Cancel, No-Show
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (res.status == ReservationStatus.PENDING) {
                            RestoButton(
                                onClick = { onUpdateReservationStatus(res.id, ReservationStatus.CONFIRMED) },
                                variant = RestoButtonVariant.PRIMARY,
                                modifier = Modifier.testTag("res_confirm_${res.id}")
                            ) {
                                Text("Confirm Reservation")
                            }
                        }
                        if (res.status in listOf(ReservationStatus.PENDING, ReservationStatus.CONFIRMED)) {
                            RestoButton(
                                onClick = { onUpdateReservationStatus(res.id, ReservationStatus.SEATED) },
                                variant = RestoButtonVariant.SECONDARY,
                                modifier = Modifier.testTag("res_seat_${res.id}")
                            ) {
                                Text("Seat Guests Now")
                            }
                            RestoButton(
                                onClick = { onUpdateReservationStatus(res.id, ReservationStatus.NO_SHOW) },
                                variant = RestoButtonVariant.OUTLINE,
                                modifier = Modifier.testTag("res_noshow_${res.id}")
                            ) {
                                Text("Mark No-Show")
                            }
                            RestoButton(
                                onClick = { onUpdateReservationStatus(res.id, ReservationStatus.CANCELLED) },
                                variant = RestoButtonVariant.OUTLINE,
                                modifier = Modifier.testTag("res_cancel_${res.id}")
                            ) {
                                Text("Cancel")
                            }
                        }
                        if (res.status == ReservationStatus.SEATED) {
                            RestoButton(
                                onClick = { onUpdateReservationStatus(res.id, ReservationStatus.COMPLETED) },
                                variant = RestoButtonVariant.OUTLINE
                            ) {
                                Text("Complete Visit")
                            }
                        }
                    }
                }
            }
        }
    }

    // Create New Reservation Modal
    if (showCreateReservationDialog) {
        var customerName by remember { mutableStateOf("") }
        var customerPhone by remember { mutableStateOf("") }
        var partySizeText by remember { mutableStateOf("4") }
        var dateText by remember { mutableStateOf("2026-10-25") }
        var timeText by remember { mutableStateOf("20:30") }
        var notesText by remember { mutableStateOf("") }
        var selectedTable by remember { mutableStateOf(tables.firstOrNull()) }

        RestoModalDialog(
            onDismissRequest = { showCreateReservationDialog = false },
            title = "Create Table Reservation"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Quick CRM Customer Selector
                if (customers.isNotEmpty()) {
                    Text("Quick Fill from CRM Guest:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        customers.forEach { cust ->
                            RestoFilterChip(
                                selected = customerName == cust.name,
                                onClick = {
                                    customerName = cust.name
                                    customerPhone = cust.phone
                                },
                                label = cust.name
                            )
                        }
                    }
                }

                RestoTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = "Guest Full Name",
                    placeholder = "e.g., Layla Al-Otaibi"
                )
                RestoTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    label = "Guest Phone Number",
                    placeholder = "+966 50 303 4040"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        RestoTextField(
                            value = dateText,
                            onValueChange = { dateText = it },
                            label = "Date (YYYY-MM-DD)",
                            placeholder = "2026-10-25"
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        RestoTextField(
                            value = timeText,
                            onValueChange = { timeText = it },
                            label = "Time (HH:mm)",
                            placeholder = "20:30"
                        )
                    }
                }
                RestoTextField(
                    value = partySizeText,
                    onValueChange = { partySizeText = it },
                    label = "Party Size (Guests)",
                    placeholder = "4"
                )

                Text("Assign Table:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tables.forEach { tbl ->
                        RestoFilterChip(
                            selected = selectedTable?.id == tbl.id,
                            onClick = { selectedTable = tbl },
                            label = "${tbl.tableNumber} (${tbl.combinedCapacity}p)"
                        )
                    }
                }

                RestoTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = "Special Notes / VIP Preferences",
                    placeholder = "Window seat, high chair, birthday..."
                )

                RestoButton(
                    onClick = {
                        val tbl = selectedTable ?: return@RestoButton
                        val pax = partySizeText.toIntOrNull() ?: 2
                        val validationErr = TableOperationsEngine.validateReservation(
                            customerName = customerName,
                            customerPhone = customerPhone,
                            partySize = pax,
                            table = tbl
                        )
                        if (validationErr != null) {
                            onShowFeedback(validationErr)
                            return@RestoButton
                        }
                        val id = "res_${UUID.randomUUID().toString().take(6)}"
                        val newRes = Reservation(
                            id = id,
                            branchId = branch.id,
                            customerName = customerName.trim(),
                            customerPhone = customerPhone.trim(),
                            tableId = tbl.id,
                            tableNumber = tbl.tableNumber,
                            reservationDate = dateText.ifBlank { "2026-10-25" },
                            reservationTime = timeText.ifBlank { "20:00" },
                            partySize = pax,
                            status = ReservationStatus.CONFIRMED,
                            notes = notesText.trim(),
                            confirmationCode = "RES-${(1000..9999).random()}"
                        )
                        onSaveReservation(newRes)
                        showCreateReservationDialog = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_reservation_submit_btn")
                ) {
                    Text("Confirm & Reserve Table")
                }
            }
        }
    }
}
