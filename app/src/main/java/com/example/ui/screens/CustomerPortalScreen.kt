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
import com.example.core.engine.CustomerCrmEngine
import com.example.core.i18n.AppLanguage
import com.example.designsystem.*
import com.example.domain.model.*
import java.util.*

private enum class CustomerPortalTab(val titleEn: String, val titleAr: String) {
    ORDER_ONLINE("Online Menu & Cart", "الطلب الإلكتروني والسلة"),
    ORDER_TRACKING("Live Tracking & History", "تتبع الطلبات والسجل"),
    PROFILE_ADDRESSES("CRM Profile, OTP & Addresses", "ملف العميل والعناوين"),
    LOYALTY_COUPONS("Loyalty Tiers & Coupons", "نقاط الولاء والكوبونات"),
    REVIEWS_COMPLAINTS("Reviews & Support Tickets", "التقييمات والشكاوى")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPortalScreen(
    company: Company,
    branch: Branch,
    branches: List<Branch>,
    activeCustomer: Customer?,
    customers: List<Customer>,
    products: List<Product>,
    categories: List<Category>,
    coupons: List<Coupon>,
    orders: List<Order>,
    addresses: List<CustomerAddress>,
    loyaltyRewards: List<LoyaltyReward>,
    reviews: List<CustomerReview>,
    complaints: List<CustomerComplaint>,
    deliveryZones: List<DeliveryZone>,
    language: AppLanguage,
    onSelectBranch: (Branch) -> Unit,
    onSelectCustomer: (Customer) -> Unit,
    onLoginWithOtp: (name: String, phone: String, email: String, otpCode: String) -> Unit,
    onToggleFavorite: (customerId: String, productId: String) -> Unit,
    onSaveAddress: (CustomerAddress) -> Unit,
    onRedeemReward: (customerId: String, rewardId: String, onRedeemed: (Double) -> Unit) -> Unit,
    onPlaceCustomerOrder: (Order, List<OrderItem>, List<Payment>) -> Unit,
    onSubmitReview: (CustomerReview) -> Unit,
    onModerateReview: (String, ReviewModerationStatus, String?) -> Unit,
    onSubmitComplaint: (CustomerComplaint) -> Unit,
    onResolveComplaint: (String, ComplaintStatus, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = language == AppLanguage.ARABIC
    var selectedTab by remember { mutableStateOf(CustomerPortalTab.ORDER_ONLINE) }

    val customer = activeCustomer ?: customers.firstOrNull() ?: Customer(
        id = "cust_guest",
        companyId = company.id,
        name = "Layla Al-Mansoor",
        phone = "+971-50-555-0101",
        email = "layla@example.com",
        loyaltyPoints = 480,
        tier = "GOLD"
    )

    val customerAddresses = remember(addresses, customer.id) {
        addresses.filter { it.customerId == customer.id }
    }
    val defaultAddress = customerAddresses.find { it.isDefault } ?: customerAddresses.firstOrNull()

    // Online ordering state
    var selectedOrderType by remember { mutableStateOf(OrderType.DELIVERY) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var cartQuantities by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var couponCodeInput by remember { mutableStateOf("") }
    var appliedRewardCredit by remember { mutableStateOf(0.0) }
    var selectedPaymentMethod by remember { mutableStateOf<PaymentMethod>(PaymentMethod.CREDIT_CARD) }

    // OTP Login modal state
    var otpName by remember { mutableStateOf("") }
    var otpPhone by remember { mutableStateOf("+971-50-") }
    var otpEmail by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("4829") }

    // New address state
    var addrLabel by remember { mutableStateOf("Home") }
    var addrStreet by remember { mutableStateOf("") }
    var addrBuilding by remember { mutableStateOf("") }
    var addrNotes by remember { mutableStateOf("") }

    // Review & Complaint form state
    var reviewRating by remember { mutableStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }
    var complaintCategory by remember { mutableStateOf("LATE_DELIVERY") }
    var complaintPriority by remember { mutableStateOf(ComplaintPriority.HIGH) }
    var complaintSubject by remember { mutableStateOf("") }
    var complaintDescription by remember { mutableStateOf("") }

    val crmMetrics: CustomerCrmMetrics = remember(customer, orders, products) {
        CustomerCrmEngine.computeCrmMetrics(customer, orders, products)
    }
    val currentTierLevel: LoyaltyTierLevel = remember(customer.loyaltyPoints) {
        CustomerCrmEngine.resolveLoyaltyTier(customer.loyaltyPoints)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("customer_portal_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with Active Customer & Loyalty Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isAr) "منصة طلبات العملاء وإدارة علاقات العملاء (Phase 7)" else "Customer Ordering Platform & CRM (Phase 7)",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${customer.name} (${customer.phone}) • Tier: ${currentTierLevel.titleEn} • ${customer.loyaltyPoints} pts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RestoBadge(
                    text = "${currentTierLevel.titleEn} (${currentTierLevel.earnMultiplier}x)",
                    variant = BadgeVariant.WARNING
                )
                RestoBadge(
                    text = "${customer.loyaltyPoints} PTS",
                    variant = BadgeVariant.SUCCESS
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
            CustomerPortalTab.entries.forEach { tab ->
                FilterChip(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    label = { Text(if (isAr) tab.titleAr else tab.titleEn) },
                    modifier = Modifier.testTag("customer_tab_${tab.name.lowercase()}")
                )
            }
        }

        when (selectedTab) {
            CustomerPortalTab.ORDER_ONLINE -> {
                val filteredProducts = products.filter { prod ->
                    prod.isAvailable && prod.isSellable &&
                        (selectedCategoryId == null || prod.categoryId == selectedCategoryId) &&
                        (!showFavoritesOnly || prod.id in customer.favoriteProductIds)
                }

                val cartOrderItems = cartQuantities.mapNotNull { (prodId, qty) ->
                    val prod = products.find { it.id == prodId } ?: return@mapNotNull null
                    OrderItem(
                        id = "ci_${prod.id}",
                        orderId = "draft_cust",
                        productId = prod.id,
                        productNameEn = prod.nameEn,
                        productNameAr = prod.nameAr,
                        quantity = qty,
                        unitPrice = prod.price,
                        totalPrice = prod.price * qty
                    )
                }
                val subtotal = cartOrderItems.sumOf { it.totalPrice }
                val matchedCoupon = coupons.find { it.code.equals(couponCodeInput.trim(), ignoreCase = true) }
                val couponResult = remember(matchedCoupon, subtotal, branch.id, customer, cartOrderItems, products) {
                    if (matchedCoupon == null) null
                    else {
                        val catIds = cartOrderItems.mapNotNull { ci -> products.find { p -> p.id == ci.productId }?.categoryId }.toSet()
                        val prodIds = cartOrderItems.map { it.productId }.toSet()
                        CustomerCrmEngine.validateCouponWithRestrictions(
                            coupon = matchedCoupon,
                            orderSubtotal = subtotal,
                            branchId = branch.id,
                            customer = customer,
                            cartCategoryIds = catIds,
                            cartProductIds = prodIds
                        )
                    }
                }
                val couponDiscount = (couponResult as? CustomerCrmEngine.CouponValidationResult.Valid)?.discountAmount ?: 0.0
                val totalDiscount = (couponDiscount + appliedRewardCredit).coerceAtMost(subtotal)
                val activeZone = deliveryZones.find { it.id == defaultAddress?.deliveryZoneId } ?: deliveryZones.firstOrNull()
                val deliveryFee = if (selectedOrderType == OrderType.DELIVERY && subtotal > 0) (activeZone?.deliveryFee ?: 3.50) else 0.0
                val taxAmount = ((subtotal - totalDiscount).coerceAtLeast(0.0)) * 0.10
                val grandTotal = (subtotal - totalDiscount).coerceAtLeast(0.0) + taxAmount + deliveryFee

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isAr) "طريقة الاستلام والفرع" else "Fulfillment Channel & Branch",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(OrderType.DELIVERY, OrderType.PICKUP, OrderType.DINE_IN).forEach { type ->
                                            FilterChip(
                                                selected = selectedOrderType == type,
                                                onClick = { selectedOrderType = type },
                                                label = { Text(type.titleEn) },
                                                modifier = Modifier.testTag("cust_order_type_${type.name.lowercase()}")
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    branches.forEach { b ->
                                        AssistChip(
                                            onClick = { onSelectBranch(b) },
                                            label = { Text(if (isAr) b.nameAr else b.nameEn) },
                                            leadingIcon = {
                                                Icon(
                                                    if (b.id == branch.id) Icons.Default.CheckCircle else Icons.Default.Store,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        )
                                    }
                                    FilterChip(
                                        selected = showFavoritesOnly,
                                        onClick = { showFavoritesOnly = !showFavoritesOnly },
                                        label = { Text("Favorites (${customer.favoriteProductIds.size})") },
                                        leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        modifier = Modifier.testTag("cust_filter_favorites")
                                    )
                                }

                                if (selectedOrderType == OrderType.DELIVERY && defaultAddress != null) {
                                    Text(
                                        text = "Delivering to [${defaultAddress.label}]: ${defaultAddress.streetAddress}, ${defaultAddress.buildingAndFloor} (${defaultAddress.deliveryNotes})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // Category chips
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedCategoryId == null,
                                onClick = { selectedCategoryId = null },
                                label = { Text(if (isAr) "جميع الأصناف" else "All Categories") }
                            )
                            categories.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategoryId == cat.id,
                                    onClick = { selectedCategoryId = cat.id },
                                    label = { Text(if (isAr) cat.nameAr else cat.nameEn) }
                                )
                            }
                        }
                    }

                    // Menu Items
                    items(filteredProducts, key = { it.id }) { prod ->
                        val qty = cartQuantities[prod.id] ?: 0
                        val isFav = prod.id in customer.favoriteProductIds
                        RestoCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = if (isAr) prod.nameAr else prod.nameEn,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = prod.sku,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = if (isAr) prod.descriptionAr else prod.descriptionEn,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$${String.format(Locale.US, "%.2f", prod.price)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(
                                        onClick = { onToggleFavorite(customer.id, prod.id) },
                                        modifier = Modifier.testTag("fav_btn_${prod.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Favorite",
                                            tint = if (isFav) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    if (qty > 0) {
                                        OutlinedButton(
                                            onClick = {
                                                cartQuantities = if (qty <= 1) cartQuantities - prod.id else cartQuantities + (prod.id to (qty - 1))
                                            }
                                        ) { Text("-") }
                                        Text("$qty", fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            cartQuantities = cartQuantities + (prod.id to (qty + 1))
                                        },
                                        modifier = Modifier.testTag("cust_add_cart_${prod.id}")
                                    ) {
                                        Text(if (qty == 0) "Add" else "+")
                                    }
                                }
                            }
                        }
                    }

                    // Checkout Summary Card
                    if (cartOrderItems.isNotEmpty()) {
                        item {
                            RestoCard {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "Customer Cart & Checkout (${cartOrderItems.sumOf { it.quantity }} items)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = couponCodeInput,
                                            onValueChange = { couponCodeInput = it.uppercase() },
                                            label = { Text("Promo Code (WELCOME10, VIP20)") },
                                            singleLine = true,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("cust_coupon_input")
                                        )
                                        listOf(PaymentMethod.CREDIT_CARD, PaymentMethod.CASH, PaymentMethod.DIGITAL_WALLET).forEach { pm ->
                                            FilterChip(
                                                selected = selectedPaymentMethod == pm,
                                                onClick = { selectedPaymentMethod = pm },
                                                label = { Text(pm.title) }
                                            )
                                        }
                                    }

                                    when (couponResult) {
                                        is CustomerCrmEngine.CouponValidationResult.Valid -> {
                                            Text(
                                                text = "Promo ${couponResult.coupon.code} applied: -$${String.format(Locale.US, "%.2f", couponResult.discountAmount)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        is CustomerCrmEngine.CouponValidationResult.Invalid -> {
                                            Text(
                                                text = if (isAr) couponResult.reasonAr else couponResult.reasonEn,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        null -> {}
                                    }

                                    HorizontalDivider()
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                        Text("Subtotal:")
                                        Text("$${String.format(Locale.US, "%.2f", subtotal)}")
                                    }
                                    if (totalDiscount > 0) {
                                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                            Text("Discounts & Loyalty Credit:")
                                            Text("-$${String.format(Locale.US, "%.2f", totalDiscount)}", color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    if (deliveryFee > 0) {
                                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                            Text("Delivery Fee (${activeZone?.nameEn ?: "Standard"}):")
                                            Text("$${String.format(Locale.US, "%.2f", deliveryFee)}")
                                        }
                                    }
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                        Text("VAT (10%):")
                                        Text("$${String.format(Locale.US, "%.2f", taxAmount)}")
                                    }
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                        Text("Grand Total:", fontWeight = FontWeight.Bold)
                                        Text("$${String.format(Locale.US, "%.2f", grandTotal)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    }

                                    RestoButton(
                                        onClick = {
                                            val orderId = "ord_cust_${UUID.randomUUID().toString().take(6)}"
                                            val orderNum = "ORD-WEB-${1050 + orders.size}"
                                            val finalItems = cartOrderItems.map { it.copy(orderId = orderId) }
                                            val isCod = selectedPaymentMethod == PaymentMethod.CASH && selectedOrderType == OrderType.DELIVERY
                                            val payment = Payment(
                                                id = "pay_${UUID.randomUUID().toString().take(6)}",
                                                orderId = orderId,
                                                branchId = branch.id,
                                                amount = grandTotal,
                                                paymentMethod = selectedPaymentMethod,
                                                referenceNumber = "ONLINE-${(1000..9999).random()}",
                                                status = if (isCod) "PENDING_COD" else "SUCCESS",
                                                tenderedAmount = grandTotal
                                            )
                                            val newOrder = Order(
                                                id = orderId,
                                                branchId = branch.id,
                                                orderNumber = orderNum,
                                                orderType = selectedOrderType,
                                                customerId = customer.id,
                                                customerName = customer.name,
                                                customerPhone = customer.phone,
                                                channel = SalesChannel.CUSTOMER_APP,
                                                deliveryAddress = defaultAddress?.let { "${it.streetAddress}, ${it.buildingAndFloor}" } ?: "Olaya Main St",
                                                status = OrderStatus.PREPARING,
                                                items = finalItems,
                                                payments = listOf(payment),
                                                subtotal = subtotal,
                                                taxAmount = taxAmount,
                                                discountAmount = totalDiscount,
                                                couponCode = couponCodeInput.ifBlank { null },
                                                deliveryFee = deliveryFee,
                                                totalAmount = grandTotal,
                                                paymentStatus = if (isCod) "UNPAID" else "PAID"
                                            )
                                            onPlaceCustomerOrder(newOrder, finalItems, listOf(payment))
                                            cartQuantities = emptyMap()
                                            appliedRewardCredit = 0.0
                                            selectedTab = CustomerPortalTab.ORDER_TRACKING
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        testTag = "cust_submit_order_btn"
                                    ) {
                                        Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Place Online Order ($${String.format(Locale.US, "%.2f", grandTotal)})")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            CustomerPortalTab.ORDER_TRACKING -> {
                val custOrders = orders.filter { it.customerId == customer.id || it.customerPhone == customer.phone }.ifEmpty { orders }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(custOrders, key = { it.id }) { ord ->
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${ord.orderNumber} • ${ord.orderType.titleEn}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    RestoBadge(
                                        text = ord.status.titleEn,
                                        variant = when (ord.status) {
                                            OrderStatus.READY, OrderStatus.COMPLETED -> BadgeVariant.SUCCESS
                                            OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY -> BadgeVariant.WARNING
                                            else -> BadgeVariant.INFO
                                        }
                                    )
                                }
                                Text(
                                    text = ord.items.joinToString(" • ") { "${it.quantity}x ${it.productNameEn}" },
                                    style = MaterialTheme.typography.bodySmall
                                )
                                LinearProgressIndicator(
                                    progress = {
                                        when (ord.status) {
                                            OrderStatus.DRAFT, OrderStatus.PENDING -> 0.25f
                                            OrderStatus.PREPARING -> 0.6f
                                            OrderStatus.READY, OrderStatus.OUT_FOR_DELIVERY -> 0.85f
                                            OrderStatus.SERVED, OrderStatus.COMPLETED -> 1.0f
                                            else -> 0.1f
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Total: $${String.format(Locale.US, "%.2f", ord.totalAmount)} (${ord.paymentStatus})",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            val reorderMap = mutableMapOf<String, Int>()
                                            ord.items.forEach { item ->
                                                reorderMap[item.productId] = (reorderMap[item.productId] ?: 0) + item.quantity
                                            }
                                            cartQuantities = reorderMap
                                            selectedTab = CustomerPortalTab.ORDER_ONLINE
                                        },
                                        modifier = Modifier.testTag("reorder_btn_${ord.id}")
                                    ) {
                                        Text("Reorder Items")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            CustomerPortalTab.PROFILE_ADDRESSES -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("CRM Customer Behavior Intelligence", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    customers.forEach { c ->
                                        FilterChip(
                                            selected = c.id == customer.id,
                                            onClick = { onSelectCustomer(c) },
                                            label = { Text("${c.name} (${c.tier})") }
                                        )
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Orders: ${crmMetrics.orderCount}", fontWeight = FontWeight.SemiBold)
                                    Text("Total Spend: $${String.format(Locale.US, "%.2f", crmMetrics.totalSpend)}", fontWeight = FontWeight.SemiBold)
                                    Text("Avg Order: $${String.format(Locale.US, "%.2f", crmMetrics.averageOrderValue)}", fontWeight = FontWeight.SemiBold)
                                }
                                if (crmMetrics.favoriteProductNames.isNotEmpty()) {
                                    Text(
                                        text = "Top Ordered Products: ${crmMetrics.favoriteProductNames.joinToString(", ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // OTP Login / Registration Form
                    item {
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("OTP-Ready Customer Authentication / Registration", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = otpName,
                                        onValueChange = { otpName = it },
                                        label = { Text("Full Name") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = otpPhone,
                                        onValueChange = { otpPhone = it },
                                        label = { Text("Phone Number") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = otpCode,
                                        onValueChange = { otpCode = it },
                                        label = { Text("4-Digit SMS OTP") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = { onLoginWithOtp(otpName, otpPhone, otpEmail, otpCode) },
                                        modifier = Modifier.testTag("cust_otp_verify_btn")
                                    ) {
                                        Text("Verify OTP & Sign In")
                                    }
                                }
                            }
                        }
                    }

                    // Delivery Addresses
                    item {
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Saved Delivery Addresses (${customerAddresses.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                customerAddresses.forEach { addr ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("${addr.label} • ${addr.streetAddress}, ${addr.buildingAndFloor}", fontWeight = FontWeight.Bold)
                                                Text("Notes: ${addr.deliveryNotes}", style = MaterialTheme.typography.bodySmall)
                                            }
                                            if (addr.isDefault) {
                                                RestoBadge(text = "DEFAULT", variant = BadgeVariant.SUCCESS)
                                            }
                                        }
                                    }
                                }
                                HorizontalDivider()
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = addrLabel,
                                        onValueChange = { addrLabel = it },
                                        label = { Text("Label (Home/Office)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = addrStreet,
                                        onValueChange = { addrStreet = it },
                                        label = { Text("Street / Area") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = addrNotes,
                                        onValueChange = { addrNotes = it },
                                        label = { Text("Delivery Instructions / Gate Code") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = {
                                            if (addrStreet.isNotBlank()) {
                                                onSaveAddress(
                                                    CustomerAddress(
                                                        id = "addr_${UUID.randomUUID().toString().take(6)}",
                                                        customerId = customer.id,
                                                        label = addrLabel,
                                                        streetAddress = addrStreet,
                                                        buildingAndFloor = addrBuilding.ifBlank { "Tower A" },
                                                        deliveryNotes = addrNotes,
                                                        isDefault = true
                                                    )
                                                )
                                                addrStreet = ""
                                                addrNotes = ""
                                            }
                                        },
                                        modifier = Modifier.testTag("save_customer_address_btn")
                                    ) {
                                        Text("Save Address")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            CustomerPortalTab.LOYALTY_COUPONS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Loyalty Program • ${currentTierLevel.titleEn} (${customer.loyaltyPoints} pts)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${currentTierLevel.perksEn} • ${crmMetrics.expiryRuleDescription}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    items(loyaltyRewards, key = { it.id }) { reward ->
                        val canRedeem = customer.loyaltyPoints >= reward.pointsCost
                        RestoCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(if (isAr) reward.titleAr else reward.titleEn, fontWeight = FontWeight.Bold)
                                    Text("${reward.pointsCost} pts • Value: $${reward.discountValue} • Expiry: ${reward.expiryDaysAfterRedeem} days", style = MaterialTheme.typography.bodySmall)
                                }
                                Button(
                                    onClick = {
                                        onRedeemReward(customer.id, reward.id) { credit ->
                                            appliedRewardCredit += credit
                                        }
                                    },
                                    enabled = canRedeem,
                                    modifier = Modifier.testTag("redeem_reward_${reward.id}")
                                ) {
                                    Text("Redeem")
                                }
                            }
                        }
                    }

                    item {
                        Text("Active Promotional Coupons", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    items(coupons, key = { it.code }) { cpn ->
                        RestoCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("${cpn.code} — ${if (isAr) cpn.titleAr else cpn.titleEn}", fontWeight = FontWeight.Bold)
                                    Text("Min Order: $${cpn.minOrderAmount} • Valid: ${cpn.validFromDate} to ${cpn.validUntilDate}", style = MaterialTheme.typography.bodySmall)
                                }
                                OutlinedButton(
                                    onClick = {
                                        couponCodeInput = cpn.code
                                        selectedTab = CustomerPortalTab.ORDER_ONLINE
                                    }
                                ) {
                                    Text("Apply Code")
                                }
                            }
                        }
                    }
                }
            }

            CustomerPortalTab.REVIEWS_COMPLAINTS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Rate Order / Product & Moderation Queue", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    (1..5).forEach { star ->
                                        FilterChip(
                                            selected = reviewRating == star,
                                            onClick = { reviewRating = star },
                                            label = { Text("$star ★") }
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = reviewComment,
                                        onValueChange = { reviewComment = it },
                                        label = { Text("Share your feedback...") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = {
                                            if (reviewComment.isNotBlank()) {
                                                onSubmitReview(
                                                    CustomerReview(
                                                        id = "rev_${UUID.randomUUID().toString().take(6)}",
                                                        orderId = orders.firstOrNull()?.id ?: "ord_01",
                                                        orderNumber = orders.firstOrNull()?.orderNumber ?: "ORD-1001",
                                                        customerId = customer.id,
                                                        customerName = customer.name,
                                                        rating = reviewRating,
                                                        comment = reviewComment
                                                    )
                                                )
                                                reviewComment = ""
                                            }
                                        },
                                        modifier = Modifier.testTag("submit_review_btn")
                                    ) {
                                        Text("Post Review")
                                    }
                                }
                            }
                        }
                    }

                    items(reviews, key = { it.id }) { rev ->
                        RestoCard {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("${rev.customerName} • ${rev.rating}/5 ★ (${rev.orderNumber})", fontWeight = FontWeight.Bold)
                                    Text(rev.comment, style = MaterialTheme.typography.bodySmall)
                                    rev.managerReply?.let {
                                        Text("Management Reply: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    RestoBadge(text = rev.moderationStatus.titleEn, variant = BadgeVariant.INFO)
                                    if (rev.moderationStatus != ReviewModerationStatus.APPROVED) {
                                        OutlinedButton(onClick = { onModerateReview(rev.id, ReviewModerationStatus.APPROVED, "Thank you for dining with us!") }) {
                                            Text("Approve")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Log Customer Support / Complaint Ticket", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = complaintSubject,
                                        onValueChange = { complaintSubject = it },
                                        label = { Text("Ticket Subject") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = complaintDescription,
                                        onValueChange = { complaintDescription = it },
                                        label = { Text("Issue Details") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        ComplaintPriority.entries.forEach { pri ->
                                            FilterChip(
                                                selected = complaintPriority == pri,
                                                onClick = { complaintPriority = pri },
                                                label = { Text(pri.titleEn) }
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            if (complaintSubject.isNotBlank()) {
                                                onSubmitComplaint(
                                                    CustomerComplaint(
                                                        id = "cmp_${UUID.randomUUID().toString().take(6)}",
                                                        ticketNumber = "TKT-${(1050..9999).random()}",
                                                        branchId = branch.id,
                                                        orderId = orders.firstOrNull()?.id,
                                                        orderNumber = orders.firstOrNull()?.orderNumber,
                                                        customerId = customer.id,
                                                        customerName = customer.name,
                                                        category = complaintCategory,
                                                        priority = complaintPriority,
                                                        subject = complaintSubject,
                                                        description = complaintDescription
                                                    )
                                                )
                                                complaintSubject = ""
                                                complaintDescription = ""
                                            }
                                        },
                                        modifier = Modifier.testTag("submit_complaint_btn")
                                    ) {
                                        Text("Open Ticket")
                                    }
                                }
                            }
                        }
                    }

                    items(complaints, key = { it.id }) { cmp ->
                        RestoCard {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("${cmp.ticketNumber} • ${cmp.subject} (${cmp.customerName})", fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        RestoBadge(text = cmp.priority.titleEn, variant = BadgeVariant.DANGER)
                                        RestoBadge(text = cmp.status.titleEn, variant = BadgeVariant.WARNING)
                                    }
                                }
                                Text(cmp.description, style = MaterialTheme.typography.bodySmall)
                                if (cmp.managementResponse.isNotBlank()) {
                                    Text("Resolution: ${cmp.managementResponse} (${cmp.compensationOffered})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                if (cmp.status != ComplaintStatus.RESOLVED) {
                                    OutlinedButton(
                                        onClick = {
                                            onResolveComplaint(cmp.id, ComplaintStatus.RESOLVED, "Apologized to guest & issued loyalty credit", "200 Loyalty Points")
                                        }
                                    ) {
                                        Text("Resolve Ticket with Compensation")
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
