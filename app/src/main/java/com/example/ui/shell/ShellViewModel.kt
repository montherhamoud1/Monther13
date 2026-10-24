package com.example.ui.shell

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.constants.AppConstants
import com.example.core.constants.AppThemeMode
import com.example.core.constants.NavigationSection
import com.example.core.i18n.AppLanguage
import com.example.data.local.RestoOpsDatabase
import com.example.data.repository.AppNotification
import com.example.data.repository.LoginResult
import com.example.data.repository.RestaurantRepository
import com.example.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class ShellUiState(
    val currentLanguage: AppLanguage = AppLanguage.ARABIC,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val currentSection: NavigationSection = NavigationSection.DASHBOARD,
    val isBranchSelectorOpen: Boolean = false,
    val isNotificationsOpen: Boolean = false,
    val isGlobalSearchOpen: Boolean = false,
    val isUserMenuOpen: Boolean = false,
    val isLoginOpen: Boolean = false,
    val searchQuery: String = "",
    val feedbackMessage: String? = null,
    val unauthorizedSectionAttempt: String? = null,
    val missingPermissionRequired: String? = null,
    val loginError: String? = null
)

class ShellViewModel(application: Application) : AndroidViewModel(application) {
    val database = RestoOpsDatabase.getInstance(application)
    val repository = RestaurantRepository(database, viewModelScope)

    private val _uiState = MutableStateFlow(ShellUiState())
    val uiState = _uiState.asStateFlow()

