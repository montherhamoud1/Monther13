package com.example.ui.shell

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.constants.AppThemeMode
import com.example.core.constants.NavigationSection
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalAppLanguage
import com.example.domain.model.Role
import com.example.ui.screens.*
import com.example.ui.theme.RestoOpsTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    viewModel: ShellViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val company by viewModel.currentCompany.collectAsStateWithLifecycle()
    val brand by viewModel.currentBrand.collectAsStateWithLifecycle()
    val branch by viewModel.currentBranch.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentSession by viewModel.currentSession.collectAsStateWithLifecycle()

    val branches by viewModel.allBranches.collectAsStateWithLifecycle()
    val brands by viewModel.brands.collectAsStateWithLifecycle()
    val users by viewModel.users.collectAsStateWithLifecycle()
    val roles by viewModel.roles.collectAsStateWithLifecycle()
    val employees by viewModel.employees.collectAsStateWithLifecycle()
    val terminals by viewModel.terminals.collectAsStateWithLifecycle()

    val products by viewModel.products.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val ingredients by viewModel.ingredients.collectAsStateWithLifecycle()
    val recipes by viewModel.recipes.collectAsStateWithLifecycle()
    val combos by viewModel.combos.collectAsStateWithLifecycle()
    val modifierGroups by viewModel.modifierGroups.collectAsStateWithLifecycle()
    val addons by viewModel.addons.collectAsStateWithLifecycle()
    val priceRules by viewModel.priceRules.collectAsStateWithLifecycle()
    val taxProfiles by viewModel.taxProfiles.collectAsStateWithLifecycle()
    val tables by viewModel.tables.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val kitchenTickets by viewModel.kitchenTickets.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val coupons by viewModel.coupons.collectAsStateWithLifecycle()
    val currentShift by viewModel.currentShift.collectAsStateWithLifecycle()
    val isOfflineMode by viewModel.isOfflineMode.collectAsStateWithLifecycle()
    val offlineSyncQueue by viewModel.offlineSyncQueue.collectAsStateWithLifecycle()
    val floors by viewModel.floors.collectAsStateWithLifecycle()
    val zones by viewModel.zones.collectAsStateWithLifecycle()
    val reservations by viewModel.reservations.collectAsStateWithLifecycle()
    val activeCustomerQrSession by viewModel.activeCustomerQrSession.collectAsStateWithLifecycle()
    val kitchens by viewModel.kitchens.collectAsStateWithLifecycle()
    val preparationAreas by viewModel.preparationAreas.collectAsStateWithLifecycle()
    val kitchenStations by viewModel.kitchenStations.collectAsStateWithLifecycle()
    val kitchenEmployees by viewModel.kitchenEmployees.collectAsStateWithLifecycle()
    val stationRoutingRules by viewModel.stationRoutingRules.collectAsStateWithLifecycle()
    val realTimeEvents by viewModel.realTimeEvents.collectAsStateWithLifecycle()

    // Phase 7 - 12 StateFlows
    val activePortalCustomer by viewModel.activePortalCustomer.collectAsStateWithLifecycle()
    val customerAddresses by viewModel.customerAddresses.collectAsStateWithLifecycle()
    val loyaltyRewards by viewModel.loyaltyRewards.collectAsStateWithLifecycle()
    val customerReviews by viewModel.customerReviews.collectAsStateWithLifecycle()
    val customerComplaints by viewModel.customerComplaints.collectAsStateWithLifecycle()

    val deliveryZones by viewModel.deliveryZones.collectAsStateWithLifecycle()
    val drivers by viewModel.drivers.collectAsStateWithLifecycle()
    val deliveryAssignments by viewModel.deliveryAssignments.collectAsStateWithLifecycle()
    val driverCashSettlements by viewModel.driverCashSettlements.collectAsStateWithLifecycle()

    val warehouses by viewModel.warehouses.collectAsStateWithLifecycle()
    val unitConversionRules by viewModel.unitConversionRules.collectAsStateWithLifecycle()
    val warehouseStockItems by viewModel.warehouseStockItems.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val inventoryTransactions by viewModel.inventoryTransactions.collectAsStateWithLifecycle()
    val warehouseTransfers by viewModel.warehouseTransfers.collectAsStateWithLifecycle()
    val purchaseOrders by viewModel.purchaseOrders.collectAsStateWithLifecycle()
    val wasteRecords by viewModel.wasteRecords.collectAsStateWithLifecycle()
    val stockCountSessions by viewModel.stockCountSessions.collectAsStateWithLifecycle()

    val chartOfAccounts by viewModel.chartOfAccounts.collectAsStateWithLifecycle()
    val costCenters by viewModel.costCenters.collectAsStateWithLifecycle()
    val financialPeriods by viewModel.financialPeriods.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val journalEntries by viewModel.journalEntries.collectAsStateWithLifecycle()
    val bankAccounts by viewModel.bankAccounts.collectAsStateWithLifecycle()
    val subledgerInvoices by viewModel.subledgerInvoices.collectAsStateWithLifecycle()

    val shiftTemplates by viewModel.shiftTemplates.collectAsStateWithLifecycle()
    val attendanceRecords by viewModel.attendanceRecords.collectAsStateWithLifecycle()
    val leaveRequests by viewModel.leaveRequests.collectAsStateWithLifecycle()
    val payrollRecords by viewModel.payrollRecords.collectAsStateWithLifecycle()
    val centralNotificationEvents by viewModel.centralNotificationEvents.collectAsStateWithLifecycle()

    val isDark = when (uiState.themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    // Wrap in Theme and Layout Direction (LTR for English, RTL for Arabic)
    RestoOpsTheme(darkTheme = isDark) {
        CompositionLocalProvider(
            LocalLayoutDirection provides uiState.currentLanguage.layoutDirection,
            LocalAppLanguage provides uiState.currentLanguage
        ) {
            BoxWithConstraints(modifier = modifier.fillMaxSize()) {
                val isWideScreen = maxWidth >= 768.dp

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = !isWideScreen,
                    drawerContent = {
                        RestoDrawerContent(
                            currentSection = uiState.currentSection,
                            language = uiState.currentLanguage,
                            companyName = company.nameEn,
                            brandName = brand.nameEn,
                            onSectionSelected = { section ->
                                viewModel.navigateTo(section)
                                coroutineScope.launch { drawerState.close() }
                            }
                        )
                    }
                ) {
                    Scaffold(
                        topBar = {
                            RestoTopBar(
                                branch = branch,
                                user = user,
                                language = uiState.currentLanguage,
                                unreadNotificationCount = notifications.size,
                                onMenuClick = {
                                    coroutineScope.launch {
                                        if (drawerState.isOpen) drawerState.close() else drawerState.open()
                                    }
                                },
                                onBranchClick = { viewModel.setBranchSelectorOpen(true) },
                                onSearchClick = { viewModel.setGlobalSearchOpen(true) },
                                onLanguageClick = { viewModel.toggleLanguage() },
                                onThemeClick = { viewModel.toggleTheme() },
                                onNotificationClick = { viewModel.setNotificationsOpen(true) },
                                onUserClick = { viewModel.setUserMenuOpen(true) }
                            )
                        },
                        bottomBar = {
                            if (!isWideScreen) {
                                RestoBottomBar(
                                    currentSection = uiState.currentSection,
                                    language = uiState.currentLanguage,
                                    onSectionSelected = { viewModel.navigateTo(it) }
                                )
                            }
                        },
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        contentWindowInsets = WindowInsets.safeDrawing
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (isWideScreen) {
                                RestoNavigationRail(
                                    currentSection = uiState.currentSection,
                                    language = uiState.currentLanguage,
                                    onSectionSelected = { viewModel.navigateTo(it) },
                                    modifier = Modifier.fillMaxHeight()
                                )
                                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }

                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                when (uiState.currentSection) {
                                    NavigationSection.DASHBOARD -> DashboardScreen(
                                        company = company,
                                        branch = branch,
                                        orders = orders,
                                        tables = tables,
                                        language = uiState.currentLanguage,
                                        onNavigate = { viewModel.navigateTo(it) },
                                        customersCount = customers.size,
                                        kitchenQueueCount = kitchenTickets.count { it.ticketStatus != com.example.domain.model.KdsTicketStatus.COMPLETED && it.ticketStatus != com.example.domain.model.KdsTicketStatus.CANCELLED },
                                        deliveryOrdersCount = deliveryAssignments.count { it.status != com.example.domain.model.DeliveryWorkflowStatus.DELIVERED && it.status != com.example.domain.model.DeliveryWorkflowStatus.RETURNED_TO_BRANCH },
                                        activeDriversCount = drivers.count { it.shiftStatus == "ONLINE" },
                                        lowStockCount = warehouseStockItems.count { it.isLowStock },
                                        expensesTotal = expenses.sumOf { it.amount },
                                        cashInDrawer = currentShift?.expectedCash ?: 680.0
                                    )
                                    NavigationSection.MENU_CATALOG -> MenuCatalogScreen(
                                        branch = branch,
                                        products = products,
                                        categories = categories,
                                        ingredients = ingredients,
                                        recipes = recipes,
                                        combos = combos,
                                        modifierGroups = modifierGroups,
                                        addons = addons,
                                        priceRules = priceRules,
                                        taxProfiles = taxProfiles,
                                        language = uiState.currentLanguage,
                                        onSaveProduct = { viewModel.saveProduct(it) },
                                        onDeleteProduct = { viewModel.deleteProduct(it) },
                                        onToggleProductAvailability = { id, avail -> viewModel.toggleProductAvailability(id, avail) },
                                        onSaveCategory = { viewModel.saveCategory(it) },
                                        onDeleteCategory = { viewModel.deleteCategory(it) },
                                        onSaveIngredient = { viewModel.saveIngredient(it) },
                                        onDeleteIngredient = { viewModel.deleteIngredient(it) },
                                        onSaveRecipe = { viewModel.saveRecipe(it) },
                                        onSaveCombo = { viewModel.saveCombo(it) },
                                        onSaveModifierGroup = { viewModel.saveModifierGroup(it) },
                                        onSaveAddon = { viewModel.saveAddon(it) },
                                        onSavePriceRule = { viewModel.savePriceRule(it) },
                                        onDeletePriceRule = { viewModel.deletePriceRule(it) }
                                    )
                                    NavigationSection.ADMIN_PORTAL -> AdminPortalScreen(
                                        company = company,
                                        brands = brands,
                                        branches = branches,
                                        users = users,
                                        roles = roles,
                                        employees = employees,
                                        auditLogs = auditLogs,
                                        terminals = terminals,
                                        language = uiState.currentLanguage,
                                        onUpdateCompany = { viewModel.updateCompany(it) },
                                        onSaveBrand = { viewModel.saveBrand(it) },
                                        onSaveBranch = { viewModel.saveBranch(it) },
                                        onToggleBranchStatus = { id, active -> viewModel.toggleBranchStatus(id, active) },
                                        onAddTerminal = { viewModel.addTerminal(it) },
                                        onDeleteTerminal = { viewModel.deleteTerminal(it) },
                                        onSaveUser = { viewModel.saveUser(it) },
                                        onToggleUserStatus = { id, status -> viewModel.toggleUserStatus(id, status) },
                                        onResetPassword = { id, pwd -> viewModel.resetPassword(id, pwd) },
                                        onSaveRole = { viewModel.saveRole(it) },
                                        onDeleteRole = { viewModel.deleteRole(it) },
                                        onSaveEmployee = { viewModel.saveEmployee(it) },
                                        onUpdateEmployeeStatus = { id, status -> viewModel.updateEmployeeStatus(id, status) }
                                    )
                                    NavigationSection.POS -> POSScreen(
                                        company = company,
                                        branch = branch,
                                        user = user,
                                        session = currentSession,
                                        products = products,
                                        categories = categories,
                                        modifierGroups = modifierGroups,
                                        addons = addons,
                                        priceRules = priceRules,
                                        taxProfiles = taxProfiles,
                                        tables = tables,
                                        orders = orders,
                                        customers = customers,
                                        coupons = coupons,
                                        currentShift = currentShift,
                                        isOfflineMode = isOfflineMode,
                                        offlineSyncQueue = offlineSyncQueue,
                                        language = uiState.currentLanguage,
                                        onPlaceOrder = { order, items ->
                                            viewModel.placeOrUpdateOrder(order, items, order.payments)
                                        },
                                        onPlaceCompleteOrder = { order, items, payments ->
                                            viewModel.placeOrUpdateOrder(order, items, payments)
                                        },
                                        onHoldOrder = { order, items ->
                                            viewModel.placeOrUpdateOrder(order.copy(status = com.example.domain.model.OrderStatus.HELD), items, emptyList())
                                        },
                                        onTransitionOrderState = { orderId, newState, reason ->
                                            viewModel.transitionOrder(orderId, newState, reason)
                                        },
                                        onRefundOrReturnOrder = { orderId, isReturn, amount, reason ->
                                            viewModel.refundOrReturnOrder(orderId, isReturn, amount, reason)
                                        },
                                        onMergeOrders = { primaryId, secondaryId ->
                                            viewModel.mergeOrders(primaryId, secondaryId)
                                        },
                                        onTransferTable = { orderId, targetTableId ->
                                            viewModel.transferOrderTable(orderId, targetTableId)
                                        },
                                        onSaveCustomer = { viewModel.saveCustomer(it) },
                                        onOpenShift = { terminalId, openingCash ->
                                            viewModel.openCashierShift(terminalId, openingCash)
                                        },
                                        onRecordCashMovement = { type, amount, reason ->
                                            viewModel.recordCashMovement(type, amount, reason)
                                        },
                                        onCloseShift = { actualCash, notes ->
                                            viewModel.closeCashierShift(actualCash, notes)
                                        },
                                        onToggleOfflineMode = { viewModel.toggleOfflineMode(it) },
                                        onSyncOfflineQueue = { viewModel.syncOfflineQueue() },
                                        onShowFeedback = { viewModel.showToast(it) }
                                    )
                                    NavigationSection.TABLES -> TablesScreen(
                                        tables = tables,
                                        language = uiState.currentLanguage,
                                        onUpdateTableStatus = { tableId, status ->
                                            viewModel.updateTableStatus(tableId, status)
                                        },
                                        company = company,
                                        branch = branch,
                                        branches = branches,
                                        currentUser = user,
                                        floors = floors,
                                        zones = zones,
                                        reservations = reservations,
                                        orders = orders,
                                        products = products,
                                        categories = categories,
                                        modifierGroups = modifierGroups,
                                        addons = addons,
                                        priceRules = priceRules,
                                        taxProfiles = taxProfiles,
                                        employees = employees,
                                        customers = customers,
                                        activeCustomerQrSession = activeCustomerQrSession,
                                        onSaveTable = { viewModel.saveTable(it) },
                                        onOpenTableForGuests = { tblId, guests, wId, wName ->
                                            viewModel.openTableForGuests(tblId, guests, wId, wName)
                                        },
                                        onCombineTables = { primaryId, secondaryIds ->
                                            viewModel.combineTables(primaryId, secondaryIds)
                                        },
                                        onUncombineTable = { viewModel.uncombineTable(it) },
                                        onMoveTableSession = { srcId, dstId ->
                                            viewModel.moveTableSession(srcId, dstId)
                                        },
                                        onMergeOrders = { priId, secId ->
                                            viewModel.mergeOrders(priId, secId)
                                        },
                                        onPlaceOrUpdateOrder = { ord, items ->
                                            viewModel.placeOrUpdateOrder(ord, items, ord.payments)
                                        },
                                        onRequestTableBill = { tblId, reqBy ->
                                            viewModel.requestTableBill(tblId, reqBy)
                                        },
                                        onRegenerateTableQrToken = { viewModel.regenerateTableQrToken(it) },
                                        onStartCustomerQrSession = { tok, gName, pSize ->
                                            viewModel.startCustomerQrSession(tok, gName, pSize)
                                        },
                                        onClearCustomerQrSession = { viewModel.clearCustomerQrSession() },
                                        onSaveReservation = { viewModel.saveReservation(it) },
                                        onUpdateReservationStatus = { resId, st ->
                                            viewModel.updateReservationStatus(resId, st)
                                        },
                                        onShowFeedback = { viewModel.showToast(it) }
                                    )
                                    NavigationSection.KITCHEN -> KitchenDisplayScreen(
                                        tickets = kitchenTickets,
                                        language = uiState.currentLanguage,
                                        onUpdateTicketStatus = { ticketId, status ->
                                            viewModel.repository.updateKitchenTicketStatus(ticketId, status)
                                        },
                                        branchId = branch.id,
                                        kitchens = kitchens,
                                        preparationAreas = preparationAreas,
                                        stations = kitchenStations,
                                        routingRules = stationRoutingRules,
                                        kitchenEmployees = kitchenEmployees,
                                        realTimeEvents = realTimeEvents,
                                        categories = categories,
                                        products = products,
                                        onTransitionTicket = { ticketId, targetStatus ->
                                            viewModel.transitionKitchenTicket(ticketId, targetStatus)
                                        },
                                        onSaveStation = { viewModel.saveKitchenStation(it) },
                                        onSaveRoutingRule = { viewModel.saveStationRoutingRule(it) },
                                        onDeleteRoutingRule = { viewModel.deleteStationRoutingRule(it) },
                                        onSaveKitchenEmployee = { viewModel.saveKitchenEmployeeAssignment(it) },
                                        onToggleEmployeeDuty = { empId, onDuty ->
                                            viewModel.toggleKitchenEmployeeDuty(empId, onDuty)
                                        }
                                    )
                                    NavigationSection.CUSTOMER_PORTAL -> CustomerPortalScreen(
                                        company = company,
                                        branch = branch,
                                        branches = branches,
                                        activeCustomer = activePortalCustomer,
                                        customers = customers,
                                        products = products,
                                        categories = categories,
                                        coupons = coupons,
                                        orders = orders,
                                        addresses = customerAddresses,
                                        loyaltyRewards = loyaltyRewards,
                                        reviews = customerReviews,
                                        complaints = customerComplaints,
                                        deliveryZones = deliveryZones,
                                        language = uiState.currentLanguage,
                                        onSelectBranch = { viewModel.selectBranch(it) },
                                        onSelectCustomer = { viewModel.selectActivePortalCustomer(it) },
                                        onLoginWithOtp = { name, phone, email, otp ->
                                            viewModel.loginOrRegisterCustomerWithOtp(name, phone, email, otp)
                                        },
                                        onToggleFavorite = { custId, prodId ->
                                            viewModel.toggleCustomerFavoriteProduct(custId, prodId)
                                        },
                                        onSaveAddress = { viewModel.saveCustomerAddress(it) },
                                        onRedeemReward = { custId, rewardId, onRedeemed ->
                                            viewModel.redeemLoyaltyReward(custId, rewardId, onRedeemed)
                                        },
                                        onPlaceCustomerOrder = { ord, items, payments ->
                                            viewModel.placeOrUpdateOrder(ord, items, payments)
                                        },
                                        onSubmitReview = { viewModel.submitCustomerReview(it) },
                                        onModerateReview = { revId, st, reply ->
                                            viewModel.moderateCustomerReview(revId, st, reply)
                                        },
                                        onSubmitComplaint = { viewModel.submitCustomerComplaint(it) },
                                        onResolveComplaint = { cmpId, st, resp, comp ->
                                            viewModel.resolveCustomerComplaint(cmpId, st, resp, comp)
                                        }
                                    )
                                    NavigationSection.DELIVERY_DISPATCH -> DeliveryDispatchScreen(
                                        branch = branch,
                                        deliveryZones = deliveryZones,
                                        drivers = drivers,
                                        assignments = deliveryAssignments,
                                        cashSettlements = driverCashSettlements,
                                        language = uiState.currentLanguage,
                                        onAssignDriver = { asnId, drvId ->
                                            viewModel.assignDriverToDelivery(asnId, drvId)
                                        },
                                        onTransitionDelivery = { asnId, st, otp, failReason, proof ->
                                            viewModel.transitionDeliveryAssignment(asnId, st, otp, failReason, proof)
                                        },
                                        onToggleDriverShiftAndConsent = { drvId, shiftSt, consent ->
                                            viewModel.toggleDriverShiftAndConsent(drvId, shiftSt, consent)
                                        },
                                        onSaveDeliveryZone = { viewModel.saveDeliveryZone(it) },
                                        onSettleDriverCash = { drvId, amt, notes ->
                                            viewModel.settleDriverCash(drvId, amt, notes)
                                        }
                                    )
                                    NavigationSection.INVENTORY -> InventoryScreen(
                                        products = products,
                                        language = uiState.currentLanguage,
                                        warehouses = warehouses,
                                        unitConversions = unitConversionRules,
                                        stockItems = warehouseStockItems,
                                        suppliers = suppliers,
                                        transactions = inventoryTransactions,
                                        transfers = warehouseTransfers,
                                        purchaseOrders = purchaseOrders,
                                        wasteRecords = wasteRecords,
                                        stockCountSessions = stockCountSessions,
                                        onRecordWasteOrDamage = { stkId, qty, rsn, isDmg ->
                                            viewModel.recordWasteOrDamage(stkId, qty, rsn, isDmg)
                                        },
                                        onCreateOrAdvanceTransfer = { trfId, srcWh, dstWh, itemId, qty, st ->
                                            viewModel.createOrAdvanceWarehouseTransfer(trfId, srcWh, dstWh, itemId, qty, st)
                                        },
                                        onAdvancePurchaseOrderStage = { poId, stage ->
                                            viewModel.advancePurchaseOrderStage(poId, stage)
                                        },
                                        onCreatePurchaseOrder = { supId, whId, itemId, qty, price ->
                                            viewModel.createPurchaseOrder(supId, whId, itemId, qty, price)
                                        },
                                        onRecordStockCountAdjustment = { stkId, countedQty, notes ->
                                            viewModel.recordStockCountAdjustment(stkId, countedQty, notes)
                                        }
                                    )
                                    NavigationSection.ACCOUNTING -> AccountingScreen(
                                        accounts = chartOfAccounts,
                                        costCenters = costCenters,
                                        financialPeriods = financialPeriods,
                                        journalEntries = journalEntries,
                                        expenses = expenses,
                                        bankAccounts = bankAccounts,
                                        subledgerInvoices = subledgerInvoices,
                                        language = uiState.currentLanguage,
                                        onRecordExpense = { cat, desc, amt, cc, pm ->
                                            viewModel.recordOperatingExpense(cat, desc, amt, cc, pm)
                                        },
                                        onSettleInvoice = { invId, amt ->
                                            viewModel.settleSubledgerInvoice(invId, amt)
                                        },
                                        onUpdatePeriodStatus = { pId, st ->
                                            viewModel.updateFinancialPeriodStatus(pId, st)
                                        }
                                    )
                                    NavigationSection.HR_PAYROLL -> HrPayrollScreen(
                                        currentUser = user,
                                        employees = employees,
                                        shiftTemplates = shiftTemplates,
                                        attendanceRecords = attendanceRecords,
                                        leaveRequests = leaveRequests,
                                        payrollRecords = payrollRecords,
                                        language = uiState.currentLanguage,
                                        onRecordAttendanceCheckIn = { empId, empName, dept, src, note ->
                                            viewModel.recordAttendanceCheckIn(empId, empName, dept, src, note)
                                        },
                                        onSubmitOrUpdateLeave = { lvId, empId, empName, lvType, days, rsn, st ->
                                            viewModel.submitOrUpdateLeaveRequest(lvId, empId, empName, lvType, days, rsn, st)
                                        },
                                        onUpdatePayrollStatus = { payId, st ->
                                            viewModel.updatePayrollRecordStatus(payId, st)
                                        }
                                    )
                                    NavigationSection.REPORTS_ANALYTICS -> ReportsAnalyticsScreen(
                                        branch = branch,
                                        branches = branches,
                                        session = currentSession,
                                        orders = orders,
                                        tables = tables,
                                        kitchenTickets = kitchenTickets,
                                        deliveries = deliveryAssignments,
                                        drivers = drivers,
                                        stockItems = warehouseStockItems,
                                        purchaseOrders = purchaseOrders,
                                        suppliers = suppliers,
                                        expenses = expenses,
                                        accounts = chartOfAccounts,
                                        journalEntries = journalEntries,
                                        employees = employees,
                                        attendanceRecords = attendanceRecords,
                                        customers = customers,
                                        auditLogs = auditLogs,
                                        centralNotifications = centralNotificationEvents,
                                        currentShift = currentShift,
                                        language = uiState.currentLanguage,
                                        onExecuteAiQuery = { q, ords ->
                                            viewModel.executeAuthorizedAiQuery(q, ords)
                                        }
                                    )
                                    NavigationSection.DOMAIN_MODELS -> DomainBrowserScreen(
                                        language = uiState.currentLanguage
                                    )
                                    NavigationSection.AUDIT_LOGS -> AuditScreen(
                                        auditLogs = auditLogs,
                                        language = uiState.currentLanguage
                                    )
                                    NavigationSection.UNAUTHORIZED -> UnauthorizedScreen(
                                        user = user,
                                        role = currentSession?.role ?: Role("r_denied", user.roleId, user.roleId, user.roleId),
                                        attemptedSection = uiState.unauthorizedSectionAttempt ?: "Protected Module",
                                        missingPermission = uiState.missingPermissionRequired,
                                        language = uiState.currentLanguage,
                                        onReturnToDashboard = { viewModel.navigateTo(NavigationSection.DASHBOARD) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Interactive Modals & Dialogs
                BranchSelectorDialog(
                    visible = uiState.isBranchSelectorOpen,
                    branches = branches,
                    currentBranch = branch,
                    language = uiState.currentLanguage,
                    onBranchSelected = { viewModel.selectBranch(it) },
                    onDismiss = { viewModel.setBranchSelectorOpen(false) }
                )

                NotificationCenterDialog(
                    visible = uiState.isNotificationsOpen,
                    notifications = notifications,
                    language = uiState.currentLanguage,
                    onClearAll = { viewModel.clearNotifications() },
                    onDismiss = { viewModel.setNotificationsOpen(false) }
                )

                GlobalSearchDialog(
                    visible = uiState.isGlobalSearchOpen,
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    products = products,
                    orders = orders,
                    tables = tables,
                    language = uiState.currentLanguage,
                    onNavigate = { viewModel.navigateTo(it) },
                    onDismiss = { viewModel.setGlobalSearchOpen(false) }
                )

                UserMenuDialog(
                    visible = uiState.isUserMenuOpen,
                    user = user,
                    company = company,
                    session = currentSession,
                    language = uiState.currentLanguage,
                    onRoleSwitch = { roleId, title ->
                        viewModel.switchRole(roleId, title)
                        viewModel.showToast("Switched role context to $title")
                    },
                    onOpenLogin = {
                        viewModel.setUserMenuOpen(false)
                        viewModel.setLoginOpen(true)
                    },
                    onLogout = { viewModel.performLogout() },
                    onDismiss = { viewModel.setUserMenuOpen(false) }
                )

                LoginDialog(
                    visible = uiState.isLoginOpen,
                    currentCompanyCode = company.code,
                    loginError = uiState.loginError,
                    language = uiState.currentLanguage,
                    onLoginSubmit = { compCode, uname, pwd ->
                        viewModel.performLogin(compCode, uname, pwd)
                    },
                    onDismiss = { viewModel.setLoginOpen(false) }
                )
            }
        }
    }
}
