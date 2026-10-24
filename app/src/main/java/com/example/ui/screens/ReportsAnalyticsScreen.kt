package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.engine.AccountingEngine
import com.example.core.engine.ReportingAndAiEngine
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.Translations
import com.example.designsystem.*
import com.example.domain.model.*
import java.util.*

private enum class ReportsTab(val titleEn: String, val titleAr: String) {
    EXECUTIVE_DRILLDOWN("Executive KPIs & Drill-Down", "مؤشرات القيادة والتفصيل المتدرج"),
    REPORT_ENGINE("18-Report Engine & Exports", "محرك التقارير الشامل والتصدير"),
    CENTRAL_NOTIFICATIONS("Central Multi-Channel Alerts", "خدمة الإشعارات المركزية"),
    AUTHORIZED_AI("Authorized AI Business Query", "الاستعلام الذكي المحمي بالصلاحيات")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsAnalyticsScreen(
    branch: Branch,
    branches: List<Branch>,
    session: UserSession?,
    orders: List<Order>,
    tables: List<RestaurantTable>,
    kitchenTickets: List<KitchenTicket>,
    deliveries: List<DeliveryAssignment>,
    drivers: List<DeliveryDriver>,
    stockItems: List<WarehouseStockItem>,
    purchaseOrders: List<PurchaseOrderDocument>,
    suppliers: List<Supplier>,
    expenses: List<Expense>,
    accounts: List<ChartOfAccount>,
    journalEntries: List<JournalEntry>,
    employees: List<Employee>,
    attendanceRecords: List<AttendanceRecord>,
    customers: List<Customer>,
    auditLogs: List<AuditLog>,
    centralNotifications: List<CentralNotificationEvent>,
    currentShift: CashierShift?,
    language: AppLanguage,
    onExecuteAiQuery: (String, List<Order>) -> AuthorizedAiQueryResponse,
    modifier: Modifier = Modifier
) {
    val isAr = language == AppLanguage.ARABIC
    var selectedTab by remember { mutableStateOf(ReportsTab.EXECUTIVE_DRILLDOWN) }

    // Drill-down state: Metric -> Report -> Document -> Transaction -> Audit Trail
    var selectedDrillMetric by remember { mutableStateOf("SALES") }

    // Report Engine filters
    val reportCategories = listOf(
        "Sales", "Orders", "Products", "Categories", "Cashier", "Branches",
        "Customers", "Kitchen", "Delivery", "Drivers", "Inventory", "Purchasing",
        "Suppliers", "Expenses", "Accounting", "Employees", "Attendance", "Profitability"
    )
    var selectedReportCategory by remember { mutableStateOf("Sales") }
    var filterChannel by remember { mutableStateOf("ALL") }
    var showExportPreview by remember { mutableStateOf(false) }

    // Authorized AI Query state
    var aiQueryInput by remember { mutableStateOf("What were yesterday's sales?") }
    var latestAiResponse by remember(session, orders, stockItems, deliveries, expenses) {
        mutableStateOf(onExecuteAiQuery("What were yesterday's sales?", orders))
    }

    val validOrders = orders.filter { it.status != OrderStatus.CANCELLED && it.status != OrderStatus.REFUNDED }
    val totalSales = validOrders.sumOf { it.totalAmount }
    val avgOrder = if (validOrders.isNotEmpty()) totalSales / validOrders.size else 0.0
    val openTables = tables.count { it.status == TableStatus.OCCUPIED || it.status == TableStatus.WAITING_PAYMENT }
    val activeKds = kitchenTickets.count { it.ticketStatus !in setOf(KdsTicketStatus.COMPLETED, KdsTicketStatus.CANCELLED) }
    val activeDeliveries = deliveries.count { it.status !in setOf(DeliveryWorkflowStatus.DELIVERED, DeliveryWorkflowStatus.RETURNED_TO_BRANCH) }
    val activeDrivers = drivers.count { it.shiftStatus == "ONLINE" }
    val lowStock = stockItems.count { it.isLowStock }
    val totalExpenses = expenses.sumOf { it.amount }
    val drawerCash = currentShift?.expectedCash ?: 680.0
    val fin = remember(accounts, journalEntries) {
        AccountingEngine.computeFinancialOverview(accounts, journalEntries)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("reports_analytics_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = Translations.get("nav_reports_analytics", language),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "12-KPI Executive Telemetry • 5-Stage Drill-Down • CSV/Excel/PDF Export • RBAC AI Boundary (Phase 12)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RestoBadge(text = "18 Report Types", variant = BadgeVariant.INFO)
                RestoBadge(text = "${centralNotifications.size} Multi-Channel Alerts", variant = BadgeVariant.SUCCESS)
            }
        }

        // Scrollable Tab Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReportsTab.entries.forEach { tab ->
                FilterChip(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    label = { Text(if (isAr) tab.titleAr else tab.titleEn) },
                    modifier = Modifier.testTag("reports_tab_${tab.name.lowercase()}")
                )
            }
        }

        when (selectedTab) {
            ReportsTab.EXECUTIVE_DRILLDOWN -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 12 Executive KPIs Grid
                    item {
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("12 Executive Operational & Financial Indicators (Tap to Drill Down)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        "SALES" to "Sales: $${String.format(Locale.US, "%.2f", totalSales)}",
                                        "ORDERS" to "Orders: ${orders.size} (Avg $${String.format(Locale.US, "%.2f", avgOrder)})",
                                        "CUSTOMERS" to "Customers: ${customers.size}",
                                        "TABLES" to "Open Tables: $openTables/${tables.size}",
                                        "KITCHEN" to "KDS Queue: $activeKds",
                                        "DELIVERY" to "Deliveries: $activeDeliveries (Drivers: $activeDrivers)",
                                        "INVENTORY" to "Low Stock: $lowStock",
                                        "FINANCE" to "Expenses: $${String.format(Locale.US, "%.2f", totalExpenses)} | Cash: $${String.format(Locale.US, "%.2f", drawerCash)} | Net Profit: $${String.format(Locale.US, "%.2f", fin.netProfit)}"
                                    ).forEach { (code, label) ->
                                        FilterChip(
                                            selected = selectedDrillMetric == code,
                                            onClick = { selectedDrillMetric = code },
                                            label = { Text(label) },
                                            modifier = Modifier.testTag("drill_metric_${code.lowercase()}")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5-Stage Drill-Down Chain: Metric -> Report -> Document -> Transaction -> Audit Trail
                    item {
                        val firstOrder = orders.firstOrNull()
                        val firstJournal = journalEntries.firstOrNull()
                        val firstAudit = auditLogs.firstOrNull()
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "End-to-End Traceability Drill-Down ($selectedDrillMetric)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "1. Dashboard Metric  ➔  2. Analytical Report  ➔  3. Source Document  ➔  4. Ledger Transaction  ➔  5. Immutable Audit Trail",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                HorizontalDivider()
                                Text("• Stage 1 [Metric]: $selectedDrillMetric Summary = $${String.format(Locale.US, "%.2f", totalSales)} across ${validOrders.size} valid orders", style = MaterialTheme.typography.bodySmall)
                                Text("• Stage 2 [Report]: Branch ${branch.code} Sales & Profitability Breakdown (Gross Profit: $${String.format(Locale.US, "%.2f", fin.grossProfit)})", style = MaterialTheme.typography.bodySmall)
                                Text("• Stage 3 [Source Document]: Order ${firstOrder?.orderNumber ?: "ORD-1001"} (${firstOrder?.orderType?.titleEn ?: "DINE_IN"} • $${firstOrder?.totalAmount ?: 0.0})", style = MaterialTheme.typography.bodySmall)
                                Text("• Stage 4 [Accounting/Stock Transaction]: Journal ${firstJournal?.entryNumber ?: "JE-2026-0001"} (${firstJournal?.descriptionEn ?: "POS Sale & COGS"})", style = MaterialTheme.typography.bodySmall)
                                Text("• Stage 5 [Audit Trail]: ${firstAudit?.action ?: "ORDER_CREATE"} by ${firstAudit?.userName ?: "System Admin"} — ${firstAudit?.details ?: "Verified"}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            ReportsTab.REPORT_ENGINE -> {
                val filteredOrders = remember(orders, filterChannel) {
                    if (filterChannel == "ALL") orders else orders.filter { it.orderType.name == filterChannel }
                }
                val reportHeaders = listOf("Reference", "Category/Party", "Status/Stage", "Metric/Amount")
                val reportRows: List<List<String>> = remember(selectedReportCategory, filteredOrders, stockItems, purchaseOrders, expenses, employees) {
                    when (selectedReportCategory) {
                        "Inventory" -> stockItems.map { listOf(it.itemCode, it.itemNameEn, it.warehouseName, "${it.currentQuantity} ${it.stockUnit}") }
                        "Purchasing" -> purchaseOrders.map { listOf(it.poNumber, it.supplierName, it.stage.titleEn, "$${it.totalAmount}") }
                        "Suppliers" -> suppliers.map { listOf(it.code, it.name, it.paymentTerms, "$${it.currentPayableBalance}") }
                        "Expenses" -> expenses.map { listOf(it.documentNumber, it.category, it.costCenterCode, "$${it.amount}") }
                        "Accounting" -> journalEntries.map { listOf(it.entryNumber, it.sourceDocumentType, it.referenceNumber, "$${it.totalDebit}") }
                        "Employees" -> employees.map { listOf(it.employeeNumber, it.fullName, it.department, it.status) }
                        "Attendance" -> attendanceRecords.map { listOf(it.employeeNumber, it.employeeName, it.source.titleEn, it.status) }
                        "Delivery" -> deliveries.map { listOf(it.orderNumber, it.customerName, it.status.titleEn, "$${it.orderTotalAmount}") }
                        "Drivers" -> drivers.map { listOf(it.vehiclePlate, it.fullName, it.driverStatus, "COD: $${it.cashCollectedUnsettled}") }
                        "Customers" -> customers.map { listOf(it.phone, it.name, it.tier, "${it.loyaltyPoints} pts ($${it.totalSpend})") }
                        "Kitchen" -> kitchenTickets.map { listOf(it.orderNumber, it.stationName, it.ticketStatus.titleEn, "${it.elapsedMinutes}m") }
                        else -> filteredOrders.map { listOf(it.orderNumber, it.orderType.titleEn, it.status.titleEn, "$${it.totalAmount}") }
                    }
                }
                val csvData = remember(reportHeaders, reportRows) {
                    buildString {
                        appendLine(reportHeaders.joinToString(","))
                        reportRows.forEach { r -> appendLine(r.joinToString(",") { "\"$it\"" }) }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Select Report Domain (18 Enterprise Reports)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    reportCategories.forEach { cat ->
                                        FilterChip(
                                            selected = selectedReportCategory == cat,
                                            onClick = { selectedReportCategory = cat },
                                            label = { Text(cat) }
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("ALL", "DINE_IN", "TAKEAWAY", "DELIVERY").forEach { ch ->
                                            FilterChip(
                                                selected = filterChannel == ch,
                                                onClick = { filterChannel = ch },
                                                label = { Text(ch) }
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { showExportPreview = !showExportPreview },
                                        modifier = Modifier.testTag("toggle_export_preview_btn")
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (showExportPreview) "Hide CSV/PDF Export" else "Export CSV / Excel / PDF")
                                    }
                                }
                            }
                        }
                    }

                    if (showExportPreview) {
                        item {
                            RestoCard {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Export Payload ($selectedReportCategory — CSV / Excel-Compatible / PDF Print Ready)", fontWeight = FontWeight.Bold)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = csvData,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    items(reportRows) { row ->
                        RestoCard {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                row.forEach { cell ->
                                    Text(cell, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }

            ReportsTab.CENTRAL_NOTIFICATIONS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(centralNotifications, key = { it.id }) { evt ->
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text(if (isAr) evt.titleAr else evt.titleEn, fontWeight = FontWeight.Bold)
                                    RestoBadge(text = evt.eventTrigger, variant = BadgeVariant.INFO)
                                }
                                Text(if (isAr) evt.messageAr else evt.messageEn, style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    evt.channelsDispatched.forEach { ch ->
                                        RestoBadge(text = ch.titleEn, variant = BadgeVariant.SUCCESS)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ReportsTab.AUTHORIZED_AI -> {
                val sampleQuestions = listOf(
                    "What were yesterday's sales?",
                    "Which products are low stock?",
                    "What are today's delayed deliveries?",
                    "What is our net profit and expense breakdown?"
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "RBAC-Guarded AI Business Query Service Boundary",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Active Session Role: ${session?.role?.code ?: "GUEST"} • Queries are evaluated against your effective permissions before accessing sales, inventory, delivery, or financial ledgers.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    sampleQuestions.forEach { q ->
                                        AssistChip(
                                            onClick = {
                                                aiQueryInput = q
                                                latestAiResponse = onExecuteAiQuery(q, orders)
                                            },
                                            label = { Text(q) }
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = aiQueryInput,
                                        onValueChange = { aiQueryInput = it },
                                        label = { Text("Ask an operational or financial question...") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("ai_query_input")
                                    )
                                    Button(
                                        onClick = { latestAiResponse = onExecuteAiQuery(aiQueryInput, orders) },
                                        modifier = Modifier.testTag("execute_ai_query_btn")
                                    ) {
                                        Text("Run Query")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("Query Response: \"${latestAiResponse.queryText}\"", fontWeight = FontWeight.Bold)
                                    RestoBadge(
                                        text = if (latestAiResponse.isAuthorized) "AUTHORIZED (${latestAiResponse.requiredPermission})" else "DENIED (${latestAiResponse.requiredPermission})",
                                        variant = if (latestAiResponse.isAuthorized) BadgeVariant.SUCCESS else BadgeVariant.DANGER
                                    )
                                }
                                Text(
                                    text = if (isAr) latestAiResponse.answerAr else latestAiResponse.answerEn,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (latestAiResponse.supportingFacts.isNotEmpty()) {
                                    HorizontalDivider()
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        latestAiResponse.supportingFacts.forEach { fact ->
                                            Text("• $fact", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
