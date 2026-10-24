package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.engine.KitchenEngine
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.Translations
import com.example.data.repository.DefaultSeedData
import com.example.designsystem.*
import com.example.domain.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay

enum class KdsMainTab {
    LIVE_BOARD,
    MULTI_SCREEN,
    ROUTING_CONFIG,
    REALTIME_BUS,
    RECALL_HISTORY
}

@Composable
fun KitchenDisplayScreen(
    tickets: List<KitchenTicket>,
    language: AppLanguage,
    onUpdateTicketStatus: (ticketId: String, newStatus: String) -> Unit,
    modifier: Modifier = Modifier,
    branchId: String = "branch_01",
    kitchens: List<Kitchen> = DefaultSeedData.kitchens,
    preparationAreas: List<PreparationArea> = DefaultSeedData.preparationAreas,
    stations: List<KitchenStation> = DefaultSeedData.kitchenStations,
    routingRules: List<StationRoutingRule> = DefaultSeedData.stationRoutingRules,
    kitchenEmployees: List<KitchenEmployeeAssignment> = DefaultSeedData.kitchenEmployees,
    realTimeEvents: List<RealTimeOperationalEvent> = emptyList(),
    categories: List<Category> = DefaultSeedData.categories,
    products: List<Product> = DefaultSeedData.products,
    onTransitionTicket: (ticketId: String, targetStatus: KdsTicketStatus) -> Unit = { id, st ->
        onUpdateTicketStatus(id, st.code)
    },
    onSaveStation: (KitchenStation) -> Unit = {},
    onSaveRoutingRule: (StationRoutingRule) -> Unit = {},
    onDeleteRoutingRule: (String) -> Unit = {},
    onSaveKitchenEmployee: (KitchenEmployeeAssignment) -> Unit = {},
    onToggleEmployeeDuty: (String, Boolean) -> Unit = { _, _ -> }
) {
    val isAr = language == AppLanguage.ARABIC
    var activeTab by remember { mutableStateOf(KdsMainTab.LIVE_BOARD) }
    var selectedStationType by remember { mutableStateOf<KitchenStationType?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<KdsTicketStatus?>(null) }
    var showOverdueOnly by remember { mutableStateOf(false) }

    // Lightweight 1-second local clock tick strictly for elapsed timer computation (no network/DB polling)
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val activeLineTickets = remember(tickets) {
        tickets.filter { it.ticketStatus !in setOf(KdsTicketStatus.COMPLETED, KdsTicketStatus.CANCELLED) }
    }
    val completedAndCancelledTickets = remember(tickets) {
        tickets.filter { it.ticketStatus in setOf(KdsTicketStatus.COMPLETED, KdsTicketStatus.CANCELLED) }
    }
    val overdueCount = remember(activeLineTickets, nowMillis) {
        activeLineTickets.count { it.isOverdue(nowMillis) }
    }
    val warningCount = remember(activeLineTickets, nowMillis) {
        activeLineTickets.count { it.isWarning(nowMillis) }
    }

    val filteredLiveTickets = remember(activeLineTickets, selectedStationType, selectedStatusFilter, showOverdueOnly, nowMillis) {
        activeLineTickets.filter { tkt ->
            val matchesStation = selectedStationType == null || tkt.stationType == selectedStationType
            val matchesStatus = selectedStatusFilter == null || tkt.ticketStatus == selectedStatusFilter
            val matchesOverdue = !showOverdueOnly || tkt.isOverdue(nowMillis)
            matchesStation && matchesStatus && matchesOverdue
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("kds_screen")
    ) {
        // KDS Top Header & Real-Time Status Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = Translations.get("kds_title", language),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = RestoEmerald600.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, RestoEmerald600.copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(RestoEmerald600)
                            )
                            Text(
                                text = if (isAr) "بث فوري نشط" else "REAL-TIME EVENT BUS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = RestoEmerald600
                            )
                        }
                    }
                }
                Text(
                    text = if (isAr) {
                        "${activeLineTickets.size} تذاكر نشطة • ${stations.size} محطات تحضير • ${kitchenEmployees.count { it.isOnDuty }} طهاة مناوبون"
                    } else {
                        "${activeLineTickets.size} active tickets • ${stations.size} stations • ${kitchenEmployees.count { it.isOnDuty }} kitchen staff on duty"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // KPI Summary Pills
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (overdueCount > 0) {
                    RestoBadge(
                        text = "${Translations.get("kds_overdue_alert", language)}: $overdueCount",
                        variant = BadgeVariant.DANGER
                    )
                }
                if (warningCount > 0) {
                    RestoBadge(
                        text = "${Translations.get("kds_warning_alert", language)}: $warningCount",
                        variant = BadgeVariant.WARNING
                    )
                }
                RestoBadge(
                    text = "READY: ${activeLineTickets.count { it.ticketStatus == KdsTicketStatus.READY }}",
                    variant = BadgeVariant.SUCCESS
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main KDS Mode Tabs
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = activeTab == KdsMainTab.LIVE_BOARD,
                    onClick = { activeTab = KdsMainTab.LIVE_BOARD },
                    label = { Text("${Translations.get("kds_tab_live_board", language)} (${activeLineTickets.size})") },
                    leadingIcon = { Icon(Icons.Default.OutdoorGrill, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("kds_tab_live_board")
                )
            }
            item {
                FilterChip(
                    selected = activeTab == KdsMainTab.MULTI_SCREEN,
                    onClick = { activeTab = KdsMainTab.MULTI_SCREEN },
                    label = { Text(Translations.get("kds_tab_multi_screen", language)) },
                    leadingIcon = { Icon(Icons.Default.Splitscreen, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("kds_tab_multi_screen")
                )
            }
            item {
                FilterChip(
                    selected = activeTab == KdsMainTab.ROUTING_CONFIG,
                    onClick = { activeTab = KdsMainTab.ROUTING_CONFIG },
                    label = { Text(Translations.get("kds_tab_routing_config", language)) },
                    leadingIcon = { Icon(Icons.Default.AltRoute, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("kds_tab_routing_config")
                )
            }
            item {
                FilterChip(
                    selected = activeTab == KdsMainTab.REALTIME_BUS,
                    onClick = { activeTab = KdsMainTab.REALTIME_BUS },
                    label = { Text("${Translations.get("kds_tab_realtime_stream", language)} (${realTimeEvents.size})") },
                    leadingIcon = { Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("kds_tab_realtime_stream")
                )
            }
            item {
                FilterChip(
                    selected = activeTab == KdsMainTab.RECALL_HISTORY,
                    onClick = { activeTab = KdsMainTab.RECALL_HISTORY },
                    label = { Text("${Translations.get("kds_tab_recall_completed", language)} (${completedAndCancelledTickets.size})") },
                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("kds_tab_recall_completed")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (activeTab) {
            KdsMainTab.LIVE_BOARD -> {
                // Station Selector Bar (All, Grill, Pizza, Fryer, Drinks, Dessert, Packing, Expediter)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedStationType == null,
                            onClick = { selectedStationType = null },
                            label = {
                                Text(
                                    text = if (isAr) "جميع المحطات (${activeLineTickets.size})" else "All Stations (${activeLineTickets.size})",
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            modifier = Modifier.testTag("kds_station_filter_ALL")
                        )
                    }
                    items(KitchenStationType.entries) { stType ->
                        val count = activeLineTickets.count { it.stationType == stType }
                        FilterChip(
                            selected = selectedStationType == stType,
                            onClick = {
                                selectedStationType = if (selectedStationType == stType) null else stType
                            },
                            label = {
                                Text("${if (isAr) stType.titleAr else stType.titleEn} ($count)")
                            },
                            modifier = Modifier.testTag("kds_station_filter_${stType.code}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Status & Overdue Filter Bar
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedStatusFilter == null && !showOverdueOnly,
                            onClick = {
                                selectedStatusFilter = null
                                showOverdueOnly = false
                            },
                            label = { Text(if (isAr) "كل الحالات النشطة" else "All Active Statuses", fontSize = 11.sp) }
                        )
                    }
                    items(listOf(KdsTicketStatus.NEW, KdsTicketStatus.ACCEPTED, KdsTicketStatus.PREPARING, KdsTicketStatus.READY)) { st ->
                        val stCount = activeLineTickets.count { it.ticketStatus == st }
                        FilterChip(
                            selected = selectedStatusFilter == st,
                            onClick = {
                                selectedStatusFilter = if (selectedStatusFilter == st) null else st
                            },
                            label = { Text("${if (isAr) st.titleAr else st.titleEn} ($stCount)", fontSize = 11.sp) },
                            modifier = Modifier.testTag("kds_status_filter_${st.code}")
                        )
                    }
                    item {
                        FilterChip(
                            selected = showOverdueOnly,
                            onClick = { showOverdueOnly = !showOverdueOnly },
                            label = {
                                Text(
                                    text = if (isAr) "المتأخرة فقط ($overdueCount)" else "Overdue Only ($overdueCount)",
                                    fontSize = 11.sp,
                                    color = if (showOverdueOnly) RestoRose600 else Color.Unspecified
                                )
                            },
                            modifier = Modifier.testTag("kds_filter_overdue_only")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (filteredLiveTickets.isEmpty()) {
                    RestoEmptyState(
                        title = if (isAr) "جميع تذاكر هذه المحطة مكتملة" else "All Station Tickets Cleared",
                        description = if (isAr) "الخط جاهز لاستقبال الطلبات الفورية من الكاشير أو النادل أو رمز QR." else "The kitchen station line is caught up. Incoming POS, Waiter, and QR orders appear automatically via the real-time event bus.",
                        icon = Icons.Default.OutdoorGrill
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 290.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredLiveTickets, key = { it.id }) { ticket ->
                            KdsTicketCard(
                                ticket = ticket,
                                nowMillis = nowMillis,
                                language = language,
                                onTransitionTicket = onTransitionTicket
                            )
                        }
                    }
                }
            }

            KdsMainTab.MULTI_SCREEN -> {
                MultiKdsCoexistenceView(
                    tickets = tickets,
                    stations = stations,
                    nowMillis = nowMillis,
                    language = language,
                    onTransitionTicket = onTransitionTicket,
                    modifier = Modifier.fillMaxSize()
                )
            }

            KdsMainTab.ROUTING_CONFIG -> {
                KitchenStationsRoutingAndStaffView(
                    branchId = branchId,
                    kitchens = kitchens,
                    preparationAreas = preparationAreas,
                    stations = stations,
                    routingRules = routingRules,
                    kitchenEmployees = kitchenEmployees,
                    categories = categories,
                    products = products,
                    language = language,
                    onSaveStation = onSaveStation,
                    onSaveRoutingRule = onSaveRoutingRule,
                    onDeleteRoutingRule = onDeleteRoutingRule,
                    onSaveKitchenEmployee = onSaveKitchenEmployee,
                    onToggleEmployeeDuty = onToggleEmployeeDuty,
                    modifier = Modifier.fillMaxSize()
                )
            }

            KdsMainTab.REALTIME_BUS -> {
                RealTimeEventBusStreamView(
                    events = realTimeEvents,
                    language = language,
                    modifier = Modifier.fillMaxSize()
                )
            }

            KdsMainTab.RECALL_HISTORY -> {
                if (completedAndCancelledTickets.isEmpty()) {
                    RestoEmptyState(
                        title = if (isAr) "لا توجد تذاكر مكتملة أو ملغاة بعد" else "No Completed or Cancelled Tickets Yet",
                        description = if (isAr) "التذاكر التي يتم تمريرها أو إلغاؤها ستظهر هنا لإمكانية الاسترجاع الفوري." else "Tickets bumped or cancelled from the preparation line appear here for instant recall.",
                        icon = Icons.Default.History
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 290.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(completedAndCancelledTickets, key = { it.id }) { ticket ->
                            KdsTicketCard(
                                ticket = ticket,
                                nowMillis = nowMillis,
                                language = language,
                                onTransitionTicket = onTransitionTicket
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KdsTicketCard(
    ticket: KitchenTicket,
    nowMillis: Long,
    language: AppLanguage,
    onTransitionTicket: (ticketId: String, targetStatus: KdsTicketStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = language == AppLanguage.ARABIC
    val elapsedSeconds = ticket.computeElapsedSeconds(nowMillis)
    val timerFormatted = KitchenEngine.formatElapsedTimer(elapsedSeconds)
    val isOverdue = ticket.isOverdue(nowMillis)
    val isWarning = ticket.isWarning(nowMillis)

    val (statusBadgeVariant, cardBorderColor) = when {
        isOverdue -> BadgeVariant.DANGER to RestoRose600
        isWarning -> BadgeVariant.WARNING to RestoAmber500
        ticket.ticketStatus == KdsTicketStatus.READY -> BadgeVariant.SUCCESS to RestoEmerald600
        ticket.ticketStatus == KdsTicketStatus.PREPARING -> BadgeVariant.WARNING to RestoAmber500
        ticket.ticketStatus == KdsTicketStatus.ACCEPTED -> BadgeVariant.INFO to RestoBlue600
        ticket.ticketStatus == KdsTicketStatus.COMPLETED -> BadgeVariant.NEUTRAL to MaterialTheme.colorScheme.outlineVariant
        ticket.ticketStatus == KdsTicketStatus.CANCELLED -> BadgeVariant.DANGER to RestoRose600
        else -> BadgeVariant.INFO to MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("kitchen_ticket_${ticket.orderNumber}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverdue) RestoRose600.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(if (isOverdue) 2.dp else 1.5.dp, cardBorderColor.copy(alpha = 0.75f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Row 1: Order Number, Priority Badge & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = ticket.orderNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    if (ticket.priority != TicketPriority.NORMAL || ticket.isPriority) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (ticket.priority == TicketPriority.VIP_RUSH || ticket.isPriority) RestoRose600 else RestoAmber500
                        ) {
                            Text(
                                text = if (ticket.priority == TicketPriority.VIP_RUSH || ticket.isPriority) "VIP / RUSH" else "HIGH",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                RestoBadge(
                    text = if (isAr) ticket.ticketStatus.titleAr else ticket.ticketStatus.code,
                    variant = statusBadgeVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Order Type, Table, Channel & Live Timer Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val orderTypeLabel = buildString {
                    append(if (isAr) ticket.orderType.titleAr else ticket.orderType.titleEn)
                    if (!ticket.tableNumber.isNullOrBlank()) {
                        append(" • ${ticket.tableNumber}")
                    }
                    append(" (${ticket.channel.codeForDisplay()})")
                }
                Text(
                    text = orderTypeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Live Elapsed Timer with Overdue / Warning Indicator
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        isOverdue -> RestoRose600
                        isWarning -> RestoAmber500.copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = if (isOverdue) Icons.Default.WarningAmber else Icons.Default.AccessTime,
                            contentDescription = "Elapsed Time",
                            tint = if (isOverdue) Color.White else if (isWarning) RestoAmber500 else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = buildString {
                                append(timerFormatted)
                                if (isOverdue) append(" OVERDUE!")
                                else if (isWarning) append(" WARN")
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isOverdue) Color.White else if (isWarning) RestoAmber500 else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 3: Station Badge & Assigned Chef
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Station: ${if (isAr) ticket.stationType.titleAr else ticket.stationName}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!ticket.assignedChefName.isNullOrBlank()) {
                    Text(
                        text = ticket.assignedChefName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Row 4: Structured Ticket Items (or fallback itemsSummary)
            if (ticket.items.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ticket.items.forEach { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${item.quantity}x ${if (isAr) item.productNameAr else item.productNameEn}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!item.variantName.isNullOrBlank()) {
                                    Text(
                                        text = item.variantName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (item.modifiersSummary.isNotBlank()) {
                                Text(
                                    text = "Mods: ${item.modifiersSummary}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (item.addonsSummary.isNotBlank()) {
                                Text(
                                    text = "Add-ons: +${item.addonsSummary}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RestoEmerald600
                                )
                            }
                            if (item.notes.isNotBlank()) {
                                Text(
                                    text = "Note: ${item.notes}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = RestoAmber500
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = ticket.itemsSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Order-level Notes Banner
            if (ticket.orderNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = RestoAmber500.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, RestoAmber500.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Order Note: ${ticket.orderNotes}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = RestoAmber500,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 5: Controlled KDS Workflow Action Buttons
            // Statuses: NEW -> ACCEPTED -> PREPARING -> READY -> COMPLETED / CANCELLED
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (ticket.ticketStatus) {
                    KdsTicketStatus.NEW -> {
                        RestoButton(
                            onClick = { onTransitionTicket(ticket.id, KdsTicketStatus.ACCEPTED) },
                            variant = RestoButtonVariant.SECONDARY,
                            modifier = Modifier.weight(1f).testTag("kds_btn_accept_${ticket.id}")
                        ) {
                            Text(Translations.get("kds_accept_ticket", language))
                        }
                        RestoButton(
                            onClick = { onTransitionTicket(ticket.id, KdsTicketStatus.PREPARING) },
                            variant = RestoButtonVariant.PRIMARY,
                            modifier = Modifier.weight(1f).testTag("kds_btn_prep_${ticket.id}")
                        ) {
                            Text(Translations.get("kds_mark_cooking", language))
                        }
                        IconButton(
                            onClick = { onTransitionTicket(ticket.id, KdsTicketStatus.CANCELLED) },
                            modifier = Modifier.size(36.dp).testTag("kds_btn_cancel_${ticket.id}")
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = "Cancel Ticket", tint = RestoRose600)
                        }
                    }

                    KdsTicketStatus.ACCEPTED -> {
                        RestoButton(
                            onClick = { onTransitionTicket(ticket.id, KdsTicketStatus.PREPARING) },
                            variant = RestoButtonVariant.SECONDARY,
                            modifier = Modifier.weight(1f).testTag("kds_btn_prep_${ticket.id}")
                        ) {
                            Text(Translations.get("kds_mark_cooking", language))
                        }
                        RestoButton(
                            onClick = { onTransitionTicket(ticket.id, KdsTicketStatus.READY) },
                            variant = RestoButtonVariant.PRIMARY,
                            modifier = Modifier.weight(1f).testTag("kds_btn_ready_${ticket.id}")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Translations.get("kds_mark_ready", language))
                        }
                    }

                    KdsTicketStatus.PREPARING -> {
                        RestoButton(
                            onClick = { onTransitionTicket(ticket.id, KdsTicketStatus.READY) },
                            variant = RestoButtonVariant.PRIMARY,
                            modifier = Modifier.weight(1f).testTag("kds_btn_ready_${ticket.id}")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Translations.get("kds_mark_ready", language))
                        }
                        IconButton(
                            onClick = { onTransitionTicket(ticket.id, KdsTicketStatus.CANCELLED) },
                            modifier = Modifier.size(36.dp).testTag("kds_btn_cancel_${ticket.id}")
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = "Cancel Ticket", tint = RestoRose600)
                        }
                    }

                    KdsTicketStatus.READY -> {
                        RestoButton(
                            onClick = { onTransitionTicket(ticket.id, KdsTicketStatus.COMPLETED) },
                            variant = RestoButtonVariant.PRIMARY,
                            modifier = Modifier.weight(1f).testTag("kds_btn_complete_${ticket.id}")
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Translations.get("kds_bump_complete", language))
                        }
                        RestoButton(
                            onClick = { onTransitionTicket(ticket.id, KdsTicketStatus.PREPARING) },
                            variant = RestoButtonVariant.OUTLINE,
                            modifier = Modifier.testTag("kds_btn_recall_${ticket.id}")
                        ) {
                            Text(if (isAr) "إعادة" else "Recall")
                        }
                    }

                    KdsTicketStatus.COMPLETED, KdsTicketStatus.CANCELLED -> {
                        RestoButton(
                            onClick = { onTransitionTicket(ticket.id, KdsTicketStatus.PREPARING) },
                            variant = RestoButtonVariant.SECONDARY,
                            modifier = Modifier.fillMaxWidth().testTag("kds_btn_recall_${ticket.id}")
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Translations.get("kds_recall_ticket", language))
                        }
                    }
                }
            }
        }
    }
}

private fun SalesChannel.codeForDisplay(): String = when (this) {
    SalesChannel.POS -> "POS"
    SalesChannel.WAITER_APP -> "Waiter"
    SalesChannel.QR_TABLE -> "QR"
    SalesChannel.CUSTOMER_WEB -> "Web"
    SalesChannel.CUSTOMER_APP -> "App"
    SalesChannel.DELIVERY_PARTNERS -> "Delivery"
}
