package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.constants.AppConstants
import com.example.core.engine.MenuEngine
import com.example.core.engine.OrderEngine
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.Translations
import com.example.data.repository.DefaultSeedData
import com.example.designsystem.*
import com.example.domain.model.*
import java.util.Locale
import java.util.UUID

// Legacy compatibility wrapper
data class CartItem(
    val product: Product,
    var quantity: Int,
    var notes: String = ""
)

@Composable
fun POSScreen(
    company: Company = Company("comp_01", "AL-HAYA", "Al-Haya Hospitality Group", "مجموعة الحياة للضيافة", "VAT-31099288100003"),
    branch: Branch,
    user: User = User("user_03", "comp_01", "youssef_cashier", "youssef@erp.com", "Youssef Zaid", "ROLE_CASHIER", "branch_01"),
    session: UserSession? = null,
    products: List<Product>,
    categories: List<Category> = DefaultSeedData.categories,
    modifierGroups: List<ModifierGroup> = DefaultSeedData.modifierGroups,
    addons: List<ProductAddon> = DefaultSeedData.addons,
    priceRules: List<ChannelPriceRule> = DefaultSeedData.priceRules,
    taxProfiles: List<TaxProfile> = DefaultSeedData.taxProfiles,
    tables: List<RestaurantTable>,
    orders: List<Order> = emptyList(),
    customers: List<Customer> = DefaultSeedData.customers,
    coupons: List<Coupon> = DefaultSeedData.coupons,
    currentShift: CashierShift? = DefaultSeedData.initialShift,
    isOfflineMode: Boolean = false,
    offlineSyncQueue: List<OfflineSyncEvent> = emptyList(),
    language: AppLanguage,
    onPlaceOrder: (Order, List<OrderItem>) -> Unit = { _, _ -> },
    onPlaceCompleteOrder: (Order, List<OrderItem>, List<Payment>) -> Unit = { ord, items, _ -> onPlaceOrder(ord, items) },
    onHoldOrder: (Order, List<OrderItem>) -> Unit = { ord, items -> onPlaceOrder(ord.copy(status = OrderStatus.HELD), items) },
    onTransitionOrderState: (String, OrderStatus, String) -> Unit = { _, _, _ -> },
    onRefundOrReturnOrder: (String, Boolean, Double, String) -> Unit = { _, _, _, _ -> },
    onMergeOrders: (String, String) -> Unit = { _, _ -> },
    onTransferTable: (String, String) -> Unit = { _, _ -> },
    onSaveCustomer: (Customer) -> Unit = {},
    onOpenShift: (String, Double) -> Unit = { _, _ -> },
    onRecordCashMovement: (CashMovementType, Double, String) -> Unit = { _, _, _ -> },
    onCloseShift: (Double, String) -> Unit = { _, _ -> },
    onToggleOfflineMode: (Boolean) -> Unit = {},
    onSyncOfflineQueue: () -> Unit = {},
    onShowFeedback: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Active POS State
    var selectedOrderType by remember { mutableStateOf(OrderType.DINE_IN) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var barcodeQuery by remember { mutableStateOf("") }

    var cartItems by remember { mutableStateOf<List<OrderEngine.PosCartItem>>(emptyList()) }
    var selectedTable by remember(tables) { mutableStateOf(tables.firstOrNull()) }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var editingOrderId by remember { mutableStateOf<String?>(null) }
    var editingOrderNumber by remember { mutableStateOf<String?>(null) }
    var currentIdempotencyKey by remember { mutableStateOf("idem_${UUID.randomUUID()}") }

    // Order-level adjustments & notes
    var orderNotes by remember { mutableStateOf("") }
    var deliveryAddress by remember { mutableStateOf("") }
    var customerPhoneInput by remember { mutableStateOf("") }
    var holdReferenceLabel by remember { mutableStateOf("") }
    var couponCodeInput by remember { mutableStateOf("") }
    var appliedCoupon by remember { mutableStateOf<Coupon?>(null) }
    var manualDiscountType by remember { mutableStateOf(DiscountType.PERCENTAGE) }
    var manualDiscountValue by remember { mutableStateOf(0.0) }
    var manualDiscountInput by remember { mutableStateOf("") }
    var applyServiceFee by remember { mutableStateOf(true) }

    // Modal Visibility States
    var customizingItem by remember { mutableStateOf<OrderEngine.PosCartItem?>(null) }
    var showPaymentModal by remember { mutableStateOf(false) }
    var showSplitBillModal by remember { mutableStateOf(false) }
    var showOrdersManagerModal by remember { mutableStateOf(false) }
    var ordersManagerInitialTab by remember { mutableStateOf(0) }
    var showShiftModal by remember { mutableStateOf(false) }
    var showCustomerModal by remember { mutableStateOf(false) }
    var receiptPreviewOrder by remember { mutableStateOf<Order?>(null) }

    // Security / Manager Approval Gate
    var pendingSecurityActionTitle by remember { mutableStateOf<String?>(null) }
    var pendingSecurityCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }

    fun requirePermissionOrApproval(
        action: OrderEngine.PosSensitiveAction,
        actionTitle: String = action.labelEn,
        block: (String) -> Unit
    ) {
        if (OrderEngine.isAuthorizedForSensitiveAction(session, user.roleId, action)) {
            block(user.fullName)
        } else {
            pendingSecurityActionTitle = actionTitle
            pendingSecurityCallback = block
        }
    }

    // Channel Mapping from POS Mode
    val effectiveChannel = remember(selectedOrderType) {
        when (selectedOrderType) {
            OrderType.DELIVERY -> SalesChannel.DELIVERY_PARTNERS
            OrderType.QR_ONLINE -> SalesChannel.QR_TABLE
            else -> SalesChannel.POS
        }
    }

    val defaultTaxProfile = remember(taxProfiles) {
        taxProfiles.find { it.id == "tax_standard_10" } ?: taxProfiles.firstOrNull()
            ?: TaxProfile("tax_standard_10", "VAT 10%", "ضريبة 10%", AppConstants.DEFAULT_TAX_RATE, isInclusive = false)
    }

    // Financial Breakdown via Central Order Engine
    val financials = remember(
        cartItems,
        selectedOrderType,
        manualDiscountType,
        manualDiscountValue,
        appliedCoupon,
        applyServiceFee,
        defaultTaxProfile
    ) {
        OrderEngine.calculateOrderFinancials(
            cartItems = cartItems,
            orderType = selectedOrderType,
            manualDiscountType = manualDiscountType,
            manualDiscountValue = manualDiscountValue,
            coupon = appliedCoupon,
            taxProfile = defaultTaxProfile,
            applyDineInServiceFee = applyServiceFee,
            serviceFeeRate = AppConstants.DEFAULT_SERVICE_FEE_RATE,
            customDeliveryFee = AppConstants.DEFAULT_DELIVERY_FEE
        )
    }

    // Helper to add a product to the cart with resolved channel/branch pricing
    fun addProductToCart(product: Product, variant: ProductVariant? = null) {
        if (product.isAvailable && product.isSellable && product.currentStock > 0) {
            val defaultVar = variant ?: product.variants.find { it.isDefault } ?: product.variants.firstOrNull()
            val resolvedPrice = MenuEngine.resolveProductPrice(
                product = product,
                variant = defaultVar,
                branchId = branch.id,
                channel = effectiveChannel,
                customerTier = selectedCustomer?.tier,
                rules = priceRules
            )
            val existingMatch = cartItems.find {
                it.product.id == product.id &&
                    it.selectedVariant?.id == defaultVar?.id &&
                    it.selectedModifiers.isEmpty() &&
                    it.selectedAddons.isEmpty() &&
                    it.notes.isBlank()
            }
            cartItems = if (existingMatch != null) {
                cartItems.map {
                    if (it.uid == existingMatch.uid) it.copy(quantity = it.quantity + 1) else it
                }
            } else {
                cartItems + OrderEngine.PosCartItem(
                    product = product,
                    selectedVariant = defaultVar,
                    baseResolvedPrice = resolvedPrice,
                    quantity = 1
                )
            }
        } else {
            onShowFeedback("${product.nameEn} is currently unavailable or out of stock.")
        }
    }

    // Barcode Scanner / SKU Lookup Handler
    fun handleBarcodeLookup(code: String) {
        val trimmed = code.trim()
        if (trimmed.isBlank()) return
        for (prod in products) {
            val matchedVariant = prod.variants.find {
                it.barcode.equals(trimmed, ignoreCase = true) || it.sku.equals(trimmed, ignoreCase = true)
            }
            if (matchedVariant != null) {
                addProductToCart(prod, matchedVariant)
                barcodeQuery = ""
                onShowFeedback("Scanned: ${prod.nameEn} (${matchedVariant.nameEn})")
                return
            }
            if (prod.barcode.equals(trimmed, ignoreCase = true) || prod.sku.equals(trimmed, ignoreCase = true)) {
                addProductToCart(prod)
                barcodeQuery = ""
                onShowFeedback("Scanned: ${prod.nameEn}")
                return
            }
        }
        onShowFeedback("No product found matching barcode/SKU: $trimmed")
    }

    // Build Order & OrderItems from Current POS Ticket
    fun buildCurrentOrderAndItems(status: OrderStatus, payments: List<Payment>): Pair<Order, List<OrderItem>> {
        val orderId = editingOrderId ?: "ord_${UUID.randomUUID().toString().take(8)}"
        val orderNumber = editingOrderNumber ?: "ORD-${(1050..9999).random()}"
        val now = System.currentTimeMillis()
        val itemEntities = cartItems.map { line -> line.toOrderItem(orderId) }
        val paymentResolution = OrderEngine.resolvePayments(financials.totalPayable, payments)
        val order = Order(
            id = orderId,
            branchId = branch.id,
            terminalId = currentShift?.terminalId ?: "POS-01",
            orderNumber = orderNumber,
            orderType = selectedOrderType,
            channel = effectiveChannel,
            tableId = if (selectedOrderType == OrderType.DINE_IN) selectedTable?.id else null,
            cashierId = user.id,
            cashierName = user.fullName,
            customerId = selectedCustomer?.id,
            customerName = selectedCustomer?.name,
            customerPhone = selectedCustomer?.phone ?: customerPhoneInput.ifBlank { null },
            deliveryAddress = if (selectedOrderType == OrderType.DELIVERY || selectedOrderType == OrderType.PHONE_ORDER) deliveryAddress else "",
            status = status,
            items = itemEntities,
            subtotal = financials.grossItemsSubtotal,
            discountAmount = financials.totalDiscount,
            couponCode = appliedCoupon?.code,
            taxAmount = financials.taxAmount,
            deliveryFee = financials.deliveryFee,
            serviceFee = financials.serviceFee,
            totalAmount = financials.totalPayable,
            paymentStatus = if (status == OrderStatus.HELD) "UNPAID" else paymentResolution.paymentStatus,
            payments = paymentResolution.deduplicatedPayments,
            notes = orderNotes,
            holdLabel = if (status == OrderStatus.HELD) holdReferenceLabel.ifBlank { selectedCustomer?.name ?: "Held Ticket" } else null,
            idempotencyKey = currentIdempotencyKey,
            syncStatus = if (isOfflineMode) SyncStatus.QUEUED_OFFLINE else SyncStatus.SYNCED,
            createdAt = now,
            updatedAt = now
        )
        return order to itemEntities
    }

    fun resetPosTicket() {
        cartItems = emptyList()
        editingOrderId = null
        editingOrderNumber = null
        orderNotes = ""
        holdReferenceLabel = ""
        couponCodeInput = ""
        appliedCoupon = null
        manualDiscountValue = 0.0
        manualDiscountInput = ""
        currentIdempotencyKey = "idem_${UUID.randomUUID()}"
    }

    // Recall a Held or Active Order back into the POS Cart for editing or checkout
    fun recallOrderIntoCart(order: Order) {
        editingOrderId = order.id
        editingOrderNumber = order.orderNumber
        selectedOrderType = order.orderType
        selectedTable = tables.find { it.id == order.tableId } ?: selectedTable
        selectedCustomer = customers.find { it.id == order.customerId }
        orderNotes = order.notes
        deliveryAddress = order.deliveryAddress
        customerPhoneInput = order.customerPhone ?: ""
        currentIdempotencyKey = "idem_${UUID.randomUUID()}"

        val reconstructed = order.items.mapNotNull { item ->
            val prod = products.find { it.id == item.productId } ?: products.firstOrNull() ?: return@mapNotNull null
            val variant = prod.variants.find { it.id == item.variantId }
            OrderEngine.PosCartItem(
                uid = item.id,
                product = prod,
                selectedVariant = variant,
                quantity = item.quantity,
                baseResolvedPrice = (item.unitPrice - item.modifiersDelta - item.addonsDelta).coerceAtLeast(0.0),
                itemDiscountAmount = item.discountAmount,
                notes = item.notes
            )
        }
        cartItems = reconstructed
        onShowFeedback("Loaded ${order.orderNumber} into POS editor")
    }

    // Filtered Catalog Products
    val filteredProducts = remember(products, selectedCategoryId, searchQuery, branch.id) {
        products.filter { prod ->
            val matchesBranch = prod.branchAvailability.isEmpty() || branch.id in prod.branchAvailability
            val matchesCat = selectedCategoryId == null || selectedCategoryId == "ALL" ||
                prod.categoryId == selectedCategoryId || prod.categoryId.startsWith("${selectedCategoryId}_")
            val matchesSearch = searchQuery.isBlank() ||
                prod.nameEn.contains(searchQuery, ignoreCase = true) ||
                prod.nameAr.contains(searchQuery, ignoreCase = true) ||
                prod.sku.contains(searchQuery, ignoreCase = true) ||
                prod.barcode.contains(searchQuery, ignoreCase = true)
            prod.isAvailable && prod.isSellable && matchesBranch && matchesCat && matchesSearch
        }
    }

    val heldOrdersCount = orders.count { it.status == OrderStatus.HELD }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("pos_screen")
    ) {
        val isWide = maxWidth >= 840.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // Top POS Mode & Operational Control Strip
            PosOperationalHeaderBar(
                selectedOrderType = selectedOrderType,
                onSelectOrderType = { selectedOrderType = it },
                currentShift = currentShift,
                heldOrdersCount = heldOrdersCount,
                totalOrdersCount = orders.size,
                isOfflineMode = isOfflineMode,
                offlineQueueCount = offlineSyncQueue.count { it.status != SyncStatus.SYNCED },
                language = language,
                onOpenShiftModal = { showShiftModal = true },
                onOpenHeldOrders = {
                    ordersManagerInitialTab = 0
                    showOrdersManagerModal = true
                },
                onOpenOrderManager = {
                    ordersManagerInitialTab = 1
                    showOrdersManagerModal = true
                },
                onToggleOffline = onToggleOfflineMode,
                onSyncNow = onSyncOfflineQueue
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (isWide) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    // Left Pane: Search, Barcode Scanner, Categories, Product Grid
                    Column(
                        modifier = Modifier
                            .weight(1.35f)
                            .fillMaxHeight()
                            .padding(12.dp)
                    ) {
                        CatalogSearchAndBarcodeBar(
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            barcodeQuery = barcodeQuery,
                            onBarcodeChange = { barcodeQuery = it },
                            onBarcodeSubmit = { handleBarcodeLookup(barcodeQuery) },
                            language = language
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PosCategoryFilterRow(
                            categories = categories,
                            selectedCategoryId = selectedCategoryId ?: "ALL",
                            language = language,
                            onSelectCategory = { selectedCategoryId = if (it == "ALL") null else it }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        PosTouchProductGrid(
                            products = filteredProducts,
                            branchId = branch.id,
                            channel = effectiveChannel,
                            customerTier = selectedCustomer?.tier,
                            priceRules = priceRules,
                            language = language,
                            onProductClick = { prod -> addProductToCart(prod) },
                            onCustomizeClick = { prod ->
                                val defaultVar = prod.variants.find { it.isDefault } ?: prod.variants.firstOrNull()
                                val basePrice = MenuEngine.resolveProductPrice(
                                    product = prod,
                                    variant = defaultVar,
                                    branchId = branch.id,
                                    channel = effectiveChannel,
                                    customerTier = selectedCustomer?.tier,
                                    rules = priceRules
                                )
                                customizingItem = OrderEngine.PosCartItem(
                                    product = prod,
                                    selectedVariant = defaultVar,
                                    baseResolvedPrice = basePrice,
                                    quantity = 1
                                )
                            }
                        )
                    }

                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Right Pane: Central Order Ticket & Financial Breakdown
                    Column(
                        modifier = Modifier
                            .weight(0.95f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(12.dp)
                    ) {
                        PosRichOrderTicketPane(
                            editingOrderNumber = editingOrderNumber,
                            selectedOrderType = selectedOrderType,
                            selectedTable = selectedTable,
                            tables = tables,
                            selectedCustomer = selectedCustomer,
                            deliveryAddress = deliveryAddress,
                            onDeliveryAddressChange = { deliveryAddress = it },
                            customerPhoneInput = customerPhoneInput,
                            onCustomerPhoneChange = { customerPhoneInput = it },
                            orderNotes = orderNotes,
                            onOrderNotesChange = { orderNotes = it },
                            cartItems = cartItems,
                            financials = financials,
                            couponCodeInput = couponCodeInput,
                            onCouponCodeInputChange = { couponCodeInput = it },
                            appliedCoupon = appliedCoupon,
                            onApplyCoupon = {
                                val found = coupons.find { it.code.equals(couponCodeInput.trim(), ignoreCase = true) && it.isActive }
                                if (found != null) {
                                    appliedCoupon = found
                                    onShowFeedback("Coupon ${found.code} applied!")
                                } else {
                                    onShowFeedback("Invalid or expired coupon code")
                                }
                            },
                            onRemoveCoupon = {
                                appliedCoupon = null
                                couponCodeInput = ""
                            },
                            manualDiscountInput = manualDiscountInput,
                            onManualDiscountInputChange = { manualDiscountInput = it },
                            onApplyManualDiscount = { type ->
                                val v = manualDiscountInput.toDoubleOrNull() ?: 0.0
                                if (v <= 0.0) {
                                    manualDiscountValue = 0.0
                                } else if (type == DiscountType.PERCENTAGE && v > 15.0) {
                                    requirePermissionOrApproval(
                                        OrderEngine.PosSensitiveAction.APPLY_HIGH_DISCOUNT,
                                        "Apply ${v}% Order Discount"
                                    ) {
                                        manualDiscountType = type
                                        manualDiscountValue = v
                                        onShowFeedback("Approved ${v}% discount")
                                    }
                                } else {
                                    manualDiscountType = type
                                    manualDiscountValue = v
                                }
                            },
                            applyServiceFee = applyServiceFee,
                            onToggleServiceFee = { applyServiceFee = it },
                            language = language,
                            onSelectTable = { selectedTable = it },
                            onOpenCustomerModal = { showCustomerModal = true },
                            onEditCartItem = { customizingItem = it },
                            onUpdateItemQty = { item, newQty ->
                                cartItems = if (newQty <= 0) {
                                    cartItems.filterNot { it.uid == item.uid }
                                } else {
                                    cartItems.map { if (it.uid == item.uid) it.copy(quantity = newQty) else it }
                                }
                            },
                            onClearCart = { resetPosTicket() },
                            onHoldOrderClick = {
                                if (cartItems.isNotEmpty()) {
                                    val (heldOrder, heldItems) = buildCurrentOrderAndItems(OrderStatus.HELD, emptyList())
                                    onHoldOrder(heldOrder, heldItems)
                                    resetPosTicket()
                                }
                            },
                            onSplitBillClick = { showSplitBillModal = true },
                            onQuickPayCash = {
                                if (cartItems.isNotEmpty()) {
                                    val cashLeg = OrderEngine.createPaymentLeg(
                                        orderId = editingOrderId ?: "pending",
                                        branchId = branch.id,
                                        remainingOrderBalance = financials.totalPayable,
                                        tenderedAmount = financials.totalPayable,
                                        method = PaymentMethod.CASH,
                                        cashierId = user.id,
                                        shiftId = currentShift?.id,
                                        referenceNumber = "CASH-EXACT"
                                    )
                                    val (ord, items) = buildCurrentOrderAndItems(OrderStatus.PREPARING, listOf(cashLeg))
                                    onPlaceCompleteOrder(ord, items, listOf(cashLeg))
                                    receiptPreviewOrder = ord
                                    resetPosTicket()
                                }
                            },
                            onOpenMultiPaymentModal = { showPaymentModal = true },
                            onPreviewReceipt = {
                                if (cartItems.isNotEmpty()) {
                                    val (previewOrd, _) = buildCurrentOrderAndItems(OrderStatus.DRAFT, emptyList())
                                    receiptPreviewOrder = previewOrd
                                } else {
                                    orders.firstOrNull()?.let { receiptPreviewOrder = it }
                                }
                            }
                        )
                    }
                }
            } else {
                // Compact Layout: Scrollable catalog on top + condensed ticket bar with actions on bottom
                Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(10.dp)) {
                    CatalogSearchAndBarcodeBar(
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        barcodeQuery = barcodeQuery,
                        onBarcodeChange = { barcodeQuery = it },
                        onBarcodeSubmit = { handleBarcodeLookup(barcodeQuery) },
                        language = language
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    PosCategoryFilterRow(
                        categories = categories,
                        selectedCategoryId = selectedCategoryId ?: "ALL",
                        language = language,
                        onSelectCategory = { selectedCategoryId = if (it == "ALL") null else it }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        PosTouchProductGrid(
                            products = filteredProducts,
                            branchId = branch.id,
                            channel = effectiveChannel,
                            customerTier = selectedCustomer?.tier,
                            priceRules = priceRules,
                            language = language,
                            onProductClick = { prod -> addProductToCart(prod) },
                            onCustomizeClick = { prod ->
                                val defaultVar = prod.variants.find { it.isDefault } ?: prod.variants.firstOrNull()
                                val basePrice = MenuEngine.resolveProductPrice(
                                    product = prod,
                                    variant = defaultVar,
                                    branchId = branch.id,
                                    channel = effectiveChannel,
                                    customerTier = selectedCustomer?.tier,
                                    rules = priceRules
                                )
                                customizingItem = OrderEngine.PosCartItem(
                                    product = prod,
                                    selectedVariant = defaultVar,
                                    baseResolvedPrice = basePrice,
                                    quantity = 1
                                )
                            }
                        )
                    }

                    // Compact Bottom Ticket Summary & Actions
                    Surface(
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${cartItems.sumOf { it.quantity }} Items • ${selectedOrderType.titleEn}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Customer: ${selectedCustomer?.name ?: "Walk-in"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.clickable { showCustomerModal = true }
                                    )
                                }
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", financials.totalPayable)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                RestoButton(
                                    onClick = {
                                        if (cartItems.isNotEmpty()) {
                                            val (heldOrder, heldItems) = buildCurrentOrderAndItems(OrderStatus.HELD, emptyList())
                                            onHoldOrder(heldOrder, heldItems)
                                            resetPosTicket()
                                        }
                                    },
                                    variant = RestoButtonVariant.OUTLINE,
                                    enabled = cartItems.isNotEmpty(),
                                    modifier = Modifier.weight(0.8f)
                                ) {
                                    Text(Translations.get("pos_hold_order", language))
                                }

                                RestoButton(
                                    onClick = { showPaymentModal = true },
                                    variant = RestoButtonVariant.PRIMARY,
                                    enabled = cartItems.isNotEmpty(),
                                    modifier = Modifier.weight(1.4f),
                                    testTag = "pos_tender_button"
                                ) {
                                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(Translations.get("pos_tender_mixed", language))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Interactive POS Modals ---

    // 1. Item Customization Modal
    ItemCustomizationModal(
        visible = customizingItem != null,
        cartItem = customizingItem,
        modifierGroups = modifierGroups,
        addons = addons,
        language = language,
        onConfirm = { customized ->
            val exists = cartItems.any { it.uid == customized.uid }
            cartItems = if (exists) {
                cartItems.map { if (it.uid == customized.uid) customized else it }
            } else {
                cartItems + customized
            }
            customizingItem = null
        },
        onDismiss = { customizingItem = null }
    )

    // 2. Multi-Method & Mixed Tender Payment Modal
    PaymentTenderModal(
        visible = showPaymentModal,
        orderTotal = financials.totalPayable,
        branchId = branch.id,
        cashierId = user.id,
        shiftId = currentShift?.id,
        language = language,
        onCompleteOrderPayment = { payments ->
            val (completedOrder, items) = buildCurrentOrderAndItems(OrderStatus.PREPARING, payments)
            onPlaceCompleteOrder(completedOrder, items, payments)
            showPaymentModal = false
            receiptPreviewOrder = completedOrder
            resetPosTicket()
        },
        onDismiss = { showPaymentModal = false }
    )

    // 3. Split Bill Modal
    val currentDraftOrderForSplit = remember(cartItems, financials) {
        val (draftOrd, draftItems) = buildCurrentOrderAndItems(OrderStatus.DRAFT, emptyList())
        draftOrd to draftItems
    }
    SplitBillModal(
        visible = showSplitBillModal,
        order = currentDraftOrderForSplit.first,
        items = currentDraftOrderForSplit.second,
        language = language,
        onDismiss = { showSplitBillModal = false }
    )

    // 4. Held Orders & Central Order Operations Manager Modal
    OrderOperationsManagerModal(
        visible = showOrdersManagerModal,
        orders = orders,
        tables = tables,
        language = language,
        initialTab = ordersManagerInitialTab,
        onRecallOrderToPos = { recalled ->
            recallOrderIntoCart(recalled)
            showOrdersManagerModal = false
        },
        onTransitionState = { ordId, targetState, reason ->
            if (targetState == OrderStatus.CANCELLED) {
                requirePermissionOrApproval(
                    OrderEngine.PosSensitiveAction.CANCEL_ORDER,
                    "Cancel Order $ordId"
                ) {
                    onTransitionOrderState(ordId, targetState, reason)
                }
            } else {
                onTransitionOrderState(ordId, targetState, reason)
            }
        },
        onRefundOrReturn = { ordId, isReturn, amount, reason ->
            val action = if (isReturn) OrderEngine.PosSensitiveAction.RETURN_ITEMS else OrderEngine.PosSensitiveAction.REFUND_ORDER
            requirePermissionOrApproval(action, "${if (isReturn) "Return" else "Refund"} Order $ordId") {
                onRefundOrReturnOrder(ordId, isReturn, amount, reason)
            }
        },
        onMergeOrders = onMergeOrders,
        onTransferTable = onTransferTable,
        onPrintReceipt = { ord ->
            showOrdersManagerModal = false
            receiptPreviewOrder = ord
        },
        onDismiss = { showOrdersManagerModal = false }
    )

    // 5. Cashier Shift & Cash Drawer Modal
    CashierShiftModal(
        visible = showShiftModal,
        shift = currentShift,
        language = language,
        onOpenShift = onOpenShift,
        onRecordMovement = onRecordCashMovement,
        onCloseShift = onCloseShift,
        onDismiss = { showShiftModal = false }
    )

    // 6. Customer Assignment Modal
    CustomerAssignmentModal(
        visible = showCustomerModal,
        customers = customers,
        selectedCustomer = selectedCustomer,
        language = language,
        onSelectCustomer = { cust ->
            selectedCustomer = cust
            if (cust != null) {
                customerPhoneInput = cust.phone
            }
        },
        onCreateCustomer = onSaveCustomer,
        onDismiss = { showCustomerModal = false }
    )

    // 7. Thermal Receipt Print Preview Modal
    ReceiptPreviewModal(
        visible = receiptPreviewOrder != null,
        company = company,
        branch = branch,
        order = receiptPreviewOrder,
        language = language,
        onPrintConfirmed = {
            onShowFeedback("Receipt ${receiptPreviewOrder?.orderNumber} sent to thermal printer.")
        },
        onDismiss = { receiptPreviewOrder = null }
    )

    // 8. Manager Override Security Approval Modal
    ManagerApprovalModal(
        visible = pendingSecurityActionTitle != null,
        actionTitle = pendingSecurityActionTitle ?: "",
        onApproved = { approver ->
            pendingSecurityCallback?.invoke(approver)
            pendingSecurityActionTitle = null
            pendingSecurityCallback = null
        },
        onDismiss = {
            pendingSecurityActionTitle = null
            pendingSecurityCallback = null
        }
    )
}

@Composable
private fun PosOperationalHeaderBar(
    selectedOrderType: OrderType,
    onSelectOrderType: (OrderType) -> Unit,
    currentShift: CashierShift?,
    heldOrdersCount: Int,
    totalOrdersCount: Int,
    isOfflineMode: Boolean,
    offlineQueueCount: Int,
    language: AppLanguage,
    onOpenShiftModal: () -> Unit,
    onOpenHeldOrders: () -> Unit,
    onOpenOrderManager: () -> Unit,
    onToggleOffline: (Boolean) -> Unit,
    onSyncNow: () -> Unit
) {
    val posModes = listOf(
        OrderType.DINE_IN,
        OrderType.TAKEOUT,
        OrderType.PICKUP,
        OrderType.DELIVERY,
        OrderType.PHONE_ORDER
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: 5 POS Modes
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(posModes) { mode ->
                    val isSelected = selectedOrderType == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectOrderType(mode) },
                        label = {
                            Text(
                                text = if (language == AppLanguage.ARABIC) mode.titleAr else mode.titleEn,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            val icon = when (mode) {
                                OrderType.DINE_IN -> Icons.Default.Restaurant
                                OrderType.TAKEOUT -> Icons.Default.TakeoutDining
                                OrderType.PICKUP -> Icons.Default.Storefront
                                OrderType.DELIVERY -> Icons.Default.DeliveryDining
                                OrderType.PHONE_ORDER -> Icons.Default.PhoneInTalk
                                else -> Icons.Default.QrCode
                            }
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("pos_mode_${mode.name.lowercase()}")
                    )
                }
            }

            // Right: Operational Actions (Held Orders, Order Engine Manager, Cashier Shift, Offline Queue)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Held Orders Button
                OutlinedButton(
                    onClick = onOpenHeldOrders,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("pos_held_orders_button")
                ) {
                    Icon(Icons.Default.PauseCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${Translations.get("pos_held_orders", language)} ($heldOrdersCount)", fontSize = 12.sp)
                }

                // Central Order Manager Button
                OutlinedButton(
                    onClick = onOpenOrderManager,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("pos_order_manager_button")
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Orders ($totalOrdersCount)", fontSize = 12.sp)
                }

                // Cashier Shift Status Button
                val isShiftOpen = currentShift?.status == ShiftStatus.OPEN
                OutlinedButton(
                    onClick = onOpenShiftModal,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("pos_shift_button")
                ) {
                    Icon(
                        if (isShiftOpen) Icons.Default.PointOfSale else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isShiftOpen) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isShiftOpen) "Shift $${String.format(Locale.US, "%.0f", currentShift?.expectedCash ?: 0.0)}" else "Shift Closed",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Offline Queue & Sync Toggle
                FilterChip(
                    selected = isOfflineMode,
                    onClick = { onToggleOffline(!isOfflineMode) },
                    label = {
                        Text(
                            text = if (isOfflineMode) "Offline (${offlineQueueCount})" else "Online",
                            fontSize = 11.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            if (isOfflineMode) Icons.Default.CloudOff else Icons.Default.CloudDone,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    modifier = Modifier.testTag("pos_offline_toggle")
                )

                if (offlineQueueCount > 0 && !isOfflineMode) {
                    IconButton(
                        onClick = onSyncNow,
                        modifier = Modifier.size(32.dp).testTag("pos_sync_queue_button")
                    ) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "Sync Offline Queue",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogSearchAndBarcodeBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    barcodeQuery: String,
    onBarcodeChange: (String) -> Unit,
    onBarcodeSubmit: () -> Unit,
    language: AppLanguage
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1.2f)) {
            RestoTextInput(
                value = searchQuery,
                onValueChange = onSearchChange,
                label = if (language == AppLanguage.ARABIC) "بحث عن صنف أو SKU" else "Search Products, Name or SKU...",
                placeholder = "Type burger, truffle, BUR-001...",
                leadingIcon = Icons.Default.Search
            )
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                RestoTextInput(
                    value = barcodeQuery,
                    onValueChange = onBarcodeChange,
                    label = Translations.get("pos_barcode_scan", language),
                    placeholder = "6281001001",
                    leadingIcon = Icons.Default.QrCodeScanner
                )
            }
            RestoButton(
                onClick = onBarcodeSubmit,
                variant = RestoButtonVariant.SECONDARY,
                testTag = "barcode_scan_submit"
            ) {
                Text("Scan")
            }
        }
    }
}

@Composable
private fun PosCategoryFilterRow(
    categories: List<Category>,
    selectedCategoryId: String,
    language: AppLanguage,
    onSelectCategory: (String) -> Unit
) {
    val allOption = "ALL" to (if (language == AppLanguage.ARABIC) "جميع الأصناف" else "All Categories")
    val categoryPairs = listOf(allOption) + categories.sortedBy { it.sortOrder }.map {
        it.id to (if (language == AppLanguage.ARABIC) it.nameAr else it.nameEn)
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categoryPairs) { (id, label) ->
            RestoFilterChip(
                selected = selectedCategoryId == id,
                onClick = { onSelectCategory(id) },
                label = label
            )
        }
    }
}

@Composable
private fun PosTouchProductGrid(
    products: List<Product>,
    branchId: String,
    channel: SalesChannel,
    customerTier: String?,
    priceRules: List<ChannelPriceRule>,
    language: AppLanguage,
    onProductClick: (Product) -> Unit,
    onCustomizeClick: (Product) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 165.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(products, key = { it.id }) { prod ->
            val defaultVar = prod.variants.find { it.isDefault } ?: prod.variants.firstOrNull()
            val resolvedPrice = MenuEngine.resolveProductPrice(
                product = prod,
                variant = defaultVar,
                branchId = branchId,
                channel = channel,
                customerTier = customerTier,
                rules = priceRules
            )
            val isOutOfStock = prod.currentStock <= 0.0 || !prod.isAvailable
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (isOutOfStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onProductClick(prod) }
                    .testTag("pos_product_${prod.id}")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        RestoBadge(
                            text = prod.sku,
                            variant = BadgeVariant.NEUTRAL
                        )
                        if (prod.variants.isNotEmpty() || prod.modifierGroupIds.isNotEmpty() || prod.addonIds.isNotEmpty()) {
                            IconButton(
                                onClick = { onCustomizeClick(prod) },
                                modifier = Modifier.size(26.dp).testTag("customize_${prod.id}")
                            ) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = "Customize Modifiers & Variants",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (language == AppLanguage.ARABIC) prod.nameAr else prod.nameEn,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", resolvedPrice)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isOutOfStock) "86'd" else "${prod.currentStock.toInt()} left",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (prod.currentStock <= prod.reorderPoint) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PosRichOrderTicketPane(
    editingOrderNumber: String?,
    selectedOrderType: OrderType,
    selectedTable: RestaurantTable?,
    tables: List<RestaurantTable>,
    selectedCustomer: Customer?,
    deliveryAddress: String,
    onDeliveryAddressChange: (String) -> Unit,
    customerPhoneInput: String,
    onCustomerPhoneChange: (String) -> Unit,
    orderNotes: String,
    onOrderNotesChange: (String) -> Unit,
    cartItems: List<OrderEngine.PosCartItem>,
    financials: OrderEngine.OrderFinancialSummary,
    couponCodeInput: String,
    onCouponCodeInputChange: (String) -> Unit,
    appliedCoupon: Coupon?,
    onApplyCoupon: () -> Unit,
    onRemoveCoupon: () -> Unit,
    manualDiscountInput: String,
    onManualDiscountInputChange: (String) -> Unit,
    onApplyManualDiscount: (DiscountType) -> Unit,
    applyServiceFee: Boolean,
    onToggleServiceFee: (Boolean) -> Unit,
    language: AppLanguage,
    onSelectTable: (RestaurantTable) -> Unit,
    onOpenCustomerModal: () -> Unit,
    onEditCartItem: (OrderEngine.PosCartItem) -> Unit,
    onUpdateItemQty: (OrderEngine.PosCartItem, Int) -> Unit,
    onClearCart: () -> Unit,
    onHoldOrderClick: () -> Unit,
    onSplitBillClick: () -> Unit,
    onQuickPayCash: () -> Unit,
    onOpenMultiPaymentModal: () -> Unit,
    onPreviewReceipt: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Ticket Header + Customer Assignment + Clear/Print
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (editingOrderNumber != null) "Editing $editingOrderNumber" else Translations.get("pos_order_ticket", language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Mode: ${if (language == AppLanguage.ARABIC) selectedOrderType.titleAr else selectedOrderType.titleEn}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onOpenCustomerModal,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("assign_customer_button")
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = selectedCustomer?.name ?: Translations.get("pos_assign_customer", language),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
                IconButton(onClick = onPreviewReceipt, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Print, contentDescription = "Print Preview", modifier = Modifier.size(18.dp))
                }
                if (cartItems.isNotEmpty()) {
                    IconButton(onClick = onClearCart, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Clear Ticket",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Context-Specific Inputs: Table Selector for Dine-In, Delivery/Phone Fields for Delivery/Phone Order
        when (selectedOrderType) {
            OrderType.DINE_IN -> {
                if (tables.isNotEmpty()) {
                    RestoDropdown(
                        items = tables,
                        selectedItem = selectedTable ?: tables.first(),
                        onItemSelected = onSelectTable,
                        label = Translations.get("pos_select_table", language),
                        itemLabel = { "Table ${it.tableNumber} (${it.section} • ${it.status.name})" }
                    )
                }
            }
            OrderType.DELIVERY, OrderType.PHONE_ORDER -> {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.weight(0.9f)) {
                        RestoTextInput(
                            value = customerPhoneInput,
                            onValueChange = onCustomerPhoneChange,
                            label = "Caller / Recipient Phone",
                            placeholder = "+966 5X..."
                        )
                    }
                    Box(modifier = Modifier.weight(1.1f)) {
                        RestoTextInput(
                            value = deliveryAddress,
                            onValueChange = onDeliveryAddressChange,
                            label = "Delivery / Dispatch Address",
                            placeholder = "District, Street, Building..."
                        )
                    }
                }
            }
            else -> {}
        }

        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(6.dp))

        // Cart Items List
        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Translations.get("pos_no_items", language),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(cartItems, key = { it.uid }) { item ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditCartItem(item) }
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (language == AppLanguage.ARABIC) item.product.nameAr else item.product.nameEn,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val subDetails = buildList {
                                        item.selectedVariant?.let { add(it.nameEn) }
                                        if (item.selectedModifiers.isNotEmpty()) {
                                            add(item.selectedModifiers.joinToString(", ") { it.nameEn })
                                        }
                                        if (item.selectedAddons.isNotEmpty()) {
                                            add(item.selectedAddons.entries.joinToString(", ") { "+${it.value}x ${it.key.nameEn}" })
                                        }
                                    }
                                    if (subDetails.isNotEmpty()) {
                                        Text(
                                            text = subDetails.joinToString(" • "),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (item.notes.isNotBlank()) {
                                        Text(
                                            text = "Note: ${item.notes}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                    if (item.itemDiscountAmount > 0) {
                                        Text(
                                            text = "Line Discount: -$${String.format(Locale.US, "%.2f", item.itemDiscountAmount)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RestoNumberStepper(
                                        value = item.quantity,
                                        onValueChange = { qty -> onUpdateItemQty(item, qty) },
                                        minValue = 0
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$${String.format(Locale.US, "%.2f", item.netTotal)}",
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(6.dp))

        // Promotions, Coupons & Order Notes Compact Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                RestoTextInput(
                    value = couponCodeInput,
                    onValueChange = onCouponCodeInputChange,
                    label = Translations.get("pos_coupon_code", language),
                    placeholder = "WELCOME10, VIP20..."
                )
            }
            if (appliedCoupon == null) {
                RestoButton(
                    onClick = onApplyCoupon,
                    variant = RestoButtonVariant.SECONDARY,
                    enabled = couponCodeInput.isNotBlank()
                ) {
                    Text("Apply")
                }
            } else {
                RestoButton(
                    onClick = onRemoveCoupon,
                    variant = RestoButtonVariant.DANGER
                ) {
                    Text("Remove ${appliedCoupon.code}")
                }
            }

            Box(modifier = Modifier.weight(0.85f)) {
                RestoTextInput(
                    value = manualDiscountInput,
                    onValueChange = onManualDiscountInputChange,
                    label = "Manual Disc %",
                    placeholder = "10"
                )
            }
            OutlinedButton(
                onClick = { onApplyManualDiscount(DiscountType.PERCENTAGE) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("%")
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        RestoTextInput(
            value = orderNotes,
            onValueChange = onOrderNotesChange,
            label = "Order / Kitchen Note",
            placeholder = "Allergies, VIP table instructions..."
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Financial Summary Breakdown
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            FinancialLine(Translations.get("pos_subtotal", language), financials.grossItemsSubtotal)
            if (financials.totalDiscount > 0) {
                FinancialLine(
                    label = "${Translations.get("pos_discount", language)}${appliedCoupon?.let { " (${it.code})" } ?: ""}",
                    amount = -financials.totalDiscount,
                    color = Color(0xFF10B981)
                )
            }
            if (selectedOrderType == OrderType.DINE_IN) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = applyServiceFee,
                            onCheckedChange = onToggleServiceFee,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${Translations.get("pos_service_fee", language)} (5%)", style = MaterialTheme.typography.labelSmall)
                    }
                    Text("$${String.format(Locale.US, "%.2f", financials.serviceFee)}", style = MaterialTheme.typography.labelSmall)
                }
            }
            if (financials.deliveryFee > 0) {
                FinancialLine(Translations.get("pos_delivery_fee", language), financials.deliveryFee)
            }
            FinancialLine(Translations.get("pos_tax", language), financials.taxAmount)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Translations.get("pos_total", language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "$${String.format(Locale.US, "%.2f", financials.totalPayable)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Primary POS Action Buttons: Hold, Split Bill, Quick Cash, Mixed/Card Payment
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RestoButton(
                onClick = onHoldOrderClick,
                variant = RestoButtonVariant.OUTLINE,
                enabled = cartItems.isNotEmpty(),
                modifier = Modifier.weight(0.8f),
                testTag = "hold_order_button"
            ) {
                Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(Translations.get("pos_hold_order", language), fontSize = 12.sp)
            }

            RestoButton(
                onClick = onSplitBillClick,
                variant = RestoButtonVariant.OUTLINE,
                enabled = cartItems.isNotEmpty(),
                modifier = Modifier.weight(0.8f),
                testTag = "split_bill_button"
            ) {
                Icon(Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(Translations.get("pos_split_bill", language), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RestoButton(
                onClick = onQuickPayCash,
                variant = RestoButtonVariant.SECONDARY,
                enabled = cartItems.isNotEmpty(),
                modifier = Modifier.weight(1f),
                testTag = "pay_cash_button"
            ) {
                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(Translations.get("pos_tender_cash", language), fontSize = 12.sp)
            }

            RestoButton(
                onClick = onOpenMultiPaymentModal,
                variant = RestoButtonVariant.PRIMARY,
                enabled = cartItems.isNotEmpty(),
                modifier = Modifier.weight(1.25f),
                testTag = "pay_card_button"
            ) {
                Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(Translations.get("pos_tender_mixed", language), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun FinancialLine(
    label: String,
    amount: Double,
    color: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = color)
        Text(
            text = if (amount < 0) "-$${String.format(Locale.US, "%.2f", -amount)}" else "$${String.format(Locale.US, "%.2f", amount)}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}
