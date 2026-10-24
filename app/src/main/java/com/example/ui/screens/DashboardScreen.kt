package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.constants.NavigationSection
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.Translations
import com.example.designsystem.*
import com.example.domain.model.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    company: Company,
    branch: Branch,
    orders: List<Order>,
    tables: List<RestaurantTable>,
    language: AppLanguage,
    onNavigate: (NavigationSection) -> Unit,
    customersCount: Int = 3,
    kitchenQueueCount: Int = 3,
    deliveryOrdersCount: Int = 2,
    activeDriversCount: Int = 2,
    lowStockCount: Int = 2,
    expensesTotal: Double = 605.0,
    cashInDrawer: Double = 680.0,
    netProfitEstimate: Double = 6400.0,
    modifier: Modifier = Modifier
) {
    val validOrders = orders.filter { it.status.name != "CANCELLED" && it.status.name != "REFUNDED" }
    val totalSales = validOrders.sumOf { it.totalAmount }
    val avgOrderValue = if (validOrders.isNotEmpty()) totalSales / validOrders.size else 0.0
    val occupiedTablesCount = tables.count { it.status.name == "OCCUPIED" || it.status.name == "WAITING_PAYMENT" }
    val activeOrdersCount = orders.count { it.status.name != "COMPLETED" && it.status.name != "CANCELLED" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Breadcrumb
        item {
            RestoBreadcrumb(
                items = listOf(
                    BreadcrumbItem(company.nameEn),
                    BreadcrumbItem(if (language == AppLanguage.ARABIC) branch.nameAr else branch.nameEn),
                    BreadcrumbItem(Translations.get("nav_dashboard", language))
                )
            )
        }

        // Executive Title & Quick Action Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = Translations.get("nav_dashboard", language),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enterprise 12-Module Real-Time Command Center • POS, KDS, CRM, Delivery, Inventory, Finance, HR & Analytics",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                RestoButton(
                    onClick = { onNavigate(NavigationSection.POS) },
                    variant = RestoButtonVariant.PRIMARY,
                    testTag = "dashboard_new_order_button"
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Translations.get("nav_pos", language))
                }
            }
        }

        // 12 Executive KPI Cards Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RestoKpiCard(
                        title = Translations.get("kpi_revenue", language),
                        value = "$${String.format(Locale.US, "%.2f", totalSales)}",
                        subtitle = "Avg Ticket: $${String.format(Locale.US, "%.2f", avgOrderValue)}",
                        icon = Icons.Default.AttachMoney,
                        trendPositive = true,
                        trendLabel = "+14.8%",
                        modifier = Modifier.weight(1f),
                        testTag = "kpi_revenue"
                    )
                    RestoKpiCard(
                        title = Translations.get("kpi_orders", language),
                        value = "$activeOrdersCount / ${orders.size}",
                        subtitle = "$customersCount CRM Customers",
                        icon = Icons.Default.ReceiptLong,
                        trendPositive = true,
                        trendLabel = "+8",
                        modifier = Modifier.weight(1f),
                        testTag = "kpi_orders"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RestoKpiCard(
                        title = Translations.get("kpi_tables_occupied", language),
                        value = "$occupiedTablesCount / ${tables.size}",
                        subtitle = "KDS Queue: $kitchenQueueCount tickets",
                        icon = Icons.Default.TableBar,
                        modifier = Modifier.weight(1f),
                        testTag = "kpi_tables"
                    )
                    RestoKpiCard(
                        title = "Deliveries & Drivers",
                        value = "$deliveryOrdersCount Active",
                        subtitle = "$activeDriversCount Drivers Online • $lowStockCount Low Stock",
                        icon = Icons.Default.DeliveryDining,
                        trendPositive = true,
                        trendLabel = "On Pace",
                        modifier = Modifier.weight(1f),
                        testTag = "kpi_prep"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RestoKpiCard(
                        title = "Cash & Expenses",
                        value = "$${String.format(Locale.US, "%.2f", cashInDrawer)}",
                        subtitle = "Expenses: $${String.format(Locale.US, "%.2f", expensesTotal)}",
                        icon = Icons.Default.AccountBalanceWallet,
                        modifier = Modifier.weight(1f),
                        testTag = "kpi_cash_expenses"
                    )
                    RestoKpiCard(
                        title = "Net Profit Indicator",
                        value = "$${String.format(Locale.US, "%.2f", netProfitEstimate)}",
                        subtitle = "Double-Entry GL Verified",
                        icon = Icons.Default.TrendingUp,
                        trendPositive = netProfitEstimate >= 0,
                        trendLabel = "Healthy",
                        modifier = Modifier.weight(1f),
                        testTag = "kpi_profit"
                    )
                }
            }
        }

        // Operational Shortcuts across all 12 Phases
        item {
            Text(
                text = Translations.get("quick_actions", language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RestoButton(
                    onClick = { onNavigate(NavigationSection.TABLES) },
                    variant = RestoButtonVariant.OUTLINE
                ) {
                    Icon(Icons.Default.TableRestaurant, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Translations.get("nav_tables", language))
                }
                RestoButton(
                    onClick = { onNavigate(NavigationSection.KITCHEN) },
                    variant = RestoButtonVariant.OUTLINE
                ) {
                    Icon(Icons.Default.Kitchen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Translations.get("nav_kitchen", language))
                }
                RestoButton(
                    onClick = { onNavigate(NavigationSection.CUSTOMER_PORTAL) },
                    variant = RestoButtonVariant.OUTLINE,
                    testTag = "dash_shortcut_customer"
                ) {
                    Icon(Icons.Default.PhoneIphone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Translations.get("nav_customer_portal", language))
                }
                RestoButton(
                    onClick = { onNavigate(NavigationSection.DELIVERY_DISPATCH) },
                    variant = RestoButtonVariant.OUTLINE,
                    testTag = "dash_shortcut_delivery"
                ) {
                    Icon(Icons.Default.DeliveryDining, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Translations.get("nav_delivery_dispatch", language))
                }
                RestoButton(
                    onClick = { onNavigate(NavigationSection.INVENTORY) },
                    variant = RestoButtonVariant.OUTLINE
                ) {
                    Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Translations.get("nav_inventory", language))
                }
                RestoButton(
                    onClick = { onNavigate(NavigationSection.ACCOUNTING) },
                    variant = RestoButtonVariant.OUTLINE,
                    testTag = "dash_shortcut_accounting"
                ) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Translations.get("nav_accounting", language))
                }
                RestoButton(
                    onClick = { onNavigate(NavigationSection.HR_PAYROLL) },
                    variant = RestoButtonVariant.OUTLINE,
                    testTag = "dash_shortcut_hr"
                ) {
                    Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Translations.get("nav_hr_payroll", language))
                }
                RestoButton(
                    onClick = { onNavigate(NavigationSection.REPORTS_ANALYTICS) },
                    variant = RestoButtonVariant.OUTLINE,
                    testTag = "dash_shortcut_reports"
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Translations.get("nav_reports_analytics", language))
                }
            }
        }

        // Live Operations Stream (Recent Orders Table)
        item {
            Text(
                text = Translations.get("recent_orders_title", language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            val columns = listOf(
                TableColumn<Order>(
                    title = "Order #",
                    width = 110.dp
                ) { order ->
                    Text(order.orderNumber, fontWeight = FontWeight.Bold)
                },
                TableColumn<Order>(
                    title = "Type",
                    width = 110.dp
                ) { order ->
                    RestoBadge(text = order.orderType.name, variant = BadgeVariant.NEUTRAL)
                },
                TableColumn<Order>(
                    title = "Status",
                    width = 120.dp
                ) { order ->
                    val variant = when (order.status.name) {
                        "READY" -> BadgeVariant.SUCCESS
                        "PREPARING" -> BadgeVariant.WARNING
                        "COMPLETED" -> BadgeVariant.INFO
                        else -> BadgeVariant.NEUTRAL
                    }
                    RestoBadge(text = order.status.name, variant = variant)
                },
                TableColumn<Order>(
                    title = "Total",
                    width = 100.dp
                ) { order ->
                    Text("$${String.format(Locale.US, "%.2f", order.totalAmount)}", fontWeight = FontWeight.Bold)
                },
                TableColumn<Order>(
                    title = "Payment",
                    width = 100.dp
                ) { order ->
                    RestoBadge(
                        text = order.paymentStatus,
                        variant = if (order.paymentStatus == "PAID") BadgeVariant.SUCCESS else BadgeVariant.DANGER
                    )
                },
                TableColumn<Order>(
                    title = "Time",
                    width = 90.dp
                ) { order ->
                    Text(
                        text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(order.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            RestoDataTable(
                items = orders,
                columns = columns,
                onRowClick = { onNavigate(NavigationSection.REPORTS_ANALYTICS) },
                minTableWidth = 630.dp,
                testTag = "dashboard_orders_table"
            )
        }
    }
}
