package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.engine.MenuEngine
import com.example.core.engine.OrderEngine
import com.example.core.i18n.AppLanguage
import com.example.designsystem.*
import com.example.domain.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// --- 1. Item Customization Modal (Variants, Modifiers, Add-ons, Item Discount, Notes) ---
@Composable
fun ItemCustomizationModal(
    visible: Boolean,
    cartItem: OrderEngine.PosCartItem?,
    modifierGroups: List<ModifierGroup>,
    addons: List<ProductAddon>,
    language: AppLanguage,
    onConfirm: (OrderEngine.PosCartItem) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible || cartItem == null) return

    val product = cartItem.product
    var selectedVariant by remember(cartItem) {
        mutableStateOf(cartItem.selectedVariant ?: product.variants.find { it.isDefault } ?: product.variants.firstOrNull())
    }
    var selectedMods by remember(cartItem) {
        mutableStateOf(cartItem.selectedModifiers)
    }
    var selectedAddons by remember(cartItem) {
        mutableStateOf(cartItem.selectedAddons)
    }
    var quantity by remember(cartItem) { mutableStateOf(cartItem.quantity.coerceAtLeast(1)) }
    var itemDiscountText by remember(cartItem) {
        mutableStateOf(if (cartItem.itemDiscountAmount > 0) cartItem.itemDiscountAmount.toString() else "")
    }
    var notes by remember(cartItem) { mutableStateOf(cartItem.notes) }
    var validationError by remember(cartItem) { mutableStateOf<String?>(null) }

    val applicableModGroups = modifierGroups.filter { it.id in product.modifierGroupIds }
    val applicableAddons = addons.filter { it.id in product.addonIds || it.productId == product.id }

    RestoModalDialog(
        visible = visible,
        title = if (language == AppLanguage.ARABIC) "تخصيص الصنف: ${product.nameAr}" else "Customize: ${product.nameEn}",
        onDismiss = onDismiss,
        confirmContent = {
            RestoButton(
                onClick = {
                    // Validate required modifier groups
                    for (group in applicableModGroups) {
                        val groupChosenIds = selectedMods.filter { it.groupId == group.id }.map { it.id }
                        val check = MenuEngine.validateModifierSelection(group, groupChosenIds)
                        if (!check.isValid) {
                            validationError = check.errorMessageEn
                            return@RestoButton
                        }
                    }
                    val basePrice = product.price + (selectedVariant?.priceAdjustment ?: 0.0)
                    val updated = cartItem.copy(
                        selectedVariant = selectedVariant,
                        selectedModifiers = selectedMods,
                        selectedAddons = selectedAddons.filterValues { it > 0 },
                        quantity = quantity,
                        baseResolvedPrice = basePrice,
                        itemDiscountAmount = itemDiscountText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                        notes = notes.trim()
                    )
                    onConfirm(updated)
                },
                testTag = "confirm_item_customization"
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (language == AppLanguage.ARABIC) "تحديث الصنف" else "Apply to Ticket")
            }
        },
        dismissContent = {
            RestoButton(onClick = onDismiss, variant = RestoButtonVariant.GHOST) {
                Text(if (language == AppLanguage.ARABIC) "إلغاء" else "Cancel")
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (validationError != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = validationError!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            // Quantity Stepper
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.ARABIC) "الكمية" else "Quantity",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }
                        Text(
                            text = "$quantity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        FilledTonalIconButton(
                            onClick = { quantity++ },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                    }
                }
            }

            // Variants (Sizes)
            if (product.variants.isNotEmpty()) {
                item {
                    Text(
                        text = if (language == AppLanguage.ARABIC) "الحجم / المتغير" else "Select Size / Variant",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(product.variants) { variant ->
                            val isSelected = selectedVariant?.id == variant.id
                            val deltaStr = if (variant.priceAdjustment > 0) " (+$${String.format(Locale.US, "%.2f", variant.priceAdjustment)})" else ""
                            RestoFilterChip(
                                selected = isSelected,
                                onClick = { selectedVariant = variant },
                                label = (if (language == AppLanguage.ARABIC) variant.nameAr else variant.nameEn) + deltaStr
                            )
                        }
                    }
                }
            }

            // Modifier Groups
            items(applicableModGroups) { group ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == AppLanguage.ARABIC) group.nameAr else group.nameEn,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        RestoStatusBadge(
                            text = if (group.isRequired) "Required (${group.minSelections}-${group.maxSelections})" else "Optional (Max ${group.maxSelections})",
                            variant = if (group.isRequired) BadgeVariant.WARNING else BadgeVariant.INFO
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    group.options.forEach { option ->
                        val isSelected = selectedMods.any { it.id == option.id }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    validationError = null
                                    val currentGroupMods = selectedMods.filter { it.groupId == group.id }
                                    selectedMods = if (isSelected) {
                                        selectedMods.filterNot { it.id == option.id }
                                    } else {
                                        if (group.maxSelections == 1) {
                                            selectedMods.filterNot { it.groupId == group.id } + option
                                        } else if (currentGroupMods.size < group.maxSelections) {
                                            selectedMods + option
                                        } else {
                                            selectedMods
                                        }
                                    }
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = isSelected, onCheckedChange = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == AppLanguage.ARABIC) option.nameAr else option.nameEn,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (option.priceAdjustment > 0) {
                                Text(
                                    text = "+$${String.format(Locale.US, "%.2f", option.priceAdjustment)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Add-ons
            if (applicableAddons.isNotEmpty()) {
                item {
                    Text(
                        text = if (language == AppLanguage.ARABIC) "الإضافات الاختيارية" else "Optional Add-Ons",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    applicableAddons.forEach { addon ->
                        val count = selectedAddons[addon] ?: 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (language == AppLanguage.ARABIC) addon.nameAr else addon.nameEn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "+$${String.format(Locale.US, "%.2f", addon.price)} each (Max ${addon.maxQuantity})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedIconButton(
                                    onClick = {
                                        if (count > 0) {
                                            selectedAddons = selectedAddons.toMutableMap().apply { put(addon, count - 1) }
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Text("$count", fontWeight = FontWeight.Bold)
                                OutlinedIconButton(
                                    onClick = {
                                        if (count < addon.maxQuantity) {
                                            selectedAddons = selectedAddons.toMutableMap().apply { put(addon, count + 1) }
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Item Notes & Line Discount
            item {
                RestoTextInput(
                    value = notes,
                    onValueChange = { notes = it },
                    label = if (language == AppLanguage.ARABIC) "ملاحظات المطبخ لهذا الصنف" else "Kitchen Prep Notes (e.g., No onions, sauce on side)",
                    placeholder = "Enter special cooking instruction..."
                )
                Spacer(modifier = Modifier.height(8.dp))
                RestoTextInput(
                    value = itemDiscountText,
                    onValueChange = { itemDiscountText = it },
                    label = if (language == AppLanguage.ARABIC) "خصم خاص على السطر ($)" else "Item-Level Discount ($)",
                    placeholder = "0.00"
                )
            }
        }
    }
}

// --- 2. Multi-Method & Mixed Payment Tender Modal ---
@Composable
fun PaymentTenderModal(
    visible: Boolean,
    orderTotal: Double,
    branchId: String,
    cashierId: String,
    shiftId: String?,
    language: AppLanguage,
    onCompleteOrderPayment: (List<Payment>) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    var paymentLegs by remember(visible, orderTotal) { mutableStateOf<List<Payment>>(emptyList()) }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    val resolution = remember(orderTotal, paymentLegs) {
        OrderEngine.resolvePayments(orderTotal, paymentLegs)
    }
    var tenderInput by remember(resolution.remainingBalance) {
        mutableStateOf(String.format(Locale.US, "%.2f", resolution.remainingBalance))
    }
    var referenceInput by remember { mutableStateOf("") }

    RestoModalDialog(
        visible = visible,
        title = if (language == AppLanguage.ARABIC) "تحصيل المدفوعات المتعددة والمختلطة" else "Payment Terminal (Cash / Card / Mixed Tender)",
        onDismiss = onDismiss,
        confirmContent = {
            RestoButton(
                onClick = {
                    val finalPayments = if (paymentLegs.isEmpty() && resolution.remainingBalance > 0.0) {
                        val leg = OrderEngine.createPaymentLeg(
                            orderId = "pending",
                            branchId = branchId,
                            remainingOrderBalance = orderTotal,
                            tenderedAmount = tenderInput.toDoubleOrNull() ?: orderTotal,
                            method = selectedMethod,
                            cashierId = cashierId,
                            shiftId = shiftId,
                            referenceNumber = referenceInput
                        )
                        listOf(leg)
                    } else {
                        resolution.deduplicatedPayments
                    }
                    onCompleteOrderPayment(finalPayments)
                },
                enabled = resolution.paymentStatus == "PAID" || (paymentLegs.isEmpty() && (tenderInput.toDoubleOrNull() ?: 0.0) >= orderTotal),
                testTag = "confirm_payment_complete"
            ) {
                Icon(Icons.Default.Paid, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (language == AppLanguage.ARABIC) "إتمام الدفع وإصدار الطلب" else "Finalize Payment & Dispatch")
            }
        },
        dismissContent = {
            RestoButton(onClick = onDismiss, variant = RestoButtonVariant.GHOST) {
                Text(if (language == AppLanguage.ARABIC) "إلغاء" else "Cancel")
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Balance Summary Banner
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Order Total", style = MaterialTheme.typography.labelSmall)
                        Text(
                            "$${String.format(Locale.US, "%.2f", orderTotal)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Paid So Far", style = MaterialTheme.typography.labelSmall)
                        Text(
                            "$${String.format(Locale.US, "%.2f", resolution.totalPaid)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            if (resolution.totalChangeDue > 0) "Change Due" else "Remaining Balance",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = if (resolution.totalChangeDue > 0) {
                                "$${String.format(Locale.US, "%.2f", resolution.totalChangeDue)}"
                            } else {
                                "$${String.format(Locale.US, "%.2f", resolution.remainingBalance)}"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = if (resolution.remainingBalance > 0.009) MaterialTheme.colorScheme.error else Color(0xFF10B981)
                        )
                    }
                }
            }

            // Payment Method Selector Chips
            Text(
                text = if (language == AppLanguage.ARABIC) "طريقة الدفع" else "Select Payment Method",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val methods = listOf(
                    PaymentMethod.CASH,
                    PaymentMethod.CREDIT_CARD,
                    PaymentMethod.DEBIT_CARD,
                    PaymentMethod.BANK_TRANSFER,
                    PaymentMethod.DIGITAL_WALLET
                )
                items(methods) { method ->
                    RestoFilterChip(
                        selected = selectedMethod == method,
                        onClick = { selectedMethod = method },
                        label = if (language == AppLanguage.ARABIC) method.titleAr else method.title
                    )
                }
            }

            if (resolution.remainingBalance > 0.009) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        RestoTextInput(
                            value = tenderInput,
                            onValueChange = { tenderInput = it },
                            label = "Tender Amount ($)",
                            placeholder = "0.00"
                        )
                    }
                    if (selectedMethod != PaymentMethod.CASH) {
                        Box(modifier = Modifier.weight(1f)) {
                            RestoTextInput(
                                value = referenceInput,
                                onValueChange = { referenceInput = it },
                                label = "Auth / Ref #",
                                placeholder = "Terminal Ref..."
                            )
                        }
                    }
                }

                // Quick Cash Denominations
                if (selectedMethod == PaymentMethod.CASH) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val quickAmounts = listOf(
                            "Exact" to resolution.remainingBalance,
                            "$10" to 10.0,
                            "$20" to 20.0,
                            "$50" to 50.0,
                            "$100" to 100.0
                        )
                        items(quickAmounts) { (label, amt) ->
                            OutlinedButton(
                                onClick = { tenderInput = String.format(Locale.US, "%.2f", amt) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                RestoButton(
                    onClick = {
                        val amt = tenderInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0.0) {
                            val leg = OrderEngine.createPaymentLeg(
                                orderId = "pending",
                                branchId = branchId,
                                remainingOrderBalance = resolution.remainingBalance,
                                tenderedAmount = amt,
                                method = selectedMethod,
                                cashierId = cashierId,
                                shiftId = shiftId,
                                referenceNumber = referenceInput
                            )
                            paymentLegs = paymentLegs + leg
                            referenceInput = ""
                        }
                    },
                    variant = RestoButtonVariant.SECONDARY,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "add_payment_leg_button"
                ) {
                    Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Payment Leg (Mixed / Split Tender)")
                }
            }

            // Added Payment Legs List
            if (paymentLegs.isNotEmpty()) {
                Text(
                    "Recorded Payment Legs (${paymentLegs.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                paymentLegs.forEach { leg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "${leg.paymentMethod.title} • Ref: ${leg.referenceNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (leg.changeAmount > 0) {
                                Text(
                                    "Tendered: $${String.format(Locale.US, "%.2f", leg.tenderedAmount)} | Change: $${String.format(Locale.US, "%.2f", leg.changeAmount)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "$${String.format(Locale.US, "%.2f", leg.amount)}",
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            IconButton(
                                onClick = { paymentLegs = paymentLegs.filterNot { it.id == leg.id } },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove leg", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 3. Split Bill Modal (Equal Split by Seats & Item Assignment Split) ---
@Composable
fun SplitBillModal(
    visible: Boolean,
    order: Order,
    items: List<OrderItem>,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    if (!visible) return

    var splitMode by remember { mutableStateOf(0) } // 0 = Equal Split, 1 = By Item
    var seatCount by remember { mutableStateOf(2) }
    var itemSeatMap by remember(items) {
        mutableStateOf(items.mapIndexed { idx, item -> item.id to ((idx % 2) + 1) }.toMap())
    }
    var paidSeats by remember { mutableStateOf<Set<Int>>(emptySet()) }

    val shares = remember(splitMode, seatCount, itemSeatMap, order, items) {
        if (splitMode == 0) {
            OrderEngine.splitBillEqually(order, items, seatCount)
        } else {
            val grouped = items.groupBy { itemSeatMap[it.id] ?: 1 }
            OrderEngine.splitBillByItems(order, grouped)
        }
    }

    RestoModalDialog(
        visible = visible,
        title = if (language == AppLanguage.ARABIC) "تقسيم الفاتورة بين الضيوف" else "Split Bill Calculator (${order.orderNumber})",
        onDismiss = onDismiss,
        confirmContent = {
            RestoButton(onClick = onDismiss) {
                Text(if (language == AppLanguage.ARABIC) "تم" else "Done")
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RestoFilterChip(
                    selected = splitMode == 0,
                    onClick = { splitMode = 0 },
                    label = "Split Equally by Seats"
                )
                RestoFilterChip(
                    selected = splitMode == 1,
                    onClick = { splitMode = 1 },
                    label = "Split by Item Assignment"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Number of Guests / Seats:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedIconButton(
                        onClick = { if (seatCount > 2) seatCount-- },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Text("$seatCount Guests", fontWeight = FontWeight.Black)
                    OutlinedIconButton(
                        onClick = { if (seatCount < 8) seatCount++ },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (splitMode == 1) {
                Text("Tap guest number for each item:", style = MaterialTheme.typography.labelSmall)
                items.forEach { item ->
                    val assignedSeat = itemSeatMap[item.id] ?: 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${item.quantity}x ${item.productNameEn} ($${String.format(Locale.US, "%.2f", item.totalPrice)})",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..seatCount).forEach { s ->
                                FilterChip(
                                    selected = assignedSeat == s,
                                    onClick = {
                                        itemSeatMap = itemSeatMap.toMutableMap().apply { put(item.id, s) }
                                    },
                                    label = { Text("#$s", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(shares) { share ->
                    val isSeatPaid = paidSeats.contains(share.seatNumber)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSeatPaid) Color(0xFF10B981).copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(share.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Subtotal: $${String.format(Locale.US, "%.2f", share.subtotal)} • Tax: $${String.format(Locale.US, "%.2f", share.taxAmount)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "$${String.format(Locale.US, "%.2f", share.totalDue)}",
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                RestoButton(
                                    onClick = {
                                        paidSeats = if (isSeatPaid) paidSeats - share.seatNumber else paidSeats + share.seatNumber
                                    },
                                    variant = if (isSeatPaid) RestoButtonVariant.SECONDARY else RestoButtonVariant.PRIMARY
                                ) {
                                    Text(if (isSeatPaid) "Paid ✓" else "Pay Seat")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 4. Held Orders & Central Order Operations Manager Modal ---
@Composable
fun OrderOperationsManagerModal(
    visible: Boolean,
    orders: List<Order>,
    tables: List<RestaurantTable>,
    language: AppLanguage,
    initialTab: Int = 0, // 0 = Held Orders, 1 = All Orders (Edit/State/Cancel/Refund/Merge/Transfer)
    onRecallOrderToPos: (Order) -> Unit,
    onTransitionState: (String, OrderStatus, String) -> Unit,
    onRefundOrReturn: (String, Boolean, Double, String) -> Unit,
    onMergeOrders: (String, String) -> Unit,
    onTransferTable: (String, String) -> Unit,
    onPrintReceipt: (Order) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    var activeTab by remember(initialTab) { mutableStateOf(initialTab) }
    var selectedOrderForAction by remember { mutableStateOf<Order?>(null) }
    var actionReason by remember { mutableStateOf("") }
    var refundAmountInput by remember { mutableStateOf("") }
    var mergeTargetOrderId by remember { mutableStateOf<String?>(null) }
    var transferTargetTableId by remember { mutableStateOf<String?>(null) }

    val heldOrders = orders.filter { it.status == OrderStatus.HELD }

    RestoModalDialog(
        visible = visible,
        title = if (language == AppLanguage.ARABIC) "مركز إدارة الطلبات والمرتجعات والدمج" else "Central Order Engine & Held Queue",
        onDismiss = onDismiss,
        confirmContent = {
            RestoButton(onClick = onDismiss) {
                Text(if (language == AppLanguage.ARABIC) "إغلاق" else "Close")
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RestoFilterChip(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    label = "Held Orders (${heldOrders.size})"
                )
                RestoFilterChip(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    label = "All Orders & State Machine (${orders.size})"
                )
            }

            val displayedOrders = if (activeTab == 0) heldOrders else orders

            if (displayedOrders.isEmpty()) {
                RestoEmptyState(
                    title = "No Orders Found",
                    description = if (activeTab == 0) "No orders are currently parked on hold." else "No orders recorded for this branch yet.",
                    icon = Icons.Default.ReceiptLong
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayedOrders) { ord ->
                        val isExpanded = selectedOrderForAction?.id == ord.id
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isExpanded) 1.5.dp else 1.dp,
                                    color = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = ord.orderNumber,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Black
                                            )
                                            RestoStatusBadge(
                                                text = ord.orderType.titleEn,
                                                variant = BadgeVariant.INFO
                                            )
                                            RestoStatusBadge(
                                                text = ord.status.code,
                                                variant = when (ord.status) {
                                                    OrderStatus.COMPLETED -> BadgeVariant.SUCCESS
                                                    OrderStatus.CANCELLED, OrderStatus.REFUNDED -> BadgeVariant.ERROR
                                                    OrderStatus.HELD -> BadgeVariant.WARNING
                                                    else -> BadgeVariant.NEUTRAL
                                                }
                                            )
                                        }
                                        if (!ord.holdLabel.isNullOrBlank()) {
                                            Text(
                                                text = "Hold Ref: ${ord.holdLabel}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        val tableLabel = tables.find { it.id == ord.tableId }?.tableNumber ?: "No Table"
                                        Text(
                                            text = "Table: $tableLabel • Customer: ${ord.customerName ?: "Walk-in"} • Payment: ${ord.paymentStatus}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "$${String.format(Locale.US, "%.2f", ord.totalAmount)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${ord.items.sumOf { it.quantity }} items",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Primary Action Row
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (ord.status in listOf(OrderStatus.HELD, OrderStatus.PENDING, OrderStatus.PREPARING, OrderStatus.DRAFT)) {
                                        item {
                                            RestoButton(
                                                onClick = {
                                                    onRecallOrderToPos(ord)
                                                    onDismiss()
                                                },
                                                variant = RestoButtonVariant.PRIMARY
                                            ) {
                                                Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (ord.status == OrderStatus.HELD) "Recall Held Order" else "Edit in POS")
                                            }
                                        }
                                    }
                                    item {
                                        RestoButton(
                                            onClick = { onPrintReceipt(ord) },
                                            variant = RestoButtonVariant.OUTLINE
                                        ) {
                                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Receipt")
                                        }
                                    }
                                    item {
                                        RestoButton(
                                            onClick = {
                                                selectedOrderForAction = if (isExpanded) null else ord
                                                refundAmountInput = String.format(Locale.US, "%.2f", ord.totalAmount)
                                                actionReason = ""
                                            },
                                            variant = RestoButtonVariant.SECONDARY
                                        ) {
                                            Text(if (isExpanded) "Hide Actions ▲" else "State / Refund / Merge / Table ▼")
                                        }
                                    }
                                }

                                // Expanded Controlled Actions Panel
                                if (isExpanded) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // 1. Controlled State Transitions
                                    val allowedStates = OrderEngine.getAllowedNextStates(ord.status)
                                    Text(
                                        text = "Controlled State Transitions (from ${ord.status.code}):",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (allowedStates.isEmpty()) {
                                        Text(
                                            text = "Terminal order state (${ord.status.code}) — no further transitions allowed.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    } else {
                                        RestoTextInput(
                                            value = actionReason,
                                            onValueChange = { actionReason = it },
                                            label = "Reason (Required for Cancel / Refund / Return)",
                                            placeholder = "Enter operational reason..."
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(allowedStates) { nextState ->
                                                OutlinedButton(
                                                    onClick = {
                                                        val defaultReason = actionReason.ifBlank { "POS Operator Action -> ${nextState.code}" }
                                                        if (nextState == OrderStatus.REFUNDED || nextState == OrderStatus.RETURNED) {
                                                            onRefundOrReturn(
                                                                ord.id,
                                                                nextState == OrderStatus.RETURNED,
                                                                refundAmountInput.toDoubleOrNull() ?: ord.totalAmount,
                                                                defaultReason
                                                            )
                                                        } else {
                                                            onTransitionState(ord.id, nextState, defaultReason)
                                                        }
                                                    }
                                                ) {
                                                    Text("→ ${nextState.titleEn}", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }

                                    // 2. Partial / Full Refund & Return Controls
                                    if (ord.status in listOf(OrderStatus.COMPLETED, OrderStatus.SERVED, OrderStatus.READY)) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Refund / Item Return:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                RestoTextInput(
                                                    value = refundAmountInput,
                                                    onValueChange = { refundAmountInput = it },
                                                    label = "Refund Amount ($)",
                                                    placeholder = "${ord.totalAmount}"
                                                )
                                            }
                                            RestoButton(
                                                onClick = {
                                                    val r = actionReason.ifBlank { "Customer Refund Request" }
                                                    onRefundOrReturn(ord.id, false, refundAmountInput.toDoubleOrNull() ?: ord.totalAmount, r)
                                                },
                                                variant = RestoButtonVariant.DANGER
                                            ) {
                                                Text("Refund $")
                                            }
                                            RestoButton(
                                                onClick = {
                                                    val r = actionReason.ifBlank { "Item Return & Restock" }
                                                    val allItemQtys = ord.items.associate { it.id to 1 }
                                                    onRefundOrReturn(ord.id, true, refundAmountInput.toDoubleOrNull() ?: ord.totalAmount, r)
                                                },
                                                variant = RestoButtonVariant.OUTLINE
                                            ) {
                                                Text("Return Item")
                                            }
                                        }
                                    }

                                    // 3. Transfer Table (for active orders)
                                    if (ord.status !in listOf(OrderStatus.COMPLETED, OrderStatus.CANCELLED, OrderStatus.REFUNDED)) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Transfer Order to Table:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(tables.filter { it.id != ord.tableId }) { tbl ->
                                                OutlinedButton(
                                                    onClick = { onTransferTable(ord.id, tbl.id) }
                                                ) {
                                                    Text("Move to ${tbl.tableNumber} (${tbl.status.name})", fontSize = 11.sp)
                                                }
                                            }
                                        }

                                        // 4. Merge with Another Active Order
                                        val otherActiveOrders = orders.filter {
                                            it.id != ord.id && it.status !in listOf(OrderStatus.COMPLETED, OrderStatus.CANCELLED, OrderStatus.REFUNDED)
                                        }
                                        if (otherActiveOrders.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Merge Another Order into ${ord.orderNumber}:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                items(otherActiveOrders) { secondary ->
                                                    OutlinedButton(
                                                        onClick = { onMergeOrders(ord.id, secondary.id) }
                                                    ) {
                                                        Text("Merge + ${secondary.orderNumber} ($${secondary.totalAmount})", fontSize = 11.sp)
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
        }
    }
}

// --- 5. Cashier Shift & Cash Drawer Modal ---
@Composable
fun CashierShiftModal(
    visible: Boolean,
    shift: CashierShift?,
    language: AppLanguage,
    onOpenShift: (String, Double) -> Unit,
    onRecordMovement: (CashMovementType, Double, String) -> Unit,
    onCloseShift: (Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    var terminalCode by remember { mutableStateOf(shift?.terminalId ?: "POS-01") }
    var openingCashInput by remember { mutableStateOf("250.00") }
    var movementType by remember { mutableStateOf(CashMovementType.CASH_IN) }
    var movementAmount by remember { mutableStateOf("") }
    var movementReason by remember { mutableStateOf("") }
    var actualClosingCashInput by remember(shift?.expectedCash) {
        mutableStateOf(String.format(Locale.US, "%.2f", shift?.expectedCash ?: 0.0))
    }
    var closingNotes by remember { mutableStateOf("") }

    RestoModalDialog(
        visible = visible,
        title = if (language == AppLanguage.ARABIC) "إدارة وردية الكاشير وعهدة الصندوق" else "Cashier Shift & Cash Drawer Reconciliation",
        onDismiss = onDismiss,
        confirmContent = {
            RestoButton(onClick = onDismiss) {
                Text(if (language == AppLanguage.ARABIC) "إغلاق" else "Done")
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (shift == null || shift.status == ShiftStatus.CLOSED) {
                item {
                    if (shift != null && shift.status == ShiftStatus.CLOSED) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Last Shift Closed (${shift.terminalId})", fontWeight = FontWeight.Bold)
                                Text(
                                    "Expected Cash: $${String.format(Locale.US, "%.2f", shift.expectedCash)} | Actual Counted: $${String.format(Locale.US, "%.2f", shift.actualClosingCash ?: 0.0)} | Difference: $${String.format(Locale.US, "%.2f", shift.cashDifference)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    Text("Open New Cashier Shift", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    RestoTextInput(
                        value = terminalCode,
                        onValueChange = { terminalCode = it },
                        label = "POS Terminal ID",
                        placeholder = "POS-01"
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    RestoTextInput(
                        value = openingCashInput,
                        onValueChange = { openingCashInput = it },
                        label = "Opening Cash Float ($)",
                        placeholder = "250.00"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RestoButton(
                        onClick = {
                            onOpenShift(terminalCode.ifBlank { "POS-01" }, openingCashInput.toDoubleOrNull() ?: 250.0)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "open_shift_button"
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Shift & Initialize Cash Drawer")
                    }
                }
            } else {
                // Active Shift Metrics Grid
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Terminal: ${shift.terminalId} (${shift.cashierName})", fontWeight = FontWeight.Bold)
                                RestoStatusBadge("SHIFT OPEN", BadgeVariant.SUCCESS)
                            }
                            HorizontalDivider()
                            ShiftMetricRow("Opening Cash Float", shift.openingCash)
                            ShiftMetricRow("+ Cash Sales", shift.cashSales, Color(0xFF10B981))
                            ShiftMetricRow("+ Card Sales (Non-Cash)", shift.cardSales)
                            ShiftMetricRow("+ Bank Transfer & Wallet Sales", shift.bankTransferSales + shift.walletSales)
                            ShiftMetricRow("+ Cash In (Pay-In)", shift.cashIn, Color(0xFF10B981))
                            ShiftMetricRow("- Cash Out (Safe Drops)", shift.cashOut, MaterialTheme.colorScheme.error)
                            ShiftMetricRow("- Petty Cash Expenses", shift.expenses, MaterialTheme.colorScheme.error)
                            ShiftMetricRow("- Cash Refunds", shift.cashRefunds, MaterialTheme.colorScheme.error)
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Expected Cash in Drawer", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "$${String.format(Locale.US, "%.2f", shift.expectedCash)}",
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Cash In / Cash Out / Expense Entry
                item {
                    Text("Record Cash Drawer Movement", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf(CashMovementType.CASH_IN, CashMovementType.CASH_OUT, CashMovementType.EXPENSE)) { t ->
                            RestoFilterChip(
                                selected = movementType == t,
                                onClick = { movementType = t },
                                label = t.titleEn
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(0.4f)) {
                            RestoTextInput(
                                value = movementAmount,
                                onValueChange = { movementAmount = it },
                                label = "Amount ($)",
                                placeholder = "25.00"
                            )
                        }
                        Box(modifier = Modifier.weight(0.6f)) {
                            RestoTextInput(
                                value = movementReason,
                                onValueChange = { movementReason = it },
                                label = "Reason / Voucher",
                                placeholder = "Supplier tip, change..."
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    RestoButton(
                        onClick = {
                            val amt = movementAmount.toDoubleOrNull() ?: 0.0
                            if (amt > 0.0) {
                                onRecordMovement(movementType, amt, movementReason.ifBlank { movementType.titleEn })
                                movementAmount = ""
                                movementReason = ""
                            }
                        },
                        variant = RestoButtonVariant.SECONDARY,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Record ${movementType.titleEn}")
                    }
                }

                // Close Shift Reconciliation
                item {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Close Shift & Count Drawer (Z-Report)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    val counted = actualClosingCashInput.toDoubleOrNull() ?: shift.expectedCash
                    val diff = counted - shift.expectedCash
                    RestoTextInput(
                        value = actualClosingCashInput,
                        onValueChange = { actualClosingCashInput = it },
                        label = "Actual Counted Cash ($)",
                        placeholder = "${shift.expectedCash}"
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cash Difference (Actual - Expected): $${String.format(Locale.US, "%.2f", diff)} (${if (diff == 0.0) "Balanced" else if (diff > 0) "Overage" else "Shortage"})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (diff == 0.0) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    RestoTextInput(
                        value = closingNotes,
                        onValueChange = { closingNotes = it },
                        label = "Shift Closing Notes",
                        placeholder = "End of shift handover notes..."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RestoButton(
                        onClick = { onCloseShift(counted, closingNotes) },
                        variant = RestoButtonVariant.DANGER,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "close_shift_button"
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Close Shift & Lock Drawer")
                    }
                }
            }
        }
    }
}

@Composable
private fun ShiftMetricRow(label: String, amount: Double, valueColor: Color = Color.Unspecified) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(
            "$${String.format(Locale.US, "%.2f", amount)}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

// --- 6. Customer Assignment & Quick-Create Modal ---
@Composable
fun CustomerAssignmentModal(
    visible: Boolean,
    customers: List<Customer>,
    selectedCustomer: Customer?,
    language: AppLanguage,
    onSelectCustomer: (Customer?) -> Unit,
    onCreateCustomer: (Customer) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    var searchQuery by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }

    val filtered = customers.filter {
        searchQuery.isBlank() ||
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.phone.contains(searchQuery, ignoreCase = true)
    }

    RestoModalDialog(
        visible = visible,
        title = if (language == AppLanguage.ARABIC) "تعيين عميل الولاء للطلب" else "Assign CRM Customer to Order",
        onDismiss = onDismiss,
        confirmContent = {
            if (selectedCustomer != null) {
                RestoButton(
                    onClick = {
                        onSelectCustomer(null)
                        onDismiss()
                    },
                    variant = RestoButtonVariant.OUTLINE
                ) {
                    Text("Unassign Walk-in")
                }
            }
            RestoButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RestoTextInput(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "Search by Customer Name or Phone",
                placeholder = "Type name or +966..."
            )

            LazyColumn(
                modifier = Modifier.heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered) { cust ->
                    val isSelected = selectedCustomer?.id == cust.id
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectCustomer(cust)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(cust.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(cust.phone, style = MaterialTheme.typography.labelSmall)
                            }
                            RestoStatusBadge("${cust.tier} (${cust.loyaltyPoints} pts)", BadgeVariant.SUCCESS)
                        }
                    }
                }
            }

            HorizontalDivider()
            Text("Quick Register New Customer", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    RestoTextInput(
                        value = newName,
                        onValueChange = { newName = it },
                        label = "Full Name",
                        placeholder = "Customer Name"
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    RestoTextInput(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = "Phone Number",
                        placeholder = "+966 5X XXX XXXX"
                    )
                }
            }
            RestoButton(
                onClick = {
                    if (newName.isNotBlank() && newPhone.isNotBlank()) {
                        val created = Customer(
                            id = "cust_${UUID.randomUUID().toString().take(6)}",
                            companyId = "comp_01",
                            name = newName.trim(),
                            phone = newPhone.trim(),
                            email = newEmail.ifBlank { "guest@crm.local" },
                            loyaltyPoints = 50,
                            tier = "SILVER"
                        )
                        onCreateCustomer(created)
                        onSelectCustomer(created)
                        onDismiss()
                    }
                },
                enabled = newName.isNotBlank() && newPhone.isNotBlank(),
                variant = RestoButtonVariant.SECONDARY,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Register & Assign Customer")
            }
        }
    }
}

// --- 7. Thermal Receipt Print Preview Modal ---
@Composable
fun ReceiptPreviewModal(
    visible: Boolean,
    company: Company,
    branch: Branch,
    order: Order?,
    language: AppLanguage,
    onPrintConfirmed: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible || order == null) return

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }

    RestoModalDialog(
        visible = visible,
        title = if (language == AppLanguage.ARABIC) "معاينة وطباعة الإيصال الضريبي" else "Thermal Receipt Print Preview (${order.orderNumber})",
        onDismiss = onDismiss,
        confirmContent = {
            RestoButton(
                onClick = {
                    onPrintConfirmed()
                    onDismiss()
                },
                testTag = "print_receipt_confirm_button"
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (language == AppLanguage.ARABIC) "إرسال للطابعة الحرارية" else "Send to Thermal Printer")
            }
        },
        dismissContent = {
            RestoButton(onClick = onDismiss, variant = RestoButtonVariant.GHOST) {
                Text("Close")
            }
        }
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = company.nameEn.uppercase(),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${branch.nameEn} (${branch.code})",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "VAT Reg #: ${company.taxNumber} | Tel: ${branch.phone}",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelSmall
                )
                Text("----------------------------------------", fontFamily = FontFamily.Monospace)
                Text(
                    text = "Order: ${order.orderNumber}   Mode: ${order.orderType.titleEn}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Date: ${dateFormatter.format(Date(order.createdAt))}",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "Terminal: ${order.terminalId} | Cashier: ${order.cashierName}",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelSmall
                )
                if (!order.customerName.isNullOrBlank()) {
                    Text(
                        text = "Customer: ${order.customerName} (${order.customerPhone ?: ""})",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Text("----------------------------------------", fontFamily = FontFamily.Monospace)

                order.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.quantity}x ${item.productNameEn}",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", item.totalPrice)}",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (!item.variantName.isNullOrBlank() || item.modifiersSummary.isNotBlank()) {
                        Text(
                            text = "  + ${listOfNotNull(item.variantName, item.modifiersSummary.takeIf { it.isNotBlank() }).joinToString(" | ")}",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (item.addonsSummary.isNotBlank()) {
                        Text(
                            text = "  + Addons: ${item.addonsSummary}",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (item.notes.isNotBlank()) {
                        Text(
                            text = "  * Note: ${item.notes}",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Text("----------------------------------------", fontFamily = FontFamily.Monospace)
                ReceiptRow("Subtotal:", order.subtotal)
                if (order.discountAmount > 0) {
                    ReceiptRow("Discount ${order.couponCode?.let { "($it)" } ?: ""}:", -order.discountAmount)
                }
                if (order.serviceFee > 0) {
                    ReceiptRow("Service Fee:", order.serviceFee)
                }
                if (order.deliveryFee > 0) {
                    ReceiptRow("Delivery Fee:", order.deliveryFee)
                }
                ReceiptRow("Tax (VAT):", order.taxAmount)
                Text("========================================", fontFamily = FontFamily.Monospace)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("TOTAL PAYABLE:", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                    Text(
                        "$${String.format(Locale.US, "%.2f", order.totalAmount)}",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black
                    )
                }
                if (order.payments.isNotEmpty()) {
                    Text("----------------------------------------", fontFamily = FontFamily.Monospace)
                    order.payments.forEach { p ->
                        ReceiptRow("Paid (${p.paymentMethod.title}):", p.amount)
                    }
                }
                Text("----------------------------------------", fontFamily = FontFamily.Monospace)
                Text(
                    text = "Idempotency Key: ${order.idempotencyKey.take(18)}...",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
        Text(
            "$${String.format(Locale.US, "%.2f", value)}",
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

// --- 8. Manager Override Security Approval Dialog ---
@Composable
fun ManagerApprovalModal(
    visible: Boolean,
    actionTitle: String,
    onApproved: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    RestoModalDialog(
        visible = visible,
        title = "Supervisor Security Approval Required",
        onDismiss = onDismiss,
        confirmContent = {
            RestoButton(
                onClick = {
                    if (OrderEngine.verifyManagerPin(pin)) {
                        onApproved("Manager PIN Override ($pin)")
                        onDismiss()
                    } else {
                        errorMsg = "Invalid Supervisor PIN. Use 1234 for manager authorization."
                    }
                },
                testTag = "approve_manager_pin_button"
            ) {
                Text("Authorize Action")
            }
        },
        dismissContent = {
            RestoButton(onClick = onDismiss, variant = RestoButtonVariant.GHOST) {
                Text("Cancel")
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Action '$actionTitle' requires Manager permission or supervisor PIN authorization.",
                style = MaterialTheme.typography.bodySmall
            )
            if (errorMsg != null) {
                Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }
            RestoTextInput(
                value = pin,
                onValueChange = {
                    pin = it
                    errorMsg = null
                },
                label = "Supervisor Override PIN (Default: 1234)",
                placeholder = "Enter 4-digit PIN..."
            )
        }
    }
}
