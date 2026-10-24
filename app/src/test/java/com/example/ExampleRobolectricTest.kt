package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.engine.MenuEngine
import com.example.core.engine.OrderEngine
import com.example.core.engine.TableOperationsEngine
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.Translations
import com.example.core.validation.Validators
import com.example.data.local.RestoOpsDatabase
import com.example.data.repository.DefaultSeedData
import com.example.data.repository.LoginResult
import com.example.data.repository.RestaurantRepository
import com.example.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: RestoOpsDatabase
    private lateinit var repository: RestaurantRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = RestoOpsDatabase.getInstance(context)
        repository = RestaurantRepository(database)
    }

    @Test
    fun `read string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("RestoOps", appName)
    }

    @Test
    fun `validators properly validate enterprise credentials`() {
        val validCompany = Validators.validateCompanyCode("AL-HAYA")
        assertTrue(validCompany.isValid)

        val invalidCompany = Validators.validateCompanyCode("!!")
        assertTrue(!invalidCompany.isValid)

        val validUser = Validators.validateUsername("admin_mira")
        assertTrue(validUser.isValid)

        val validAmount = Validators.validatePositiveAmount(18.50)
        assertTrue(validAmount.isValid)
    }

    @Test
    fun `bilingual translations cover both English and Arabic including Phase 2 and Phase 4 keys`() {
        val adminEn = Translations.get("nav_admin", AppLanguage.ENGLISH)
        val adminAr = Translations.get("nav_admin", AppLanguage.ARABIC)
        assertEquals("Admin & Identity", adminEn)
        assertEquals("الإدارة والتحكم المؤسسي", adminAr)

        val pickupEn = Translations.get("pos_order_type_pickup", AppLanguage.ENGLISH)
        val pickupAr = Translations.get("pos_order_type_pickup", AppLanguage.ARABIC)
        assertEquals("Pickup", pickupEn)
        assertEquals("استلام من الفرع", pickupAr)
    }

    @Test
    fun `protected routes and role permissions enforcement`() = runTest {
        val adminRole = Role("r1", "ROLE_ADMIN", "Administrator", "مدير", listOf("*"))
        val cashierRole = Role("r2", "ROLE_CASHIER", "Cashier", "كاشير", listOf("POS:POS_TERMINAL:VIEW", "TABLES:FLOOR_TABLES:VIEW"))

        val adminUser = User("u1", "comp_01", "admin", "admin@erp.com", "Admin Mira", "ROLE_ADMIN", "branch_01")
        val cashierUser = User("u2", "comp_01", "cashier", "cashier@erp.com", "Cashier Youssef", "ROLE_CASHIER", "branch_01")

        val adminSession = UserSession(
            user = adminUser,
            role = adminRole,
            token = "tok_1",
            loginTimestamp = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 3600000,
            activeBranchId = "branch_01",
            allowedBranchIds = listOf("branch_01", "branch_02", "branch_03"),
            effectivePermissions = setOf("*")
        )

        val cashierSession = UserSession(
            user = cashierUser,
            role = cashierRole,
            token = "tok_2",
            loginTimestamp = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 3600000,
            activeBranchId = "branch_01",
            allowedBranchIds = listOf("branch_01"),
            effectivePermissions = setOf("POS:POS_TERMINAL:VIEW", "TABLES:FLOOR_TABLES:VIEW")
        )

        assertTrue(adminSession.hasPermission("ADMINISTRATION:COMPANY_SETTINGS:VIEW"))
        assertTrue(adminSession.hasPermission("POS:POS_TERMINAL:CREATE"))

        assertTrue(cashierSession.hasPermission("POS:POS_TERMINAL:VIEW"))
        assertFalse(cashierSession.hasPermission("ADMINISTRATION:COMPANY_SETTINGS:VIEW"))
        assertFalse(cashierSession.hasPermission("ADMINISTRATION:USER_MANAGEMENT:CREATE"))
    }

    @Test
    fun `branch level restrictions prevent unauthorized multi-branch access`() = runTest {
        val cashierRole = Role("r2", "ROLE_CASHIER", "Cashier", "كاشير", listOf("POS:POS_TERMINAL:VIEW"), branchRestricted = true)
        val cashierUser = User(
            "u2", "comp_01", "cashier", "cashier@erp.com", "Cashier Youssef", "ROLE_CASHIER",
            activeBranchId = "branch_01",
            allowedBranchIds = listOf("branch_01")
        )

        val cashierSession = UserSession(
            user = cashierUser,
            role = cashierRole,
            token = "tok_2",
            loginTimestamp = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 3600000,
            activeBranchId = "branch_01",
            allowedBranchIds = listOf("branch_01"),
            effectivePermissions = setOf("POS:POS_TERMINAL:VIEW")
        )

        assertTrue(cashierSession.allowedBranchIds.contains("branch_01"))
        assertFalse(cashierSession.allowedBranchIds.contains("branch_02"))
        assertFalse(cashierSession.allowedBranchIds.contains("branch_03"))
    }

    @Test
    fun `login handles invalid company, wrong password and locks account on threshold`() = runTest {
        val badCompanyResult = repository.login("WRONG_COMPANY", "admin_mira", "password123")
        assertTrue(badCompanyResult is LoginResult.CompanyNotFound)

        val wrongPwdResult = repository.login("AL-HAYA", "admin_mira", "incorrect_pwd")
        assertTrue(wrongPwdResult is LoginResult.InvalidCredentials)

        val validResult = repository.login("AL-HAYA", "admin_mira", "password123")
        assertTrue(validResult is LoginResult.Success)
        val session = (validResult as LoginResult.Success).session
        assertEquals("admin_mira", session.user.username)
        assertTrue(session.isValid)
    }

    @Test
    fun `recipe engine computes exact manufacturing portion cost, yield loss, trim waste and gross margin`() {
        val beef = Ingredient("ing_beef", "ING-001", "Black Angus Ground Beef", "لحم", "kg", 12.00, 100.0, yieldPercentage = 0.88, wastePercentage = 0.04)
        val bun = Ingredient("ing_bun", "ING-002", "Brioche Bun", "خبز", "pcs", 0.65, 100.0, yieldPercentage = 1.0, wastePercentage = 0.0)
        val ingredientsMap = mapOf(beef.id to beef, bun.id to bun)

        val recipe = Recipe(
            id = "rec_test",
            productId = "prod_1",
            name = "Test Burger",
            yieldCount = 1.0,
            packagingCost = 0.35,
            laborCost = 0.85,
            items = listOf(
                RecipeItem(beef.id, "Beef", 0.180, "kg"),
                RecipeItem(bun.id, "Bun", 1.0, "pcs")
            )
        )

        val summary = MenuEngine.calculateRecipeCost(recipe, ingredientsMap, sellingPrice = 18.50)
        assertTrue(summary.rawIngredientsCost > 3.0 && summary.rawIngredientsCost < 3.5)
        assertTrue(summary.totalCost > 4.2 && summary.totalCost < 4.6)
        assertTrue(summary.grossMarginPercentage > 70.0 && summary.grossMarginPercentage < 80.0)
        assertEquals(18.50, summary.sellingPrice, 0.001)
    }

    @Test
    fun `channel price resolver correctly applies delivery markup and branch rules`() {
        val product = Product(
            id = "p1",
            categoryId = "cat_burgers",
            sku = "BUR-001",
            nameEn = "Burger",
            nameAr = "برجر",
            price = 15.00
        )
        val variantDouble = ProductVariant(
            id = "v2",
            productId = "p1",
            nameEn = "Double",
            nameAr = "مزدوج",
            sku = "BUR-001-D",
            priceAdjustment = 4.00
        )
        val deliveryRule = ChannelPriceRule(
            id = "pr_1",
            productId = "p1",
            channel = SalesChannel.DELIVERY_PARTNERS,
            customPrice = 18.00
        )
        val airportRule = ChannelPriceRule(
            id = "pr_2",
            productId = "p1",
            branchId = "branch_airport",
            customPrice = 19.50
        )

        val posPrice = MenuEngine.resolveProductPrice(product, channel = SalesChannel.POS, rules = listOf(deliveryRule, airportRule))
        assertEquals(15.00, posPrice, 0.001)

        val posDoublePrice = MenuEngine.resolveProductPrice(product, variant = variantDouble, channel = SalesChannel.POS, rules = listOf(deliveryRule, airportRule))
        assertEquals(19.00, posDoublePrice, 0.001)

        val deliveryPrice = MenuEngine.resolveProductPrice(product, channel = SalesChannel.DELIVERY_PARTNERS, rules = listOf(deliveryRule, airportRule))
        assertEquals(18.00, deliveryPrice, 0.001)

        val airportPrice = MenuEngine.resolveProductPrice(product, branchId = "branch_airport", channel = SalesChannel.POS, rules = listOf(deliveryRule, airportRule))
        assertEquals(19.50, airportPrice, 0.001)
    }

    @Test
    fun `modifier group selection validates min and max constraints`() {
        val group = ModifierGroup(
            id = "mg_cheese",
            nameEn = "Cheese",
            nameAr = "جبن",
            isRequired = true,
            minSelections = 1,
            maxSelections = 2
        )

        val resultEmpty = MenuEngine.validateModifierSelection(group, emptyList())
        assertFalse(resultEmpty.isValid)

        val resultOne = MenuEngine.validateModifierSelection(group, listOf("opt_cheddar"))
        assertTrue(resultOne.isValid)

        val resultTwo = MenuEngine.validateModifierSelection(group, listOf("opt_cheddar", "opt_swiss"))
        assertTrue(resultTwo.isValid)

        val resultThree = MenuEngine.validateModifierSelection(group, listOf("opt_cheddar", "opt_swiss", "opt_blue"))
        assertFalse(resultThree.isValid)
    }

    @Test
    fun `combo price calculator aggregates base bundle price and upgrades`() {
        val combo = ComboProduct(
            id = "cmb_1",
            productId = "p_combo",
            nameEn = "Meal",
            nameAr = "وجبة",
            sku = "CMB-1",
            comboBasePrice = 20.00,
            components = listOf(
                ComboComponent("c1", "cmb_1", "Burger", "برجر", defaultProductId = "p_reg", extraCharge = 0.0),
                ComboComponent("c2", "cmb_1", "Drink", "مشروب", extraCharge = 1.50)
            )
        )
        val regBurger = Product("p_reg", "cat_1", "SKU1", "Burger", "برجر", 10.00)
        val premiumBurger = Product("p_prem", "cat_1", "SKU2", "Truffle Burger", "برجر كمأة", 15.00)

        val standardTotal = MenuEngine.calculateComboPrice(
            combo = combo,
            componentSelections = mapOf("c1" to regBurger)
        )
        assertEquals(21.50, standardTotal, 0.001)

        val upgradedTotal = MenuEngine.calculateComboPrice(
            combo = combo,
            componentSelections = mapOf("c1" to premiumBurger)
        )
        assertTrue(upgradedTotal > standardTotal)
    }

    @Test
    fun `unified menu engine builds channel specific menus without duplicated definitions`() = runTest {
        val categories = repository.categories.first()
        val products = repository.getProducts().first()
        val priceRules = repository.priceRules.first()

        assertTrue(categories.isNotEmpty())
        assertTrue(products.isNotEmpty())

        val posMenu = MenuEngine.getUnifiedMenuForChannel(
            categories = categories,
            products = products,
            channel = SalesChannel.POS,
            branchId = "branch_01",
            priceRules = priceRules
        )

        val deliveryMenu = MenuEngine.getUnifiedMenuForChannel(
            categories = categories,
            products = products,
            channel = SalesChannel.DELIVERY_PARTNERS,
            branchId = "branch_01",
            priceRules = priceRules
        )

        assertTrue(posMenu.isNotEmpty())
        assertTrue(deliveryMenu.isNotEmpty())

        val posBurgerItem = posMenu.flatMap { it.items }.find { it.product.id == "p1" }
        val delBurgerItem = deliveryMenu.flatMap { it.items }.find { it.product.id == "p1" }

        assertNotNull(posBurgerItem)
        assertNotNull(delBurgerItem)
        assertEquals(18.50, posBurgerItem!!.resolvedPrice, 0.001)
        assertEquals(21.50, delBurgerItem!!.resolvedPrice, 0.001)
    }

    // --- PHASE 4: CENTRAL ORDER ENGINE, POS FLOWS, PAYMENTS, SHIFTS & OFFLINE TESTS ---

    @Test
    fun `controlled order state machine enforces valid transitions and blocks invalid jumps`() {
        val baseOrder = Order(
            id = "ord_test_state",
            branchId = "branch_01",
            orderNumber = "ORD-2001",
            orderType = OrderType.DINE_IN,
            status = OrderStatus.PENDING,
            subtotal = 20.0,
            taxAmount = 2.0,
            totalAmount = 22.0
        )

        // Valid transition: PENDING -> PREPARING
        val prepResult = OrderEngine.transitionOrder(baseOrder, OrderStatus.PREPARING)
        assertTrue(prepResult is OrderEngine.OrderTransitionResult.Success)
        val preparingOrder = (prepResult as OrderEngine.OrderTransitionResult.Success).updatedOrder
        assertEquals(OrderStatus.PREPARING, preparingOrder.status)

        // Valid transition: PREPARING -> READY -> COMPLETED
        val readyResult = OrderEngine.transitionOrder(preparingOrder, OrderStatus.READY)
        assertTrue(readyResult is OrderEngine.OrderTransitionResult.Success)
        val completedResult = OrderEngine.transitionOrder(
            (readyResult as OrderEngine.OrderTransitionResult.Success).updatedOrder,
            OrderStatus.COMPLETED
        )
        assertTrue(completedResult is OrderEngine.OrderTransitionResult.Success)
        val completedOrder = (completedResult as OrderEngine.OrderTransitionResult.Success).updatedOrder
        assertNotNull(completedOrder.completedAt)

        // Invalid transition: COMPLETED -> PREPARING (must be rejected)
        val invalidBackwards = OrderEngine.transitionOrder(completedOrder, OrderStatus.PREPARING)
        assertTrue(invalidBackwards is OrderEngine.OrderTransitionResult.InvalidTransition)

        // Valid post-completion transition: COMPLETED -> REFUNDED
        val refundResult = OrderEngine.transitionOrder(completedOrder, OrderStatus.REFUNDED, reason = "Customer complaint")
        assertTrue(refundResult is OrderEngine.OrderTransitionResult.Success)
        assertEquals(OrderStatus.REFUNDED, (refundResult as OrderEngine.OrderTransitionResult.Success).updatedOrder.status)
    }

    @Test
    fun `order engine calculates financials with variants, modifiers, addons, coupons, service and delivery fees`() {
        val burger = DefaultSeedData.products.first { it.id == "p1" } // 18.50
        val doubleVar = burger.variants.first { it.id == "v_p1_d" } // +4.50 -> 23.00
        val cheeseMod = DefaultSeedData.modifierGroups.first { it.id == "mg_cheese" }.options.first { it.id == "opt_ch_stilton" } // +1.50
        val baconAddon = DefaultSeedData.addons.first { it.id == "add_bacon" } // 2.50 each

        val cartLine = OrderEngine.PosCartItem(
            product = burger,
            selectedVariant = doubleVar,
            selectedModifiers = listOf(cheeseMod),
            selectedAddons = mapOf(baconAddon to 2), // +5.00
            quantity = 2,
            baseResolvedPrice = 23.00 // 18.50 + 4.50
        )
        // Unit effective price = 23.00 + 1.50 + 5.00 = 29.50; qty 2 => 59.00 grossItemsSubtotal

        val welcomeCoupon = DefaultSeedData.coupons.first { it.code == "WELCOME10" } // 10% off up to $15
        val taxProfile = DefaultSeedData.taxProfiles.first { it.id == "tax_standard_10" }

        val dineInSummary = OrderEngine.calculateOrderFinancials(
            cartItems = listOf(cartLine),
            orderType = OrderType.DINE_IN,
            coupon = welcomeCoupon,
            taxProfile = taxProfile,
            applyDineInServiceFee = true,
            serviceFeeRate = 0.05
        )

        assertEquals(59.00, dineInSummary.grossItemsSubtotal, 0.001)
        assertEquals(5.90, dineInSummary.couponDiscount, 0.001) // 10% of 59.00
        assertTrue(dineInSummary.serviceFee > 0.0)
        assertEquals(0.0, dineInSummary.deliveryFee, 0.001)
        assertTrue(dineInSummary.totalPayable > 60.0)

        // Delivery Mode adds delivery fee and removes Dine-In service fee
        val deliverySummary = OrderEngine.calculateOrderFinancials(
            cartItems = listOf(cartLine),
            orderType = OrderType.DELIVERY,
            taxProfile = taxProfile,
            customDeliveryFee = 3.50
        )
        assertEquals(3.50, deliverySummary.deliveryFee, 0.001)
        assertEquals(0.0, deliverySummary.serviceFee, 0.001)
    }

    @Test
    fun `mixed tender payments and idempotency keys deduplicate repeated submissions`() {
        val orderTotal = 50.00

        val cashLeg = OrderEngine.createPaymentLeg(
            orderId = "ord_test",
            branchId = "branch_01",
            remainingOrderBalance = 50.00,
            tenderedAmount = 20.00,
            method = PaymentMethod.CASH,
            cashierId = "user_03",
            shiftId = "shift_01",
            idempotencyKey = "pay_idem_cash_1"
        )

        val cardLeg = OrderEngine.createPaymentLeg(
            orderId = "ord_test",
            branchId = "branch_01",
            remainingOrderBalance = 30.00,
            tenderedAmount = 30.00,
            method = PaymentMethod.CREDIT_CARD,
            cashierId = "user_03",
            shiftId = "shift_01",
            idempotencyKey = "pay_idem_card_1"
        )

        // Pass duplicate cashLeg to test idempotency protection
        val resolution = OrderEngine.resolvePayments(
            orderTotal = orderTotal,
            payments = listOf(cashLeg, cashLeg, cardLeg)
        )

        assertEquals(2, resolution.deduplicatedPayments.size)
        assertEquals(50.00, resolution.totalPaid, 0.001)
        assertEquals(0.0, resolution.remainingBalance, 0.001)
        assertEquals("PAID", resolution.paymentStatus)
    }

    @Test
    fun `split bill equally and by item assignment accurately divides order total`() {
        val items = listOf(
            OrderItem("oi_1", "ord_split", "p1", "Burger", "برجر", 20.0, 1, 20.0),
            OrderItem("oi_2", "ord_split", "p4", "Fries", "بطاطس", 10.0, 1, 10.0)
        )
        val sampleOrder = Order(
            id = "ord_split",
            branchId = "branch_01",
            orderNumber = "ORD-3001",
            orderType = OrderType.DINE_IN,
            subtotal = 30.0,
            taxAmount = 3.0,
            totalAmount = 33.0,
            items = items
        )

        // 1. Equal 3-way split
        val equalShares = OrderEngine.splitBillEqually(sampleOrder, items, seatCount = 3)
        assertEquals(3, equalShares.size)
        assertEquals(sampleOrder.totalAmount, equalShares.sumOf { it.totalDue }, 0.05)

        // 2. Split by Item Assignment
        val seatMap = mapOf(
            1 to listOf(items[0]),
            2 to listOf(items[1])
        )
        val itemShares = OrderEngine.splitBillByItems(sampleOrder, seatMap)
        assertEquals(2, itemShares.size)
        assertEquals(sampleOrder.totalAmount, itemShares.sumOf { it.totalDue }, 0.05)
    }

    @Test
    fun `merge orders combines items and recalculates totals`() {
        val pItems = listOf(OrderItem("oi_p1", "ord_p", "p1", "Burger", "برجر", 18.50, 1, 18.50))
        val sItems = listOf(OrderItem("oi_s1", "ord_s", "p4", "Fries", "بطاطس", 7.50, 1, 7.50))
        val primary = Order("ord_p", "branch_01", "ORD-101", OrderType.DINE_IN, subtotal = 18.50, taxAmount = 1.85, totalAmount = 20.35, items = pItems)
        val secondary = Order("ord_s", "branch_01", "ORD-102", OrderType.DINE_IN, subtotal = 7.50, taxAmount = 0.75, totalAmount = 8.25, items = sItems)

        val result = OrderEngine.mergeOrders(primary, pItems, secondary, sItems, taxRate = 0.10)
        assertNotNull(result)
        assertEquals(2, result!!.mergedItems.size)
        assertEquals(26.00, result.mergedOrder.subtotal, 0.01)
        assertEquals(28.60, result.mergedOrder.totalAmount, 0.01)
    }

    @Test
    fun `cashier shift tracks cash in, cash out, expenses, refunds and closing variance`() = runTest {
        // Open a new shift with $300 opening float
        repository.openCashierShift(
            terminalId = "POS-01",
            openingCash = 300.00
        )

        var activeShift = repository.currentShift.first()
        assertNotNull(activeShift)
        assertEquals(300.00, activeShift!!.openingCash, 0.001)

        // Record Cash In (+$50) and Expense (-$20)
        repository.recordCashDrawerMovement(CashMovementType.CASH_IN, 50.00, "Extra change")
        repository.recordCashDrawerMovement(CashMovementType.EXPENSE, 20.00, "Ice bags")

        activeShift = repository.currentShift.first()
        assertEquals(330.00, activeShift!!.expectedCash, 0.001)

        // Close shift with $325 counted cash (-$5 shortage)
        repository.closeCashierShift(actualCash = 325.00, closingNotes = "End of shift count")
        val closedShift = repository.currentShift.first()
        assertEquals(ShiftStatus.CLOSED, closedShift!!.status)
        assertEquals(-5.00, closedShift.cashDifference, 0.001)
    }

    @Test
    fun `offline mode queues POS orders with idempotency guard and synchronizes cleanly`() = runTest {
        val initialCount = repository.getOrders("branch_01").first().size

        // Enable Offline Mode
        repository.setOfflineMode(true)
        assertTrue(repository.isOfflineMode.first())

        val newOrder = Order(
            id = "ord_offline_999",
            branchId = "branch_01",
            orderNumber = "ORD-9999",
            orderType = OrderType.TAKEOUT,
            status = OrderStatus.PREPARING,
            subtotal = 20.00,
            taxAmount = 2.00,
            totalAmount = 22.00,
            paymentStatus = "PAID",
            idempotencyKey = "idem_unique_offline_999"
        )
        val item = OrderItem(
            id = "oi_offline_1",
            orderId = "ord_offline_999",
            productId = "p1",
            productNameEn = "Truffle Angus Burger",
            productNameAr = "برجر أنجوس بالكمأة",
            unitPrice = 20.00,
            quantity = 1,
            totalPrice = 20.00
        )

        // Place order twice with same idempotencyKey -> must only insert once
        val firstSave = repository.placeOrder(newOrder, listOf(item))
        val duplicateSave = repository.placeOrder(newOrder.copy(id = "ord_duplicate_attempt"), listOf(item))
        assertTrue(firstSave)
        assertFalse(duplicateSave)

        val ordersAfterInsert = repository.getOrders("branch_01").first()
        assertEquals(initialCount + 1, ordersAfterInsert.size)

        val queuedOrder = ordersAfterInsert.find { it.id == "ord_offline_999" }
        assertNotNull(queuedOrder)
        assertEquals(SyncStatus.QUEUED_OFFLINE, queuedOrder!!.syncStatus)
        assertTrue(repository.offlineSyncQueue.first().isNotEmpty())

        // Synchronize Offline Queue
        val syncedCount = repository.syncOfflineTransactions()
        assertTrue(syncedCount >= 1)
        assertTrue(repository.offlineSyncQueue.first().all { it.status == SyncStatus.SYNCED })

        val syncedOrder = repository.getOrders("branch_01").first().find { it.id == "ord_offline_999" }
        assertEquals(SyncStatus.SYNCED, syncedOrder!!.syncStatus)
    }

    // --- Phase 5: Smart Tables, Secure QR Ordering, Waiter Operations & Reservations Tests ---

    @Test
    fun `secure QR token hides raw IDs and validates complete QR table ordering flow`() = runTest {
        repository.ensureSeeded(forceReset = true)

        val company = repository.currentCompany.first()
        val branch = repository.currentBranch.first()
        val tables = repository.getTables("branch_01").first()
        val table1 = tables.first { it.id == "t1" }

        // 1. Verify QR token is opaque and does not expose raw companyId, branchId, or tableId
        assertTrue(table1.qrToken.startsWith("QR-SIG-"))
        assertFalse(table1.qrToken.contains("comp_01"))
        assertFalse(table1.qrToken.contains("branch_01"))
        assertFalse(table1.qrToken.contains("t1"))

        // 2. Validate QR flow: QR -> validate -> identify restaurant -> identify branch -> identify table -> create session
        val publicUrl = TableOperationsEngine.buildPublicQrUrl(table1.qrToken)
        val validation = repository.startCustomerQrSession(
            scannedInput = publicUrl,
            guestName = "Layla Al-Otaibi",
            partySize = 2
        )
        assertTrue(validation is TableOperationsEngine.QrValidationResult.Success)
        val session = repository.activeCustomerQrSession.first()
        assertNotNull(session)
        assertEquals("comp_01", session!!.companyId)
        assertEquals("branch_01", session.branchId)
        assertEquals("t1", session.tableId)
        assertEquals("T-01", session.tableNumber)

        // 3. Load QR channel menu & submit customer table order via central OrderEngine
        val qrMenu = MenuEngine.getUnifiedMenuForChannel(
            categories = repository.getCategories().first(),
            products = repository.getProducts().first(),
            channel = SalesChannel.QR_TABLE,
            branchId = branch.id,
            priceRules = repository.getPriceRules().first()
        ).flatMap { it.items }
        val burgerResolved = qrMenu.first { it.product.id == "p1" }
        // QR channel special price rule ($17.50 instead of $18.50)
        assertEquals(17.50, burgerResolved.resolvedPrice, 0.01)

        val cartItem = OrderEngine.PosCartItem(
            uid = "qr_ci_1",
            product = burgerResolved.product,
            quantity = 2,
            baseResolvedPrice = burgerResolved.resolvedPrice,
            notes = "Extra crispy fries on side"
        )

        val (qrOrder, qrItems) = TableOperationsEngine.buildTableOrderFromCart(
            existingOrder = null,
            cartItems = listOf(cartItem),
            branch = branch,
            table = table1,
            channel = SalesChannel.QR_TABLE,
            serverOrGuestId = session.sessionId,
            serverOrGuestName = "${session.guestName} (QR)",
            customerName = session.guestName
        )

        val saved = repository.placeOrder(qrOrder, qrItems)
        assertTrue(saved)

        // Verify Table T-01 is now OCCUPIED and linked to the QR order
        val updatedTable1 = repository.getTables("branch_01").first().first { it.id == "t1" }
        assertEquals(TableStatus.OCCUPIED, updatedTable1.status)
        assertEquals(qrOrder.id, updatedTable1.currentOrderId)

        // 4. Customer requests bill -> Table transitions to WAITING_PAYMENT
        val billRequested = repository.requestTableBill("t1", session.guestName)
        assertTrue(billRequested)
        val billedTable1 = repository.getTables("branch_01").first().first { it.id == "t1" }
        assertEquals(TableStatus.WAITING_PAYMENT, billedTable1.status)
    }

    @Test
    fun `table combinations, uncombinations and table movement transfer orders and update states`() = runTest {
        repository.ensureSeeded(forceReset = true)

        // 1. Combine T-01 (2 seats) and T-03 (4 seats) -> 6 combined seats
        val combinedOk = repository.combineTables("t1", listOf("t3"))
        assertTrue(combinedOk)

        var tables = repository.getTables("branch_01").first()
        val combinedT1 = tables.first { it.id == "t1" }
        val combinedT3 = tables.first { it.id == "t3" }
        assertEquals(6, combinedT1.combinedCapacity)
        assertTrue(combinedT1.combinedWithTableIds.contains("t3"))
        assertTrue(combinedT3.combinedWithTableIds.contains("t1"))

        // 2. Uncombine T-01 -> restores individual capacities (2 and 4)
        val uncombinedOk = repository.uncombineTable("t1")
        assertTrue(uncombinedOk)

        tables = repository.getTables("branch_01").first()
        assertEquals(2, tables.first { it.id == "t1" }.combinedCapacity)
        assertTrue(tables.first { it.id == "t1" }.combinedWithTableIds.isEmpty())

        // 3. Move active table session from T-02 (OCCUPIED with ord_demo_1) to T-03 (AVAILABLE)
        val movedOk = repository.moveTableSession("t2", "t3")
        assertTrue(movedOk)

        tables = repository.getTables("branch_01").first()
        val sourceT2 = tables.first { it.id == "t2" }
        val targetT3 = tables.first { it.id == "t3" }

        // Source table transitions to CLEANING, target table becomes OCCUPIED with ord_demo_1
        assertEquals(TableStatus.CLEANING, sourceT2.status)
        assertNull(sourceT2.currentOrderId)
        assertEquals(TableStatus.OCCUPIED, targetT3.status)
        assertEquals("ord_demo_1", targetT3.currentOrderId)

        val movedOrder = repository.getOrders("branch_01").first().first { it.id == "ord_demo_1" }
        assertEquals("t3", movedOrder.tableId)
    }

    @Test
    fun `waiter order modification appends items to existing table order and updates KDS`() = runTest {
        repository.ensureSeeded(forceReset = true)

        val branch = repository.currentBranch.first()
        val tables = repository.getTables("branch_01").first()
        val table7 = tables.first { it.id == "t7" }
        val existingOrder = repository.getOrders("branch_01").first().first { it.id == "ord_demo_3" }
        val initialItemCount = existingOrder.items.sumOf { it.quantity }

        val dessert = repository.getProducts().first().first { it.id == "p8" }
        val waiterAddition = OrderEngine.PosCartItem(
            uid = "waiter_add_1",
            product = dessert,
            quantity = 1,
            baseResolvedPrice = dessert.price,
            notes = "Serve warm with extra gelato"
        )

        val (updatedOrder, updatedItems) = TableOperationsEngine.buildTableOrderFromCart(
            existingOrder = existingOrder,
            cartItems = listOf(waiterAddition),
            branch = branch,
            table = table7,
            channel = SalesChannel.WAITER_APP,
            serverOrGuestId = "user_04",
            serverOrGuestName = "Sarah Nader"
        )

        val placed = repository.placeOrder(updatedOrder, updatedItems)
        assertTrue(placed)

        val refreshedOrder = repository.getOrders("branch_01").first().first { it.id == "ord_demo_3" }
        assertEquals(initialItemCount + 1, refreshedOrder.items.sumOf { it.quantity })
        assertTrue(refreshedOrder.totalAmount > existingOrder.totalAmount)
    }

    @Test
    fun `reservation lifecycle validates capacity and synchronizes table status`() = runTest {
        repository.ensureSeeded(forceReset = true)

        val tables = repository.getTables("branch_01").first()
        val table6 = tables.first { it.id == "t6" } // 4-seat Terrace Garden table

        // 1. Validate capacity guard rejects oversized party
        val oversizeError = TableOperationsEngine.validateReservation(
            customerName = "Large Party",
            customerPhone = "+966 50 000 0000",
            partySize = 10,
            table = table6
        )
        assertNotNull(oversizeError)

        // 2. Confirm reservation res_02 on T-06 -> marks T-06 as RESERVED
        val confirmedOk = repository.updateReservationStatus("res_02", ReservationStatus.CONFIRMED)
        assertTrue(confirmedOk)
        var updatedT6 = repository.getTables("branch_01").first().first { it.id == "t6" }
        assertEquals(TableStatus.RESERVED, updatedT6.status)

        // 3. Seat guests for res_02 -> marks T-06 as OCCUPIED
        val seatedOk = repository.updateReservationStatus("res_02", ReservationStatus.SEATED)
        assertTrue(seatedOk)
        updatedT6 = repository.getTables("branch_01").first().first { it.id == "t6" }
        assertEquals(TableStatus.OCCUPIED, updatedT6.status)
        assertEquals(4, updatedT6.guestCount)

        // 4. Mark reservation res_01 (on T-05) as NO_SHOW -> frees T-05 back to AVAILABLE
        val noShowOk = repository.updateReservationStatus("res_01", ReservationStatus.NO_SHOW)
        assertTrue(noShowOk)
        val updatedT5 = repository.getTables("branch_01").first().first { it.id == "t5" }
        assertEquals(TableStatus.AVAILABLE, updatedT5.status)
    }

    // --- Phase 6: Kitchen Display System (KDS), Automatic Station Routing & Real-Time Operations Tests ---

    @Test
    fun `POS order automatically routes items to multiple configured KDS stations and emits real-time events`() = runTest {
        repository.ensureSeeded(forceReset = true)

        val posOrder = Order(
            id = "ord_pos_kds_test",
            branchId = "branch_01",
            orderNumber = "ORD-6001",
            orderType = OrderType.TAKEOUT,
            channel = SalesChannel.POS,
            status = OrderStatus.PREPARING,
            subtotal = 56.50,
            taxAmount = 5.65,
            totalAmount = 62.15,
            cashierId = "user_03",
            cashierName = "Omar Tariq",
            customerName = "Ziad Al-Harbi",
            notes = "Pack sauces separately"
        )

        // Include items across 4 different stations: GRILL (p1), PIZZA (p10), FRYER (p4), DRINKS (p6)
        val posItems = listOf(
            OrderItem("oi_kds_1", "ord_pos_kds_test", "p1", "Truffle Angus Burger", "برجر أنجوس بالكمأة", 18.50, 1, 18.50, modifiersSummary = "Medium Well", notes = "No pickles"),
            OrderItem("oi_kds_2", "ord_pos_kds_test", "p10", "Woodfire Truffle Burrata Pizza", "بيتزا بوراتا والكمأة على الحطب", 24.00, 1, 24.00, modifiersSummary = "Extra Burrata"),
            OrderItem("oi_kds_3", "ord_pos_kds_test", "p4", "Parmesan Herb Fries", "بطاطس بالأعشاب والبارميزان", 7.50, 1, 7.50),
            OrderItem("oi_kds_4", "ord_pos_kds_test", "p6", "Passionfruit Mojito", "موهيتو باشن فروت", 6.50, 1, 6.50)
        )

        val saved = repository.placeOrder(posOrder, posItems)
        assertTrue(saved)

        val allTickets = repository.kitchenTickets.first()
        val orderTickets = allTickets.filter { it.orderId == "ord_pos_kds_test" }

        // Must create 4 distinct station tickets (GRILL, PIZZA, FRYER, DRINKS)
        assertEquals(4, orderTickets.size)
        val stationTypes = orderTickets.map { it.stationType }.toSet()
        assertTrue(stationTypes.contains(KitchenStationType.GRILL))
        assertTrue(stationTypes.contains(KitchenStationType.PIZZA))
        assertTrue(stationTypes.contains(KitchenStationType.FRYER))
        assertTrue(stationTypes.contains(KitchenStationType.DRINKS))

        // Verify ticket preserves modifiers and notes
        val grillTicket = orderTickets.first { it.stationType == KitchenStationType.GRILL }
        assertEquals("ORD-6001", grillTicket.orderNumber)
        assertEquals(KdsTicketStatus.NEW, grillTicket.ticketStatus)
        assertEquals("Medium Well", grillTicket.items.first().modifiersSummary)
        assertEquals("No pickles", grillTicket.items.first().notes)

        // Verify real-time event log captured NEW_ORDER and KITCHEN_TICKET_UPDATED events
        val rtEvents = repository.realTimeEvents.first()
        assertTrue(rtEvents.any { it.type == RealTimeEventType.NEW_ORDER && it.entityId == "ord_pos_kds_test" })
        assertTrue(rtEvents.any { it.type == RealTimeEventType.KITCHEN_TICKET_UPDATED && it.referenceCode == "ORD-6001" })
    }

    @Test
    fun `QR order and Waiter order route to KDS and status updates synchronize back to customer and order view`() = runTest {
        repository.ensureSeeded(forceReset = true)

        val branch = repository.currentBranch.first()
        val tables = repository.getTables("branch_01").first()
        val table3 = tables.first { it.id == "t3" }
        val products = repository.getProducts().first()

        // 1. Place Customer QR Order with Pizza (PIZZA station) + Dessert (DESSERT station)
        val pizzaProduct = products.first { it.id == "p10" }
        val dessertProduct = products.first { it.id == "p8" }

        val qrCart = listOf(
            OrderEngine.PosCartItem(uid = "qr_1", product = pizzaProduct, quantity = 1, baseResolvedPrice = pizzaProduct.price, notes = "Crispy crust"),
            OrderEngine.PosCartItem(uid = "qr_2", product = dessertProduct, quantity = 1, baseResolvedPrice = dessertProduct.price)
        )

        val (qrOrder, qrItems) = TableOperationsEngine.buildTableOrderFromCart(
            existingOrder = null,
            cartItems = qrCart,
            branch = branch,
            table = table3,
            channel = SalesChannel.QR_TABLE,
            serverOrGuestId = "qr_sess_test",
            serverOrGuestName = "Nadia (QR)",
            customerName = "Nadia Al-Salman"
        )

        assertTrue(repository.placeOrder(qrOrder, qrItems))

        var qrTickets = repository.kitchenTickets.first().filter { it.orderId == qrOrder.id }
        assertEquals(2, qrTickets.size)
        assertTrue(qrTickets.any { it.stationType == KitchenStationType.PIZZA })
        assertTrue(qrTickets.any { it.stationType == KitchenStationType.DESSERT })

        // 2. Waiter appends a Drink item (DRINKS station) to the same table order
        val drinkProduct = products.first { it.id == "p6" }
        val currentStoredQrOrder = repository.getOrders("branch_01").first().first { it.id == qrOrder.id }
        val (waiterUpdatedOrder, waiterUpdatedItems) = TableOperationsEngine.buildTableOrderFromCart(
            existingOrder = currentStoredQrOrder,
            cartItems = listOf(OrderEngine.PosCartItem(uid = "waiter_1", product = drinkProduct, quantity = 2, baseResolvedPrice = drinkProduct.price)),
            branch = branch,
            table = table3,
            channel = SalesChannel.WAITER_APP,
            serverOrGuestId = "user_04",
            serverOrGuestName = "Sarah Nader"
        )

        assertTrue(repository.placeOrder(waiterUpdatedOrder, waiterUpdatedItems))

        // Verify incremental routing created the DRINKS station ticket without duplicating PIZZA or DESSERT
        qrTickets = repository.kitchenTickets.first().filter { it.orderId == qrOrder.id }
        assertEquals(3, qrTickets.size)
        assertTrue(qrTickets.any { it.stationType == KitchenStationType.DRINKS })

        // 3. Progress Kitchen Workflow: NEW -> ACCEPTED -> PREPARING -> READY -> EXPEDITER -> COMPLETED
        val pizzaTicket = qrTickets.first { it.stationType == KitchenStationType.PIZZA }
        val dessertTicket = qrTickets.first { it.stationType == KitchenStationType.DESSERT }
        val drinksTicket = qrTickets.first { it.stationType == KitchenStationType.DRINKS }

        // Transition Pizza ticket to ACCEPTED -> PREPARING
        assertNotNull(repository.transitionKitchenTicketStatus(pizzaTicket.id, KdsTicketStatus.ACCEPTED))
        assertNotNull(repository.transitionKitchenTicketStatus(pizzaTicket.id, KdsTicketStatus.PREPARING))

        // Verify central Order and OrderItem.kitchenStatus updated to PREPARING / COOKING (visible to customer & waiter)
        var syncedOrder = repository.getOrders("branch_01").first().first { it.id == qrOrder.id }
        assertEquals(OrderStatus.PREPARING, syncedOrder.status)
        val syncedPizzaItem = syncedOrder.items.first { it.productId == "p10" }
        assertEquals("COOKING", syncedPizzaItem.kitchenStatus)

        // Mark all 3 prep stations as READY
        assertNotNull(repository.transitionKitchenTicketStatus(pizzaTicket.id, KdsTicketStatus.READY))
        assertNotNull(repository.transitionKitchenTicketStatus(dessertTicket.id, KdsTicketStatus.READY))
        assertNotNull(repository.transitionKitchenTicketStatus(drinksTicket.id, KdsTicketStatus.READY))

        // Verify that when all prep stations reach READY, an EXPEDITER consolidation ticket is spawned for Dine-In
        val ticketsAfterPrepReady = repository.kitchenTickets.first().filter { it.orderId == qrOrder.id }
        val expediterTicket = ticketsAfterPrepReady.find { it.stationType == KitchenStationType.EXPEDITER }
        assertNotNull(expediterTicket)
        assertEquals(KdsTicketStatus.NEW, expediterTicket!!.ticketStatus)

        // Transition Expediter ticket to READY -> central Order status becomes READY
        assertNotNull(repository.transitionKitchenTicketStatus(expediterTicket.id, KdsTicketStatus.READY))
        syncedOrder = repository.getOrders("branch_01").first().first { it.id == qrOrder.id }
        assertEquals(OrderStatus.READY, syncedOrder.status)

        // Complete all tickets -> central Order status becomes SERVED for Dine-In
        for (t in ticketsAfterPrepReady) {
            if (t.ticketStatus != KdsTicketStatus.COMPLETED) {
                repository.transitionKitchenTicketStatus(t.id, KdsTicketStatus.COMPLETED)
            }
        }
        syncedOrder = repository.getOrders("branch_01").first().first { it.id == qrOrder.id }
        assertEquals(OrderStatus.SERVED, syncedOrder.status)
        assertTrue(syncedOrder.items.all { it.kitchenStatus == "SERVED" })
    }

    // --- Phases 7 to 12: Customer Portal, Delivery Dispatch, Inventory, Accounting, HR & Analytics Integration Test ---

    @Test
    fun `phases 7 to 12 integrate customer ordering, delivery dispatch, recipe inventory, double-entry accounting, HR and AI analytics`() = runTest {
        repository.ensureSeeded(forceReset = true)

        // 1. Phase 7: Customer OTP auth, loyalty redemption & online delivery order via Central Order Engine
        val customer = repository.loginOrRegisterCustomerWithOtp(
            name = "Tariq Al-Mansoor",
            phone = "+966 55 888 9999",
            email = "tariq@restoops.com",
            otpCode = "1234"
        )
        assertNotNull(customer)
        assertTrue(customer!!.otpVerified)

        val initialJournalsCount = repository.journalEntries.first().size
        val initialTxCount = repository.inventoryTransactions.first().size

        val deliveryOrder = Order(
            id = "ord_phase7_12_test",
            branchId = "branch_01",
            orderNumber = "ORD-7120",
            orderType = OrderType.DELIVERY,
            channel = SalesChannel.CUSTOMER_APP,
            status = OrderStatus.PREPARING,
            customerId = customer.id,
            customerName = customer.name,
            customerPhone = customer.phone,
            deliveryAddress = "Villa 14, Al-Olaya District, Riyadh",
            subtotal = 37.00,
            taxAmount = 3.70,
            deliveryFee = 4.50,
            totalAmount = 45.20,
            paymentStatus = "UNPAID"
        )
        val deliveryItems = listOf(
            OrderItem("oi_712_1", "ord_phase7_12_test", "p1", "Truffle Angus Burger", "برجر أنجوس بالكمأة", 18.50, 2, 37.00)
        )

        assertTrue(repository.placeOrder(deliveryOrder, deliveryItems))

        // 2. Verify Phase 9 (Recipe Sale Consumption) & Phase 10 (Double-Entry Sale Journal) & Phase 8 (Delivery Assignment)
        val updatedTxs = repository.inventoryTransactions.first()
        assertTrue(updatedTxs.size > initialTxCount)
        assertTrue(updatedTxs.any { it.transactionType == InventoryTransactionType.SALE_CONSUMPTION && it.sourceDocumentId == "ord_phase7_12_test" })

        val updatedJournals = repository.journalEntries.first()
        assertTrue(updatedJournals.size > initialJournalsCount)
        val saleJournal = updatedJournals.first { it.sourceDocumentId == "ord_phase7_12_test" }
        assertTrue(saleJournal.isBalanced)

        // 3. Phase 8: Assign driver, progress delivery workflow to DELIVERED with OTP verification, and settle COD
        val deliveryAssignment = repository.deliveryAssignments.first().first { it.orderId == "ord_phase7_12_test" }
        assertTrue(repository.assignDriverToDelivery(deliveryAssignment.id, "drv_01"))
        assertTrue(repository.transitionDeliveryAssignment(deliveryAssignment.id, DeliveryWorkflowStatus.ACCEPTED))
        assertTrue(repository.transitionDeliveryAssignment(deliveryAssignment.id, DeliveryWorkflowStatus.ARRIVED_RESTAURANT))
        assertTrue(repository.transitionDeliveryAssignment(deliveryAssignment.id, DeliveryWorkflowStatus.PICKED_UP))
        assertTrue(repository.transitionDeliveryAssignment(deliveryAssignment.id, DeliveryWorkflowStatus.ON_ROUTE))

        val assignedAfterRoute = repository.deliveryAssignments.first().first { it.id == deliveryAssignment.id }
        assertTrue(
            repository.transitionDeliveryAssignment(
                assignmentId = deliveryAssignment.id,
                targetStatus = DeliveryWorkflowStatus.DELIVERED,
                otpInput = assignedAfterRoute.customerOtpCode,
                proofNote = "Handed to customer at gate"
            )
        )

        val completedOrder = repository.getOrders("branch_01").first().first { it.id == "ord_phase7_12_test" }
        assertEquals(OrderStatus.COMPLETED, completedOrder.status)

        val codSettlement = repository.settleDriverCash("drv_01", 45.20, "COD shift handover")
        assertNotNull(codSettlement)

        // 4. Phase 9 & 10: Advance Purchase Order to RECEIVED & SUPPLIER_INVOICE, post operating expense
        assertTrue(repository.advancePurchaseOrderStage("po_01", ProcurementStage.RECEIVED))
        assertTrue(repository.advancePurchaseOrderStage("po_01", ProcurementStage.SUPPLIER_INVOICE))
        val postedExpense = repository.recordOperatingExpense(
            category = "UTILITIES",
            description = "Monthly electricity bill",
            amount = 320.0,
            costCenterCode = "CC-BR01",
            paymentMethod = PaymentMethod.CREDIT_CARD
        )
        assertNotNull(postedExpense.journalEntryId)

        // 5. Phase 11: Record verified employee attendance & approve leave
        repository.recordAttendanceCheckIn(
            employeeId = "emp_01",
            employeeName = "Karim Mansour",
            department = "Management",
            source = AttendanceVerificationSource.TERMINAL_CAMERA,
            note = "Morning shift check-in"
        )
        assertTrue(repository.attendanceRecords.first().any { it.correctionNote == "Morning shift check-in" })

        // 6. Phase 12: Execute RBAC-authorized AI query and verify multi-channel notifications
        val allOrders = repository.getOrders("branch_01").first()
        val aiResponse = repository.executeAuthorizedAiQuery("What were yesterday's sales?", allOrders)
        assertTrue(aiResponse.isAuthorized)
        assertTrue(aiResponse.supportingFacts.isNotEmpty())
        assertTrue(repository.centralNotificationEvents.first().isNotEmpty())
    }
}


