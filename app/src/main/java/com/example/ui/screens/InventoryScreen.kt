package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.Translations
import com.example.designsystem.*
import com.example.domain.model.*
import java.util.*

private enum class InventoryTab(val titleEn: String, val titleAr: String) {
    WAREHOUSE_STOCK("Multi-Warehouse Stock", "مخزون المستودعات"),
    MOVEMENT_LEDGER("Traceable Ledger", "دفتر حركات المخزون"),
    TRANSFERS("Warehouse Transfers", "التحويلات المخزنية"),
    PROCUREMENT("Purchasing & Suppliers", "المشتريات والموردون"),
    WASTE_COUNTS("Waste & Stock Counts", "الهدر والجرد الفعلي")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    products: List<Product>,
    language: AppLanguage,
    warehouses: List<Warehouse> = emptyList(),
    unitConversions: List<UnitConversionRule> = emptyList(),
    stockItems: List<WarehouseStockItem> = emptyList(),
    suppliers: List<Supplier> = emptyList(),
    transactions: List<InventoryTransaction> = emptyList(),
    transfers: List<WarehouseTransfer> = emptyList(),
    purchaseOrders: List<PurchaseOrderDocument> = emptyList(),
    wasteRecords: List<WasteRecord> = emptyList(),
    stockCountSessions: List<StockCountSession> = emptyList(),
    onRecordWasteOrDamage: (stockItemId: String, quantity: Double, reason: String, isDamage: Boolean) -> Unit = { _, _, _, _ -> },
    onCreateOrAdvanceTransfer: (
        transferId: String?,
        sourceWarehouseId: String,
        destinationWarehouseId: String,
        itemId: String,
        quantity: Double,
        targetStatus: WarehouseTransferStatus
    ) -> Unit = { _, _, _, _, _, _ -> },
    onAdvancePurchaseOrderStage: (poId: String, targetStage: ProcurementStage) -> Unit = { _, _ -> },
    onCreatePurchaseOrder: (supplierId: String, warehouseId: String, itemId: String, quantity: Double, unitPrice: Double) -> Unit = { _, _, _, _, _ -> },
    onRecordStockCountAdjustment: (stockItemId: String, countedQuantity: Double, notes: String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val isAr = language == AppLanguage.ARABIC
    var selectedTab by remember { mutableStateOf(InventoryTab.WAREHOUSE_STOCK) }
    var selectedWarehouseId by remember { mutableStateOf<String?>(null) }

    val filteredStock = remember(stockItems, selectedWarehouseId) {
        if (selectedWarehouseId == null) stockItems else stockItems.filter { it.warehouseId == selectedWarehouseId }
    }
    val lowStockCount = stockItems.count { it.isLowStock }
    val totalValuation = stockItems.sumOf { it.totalValuation }

    // Waste / Count quick input states
    var wasteQtyInput by remember { mutableStateOf("2.0") }
    var wasteReasonInput by remember { mutableStateOf("Prep trim / expired batch") }
    var countQtyInput by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Transfer creation states
    var trfQtyInput by remember { mutableStateOf("30.0") }

    // PO creation states
    var poQtyInput by remember { mutableStateOf("50.0") }
    var poPriceInput by remember { mutableStateOf("4.20") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("inventory_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header & Valuation KPIs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = Translations.get("nav_inventory", language),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Central, Branch & Kitchen Warehouses • Recipe Consumption • Traceable Ledger & Procurement (Phase 9)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RestoBadge(
                    text = "Valuation: $${String.format(Locale.US, "%.2f", totalValuation)}",
                    variant = BadgeVariant.SUCCESS
                )
                RestoBadge(
                    text = "$lowStockCount Low Stock",
                    variant = if (lowStockCount > 0) BadgeVariant.WARNING else BadgeVariant.INFO
                )
            }
        }

