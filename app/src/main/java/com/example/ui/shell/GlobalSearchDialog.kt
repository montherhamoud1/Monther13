package com.example.ui.shell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.constants.NavigationSection
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.Translations
import com.example.designsystem.BadgeVariant
import com.example.designsystem.RestoBadge
import com.example.designsystem.RestoModalDialog
import com.example.designsystem.RestoSearchBar
import com.example.domain.model.Order
import com.example.domain.model.Product
import com.example.domain.model.RestaurantTable

@Composable
fun GlobalSearchDialog(
    visible: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    products: List<Product>,
    orders: List<Order>,
    tables: List<RestaurantTable>,
    language: AppLanguage,
    onNavigate: (NavigationSection) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val filteredProducts = if (query.isBlank()) emptyList() else {
        products.filter {
            it.nameEn.contains(query, ignoreCase = true) ||
            it.nameAr.contains(query, ignoreCase = true) ||
            it.sku.contains(query, ignoreCase = true)
        }
    }

    val filteredOrders = if (query.isBlank()) emptyList() else {
        orders.filter {
            it.orderNumber.contains(query, ignoreCase = true)
        }
    }

    val filteredTables = if (query.isBlank()) emptyList() else {
        tables.filter {
            it.tableNumber.contains(query, ignoreCase = true) ||
            it.section.contains(query, ignoreCase = true)
        }
    }

    RestoModalDialog(
        onDismissRequest = onDismiss,
        title = "Omni Search & Command Palette"
    ) {
        RestoSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            placeholder = Translations.get("search_placeholder", language)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
        ) {
            // Quick Navigation shortcuts when query is blank
            if (query.isBlank()) {
                item {
                    Text(
                        text = "Quick Navigation Shortcuts",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(NAV_ITEMS) { item ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onNavigate(item.section)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = Translations.get(item.translationKey, language),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                // Search Results
                if (filteredProducts.isNotEmpty()) {
                    item {
                        Text(
                            text = "Products (${filteredProducts.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(filteredProducts) { prod ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onNavigate(NavigationSection.POS)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (language == AppLanguage.ARABIC) prod.nameAr else prod.nameEn,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "SKU: ${prod.sku} • Stock: ${prod.currentStock.toInt()}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "$${prod.price}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                if (filteredOrders.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Orders (${filteredOrders.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(filteredOrders) { order ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onNavigate(NavigationSection.DASHBOARD)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = order.orderNumber,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Type: ${order.orderType.name} • Total: $${order.totalAmount}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RestoBadge(
                                    text = order.status.name,
                                    variant = if (order.status.name == "READY") BadgeVariant.SUCCESS else BadgeVariant.INFO
                                )
                            }
                        }
                    }
                }

                if (filteredTables.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tables (${filteredTables.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(filteredTables) { table ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onNavigate(NavigationSection.TABLES)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Table ${table.tableNumber}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Section: ${table.section} • Capacity: ${table.capacity}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RestoBadge(text = table.status.name, variant = BadgeVariant.NEUTRAL)
                            }
                        }
                    }
                }

                if (filteredProducts.isEmpty() && filteredOrders.isEmpty() && filteredTables.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No matching records found for \"$query\"",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
