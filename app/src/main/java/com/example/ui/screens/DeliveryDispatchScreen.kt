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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.engine.DeliveryDispatchEngine
import com.example.core.i18n.AppLanguage
import com.example.designsystem.*
import com.example.domain.model.*
import java.util.*

private enum class DeliveryTab(val titleEn: String, val titleAr: String) {
    DISPATCH_BOARD("Dispatch Center", "مركز التوجيه والإرسال"),
    DRIVER_APP("Driver Handheld App", "تطبيق السائق الذكي"),
    DELIVERY_ZONES("Delivery Zones & Fees", "مناطق ورسوم التوصيل"),
    COD_SETTLEMENT("Driver Fleet & COD Settlement", "أسطول السائقين وتسوية النقد")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryDispatchScreen(
    branch: Branch,
    deliveryZones: List<DeliveryZone>,
    drivers: List<DeliveryDriver>,
    assignments: List<DeliveryAssignment>,
    cashSettlements: List<DriverCashSettlement>,
    language: AppLanguage,
    onAssignDriver: (assignmentId: String, driverId: String) -> Unit,
    onTransitionDelivery: (
        assignmentId: String,
        targetStatus: DeliveryWorkflowStatus,
        otpInput: String?,
        failureReason: String?,
        proofNote: String
    ) -> Unit,
    onToggleDriverShiftAndConsent: (driverId: String, shiftStatus: String, locationConsent: Boolean) -> Unit,
    onSaveDeliveryZone: (DeliveryZone) -> Unit,
    onSettleDriverCash: (driverId: String, handedOverAmount: Double, notes: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = language == AppLanguage.ARABIC
    var selectedTab by remember { mutableStateOf(DeliveryTab.DISPATCH_BOARD) }
    var selectedDriverId by remember { mutableStateOf(drivers.firstOrNull()?.id ?: "drv_01") }
    var otpInputs by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var zoneNameEn by remember { mutableStateOf("") }
    var zoneFee by remember { mutableStateOf("4.50") }
    var zoneMinOrder by remember { mutableStateOf("20.00") }
    var zoneEstMins by remember { mutableStateOf("35") }

    val activeDeliveriesCount = assignments.count {
        it.status !in setOf(DeliveryWorkflowStatus.DELIVERED, DeliveryWorkflowStatus.RETURNED_TO_BRANCH)
    }
    val onlineDriversCount = drivers.count { it.shiftStatus == "ONLINE" }
    val totalUnsettledCod = drivers.sumOf { it.cashCollectedUnsettled }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("delivery_dispatch_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header & Live Telemetry Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isAr) "إدارة التوصيل وتوجيه السائقين (Phase 8)" else "Delivery Dispatch & Driver Fleet (Phase 8)",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Consented shift GPS architecture • Central Order Engine sync • OTP proof & COD settlement",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RestoBadge(text = "$activeDeliveriesCount Active Deliveries", variant = BadgeVariant.INFO)
                RestoBadge(text = "$onlineDriversCount Drivers Online", variant = BadgeVariant.SUCCESS)
                RestoBadge(text = "COD Unsettled: $${String.format(Locale.US, "%.2f", totalUnsettledCod)}", variant = BadgeVariant.WARNING)
            }
        }

        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DeliveryTab.entries.forEach { tab ->
                FilterChip(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    label = { Text(if (isAr) tab.titleAr else tab.titleEn) },
                    modifier = Modifier.testTag("delivery_tab_${tab.name.lowercase()}")
                )
            }
        }