        // Scrollable Tab Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InventoryTab.entries.forEach { tab ->
                FilterChip(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    label = { Text(if (isAr) tab.titleAr else tab.titleEn) },
                    modifier = Modifier.testTag("inventory_tab_${tab.name.lowercase()}")
                )
            }
        }

        when (selectedTab) {
            InventoryTab.WAREHOUSE_STOCK -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Warehouse Filter Chips
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedWarehouseId == null,
                                onClick = { selectedWarehouseId = null },
                                label = { Text("All Warehouses (${warehouses.size})") }
                            )
                            warehouses.forEach { wh ->
                                FilterChip(
                                    selected = selectedWarehouseId == wh.id,
                                    onClick = { selectedWarehouseId = wh.id },
                                    label = { Text("${wh.code} • ${if (isAr) wh.nameAr else wh.nameEn} [${wh.warehouseType}]") }
                                )
                            }
                        }
                    }

                    // Unit Conversion Rules Summary
                    if (unitConversions.isNotEmpty()) {
                        item {
                            RestoCard {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Configured Unit Conversions (Purchase -> Stock -> Recipe)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = unitConversions.joinToString("   |   ") { u ->
                                            "${u.ingredientNameEn}: 1 ${u.purchaseUnit} = ${u.purchaseToStockFactor.toInt()} ${u.stockUnit} (1 ${u.stockUnit} = ${u.stockToRecipeFactor.toInt()} ${u.recipeUnit})"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    items(filteredStock, key = { it.id }) { item ->
                        val countStr = countQtyInput[item.id] ?: item.currentQuantity.toString()
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${item.itemCode} • ${if (isAr) item.itemNameAr else item.itemNameEn}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Warehouse: ${item.warehouseName} • Valuation: ${item.costingMethod.titleEn}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    RestoBadge(
                                        text = if (item.isLowStock) "BELOW REORDER" else "OPTIMAL",
                                        variant = if (item.isLowStock) BadgeVariant.WARNING else BadgeVariant.SUCCESS
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("On-Hand: ${String.format(Locale.US, "%.1f", item.currentQuantity)} ${item.stockUnit}", fontWeight = FontWeight.Bold)
                                    Text("Reserved: ${String.format(Locale.US, "%.1f", item.reservedQuantity)} ${item.stockUnit}")
                                    Text("Available: ${String.format(Locale.US, "%.1f", item.availableQuantity)} ${item.stockUnit}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text("Reorder Point: ${item.reorderPoint.toInt()} (Max: ${item.maximumQuantity.toInt()})")
                                    Text("Unit Cost: $${String.format(Locale.US, "%.2f", item.unitCost)}")
                                }

                                // Quick Actions: Physical Stock Count & Waste Log
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = countStr,
                                        onValueChange = { countQtyInput = countQtyInput + (item.id to it) },
                                        label = { Text("Physical Count (${item.stockUnit})") },
                                        singleLine = true,
                                        modifier = Modifier.width(160.dp)
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            val counted = countStr.toDoubleOrNull() ?: item.currentQuantity
                                            onRecordStockCountAdjustment(item.id, counted, "Cycle count variance approval")
                                        },
                                        modifier = Modifier.testTag("stock_count_btn_${item.id}")
                                    ) {
                                        Text("Approve Count")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onRecordWasteOrDamage(item.id, 1.0, "Kitchen prep spoilage", false)
                                        },
                                        modifier = Modifier.testTag("log_waste_btn_${item.id}")
                                    ) {
                                        Text("Log -1 ${item.stockUnit} Waste")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            InventoryTab.MOVEMENT_LEDGER -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(transactions, key = { it.id }) { tx ->
                        RestoCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        RestoBadge(
                                            text = tx.transactionType.titleEn,
                                            variant = if (tx.quantityDelta >= 0) BadgeVariant.SUCCESS else BadgeVariant.WARNING
                                        )
                                        Text("${tx.referenceNumber} • ${if (isAr) tx.itemNameAr else tx.itemNameEn}", fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = "Warehouse: ${tx.warehouseName} • Source: ${tx.sourceDocumentType} (${tx.sourceDocumentId}) • By: ${tx.performedBy}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (tx.notes.isNotBlank()) {
                                        Text(tx.notes, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${if (tx.quantityDelta >= 0) "+" else ""}${String.format(Locale.US, "%.2f", tx.quantityDelta)} ${tx.unit}",
                                        fontWeight = FontWeight.Bold,
                                        color = if (tx.quantityDelta >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "Bal: ${String.format(Locale.US, "%.1f", tx.balanceBefore)} -> ${String.format(Locale.US, "%.1f", tx.balanceAfter)}",
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            InventoryTab.TRANSFERS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        RestoCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Request Inter-Warehouse Stock Transfer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("Central Cold Storage -> Downtown Branch Store -> Main Hot Kitchen", style = MaterialTheme.typography.bodySmall)
                                }
                                OutlinedTextField(
                                    value = trfQtyInput,
                                    onValueChange = { trfQtyInput = it },
                                    label = { Text("Qty") },
                                    singleLine = true,
                                    modifier = Modifier.width(90.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        onCreateOrAdvanceTransfer(
                                            null,
                                            "wh_central",
                                            "wh_branch_01",
                                            "ing_angus_patty",
                                            trfQtyInput.toDoubleOrNull() ?: 30.0,
                                            WarehouseTransferStatus.REQUESTED
                                        )
                                    },
                                    modifier = Modifier.testTag("request_transfer_btn")
                                ) {
                                    Text("New Transfer Request")
                                }
                            }
                        }
                    }

                    items(transfers, key = { it.id }) { trf ->
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("${trf.transferNumber} • ${trf.itemNameEn} (${trf.quantity} ${trf.unit})", fontWeight = FontWeight.Bold)
                                    RestoBadge(text = trf.status.titleEn, variant = BadgeVariant.INFO)
                                }
                                Text("${trf.sourceWarehouseName}  ➔  ${trf.destinationWarehouseName} • Requested by ${trf.requestedBy}", style = MaterialTheme.typography.bodySmall)
                                if (trf.status != WarehouseTransferStatus.COMPLETED) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val nextStatus = when (trf.status) {
                                            WarehouseTransferStatus.REQUESTED -> WarehouseTransferStatus.APPROVED
                                            WarehouseTransferStatus.APPROVED -> WarehouseTransferStatus.SHIPPED
                                            WarehouseTransferStatus.SHIPPED -> WarehouseTransferStatus.RECEIVED
                                            WarehouseTransferStatus.RECEIVED -> WarehouseTransferStatus.COMPLETED
                                            else -> WarehouseTransferStatus.COMPLETED
                                        }
                                        Button(
                                            onClick = {
                                                onCreateOrAdvanceTransfer(
                                                    trf.id,
                                                    trf.sourceWarehouseId,
                                                    trf.destinationWarehouseId,
                                                    trf.itemId,
                                                    trf.quantity,
                                                    nextStatus
                                                )
                                            },
                                            modifier = Modifier.testTag("advance_trf_${trf.id}")
                                        ) {
                                            Text("Advance to ${nextStatus.titleEn}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            InventoryTab.PROCUREMENT -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Create PO Card
                    item {
                        RestoCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Procurement Pipeline (Request -> PO -> Receive GRN -> Invoice -> Payment)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("Supplier: ${suppliers.firstOrNull()?.name ?: "Prime Meats"} • Target: Central Warehouse", style = MaterialTheme.typography.bodySmall)
                                }
                                OutlinedTextField(
                                    value = poQtyInput,
                                    onValueChange = { poQtyInput = it },
                                    label = { Text("Qty") },
                                    singleLine = true,
                                    modifier = Modifier.width(85.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        val supId = suppliers.firstOrNull()?.id ?: "sup_01"
                                        onCreatePurchaseOrder(
                                            supId,
                                            "wh_central",
                                            "ing_angus_patty",
                                            poQtyInput.toDoubleOrNull() ?: 50.0,
                                            poPriceInput.toDoubleOrNull() ?: 4.20
                                        )
                                    },
                                    modifier = Modifier.testTag("create_po_btn")
                                ) {
                                    Text("Create PO")
                                }
                            }
                        }
                    }

                    // Suppliers List
                    item {
                        Text("Approved Suppliers & Payables", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(suppliers, key = { it.id }) { sup ->
                        RestoCard {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Column {
                                    Text("${sup.code} • ${sup.name} (${sup.contactPerson})", fontWeight = FontWeight.Bold)
                                    Text("Terms: ${sup.paymentTerms} • Email: ${sup.email} • Phone: ${sup.phone}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text(
                                    text = "Payable: $${String.format(Locale.US, "%.2f", sup.currentPayableBalance)}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Purchase Orders
                    item {
                        Text("Purchase Orders & Receiving", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(purchaseOrders, key = { it.id }) { po ->
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("${po.poNumber} • ${po.supplierName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    RestoBadge(text = po.stage.titleEn, variant = BadgeVariant.INFO)
                                }
                                Text(
                                    text = "Warehouse: ${po.warehouseName} • Total: $${String.format(Locale.US, "%.2f", po.totalAmount)} • Paid: $${String.format(Locale.US, "%.2f", po.paidAmount)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        ProcurementStage.APPROVED,
                                        ProcurementStage.RECEIVED,
                                        ProcurementStage.SUPPLIER_INVOICE,
                                        ProcurementStage.SUPPLIER_PAYMENT
                                    ).forEach { stage ->
                                        OutlinedButton(
                                            onClick = { onAdvancePurchaseOrderStage(po.id, stage) },
                                            enabled = po.stage != stage,
                                            modifier = Modifier.testTag("po_stage_${po.id}_${stage.name.lowercase()}")
                                        ) {
                                            Text(stage.titleEn)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            InventoryTab.WASTE_COUNTS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        RestoCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = wasteQtyInput,
                                    onValueChange = { wasteQtyInput = it },
                                    label = { Text("Waste Qty") },
                                    singleLine = true,
                                    modifier = Modifier.width(100.dp)
                                )
                                OutlinedTextField(
                                    value = wasteReasonInput,
                                    onValueChange = { wasteReasonInput = it },
                                    label = { Text("Waste / Spoilage Reason") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        val firstStock = stockItems.firstOrNull()
                                        if (firstStock != null) {
                                            onRecordWasteOrDamage(
                                                firstStock.id,
                                                wasteQtyInput.toDoubleOrNull() ?: 1.0,
                                                wasteReasonInput,
                                                false
                                            )
                                        }
                                    }
                                ) {
                                    Text("Record Waste")
                                }
                            }
                        }
                    }

                    item {
                        Text("Recorded Waste & Damage Logs (${wasteRecords.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(wasteRecords, key = { it.id }) { wst ->
                        RestoCard {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Column {
                                    Text("${wst.wasteNumber} • ${wst.itemNameEn} (-${wst.quantity} ${wst.unit})", fontWeight = FontWeight.Bold)
                                    Text("Reason: ${wst.reason} • Reported by: ${wst.reportedByEmployee} • Approved by: ${wst.approvedBy}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text(
                                    text = "-$${String.format(Locale.US, "%.2f", wst.totalLossAmount)}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    if (stockCountSessions.isNotEmpty()) {
                        item {
                            Text("Approved Stock Count Sessions (${stockCountSessions.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        items(stockCountSessions, key = { it.id }) { sc ->
                            RestoCard {
                                Column {
                                    Text("${sc.sessionNumber} • ${sc.warehouseName} [${sc.status}]", fontWeight = FontWeight.Bold)
                                    sc.lines.forEach { ln ->
                                        Text(
                                            text = "${ln.itemNameEn}: System=${ln.systemQuantity} ${ln.unit}, Counted=${ln.countedQuantity} ${ln.unit} (Variance=${ln.varianceQuantity})",
                                            style = MaterialTheme.typography.bodySmall
                                        )
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
