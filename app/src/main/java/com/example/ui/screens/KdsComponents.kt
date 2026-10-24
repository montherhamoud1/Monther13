package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.engine.KitchenEngine
import com.example.core.i18n.AppLanguage
import com.example.designsystem.*
import com.example.domain.model.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Tab 2: Coexisting Multi-KDS Screens View.
 * Allows two independent KDS station terminals (e.g. Terminal A: Grill/Hot Line & Terminal B: Expediter/Packing Pass)
 * to operate simultaneously side-by-side on the same reactive stream without polling.
 */
@Composable
fun MultiKdsCoexistenceView(
    tickets: List<KitchenTicket>,
    stations: List<KitchenStation>,
    nowMillis: Long,
    language: AppLanguage,
    onTransitionTicket: (String, KdsTicketStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = language == AppLanguage.ARABIC
    var terminalAStationType by remember { mutableStateOf<KitchenStationType?>(KitchenStationType.GRILL) }
    var terminalBStationType by remember { mutableStateOf<KitchenStationType?>(KitchenStationType.EXPEDITER) }

    val activeTickets = remember(tickets) {
        tickets.filter { it.ticketStatus !in setOf(KdsTicketStatus.COMPLETED, KdsTicketStatus.CANCELLED) }
    }

    val terminalATickets = remember(activeTickets, terminalAStationType) {
        if (terminalAStationType == null) activeTickets
        else activeTickets.filter { it.stationType == terminalAStationType }
    }

    val terminalBTickets = remember(activeTickets, terminalBStationType) {
        if (terminalBStationType == null) activeTickets
        else activeTickets.filter { it.stationType == terminalBStationType }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag("multi_kds_coexistence_view")) {
        val isWide = maxWidth >= 720.dp
        if (isWide) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                KdsIndependentTerminalPane(
                    terminalTitle = if (isAr) "شاشة المطبخ (A) - خط التحضير" else "KDS Terminal A • Prep Line",
                    terminalCode = "KDS-TERM-01",
                    selectedStationType = terminalAStationType,
                    onSelectStationType = { terminalAStationType = it },
                    tickets = terminalATickets,
                    stations = stations,
                    nowMillis = nowMillis,
                    language = language,
                    onTransitionTicket = onTransitionTicket,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                KdsIndependentTerminalPane(
                    terminalTitle = if (isAr) "شاشة المطبخ (B) - التجميع والتغليف" else "KDS Terminal B • Expediter / Packing Pass",
                    terminalCode = "KDS-TERM-02",
                    selectedStationType = terminalBStationType,
                    onSelectStationType = { terminalBStationType = it },
                    tickets = terminalBTickets,
                    stations = stations,
                    nowMillis = nowMillis,
                    language = language,
                    onTransitionTicket = onTransitionTicket,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KdsIndependentTerminalPane(
                    terminalTitle = if (isAr) "شاشة المطبخ (A) - خط التحضير" else "KDS Terminal A • Prep Line",
                    terminalCode = "KDS-TERM-01",
                    selectedStationType = terminalAStationType,
                    onSelectStationType = { terminalAStationType = it },
                    tickets = terminalATickets,
                    stations = stations,
                    nowMillis = nowMillis,
                    language = language,
                    onTransitionTicket = onTransitionTicket,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
                KdsIndependentTerminalPane(
                    terminalTitle = if (isAr) "شاشة المطبخ (B) - التجميع والتغليف" else "KDS Terminal B • Expediter / Packing Pass",
                    terminalCode = "KDS-TERM-02",
                    selectedStationType = terminalBStationType,
                    onSelectStationType = { terminalBStationType = it },
                    tickets = terminalBTickets,
                    stations = stations,
                    nowMillis = nowMillis,
                    language = language,
                    onTransitionTicket = onTransitionTicket,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun KdsIndependentTerminalPane(
    terminalTitle: String,
    terminalCode: String,
    selectedStationType: KitchenStationType?,
    onSelectStationType: (KitchenStationType?) -> Unit,
    tickets: List<KitchenTicket>,
    stations: List<KitchenStation>,
    nowMillis: Long,
    language: AppLanguage,
    onTransitionTicket: (String, KdsTicketStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = language == AppLanguage.ARABIC
    Card(
        modifier = modifier.testTag("kds_terminal_pane_$terminalCode"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(RestoEmerald600)
                        )
                        Text(
                            text = terminalTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "$terminalCode • ${tickets.size} active ticket(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                RestoBadge(
                    text = selectedStationType?.let { if (isAr) it.titleAr else it.titleEn } ?: if (isAr) "جميع المحطات" else "All Stations",
                    variant = BadgeVariant.INFO
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Station Selector Chips for this Terminal
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = selectedStationType == null,
                        onClick = { onSelectStationType(null) },
                        label = { Text(if (isAr) "الكل" else "All", fontSize = 11.sp) }
                    )
                }
                items(KitchenStationType.entries) { stType ->
                    FilterChip(
                        selected = selectedStationType == stType,
                        onClick = { onSelectStationType(stType) },
                        label = { Text(if (isAr) stType.titleAr else stType.titleEn, fontSize = 11.sp) },
                        modifier = Modifier.testTag("${terminalCode}_chip_${stType.code}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            if (tickets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isAr) "لا توجد تذاكر نشطة لهذه المحطة" else "No active tickets on this station screen",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(tickets, key = { "${terminalCode}_${it.id}" }) { ticket ->
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

/**
 * Tab 3: Kitchens, Stations, Timer Thresholds, Product Routing & Kitchen Employees Configuration.
 */
@Composable
fun KitchenStationsRoutingAndStaffView(
    branchId: String,
    kitchens: List<Kitchen>,
    preparationAreas: List<PreparationArea>,
    stations: List<KitchenStation>,
    routingRules: List<StationRoutingRule>,
    kitchenEmployees: List<KitchenEmployeeAssignment>,
    categories: List<Category>,
    products: List<Product>,
    language: AppLanguage,
    onSaveStation: (KitchenStation) -> Unit,
    onSaveRoutingRule: (StationRoutingRule) -> Unit,
    onDeleteRoutingRule: (String) -> Unit,
    onSaveKitchenEmployee: (KitchenEmployeeAssignment) -> Unit,
    onToggleEmployeeDuty: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = language == AppLanguage.ARABIC
    var editingStation by remember { mutableStateOf<KitchenStation?>(null) }
    var showAddRoutingDialog by remember { mutableStateOf(false) }
    var showAddChefDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("kds_routing_config_view"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Kitchens & Preparation Areas Overview
        item {
            RestoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isAr) "المطابخ ومناطق التحضير النشطة" else "Kitchens & Preparation Areas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(kitchens) { kit ->
                            val areasCount = preparationAreas.count { it.kitchenId == kit.id }
                            val stCount = stations.count { it.kitchenId == kit.id }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp).widthIn(min = 210.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (isAr) kit.nameAr else kit.nameEn,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        RestoBadge(text = kit.code, variant = BadgeVariant.INFO)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$areasCount Prep Area(s) • $stCount Station(s) • SLA: ${kit.defaultWarningMinutes}m / ${kit.defaultOverdueMinutes}m",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Kitchen Stations & Configurable Timer Thresholds
        item {
            RestoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isAr) "محطات المطبخ ومؤقتات التأخير (SLA)" else "Kitchen Stations & Overdue Timer Thresholds",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isAr) "اضغط على أي محطة لتعديل حدود وقت التحذير والتأخير" else "Tap any station to configure warning and overdue SLA thresholds",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    stations.forEach { st ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { editingStation = st }
                                .testTag("station_config_row_${st.code}"),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = if (isAr) st.nameAr else st.nameEn,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        RestoBadge(text = st.code, variant = BadgeVariant.NEUTRAL)
                                    }
                                    Text(
                                        text = "${st.prepAreaName} • Type: ${st.stationType.code}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RestoBadge(
                                        text = "Warn: ${st.warningThresholdMinutes}m",
                                        variant = BadgeVariant.WARNING
                                    )
                                    RestoBadge(
                                        text = "Overdue: ${st.overdueThresholdMinutes}m",
                                        variant = BadgeVariant.DANGER
                                    )
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Station Thresholds", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Product & Category Automatic Station Routing Rules
        item {
            RestoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isAr) "قواعد التوجيه التلقائي للأصناف نحو المحطات" else "Automatic Product & Category Station Routing",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isAr) "توجيه الطلبات تلقائياً إلى الشواء، البيتزا، القلي، المشروبات، الحلويات، والتجميع" else "Routes POS, Waiter, and QR items automatically to their target preparation station",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RestoButton(
                            onClick = { showAddRoutingDialog = true },
                            variant = RestoButtonVariant.PRIMARY,
                            modifier = Modifier.testTag("add_routing_rule_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isAr) "قاعدة توجيه" else "Add Rule")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    routingRules.forEach { rule ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isAr) rule.nameAr else rule.nameEn,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val catName = categories.find { it.id == rule.categoryId }?.nameEn
                                    val prodName = products.find { it.id == rule.productId }?.nameEn
                                    Text(
                                        text = buildString {
                                            if (prodName != null) append("Product: $prodName • ")
                                            if (catName != null) append("Category: $catName • ")
                                            append("Target: ${rule.targetStationName} (${rule.targetStationType.code})")
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    RestoBadge(text = rule.targetStationType.code, variant = BadgeVariant.SUCCESS)
                                    IconButton(onClick = { onDeleteRoutingRule(rule.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Rule", tint = RestoRose600)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Kitchen Employees & Station Duty Roster
        item {
            RestoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isAr) "طهاة وموظفو المطبخ المناوبون" else "Kitchen Employees & Station Roster",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isAr) "توزيع الطهاة على محطات التحضير والتغليف والتجميع" else "Chefs and kitchen staff assigned to active stations",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RestoButton(
                            onClick = { showAddChefDialog = true },
                            variant = RestoButtonVariant.SECONDARY,
                            modifier = Modifier.testTag("assign_kitchen_staff_btn")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isAr) "تعيين طاهٍ" else "Assign Staff")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    kitchenEmployees.forEach { emp ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = emp.employeeName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${emp.roleTitle} • ${emp.stationName} • ${emp.ticketsCompletedCount} tickets completed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    RestoBadge(
                                        text = if (emp.isOnDuty) (if (isAr) "مناوب" else "ON DUTY") else (if (isAr) "خارج النوبة" else "OFF DUTY"),
                                        variant = if (emp.isOnDuty) BadgeVariant.SUCCESS else BadgeVariant.NEUTRAL
                                    )
                                    Switch(
                                        checked = emp.isOnDuty,
                                        onCheckedChange = { onToggleEmployeeDuty(emp.id, it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Station SLA Edit Dialog
    editingStation?.let { st ->
        var warnText by remember(st) { mutableStateOf(st.warningThresholdMinutes.toString()) }
        var overdueText by remember(st) { mutableStateOf(st.overdueThresholdMinutes.toString()) }
        Dialog(onDismissRequest = { editingStation = null }) {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isAr) "تعديل مؤقتات المحطة: ${st.nameAr}" else "Configure SLA Timers: ${st.nameEn}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = warnText,
                        onValueChange = { warnText = it },
                        label = { Text(if (isAr) "عتبة التحذير (دقائق)" else "Warning Threshold (Minutes)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = overdueText,
                        onValueChange = { overdueText = it },
                        label = { Text(if (isAr) "عتبة التأخير القصوى (دقائق)" else "Overdue Threshold (Minutes)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { editingStation = null }) {
                            Text(if (isAr) "إلغاء" else "Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        RestoButton(
                            onClick = {
                                val w = warnText.toIntOrNull()?.coerceAtLeast(1) ?: st.warningThresholdMinutes
                                val o = overdueText.toIntOrNull()?.coerceAtLeast(w + 1) ?: st.overdueThresholdMinutes
                                onSaveStation(st.copy(warningThresholdMinutes = w, overdueThresholdMinutes = o))
                                editingStation = null
                            }
                        ) {
                            Text(if (isAr) "حفظ" else "Save Thresholds")
                        }
                    }
                }
            }
        }
    }

    // Add Routing Rule Dialog
    if (showAddRoutingDialog) {
        var selectedCategory by remember { mutableStateOf(categories.firstOrNull()) }
        var selectedStation by remember { mutableStateOf(stations.firstOrNull()) }
        Dialog(onDismissRequest = { showAddRoutingDialog = false }) {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isAr) "إضافة قاعدة توجيه للمحطة" else "New Station Routing Rule",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isAr) "اختر التصنيف:" else "Select Menu Category:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategory?.id == cat.id,
                                onClick = { selectedCategory = cat },
                                label = { Text(if (isAr) cat.nameAr else cat.nameEn, fontSize = 11.sp) }
                            )
                        }
                    }
                    Text(
                        text = if (isAr) "اختر محطة التحضير المستهدفة:" else "Select Target Kitchen Station:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(stations) { st ->
                            FilterChip(
                                selected = selectedStation?.id == st.id,
                                onClick = { selectedStation = st },
                                label = { Text(if (isAr) st.nameAr else st.nameEn, fontSize = 11.sp) }
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddRoutingDialog = false }) {
                            Text(if (isAr) "إلغاء" else "Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        RestoButton(
                            onClick = {
                                val cat = selectedCategory
                                val st = selectedStation
                                if (cat != null && st != null) {
                                    onSaveRoutingRule(
                                        StationRoutingRule(
                                            id = "rr_${UUID.randomUUID().toString().take(6)}",
                                            branchId = branchId,
                                            nameEn = "${cat.nameEn} -> ${st.nameEn}",
                                            nameAr = "${cat.nameAr} -> ${st.nameAr}",
                                            categoryId = cat.id,
                                            targetStationId = st.id,
                                            targetStationType = st.stationType,
                                            targetStationName = st.nameEn
                                        )
                                    )
                                    showAddRoutingDialog = false
                                }
                            }
                        ) {
                            Text(if (isAr) "حفظ القاعدة" else "Save Rule")
                        }
                    }
                }
            }
        }
    }

    // Assign Kitchen Staff Dialog
    if (showAddChefDialog) {
        var chefName by remember { mutableStateOf("") }
        var roleTitle by remember { mutableStateOf("Station Line Chef") }
        var selectedStation by remember { mutableStateOf(stations.firstOrNull()) }
        Dialog(onDismissRequest = { showAddChefDialog = false }) {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isAr) "تعيين موظف مطبخ لمحطة" else "Assign Kitchen Employee to Station",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = chefName,
                        onValueChange = { chefName = it },
                        label = { Text(if (isAr) "اسم الطاهي / الموظف" else "Chef / Employee Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = roleTitle,
                        onValueChange = { roleTitle = it },
                        label = { Text(if (isAr) "المسمى الوظيفي" else "Kitchen Role Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(stations) { st ->
                            FilterChip(
                                selected = selectedStation?.id == st.id,
                                onClick = { selectedStation = st },
                                label = { Text(if (isAr) st.nameAr else st.nameEn, fontSize = 11.sp) }
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddChefDialog = false }) {
                            Text(if (isAr) "إلغاء" else "Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        RestoButton(
                            onClick = {
                                val st = selectedStation
                                if (chefName.isNotBlank() && st != null) {
                                    onSaveKitchenEmployee(
                                        KitchenEmployeeAssignment(
                                            id = "ke_${UUID.randomUUID().toString().take(6)}",
                                            branchId = branchId,
                                            employeeId = "emp_${UUID.randomUUID().toString().take(4)}",
                                            employeeName = chefName.trim(),
                                            roleTitle = roleTitle.ifBlank { "Station Chef" },
                                            kitchenId = st.kitchenId,
                                            stationId = st.id,
                                            stationName = st.nameEn,
                                            stationType = st.stationType,
                                            isOnDuty = true
                                        )
                                    )
                                    showAddChefDialog = false
                                }
                            }
                        ) {
                            Text(if (isAr) "تعيين" else "Assign")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 4: Real-Time Operational Event Bus Monitor.
 * Displays push events for New Orders, Order Status Changes, Table Changes, Kitchen Tickets, and Notifications.
 */
@Composable
fun RealTimeEventBusStreamView(
    events: List<RealTimeOperationalEvent>,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isAr = language == AppLanguage.ARABIC
    var selectedType by remember { mutableStateOf<RealTimeEventType?>(null) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    val filteredEvents = remember(events, selectedType) {
        if (selectedType == null) events else events.filter { it.type == selectedType }
    }

    Column(modifier = modifier.fillMaxSize().testTag("kds_realtime_event_bus_view")) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedType == null,
                    onClick = { selectedType = null },
                    label = { Text(if (isAr) "جميع الأحداث (${events.size})" else "All Push Events (${events.size})") }
                )
            }
            items(RealTimeEventType.entries) { evType ->
                val count = events.count { it.type == evType }
                FilterChip(
                    selected = selectedType == evType,
                    onClick = { selectedType = evType },
                    label = { Text("${if (isAr) evType.titleAr else evType.titleEn} ($count)") }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredEvents, key = { it.id }) { ev ->
                val badgeVariant = when (ev.type) {
                    RealTimeEventType.NEW_ORDER -> BadgeVariant.SUCCESS
                    RealTimeEventType.KITCHEN_TICKET_UPDATED -> BadgeVariant.WARNING
                    RealTimeEventType.ORDER_STATUS_CHANGED -> BadgeVariant.INFO
                    RealTimeEventType.TABLE_CHANGED -> BadgeVariant.NEUTRAL
                    RealTimeEventType.NOTIFICATION -> BadgeVariant.DANGER
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                RestoBadge(text = if (isAr) ev.type.titleAr else ev.type.titleEn, variant = badgeVariant)
                                Text(
                                    text = ev.referenceCode,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "• ${ev.sourceChannel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isAr) ev.titleAr else ev.titleEn,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isAr) ev.detailsAr else ev.detailsEn,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = timeFormatter.format(Date(ev.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