        when (selectedTab) {
            DeliveryTab.DISPATCH_BOARD -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Map-Ready Telemetry Banner
                    item {
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("Live Geolocation & Zone Dispatch Grid", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    RestoBadge(text = "CONSENTED SHIFT GPS ONLY", variant = BadgeVariant.SUCCESS)
                                }
                                Text(
                                    text = drivers.joinToString("   |   ") { drv ->
                                        val gpsInfo = if (drv.shiftStatus == "ONLINE" && drv.locationSharingConsented)
                                            "GPS (${drv.currentLat}, ${drv.currentLng})"
                                        else "GPS Paused (Off-Shift/No Consent)"
                                        "${drv.fullName} [${drv.driverStatus}]: $gpsInfo"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(assignments, key = { it.id }) { asn ->
                        val isOverdue = asn.isOverdue()
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${asn.orderNumber} • ${asn.customerName} (${asn.customerPhone})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Address: ${asn.deliveryAddress} • Zone: ${asn.deliveryZoneName}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (isOverdue) {
                                            RestoBadge(text = "OVERDUE (${asn.elapsedMinutes()}m)", variant = BadgeVariant.DANGER)
                                        }
                                        RestoBadge(
                                            text = asn.status.titleEn,
                                            variant = when (asn.status) {
                                                DeliveryWorkflowStatus.DELIVERED -> BadgeVariant.SUCCESS
                                                DeliveryWorkflowStatus.FAILED_DELIVERY, DeliveryWorkflowStatus.RETURNED_TO_BRANCH -> BadgeVariant.DANGER
                                                else -> BadgeVariant.WARNING
                                            }
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Courier: ${asn.driverName ?: "Unassigned"} (${asn.vehicleInfo.ifBlank { "No Vehicle" }}) • Total: $${String.format(Locale.US, "%.2f", asn.orderTotalAmount)} ${if (asn.isCashOnDelivery) "[COD]" else "[PAID]"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Customer OTP: ${asn.customerOtpCode}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Driver Assignment / Reassignment Row
                                if (asn.status !in setOf(DeliveryWorkflowStatus.DELIVERED, DeliveryWorkflowStatus.RETURNED_TO_BRANCH)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Assign / Reassign Driver:", style = MaterialTheme.typography.bodySmall)
                                        drivers.forEach { drv ->
                                            OutlinedButton(
                                                onClick = { onAssignDriver(asn.id, drv.id) },
                                                modifier = Modifier.testTag("assign_drv_${asn.id}_${drv.id}")
                                            ) {
                                                Text("${drv.fullName} (${drv.driverStatus})")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            DeliveryTab.DRIVER_APP -> {
                val activeDriver = drivers.find { it.id == selectedDriverId } ?: drivers.firstOrNull()
                val driverOrders = assignments.filter { it.driverId == activeDriver?.id || it.status == DeliveryWorkflowStatus.AWAITING_DRIVER }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Driver Handheld Session & Consented Telemetry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    drivers.forEach { drv ->
                                        FilterChip(
                                            selected = drv.id == activeDriver?.id,
                                            onClick = { selectedDriverId = drv.id },
                                            label = { Text("${drv.fullName} (${drv.vehiclePlate})") }
                                        )
                                    }
                                }
                                if (activeDriver != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf("ONLINE", "ON_BREAK", "OFFLINE").forEach { st ->
                                                FilterChip(
                                                    selected = activeDriver.shiftStatus == st,
                                                    onClick = {
                                                        onToggleDriverShiftAndConsent(activeDriver.id, st, activeDriver.locationSharingConsented)
                                                    },
                                                    label = { Text(st) }
                                                )
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("Consented Shift GPS:", style = MaterialTheme.typography.bodySmall)
                                            Switch(
                                                checked = activeDriver.locationSharingConsented,
                                                onCheckedChange = { consented ->
                                                    onToggleDriverShiftAndConsent(activeDriver.id, activeDriver.shiftStatus, consented)
                                                },
                                                modifier = Modifier.testTag("driver_gps_consent_switch")
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    items(driverOrders, key = { it.id }) { asn ->
                        val otpVal = otpInputs[asn.id] ?: asn.customerOtpCode
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("${asn.orderNumber} • ${asn.customerName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    RestoBadge(text = asn.status.titleEn, variant = BadgeVariant.INFO)
                                }
                                Text("Drop-off: ${asn.deliveryAddress} (${asn.deliveryNotes.ifBlank { "Standard drop-off" }})", style = MaterialTheme.typography.bodySmall)
                                Text("Amount: $${String.format(Locale.US, "%.2f", asn.orderTotalAmount)} • ${if (asn.isCashOnDelivery) "Collect Cash (COD)" else "Prepaid Online"}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)

                                // Step-by-step Driver Workflow Controls
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    when (asn.status) {
                                        DeliveryWorkflowStatus.AWAITING_DRIVER -> {
                                            Button(onClick = { activeDriver?.let { onAssignDriver(asn.id, it.id) } }) {
                                                Text("Claim Order")
                                            }
                                        }
                                        DeliveryWorkflowStatus.ASSIGNED -> {
                                            Button(
                                                onClick = { onTransitionDelivery(asn.id, DeliveryWorkflowStatus.ACCEPTED, null, null, "") },
                                                modifier = Modifier.testTag("drv_accept_${asn.id}")
                                            ) { Text("Accept Delivery") }
                                            OutlinedButton(
                                                onClick = { onTransitionDelivery(asn.id, DeliveryWorkflowStatus.AWAITING_DRIVER, null, "Driver declined", "") }
                                            ) { Text("Reject") }
                                        }
                                        DeliveryWorkflowStatus.ACCEPTED -> {
                                            Button(
                                                onClick = { onTransitionDelivery(asn.id, DeliveryWorkflowStatus.ARRIVED_RESTAURANT, null, null, "") },
                                                modifier = Modifier.testTag("drv_arrived_${asn.id}")
                                            ) { Text("Arrived at Branch") }
                                        }
                                        DeliveryWorkflowStatus.ARRIVED_RESTAURANT -> {
                                            Button(
                                                onClick = { onTransitionDelivery(asn.id, DeliveryWorkflowStatus.PICKED_UP, null, null, "") },
                                                modifier = Modifier.testTag("drv_picked_up_${asn.id}")
                                            ) { Text("Picked Up Package") }
                                        }
                                        DeliveryWorkflowStatus.PICKED_UP -> {
                                            Button(
                                                onClick = { onTransitionDelivery(asn.id, DeliveryWorkflowStatus.ON_ROUTE, null, null, "") },
                                                modifier = Modifier.testTag("drv_on_route_${asn.id}")
                                            ) { Text("Start Navigation (On Route)") }
                                        }
                                        DeliveryWorkflowStatus.ON_ROUTE -> {
                                            OutlinedTextField(
                                                value = otpVal,
                                                onValueChange = { otpInputs = otpInputs + (asn.id to it) },
                                                label = { Text("Customer OTP (${asn.customerOtpCode})") },
                                                singleLine = true,
                                                modifier = Modifier.width(170.dp)
                                            )
                                            Button(
                                                onClick = {
                                                    onTransitionDelivery(asn.id, DeliveryWorkflowStatus.DELIVERED, otpVal, null, "Verified OTP & Handed to Customer")
                                                },
                                                modifier = Modifier.testTag("drv_complete_${asn.id}")
                                            ) { Text("Verify OTP & Deliver") }
                                            OutlinedButton(
                                                onClick = {
                                                    onTransitionDelivery(asn.id, DeliveryWorkflowStatus.FAILED_DELIVERY, null, "Customer unreachable at gate", "")
                                                }
                                            ) { Text("Mark Failed") }
                                        }
                                        DeliveryWorkflowStatus.FAILED_DELIVERY -> {
                                            Button(
                                                onClick = { onTransitionDelivery(asn.id, DeliveryWorkflowStatus.ON_ROUTE, null, null, "Retry attempt #${asn.retryCount + 1}") }
                                            ) { Text("Retry Delivery") }
                                            OutlinedButton(
                                                onClick = { onTransitionDelivery(asn.id, DeliveryWorkflowStatus.RETURNED_TO_BRANCH, null, "Returned undelivered to branch", "") }
                                            ) { Text("Return to Branch") }
                                        }
                                        else -> {
                                            Text("Completed • Proof: ${asn.proofOfDeliveryNote}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            DeliveryTab.DELIVERY_ZONES -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Add / Configure Delivery Zone", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = zoneNameEn,
                                        onValueChange = { zoneNameEn = it },
                                        label = { Text("Zone Name") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = zoneFee,
                                        onValueChange = { zoneFee = it },
                                        label = { Text("Fee ($)") },
                                        singleLine = true,
                                        modifier = Modifier.width(100.dp)
                                    )
                                    OutlinedTextField(
                                        value = zoneMinOrder,
                                        onValueChange = { zoneMinOrder = it },
                                        label = { Text("Min ($)") },
                                        singleLine = true,
                                        modifier = Modifier.width(100.dp)
                                    )
                                }
                                Button(
                                    onClick = {
                                        if (zoneNameEn.isNotBlank()) {
                                            onSaveDeliveryZone(
                                                DeliveryZone(
                                                    id = "dzone_${UUID.randomUUID().toString().take(6)}",
                                                    branchId = branch.id,
                                                    code = "DZ-${(10..99).random()}",
                                                    nameEn = zoneNameEn,
                                                    nameAr = zoneNameEn,
                                                    boundaryDescription = "Configured polygon radius",
                                                    radiusKm = 5.0,
                                                    deliveryFee = zoneFee.toDoubleOrNull() ?: 4.0,
                                                    minimumOrderAmount = zoneMinOrder.toDoubleOrNull() ?: 15.0,
                                                    estimatedMinutes = zoneEstMins.toIntOrNull() ?: 35
                                                )
                                            )
                                            zoneNameEn = ""
                                        }
                                    },
                                    modifier = Modifier.testTag("save_delivery_zone_btn")
                                ) {
                                    Text("Save Delivery Zone")
                                }
                            }
                        }
                    }

                    items(deliveryZones, key = { it.id }) { dz ->
                        RestoCard {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Column {
                                    Text("${dz.code} • ${if (isAr) dz.nameAr else dz.nameEn}", fontWeight = FontWeight.Bold)
                                    Text("Boundary: ${dz.boundaryDescription} (${dz.radiusKm} km)", style = MaterialTheme.typography.bodySmall)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Fee: $${String.format(Locale.US, "%.2f", dz.deliveryFee)} • Min: $${String.format(Locale.US, "%.2f", dz.minimumOrderAmount)}", fontWeight = FontWeight.Bold)
                                    Text("Target SLA: ${dz.estimatedMinutes} mins", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            DeliveryTab.COD_SETTLEMENT -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(drivers, key = { it.id }) { drv ->
                        RestoCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${drv.fullName} (${drv.phone})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Vehicle: ${drv.vehicleType} • ${drv.vehiclePlate} • Completed Today: ${drv.completedDeliveriesToday}", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = "Unsettled COD Cash in Hand: $${String.format(Locale.US, "%.2f", drv.cashCollectedUnsettled)}",
                                        color = if (drv.cashCollectedUnsettled > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Button(
                                    onClick = {
                                        onSettleDriverCash(drv.id, drv.cashCollectedUnsettled, "Full shift COD handover to branch cashier")
                                    },
                                    enabled = drv.cashCollectedUnsettled > 0.0,
                                    modifier = Modifier.testTag("settle_cod_${drv.id}")
                                ) {
                                    Text("Settle COD ($${String.format(Locale.US, "%.2f", drv.cashCollectedUnsettled)})")
                                }
                            }
                        }
                    }

                    if (cashSettlements.isNotEmpty()) {
                        item {
                            Text("Recent Driver COD Settlements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        items(cashSettlements, key = { it.id }) { s ->
                            RestoCard {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                    Column {
                                        Text("${s.driverName} -> Received by ${s.receivedByCashierName}", fontWeight = FontWeight.Bold)
                                        Text(s.notes, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(
                                        text = "Handed Over: $${String.format(Locale.US, "%.2f", s.handedOverCashAmount)}",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
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