    val currentCompany = repository.getCompany().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        repository.currentCompany.value
    )
    val currentBrand = repository.currentBrand
    val currentBranch = repository.currentBranch
    val currentUser = repository.currentUser
    val currentSession = repository.currentSession
    val notifications = repository.notifications

    val allBranches = repository.getAllBranches().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val brands = repository.getAllBrands().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val users = repository.getAllUsers().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val roles = repository.getAllRoles().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val employees = repository.getAllEmployees().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val terminals = currentBranch.flatMapLatest { branch ->
        repository.getTerminalsForBranch(branch.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products = repository.getProducts().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val categories = repository.categories.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val ingredients = repository.ingredients.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val recipes = repository.recipes.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val combos = repository.combos.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val modifierGroups = repository.modifierGroups.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val addons = repository.addons.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val priceRules = repository.priceRules.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val taxProfiles = repository.taxProfiles.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val tables = currentBranch.flatMapLatest { branch ->
        repository.getTables(branch.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orders = currentBranch.flatMapLatest { branch ->
        repository.getOrders(branch.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val kitchenTickets = repository.kitchenTickets
    val auditLogs = repository.getAuditLogs().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Phase 4 POS & Central Order Engine StateFlows
    val customers = repository.customers
    val coupons = repository.coupons
    val currentShift = repository.currentShift
    val shiftHistory = repository.shiftHistory
    val isOfflineMode = repository.isOfflineMode
    val offlineSyncQueue = repository.offlineSyncQueue

    // Phase 5 Smart Tables, QR Ordering, Reservations & Waiter StateFlows
    val floors = repository.floors
    val zones = repository.zones
    val reservations = currentBranch.flatMapLatest { branch ->
        repository.getReservations(branch.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeCustomerQrSession = repository.activeCustomerQrSession

    // Phase 6 Kitchen Display System, Stations, Routing & Real-Time Event Bus StateFlows
    val kitchens = repository.kitchens
    val preparationAreas = repository.preparationAreas
    val kitchenStations = repository.kitchenStations
    val kitchenEmployees = repository.kitchenEmployees
    val stationRoutingRules = repository.stationRoutingRules
    val realTimeEvents = repository.realTimeEvents
    val realTimeEventStream = repository.realTimeEventStream

    // Phase 7 Customer Ordering Portal, Addresses, Loyalty, Reviews & Complaints StateFlows
    val activePortalCustomer = repository.activePortalCustomer
    val customerAddresses = repository.customerAddresses
    val loyaltyRewards = repository.loyaltyRewards
    val customerReviews = repository.customerReviews
    val customerComplaints = repository.customerComplaints

    // Phase 8 Delivery Zones, Drivers, Dispatch Assignments & Driver Cash Settlements StateFlows
    val deliveryZones = repository.deliveryZones
    val drivers = repository.drivers
    val deliveryAssignments = repository.deliveryAssignments
    val driverCashSettlements = repository.driverCashSettlements

    // Phase 9 Multi-Warehouse Inventory, Unit Conversions, Stock Ledger, Transfers, Suppliers & Purchasing StateFlows
    val warehouses = repository.warehouses
    val unitConversionRules = repository.unitConversionRules
    val warehouseStockItems = repository.warehouseStockItems
    val suppliers = repository.suppliers
    val inventoryTransactions = repository.inventoryTransactions
    val warehouseTransfers = repository.warehouseTransfers
    val purchaseOrders = repository.purchaseOrders
    val wasteRecords = repository.wasteRecords
    val stockCountSessions = repository.stockCountSessions

    // Phase 10 Financial & Accounting Subsystem StateFlows
    val chartOfAccounts = repository.chartOfAccounts
    val costCenters = repository.costCenters
    val financialPeriods = repository.financialPeriods
    val expenses = repository.expenses
    val journalEntries = repository.journalEntries
    val bankAccounts = repository.bankAccounts
    val bankTransactions = repository.bankTransactions
    val subledgerInvoices = repository.subledgerInvoices

    // Phase 11 HR, Shift Templates, Attendance, Leave Requests & Payroll StateFlows
    val shiftTemplates = repository.shiftTemplates
    val attendanceRecords = repository.attendanceRecords
    val leaveRequests = repository.leaveRequests
    val payrollRecords = repository.payrollRecords

    // Phase 12 Central Multi-Channel Notification Events StateFlows
    val centralNotificationEvents = repository.centralNotificationEvents

    fun navigateTo(section: NavigationSection) {
        val session = currentSession.value
        val userRole = session?.role?.code ?: currentUser.value.roleId

        // Route Guard & RBAC Authorization Check
        val (authorized, requiredPerm) = checkSectionAuthorization(section, userRole, session)

        if (!authorized) {
            viewModelScope.launch {
                repository.recordAudit(
                    action = "PERMISSION_DENIED",
                    entity = "Navigation",
                    id = section.id,
                    details = "User ${currentUser.value.username} denied access to ${section.id} (required: $requiredPerm)"
                )
            }
            _uiState.update {
                it.copy(
                    currentSection = NavigationSection.UNAUTHORIZED,
                    unauthorizedSectionAttempt = section.id,
                    missingPermissionRequired = requiredPerm
                )
            }
            return
        }

        _uiState.update { it.copy(currentSection = section) }
    }

    private fun checkSectionAuthorization(
        section: NavigationSection,
        userRole: String,
        session: UserSession?
    ): Pair<Boolean, String?> {
        // Super admin has global authorization
        if (userRole == "ROLE_ADMIN") return true to null

        return when (section) {
            NavigationSection.MENU_CATALOG -> {
                val hasPerm = session?.effectivePermissions?.any { it.startsWith("MENU") || it.startsWith("PRODUCT") } == true
                (hasPerm || userRole in listOf("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_CHEF", "ROLE_CASHIER")) to "MENU:PRODUCT_CATALOG:VIEW"
            }
            NavigationSection.ADMIN_PORTAL -> {
                val hasPerm = session?.effectivePermissions?.any { it.startsWith("ADMINISTRATION") } == true
                (hasPerm || userRole == "ROLE_ADMIN") to "ADMINISTRATION:*:*"
            }
            NavigationSection.POS -> {
                val hasPerm = session?.effectivePermissions?.any { it.startsWith("POS") } == true
                (hasPerm || userRole in listOf("ROLE_CASHIER", "ROLE_MANAGER", "ROLE_WAITER")) to "POS:POS_TERMINAL:VIEW"
            }
            NavigationSection.TABLES -> {
                val hasPerm = session?.effectivePermissions?.any { it.startsWith("TABLES") } == true
                (hasPerm || userRole in listOf("ROLE_WAITER", "ROLE_MANAGER", "ROLE_CASHIER")) to "TABLES:FLOOR_TABLES:VIEW"
            }
            NavigationSection.KITCHEN -> {
                val hasPerm = session?.effectivePermissions?.any { it.startsWith("KITCHEN") } == true
                (hasPerm || userRole in listOf("ROLE_CHEF", "ROLE_MANAGER")) to "KITCHEN:KDS_SCREEN:VIEW"
            }
            NavigationSection.DELIVERY_DISPATCH -> {
                val hasPerm = session?.effectivePermissions?.any { it.startsWith("DELIVERY") || it.startsWith("POS") } == true
                (hasPerm || userRole in listOf("ROLE_DRIVER", "ROLE_MANAGER", "ROLE_CASHIER")) to "DELIVERY:DISPATCH_BOARD:VIEW"
            }
            NavigationSection.INVENTORY -> {
                val hasPerm = session?.effectivePermissions?.any { it.startsWith("INVENTORY") } == true
                (hasPerm || userRole in listOf("ROLE_MANAGER", "ROLE_CHEF")) to "INVENTORY:WAREHOUSE_STOCK:VIEW"
            }
            NavigationSection.ACCOUNTING -> {
                val hasPerm = session?.effectivePermissions?.any { it.startsWith("ACCOUNTING") || it.startsWith("FINANCE") } == true
                (hasPerm || userRole in listOf("ROLE_ADMIN", "ROLE_MANAGER")) to "ACCOUNTING:LEDGER:VIEW"
            }
            NavigationSection.HR_PAYROLL -> {
                val hasPerm = session?.effectivePermissions?.any { it.startsWith("HR") } == true
                (hasPerm || userRole in listOf("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_CASHIER", "ROLE_WAITER", "ROLE_CHEF", "ROLE_DRIVER")) to "HR:EMPLOYEE_PORTAL:VIEW"
            }
            NavigationSection.REPORTS_ANALYTICS -> {
                val hasPerm = session?.effectivePermissions?.any { it.startsWith("REPORTS") } == true
                (hasPerm || userRole in listOf("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_CASHIER")) to "REPORTS:EXECUTIVE_DASHBOARD:VIEW"
            }
            NavigationSection.AUDIT_LOGS -> {
                val hasPerm = session?.effectivePermissions?.any { it.startsWith("REPORTS") || it.startsWith("AUDIT") } == true
                (hasPerm || userRole in listOf("ROLE_ADMIN", "ROLE_MANAGER")) to "REPORTS:AUDIT_TRAIL:VIEW"
            }
            else -> true to null
        }
    }

    fun selectBranch(branch: Branch) {
        val session = currentSession.value
        val allowed = repository.canAccessBranch(branch.id)

        if (!allowed && session?.role?.code != "ROLE_ADMIN") {
            viewModelScope.launch {
                repository.recordAudit(
                    action = "BRANCH_ACCESS_DENIED",
                    entity = "Branch",
                    id = branch.id,
                    details = "User ${currentUser.value.username} denied access to restricted branch ${branch.code}"
                )
            }
            showToast("Branch restricted: Your role is not assigned to ${branch.nameEn}")
            _uiState.update { it.copy(isBranchSelectorOpen = false) }
            return
        }

        repository.selectBranch(branch)
        _uiState.update { it.copy(isBranchSelectorOpen = false) }
        showToast("Switched active branch to ${branch.nameEn}")
    }

    fun performLogin(companyCode: String, username: String, passwordAttempt: String) {
        viewModelScope.launch {
            when (val result = repository.login(companyCode, username, passwordAttempt)) {
                is LoginResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoginOpen = false,
                            loginError = null,
                            currentSection = NavigationSection.DASHBOARD
                        )
                    }
                    showToast("Successfully logged in as ${result.session.user.fullName}")
                }
                is LoginResult.InvalidCredentials -> {
                    val msg = "Invalid credentials. ${result.remainingAttempts} attempts remaining."
                    _uiState.update { it.copy(loginError = msg) }
                    showToast(msg)
                }
                is LoginResult.AccountLocked -> {
                    val msg = "Account is LOCKED due to excessive failed attempts. Contact admin."
                    _uiState.update { it.copy(loginError = msg) }
                    showToast(msg)
                }
                is LoginResult.AccountInactive -> {
                    val msg = "Account is inactive (${result.status}). Contact system manager."
                    _uiState.update { it.copy(loginError = msg) }
                    showToast(msg)
                }
                is LoginResult.CompanyNotFound -> {
                    val msg = "Company tenant code not found: $companyCode"
                    _uiState.update { it.copy(loginError = msg) }
                    showToast(msg)
                }
            }
        }
    }

    fun performLogout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.update {
                it.copy(
                    isUserMenuOpen = false,
                    isLoginOpen = true,
                    currentSection = NavigationSection.DASHBOARD
                )
            }
            showToast("Signed out from enterprise session")
        }
    }

    fun toggleLanguage() {
        _uiState.update {
            it.copy(
                currentLanguage = if (it.currentLanguage == AppLanguage.ENGLISH) AppLanguage.ARABIC else AppLanguage.ENGLISH
            )
        }
    }

    fun toggleTheme() {
        _uiState.update {
            val nextTheme = when (it.themeMode) {
                AppThemeMode.LIGHT -> AppThemeMode.DARK
                AppThemeMode.DARK -> AppThemeMode.LIGHT
                AppThemeMode.SYSTEM -> AppThemeMode.DARK
            }
            it.copy(themeMode = nextTheme)
        }
    }

    fun setBranchSelectorOpen(open: Boolean) = _uiState.update { it.copy(isBranchSelectorOpen = open) }
    fun setNotificationsOpen(open: Boolean) = _uiState.update { it.copy(isNotificationsOpen = open) }
    fun setGlobalSearchOpen(open: Boolean) = _uiState.update { it.copy(isGlobalSearchOpen = open, searchQuery = "") }
    fun setUserMenuOpen(open: Boolean) = _uiState.update { it.copy(isUserMenuOpen = open) }
    fun setLoginOpen(open: Boolean) = _uiState.update { it.copy(isLoginOpen = open, loginError = null) }
    fun updateSearchQuery(query: String) = _uiState.update { it.copy(searchQuery = query) }
    fun clearNotifications() = repository.clearNotifications()

    fun switchRole(roleId: String, title: String) {
        val user = currentUser.value.copy(
            roleId = roleId,
            fullName = when (roleId) {
                "ROLE_ADMIN" -> "Mira Hamoud (Admin)"
                "ROLE_MANAGER" -> "Karim Mansour (Branch Mgr)"
                "ROLE_CASHIER" -> "Youssef Zaid (Cashier)"
                "ROLE_WAITER" -> "Sarah Nader (Floor Server)"
                "ROLE_CHEF" -> "Chef Antoine (Executive Chef)"
                else -> "Tariq Ali (Driver)"
            },
            allowedBranchIds = if (roleId == "ROLE_ADMIN") listOf("branch_01", "branch_02", "branch_03") else listOf("branch_01")
        )
        repository.updateCurrentUser(user)
        // Refresh session
        viewModelScope.launch {
            val roleEntity = database.roleDao().getRoleByCode(roleId)
            val permissions = roleEntity?.permissionsJson?.split(",")?.toSet() ?: setOf("*")
            val newSession = UserSession(
                user = user,
                role = Role(
                    id = roleEntity?.id ?: "r_$roleId",
                    code = roleId,
                    nameEn = title,
                    nameAr = title,
                    permissions = permissions.toList()
                ),
                token = UUID.randomUUID().toString(),
                loginTimestamp = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + (8 * 3600 * 1000),
                activeBranchId = user.activeBranchId,
                allowedBranchIds = user.allowedBranchIds,
                effectivePermissions = permissions
            )
            // Re-validate current route
            _uiState.update {
                it.copy(
                    isUserMenuOpen = false,
                    isLoginOpen = false,
                    currentSection = NavigationSection.DASHBOARD
                )
            }
        }
    }

    // --- Administration Actions Delegated to Repository ---

    fun updateCompany(comp: Company) {
        viewModelScope.launch {
            repository.updateCompany(comp)
            showToast("Company profile and settings saved successfully")
        }
    }

    fun saveBrand(brand: Brand) {
        viewModelScope.launch {
            repository.saveBrand(brand)
            showToast("Brand ${brand.nameEn} saved")
        }
    }

    fun saveBranch(branch: Branch) {
        viewModelScope.launch {
            repository.saveBranch(branch)
            showToast("Branch ${branch.code} saved")
        }
    }

    fun toggleBranchStatus(branchId: String, isActive: Boolean) {
        viewModelScope.launch {
            repository.toggleBranchStatus(branchId, isActive)
            showToast("Branch status updated to: ${if (isActive) "Active" else "Inactive"}")
        }
    }

    fun addTerminal(terminal: PosTerminal) {
        viewModelScope.launch {
            repository.addPosTerminal(terminal)
            showToast("POS Terminal ${terminal.terminalCode} registered")
        }
    }

    fun deleteTerminal(terminalId: String) {
        viewModelScope.launch {
            repository.deletePosTerminal(terminalId)
            showToast("POS Terminal removed")
        }
    }

    fun saveUser(user: User) {
        viewModelScope.launch {
            repository.saveUser(user)
            showToast("User account ${user.username} saved")
        }
    }

    fun toggleUserStatus(userId: String, newStatus: String) {
        viewModelScope.launch {
            repository.toggleUserStatus(userId, newStatus)
            showToast("User status updated to $newStatus")
        }
    }

    fun resetPassword(userId: String, newPassword: String) {
        viewModelScope.launch {
            repository.resetUserPassword(userId, newPassword)
            showToast("Password reset successfully. Security lockouts cleared.")
        }
    }

    fun saveRole(role: Role) {
        viewModelScope.launch {
            repository.saveRole(role)
            showToast("Role ${role.nameEn} saved")
        }
    }

    fun deleteRole(roleId: String) {
        viewModelScope.launch {
            repository.deleteCustomRole(roleId)
            showToast("Custom role deleted")
        }
    }

    fun saveEmployee(emp: Employee) {
        viewModelScope.launch {
            repository.saveEmployee(emp)
            showToast("Employee record for ${emp.fullName} saved")
        }
    }

    fun updateEmployeeStatus(empId: String, status: String) {
        viewModelScope.launch {
            repository.updateEmployeeStatus(empId, status)
            showToast("Employee status updated to $status")
        }
    }

    // Phase 3 - Menu & Product Catalog Actions
    fun saveProduct(prod: Product) {
        viewModelScope.launch {
            repository.saveProduct(prod)
            showToast("Product ${prod.nameEn} saved successfully")
        }
    }

    fun deleteProduct(prodId: String) {
        viewModelScope.launch {
            repository.deleteProduct(prodId)
            showToast("Product deleted")
        }
    }

    fun toggleProductAvailability(prodId: String, isAvailable: Boolean) {
        viewModelScope.launch {
            repository.toggleProductAvailability(prodId, isAvailable)
            showToast("Product availability updated")
        }
    }

    fun saveCategory(cat: Category) {
        viewModelScope.launch {
            repository.saveCategory(cat)
            showToast("Category ${cat.nameEn} saved")
        }
    }

    fun deleteCategory(catId: String) {
        viewModelScope.launch {
            repository.deleteCategory(catId)
            showToast("Category removed")
        }
    }

    fun saveIngredient(ing: Ingredient) {
        viewModelScope.launch {
            repository.saveIngredient(ing)
            showToast("Ingredient ${ing.nameEn} saved")
        }
    }

    fun deleteIngredient(ingId: String) {
        viewModelScope.launch {
            repository.deleteIngredient(ingId)
            showToast("Ingredient removed")
        }
    }

    fun saveRecipe(recipe: Recipe) {
        viewModelScope.launch {
            repository.saveRecipe(recipe)
            showToast("Recipe ${recipe.name} saved")
        }
    }

    fun deleteRecipe(recipeId: String) {
        viewModelScope.launch {
            repository.deleteRecipe(recipeId)
            showToast("Recipe removed")
        }
    }

    fun saveCombo(combo: ComboProduct) {
        viewModelScope.launch {
            repository.saveCombo(combo)
            showToast("Combo bundle ${combo.nameEn} saved")
        }
    }

    fun deleteCombo(comboId: String) {
        viewModelScope.launch {
            repository.deleteCombo(comboId)
            showToast("Combo bundle deleted")
        }
    }

    fun saveModifierGroup(group: ModifierGroup) {
        viewModelScope.launch {
            repository.saveModifierGroup(group)
            showToast("Modifier group ${group.nameEn} saved")
        }
    }

    fun deleteModifierGroup(groupId: String) {
        viewModelScope.launch {
            repository.deleteModifierGroup(groupId)
            showToast("Modifier group removed")
        }
    }

    fun saveAddon(addon: ProductAddon) {
        viewModelScope.launch {
            repository.saveAddon(addon)
            showToast("Addon ${addon.nameEn} saved")
        }
    }

    fun deleteAddon(addonId: String) {
        viewModelScope.launch {
            repository.deleteAddon(addonId)
            showToast("Addon removed")
        }
    }

    fun savePriceRule(rule: ChannelPriceRule) {
        viewModelScope.launch {
            repository.savePriceRule(rule)
            showToast("Channel price rule saved")
        }
    }

    fun deletePriceRule(ruleId: String) {
        viewModelScope.launch {
            repository.deletePriceRule(ruleId)
            showToast("Price rule removed")
        }
    }

    fun saveTaxProfile(profile: TaxProfile) {
        viewModelScope.launch {
            repository.saveTaxProfile(profile)
            showToast("Tax profile ${profile.nameEn} saved")
        }
    }

    // --- Phase 4: POS & Central Order Engine Actions ---
    fun placeOrUpdateOrder(order: Order, items: List<OrderItem>, payments: List<Payment> = order.payments) {
        viewModelScope.launch {
            val saved = repository.placeOrder(order, items, payments)
            if (saved) {
                val label = if (order.status == OrderStatus.HELD) "held on park queue" else "recorded & dispatched"
                showToast("Order ${order.orderNumber} $label")
            } else {
                showToast("Idempotency guard prevented duplicate order ${order.orderNumber}")
            }
        }
    }

    fun transitionOrder(orderId: String, targetStatus: OrderStatus, reason: String = "", approvedBy: String? = null) {
        viewModelScope.launch {
            when (val res = repository.transitionOrderState(orderId, targetStatus, reason, approvedBy)) {
                is com.example.core.engine.OrderEngine.OrderTransitionResult.Success -> {
                    showToast("Order ${res.updatedOrder.orderNumber} -> ${targetStatus.titleEn}")
                }
                is com.example.core.engine.OrderEngine.OrderTransitionResult.InvalidTransition -> {
                    showToast(res.message)
                }
                is com.example.core.engine.OrderEngine.OrderTransitionResult.PermissionDenied -> {
                    showToast(res.message)
                }
            }
        }
    }

    fun refundOrReturnOrder(
        orderId: String,
        isReturnItems: Boolean,
        refundAmount: Double,
        reason: String,
        returnedItemQuantities: Map<String, Int> = emptyMap(),
        refundMethod: PaymentMethod = PaymentMethod.CASH,
        approvedBy: String? = null
    ) {
        viewModelScope.launch {
            when (val res = repository.refundOrReturnOrder(
                orderId = orderId,
                isReturnItems = isReturnItems,
                refundAmount = refundAmount,
                reason = reason,
                returnedItemQuantities = returnedItemQuantities,
                refundMethod = refundMethod,
                approvedBy = approvedBy
            )) {
                is com.example.core.engine.OrderEngine.OrderTransitionResult.Success -> {
                    showToast("${if (isReturnItems) "Return" else "Refund"} processed for ${res.updatedOrder.orderNumber} ($${String.format(java.util.Locale.US, "%.2f", refundAmount)})")
                }
                is com.example.core.engine.OrderEngine.OrderTransitionResult.InvalidTransition -> {
                    showToast(res.message)
                }
                is com.example.core.engine.OrderEngine.OrderTransitionResult.PermissionDenied -> {
                    showToast(res.message)
                }
            }
        }
    }

    fun mergeOrders(primaryOrderId: String, secondaryOrderId: String) {
        viewModelScope.launch {
            val ok = repository.mergeActiveOrders(primaryOrderId, secondaryOrderId)
            if (ok) {
                showToast("Orders merged successfully into primary ticket")
            } else {
                showToast("Cannot merge completed, cancelled, or identical orders")
            }
        }
    }

    fun transferOrderTable(orderId: String, targetTableId: String) {
        viewModelScope.launch {
            val ok = repository.transferOrderTable(orderId, targetTableId)
            if (ok) {
                showToast("Order transferred to selected table")
            } else {
                showToast("Table transfer could not be completed")
            }
        }
    }

    fun saveCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.saveCustomer(customer)
            showToast("Customer ${customer.name} saved & available for assignment")
        }
    }

    fun openCashierShift(terminalId: String, openingCash: Double) {
        viewModelScope.launch {
            val shift = repository.openCashierShift(terminalId, openingCash)
            showToast("Opened cashier shift on ${shift.terminalId} ($${shift.openingCash})")
        }
    }

    fun recordCashMovement(type: CashMovementType, amount: Double, reason: String, approvedBy: String? = null) {
        viewModelScope.launch {
            repository.recordCashDrawerMovement(type, amount, reason, approvedBy)
            showToast("Recorded ${type.titleEn}: $${String.format(java.util.Locale.US, "%.2f", amount)}")
        }
    }

    fun closeCashierShift(actualCash: Double, notes: String) {
        viewModelScope.launch {
            val closed = repository.closeCashierShift(actualCash, notes)
            if (closed != null) {
                showToast("Shift closed. Variance: $${String.format(java.util.Locale.US, "%.2f", closed.cashDifference)}")
            }
        }
    }

    fun toggleOfflineMode(offline: Boolean) {
        repository.setOfflineMode(offline)
        showToast(if (offline) "Offline POS Mode active — orders queued locally" else "Online connectivity restored")
    }

    fun syncOfflineQueue() {
        viewModelScope.launch {
            val count = repository.syncOfflineTransactions()
            showToast(if (count > 0) "Synchronized $count queued offline transactions" else "All transactions are already in sync")
        }
    }

    // --- Phase 5: Smart Tables, Waiter Operations, QR Ordering & Reservations ---
    fun updateTableStatus(tableId: String, status: TableStatus) {
        viewModelScope.launch {
            repository.updateTableStatus(tableId, status)
            showToast("Table status updated to ${status.titleEn}")
        }
    }

    fun saveTable(table: RestaurantTable) {
        viewModelScope.launch {
            repository.saveTable(table)
            showToast("Table ${table.tableNumber} configuration saved")
        }
    }

    fun openTableForGuests(tableId: String, guestCount: Int, waiterId: String?, waiterName: String?) {
        viewModelScope.launch {
            repository.openTableForGuests(tableId, guestCount, waiterId, waiterName)
            showToast("Table opened for $guestCount guests")
        }
    }

    fun combineTables(primaryTableId: String, secondaryTableIds: List<String>) {
        viewModelScope.launch {
            val ok = repository.combineTables(primaryTableId, secondaryTableIds)
            showToast(if (ok) "Tables combined into joined seating group" else "Select at least one secondary table to combine")
        }
    }

    fun uncombineTable(tableId: String) {
        viewModelScope.launch {
            val ok = repository.uncombineTable(tableId)
            if (ok) showToast("Combined tables separated into individual seating")
        }
    }

    fun moveTableSession(sourceTableId: String, targetTableId: String) {
        viewModelScope.launch {
            val ok = repository.moveTableSession(sourceTableId, targetTableId)
            showToast(if (ok) "Guests and active order moved to target table" else "Target table must be Available or Reserved")
        }
    }

    fun requestTableBill(tableId: String, requestedBy: String) {
        viewModelScope.launch {
            val ok = repository.requestTableBill(tableId, requestedBy)
            if (ok) showToast("Bill requested — table status set to Waiting Payment")
        }
    }

    fun regenerateTableQrToken(tableId: String) {
        viewModelScope.launch {
            val newToken = repository.regenerateTableQrToken(tableId)
            if (newToken != null) {
                showToast("New cryptographic QR token generated: $newToken")
            }
        }
    }

    fun startCustomerQrSession(scannedToken: String, guestName: String, partySize: Int) {
        viewModelScope.launch {
            when (val res = repository.startCustomerQrSession(scannedToken, guestName, partySize)) {
                is com.example.core.engine.TableOperationsEngine.QrValidationResult.Success -> {
                    showToast("QR Verified: ${res.context.company.nameEn} • ${res.context.branch.nameEn} • Table ${res.context.table.tableNumber}")
                }
                is com.example.core.engine.TableOperationsEngine.QrValidationResult.InvalidToken -> {
                    showToast(res.messageEn)
                }
                is com.example.core.engine.TableOperationsEngine.QrValidationResult.TableUnavailable -> {
                    showToast(res.messageEn)
                }
            }
        }
    }

    fun clearCustomerQrSession() {
        repository.clearCustomerQrSession()
    }

    fun saveReservation(reservation: Reservation) {
        viewModelScope.launch {
            repository.saveReservation(reservation)
            showToast("Reservation ${reservation.confirmationCode} saved for ${reservation.customerName}")
        }
    }

    fun updateReservationStatus(reservationId: String, status: ReservationStatus) {
        viewModelScope.launch {
            val ok = repository.updateReservationStatus(reservationId, status)
            if (ok) showToast("Reservation updated to ${status.titleEn}")
        }
    }

    // --- Phase 6: Kitchen Display System (KDS), Stations, Routing & Real-Time Operations ---
    fun transitionKitchenTicket(ticketId: String, targetStatus: KdsTicketStatus) {
        viewModelScope.launch {
            val outcome = repository.transitionKitchenTicketStatus(ticketId, targetStatus)
            if (outcome != null) {
                showToast(outcome.workflowMessageEn)
            } else {
                showToast("Transition to ${targetStatus.titleEn} is not permitted from current ticket state")
            }
        }
    }

    fun saveKitchen(kitchen: Kitchen) {
        viewModelScope.launch {
            repository.saveKitchen(kitchen)
            showToast("Kitchen ${kitchen.nameEn} saved")
        }
    }

    fun savePreparationArea(area: PreparationArea) {
        viewModelScope.launch {
            repository.savePreparationArea(area)
            showToast("Preparation area ${area.nameEn} saved")
        }
    }

    fun saveKitchenStation(station: KitchenStation) {
        viewModelScope.launch {
            repository.saveKitchenStation(station)
            showToast("Station ${station.nameEn} saved (Warning: ${station.warningThresholdMinutes}m / Overdue: ${station.overdueThresholdMinutes}m)")
        }
    }

    fun saveStationRoutingRule(rule: StationRoutingRule) {
        viewModelScope.launch {
            repository.saveStationRoutingRule(rule)
            showToast("Routing rule saved -> ${rule.targetStationName}")
        }
    }

    fun deleteStationRoutingRule(ruleId: String) {
        viewModelScope.launch {
            repository.deleteStationRoutingRule(ruleId)
            showToast("Station routing rule removed")
        }
    }

    fun saveKitchenEmployeeAssignment(assignment: KitchenEmployeeAssignment) {
        viewModelScope.launch {
            repository.saveKitchenEmployeeAssignment(assignment)
            showToast("Assigned ${assignment.employeeName} to ${assignment.stationName}")
        }
    }

    fun toggleKitchenEmployeeDuty(assignmentId: String, isOnDuty: Boolean) {
        viewModelScope.launch {
            repository.toggleKitchenEmployeeDuty(assignmentId, isOnDuty)
            showToast(if (isOnDuty) "Kitchen staff marked On-Duty" else "Kitchen staff marked Off-Duty")
        }
    }

    // --- Phase 7: Customer Portal, OTP Auth, Addresses, Loyalty, Reviews & Complaints ---
    fun loginOrRegisterCustomerWithOtp(name: String, phone: String, email: String, otpCode: String) {
        viewModelScope.launch {
            val cust = repository.loginOrRegisterCustomerWithOtp(name, phone, email, otpCode)
            if (cust != null) {
                showToast("OTP Verified! Welcome ${cust.name} (${cust.loyaltyPoints} pts • ${cust.tier})")
            } else {
                showToast("Invalid OTP code — enter at least 4 digits")
            }
        }
    }

    fun selectActivePortalCustomer(customer: Customer) {
        repository.selectActivePortalCustomer(customer)
        showToast("Switched customer portal profile to ${customer.name}")
    }

    fun toggleCustomerFavoriteProduct(customerId: String, productId: String) {
        viewModelScope.launch {
            repository.toggleCustomerFavoriteProduct(customerId, productId)
            showToast("Updated favorite items")
        }
    }

    fun saveCustomerAddress(address: CustomerAddress) {
        viewModelScope.launch {
            repository.saveCustomerAddress(address)
            showToast("Delivery address '${address.label}' saved")
        }
    }

    fun redeemLoyaltyReward(customerId: String, rewardId: String, onRedeemed: (Double) -> Unit = {}) {
        viewModelScope.launch {
            val credit = repository.redeemLoyaltyReward(customerId, rewardId)
            if (credit != null) {
                onRedeemed(credit)
                showToast("Loyalty reward redeemed for $${String.format(java.util.Locale.US, "%.2f", credit)} credit!")
            } else {
                showToast("Insufficient loyalty points for this reward")
            }
        }
    }

    fun submitCustomerReview(review: CustomerReview) {
        viewModelScope.launch {
            repository.submitCustomerReview(review)
            showToast("Review submitted (${review.rating}/5 stars)")
        }
    }

    fun moderateCustomerReview(reviewId: String, status: ReviewModerationStatus, managerReply: String?) {
        viewModelScope.launch {
            repository.moderateCustomerReview(reviewId, status, managerReply)
            showToast("Review moderation updated to ${status.titleEn}")
        }
    }

    fun submitCustomerComplaint(complaint: CustomerComplaint) {
        viewModelScope.launch {
            repository.submitCustomerComplaint(complaint)
            showToast("Support ticket ${complaint.ticketNumber} created & dispatched")
        }
    }

    fun resolveCustomerComplaint(complaintId: String, status: ComplaintStatus, response: String, compensation: String) {
        viewModelScope.launch {
            repository.resolveCustomerComplaint(complaintId, status, response, compensation)
            showToast("Ticket updated to ${status.titleEn}")
        }
    }

    // --- Phase 8: Delivery Zones, Driver Fleet, Dispatch & Cash Collection ---
    fun saveDeliveryZone(zone: DeliveryZone) {
        viewModelScope.launch {
            repository.saveDeliveryZone(zone)
            showToast("Delivery zone ${zone.nameEn} saved")
        }
    }

    fun toggleDriverShiftAndConsent(driverId: String, shiftStatus: String, locationConsent: Boolean) {
        viewModelScope.launch {
            repository.toggleDriverShiftAndConsent(driverId, shiftStatus, locationConsent)
            showToast("Driver shift set to $shiftStatus (GPS Consent: ${if (locationConsent) "Active" else "Off"})")
        }
    }

    fun assignDriverToDelivery(assignmentId: String, driverId: String) {
        viewModelScope.launch {
            val ok = repository.assignDriverToDelivery(assignmentId, driverId)
            showToast(if (ok) "Driver assigned & notified via Push/SMS" else "Could not assign driver")
        }
    }

    fun transitionDeliveryAssignment(
        assignmentId: String,
        targetStatus: DeliveryWorkflowStatus,
        otpInput: String? = null,
        failureReason: String? = null,
        proofNote: String = ""
    ) {
        viewModelScope.launch {
            val ok = repository.transitionDeliveryAssignment(assignmentId, targetStatus, otpInput, failureReason, proofNote)
            showToast(
                if (ok) "Delivery updated -> ${targetStatus.titleEn}"
                else "Transition rejected (verify workflow order or customer OTP)"
            )
        }
    }

    fun settleDriverCash(driverId: String, handedOverAmount: Double, notes: String) {
        viewModelScope.launch {
            val settlement = repository.settleDriverCash(driverId, handedOverAmount, notes)
            if (settlement != null) {
                showToast("Driver COD settled: $${String.format(java.util.Locale.US, "%.2f", handedOverAmount)} (Variance: $${String.format(java.util.Locale.US, "%.2f", settlement.varianceAmount)})")
            }
        }
    }

    // --- Phase 9: Warehouses, Traceable Movements, Transfers, Waste, Stock Count & Procurement ---
    fun saveWarehouse(warehouse: Warehouse) {
        viewModelScope.launch {
            repository.saveWarehouse(warehouse)
            showToast("Warehouse ${warehouse.nameEn} saved")
        }
    }

    fun recordWasteOrDamage(stockItemId: String, quantity: Double, reason: String, isDamage: Boolean) {
        viewModelScope.launch {
            val ok = repository.recordWasteOrDamage(stockItemId, quantity, reason, isDamage)
            showToast(if (ok) "${if (isDamage) "Damage" else "Waste"} logged & deducted from warehouse stock" else "Invalid stock item or quantity")
        }
    }

    fun createOrAdvanceWarehouseTransfer(
        transferId: String?,
        sourceWarehouseId: String = "wh_central",
        destinationWarehouseId: String = "wh_branch_01",
        itemId: String = "ing_angus_patty",
        quantity: Double = 50.0,
        targetStatus: WarehouseTransferStatus = WarehouseTransferStatus.REQUESTED
    ) {
        viewModelScope.launch {
            val ok = repository.createOrAdvanceWarehouseTransfer(
                transferId = transferId,
                sourceWarehouseId = sourceWarehouseId,
                destinationWarehouseId = destinationWarehouseId,
                itemId = itemId,
                quantity = quantity,
                targetStatus = targetStatus
            )
            showToast(if (ok) "Warehouse transfer -> ${targetStatus.titleEn}" else "Could not process warehouse transfer")
        }
    }

    fun advancePurchaseOrderStage(poId: String, targetStage: ProcurementStage) {
        viewModelScope.launch {
            val ok = repository.advancePurchaseOrderStage(poId, targetStage)
            showToast(if (ok) "Purchase Order advanced to ${targetStage.titleEn}" else "Could not advance PO stage")
        }
    }

    fun createPurchaseOrder(supplierId: String, warehouseId: String, itemId: String, quantity: Double, unitPrice: Double) {
        viewModelScope.launch {
            val po = repository.createPurchaseOrder(supplierId, warehouseId, itemId, quantity, unitPrice)
            if (po != null) {
                showToast("Created ${po.poNumber} for $${String.format(java.util.Locale.US, "%.2f", po.totalAmount)}")
            }
        }
    }

    fun recordStockCountAdjustment(stockItemId: String, countedQuantity: Double, notes: String) {
        viewModelScope.launch {
            val ok = repository.recordStockCountAdjustment(stockItemId, countedQuantity, notes)
            showToast(if (ok) "Stock count variance approved & ledger updated" else "Could not record stock count")
        }
    }

    // --- Phase 10: Financial & Accounting Subsystem ---
    fun recordOperatingExpense(
        category: String,
        description: String,
        amount: Double,
        costCenterCode: String,
        paymentMethod: PaymentMethod
    ) {
        viewModelScope.launch {
            val exp = repository.recordOperatingExpense(category, description, amount, costCenterCode, paymentMethod)
            showToast("Expense ${exp.documentNumber} ($${String.format(java.util.Locale.US, "%.2f", exp.amount)}) posted with balanced Journal Entry")
        }
    }

    fun settleSubledgerInvoice(invoiceId: String, paymentAmount: Double) {
        viewModelScope.launch {
            repository.settleSubledgerInvoice(invoiceId, paymentAmount)
            showToast("Subledger payment of $${String.format(java.util.Locale.US, "%.2f", paymentAmount)} recorded")
        }
    }

    fun updateFinancialPeriodStatus(periodId: String, status: FinancialPeriodStatus) {
        viewModelScope.launch {
            repository.updateFinancialPeriodStatus(periodId, status)
            showToast("Financial period status updated to ${status.titleEn}")
        }
    }

    // --- Phase 11: HR, Attendance, Leave & Payroll ---
    fun recordAttendanceCheckIn(
        employeeId: String,
        employeeName: String,
        department: String,
        source: AttendanceVerificationSource,
        note: String = ""
    ) {
        viewModelScope.launch {
            repository.recordAttendanceCheckIn(employeeId, employeeName, department, source, note)
            showToast("Attendance check-in recorded for $employeeName (${source.titleEn})")
        }
    }

    fun submitOrUpdateLeaveRequest(
        leaveId: String?,
        employeeId: String = "emp_04",
        employeeName: String = "Sarah Nader",
        leaveType: LeaveType = LeaveType.ANNUAL,
        daysCount: Int = 3,
        reason: String = "Personal leave",
        newStatus: String = "APPROVED"
    ) {
        viewModelScope.launch {
            repository.submitOrUpdateLeaveRequest(leaveId, employeeId, employeeName, leaveType, daysCount, reason, newStatus)
            showToast(if (leaveId == null) "Leave request submitted for $employeeName" else "Leave request marked $newStatus")
        }
    }

    fun updatePayrollRecordStatus(payrollId: String, newStatus: String) {
        viewModelScope.launch {
            repository.updatePayrollRecordStatus(payrollId, newStatus)
            showToast("Payroll record updated to $newStatus")
        }
    }

    // --- Phase 12: Authorized AI Business Query & Central Notifications ---
    fun executeAuthorizedAiQuery(queryText: String, allOrders: List<Order>): AuthorizedAiQueryResponse {
        return repository.executeAuthorizedAiQuery(queryText, allOrders)
    }

    fun showToast(msg: String) = _uiState.update { it.copy(feedbackMessage = msg) }
    fun clearToast() = _uiState.update { it.copy(feedbackMessage = null) }
}
