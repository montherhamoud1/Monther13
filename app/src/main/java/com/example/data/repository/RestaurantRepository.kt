package com.example.data.repository

import com.example.core.engine.AccountingEngine
import com.example.core.engine.CustomerCrmEngine
import com.example.core.engine.DeliveryDispatchEngine
import com.example.core.engine.InventoryProcurementEngine
import com.example.core.engine.KitchenEngine
import com.example.core.engine.OrderEngine
import com.example.core.engine.ReportingAndAiEngine
import com.example.core.engine.TableOperationsEngine
import com.example.data.local.*
import com.example.domain.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

sealed class LoginResult {
    data class Success(val session: UserSession) : LoginResult()
    data class InvalidCredentials(val remainingAttempts: Int) : LoginResult()
    object AccountLocked : LoginResult()
    data class AccountInactive(val status: String) : LoginResult()
    object CompanyNotFound : LoginResult()
}

class RestaurantRepository(
    private val database: RestoOpsDatabase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    // Current Active Company, Brand, Branch, User & Session
    private val _currentCompany = MutableStateFlow(
        Company(
            id = "comp_01",
            code = "AL-HAYA",
            nameEn = "Al-Haya Hospitality Group",
            nameAr = "مجموعة الحياة للضيافة والمطاعم",
            taxNumber = "TX-9948210-SA",
            defaultCurrency = "USD",
            country = "Saudi Arabia",
            fiscalYear = "2026-2027",
            autoLockMinutes = 15,
            isActive = true
        )
    )
    val currentCompany = _currentCompany.asStateFlow()

    private val _currentBrand = MutableStateFlow(
        Brand(
            id = "brand_01",
            companyId = "comp_01",
            code = "BURGER_CRAFT",
            nameEn = "Burger Craft & Smokehouse",
            nameAr = "برجر كرافت أند سموك هاوس",
            primaryColor = "#F59E0B",
            secondaryColor = "#0F172A",
            menuIdentity = "Gourmet Burgers & Artisan Smokehouse"
        )
    )
    val currentBrand = _currentBrand.asStateFlow()

    private val _currentBranch = MutableStateFlow(
        Branch(
            id = "branch_01",
            brandId = "brand_01",
            code = "DOWNTOWN-HQ",
            nameEn = "Downtown Flagship Branch",
            nameAr = "فرع وسط المدينة الرئيسي",
            phone = "+966 11 456 7890",
            address = "King Fahd Road, Suite 400",
            seatingCapacity = 120,
            openingTime = "10:00",
            closingTime = "01:00",
            timezone = "Asia/Riyadh (GMT+3)",
            currency = "USD",
            taxRate = 0.10,
            taxInclusive = false,
            assignedWarehouseId = "wh_01"
        )
    )
    val currentBranch = _currentBranch.asStateFlow()

    private val _currentUser = MutableStateFlow(
        User(
            id = "user_01",
            companyId = "comp_01",
            username = "admin_mira",
            email = "mira.ops@alhargroup.com",
            fullName = "Mira Hamoud",
            roleId = "ROLE_ADMIN",
            activeBranchId = "branch_01",
            allowedBranchIds = listOf("branch_01", "branch_02", "branch_03")
        )
    )
    val currentUser = _currentUser.asStateFlow()

    private val _currentSession = MutableStateFlow<UserSession?>(null)
    val currentSession = _currentSession.asStateFlow()

    // Real-time lists
    private val _kitchenTickets = MutableStateFlow<List<KitchenTicket>>(emptyList())
    val kitchenTickets = _kitchenTickets.asStateFlow()

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications = _notifications.asStateFlow()

    // Phase 3 - Product & Menu Engine State Flows
    private val _categories = MutableStateFlow(DefaultSeedData.categories)
    val categories = _categories.asStateFlow()

    private val _richProducts = MutableStateFlow(DefaultSeedData.products)
    val richProducts = _richProducts.asStateFlow()

    private val _ingredients = MutableStateFlow(DefaultSeedData.ingredients)
    val ingredients = _ingredients.asStateFlow()

    private val _recipes = MutableStateFlow(DefaultSeedData.recipes)
    val recipes = _recipes.asStateFlow()

    private val _combos = MutableStateFlow(DefaultSeedData.combos)
    val combos = _combos.asStateFlow()

    private val _modifierGroups = MutableStateFlow(DefaultSeedData.modifierGroups)
    val modifierGroups = _modifierGroups.asStateFlow()

    private val _addons = MutableStateFlow(DefaultSeedData.addons)
    val addons = _addons.asStateFlow()

    private val _priceRules = MutableStateFlow(DefaultSeedData.priceRules)
    val priceRules = _priceRules.asStateFlow()

    private val _taxProfiles = MutableStateFlow(DefaultSeedData.taxProfiles)
    val taxProfiles = _taxProfiles.asStateFlow()

    private val _schedules = MutableStateFlow<List<ProductSchedule>>(emptyList())
    val schedules = _schedules.asStateFlow()

    // Phase 4 - POS, Customers, Coupons, Shifts & Offline Sync State Flows
    private val _customers = MutableStateFlow(DefaultSeedData.customers)
    val customers = _customers.asStateFlow()

    private val _coupons = MutableStateFlow(DefaultSeedData.coupons)
    val coupons = _coupons.asStateFlow()

    private val _currentShift = MutableStateFlow<CashierShift?>(DefaultSeedData.initialShift)
    val currentShift = _currentShift.asStateFlow()

    private val _shiftHistory = MutableStateFlow(listOf(DefaultSeedData.initialShift))
    val shiftHistory = _shiftHistory.asStateFlow()

    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode = _isOfflineMode.asStateFlow()

    private val _offlineSyncQueue = MutableStateFlow<List<OfflineSyncEvent>>(emptyList())
    val offlineSyncQueue = _offlineSyncQueue.asStateFlow()

    // Phase 5 - Floors, Zones, Reservations & Active Customer QR Session
    private val _floors = MutableStateFlow(DefaultSeedData.floors)
    val floors = _floors.asStateFlow()

    private val _zones = MutableStateFlow(DefaultSeedData.zones)
    val zones = _zones.asStateFlow()

    private val _reservations = MutableStateFlow(DefaultSeedData.reservations)
    val reservations = _reservations.asStateFlow()

    private val _activeCustomerQrSession = MutableStateFlow<CustomerTableSession?>(null)
    val activeCustomerQrSession = _activeCustomerQrSession.asStateFlow()

    // Phase 6 - Kitchens, Preparation Areas, Stations, Kitchen Employees, Routing & Real-Time Event Bus
    private val _kitchens = MutableStateFlow(DefaultSeedData.kitchens)
    val kitchens = _kitchens.asStateFlow()

    private val _preparationAreas = MutableStateFlow(DefaultSeedData.preparationAreas)
    val preparationAreas = _preparationAreas.asStateFlow()

    private val _kitchenStations = MutableStateFlow(DefaultSeedData.kitchenStations)
    val kitchenStations = _kitchenStations.asStateFlow()

    private val _kitchenEmployees = MutableStateFlow(DefaultSeedData.kitchenEmployees)
    val kitchenEmployees = _kitchenEmployees.asStateFlow()

    private val _stationRoutingRules = MutableStateFlow(DefaultSeedData.stationRoutingRules)
    val stationRoutingRules = _stationRoutingRules.asStateFlow()

    private val _realTimeEvents = MutableStateFlow<List<RealTimeOperationalEvent>>(emptyList())
    val realTimeEvents = _realTimeEvents.asStateFlow()

    private val _realTimeEventStream = MutableSharedFlow<RealTimeOperationalEvent>(extraBufferCapacity = 64)
    val realTimeEventStream = _realTimeEventStream.asSharedFlow()

    // Phase 7 - Customer Portal, Addresses, Loyalty, Reviews & Complaints
    private val _activePortalCustomer = MutableStateFlow<Customer?>(DefaultSeedData.customers.firstOrNull())
    val activePortalCustomer = _activePortalCustomer.asStateFlow()

    private val _customerAddresses = MutableStateFlow(EnterprisePhase7To12SeedData.customerAddresses)
    val customerAddresses = _customerAddresses.asStateFlow()

    private val _loyaltyRewards = MutableStateFlow(EnterprisePhase7To12SeedData.loyaltyRewards)
    val loyaltyRewards = _loyaltyRewards.asStateFlow()

    private val _customerReviews = MutableStateFlow(EnterprisePhase7To12SeedData.customerReviews)
    val customerReviews = _customerReviews.asStateFlow()

    private val _customerComplaints = MutableStateFlow(EnterprisePhase7To12SeedData.customerComplaints)
    val customerComplaints = _customerComplaints.asStateFlow()

    // Phase 8 - Delivery Zones, Drivers, Dispatch Assignments & Driver Cash Settlements
    private val _deliveryZones = MutableStateFlow(EnterprisePhase7To12SeedData.deliveryZones)
    val deliveryZones = _deliveryZones.asStateFlow()

    private val _drivers = MutableStateFlow(EnterprisePhase7To12SeedData.drivers)
    val drivers = _drivers.asStateFlow()

    private val _deliveryAssignments = MutableStateFlow(EnterprisePhase7To12SeedData.initialDeliveryAssignments)
    val deliveryAssignments = _deliveryAssignments.asStateFlow()

    private val _driverCashSettlements = MutableStateFlow<List<DriverCashSettlement>>(emptyList())
    val driverCashSettlements = _driverCashSettlements.asStateFlow()

    // Phase 9 - Warehouses, Unit Conversions, Stock Items, Ledger, Transfers, Suppliers, Purchasing & Waste
    private val _warehouses = MutableStateFlow(EnterprisePhase7To12SeedData.warehouses)
    val warehouses = _warehouses.asStateFlow()

    private val _unitConversionRules = MutableStateFlow(EnterprisePhase7To12SeedData.unitConversionRules)
    val unitConversionRules = _unitConversionRules.asStateFlow()

    private val _warehouseStockItems = MutableStateFlow(EnterprisePhase7To12SeedData.warehouseStockItems)
    val warehouseStockItems = _warehouseStockItems.asStateFlow()

    private val _suppliers = MutableStateFlow(EnterprisePhase7To12SeedData.suppliers)
    val suppliers = _suppliers.asStateFlow()

    private val _inventoryTransactions = MutableStateFlow(EnterprisePhase7To12SeedData.initialInventoryTransactions)
    val inventoryTransactions = _inventoryTransactions.asStateFlow()

    private val _warehouseTransfers = MutableStateFlow(EnterprisePhase7To12SeedData.initialWarehouseTransfers)
    val warehouseTransfers = _warehouseTransfers.asStateFlow()

    private val _purchaseOrders = MutableStateFlow(EnterprisePhase7To12SeedData.initialPurchaseOrders)
    val purchaseOrders = _purchaseOrders.asStateFlow()

    private val _wasteRecords = MutableStateFlow(EnterprisePhase7To12SeedData.initialWasteRecords)
    val wasteRecords = _wasteRecords.asStateFlow()

    private val _stockCountSessions = MutableStateFlow<List<StockCountSession>>(emptyList())
    val stockCountSessions = _stockCountSessions.asStateFlow()

    // Phase 10 - Chart of Accounts, Cost Centers, Financial Periods, Expenses, Journals, Bank & AR/AP
    private val _chartOfAccounts = MutableStateFlow(EnterprisePhase7To12SeedData.chartOfAccounts)
    val chartOfAccounts = _chartOfAccounts.asStateFlow()

    private val _costCenters = MutableStateFlow(EnterprisePhase7To12SeedData.costCenters)
    val costCenters = _costCenters.asStateFlow()

    private val _financialPeriods = MutableStateFlow(EnterprisePhase7To12SeedData.financialPeriods)
    val financialPeriods = _financialPeriods.asStateFlow()

    private val _expenses = MutableStateFlow(EnterprisePhase7To12SeedData.initialExpenses)
    val expenses = _expenses.asStateFlow()

    private val _journalEntries = MutableStateFlow(EnterprisePhase7To12SeedData.initialJournalEntries)
    val journalEntries = _journalEntries.asStateFlow()

    private val _bankAccounts = MutableStateFlow(EnterprisePhase7To12SeedData.bankAccounts)
    val bankAccounts = _bankAccounts.asStateFlow()

    private val _bankTransactions = MutableStateFlow<List<BankTransaction>>(emptyList())
    val bankTransactions = _bankTransactions.asStateFlow()

    private val _subledgerInvoices = MutableStateFlow(EnterprisePhase7To12SeedData.subledgerInvoices)
    val subledgerInvoices = _subledgerInvoices.asStateFlow()

    // Phase 11 - HR, Shift Templates, Attendance, Leave Requests & Payroll
    private val _shiftTemplates = MutableStateFlow(EnterprisePhase7To12SeedData.shiftTemplates)
    val shiftTemplates = _shiftTemplates.asStateFlow()

    private val _attendanceRecords = MutableStateFlow(EnterprisePhase7To12SeedData.attendanceRecords)
    val attendanceRecords = _attendanceRecords.asStateFlow()

    private val _leaveRequests = MutableStateFlow(EnterprisePhase7To12SeedData.leaveRequests)
    val leaveRequests = _leaveRequests.asStateFlow()

    private val _payrollRecords = MutableStateFlow(EnterprisePhase7To12SeedData.payrollRecords)
    val payrollRecords = _payrollRecords.asStateFlow()

    // Phase 12 - Central Multi-Channel Notification Events
    private val _centralNotificationEvents = MutableStateFlow(EnterprisePhase7To12SeedData.centralNotificationEvents)
    val centralNotificationEvents = _centralNotificationEvents.asStateFlow()

    // Track processed idempotency keys in memory as well as Room
    private val processedOrderIdempotencyKeys = mutableSetOf<String>()
    private val processedPaymentIdempotencyKeys = mutableSetOf<String>()
    private val seedMutex = Mutex()

    init {
        scope.launch {
            ensureSeeded(forceReset = false)
            // Establish initial session for admin
            val user = _currentUser.value
            val role = database.roleDao().getRoleByCode(user.roleId)
            val permissions = role?.permissionsJson?.split(",")?.toSet() ?: setOf("*")
            _currentSession.value = UserSession(
                user = user,
                role = Role(
                    id = role?.id ?: "r_admin",
                    code = role?.code ?: "ROLE_ADMIN",
                    nameEn = role?.nameEn ?: "System Administrator",
                    nameAr = role?.nameAr ?: "مدير النظام العام",
                    permissions = permissions.toList()
                ),
                token = UUID.randomUUID().toString(),
                loginTimestamp = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + (8 * 3600 * 1000), // 8 hours
                activeBranchId = user.activeBranchId,
                allowedBranchIds = user.allowedBranchIds,
                effectivePermissions = permissions
            )
        }
    }

    suspend fun ensureSeeded(forceReset: Boolean = false) {
        seedMutex.withLock {
            val alreadySeeded = database.companyDao().getCompanyById("comp_01") != null &&
                database.tableDao().getTableById("t1") != null
            if (!forceReset && alreadySeeded) {
                return
            }
            seedInitialDataIfEmpty()
        }
    }

    // --- Authentication & Session Management ---

    suspend fun login(companyCode: String, username: String, passwordAttempt: String): LoginResult {
        ensureSeeded(forceReset = false)
        val company = database.companyDao().getCompanyById("comp_01")
        if (company == null || !company.code.equals(companyCode.trim(), ignoreCase = true)) {
            recordAudit("LOGIN_FAILED", "Company", companyCode, "Invalid company tenant code: $companyCode")
            return LoginResult.CompanyNotFound
        }

        val userEntity = database.userDao().getUserByUsername(username.trim())
        if (userEntity == null) {
            recordAudit("LOGIN_FAILED", "User", username, "Username not recognized: $username")
            return LoginResult.InvalidCredentials(remainingAttempts = 3)
        }

        if (userEntity.isLocked) {
            recordAudit("LOGIN_BLOCKED", "User", userEntity.id, "Attempted login on locked account: $username")
            return LoginResult.AccountLocked
        }

        if (userEntity.status != "ACTIVE") {
            recordAudit("LOGIN_BLOCKED", "User", userEntity.id, "Attempted login on inactive account (${userEntity.status}): $username")
            return LoginResult.AccountInactive(userEntity.status)
        }

        // Validate password (plain match or default demo check)
        val passwordMatches = userEntity.passwordHash == passwordAttempt || passwordAttempt == "••••••••" || passwordAttempt == "password123"
        if (!passwordMatches) {
            val newAttempts = userEntity.failedAttempts + 1
            val shouldLock = newAttempts >= 5
            database.userDao().updateFailedAttempts(userEntity.id, newAttempts, shouldLock)
            val remaining = (5 - newAttempts).coerceAtLeast(0)
            recordAudit("LOGIN_FAILED", "User", userEntity.id, "Incorrect password attempt ($newAttempts/5) for $username")
            return if (shouldLock) LoginResult.AccountLocked else LoginResult.InvalidCredentials(remaining)
        }

        // Login success: reset failed attempts
        database.userDao().updateFailedAttempts(userEntity.id, 0, false)
        val user = mapUserEntityToDomain(userEntity)
        _currentUser.value = user

        val roleEntity = database.roleDao().getRoleByCode(user.roleId)
        val permissions = roleEntity?.permissionsJson?.split(",")?.toSet() ?: setOf("*")
        val role = Role(
            id = roleEntity?.id ?: "r_${user.roleId}",
            code = roleEntity?.code ?: user.roleId,
            nameEn = roleEntity?.nameEn ?: user.roleId,
            nameAr = roleEntity?.nameAr ?: user.roleId,
            permissions = permissions.toList()
        )

        val session = UserSession(
            user = user,
            role = role,
            token = UUID.randomUUID().toString(),
            loginTimestamp = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + (company.autoLockMinutes * 60 * 1000L).coerceAtLeast(4 * 3600 * 1000L),
            activeBranchId = user.activeBranchId,
            allowedBranchIds = user.allowedBranchIds,
            effectivePermissions = permissions
        )
        _currentSession.value = session

        // Switch to user's branch if available
        val branch = database.branchDao().getBranchById(user.activeBranchId)
        if (branch != null) {
            _currentBranch.value = mapBranchEntityToDomain(branch)
        }

        recordAudit("LOGIN_SUCCESS", "User", user.id, "Session initiated for ${user.fullName} (${user.roleId})")
        return LoginResult.Success(session)
    }

    suspend fun logout() {
        val user = _currentUser.value
        recordAudit("LOGOUT", "User", user.id, "User ${user.fullName} signed out")
        _currentSession.value = null
    }

    fun hasPermission(permissionCode: String): Boolean {
        val session = _currentSession.value ?: return false
        return session.hasPermission(permissionCode)
    }

    fun canAccessBranch(branchId: String): Boolean {
        val session = _currentSession.value ?: return false
        if (session.role.code == "ROLE_ADMIN") return true
        return session.allowedBranchIds.contains(branchId)
    }

    // --- Company Management ---

    fun getCompany(): Flow<Company> {
        return database.companyDao().getCompany().map { entity ->
            if (entity != null) {
                Company(
                    id = entity.id,
                    code = entity.code,
                    nameEn = entity.nameEn,
                    nameAr = entity.nameAr,
                    taxNumber = entity.taxNumber,
                    defaultCurrency = entity.defaultCurrency,
                    country = entity.country,
                    fiscalYear = entity.fiscalYear,
                    autoLockMinutes = entity.autoLockMinutes,
                    isActive = entity.isActive,
                    createdAt = entity.createdAt
                )
            } else _currentCompany.value
        }
    }

    suspend fun updateCompany(company: Company) {
        val entity = CompanyEntity(
            id = company.id,
            code = company.code,
            nameEn = company.nameEn,
            nameAr = company.nameAr,
            taxNumber = company.taxNumber,
            defaultCurrency = company.defaultCurrency,
            country = company.country,
            fiscalYear = company.fiscalYear,
            autoLockMinutes = company.autoLockMinutes,
            isActive = company.isActive,
            createdAt = company.createdAt
        )
        database.companyDao().updateCompany(entity)
        _currentCompany.value = company
        recordAudit("COMPANY_UPDATE", "Company", company.id, "Company profile updated: ${company.nameEn}")
    }

    // --- Brand Management ---

    fun getAllBrands(): Flow<List<Brand>> {
        return database.brandDao().getAllBrands().map { list ->
            list.map {
                Brand(
                    id = it.id,
                    companyId = it.companyId,
                    code = it.code,
                    nameEn = it.nameEn,
                    nameAr = it.nameAr,
                    status = it.status,
                    logoUrl = it.logoUrl,
                    primaryColor = it.primaryColor,
                    secondaryColor = it.secondaryColor,
                    menuIdentity = it.menuIdentity,
                    isActive = it.isActive
                )
            }
        }
    }

    suspend fun saveBrand(brand: Brand) {
        val entity = BrandEntity(
            id = brand.id,
            companyId = brand.companyId,
            code = brand.code,
            nameEn = brand.nameEn,
            nameAr = brand.nameAr,
            status = brand.status,
            logoUrl = brand.logoUrl,
            primaryColor = brand.primaryColor,
            secondaryColor = brand.secondaryColor,
            menuIdentity = brand.menuIdentity,
            isActive = brand.isActive
        )
        database.brandDao().insertBrands(listOf(entity))
        recordAudit("BRAND_SAVE", "Brand", brand.id, "Brand configuration saved: ${brand.nameEn}")
    }

    // --- Branch & POS Terminal Management ---

    fun getAllBranches(): Flow<List<Branch>> {
        return database.branchDao().getAllBranches().map { list ->
            list.map { mapBranchEntityToDomain(it) }
        }
    }

    fun getTerminalsForBranch(branchId: String): Flow<List<PosTerminal>> {
        return database.posTerminalDao().getTerminalsForBranch(branchId).map { list ->
            list.map {
                PosTerminal(
                    id = it.id,
                    branchId = it.branchId,
                    terminalCode = it.terminalCode,
                    name = it.name,
                    status = it.status,
                    ipAddress = it.ipAddress
                )
            }
        }
    }

    suspend fun saveBranch(branch: Branch) {
        val entity = BranchEntity(
            id = branch.id,
            brandId = branch.brandId,
            code = branch.code,
            nameEn = branch.nameEn,
            nameAr = branch.nameAr,
            phone = branch.phone,
            address = branch.address,
            seatingCapacity = branch.seatingCapacity,
            isActive = branch.isActive,
            openingTime = branch.openingTime,
            closingTime = branch.closingTime,
            timezone = branch.timezone,
            currency = branch.currency,
            taxRate = branch.taxRate,
            taxInclusive = branch.taxInclusive,
            assignedWarehouseId = branch.assignedWarehouseId
        )
        database.branchDao().insertBranch(entity)
        recordAudit("BRANCH_SAVE", "Branch", branch.id, "Branch saved: ${branch.nameEn} (${branch.code})")
    }

    suspend fun toggleBranchStatus(branchId: String, isActive: Boolean) {
        database.branchDao().updateBranchStatus(branchId, isActive)
        recordAudit("BRANCH_STATUS", "Branch", branchId, "Branch status changed to active: $isActive")
    }

    suspend fun addPosTerminal(terminal: PosTerminal) {
        val entity = PosTerminalEntity(
            id = terminal.id,
            branchId = terminal.branchId,
            terminalCode = terminal.terminalCode,
            name = terminal.name,
            status = terminal.status,
            ipAddress = terminal.ipAddress
        )
        database.posTerminalDao().insertTerminal(entity)
        recordAudit("TERMINAL_ADD", "PosTerminal", terminal.id, "POS Terminal registered: ${terminal.name} (${terminal.terminalCode})")
    }

    suspend fun deletePosTerminal(terminalId: String) {
        database.posTerminalDao().deleteTerminal(terminalId)
        recordAudit("TERMINAL_DELETE", "PosTerminal", terminalId, "POS Terminal removed")
    }

    // --- Users & Identity Management ---

    fun getAllUsers(): Flow<List<User>> {
        return database.userDao().getAllUsers().map { list ->
            list.map { mapUserEntityToDomain(it) }
        }
    }

    suspend fun saveUser(user: User) {
        val entity = UserEntity(
            id = user.id,
            companyId = user.companyId,
            username = user.username,
            email = user.email,
            fullName = user.fullName,
            roleId = user.roleId,
            activeBranchId = user.activeBranchId,
            status = user.status,
            passwordHash = user.passwordHash,
            allowedBranchIds = user.allowedBranchIds.joinToString(","),
            failedAttempts = user.failedAttempts,
            isLocked = user.isLocked,
            lastLoginAt = user.lastLoginAt
        )
        database.userDao().insertUser(entity)
        recordAudit("USER_SAVE", "User", user.id, "User account saved: ${user.fullName} (${user.username})")
    }

    suspend fun toggleUserStatus(userId: String, newStatus: String) {
        database.userDao().updateUserStatus(userId, newStatus)
        recordAudit("USER_STATUS_CHANGE", "User", userId, "User account status updated to $newStatus")
    }

    suspend fun resetUserPassword(userId: String, newPasswordHash: String) {
        database.userDao().resetPassword(userId, newPasswordHash)
        recordAudit("PASSWORD_RESET", "User", userId, "Password credentials reset by administrator")
    }

    // --- Roles & Permissions Management ---

    fun getAllRoles(): Flow<List<Role>> {
        return database.roleDao().getAllRoles().map { list ->
            list.map {
                Role(
                    id = it.id,
                    code = it.code,
                    nameEn = it.nameEn,
                    nameAr = it.nameAr,
                    permissions = it.permissionsJson.split(",").filter { s -> s.isNotBlank() },
                    isSystem = it.isSystem,
                    branchRestricted = it.branchRestricted,
                    description = it.description
                )
            }
        }
    }

    suspend fun saveRole(role: Role) {
        val entity = RoleEntity(
            id = role.id,
            code = role.code,
            nameEn = role.nameEn,
            nameAr = role.nameAr,
            permissionsJson = role.permissions.joinToString(","),
            isSystem = role.isSystem,
            branchRestricted = role.branchRestricted,
            description = role.description
        )
        database.roleDao().insertRole(entity)
        recordAudit("ROLE_SAVE", "Role", role.id, "Role configuration saved: ${role.nameEn}")
    }

    suspend fun deleteCustomRole(roleId: String) {
        database.roleDao().deleteCustomRole(roleId)
        recordAudit("ROLE_DELETE", "Role", roleId, "Custom role deleted")
    }

    // --- Employee Profiles Management ---

    fun getAllEmployees(): Flow<List<Employee>> {
        return database.employeeDao().getAllEmployees().map { list ->
            list.map {
                Employee(
                    id = it.id,
                    branchId = it.branchId,
                    userId = it.userId,
                    employeeNumber = it.employeeNumber,
                    jobTitle = it.jobTitle,
                    hourlyRate = it.hourlyRate,
                    status = it.status,
                    fullName = it.fullName,
                    department = it.department,
                    phone = it.phone,
                    email = it.email,
                    hireDate = it.hireDate
                )
            }
        }
    }

    suspend fun saveEmployee(emp: Employee) {
        val entity = EmployeeEntity(
            id = emp.id,
            branchId = emp.branchId,
            userId = emp.userId,
            employeeNumber = emp.employeeNumber,
            fullName = emp.fullName,
            jobTitle = emp.jobTitle,
            department = emp.department,
            phone = emp.phone,
            email = emp.email,
            hourlyRate = emp.hourlyRate,
            status = emp.status,
            hireDate = emp.hireDate
        )
        database.employeeDao().insertEmployee(entity)
        recordAudit("EMPLOYEE_SAVE", "Employee", emp.id, "Staff employee profile saved: ${emp.fullName} (${emp.employeeNumber})")
    }

    suspend fun updateEmployeeStatus(empId: String, newStatus: String) {
        database.employeeDao().updateEmployeeStatus(empId, newStatus)
        recordAudit("EMPLOYEE_STATUS_CHANGE", "Employee", empId, "Employee status changed to $newStatus")
    }

    // --- Core Operations Flow & Phase 3 Product & Menu Engine ---

    fun getProducts(): Flow<List<Product>> {
        return _richProducts.asStateFlow()
    }

    fun getCategories(): Flow<List<Category>> = _categories.asStateFlow()
    fun getIngredients(): Flow<List<Ingredient>> = _ingredients.asStateFlow()
    fun getRecipes(): Flow<List<Recipe>> = _recipes.asStateFlow()
    fun getCombos(): Flow<List<ComboProduct>> = _combos.asStateFlow()
    fun getModifierGroups(): Flow<List<ModifierGroup>> = _modifierGroups.asStateFlow()
    fun getAddons(): Flow<List<ProductAddon>> = _addons.asStateFlow()
    fun getPriceRules(): Flow<List<ChannelPriceRule>> = _priceRules.asStateFlow()
    fun getTaxProfiles(): Flow<List<TaxProfile>> = _taxProfiles.asStateFlow()
    fun getSchedules(): Flow<List<ProductSchedule>> = _schedules.asStateFlow()

    suspend fun saveProduct(product: Product) {
        val current = _richProducts.value.toMutableList()
        val index = current.indexOfFirst { it.id == product.id }
        if (index >= 0) {
            current[index] = product
        } else {
            current.add(product)
        }
        _richProducts.value = current

        // Keep local database in sync
        val entity = ProductEntity(
            id = product.id,
            categoryId = product.categoryId,
            sku = product.sku,
            nameEn = product.nameEn,
            nameAr = product.nameAr,
            price = product.price,
            costPrice = product.costPrice,
            isAvailable = product.isAvailable,
            isTaxable = product.isTaxable,
            currentStock = product.currentStock,
            reorderPoint = product.reorderPoint
        )
        database.productDao().insertProducts(listOf(entity))
        recordAudit("PRODUCT_SAVE", "Product", product.id, "Saved product: ${product.nameEn} (SKU: ${product.sku}, Price: $${product.price})")
    }

    suspend fun deleteProduct(productId: String) {
        _richProducts.value = _richProducts.value.filter { it.id != productId }
        recordAudit("PRODUCT_DELETE", "Product", productId, "Deleted product $productId")
    }

    suspend fun toggleProductAvailability(productId: String, isAvailable: Boolean) {
        _richProducts.value = _richProducts.value.map {
            if (it.id == productId) it.copy(isAvailable = isAvailable) else it
        }
        recordAudit("PRODUCT_AVAILABILITY_CHANGE", "Product", productId, "Product availability toggled to $isAvailable")
    }

    suspend fun saveCategory(category: Category) {
        val current = _categories.value.toMutableList()
        val index = current.indexOfFirst { it.id == category.id }
        if (index >= 0) {
            current[index] = category
        } else {
            current.add(category)
        }
        _categories.value = current
        recordAudit("CATEGORY_SAVE", "Category", category.id, "Saved category: ${category.nameEn}")
    }

    suspend fun deleteCategory(categoryId: String) {
        _categories.value = _categories.value.filter { it.id != categoryId }
        recordAudit("CATEGORY_DELETE", "Category", categoryId, "Deleted category $categoryId")
    }

    suspend fun saveIngredient(ingredient: Ingredient) {
        val current = _ingredients.value.toMutableList()
        val index = current.indexOfFirst { it.id == ingredient.id }
        if (index >= 0) {
            current[index] = ingredient
        } else {
            current.add(ingredient)
        }
        _ingredients.value = current
        recordAudit("INGREDIENT_SAVE", "Ingredient", ingredient.id, "Saved ingredient: ${ingredient.nameEn} ($${ingredient.costPerUnit}/${ingredient.unit})")
    }

    suspend fun deleteIngredient(ingredientId: String) {
        _ingredients.value = _ingredients.value.filter { it.id != ingredientId }
        recordAudit("INGREDIENT_DELETE", "Ingredient", ingredientId, "Deleted ingredient $ingredientId")
    }

    suspend fun saveRecipe(recipe: Recipe) {
        val current = _recipes.value.toMutableList()
        val index = current.indexOfFirst { it.id == recipe.id }
        if (index >= 0) {
            current[index] = recipe
        } else {
            current.add(recipe)
        }
        _recipes.value = current
        recordAudit("RECIPE_SAVE", "Recipe", recipe.id, "Saved recipe: ${recipe.name}")
    }

    suspend fun deleteRecipe(recipeId: String) {
        _recipes.value = _recipes.value.filter { it.id != recipeId }
        recordAudit("RECIPE_DELETE", "Recipe", recipeId, "Deleted recipe $recipeId")
    }

    suspend fun saveCombo(combo: ComboProduct) {
        val current = _combos.value.toMutableList()
        val index = current.indexOfFirst { it.id == combo.id }
        if (index >= 0) {
            current[index] = combo
        } else {
            current.add(combo)
        }
        _combos.value = current
        recordAudit("COMBO_SAVE", "ComboProduct", combo.id, "Saved combo: ${combo.nameEn}")
    }

    suspend fun deleteCombo(comboId: String) {
        _combos.value = _combos.value.filter { it.id != comboId }
        recordAudit("COMBO_DELETE", "ComboProduct", comboId, "Deleted combo $comboId")
    }

    suspend fun saveModifierGroup(group: ModifierGroup) {
        val current = _modifierGroups.value.toMutableList()
        val index = current.indexOfFirst { it.id == group.id }
        if (index >= 0) {
            current[index] = group
        } else {
            current.add(group)
        }
        _modifierGroups.value = current
        recordAudit("MODIFIER_GROUP_SAVE", "ModifierGroup", group.id, "Saved modifier group: ${group.nameEn}")
    }

    suspend fun deleteModifierGroup(groupId: String) {
        _modifierGroups.value = _modifierGroups.value.filter { it.id != groupId }
        recordAudit("MODIFIER_GROUP_DELETE", "ModifierGroup", groupId, "Deleted modifier group $groupId")
    }

    suspend fun saveAddon(addon: ProductAddon) {
        val current = _addons.value.toMutableList()
        val index = current.indexOfFirst { it.id == addon.id }
        if (index >= 0) {
            current[index] = addon
        } else {
            current.add(addon)
        }
        _addons.value = current
        recordAudit("ADDON_SAVE", "ProductAddon", addon.id, "Saved addon: ${addon.nameEn}")
    }

    suspend fun deleteAddon(addonId: String) {
        _addons.value = _addons.value.filter { it.id != addonId }
        recordAudit("ADDON_DELETE", "ProductAddon", addonId, "Deleted addon $addonId")
    }

    suspend fun savePriceRule(rule: ChannelPriceRule) {
        val current = _priceRules.value.toMutableList()
        val index = current.indexOfFirst { it.id == rule.id }
        if (index >= 0) {
            current[index] = rule
        } else {
            current.add(rule)
        }
        _priceRules.value = current
        recordAudit("PRICE_RULE_SAVE", "ChannelPriceRule", rule.id, "Saved price rule for product ${rule.productId}: $${rule.customPrice}")
    }

    suspend fun deletePriceRule(ruleId: String) {
        _priceRules.value = _priceRules.value.filter { it.id != ruleId }
        recordAudit("PRICE_RULE_DELETE", "ChannelPriceRule", ruleId, "Deleted price rule $ruleId")
    }

    suspend fun saveTaxProfile(profile: TaxProfile) {
        val current = _taxProfiles.value.toMutableList()
        val index = current.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            current[index] = profile
        } else {
            current.add(profile)
        }
        _taxProfiles.value = current
        recordAudit("TAX_PROFILE_SAVE", "TaxProfile", profile.id, "Saved tax profile: ${profile.nameEn}")
    }

    fun getTables(branchId: String): Flow<List<RestaurantTable>> {
        return database.tableDao().getTablesByBranch(branchId).map { list ->
            list.map { mapTableEntityToDomain(it) }
        }
    }

    fun getReservations(branchId: String): Flow<List<Reservation>> {
        return database.reservationDao().getReservationsByBranch(branchId).map { list ->
            if (list.isEmpty()) {
                _reservations.value.filter { it.branchId == branchId }
            } else {
                list.map { mapReservationEntityToDomain(it) }
            }
        }
    }

    fun getOrders(branchId: String): Flow<List<Order>> {
        return combine(
            database.orderDao().getOrdersByBranch(branchId),
            database.orderDao().getAllOrderItems(),
            database.paymentDao().getPaymentsByBranch(branchId)
        ) { orderEntities, allItemEntities, branchPaymentEntities ->
            val itemsByOrder = allItemEntities.groupBy { it.orderId }
            val paymentsByOrder = branchPaymentEntities.groupBy { it.orderId }
            orderEntities.map { entity ->
                val domainItems = (itemsByOrder[entity.id] ?: emptyList()).map { mapOrderItemEntityToDomain(it) }
                val domainPayments = (paymentsByOrder[entity.id] ?: emptyList()).map { mapPaymentEntityToDomain(it) }
                mapOrderEntityToDomain(entity, domainItems, domainPayments)
            }
        }
    }

    fun getAuditLogs(): Flow<List<AuditLog>> {
        return database.auditDao().getRecentAuditLogs().map { list ->
            list.map {
                AuditLog(
                    id = it.id,
                    companyId = it.companyId,
                    branchId = it.branchId,
                    userId = it.userId,
                    userName = it.userName,
                    action = it.action,
                    entityName = it.entityName,
                    entityId = it.entityId,
                    details = it.details,
                    timestamp = it.timestamp
                )
            }
        }
    }

    suspend fun placeOrder(
        order: Order,
        items: List<OrderItem>,
        payments: List<Payment> = order.payments
    ): Boolean {
        // Idempotency protection: prevent duplicate order creation for the same idempotencyKey
        val existingByIdem = database.orderDao().getOrderByIdempotencyKey(order.idempotencyKey)
        if (existingByIdem != null && existingByIdem.id != order.id) {
            recordAudit(
                action = "IDEMPOTENCY_GUARD",
                entity = "Order",
                id = existingByIdem.id,
                details = "Ignored duplicate order submission with idempotencyKey=${order.idempotencyKey}"
            )
            return false
        }
        val isEditingExisting = database.orderDao().getOrderById(order.id) != null
        processedOrderIdempotencyKeys.add(order.idempotencyKey)

        val effectiveSyncStatus = if (_isOfflineMode.value) SyncStatus.QUEUED_OFFLINE else SyncStatus.SYNCED
        val finalOrder = order.copy(
            syncStatus = effectiveSyncStatus,
            updatedAt = System.currentTimeMillis()
        )

        val orderEntity = mapOrderDomainToEntity(finalOrder)
        val itemEntities = items.map { mapOrderItemDomainToEntity(it.copy(orderId = finalOrder.id)) }

        database.orderDao().insertOrder(orderEntity)
        database.orderDao().deleteItemsForOrder(finalOrder.id)
        database.orderDao().insertOrderItems(itemEntities)

        // Process & persist payments with idempotency deduplication
        val newValidPayments = mutableListOf<Payment>()
        for (payment in payments) {
            val existingPay = database.paymentDao().getPaymentByIdempotencyKey(payment.idempotencyKey)
            if (existingPay == null && !processedPaymentIdempotencyKeys.contains(payment.idempotencyKey)) {
                processedPaymentIdempotencyKeys.add(payment.idempotencyKey)
                val shiftId = _currentShift.value?.id
                val enrichedPay = payment.copy(orderId = finalOrder.id, shiftId = shiftId)
                database.paymentDao().insertPayment(mapPaymentDomainToEntity(enrichedPay))
                newValidPayments.add(enrichedPay)
            }
        }

        // Update active CashierShift totals for new payments
        if (newValidPayments.isNotEmpty()) {
            _currentShift.value?.let { shift ->
                var addCash = 0.0
                var addCard = 0.0
                var addBank = 0.0
                var addWallet = 0.0
                newValidPayments.forEach { pay ->
                    when (pay.paymentMethod) {
                        PaymentMethod.CASH -> addCash += pay.amount
                        PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD, PaymentMethod.GIFT_CARD -> addCard += pay.amount
                        PaymentMethod.BANK_TRANSFER -> addBank += pay.amount
                        PaymentMethod.DIGITAL_WALLET, PaymentMethod.MOBILE_PAY -> addWallet += pay.amount
                    }
                }
                val updatedShift = shift.copy(
                    cashSales = shift.cashSales + addCash,
                    cardSales = shift.cardSales + addCard,
                    bankTransferSales = shift.bankTransferSales + addBank,
                    walletSales = shift.walletSales + addWallet
                )
                _currentShift.value = updatedShift
                updateShiftInHistory(updatedShift)
            }
        }

        // Update table status for Dine-In
        if (finalOrder.tableId != null) {
            val newTableStatus = when (finalOrder.status) {
                OrderStatus.COMPLETED, OrderStatus.CANCELLED -> TableStatus.AVAILABLE
                OrderStatus.HELD -> TableStatus.RESERVED
                else -> TableStatus.OCCUPIED
            }
            val linkedOrderId = if (newTableStatus == TableStatus.AVAILABLE) null else finalOrder.id
            database.tableDao().updateTableStatus(finalOrder.tableId, newTableStatus.name, linkedOrderId)
        }

        // Decrement stock for newly placed (non-held) orders
        if (!isEditingExisting && finalOrder.status != OrderStatus.HELD) {
            _richProducts.value = _richProducts.value.map { prod ->
                val orderedQty = items.filter { it.productId == prod.id }.sumOf { it.quantity }
                if (orderedQty > 0 && prod.isInventoryItem) {
                    prod.copy(currentStock = (prod.currentStock - orderedQty).coerceAtLeast(0.0))
                } else {
                    prod
                }
            }
        }

        // Dispatch to Kitchen Display System (KDS) with automatic Station Routing if order is active preparation
        if (finalOrder.status in listOf(OrderStatus.PREPARING, OrderStatus.PENDING)) {
            val branchTables = database.tableDao().getTablesByBranch(finalOrder.branchId).first().map { mapTableEntityToDomain(it) }
            val existingOrderTickets = _kitchenTickets.value.filter { it.orderId == finalOrder.id }

            val routingOutcome = KitchenEngine.routeOrderToStations(
                order = finalOrder,
                items = items,
                products = _richProducts.value,
                categories = _categories.value,
                stations = _kitchenStations.value,
                routingRules = _stationRoutingRules.value,
                kitchenEmployees = _kitchenEmployees.value,
                tables = branchTables,
                existingTicketsForOrder = existingOrderTickets
            )

            if (routingOutcome.stationTickets.isNotEmpty()) {
                database.kitchenTicketDao().insertTickets(
                    routingOutcome.stationTickets.map { mapKitchenTicketDomainToEntity(it) }
                )
                val otherTickets = _kitchenTickets.value.filter { it.orderId != finalOrder.id || it.workflowStage != "STATION_PREP" }
                _kitchenTickets.value = routingOutcome.stationTickets + otherTickets

                publishRealTimeEvent(
                    type = RealTimeEventType.KITCHEN_TICKET_UPDATED,
                    branchId = finalOrder.branchId,
                    entityId = finalOrder.id,
                    referenceCode = finalOrder.orderNumber,
                    titleEn = "Routed ${finalOrder.orderNumber} to ${routingOutcome.stationTickets.size} Station(s)",
                    titleAr = "تم توجيه الطلب ${finalOrder.orderNumber} إلى ${routingOutcome.stationTickets.size} محطات",
                    detailsEn = routingOutcome.routedStationsSummary,
                    detailsAr = routingOutcome.routedStationsSummary,
                    sourceChannel = finalOrder.channel.name
                )
            }

            // Phase 9 & 10: Automatic Recipe Inventory Consumption & Double-Entry Accounting Journal Posting
            if (!isEditingExisting) {
                val consumption = InventoryProcurementEngine.consumeInventoryForOrder(
                    order = finalOrder,
                    orderItems = items,
                    recipes = _recipes.value,
                    currentStockItems = _warehouseStockItems.value,
                    unitRules = _unitConversionRules.value,
                    targetWarehouseId = "wh_branch_01",
                    performedBy = finalOrder.cashierName
                )
                _warehouseStockItems.value = consumption.updatedStockItems
                if (consumption.transactions.isNotEmpty()) {
                    _inventoryTransactions.value = consumption.transactions + _inventoryTransactions.value
                }

                // Post automatic balanced Double-Entry Journal Entry
                val saleJournal = AccountingEngine.createSaleJournalEntry(
                    order = finalOrder,
                    cogsAmount = consumption.totalCogsCost
                )
                _journalEntries.value = listOf(saleJournal) + _journalEntries.value

                // Phase 7: Update Customer CRM Spend & Loyalty Points if customer is linked
                val custId = finalOrder.customerId
                if (custId != null) {
                    _customers.value = _customers.value.map { cust ->
                        if (cust.id == custId) {
                            val earnedPts = CustomerCrmEngine.computeEarnedPointsForOrder(finalOrder.subtotal, cust.loyaltyPoints)
                            val newPts = cust.loyaltyPoints + earnedPts
                            val newTier = CustomerCrmEngine.resolveLoyaltyTier(newPts).code
                            val newCount = cust.orderCount + 1
                            val newSpend = cust.totalSpend + finalOrder.totalAmount
                            val updatedCust = cust.copy(
                                loyaltyPoints = newPts,
                                tier = newTier,
                                orderCount = newCount,
                                totalSpend = newSpend,
                                averageOrderValue = newSpend / newCount,
                                lastOrderAt = System.currentTimeMillis()
                            )
                            if (_activePortalCustomer.value?.id == custId) {
                                _activePortalCustomer.value = updatedCust
                            }
                            updatedCust
                        } else cust
                    }
                }

                // Phase 8: Automatically create Delivery Dispatch Assignment for DELIVERY orders
                if (finalOrder.orderType == OrderType.DELIVERY && _deliveryAssignments.value.none { it.orderId == finalOrder.id }) {
                    val defaultZone = _deliveryZones.value.firstOrNull() ?: DeliveryZone(
                        id = "dzone_01",
                        branchId = finalOrder.branchId,
                        code = "ZONE-A",
                        nameEn = "Zone A • Olaya Core",
                        nameAr = "المنطقة أ • العليا",
                        boundaryDescription = "0-5 km",
                        radiusKm = 5.0,
                        deliveryFee = finalOrder.deliveryFee.takeIf { it > 0 } ?: 3.50,
                        minimumOrderAmount = 15.0,
                        estimatedMinutes = 30
                    )
                    val newAssignment = DeliveryAssignment(
                        id = "deliv_${finalOrder.id}",
                        orderId = finalOrder.id,
                        orderNumber = finalOrder.orderNumber,
                        branchId = finalOrder.branchId,
                        deliveryZoneId = defaultZone.id,
                        deliveryZoneName = defaultZone.nameEn,
                        driverId = null,
                        driverName = null,
                        driverPhone = null,
                        customerName = finalOrder.customerName ?: "Delivery Guest",
                        customerPhone = finalOrder.customerPhone ?: "+966 50 000 0000",
                        deliveryAddress = finalOrder.deliveryAddress.ifBlank { "Riyadh Zone A Delivery Address" },
                        deliveryNotes = finalOrder.notes,
                        status = DeliveryWorkflowStatus.AWAITING_DRIVER,
                        orderTotalAmount = finalOrder.totalAmount,
                        paymentMethod = finalOrder.payments.firstOrNull()?.paymentMethod ?: PaymentMethod.CASH,
                        isCashOnDelivery = finalOrder.paymentStatus != "PAID" || finalOrder.payments.any { it.paymentMethod == PaymentMethod.CASH },
                        customerOtpCode = ((1000..9999).random()).toString(),
                        estimatedDeliveryMinutes = defaultZone.estimatedMinutes
                    )
                    _deliveryAssignments.value = listOf(newAssignment) + _deliveryAssignments.value
                }

                // Phase 12: Central Notification Service dispatch
                dispatchCentralNotification(
                    eventTrigger = "ORDER_CREATED",
                    titleEn = "New Order ${finalOrder.orderNumber} (${finalOrder.orderType.titleEn})",
                    titleAr = "طلب جديد ${finalOrder.orderNumber} (${finalOrder.orderType.titleAr})",
                    messageEn = "Total $${String.format(java.util.Locale.US, "%.2f", finalOrder.totalAmount)} • Routed to KDS & Accounting Ledger",
                    messageAr = "الإجمالي $${String.format(java.util.Locale.US, "%.2f", finalOrder.totalAmount)} • تم التوجيه للمطبخ والمحاسبة",
                    channels = listOf(NotificationChannelType.IN_APP, NotificationChannelType.PUSH_READY, NotificationChannelType.WHATSAPP_READY),
                    referenceId = finalOrder.orderNumber
                )
            }
        }

        // Publish real-time order event
        publishRealTimeEvent(
            type = if (isEditingExisting) RealTimeEventType.ORDER_STATUS_CHANGED else RealTimeEventType.NEW_ORDER,
            branchId = finalOrder.branchId,
            entityId = finalOrder.id,
            referenceCode = finalOrder.orderNumber,
            titleEn = if (isEditingExisting) "Order ${finalOrder.orderNumber} Modified" else "New Order ${finalOrder.orderNumber} (${finalOrder.channel.titleEn})",
            titleAr = if (isEditingExisting) "تعديل الطلب ${finalOrder.orderNumber}" else "طلب جديد ${finalOrder.orderNumber} (${finalOrder.channel.titleAr})",
            detailsEn = "${items.sumOf { it.quantity }} items • ${finalOrder.orderType.titleEn} • $${String.format(java.util.Locale.US, "%.2f", finalOrder.totalAmount)}",
            detailsAr = "${items.sumOf { it.quantity }} أصناف • ${finalOrder.orderType.titleAr} • $${String.format(java.util.Locale.US, "%.2f", finalOrder.totalAmount)}",
            sourceChannel = finalOrder.channel.name
        )

        // Queue offline sync record if in offline mode
        if (_isOfflineMode.value) {
            enqueueOfflineOperation(
                idempotencyKey = finalOrder.idempotencyKey,
                branchId = finalOrder.branchId,
                operationType = if (isEditingExisting) "UPDATE_ORDER" else "PLACE_ORDER",
                entityId = finalOrder.id,
                summary = "${finalOrder.orderNumber} (${finalOrder.orderType.titleEn}) - $${String.format(java.util.Locale.US, "%.2f", finalOrder.totalAmount)}"
            )
        }

        val auditAction = when {
            finalOrder.status == OrderStatus.HELD -> "HOLD_ORDER"
            isEditingExisting -> "EDIT_ORDER"
            else -> "CREATE_ORDER"
        }
        recordAudit(
            action = auditAction,
            entity = "Order",
            id = finalOrder.id,
            details = "Order ${finalOrder.orderNumber} [${finalOrder.orderType.code}/${finalOrder.status.code}] (${items.size} lines, Total: $${finalOrder.totalAmount}, Sync: ${finalOrder.syncStatus.name})"
        )

        addNotification(
            titleEn = if (finalOrder.status == OrderStatus.HELD) "Order Held ${finalOrder.orderNumber}" else "Order ${finalOrder.orderNumber} Dispatched",
            titleAr = if (finalOrder.status == OrderStatus.HELD) "تعليق الطلب ${finalOrder.orderNumber}" else "تم إرسال الطلب ${finalOrder.orderNumber}",
            bodyEn = "${items.sumOf { it.quantity }} items • $${String.format(java.util.Locale.US, "%.2f", finalOrder.totalAmount)} (${finalOrder.orderType.titleEn})",
            bodyAr = "${items.sumOf { it.quantity }} أصناف • $${String.format(java.util.Locale.US, "%.2f", finalOrder.totalAmount)} (${finalOrder.orderType.titleAr})",
            type = NotificationType.ORDER
        )
        return true
    }

    suspend fun transitionOrderState(
        orderId: String,
        targetStatus: OrderStatus,
        reason: String = "",
        approvedBy: String? = null
    ): OrderEngine.OrderTransitionResult {
        val entity = database.orderDao().getOrderById(orderId)
            ?: return OrderEngine.OrderTransitionResult.InvalidTransition(OrderStatus.DRAFT, targetStatus, "Order not found")
        val items = database.orderDao().getItemsForOrder(orderId).map { mapOrderItemEntityToDomain(it) }
        val payments = database.paymentDao().getPaymentsForOrder(orderId).map { mapPaymentEntityToDomain(it) }
        val currentOrder = mapOrderEntityToDomain(entity, items, payments)

        val result = OrderEngine.transitionOrder(
            order = currentOrder,
            targetStatus = targetStatus,
            reason = reason,
            approvedBy = approvedBy
        )

        if (result is OrderEngine.OrderTransitionResult.Success) {
            val updated = result.updatedOrder.copy(
                syncStatus = if (_isOfflineMode.value) SyncStatus.QUEUED_OFFLINE else SyncStatus.SYNCED
            )
            database.orderDao().insertOrder(mapOrderDomainToEntity(updated))

            // Free table if completed or cancelled
            if (updated.tableId != null && targetStatus in listOf(OrderStatus.COMPLETED, OrderStatus.CANCELLED, OrderStatus.REFUNDED)) {
                database.tableDao().updateTableStatus(updated.tableId, TableStatus.AVAILABLE.name, null)
            }

            // Update KDS ticket if applicable
            if (targetStatus in listOf(OrderStatus.READY, OrderStatus.SERVED, OrderStatus.COMPLETED, OrderStatus.CANCELLED)) {
                val mappedKdsStatus = when (targetStatus) {
                    OrderStatus.READY -> KdsTicketStatus.READY
                    OrderStatus.SERVED, OrderStatus.COMPLETED -> KdsTicketStatus.COMPLETED
                    OrderStatus.CANCELLED -> KdsTicketStatus.CANCELLED
                    else -> KdsTicketStatus.PREPARING
                }
                _kitchenTickets.value = _kitchenTickets.value.map {
                    if (it.orderId == orderId) {
                        val updatedTkt = it.copy(status = mappedKdsStatus.code, ticketStatus = mappedKdsStatus)
                        database.kitchenTicketDao().insertTicket(mapKitchenTicketDomainToEntity(updatedTkt))
                        updatedTkt
                    } else it
                }
            }

            publishRealTimeEvent(
                type = RealTimeEventType.ORDER_STATUS_CHANGED,
                branchId = updated.branchId,
                entityId = orderId,
                referenceCode = updated.orderNumber,
                titleEn = "Order ${updated.orderNumber} -> ${targetStatus.titleEn}",
                titleAr = "الطلب ${updated.orderNumber} -> ${targetStatus.titleAr}",
                detailsEn = "Transitioned from ${currentOrder.status.titleEn} to ${targetStatus.titleEn}",
                detailsAr = "تحول من ${currentOrder.status.titleAr} إلى ${targetStatus.titleAr}",
                sourceChannel = updated.channel.name
            )

            if (_isOfflineMode.value) {
                enqueueOfflineOperation(
                    idempotencyKey = "trans_${orderId}_${targetStatus.code}_${System.currentTimeMillis()}",
                    branchId = updated.branchId,
                    operationType = "STATE_TRANSITION_${targetStatus.code}",
                    entityId = orderId,
                    summary = "${updated.orderNumber}: ${currentOrder.status.code} -> ${targetStatus.code}"
                )
            }

            recordAudit(
                action = "ORDER_STATE_${targetStatus.code}",
                entity = "Order",
                id = orderId,
                details = "Order ${updated.orderNumber} transitioned ${currentOrder.status.code} -> ${targetStatus.code}. Reason: ${reason.ifBlank { "Standard flow" }}"
            )
        }
        return result
    }

    suspend fun refundOrReturnOrder(
        orderId: String,
        isReturnItems: Boolean,
        refundAmount: Double,
        reason: String,
        returnedItemQuantities: Map<String, Int> = emptyMap(),
        refundMethod: PaymentMethod = PaymentMethod.CASH,
        restockInventory: Boolean = true,
        approvedBy: String? = null
    ): OrderEngine.OrderTransitionResult {
        val entity = database.orderDao().getOrderById(orderId)
            ?: return OrderEngine.OrderTransitionResult.InvalidTransition(OrderStatus.DRAFT, OrderStatus.REFUNDED, "Order not found")
        val items = database.orderDao().getItemsForOrder(orderId).map { mapOrderItemEntityToDomain(it) }
        val payments = database.paymentDao().getPaymentsForOrder(orderId).map { mapPaymentEntityToDomain(it) }
        val currentOrder = mapOrderEntityToDomain(entity, items, payments)

        val targetStatus = if (isReturnItems) OrderStatus.RETURNED else OrderStatus.REFUNDED
        val transitionResult = OrderEngine.transitionOrder(
            order = currentOrder,
            targetStatus = targetStatus,
            reason = reason,
            approvedBy = approvedBy
        )

        if (transitionResult is OrderEngine.OrderTransitionResult.Success) {
            val cleanRefund = refundAmount.coerceIn(0.0, currentOrder.totalAmount)
            val updatedOrder = transitionResult.updatedOrder.copy(
                refundedAmount = cleanRefund,
                refundReason = reason,
                paymentStatus = "REFUNDED",
                syncStatus = if (_isOfflineMode.value) SyncStatus.QUEUED_OFFLINE else SyncStatus.SYNCED
            )
            database.orderDao().insertOrder(mapOrderDomainToEntity(updatedOrder))

            // Update returned quantities on items & restock inventory
            if (returnedItemQuantities.isNotEmpty()) {
                val updatedItems = items.map { item ->
                    val retQty = returnedItemQuantities[item.id] ?: 0
                    if (retQty > 0) item.copy(returnedQuantity = (item.returnedQuantity + retQty).coerceAtMost(item.quantity)) else item
                }
                database.orderDao().insertOrderItems(updatedItems.map { mapOrderItemDomainToEntity(it) })

                if (restockInventory) {
                    _richProducts.value = _richProducts.value.map { prod ->
                        val returnedForProd = items.filter { it.productId == prod.id }
                            .sumOf { returnedItemQuantities[it.id] ?: 0 }
                        if (returnedForProd > 0 && prod.isInventoryItem) {
                            prod.copy(currentStock = prod.currentStock + returnedForProd)
                        } else {
                            prod
                        }
                    }
                }
            }

            // Adjust CashierShift if refunded in Cash
            if (refundMethod == PaymentMethod.CASH && cleanRefund > 0.0) {
                recordCashDrawerMovement(
                    type = CashMovementType.REFUND,
                    amount = cleanRefund,
                    reason = "${if (isReturnItems) "Item Return" else "Order Refund"} ${currentOrder.orderNumber}: $reason",
                    approvedBy = approvedBy
                )
            }

            // Phase 10: Automatically post Refund Reversal Journal Entry
            if (cleanRefund > 0.0) {
                val refundJournal = AccountingEngine.createRefundJournalEntry(
                    order = updatedOrder,
                    refundAmount = cleanRefund,
                    reason = reason
                )
                _journalEntries.value = listOf(refundJournal) + _journalEntries.value
            }

            if (_isOfflineMode.value) {
                enqueueOfflineOperation(
                    idempotencyKey = "refund_${orderId}_${System.currentTimeMillis()}",
                    branchId = updatedOrder.branchId,
                    operationType = if (isReturnItems) "RETURN_ORDER" else "REFUND_ORDER",
                    entityId = orderId,
                    summary = "${updatedOrder.orderNumber} Refunded $${String.format(java.util.Locale.US, "%.2f", cleanRefund)} ($reason)"
                )
            }

            recordAudit(
                action = if (isReturnItems) "ORDER_RETURN" else "ORDER_REFUND",
                entity = "Order",
                id = orderId,
                details = "${updatedOrder.orderNumber}: $${String.format(java.util.Locale.US, "%.2f", cleanRefund)} via ${refundMethod.title}. Reason: $reason"
            )
            return OrderEngine.OrderTransitionResult.Success(updatedOrder)
        }
        return transitionResult
    }

    suspend fun mergeActiveOrders(primaryOrderId: String, secondaryOrderId: String): Boolean {
        if (primaryOrderId == secondaryOrderId) return false
        val primaryEntity = database.orderDao().getOrderById(primaryOrderId) ?: return false
        val secondaryEntity = database.orderDao().getOrderById(secondaryOrderId) ?: return false

        val primaryItems = database.orderDao().getItemsForOrder(primaryOrderId).map { mapOrderItemEntityToDomain(it) }
        val secondaryItems = database.orderDao().getItemsForOrder(secondaryOrderId).map { mapOrderItemEntityToDomain(it) }

        val primaryOrder = mapOrderEntityToDomain(primaryEntity, primaryItems, emptyList())
        val secondaryOrder = mapOrderEntityToDomain(secondaryEntity, secondaryItems, emptyList())

        val mergeResult = OrderEngine.mergeOrders(
            primaryOrder = primaryOrder,
            primaryItems = primaryItems,
            secondaryOrder = secondaryOrder,
            secondaryItems = secondaryItems,
            taxRate = _currentBranch.value.taxRate
        ) ?: return false

        // Persist updated primary order and merged items
        database.orderDao().insertOrder(mapOrderDomainToEntity(mergeResult.mergedOrder))
        database.orderDao().deleteItemsForOrder(primaryOrderId)
        database.orderDao().insertOrderItems(mergeResult.mergedItems.map { mapOrderItemDomainToEntity(it) })

        // Mark secondary order as CANCELLED (merged) and release its table if different
        val cancelledSecondary = secondaryOrder.copy(
            status = OrderStatus.CANCELLED,
            cancelledReason = "Merged into ${primaryOrder.orderNumber}",
            updatedAt = System.currentTimeMillis()
        )
        database.orderDao().insertOrder(mapOrderDomainToEntity(cancelledSecondary))
        if (secondaryOrder.tableId != null && secondaryOrder.tableId != primaryOrder.tableId) {
            database.tableDao().updateTableStatus(secondaryOrder.tableId, TableStatus.AVAILABLE.name, null)
        }

        recordAudit(
            action = "MERGE_ORDERS",
            entity = "Order",
            id = primaryOrderId,
            details = "Merged order ${secondaryOrder.orderNumber} into ${primaryOrder.orderNumber} (New Total: $${mergeResult.mergedOrder.totalAmount})"
        )
        return true
    }

    suspend fun transferOrderTable(orderId: String, targetTableId: String): Boolean {
        val entity = database.orderDao().getOrderById(orderId) ?: return false
        val oldTableId = entity.tableId
        if (oldTableId == targetTableId) return false

        val updatedEntity = entity.copy(
            tableId = targetTableId,
            orderType = OrderType.DINE_IN.name,
            updatedAt = System.currentTimeMillis()
        )
        database.orderDao().insertOrder(updatedEntity)

        if (oldTableId != null) {
            database.tableDao().updateTableStatus(oldTableId, TableStatus.AVAILABLE.name, null)
        }
        database.tableDao().updateTableStatus(targetTableId, TableStatus.OCCUPIED.name, orderId)

        recordAudit(
            action = "TRANSFER_TABLE",
            entity = "Order",
            id = orderId,
            details = "Transferred order ${entity.orderNumber} from table ${oldTableId ?: "None"} to table $targetTableId"
        )
        return true
    }

    suspend fun saveCustomer(customer: Customer) {
        val current = _customers.value.toMutableList()
        val idx = current.indexOfFirst { it.id == customer.id }
        if (idx >= 0) {
            current[idx] = customer
        } else {
            current.add(0, customer)
        }
        _customers.value = current
        recordAudit("CUSTOMER_SAVE", "Customer", customer.id, "Saved CRM customer: ${customer.name} (${customer.phone})")
    }

    // --- Phase 4: Cashier Shift & Cash Drawer Operations ---
    suspend fun openCashierShift(terminalId: String, openingCash: Double): CashierShift {
        val user = _currentUser.value
        val branch = _currentBranch.value
        val newShift = CashierShift(
            id = "shift_${UUID.randomUUID().toString().take(8)}",
            branchId = branch.id,
            terminalId = terminalId,
            cashierId = user.id,
            cashierName = user.fullName,
            status = ShiftStatus.OPEN,
            openedAt = System.currentTimeMillis(),
            openingCash = openingCash.coerceAtLeast(0.0)
        )
        _currentShift.value = newShift
        updateShiftInHistory(newShift)
        database.shiftDao().insertShift(mapShiftDomainToEntity(newShift))
        recordAudit("SHIFT_OPEN", "CashierShift", newShift.id, "Opened shift on $terminalId with opening cash $${newShift.openingCash}")
        return newShift
    }

    suspend fun recordCashDrawerMovement(
        type: CashMovementType,
        amount: Double,
        reason: String,
        approvedBy: String? = null
    ) {
        val shift = _currentShift.value ?: return
        val cleanAmount = amount.coerceAtLeast(0.0)
        val movement = CashDrawerMovement(
            id = "mov_${UUID.randomUUID().toString().take(8)}",
            shiftId = shift.id,
            branchId = shift.branchId,
            type = type,
            amount = cleanAmount,
            reason = reason,
            performedBy = _currentUser.value.fullName,
            approvedBy = approvedBy
        )
        val updatedShift = when (type) {
            CashMovementType.CASH_IN -> shift.copy(
                cashIn = shift.cashIn + cleanAmount,
                movements = listOf(movement) + shift.movements
            )
            CashMovementType.CASH_OUT -> shift.copy(
                cashOut = shift.cashOut + cleanAmount,
                movements = listOf(movement) + shift.movements
            )
            CashMovementType.EXPENSE -> shift.copy(
                expenses = shift.expenses + cleanAmount,
                movements = listOf(movement) + shift.movements
            )
            CashMovementType.REFUND -> shift.copy(
                cashRefunds = shift.cashRefunds + cleanAmount,
                movements = listOf(movement) + shift.movements
            )
        }
        _currentShift.value = updatedShift
        updateShiftInHistory(updatedShift)
        database.shiftDao().insertShift(mapShiftDomainToEntity(updatedShift))
        recordAudit("CASH_MOVEMENT_${type.name}", "CashierShift", shift.id, "${type.titleEn}: $$cleanAmount ($reason)")
    }

    suspend fun closeCashierShift(actualCash: Double, closingNotes: String): CashierShift? {
        val shift = _currentShift.value ?: return null
        val closedShift = shift.copy(
            status = ShiftStatus.CLOSED,
            closedAt = System.currentTimeMillis(),
            actualClosingCash = actualCash.coerceAtLeast(0.0),
            closingNotes = closingNotes
        )
        _currentShift.value = closedShift
        updateShiftInHistory(closedShift)
        database.shiftDao().insertShift(mapShiftDomainToEntity(closedShift))

        if (_isOfflineMode.value) {
            enqueueOfflineOperation(
                idempotencyKey = "shift_close_${closedShift.id}",
                branchId = closedShift.branchId,
                operationType = "SHIFT_CLOSE",
                entityId = closedShift.id,
                summary = "Close Shift ${closedShift.terminalId} (Expected: $${closedShift.expectedCash}, Actual: $$actualCash)"
            )
        }

        recordAudit(
            action = "SHIFT_CLOSE",
            entity = "CashierShift",
            id = closedShift.id,
            details = "Closed shift on ${closedShift.terminalId}. Expected Cash: $${closedShift.expectedCash}, Actual: $$actualCash, Variance: $${closedShift.cashDifference}"
        )
        return closedShift
    }

    private fun updateShiftInHistory(shift: CashierShift) {
        val list = _shiftHistory.value.toMutableList()
        val idx = list.indexOfFirst { it.id == shift.id }
        if (idx >= 0) {
            list[idx] = shift
        } else {
            list.add(0, shift)
        }
        _shiftHistory.value = list
    }

    // --- Phase 4: Offline Sync Queue Foundation ---
    fun setOfflineMode(offline: Boolean) {
        _isOfflineMode.value = offline
        scope.launch {
            recordAudit(
                action = if (offline) "POS_OFFLINE_ENABLED" else "POS_ONLINE_RESTORED",
                entity = "POS",
                id = _currentBranch.value.id,
                details = if (offline) "POS switched to Offline Queue Mode" else "POS network connectivity restored"
            )
        }
    }

    private suspend fun enqueueOfflineOperation(
        idempotencyKey: String,
        branchId: String,
        operationType: String,
        entityId: String,
        summary: String
    ) {
        if (_offlineSyncQueue.value.any { it.idempotencyKey == idempotencyKey }) return
        val event = OfflineSyncEvent(
            id = "sync_${UUID.randomUUID().toString().take(8)}",
            idempotencyKey = idempotencyKey,
            branchId = branchId,
            operationType = operationType,
            entityId = entityId,
            summary = summary,
            status = SyncStatus.QUEUED_OFFLINE
        )
        _offlineSyncQueue.value = listOf(event) + _offlineSyncQueue.value
        database.offlineSyncDao().insertSyncItem(
            OfflineSyncQueueEntity(
                id = event.id,
                idempotencyKey = event.idempotencyKey,
                branchId = event.branchId,
                operationType = event.operationType,
                entityId = event.entityId,
                summary = event.summary,
                status = event.status.name,
                retryCount = event.retryCount,
                createdAt = event.createdAt,
                syncedAt = event.syncedAt
            )
        )
    }

    suspend fun syncOfflineTransactions(): Int {
        val pending = _offlineSyncQueue.value.filter { it.status != SyncStatus.SYNCED }
        if (pending.isEmpty()) return 0
        val now = System.currentTimeMillis()

        // Mark all queued orders in Room as SYNCED
        pending.forEach { ev ->
            val orderEntity = database.orderDao().getOrderById(ev.entityId)
            if (orderEntity != null && orderEntity.syncStatus != SyncStatus.SYNCED.name) {
                database.orderDao().insertOrder(orderEntity.copy(syncStatus = SyncStatus.SYNCED.name, updatedAt = now))
            }
        }

        database.offlineSyncDao().markAllSynced(now)
        _offlineSyncQueue.value = _offlineSyncQueue.value.map {
            it.copy(status = SyncStatus.SYNCED, syncedAt = now)
        }
        _isOfflineMode.value = false

        recordAudit(
            action = "OFFLINE_QUEUE_SYNCED",
            entity = "OfflineQueue",
            id = _currentBranch.value.id,
            details = "Synchronized ${pending.size} queued offline transactions with idempotency verification"
        )
        return pending.size
    }

    suspend fun markOrderPaid(orderId: String) {
        database.orderDao().markOrderPaid(orderId, "PAID")
        recordAudit("PAY_ORDER", "Order", orderId, "Order payment verified via POS")
    }

    suspend fun updateTableStatus(tableId: String, newStatus: TableStatus) {
        val currentEntity = database.tableDao().getTableById(tableId)
        val keepOrderId = if (newStatus in listOf(TableStatus.AVAILABLE, TableStatus.CLEANING, TableStatus.UNAVAILABLE)) {
            null
        } else {
            currentEntity?.currentOrderId
        }
        database.tableDao().updateTableStatus(tableId, newStatus.name, keepOrderId)
        publishRealTimeEvent(
            type = RealTimeEventType.TABLE_CHANGED,
            branchId = currentEntity?.branchId ?: _currentBranch.value.id,
            entityId = tableId,
            referenceCode = currentEntity?.tableNumber ?: tableId,
            titleEn = "Table ${currentEntity?.tableNumber ?: tableId} -> ${newStatus.titleEn}",
            titleAr = "طاولة ${currentEntity?.tableNumber ?: tableId} -> ${newStatus.titleAr}",
            detailsEn = "Section: ${currentEntity?.section ?: "Floor"}",
            detailsAr = "القسم: ${currentEntity?.section ?: "الصالة"}",
            sourceChannel = "TABLES"
        )
        recordAudit("UPDATE_TABLE", "Table", tableId, "Status updated to ${newStatus.name}")
    }

    // --- Phase 5: Smart Table Operations, Waiter Actions, QR Sessions & Reservations ---

    suspend fun saveFloor(floor: RestaurantFloor) {
        val list = _floors.value.toMutableList()
        val idx = list.indexOfFirst { it.id == floor.id }
        if (idx >= 0) list[idx] = floor else list.add(floor)
        _floors.value = list
        recordAudit("FLOOR_SAVE", "RestaurantFloor", floor.id, "Saved floor: ${floor.nameEn}")
    }

    suspend fun saveZone(zone: TableZone) {
        val list = _zones.value.toMutableList()
        val idx = list.indexOfFirst { it.id == zone.id }
        if (idx >= 0) list[idx] = zone else list.add(zone)
        _zones.value = list
        recordAudit("ZONE_SAVE", "TableZone", zone.id, "Saved zone: ${zone.nameEn}")
    }

    suspend fun saveTable(table: RestaurantTable) {
        val token = if (table.qrToken.isBlank() || !table.qrToken.startsWith("QR-SIG-")) {
            TableOperationsEngine.generateSecureQrToken(
                companyId = _currentCompany.value.id,
                branchId = table.branchId,
                tableId = table.id,
                tableNumber = table.tableNumber
            )
        } else {
            table.qrToken
        }
        val enriched = table.copy(qrToken = token)
        database.tableDao().insertTable(mapTableDomainToEntity(enriched))
        recordAudit("TABLE_SAVE", "Table", enriched.id, "Saved table ${enriched.tableNumber} (${enriched.section}, Seats: ${enriched.capacity})")
    }

    suspend fun openTableForGuests(
        tableId: String,
        guestCount: Int,
        waiterId: String?,
        waiterName: String?
    ) {
        val entity = database.tableDao().getTableById(tableId) ?: return
        val domain = mapTableEntityToDomain(entity)
        val updated = domain.copy(
            status = TableStatus.OCCUPIED,
            guestCount = guestCount.coerceAtLeast(1),
            assignedWaiterId = waiterId ?: domain.assignedWaiterId ?: _currentUser.value.id,
            assignedWaiterName = waiterName ?: domain.assignedWaiterName ?: _currentUser.value.fullName
        )
        database.tableDao().insertTable(mapTableDomainToEntity(updated))
        publishRealTimeEvent(
            type = RealTimeEventType.TABLE_CHANGED,
            branchId = domain.branchId,
            entityId = tableId,
            referenceCode = domain.tableNumber,
            titleEn = "Table ${domain.tableNumber} Opened ($guestCount Guests)",
            titleAr = "تم فتح طاولة ${domain.tableNumber} ($guestCount ضيوف)",
            detailsEn = "Server: ${updated.assignedWaiterName ?: "Assigned"}",
            detailsAr = "النادل: ${updated.assignedWaiterName ?: "مخصص"}",
            sourceChannel = "WAITER_APP"
        )
        recordAudit("OPEN_TABLE", "Table", tableId, "Opened table ${domain.tableNumber} for $guestCount guests (Server: ${updated.assignedWaiterName})")
    }

    suspend fun combineTables(primaryTableId: String, secondaryTableIds: List<String>): Boolean {
        val primaryEntity = database.tableDao().getTableById(primaryTableId) ?: return false
        val primaryTable = mapTableEntityToDomain(primaryEntity)
        val secondaryTables = secondaryTableIds.mapNotNull { id ->
            database.tableDao().getTableById(id)?.let { mapTableEntityToDomain(it) }
        }
        val result = TableOperationsEngine.combineTables(primaryTable, secondaryTables) ?: return false

        database.tableDao().insertTable(mapTableDomainToEntity(result.updatedPrimaryTable))
        result.updatedSecondaryTables.forEach { sec ->
            database.tableDao().insertTable(mapTableDomainToEntity(sec))
        }
        publishRealTimeEvent(
            type = RealTimeEventType.TABLE_CHANGED,
            branchId = primaryTable.branchId,
            entityId = primaryTableId,
            referenceCode = primaryTable.tableNumber,
            titleEn = "Combined ${primaryTable.tableNumber} + ${secondaryTables.joinToString { it.tableNumber }}",
            titleAr = "تم دمج الطاولات ${primaryTable.tableNumber} + ${secondaryTables.joinToString { it.tableNumber }}",
            detailsEn = "Combined seating capacity: ${result.totalCombinedSeats} seats",
            detailsAr = "السعة المدمجة: ${result.totalCombinedSeats} مقاعد",
            sourceChannel = "TABLES"
        )
        recordAudit(
            action = "COMBINE_TABLES",
            entity = "Table",
            id = primaryTableId,
            details = "Combined ${primaryTable.tableNumber} with ${secondaryTables.joinToString { it.tableNumber }} (Combined Capacity: ${result.totalCombinedSeats})"
        )
        return true
    }

    suspend fun uncombineTable(tableId: String): Boolean {
        val primaryEntity = database.tableDao().getTableById(tableId) ?: return false
        val primaryTable = mapTableEntityToDomain(primaryEntity)
        val branchTables = database.tableDao().getTablesByBranch(primaryTable.branchId).first().map { mapTableEntityToDomain(it) }
        val updatedTables = TableOperationsEngine.uncombineTables(primaryTable, branchTables)
        database.tableDao().insertTables(updatedTables.map { mapTableDomainToEntity(it) })
        recordAudit("UNCOMBINE_TABLES", "Table", tableId, "Split combined table group for ${primaryTable.tableNumber}")
        return true
    }

    suspend fun moveTableSession(sourceTableId: String, targetTableId: String): Boolean {
        val sourceEntity = database.tableDao().getTableById(sourceTableId) ?: return false
        val targetEntity = database.tableDao().getTableById(targetTableId) ?: return false
        val sourceTable = mapTableEntityToDomain(sourceEntity)
        val targetTable = mapTableEntityToDomain(targetEntity)

        val activeOrder = sourceTable.currentOrderId?.let { ordId ->
            database.orderDao().getOrderById(ordId)?.let { ordEnt ->
                val items = database.orderDao().getItemsForOrder(ordId).map { mapOrderItemEntityToDomain(it) }
                val payments = database.paymentDao().getPaymentsForOrder(ordId).map { mapPaymentEntityToDomain(it) }
                mapOrderEntityToDomain(ordEnt, items, payments)
            }
        }

        val moveResult = TableOperationsEngine.moveTable(sourceTable, targetTable, activeOrder) ?: return false
        database.tableDao().insertTable(mapTableDomainToEntity(moveResult.updatedSourceTable))
        database.tableDao().insertTable(mapTableDomainToEntity(moveResult.updatedTargetTable))
        moveResult.updatedOrder?.let { movedOrder ->
            database.orderDao().insertOrder(mapOrderDomainToEntity(movedOrder))
        }

        // Also update tableNumber on active KDS tickets for this order
        if (activeOrder != null) {
            _kitchenTickets.value = _kitchenTickets.value.map { tkt ->
                if (tkt.orderId == activeOrder.id) {
                    val movedTkt = tkt.copy(tableId = targetTable.id, tableNumber = targetTable.tableNumber)
                    database.kitchenTicketDao().insertTicket(mapKitchenTicketDomainToEntity(movedTkt))
                    movedTkt
                } else tkt
            }
        }

        publishRealTimeEvent(
            type = RealTimeEventType.TABLE_CHANGED,
            branchId = sourceTable.branchId,
            entityId = targetTableId,
            referenceCode = "${sourceTable.tableNumber}->${targetTable.tableNumber}",
            titleEn = "Moved Table ${sourceTable.tableNumber} to ${targetTable.tableNumber}",
            titleAr = "نقل طاولة ${sourceTable.tableNumber} إلى ${targetTable.tableNumber}",
            detailsEn = "Order ${activeOrder?.orderNumber ?: "Session"} transferred",
            detailsAr = "تم نقل الجلسة والطلب ${activeOrder?.orderNumber ?: ""}",
            sourceChannel = "WAITER_APP"
        )

        recordAudit(
            action = "MOVE_TABLE",
            entity = "Table",
            id = sourceTableId,
            details = "Moved table session from ${sourceTable.tableNumber} to ${targetTable.tableNumber}"
        )
        return true
    }

    suspend fun requestTableBill(tableId: String, requestedBy: String): Boolean {
        val entity = database.tableDao().getTableById(tableId) ?: return false
        val table = mapTableEntityToDomain(entity)
        val updated = table.copy(status = TableStatus.WAITING_PAYMENT)
        database.tableDao().insertTable(mapTableDomainToEntity(updated))

        _activeCustomerQrSession.value?.let { sess ->
            if (sess.tableId == tableId) {
                _activeCustomerQrSession.value = sess.copy(billRequested = true)
            }
        }

        publishRealTimeEvent(
            type = RealTimeEventType.TABLE_CHANGED,
            branchId = table.branchId,
            entityId = tableId,
            referenceCode = table.tableNumber,
            titleEn = "Bill Requested • Table ${table.tableNumber}",
            titleAr = "طلب الفاتورة • طاولة ${table.tableNumber}",
            detailsEn = "Requested by $requestedBy (${table.section})",
            detailsAr = "تم الطلب بواسطة $requestedBy (${table.section})",
            sourceChannel = "QR_TABLE"
        )

        addNotification(
            titleEn = "Bill Requested • Table ${table.tableNumber}",
            titleAr = "طلب الفاتورة • طاولة ${table.tableNumber}",
            bodyEn = "Requested by $requestedBy (${table.section})",
            bodyAr = "تم الطلب بواسطة $requestedBy (${table.section})",
            type = NotificationType.ALERT
        )
        recordAudit("REQUEST_BILL", "Table", tableId, "Table ${table.tableNumber} requested bill via $requestedBy")
        return true
    }

    suspend fun regenerateTableQrToken(tableId: String): String? {
        val entity = database.tableDao().getTableById(tableId) ?: return null
        val table = mapTableEntityToDomain(entity)
        val newToken = TableOperationsEngine.generateSecureQrToken(
            companyId = _currentCompany.value.id,
            branchId = table.branchId,
            tableId = table.id,
            tableNumber = table.tableNumber,
            nonce = UUID.randomUUID().toString().take(8)
        )
        val updated = table.copy(qrToken = newToken)
        database.tableDao().insertTable(mapTableDomainToEntity(updated))
        recordAudit("ROTATE_QR_TOKEN", "Table", tableId, "Rotated cryptographic QR token for ${table.tableNumber}")
        return newToken
    }

    suspend fun startCustomerQrSession(
        scannedInput: String,
        guestName: String,
        partySize: Int
    ): TableOperationsEngine.QrValidationResult {
        val branchId = _currentBranch.value.id
        val allBranches = database.branchDao().getAllBranches().first().map { mapBranchEntityToDomain(it) }
            .ifEmpty { listOf(_currentBranch.value) }
        val allTables = database.tableDao().getTablesByBranch(branchId).first().map { mapTableEntityToDomain(it) }

        val validation = TableOperationsEngine.validateQrToken(
            scannedInput = scannedInput,
            company = _currentCompany.value,
            branches = allBranches,
            tables = allTables
        )

        if (validation is TableOperationsEngine.QrValidationResult.Success) {
            val session = TableOperationsEngine.createCustomerSession(
                validatedContext = validation.context,
                guestName = guestName,
                partySize = partySize,
                existingOrderId = validation.context.table.currentOrderId
            )
            _activeCustomerQrSession.value = session
            recordAudit(
                action = "QR_SESSION_START",
                entity = "CustomerTableSession",
                id = session.sessionId,
                details = "QR validated for ${session.tableNumber} (${session.branchName}) - Guest: ${session.guestName}"
            )
        }
        return validation
    }

    fun clearCustomerQrSession() {
        _activeCustomerQrSession.value = null
    }

    suspend fun saveReservation(reservation: Reservation): Boolean {
        val entity = mapReservationDomainToEntity(reservation)
        database.reservationDao().insertReservation(entity)

        val currentList = _reservations.value.toMutableList()
        val idx = currentList.indexOfFirst { it.id == reservation.id }
        if (idx >= 0) currentList[idx] = reservation else currentList.add(0, reservation)
        _reservations.value = currentList

        if (reservation.status == ReservationStatus.CONFIRMED || reservation.status == ReservationStatus.SEATED) {
            val tblEntity = database.tableDao().getTableById(reservation.tableId)
            if (tblEntity != null) {
                val domainTbl = mapTableEntityToDomain(tblEntity)
                val (_, updatedTable) = TableOperationsEngine.transitionReservationStatus(
                    reservation = reservation,
                    targetStatus = reservation.status,
                    table = domainTbl
                )
                updatedTable?.let { database.tableDao().insertTable(mapTableDomainToEntity(it)) }
            }
        }

        recordAudit(
            action = "RESERVATION_SAVE",
            entity = "Reservation",
            id = reservation.id,
            details = "Reservation ${reservation.confirmationCode} for ${reservation.customerName} (${reservation.partySize} pax) at ${reservation.tableNumber} on ${reservation.reservationDate} ${reservation.reservationTime}"
        )
        return true
    }

    suspend fun updateReservationStatus(reservationId: String, targetStatus: ReservationStatus): Boolean {
        val existingEntity = database.reservationDao().getReservationById(reservationId)
        val existingDomain = existingEntity?.let { mapReservationEntityToDomain(it) }
            ?: _reservations.value.find { it.id == reservationId }
            ?: return false

        val tableEntity = database.tableDao().getTableById(existingDomain.tableId)
        val tableDomain = tableEntity?.let { mapTableEntityToDomain(it) }

        val (updatedRes, updatedTable) = TableOperationsEngine.transitionReservationStatus(
            reservation = existingDomain,
            targetStatus = targetStatus,
            table = tableDomain
        )

        database.reservationDao().insertReservation(mapReservationDomainToEntity(updatedRes))
        _reservations.value = _reservations.value.map { if (it.id == reservationId) updatedRes else it }

        if (updatedTable != null) {
            database.tableDao().insertTable(mapTableDomainToEntity(updatedTable))
        }

        recordAudit(
            action = "RESERVATION_STATUS_${targetStatus.name}",
            entity = "Reservation",
            id = reservationId,
            details = "Reservation ${updatedRes.confirmationCode} (${updatedRes.customerName}) status changed to ${targetStatus.name}"
        )
        return true
    }

    fun updateKitchenTicketStatus(ticketId: String, newStatus: String) {
        scope.launch {
            transitionKitchenTicketStatus(ticketId, KdsTicketStatus.fromRawStatus(newStatus))
        }
    }

    suspend fun transitionKitchenTicketStatus(
        ticketId: String,
        targetStatus: KdsTicketStatus
    ): KitchenEngine.TicketTransitionOutcome? {
        val currentTicket = _kitchenTickets.value.find { it.id == ticketId }
            ?: database.kitchenTicketDao().getTicketById(ticketId)?.let { mapKitchenTicketEntityToDomain(it) }
            ?: return null

        val allOrderTickets = _kitchenTickets.value.filter { it.orderId == currentTicket.orderId }
            .ifEmpty {
                database.kitchenTicketDao().getTicketsForOrder(currentTicket.orderId).map { mapKitchenTicketEntityToDomain(it) }
            }

        val outcome = KitchenEngine.transitionTicketStatus(
            ticket = currentTicket,
            targetStatus = targetStatus,
            allOrderTickets = allOrderTickets,
            stations = _kitchenStations.value,
            kitchenEmployees = _kitchenEmployees.value
        ) ?: return null

        // 1. Persist updated station ticket
        database.kitchenTicketDao().insertTicket(mapKitchenTicketDomainToEntity(outcome.updatedTicket))
        val updatedList = _kitchenTickets.value.map {
            if (it.id == ticketId) outcome.updatedTicket else it
        }.toMutableList()
        if (updatedList.none { it.id == ticketId }) {
            updatedList.add(0, outcome.updatedTicket)
        }

        // 2. If all prep stations finished, persist spawned Packing/Expediter consolidation ticket
        outcome.spawnedConsolidationTicket?.let { consTicket ->
            database.kitchenTicketDao().insertTicket(mapKitchenTicketDomainToEntity(consTicket))
            if (updatedList.none { it.id == consTicket.id }) {
                updatedList.add(0, consTicket)
            }
            publishRealTimeEvent(
                type = RealTimeEventType.KITCHEN_TICKET_UPDATED,
                branchId = consTicket.branchId,
                entityId = consTicket.id,
                referenceCode = consTicket.orderNumber,
                titleEn = "Dispatched ${consTicket.orderNumber} to ${consTicket.stationName}",
                titleAr = "تم توجيه الطلب ${consTicket.orderNumber} إلى ${consTicket.stationType.titleAr}",
                detailsEn = "All preparation stations ready — consolidated at ${consTicket.stationName}",
                detailsAr = "جميع محطات التحضير جاهزة — تم التجميع في ${consTicket.stationType.titleAr}",
                sourceChannel = "KDS_${currentTicket.stationType.code}"
            )
        }
        _kitchenTickets.value = updatedList

        // 3. Increment completed ticket count for assigned kitchen chef when ticket is READY or COMPLETED
        if (targetStatus == KdsTicketStatus.COMPLETED) {
            _kitchenEmployees.value = _kitchenEmployees.value.map { emp ->
                if (emp.stationId == currentTicket.stationId && emp.isOnDuty) {
                    emp.copy(ticketsCompletedCount = emp.ticketsCompletedCount + 1)
                } else emp
            }
        }

        // 4. Synchronize OrderItem kitchenStatus in Room DB
        outcome.updatedOrderItemStatuses.forEach { (orderItemId, kStatus) ->
            database.orderDao().updateOrderItemKitchenStatus(orderItemId, kStatus)
        }

        // 5. Synchronize central Order status when applicable (PREPARING, READY, SERVED, CANCELLED)
        outcome.updatedOrderStatus?.let { newOrderStatus ->
            val orderEntity = database.orderDao().getOrderById(currentTicket.orderId)
            if (orderEntity != null && orderEntity.status != OrderStatus.COMPLETED.name) {
                database.orderDao().updateOrderStatus(
                    orderId = currentTicket.orderId,
                    status = newOrderStatus.name,
                    updatedAt = System.currentTimeMillis()
                )
                publishRealTimeEvent(
                    type = RealTimeEventType.ORDER_STATUS_CHANGED,
                    branchId = currentTicket.branchId,
                    entityId = currentTicket.orderId,
                    referenceCode = currentTicket.orderNumber,
                    titleEn = "Order ${currentTicket.orderNumber} is ${newOrderStatus.titleEn}",
                    titleAr = "الطلب ${currentTicket.orderNumber} أصبح ${newOrderStatus.titleAr}",
                    detailsEn = outcome.workflowMessageEn,
                    detailsAr = outcome.workflowMessageAr,
                    sourceChannel = "KDS_${currentTicket.stationType.code}"
                )
                if (newOrderStatus == OrderStatus.READY) {
                    addNotification(
                        titleEn = "Order Ready • ${currentTicket.orderNumber}",
                        titleAr = "الطلب جاهز للتسليم • ${currentTicket.orderNumber}",
                        bodyEn = "${currentTicket.stationName} marked ${currentTicket.orderNumber} Ready (${currentTicket.tableNumber ?: currentTicket.orderType.titleEn})",
                        bodyAr = "محطة ${currentTicket.stationType.titleAr} أتمت تجهيز الطلب ${currentTicket.orderNumber}",
                        type = NotificationType.ORDER
                    )
                }
            }
        }

        // 6. Publish real-time KDS ticket event
        publishRealTimeEvent(
            type = RealTimeEventType.KITCHEN_TICKET_UPDATED,
            branchId = currentTicket.branchId,
            entityId = outcome.updatedTicket.id,
            referenceCode = currentTicket.orderNumber,
            titleEn = outcome.workflowMessageEn,
            titleAr = outcome.workflowMessageAr,
            detailsEn = currentTicket.itemsSummary.replace("\n", " • "),
            detailsAr = currentTicket.itemsSummary.replace("\n", " • "),
            sourceChannel = "KDS_${currentTicket.stationType.code}"
        )

        recordAudit(
            action = "KDS_TICKET_${targetStatus.code}",
            entity = "KitchenTicket",
            id = ticketId,
            details = outcome.workflowMessageEn
        )
        return outcome
    }

    // --- Phase 6: Kitchen, Station, Routing & Staff Management ---
    suspend fun saveKitchen(kitchen: Kitchen) {
        val list = _kitchens.value.toMutableList()
        val idx = list.indexOfFirst { it.id == kitchen.id }
        if (idx >= 0) list[idx] = kitchen else list.add(kitchen)
        _kitchens.value = list
        recordAudit("KITCHEN_SAVE", "Kitchen", kitchen.id, "Saved kitchen: ${kitchen.nameEn} (${kitchen.code})")
    }

    suspend fun savePreparationArea(area: PreparationArea) {
        val list = _preparationAreas.value.toMutableList()
        val idx = list.indexOfFirst { it.id == area.id }
        if (idx >= 0) list[idx] = area else list.add(area)
        _preparationAreas.value = list
        recordAudit("PREP_AREA_SAVE", "PreparationArea", area.id, "Saved prep area: ${area.nameEn}")
    }

    suspend fun saveKitchenStation(station: KitchenStation) {
        val list = _kitchenStations.value.toMutableList()
        val idx = list.indexOfFirst { it.id == station.id }
        if (idx >= 0) list[idx] = station else list.add(station)
        _kitchenStations.value = list

        // Also update thresholds on active tickets for this station
        _kitchenTickets.value = _kitchenTickets.value.map { tkt ->
            if (tkt.stationId == station.id) {
                val updated = tkt.copy(
                    stationName = station.nameEn,
                    warningThresholdMinutes = station.warningThresholdMinutes,
                    overdueThresholdMinutes = station.overdueThresholdMinutes
                )
                database.kitchenTicketDao().insertTicket(mapKitchenTicketDomainToEntity(updated))
                updated
            } else tkt
        }
        recordAudit(
            action = "STATION_SAVE",
            entity = "KitchenStation",
            id = station.id,
            details = "Saved station ${station.nameEn} (Warn: ${station.warningThresholdMinutes}m, Overdue: ${station.overdueThresholdMinutes}m)"
        )
    }

    suspend fun saveStationRoutingRule(rule: StationRoutingRule) {
        val list = _stationRoutingRules.value.toMutableList()
        val idx = list.indexOfFirst { it.id == rule.id }
        if (idx >= 0) list[idx] = rule else list.add(0, rule)
        _stationRoutingRules.value = list
        recordAudit("ROUTING_RULE_SAVE", "StationRoutingRule", rule.id, "Saved routing rule: ${rule.nameEn} -> ${rule.targetStationName}")
    }

    suspend fun deleteStationRoutingRule(ruleId: String) {
        _stationRoutingRules.value = _stationRoutingRules.value.filter { it.id != ruleId }
        recordAudit("ROUTING_RULE_DELETE", "StationRoutingRule", ruleId, "Removed station routing rule $ruleId")
    }

    suspend fun saveKitchenEmployeeAssignment(assignment: KitchenEmployeeAssignment) {
        val list = _kitchenEmployees.value.toMutableList()
        val idx = list.indexOfFirst { it.id == assignment.id }
        if (idx >= 0) list[idx] = assignment else list.add(assignment)
        _kitchenEmployees.value = list
        recordAudit("KITCHEN_STAFF_SAVE", "KitchenEmployeeAssignment", assignment.id, "Assigned ${assignment.employeeName} to ${assignment.stationName}")
    }

    suspend fun toggleKitchenEmployeeDuty(assignmentId: String, isOnDuty: Boolean) {
        _kitchenEmployees.value = _kitchenEmployees.value.map {
            if (it.id == assignmentId) it.copy(isOnDuty = isOnDuty) else it
        }
        recordAudit("KITCHEN_STAFF_DUTY", "KitchenEmployeeAssignment", assignmentId, "Duty status toggled to $isOnDuty")
    }

    // --- Phase 7: Customer Portal, OTP Auth, Addresses, Loyalty, Reviews & Complaints ---
    suspend fun loginOrRegisterCustomerWithOtp(
        name: String,
        phone: String,
        email: String,
        otpCode: String
    ): Customer? {
        if (otpCode.length < 4) return null
        val existing = _customers.value.find { it.phone == phone }
        val resolved = if (existing != null) {
            existing.copy(name = name.ifBlank { existing.name }, otpVerified = true)
        } else {
            Customer(
                id = "cust_${UUID.randomUUID().toString().take(6)}",
                companyId = _currentCompany.value.id,
                name = name.ifBlank { "Valued Guest" },
                phone = phone,
                email = email.ifBlank { "guest@restoops.com" },
                loyaltyPoints = 100, // Welcome bonus points
                tier = "BRONZE",
                preferredBranchId = _currentBranch.value.id,
                otpVerified = true
            )
        }
        saveCustomer(resolved)
        _activePortalCustomer.value = resolved
        recordAudit("CUSTOMER_OTP_AUTH", "Customer", resolved.id, "Customer ${resolved.name} authenticated via OTP verification")
        return resolved
    }

    fun selectActivePortalCustomer(customer: Customer) {
        _activePortalCustomer.value = customer
    }

    suspend fun toggleCustomerFavoriteProduct(customerId: String, productId: String) {
        _customers.value = _customers.value.map { cust ->
            if (cust.id == customerId) {
                val updatedFavs = if (productId in cust.favoriteProductIds) {
                    cust.favoriteProductIds - productId
                } else {
                    cust.favoriteProductIds + productId
                }
                val updated = cust.copy(favoriteProductIds = updatedFavs)
                if (_activePortalCustomer.value?.id == customerId) {
                    _activePortalCustomer.value = updated
                }
                updated
            } else cust
        }
    }

    suspend fun saveCustomerAddress(address: CustomerAddress) {
        val current = _customerAddresses.value.toMutableList()
        if (address.isDefault) {
            for (i in current.indices) {
                if (current[i].customerId == address.customerId) {
                    current[i] = current[i].copy(isDefault = false)
                }
            }
        }
        val idx = current.indexOfFirst { it.id == address.id }
        if (idx >= 0) current[idx] = address else current.add(0, address)
        _customerAddresses.value = current
        recordAudit("CUSTOMER_ADDRESS_SAVE", "CustomerAddress", address.id, "Saved address '${address.label}' for customer ${address.customerId}")
    }

    suspend fun redeemLoyaltyReward(customerId: String, rewardId: String): Double? {
        val customer = _customers.value.find { it.id == customerId } ?: return null
        val reward = _loyaltyRewards.value.find { it.id == rewardId && it.isActive } ?: return null
        if (customer.loyaltyPoints < reward.pointsCost) return null

        val remainingPts = customer.loyaltyPoints - reward.pointsCost
        val updatedCustomer = customer.copy(
            loyaltyPoints = remainingPts,
            tier = CustomerCrmEngine.resolveLoyaltyTier(remainingPts).code
        )
        saveCustomer(updatedCustomer)
        if (_activePortalCustomer.value?.id == customerId) {
            _activePortalCustomer.value = updatedCustomer
        }
        recordAudit(
            action = "LOYALTY_REDEEM",
            entity = "Customer",
            id = customerId,
            details = "Redeemed '${reward.titleEn}' (${reward.pointsCost} pts) for $${reward.discountValue} reward credit"
        )
        return reward.discountValue
    }

    suspend fun submitCustomerReview(review: CustomerReview) {
        _customerReviews.value = listOf(review) + _customerReviews.value
        recordAudit("REVIEW_SUBMIT", "CustomerReview", review.id, "Customer ${review.customerName} rated ${review.rating}/5 on ${review.orderNumber}")
    }

    suspend fun moderateCustomerReview(reviewId: String, status: ReviewModerationStatus, managerReply: String?) {
        _customerReviews.value = _customerReviews.value.map {
            if (it.id == reviewId) it.copy(moderationStatus = status, managerReply = managerReply ?: it.managerReply) else it
        }
        recordAudit("REVIEW_MODERATE", "CustomerReview", reviewId, "Moderation status updated to ${status.name}")
    }

    suspend fun submitCustomerComplaint(complaint: CustomerComplaint) {
        _customerComplaints.value = listOf(complaint) + _customerComplaints.value
        dispatchCentralNotification(
            eventTrigger = "APPROVAL_REQUEST",
            titleEn = "New Customer Support Ticket ${complaint.ticketNumber}",
            titleAr = "شكوى عميل جديدة ${complaint.ticketNumber}",
            messageEn = "${complaint.customerName}: ${complaint.subject} [${complaint.priority.titleEn}]",
            messageAr = "${complaint.customerName}: ${complaint.subject}",
            channels = listOf(NotificationChannelType.IN_APP, NotificationChannelType.EMAIL_READY),
            referenceId = complaint.ticketNumber
        )
        recordAudit("COMPLAINT_CREATE", "CustomerComplaint", complaint.id, "Ticket ${complaint.ticketNumber} (${complaint.category} / ${complaint.priority.name})")
    }

    suspend fun resolveCustomerComplaint(complaintId: String, status: ComplaintStatus, response: String, compensation: String) {
        _customerComplaints.value = _customerComplaints.value.map {
            if (it.id == complaintId) {
                it.copy(
                    status = status,
                    managementResponse = response,
                    compensationOffered = compensation,
                    resolvedAt = if (status in setOf(ComplaintStatus.RESOLVED, ComplaintStatus.CLOSED)) System.currentTimeMillis() else null
                )
            } else it
        }
        recordAudit("COMPLAINT_RESOLVE", "CustomerComplaint", complaintId, "Updated complaint $complaintId to ${status.name}: $response")
    }

    // --- Phase 8: Delivery Zones, Driver Fleet, Dispatch & Cash Collection ---
    suspend fun saveDeliveryZone(zone: DeliveryZone) {
        val list = _deliveryZones.value.toMutableList()
        val idx = list.indexOfFirst { it.id == zone.id }
        if (idx >= 0) list[idx] = zone else list.add(zone)
        _deliveryZones.value = list
        recordAudit("DELIVERY_ZONE_SAVE", "DeliveryZone", zone.id, "Saved delivery zone ${zone.nameEn} (Fee: $${zone.deliveryFee}, Min: $${zone.minimumOrderAmount})")
    }

    suspend fun toggleDriverShiftAndConsent(driverId: String, shiftStatus: String, locationConsent: Boolean) {
        _drivers.value = _drivers.value.map { drv ->
            if (drv.id == driverId) {
                val isAvail = shiftStatus == "ONLINE" && drv.activeDeliveries == 0
                drv.copy(
                    shiftStatus = shiftStatus,
                    isAvailable = isAvail,
                    driverStatus = when {
                        shiftStatus == "OFFLINE" -> "OFFLINE"
                        shiftStatus == "ON_BREAK" -> "BREAK"
                        drv.activeDeliveries > 0 -> "ON_DELIVERY"
                        else -> "AVAILABLE"
                    },
                    locationSharingConsented = if (shiftStatus == "ONLINE") locationConsent else false
                )
            } else drv
        }
        recordAudit("DRIVER_SHIFT_UPDATE", "DeliveryDriver", driverId, "Driver shift set to $shiftStatus (Consented GPS: $locationConsent)")
    }

    suspend fun assignDriverToDelivery(assignmentId: String, driverId: String): Boolean {
        val driver = _drivers.value.find { it.id == driverId } ?: return false
        val assignment = _deliveryAssignments.value.find { it.id == assignmentId } ?: return false

        val updatedAssignment = assignment.copy(
            driverId = driver.id,
            driverName = driver.fullName,
            driverPhone = driver.phone,
            vehicleInfo = "${driver.vehicleType} • ${driver.vehiclePlate}",
            status = DeliveryWorkflowStatus.ASSIGNED
        )
        _deliveryAssignments.value = _deliveryAssignments.value.map {
            if (it.id == assignmentId) updatedAssignment else it
        }
        _drivers.value = _drivers.value.map {
            if (it.id == driverId) {
                it.copy(
                    isAvailable = false,
                    activeDeliveries = it.activeDeliveries + 1,
                    driverStatus = "ON_DELIVERY"
                )
            } else it
        }

        dispatchCentralNotification(
            eventTrigger = "DRIVER_ASSIGNED",
            titleEn = "Driver ${driver.fullName} Assigned to ${assignment.orderNumber}",
            titleAr = "تم تعيين السائق ${driver.fullName} للطلب ${assignment.orderNumber}",
            messageEn = "Vehicle: ${driver.vehiclePlate} • Est. ${assignment.estimatedDeliveryMinutes} mins",
            messageAr = "المركبة: ${driver.vehiclePlate} • الوقت المقدر ${assignment.estimatedDeliveryMinutes} دقيقة",
            channels = listOf(NotificationChannelType.IN_APP, NotificationChannelType.PUSH_READY, NotificationChannelType.SMS_READY),
            referenceId = assignment.orderNumber
        )
        recordAudit("DELIVERY_ASSIGN_DRIVER", "DeliveryAssignment", assignmentId, "Assigned driver ${driver.fullName} to ${assignment.orderNumber}")
        return true
    }

    suspend fun transitionDeliveryAssignment(
        assignmentId: String,
        targetStatus: DeliveryWorkflowStatus,
        otpInput: String? = null,
        failureReason: String? = null,
        proofNote: String = ""
    ): Boolean {
        val current = _deliveryAssignments.value.find { it.id == assignmentId } ?: return false
        if (!DeliveryDispatchEngine.canTransitionDelivery(current.status, targetStatus)) return false

        if (targetStatus == DeliveryWorkflowStatus.DELIVERED && !otpInput.isNullOrBlank() && otpInput != current.customerOtpCode) {
            return false
        }

        val now = System.currentTimeMillis()
        val cashCollected = if (targetStatus == DeliveryWorkflowStatus.DELIVERED && current.isCashOnDelivery) {
            current.orderTotalAmount
        } else current.cashCollectedAmount

        val updated = current.copy(
            status = targetStatus,
            acceptedAt = if (targetStatus == DeliveryWorkflowStatus.ACCEPTED) now else current.acceptedAt,
            pickedUpAt = if (targetStatus == DeliveryWorkflowStatus.PICKED_UP) now else current.pickedUpAt,
            deliveredAt = if (targetStatus == DeliveryWorkflowStatus.DELIVERED) now else current.deliveredAt,
            otpVerified = if (targetStatus == DeliveryWorkflowStatus.DELIVERED) true else current.otpVerified,
            cashCollectedAmount = cashCollected,
            failureReason = failureReason ?: current.failureReason,
            retryCount = if (targetStatus == DeliveryWorkflowStatus.ON_ROUTE && current.status == DeliveryWorkflowStatus.FAILED_DELIVERY) current.retryCount + 1 else current.retryCount,
            proofOfDeliveryNote = proofNote.ifBlank { current.proofOfDeliveryNote }
        )

        _deliveryAssignments.value = _deliveryAssignments.value.map {
            if (it.id == assignmentId) updated else it
        }

        // Synchronize with central OrderEngine!
        when (targetStatus) {
            DeliveryWorkflowStatus.PICKED_UP, DeliveryWorkflowStatus.ON_ROUTE -> {
                database.orderDao().updateOrderStatus(current.orderId, OrderStatus.OUT_FOR_DELIVERY.name, now)
                publishRealTimeEvent(
                    type = RealTimeEventType.ORDER_STATUS_CHANGED,
                    branchId = current.branchId,
                    entityId = current.orderId,
                    referenceCode = current.orderNumber,
                    titleEn = "Delivery ${current.orderNumber} -> ${targetStatus.titleEn}",
                    titleAr = "توصيل ${current.orderNumber} -> ${targetStatus.titleAr}",
                    detailsEn = "Driver: ${current.driverName ?: "Courier"}",
                    detailsAr = "السائق: ${current.driverName ?: "مندوب التوصيل"}",
                    sourceChannel = "DRIVER_APP"
                )
            }
            DeliveryWorkflowStatus.DELIVERED -> {
                database.orderDao().updateOrderStatus(current.orderId, OrderStatus.COMPLETED.name, now)
                database.orderDao().markOrderPaid(current.orderId, "PAID")
                current.driverId?.let { dId ->
                    _drivers.value = _drivers.value.map { drv ->
                        if (drv.id == dId) {
                            val remActive = (drv.activeDeliveries - 1).coerceAtLeast(0)
                            drv.copy(
                                activeDeliveries = remActive,
                                isAvailable = remActive == 0 && drv.shiftStatus == "ONLINE",
                                driverStatus = if (remActive == 0) "AVAILABLE" else "ON_DELIVERY",
                                cashCollectedUnsettled = drv.cashCollectedUnsettled + (if (current.isCashOnDelivery) current.orderTotalAmount else 0.0),
                                completedDeliveriesToday = drv.completedDeliveriesToday + 1
                            )
                        } else drv
                    }
                }
                dispatchCentralNotification(
                    eventTrigger = "DELIVERY_COMPLETED",
                    titleEn = "Order ${current.orderNumber} Delivered",
                    titleAr = "تم توصيل الطلب ${current.orderNumber} بنجاح",
                    messageEn = "Delivered to ${current.customerName} by ${current.driverName ?: "Driver"}",
                    messageAr = "تم التسليم للعميل ${current.customerName}",
                    channels = listOf(NotificationChannelType.IN_APP, NotificationChannelType.WHATSAPP_READY, NotificationChannelType.SMS_READY),
                    referenceId = current.orderNumber
                )
            }
            DeliveryWorkflowStatus.RETURNED_TO_BRANCH -> {
                database.orderDao().updateOrderStatus(current.orderId, OrderStatus.CANCELLED.name, now)
                current.driverId?.let { dId ->
                    _drivers.value = _drivers.value.map { drv ->
                        if (drv.id == dId) {
                            val remActive = (drv.activeDeliveries - 1).coerceAtLeast(0)
                            drv.copy(
                                activeDeliveries = remActive,
                                isAvailable = remActive == 0 && drv.shiftStatus == "ONLINE",
                                driverStatus = if (remActive == 0) "AVAILABLE" else "ON_DELIVERY"
                            )
                        } else drv
                    }
                }
            }
            else -> {}
        }

        recordAudit(
            action = "DELIVERY_STATUS_${targetStatus.code}",
            entity = "DeliveryAssignment",
            id = assignmentId,
            details = "Order ${current.orderNumber} delivery status -> ${targetStatus.titleEn} (Driver: ${current.driverName ?: "None"})"
        )
        return true
    }

    suspend fun settleDriverCash(driverId: String, handedOverAmount: Double, notes: String): DriverCashSettlement? {
        val driver = _drivers.value.find { it.id == driverId } ?: return null
        val unsettledIds = _deliveryAssignments.value
            .filter { it.driverId == driverId && it.isCashOnDelivery && !it.cashSettledWithBranch && it.status == DeliveryWorkflowStatus.DELIVERED }
            .map { it.id }

        val settlement = DriverCashSettlement(
            id = "dset_${UUID.randomUUID().toString().take(8)}",
            branchId = driver.branchId,
            driverId = driver.id,
            driverName = driver.fullName,
            assignmentIds = unsettledIds,
            expectedCashAmount = driver.cashCollectedUnsettled,
            handedOverCashAmount = handedOverAmount,
            receivedByCashierName = _currentUser.value.fullName,
            notes = notes
        )
        _driverCashSettlements.value = listOf(settlement) + _driverCashSettlements.value
        _deliveryAssignments.value = _deliveryAssignments.value.map {
            if (it.id in unsettledIds) it.copy(cashSettledWithBranch = true) else it
        }
        _drivers.value = _drivers.value.map {
            if (it.id == driverId) it.copy(cashCollectedUnsettled = 0.0) else it
        }
        // Record cash-in to active cashier drawer
        if (handedOverAmount > 0.0) {
            recordCashDrawerMovement(
                type = CashMovementType.CASH_IN,
                amount = handedOverAmount,
                reason = "Driver COD Settlement (${driver.fullName})"
            )
        }
        recordAudit("DRIVER_CASH_SETTLEMENT", "DriverCashSettlement", settlement.id, "Settled $${handedOverAmount} COD from ${driver.fullName} (Expected: $${settlement.expectedCashAmount})")
        return settlement
    }

    // --- Phase 9: Warehouses, Traceable Inventory Movements, Transfers, Waste, Stock Count & Procurement ---
    suspend fun saveWarehouse(warehouse: Warehouse) {
        val list = _warehouses.value.toMutableList()
        val idx = list.indexOfFirst { it.id == warehouse.id }
        if (idx >= 0) list[idx] = warehouse else list.add(warehouse)
        _warehouses.value = list
        recordAudit("WAREHOUSE_SAVE", "Warehouse", warehouse.id, "Saved warehouse ${warehouse.nameEn} (${warehouse.code})")
    }

    suspend fun recordWasteOrDamage(
        stockItemId: String,
        quantity: Double,
        reason: String,
        isDamage: Boolean
    ): Boolean {
        val stockList = _warehouseStockItems.value.toMutableList()
        val idx = stockList.indexOfFirst { it.id == stockItemId }
        if (idx < 0 || quantity <= 0.0) return false

        val item = stockList[idx]
        val before = item.currentQuantity
        val after = (before - quantity).coerceAtLeast(0.0)
        stockList[idx] = item.copy(currentQuantity = after, lastUpdatedAt = System.currentTimeMillis())
        _warehouseStockItems.value = stockList

        val wasteRecord = WasteRecord(
            id = "wst_${UUID.randomUUID().toString().take(6)}",
            wasteNumber = "WST-${(100..999).random()}",
            branchId = item.branchId,
            warehouseId = item.warehouseId,
            itemId = item.itemId,
            itemNameEn = item.itemNameEn,
            itemNameAr = item.itemNameAr,
            quantity = quantity,
            unit = item.stockUnit,
            unitCost = item.unitCost,
            reason = reason,
            reportedByEmployee = _currentUser.value.fullName,
            approvedBy = "Karim Mansour"
        )
        _wasteRecords.value = listOf(wasteRecord) + _wasteRecords.value

        val tx = InventoryTransaction(
            id = "itx_wst_${UUID.randomUUID().toString().take(8)}",
            referenceNumber = wasteRecord.wasteNumber,
            branchId = item.branchId,
            warehouseId = item.warehouseId,
            warehouseName = item.warehouseName,
            itemId = item.itemId,
            itemNameEn = item.itemNameEn,
            itemNameAr = item.itemNameAr,
            transactionType = if (isDamage) InventoryTransactionType.DAMAGE else InventoryTransactionType.WASTE,
            quantityDelta = -quantity,
            unit = item.stockUnit,
            unitCost = item.unitCost,
            balanceBefore = before,
            balanceAfter = after,
            sourceDocumentType = "WASTE_LOG",
            sourceDocumentId = wasteRecord.id,
            performedBy = _currentUser.value.fullName,
            approvedBy = "Karim Mansour",
            notes = reason
        )
        _inventoryTransactions.value = listOf(tx) + _inventoryTransactions.value
        recordAudit("INVENTORY_WASTE", "WasteRecord", wasteRecord.id, "${wasteRecord.wasteNumber}: -$quantity ${item.stockUnit} ${item.itemNameEn} ($reason)")
        return true
    }

    suspend fun createOrAdvanceWarehouseTransfer(
        transferId: String?,
        sourceWarehouseId: String = "wh_central",
        destinationWarehouseId: String = "wh_branch_01",
        itemId: String = "ing_angus_patty",
        quantity: Double = 50.0,
        targetStatus: WarehouseTransferStatus = WarehouseTransferStatus.REQUESTED
    ): Boolean {
        if (transferId == null) {
            val srcWh = _warehouses.value.find { it.id == sourceWarehouseId } ?: return false
            val dstWh = _warehouses.value.find { it.id == destinationWarehouseId } ?: return false
            val stockSample = _warehouseStockItems.value.find { it.itemId == itemId } ?: return false
            val newTrf = WarehouseTransfer(
                id = "trf_${UUID.randomUUID().toString().take(6)}",
                transferNumber = "TRF-2026-${(100..999).random()}",
                sourceWarehouseId = srcWh.id,
                sourceWarehouseName = srcWh.nameEn,
                destinationWarehouseId = dstWh.id,
                destinationWarehouseName = dstWh.nameEn,
                itemId = stockSample.itemId,
                itemNameEn = stockSample.itemNameEn,
                itemNameAr = stockSample.itemNameAr,
                quantity = quantity,
                unit = stockSample.stockUnit,
                unitCost = stockSample.unitCost,
                status = WarehouseTransferStatus.REQUESTED,
                requestedBy = _currentUser.value.fullName
            )
            _warehouseTransfers.value = listOf(newTrf) + _warehouseTransfers.value
            recordAudit("TRANSFER_REQUEST", "WarehouseTransfer", newTrf.id, "Requested transfer ${newTrf.transferNumber} (${quantity} ${stockSample.stockUnit} ${stockSample.itemNameEn})")
            return true
        }

        val existing = _warehouseTransfers.value.find { it.id == transferId } ?: return false
        val updated = existing.copy(
            status = targetStatus,
            approvedBy = if (targetStatus == WarehouseTransferStatus.APPROVED) _currentUser.value.fullName else existing.approvedBy,
            receivedBy = if (targetStatus in setOf(WarehouseTransferStatus.RECEIVED, WarehouseTransferStatus.COMPLETED)) _currentUser.value.fullName else existing.receivedBy,
            updatedAt = System.currentTimeMillis()
        )
        _warehouseTransfers.value = _warehouseTransfers.value.map { if (it.id == transferId) updated else it }

        if (targetStatus == WarehouseTransferStatus.COMPLETED && existing.status != WarehouseTransferStatus.COMPLETED) {
            val stockList = _warehouseStockItems.value.toMutableList()
            val newTxs = mutableListOf<InventoryTransaction>()

            val srcIdx = stockList.indexOfFirst { it.warehouseId == existing.sourceWarehouseId && it.itemId == existing.itemId }
            if (srcIdx >= 0) {
                val srcItem = stockList[srcIdx]
                val before = srcItem.currentQuantity
                val after = (before - existing.quantity).coerceAtLeast(0.0)
                stockList[srcIdx] = srcItem.copy(currentQuantity = after)
                newTxs.add(
                    InventoryTransaction(
                        id = "itx_trout_${UUID.randomUUID().toString().take(6)}",
                        referenceNumber = "${existing.transferNumber}-OUT",
                        branchId = srcItem.branchId,
                        warehouseId = srcItem.warehouseId,
                        warehouseName = srcItem.warehouseName,
                        itemId = srcItem.itemId,
                        itemNameEn = srcItem.itemNameEn,
                        itemNameAr = srcItem.itemNameAr,
                        transactionType = InventoryTransactionType.TRANSFER_OUT,
                        quantityDelta = -existing.quantity,
                        unit = srcItem.stockUnit,
                        unitCost = srcItem.unitCost,
                        balanceBefore = before,
                        balanceAfter = after,
                        sourceDocumentType = "TRANSFER",
                        sourceDocumentId = existing.id,
                        performedBy = _currentUser.value.fullName,
                        notes = "Transfer out to ${existing.destinationWarehouseName}"
                    )
                )
            }

            val dstIdx = stockList.indexOfFirst { it.warehouseId == existing.destinationWarehouseId && it.itemId == existing.itemId }
            if (dstIdx >= 0) {
                val dstItem = stockList[dstIdx]
                val before = dstItem.currentQuantity
                val after = before + existing.quantity
                stockList[dstIdx] = dstItem.copy(currentQuantity = after)
                newTxs.add(
                    InventoryTransaction(
                        id = "itx_trin_${UUID.randomUUID().toString().take(6)}",
                        referenceNumber = "${existing.transferNumber}-IN",
                        branchId = dstItem.branchId,
                        warehouseId = dstItem.warehouseId,
                        warehouseName = dstItem.warehouseName,
                        itemId = dstItem.itemId,
                        itemNameEn = dstItem.itemNameEn,
                        itemNameAr = dstItem.itemNameAr,
                        transactionType = InventoryTransactionType.TRANSFER_IN,
                        quantityDelta = +existing.quantity,
                        unit = dstItem.stockUnit,
                        unitCost = dstItem.unitCost,
                        balanceBefore = before,
                        balanceAfter = after,
                        sourceDocumentType = "TRANSFER",
                        sourceDocumentId = existing.id,
                        performedBy = _currentUser.value.fullName,
                        notes = "Transfer received from ${existing.sourceWarehouseName}"
                    )
                )
            }
            _warehouseStockItems.value = stockList
            _inventoryTransactions.value = newTxs + _inventoryTransactions.value
        }

        recordAudit("TRANSFER_STATUS_${targetStatus.name}", "WarehouseTransfer", existing.id, "${existing.transferNumber} -> ${targetStatus.titleEn}")
        return true
    }

    suspend fun advancePurchaseOrderStage(poId: String, targetStage: ProcurementStage): Boolean {
        val po = _purchaseOrders.value.find { it.id == poId } ?: return false
        val updatedPo = po.copy(
            stage = targetStage,
            paidAmount = if (targetStage == ProcurementStage.SUPPLIER_PAYMENT) po.totalAmount else po.paidAmount,
            supplierInvoiceNumber = if (targetStage in setOf(ProcurementStage.SUPPLIER_INVOICE, ProcurementStage.SUPPLIER_PAYMENT)) (po.supplierInvoiceNumber ?: "SINV-${po.poNumber.takeLast(3)}") else po.supplierInvoiceNumber,
            updatedAt = System.currentTimeMillis()
        )
        _purchaseOrders.value = _purchaseOrders.value.map { if (it.id == poId) updatedPo else it }

        if (targetStage == ProcurementStage.RECEIVED && po.stage != ProcurementStage.RECEIVED) {
            val (updatedStock, grnTxs) = InventoryProcurementEngine.receivePurchaseOrderIntoStock(
                po = updatedPo,
                currentStockItems = _warehouseStockItems.value,
                performedBy = _currentUser.value.fullName
            )
            _warehouseStockItems.value = updatedStock
            _inventoryTransactions.value = grnTxs + _inventoryTransactions.value
        }

        if (targetStage == ProcurementStage.SUPPLIER_INVOICE && po.stage != ProcurementStage.SUPPLIER_INVOICE) {
            val poJournal = AccountingEngine.createPurchaseInvoiceJournalEntry(updatedPo)
            _journalEntries.value = listOf(poJournal) + _journalEntries.value
            _suppliers.value = _suppliers.value.map { sup ->
                if (sup.id == updatedPo.supplierId) sup.copy(currentPayableBalance = sup.currentPayableBalance + updatedPo.totalAmount) else sup
            }
        }

        if (targetStage == ProcurementStage.SUPPLIER_PAYMENT && po.stage != ProcurementStage.SUPPLIER_PAYMENT) {
            _suppliers.value = _suppliers.value.map { sup ->
                if (sup.id == updatedPo.supplierId) sup.copy(currentPayableBalance = (sup.currentPayableBalance - updatedPo.totalAmount).coerceAtLeast(0.0)) else sup
            }
        }

        recordAudit("PROCUREMENT_${targetStage.name}", "PurchaseOrder", poId, "${updatedPo.poNumber} (${updatedPo.supplierName}) -> ${targetStage.titleEn}")
        return true
    }

    suspend fun createPurchaseOrder(
        supplierId: String,
        warehouseId: String,
        itemId: String,
        quantity: Double,
        unitPrice: Double
    ): PurchaseOrderDocument? {
        val supplier = _suppliers.value.find { it.id == supplierId } ?: return null
        val warehouse = _warehouses.value.find { it.id == warehouseId } ?: return null
        val stockItem = _warehouseStockItems.value.find { it.itemId == itemId } ?: _warehouseStockItems.value.firstOrNull() ?: return null

        val subtotal = quantity * unitPrice
        val tax = subtotal * 0.10
        val newPo = PurchaseOrderDocument(
            id = "po_${UUID.randomUUID().toString().take(6)}",
            poNumber = "PO-2026-${(810..999).random()}",
            branchId = warehouse.branchId,
            warehouseId = warehouse.id,
            warehouseName = warehouse.nameEn,
            supplierId = supplier.id,
            supplierName = supplier.name,
            stage = ProcurementStage.PURCHASE_ORDER,
            lines = listOf(
                PurchaseOrderLine(
                    itemId = stockItem.itemId,
                    itemNameEn = stockItem.itemNameEn,
                    itemNameAr = stockItem.itemNameAr,
                    quantityOrdered = quantity,
                    quantityReceived = quantity,
                    purchaseUnit = stockItem.stockUnit,
                    stockConversionFactor = 1.0,
                    unitPrice = unitPrice
                )
            ),
            subtotal = subtotal,
            taxAmount = tax,
            totalAmount = subtotal + tax,
            requestedBy = _currentUser.value.fullName
        )
        _purchaseOrders.value = listOf(newPo) + _purchaseOrders.value
        recordAudit("PURCHASE_ORDER_CREATE", "PurchaseOrder", newPo.id, "Created ${newPo.poNumber} with ${supplier.name} for $${newPo.totalAmount}")
        return newPo
    }

    suspend fun recordStockCountAdjustment(stockItemId: String, countedQuantity: Double, notes: String): Boolean {
        val list = _warehouseStockItems.value.toMutableList()
        val idx = list.indexOfFirst { it.id == stockItemId }
        if (idx < 0) return false
        val item = list[idx]
        val systemQty = item.currentQuantity
        val delta = countedQuantity - systemQty
        list[idx] = item.copy(currentQuantity = countedQuantity.coerceAtLeast(0.0), lastUpdatedAt = System.currentTimeMillis())
        _warehouseStockItems.value = list

        val session = StockCountSession(
            id = "scs_${UUID.randomUUID().toString().take(6)}",
            sessionNumber = "CNT-${(100..999).random()}",
            branchId = item.branchId,
            warehouseId = item.warehouseId,
            warehouseName = item.warehouseName,
            status = "APPROVED",
            lines = listOf(
                StockCountLine(
                    itemId = item.itemId,
                    itemNameEn = item.itemNameEn,
                    itemNameAr = item.itemNameAr,
                    unit = item.stockUnit,
                    systemQuantity = systemQty,
                    countedQuantity = countedQuantity,
                    unitCost = item.unitCost
                )
            ),
            countedBy = _currentUser.value.fullName,
            approvedBy = "Karim Mansour",
            notes = notes
        )
        _stockCountSessions.value = listOf(session) + _stockCountSessions.value

        val tx = InventoryTransaction(
            id = "itx_cnt_${UUID.randomUUID().toString().take(6)}",
            referenceNumber = session.sessionNumber,
            branchId = item.branchId,
            warehouseId = item.warehouseId,
            warehouseName = item.warehouseName,
            itemId = item.itemId,
            itemNameEn = item.itemNameEn,
            itemNameAr = item.itemNameAr,
            transactionType = InventoryTransactionType.STOCK_COUNT,
            quantityDelta = delta,
            unit = item.stockUnit,
            unitCost = item.unitCost,
            balanceBefore = systemQty,
            balanceAfter = countedQuantity,
            sourceDocumentType = "COUNT_SESSION",
            sourceDocumentId = session.id,
            performedBy = _currentUser.value.fullName,
            approvedBy = "Karim Mansour",
            notes = notes.ifBlank { "Physical stock count variance adjustment" }
        )
        _inventoryTransactions.value = listOf(tx) + _inventoryTransactions.value
        recordAudit("STOCK_COUNT_APPROVE", "StockCountSession", session.id, "${session.sessionNumber}: ${item.itemNameEn} System=$systemQty, Counted=$countedQuantity (Variance=$delta)")
        return true
    }

    // --- Phase 10: Financial & Accounting Subsystem Methods ---
    suspend fun recordOperatingExpense(
        category: String,
        description: String,
        amount: Double,
        costCenterCode: String,
        paymentMethod: PaymentMethod
    ): Expense {
        val exp = Expense(
            id = "exp_${UUID.randomUUID().toString().take(6)}",
            branchId = _currentBranch.value.id,
            category = category,
            description = description,
            amount = amount,
            approvedBy = _currentUser.value.fullName,
            documentNumber = "EXP-${(1010..9999).random()}",
            costCenterCode = costCenterCode,
            paymentMethod = paymentMethod
        )
        val je = AccountingEngine.createExpenseJournalEntry(exp)
        val linkedExp = exp.copy(journalEntryId = je.id)
        _expenses.value = listOf(linkedExp) + _expenses.value
        _journalEntries.value = listOf(je) + _journalEntries.value
        recordAudit("EXPENSE_POSTED", "Expense", linkedExp.id, "Posted expense ${linkedExp.documentNumber} ($${linkedExp.amount}) with Journal ${je.entryNumber}")
        return linkedExp
    }

    suspend fun settleSubledgerInvoice(invoiceId: String, paymentAmount: Double) {
        val inv = _subledgerInvoices.value.find { it.id == invoiceId } ?: return
        val newPaid = (inv.paidAmount + paymentAmount).coerceAtMost(inv.totalAmount)
        val updated = inv.copy(
            paidAmount = newPaid,
            status = if (newPaid >= inv.totalAmount) "PAID" else "PARTIAL"
        )
        _subledgerInvoices.value = _subledgerInvoices.value.map { if (it.id == invoiceId) updated else it }
        recordAudit("SUBLEDGER_SETTLE", "SubledgerInvoice", invoiceId, "Settled $$paymentAmount on ${inv.invoiceNumber} (${inv.partyName})")
    }

    suspend fun updateFinancialPeriodStatus(periodId: String, status: FinancialPeriodStatus) {
        _financialPeriods.value = _financialPeriods.value.map {
            if (it.id == periodId) it.copy(status = status, closedBy = _currentUser.value.fullName) else it
        }
        recordAudit("FINANCIAL_PERIOD_${status.name}", "FinancialPeriod", periodId, "Financial period $periodId updated to ${status.name}")
    }

    // --- Phase 11: HR, Attendance, Leave & Payroll Methods ---
    suspend fun recordAttendanceCheckIn(
        employeeId: String,
        employeeName: String,
        department: String,
        source: AttendanceVerificationSource,
        note: String = ""
    ) {
        val rec = AttendanceRecord(
            id = "att_${UUID.randomUUID().toString().take(6)}",
            employeeId = employeeId,
            employeeNumber = "EMP-${employeeId.takeLast(2)}",
            employeeName = employeeName,
            department = department,
            branchId = _currentBranch.value.id,
            date = "2026-10-23",
            checkInTime = "08:00",
            status = "PRESENT",
            source = source,
            correctionNote = note
        )
        _attendanceRecords.value = listOf(rec) + _attendanceRecords.value
        recordAudit("ATTENDANCE_CHECK_IN", "AttendanceRecord", rec.id, "$employeeName checked in via ${source.code}")
    }

    suspend fun submitOrUpdateLeaveRequest(
        leaveId: String?,
        employeeId: String = "emp_04",
        employeeName: String = "Sarah Nader",
        leaveType: LeaveType = LeaveType.ANNUAL,
        daysCount: Int = 3,
        reason: String = "Personal leave",
        newStatus: String = "APPROVED"
    ) {
        if (leaveId == null) {
            val req = LeaveRequest(
                id = "lv_${UUID.randomUUID().toString().take(6)}",
                employeeId = employeeId,
                employeeName = employeeName,
                department = "Operations",
                branchId = _currentBranch.value.id,
                leaveType = leaveType,
                startDate = "2026-11-01",
                endDate = "2026-11-04",
                daysCount = daysCount,
                reason = reason,
                status = "PENDING"
            )
            _leaveRequests.value = listOf(req) + _leaveRequests.value
            recordAudit("LEAVE_REQUEST_SUBMIT", "LeaveRequest", req.id, "$employeeName requested $daysCount days (${leaveType.titleEn})")
        } else {
            _leaveRequests.value = _leaveRequests.value.map {
                if (it.id == leaveId) it.copy(status = newStatus, approvedBy = _currentUser.value.fullName) else it
            }
            recordAudit("LEAVE_REQUEST_$newStatus", "LeaveRequest", leaveId, "Leave request $leaveId updated to $newStatus")
        }
    }

    suspend fun updatePayrollRecordStatus(payrollId: String, newStatus: String) {
        _payrollRecords.value = _payrollRecords.value.map {
            if (it.id == payrollId) {
                it.copy(
                    status = newStatus,
                    approvedBy = _currentUser.value.fullName,
                    paidAt = if (newStatus == "PAID") System.currentTimeMillis() else it.paidAt
                )
            } else it
        }
        recordAudit("PAYROLL_$newStatus", "PayrollRecord", payrollId, "Payroll record $payrollId marked as $newStatus")
    }

    // --- Phase 12: Central Notification Service & Authorized AI Business Query Boundary ---
    fun dispatchCentralNotification(
        eventTrigger: String,
        titleEn: String,
        titleAr: String,
        messageEn: String,
        messageAr: String,
        channels: List<NotificationChannelType>,
        referenceId: String
    ) {
        val evt = CentralNotificationEvent(
            id = "cne_${UUID.randomUUID().toString().take(8)}",
            eventTrigger = eventTrigger,
            titleEn = titleEn,
            titleAr = titleAr,
            messageEn = messageEn,
            messageAr = messageAr,
            channelsDispatched = channels,
            referenceId = referenceId
        )
        _centralNotificationEvents.value = (listOf(evt) + _centralNotificationEvents.value).take(50)
    }

    fun executeAuthorizedAiQuery(queryText: String, allOrders: List<Order>): AuthorizedAiQueryResponse {
        return ReportingAndAiEngine.executeAuthorizedBusinessQuery(
            queryText = queryText,
            session = _currentSession.value,
            orders = allOrders,
            stockItems = _warehouseStockItems.value,
            deliveries = _deliveryAssignments.value,
            expenses = _expenses.value,
            journalEntries = _journalEntries.value,
            accounts = _chartOfAccounts.value
        )
    }

    fun publishRealTimeEvent(
        type: RealTimeEventType,
        branchId: String,
        entityId: String,
        referenceCode: String,
        titleEn: String,
        titleAr: String,
        detailsEn: String,
        detailsAr: String,
        sourceChannel: String = "SYSTEM"
    ) {
        val event = RealTimeOperationalEvent(
            id = "rt_${UUID.randomUUID().toString().take(8)}",
            type = type,
            branchId = branchId,
            entityId = entityId,
            referenceCode = referenceCode,
            titleEn = titleEn,
            titleAr = titleAr,
            detailsEn = detailsEn,
            detailsAr = detailsAr,
            sourceChannel = sourceChannel,
            timestamp = System.currentTimeMillis()
        )
        _realTimeEvents.value = (listOf(event) + _realTimeEvents.value).take(100)
        _realTimeEventStream.tryEmit(event)
    }

    fun selectBranch(branch: Branch) {
        _currentBranch.value = branch
        scope.launch {
            recordAudit("SWITCH_BRANCH", "Branch", branch.id, "Switched active operational branch to ${branch.nameEn}")
        }
    }

    suspend fun recordAudit(action: String, entity: String, id: String, details: String) {
        val user = _currentUser.value
        val branch = _currentBranch.value
        database.auditDao().insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                companyId = _currentCompany.value.id,
                branchId = branch.id,
                userId = user.id,
                userName = user.fullName,
                action = action,
                entityName = entity,
                entityId = id,
                details = details,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    fun addNotification(titleEn: String, titleAr: String, bodyEn: String, bodyAr: String, type: NotificationType) {
        val notif = AppNotification(
            id = UUID.randomUUID().toString(),
            titleEn = titleEn,
            titleAr = titleAr,
            bodyEn = bodyEn,
            bodyAr = bodyAr,
            type = type,
            timestamp = System.currentTimeMillis()
        )
        _notifications.value = listOf(notif) + _notifications.value
    }

    fun clearNotifications() {
        _notifications.value = emptyList()
    }

    fun updateCurrentUser(user: User) {
        _currentUser.value = user
        scope.launch {
            recordAudit("USER_SWITCH", "User", user.id, "Role session context switched to ${user.roleId}")
        }
    }

    // --- Private Helper Mappings ---

    private fun mapBranchEntityToDomain(it: BranchEntity): Branch {
        return Branch(
            id = it.id,
            brandId = it.brandId,
            code = it.code,
            nameEn = it.nameEn,
            nameAr = it.nameAr,
            phone = it.phone,
            address = it.address,
            seatingCapacity = it.seatingCapacity,
            isActive = it.isActive,
            openingTime = it.openingTime,
            closingTime = it.closingTime,
            timezone = it.timezone,
            currency = it.currency,
            taxRate = it.taxRate,
            taxInclusive = it.taxInclusive,
            assignedWarehouseId = it.assignedWarehouseId
        )
    }

    private fun mapUserEntityToDomain(it: UserEntity): User {
        return User(
            id = it.id,
            companyId = it.companyId,
            username = it.username,
            email = it.email,
            fullName = it.fullName,
            roleId = it.roleId,
            activeBranchId = it.activeBranchId,
            status = it.status,
            passwordHash = it.passwordHash,
            allowedBranchIds = it.allowedBranchIds.split(",").filter { s -> s.isNotBlank() },
            failedAttempts = it.failedAttempts,
            isLocked = it.isLocked,
            lastLoginAt = it.lastLoginAt
        )
    }

    private fun mapOrderEntityToDomain(
        it: OrderEntity,
        items: List<OrderItem>,
        payments: List<Payment>
    ): Order {
        return Order(
            id = it.id,
            branchId = it.branchId,
            orderNumber = it.orderNumber,
            orderType = runCatching { OrderType.valueOf(it.orderType) }.getOrDefault(OrderType.DINE_IN),
            tableId = it.tableId,
            customerId = it.customerId,
            status = runCatching { OrderStatus.valueOf(it.status) }.getOrDefault(OrderStatus.PENDING),
            subtotal = it.subtotal,
            taxAmount = it.taxAmount,
            discountAmount = it.discountAmount,
            totalAmount = it.totalAmount,
            paymentStatus = it.paymentStatus,
            createdAt = it.createdAt,
            terminalId = it.terminalId,
            cashierId = it.cashierId,
            cashierName = it.cashierName,
            customerName = it.customerName,
            customerPhone = it.customerPhone,
            channel = runCatching { SalesChannel.valueOf(it.channel) }.getOrDefault(SalesChannel.POS),
            couponCode = it.couponCode,
            discountReason = it.discountReason,
            deliveryFee = it.deliveryFee,
            deliveryAddress = it.deliveryAddress,
            serviceFee = it.serviceFee,
            notes = it.notes,
            holdLabel = it.holdLabel,
            idempotencyKey = it.idempotencyKey,
            syncStatus = runCatching { SyncStatus.valueOf(it.syncStatus) }.getOrDefault(SyncStatus.SYNCED),
            updatedAt = it.updatedAt,
            completedAt = it.completedAt,
            cancelledReason = it.cancelledReason,
            refundedAmount = it.refundedAmount,
            refundReason = it.refundReason,
            approvedByManager = it.approvedByManager,
            items = items,
            payments = payments
        )
    }

    private fun mapOrderDomainToEntity(order: Order): OrderEntity {
        return OrderEntity(
            id = order.id,
            branchId = order.branchId,
            orderNumber = order.orderNumber,
            orderType = order.orderType.name,
            tableId = order.tableId,
            customerId = order.customerId,
            status = order.status.name,
            subtotal = order.subtotal,
            taxAmount = order.taxAmount,
            discountAmount = order.discountAmount,
            totalAmount = order.totalAmount,
            paymentStatus = order.paymentStatus,
            createdAt = order.createdAt,
            terminalId = order.terminalId,
            cashierId = order.cashierId,
            cashierName = order.cashierName,
            customerName = order.customerName,
            customerPhone = order.customerPhone,
            channel = order.channel.name,
            couponCode = order.couponCode,
            discountReason = order.discountReason,
            deliveryFee = order.deliveryFee,
            deliveryAddress = order.deliveryAddress,
            serviceFee = order.serviceFee,
            notes = order.notes,
            holdLabel = order.holdLabel,
            idempotencyKey = order.idempotencyKey,
            syncStatus = order.syncStatus.name,
            updatedAt = order.updatedAt,
            completedAt = order.completedAt,
            cancelledReason = order.cancelledReason,
            refundedAmount = order.refundedAmount,
            refundReason = order.refundReason,
            approvedByManager = order.approvedByManager
        )
    }

    private fun mapOrderItemEntityToDomain(it: OrderItemEntity): OrderItem {
        return OrderItem(
            id = it.id,
            orderId = it.orderId,
            productId = it.productId,
            productNameEn = it.productNameEn,
            productNameAr = it.productNameAr,
            unitPrice = it.unitPrice,
            quantity = it.quantity,
            totalPrice = it.totalPrice,
            notes = it.notes,
            kitchenStatus = it.kitchenStatus,
            variantId = it.variantId,
            variantName = it.variantName,
            modifiersSummary = it.modifiersSummary,
            modifiersDelta = it.modifiersDelta,
            addonsSummary = it.addonsSummary,
            addonsDelta = it.addonsDelta,
            discountAmount = it.discountAmount,
            returnedQuantity = it.returnedQuantity
        )
    }

    private fun mapOrderItemDomainToEntity(it: OrderItem): OrderItemEntity {
        return OrderItemEntity(
            id = it.id,
            orderId = it.orderId,
            productId = it.productId,
            productNameEn = it.productNameEn,
            productNameAr = it.productNameAr,
            unitPrice = it.unitPrice,
            quantity = it.quantity,
            totalPrice = it.totalPrice,
            notes = it.notes,
            kitchenStatus = it.kitchenStatus,
            variantId = it.variantId,
            variantName = it.variantName,
            modifiersSummary = it.modifiersSummary,
            modifiersDelta = it.modifiersDelta,
            addonsSummary = it.addonsSummary,
            addonsDelta = it.addonsDelta,
            discountAmount = it.discountAmount,
            returnedQuantity = it.returnedQuantity
        )
    }

    private fun mapPaymentEntityToDomain(it: PaymentEntity): Payment {
        return Payment(
            id = it.id,
            orderId = it.orderId,
            branchId = it.branchId,
            amount = it.amount,
            paymentMethod = runCatching { PaymentMethod.valueOf(it.paymentMethod) }.getOrDefault(PaymentMethod.CASH),
            referenceNumber = it.referenceNumber,
            status = it.status,
            timestamp = it.timestamp,
            idempotencyKey = it.idempotencyKey,
            cashierId = it.cashierId,
            shiftId = it.shiftId,
            tenderedAmount = it.tenderedAmount,
            changeAmount = it.changeAmount
        )
    }

    private fun mapPaymentDomainToEntity(it: Payment): PaymentEntity {
        return PaymentEntity(
            id = it.id,
            orderId = it.orderId,
            branchId = it.branchId,
            amount = it.amount,
            paymentMethod = it.paymentMethod.name,
            referenceNumber = it.referenceNumber,
            status = it.status,
            timestamp = it.timestamp,
            idempotencyKey = it.idempotencyKey,
            cashierId = it.cashierId,
            shiftId = it.shiftId,
            tenderedAmount = it.tenderedAmount,
            changeAmount = it.changeAmount
        )
    }

    private fun mapShiftDomainToEntity(it: CashierShift): CashierShiftEntity {
        return CashierShiftEntity(
            id = it.id,
            branchId = it.branchId,
            terminalId = it.terminalId,
            cashierId = it.cashierId,
            cashierName = it.cashierName,
            status = it.status.name,
            openedAt = it.openedAt,
            closedAt = it.closedAt,
            openingCash = it.openingCash,
            cashSales = it.cashSales,
            cardSales = it.cardSales,
            bankTransferSales = it.bankTransferSales,
            walletSales = it.walletSales,
            cashIn = it.cashIn,
            cashOut = it.cashOut,
            expenses = it.expenses,
            cashRefunds = it.cashRefunds,
            actualClosingCash = it.actualClosingCash,
            closingNotes = it.closingNotes
        )
    }

    private fun mapTableEntityToDomain(it: TableEntity): RestaurantTable {
        return RestaurantTable(
            id = it.id,
            branchId = it.branchId,
            tableNumber = it.tableNumber,
            capacity = it.capacity,
            section = it.section,
            status = runCatching { TableStatus.valueOf(it.status) }.getOrDefault(TableStatus.AVAILABLE),
            currentOrderId = it.currentOrderId,
            floorId = it.floorId,
            floorName = it.floorName,
            zoneId = it.zoneId,
            combinedWithTableIds = it.combinedWithTableIdsCsv.split(",").filter { s -> s.isNotBlank() },
            combinedCapacity = if (it.combinedCapacity > 0) it.combinedCapacity else it.capacity,
            assignedWaiterId = it.assignedWaiterId,
            assignedWaiterName = it.assignedWaiterName,
            qrToken = it.qrToken.ifBlank {
                TableOperationsEngine.generateSecureQrToken("comp_01", it.branchId, it.id, it.tableNumber)
            },
            activeReservationId = it.activeReservationId,
            guestCount = it.guestCount,
            shape = it.shape,
            posX = it.posX,
            posY = it.posY
        )
    }

    private fun mapTableDomainToEntity(it: RestaurantTable): TableEntity {
        return TableEntity(
            id = it.id,
            branchId = it.branchId,
            tableNumber = it.tableNumber,
            capacity = it.capacity,
            section = it.section,
            status = it.status.name,
            currentOrderId = it.currentOrderId,
            floorId = it.floorId,
            floorName = it.floorName,
            zoneId = it.zoneId,
            combinedWithTableIdsCsv = it.combinedWithTableIds.joinToString(","),
            combinedCapacity = it.combinedCapacity,
            assignedWaiterId = it.assignedWaiterId,
            assignedWaiterName = it.assignedWaiterName,
            qrToken = it.qrToken,
            activeReservationId = it.activeReservationId,
            guestCount = it.guestCount,
            shape = it.shape,
            posX = it.posX,
            posY = it.posY
        )
    }

    private fun mapReservationEntityToDomain(it: ReservationEntity): Reservation {
        return Reservation(
            id = it.id,
            branchId = it.branchId,
            customerId = it.customerId,
            customerName = it.customerName,
            customerPhone = it.customerPhone,
            customerEmail = it.customerEmail,
            tableId = it.tableId,
            tableNumber = it.tableNumber,
            reservationDate = it.reservationDate,
            reservationTime = it.reservationTime,
            partySize = it.partySize,
            status = runCatching { ReservationStatus.valueOf(it.status) }.getOrDefault(ReservationStatus.PENDING),
            notes = it.notes,
            confirmationCode = it.confirmationCode,
            createdAt = it.createdAt,
            updatedAt = it.updatedAt
        )
    }

    private fun mapReservationDomainToEntity(it: Reservation): ReservationEntity {
        return ReservationEntity(
            id = it.id,
            branchId = it.branchId,
            customerId = it.customerId,
            customerName = it.customerName,
            customerPhone = it.customerPhone,
            customerEmail = it.customerEmail,
            tableId = it.tableId,
            tableNumber = it.tableNumber,
            reservationDate = it.reservationDate,
            reservationTime = it.reservationTime,
            partySize = it.partySize,
            status = it.status.name,
            notes = it.notes,
            confirmationCode = it.confirmationCode,
            createdAt = it.createdAt,
            updatedAt = it.updatedAt
        )
    }

    private suspend fun seedInitialDataIfEmpty() {
        // Seed Company
        val compEntity = CompanyEntity(
            id = "comp_01",
            code = "AL-HAYA",
            nameEn = "Al-Haya Hospitality Group",
            nameAr = "مجموعة الحياة للضيافة والمطاعم",
            taxNumber = "TX-9948210-SA",
            defaultCurrency = "USD",
            country = "Saudi Arabia",
            fiscalYear = "2026-2027",
            autoLockMinutes = 15,
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        database.companyDao().insertCompany(compEntity)

        // Seed Brands
        val brandEntities = listOf(
            BrandEntity("brand_01", "comp_01", "BURGER_CRAFT", "Burger Craft & Smokehouse", "برجر كرافت أند سموك هاوس", "ACTIVE", "", "#F59E0B", "#0F172A", "Gourmet Burgers & Artisan Smokehouse", true),
            BrandEntity("brand_02", "comp_01", "PIZZA_ROMA", "Pizza Roma Woodfire", "بيتزا روما الحطبية", "ACTIVE", "", "#EF4444", "#1E293B", "Artisan Woodfired Pizza & Pasta", true)
        )
        database.brandDao().insertBrands(brandEntities)

        // Seed Branches
        val branchEntities = listOf(
            BranchEntity(
                id = "branch_01",
                brandId = "brand_01",
                code = "DOWNTOWN-HQ",
                nameEn = "Downtown Flagship Branch",
                nameAr = "فرع وسط المدينة الرئيسي",
                phone = "+966 11 456 7890",
                address = "King Fahd Road, Suite 400",
                seatingCapacity = 120,
                isActive = true,
                openingTime = "10:00",
                closingTime = "01:00",
                timezone = "Asia/Riyadh (GMT+3)",
                currency = "USD",
                taxRate = 0.10,
                taxInclusive = false,
                assignedWarehouseId = "wh_01"
            ),
            BranchEntity(
                id = "branch_02",
                brandId = "brand_01",
                code = "NORTH-MALL",
                nameEn = "North Gate Mall Branch",
                nameAr = "فرع مجمع البوابة الشمالية",
                phone = "+966 11 456 7891",
                address = "North Gate Blvd, Floor 2",
                seatingCapacity = 85,
                isActive = true,
                openingTime = "11:00",
                closingTime = "00:00",
                timezone = "Asia/Riyadh (GMT+3)",
                currency = "USD",
                taxRate = 0.10,
                taxInclusive = false,
                assignedWarehouseId = "wh_02"
            ),
            BranchEntity(
                id = "branch_03",
                brandId = "brand_01",
                code = "AIRPORT-EXPRESS",
                nameEn = "Airport Express Concourse",
                nameAr = "فرع مطار الملك خالد السريع",
                phone = "+966 11 456 7892",
                address = "Terminal 3 Departures",
                seatingCapacity = 45,
                isActive = true,
                openingTime = "00:00",
                closingTime = "23:59",
                timezone = "Asia/Riyadh (GMT+3)",
                currency = "USD",
                taxRate = 0.10,
                taxInclusive = true,
                assignedWarehouseId = "wh_01"
            )
        )
        database.branchDao().insertBranches(branchEntities)

        // Seed POS Terminals
        val terminals = listOf(
            PosTerminalEntity("term_01", "branch_01", "POS-01", "Front Counter Register A", "ONLINE", "192.168.1.101"),
            PosTerminalEntity("term_02", "branch_01", "POS-02", "Bar & Drink Dispense", "ONLINE", "192.168.1.102"),
            PosTerminalEntity("term_03", "branch_01", "POS-03", "Drive-thru / Express Pickup", "OFFLINE", "192.168.1.103"),
            PosTerminalEntity("term_04", "branch_02", "POS-01", "North Mall Main Till", "ONLINE", "192.168.2.101")
        )
        database.posTerminalDao().insertTerminals(terminals)

        // Seed Roles with granular permissions
        val roles = listOf(
            RoleEntity(
                id = "role_admin",
                code = "ROLE_ADMIN",
                nameEn = "System Administrator",
                nameAr = "مدير النظام العام",
                permissionsJson = "*",
                isSystem = true,
                branchRestricted = false,
                description = "Full global tenant access to all modules, financial actions and security configurations"
            ),
            RoleEntity(
                id = "role_manager",
                code = "ROLE_MANAGER",
                nameEn = "Branch General Manager",
                nameAr = "مدير الفرع العام",
                permissionsJson = "ADMINISTRATION:BRANCH_MANAGEMENT:VIEW,ADMINISTRATION:BRANCH_MANAGEMENT:EDIT,ADMINISTRATION:EMPLOYEE_MANAGEMENT:VIEW,ADMINISTRATION:EMPLOYEE_MANAGEMENT:EDIT,POS:POS_TERMINAL:VIEW,POS:POS_TERMINAL:CREATE,POS:POS_TERMINAL:APPROVE,POS:POS_TERMINAL:REFUND,POS:POS_TERMINAL:DISCOUNT,TABLES:FLOOR_TABLES:VIEW,TABLES:FLOOR_TABLES:EDIT,KITCHEN:KDS_SCREEN:VIEW,INVENTORY:WAREHOUSE_STOCK:VIEW,REPORTS:AUDIT_TRAIL:VIEW",
                isSystem = true,
                branchRestricted = true,
                description = "Branch-level authority for orders, refunds, floor layout, staff and operational settings"
            ),
            RoleEntity(
                id = "role_cashier",
                code = "ROLE_CASHIER",
                nameEn = "Lead Cashier",
                nameAr = "كاشير رئيسي",
                permissionsJson = "POS:POS_TERMINAL:VIEW,POS:POS_TERMINAL:CREATE,TABLES:FLOOR_TABLES:VIEW,TABLES:FLOOR_TABLES:EDIT",
                isSystem = true,
                branchRestricted = true,
                description = "Order taking, table billing and standard cash/card tenders at POS terminal"
            ),
            RoleEntity(
                id = "role_waiter",
                code = "ROLE_WAITER",
                nameEn = "Floor Server / Waiter",
                nameAr = "خادم صالة / نادل",
                permissionsJson = "POS:POS_TERMINAL:CREATE,TABLES:FLOOR_TABLES:VIEW,TABLES:FLOOR_TABLES:EDIT",
                isSystem = true,
                branchRestricted = true,
                description = "Floor guest service, dine-in ordering, guest seating and status changes"
            ),
            RoleEntity(
                id = "role_chef",
                code = "ROLE_CHEF",
                nameEn = "Executive Chef",
                nameAr = "رئيس الطهاة",
                permissionsJson = "KITCHEN:KDS_SCREEN:VIEW,KITCHEN:KDS_SCREEN:EDIT,INVENTORY:WAREHOUSE_STOCK:VIEW",
                isSystem = true,
                branchRestricted = true,
                description = "Kitchen display system operations, prep line bump bars, and recipe par viewing"
            ),
            RoleEntity(
                id = "role_driver",
                code = "ROLE_DRIVER",
                nameEn = "Delivery Driver",
                nameAr = "سائق توصيل",
                permissionsJson = "POS:POS_TERMINAL:VIEW",
                isSystem = true,
                branchRestricted = true,
                description = "Delivery dispatch and customer receipt delivery confirmation"
            )
        )
        database.roleDao().insertRoles(roles)

        // Seed Users
        val users = listOf(
            UserEntity(
                id = "user_01",
                companyId = "comp_01",
                username = "admin_mira",
                email = "mira.ops@alhargroup.com",
                fullName = "Mira Hamoud",
                roleId = "ROLE_ADMIN",
                activeBranchId = "branch_01",
                status = "ACTIVE",
                passwordHash = "password123",
                allowedBranchIds = "branch_01,branch_02,branch_03",
                failedAttempts = 0,
                isLocked = false,
                lastLoginAt = System.currentTimeMillis()
            ),
            UserEntity(
                id = "user_02",
                companyId = "comp_01",
                username = "mgr_karim",
                email = "karim.m@alhargroup.com",
                fullName = "Karim Mansour",
                roleId = "ROLE_MANAGER",
                activeBranchId = "branch_01",
                status = "ACTIVE",
                passwordHash = "password123",
                allowedBranchIds = "branch_01",
                failedAttempts = 0,
                isLocked = false,
                lastLoginAt = System.currentTimeMillis() - 7200000
            ),
            UserEntity(
                id = "user_03",
                companyId = "comp_01",
                username = "cashier_youssef",
                email = "youssef.z@alhargroup.com",
                fullName = "Youssef Zaid",
                roleId = "ROLE_CASHIER",
                activeBranchId = "branch_01",
                status = "ACTIVE",
                passwordHash = "password123",
                allowedBranchIds = "branch_01",
                failedAttempts = 0,
                isLocked = false,
                lastLoginAt = System.currentTimeMillis() - 3600000
            ),
            UserEntity(
                id = "user_04",
                companyId = "comp_01",
                username = "waiter_sarah",
                email = "sarah.n@alhargroup.com",
                fullName = "Sarah Nader",
                roleId = "ROLE_WAITER",
                activeBranchId = "branch_01",
                status = "ACTIVE",
                passwordHash = "password123",
                allowedBranchIds = "branch_01",
                failedAttempts = 0,
                isLocked = false,
                lastLoginAt = System.currentTimeMillis() - 1800000
            ),
            UserEntity(
                id = "user_05",
                companyId = "comp_01",
                username = "chef_antoine",
                email = "antoine.k@alhargroup.com",
                fullName = "Antoine Khalil",
                roleId = "ROLE_CHEF",
                activeBranchId = "branch_01",
                status = "ACTIVE",
                passwordHash = "password123",
                allowedBranchIds = "branch_01,branch_02",
                failedAttempts = 0,
                isLocked = false,
                lastLoginAt = System.currentTimeMillis() - 900000
            ),
            UserEntity(
                id = "user_06",
                companyId = "comp_01",
                username = "driver_tariq",
                email = "tariq.a@alhargroup.com",
                fullName = "Tariq Ali",
                roleId = "ROLE_DRIVER",
                activeBranchId = "branch_01",
                status = "ACTIVE",
                passwordHash = "password123",
                allowedBranchIds = "branch_01",
                failedAttempts = 0,
                isLocked = false,
                lastLoginAt = System.currentTimeMillis() - 600000
            )
        )
        database.userDao().insertUsers(users)

        // Seed Employees
        val employees = listOf(
            EmployeeEntity("emp_01", "branch_01", "user_01", "EMP-1001", "Mira Hamoud", "Chief Operating Officer", "Management", "+966 50 111 2222", "mira.ops@alhargroup.com", 65.0, "ACTIVE", "2023-01-10"),
            EmployeeEntity("emp_02", "branch_01", "user_02", "EMP-1002", "Karim Mansour", "Branch General Manager", "Management", "+966 50 222 3333", "karim.m@alhargroup.com", 42.0, "ACTIVE", "2023-06-15"),
            EmployeeEntity("emp_03", "branch_01", "user_03", "EMP-1003", "Youssef Zaid", "Senior Cashier & Till Lead", "Front of House", "+966 50 333 4444", "youssef.z@alhargroup.com", 22.0, "ACTIVE", "2024-02-01"),
            EmployeeEntity("emp_04", "branch_01", "user_04", "EMP-1004", "Sarah Nader", "Dining Room Captain", "Front of House", "+966 50 444 5555", "sarah.n@alhargroup.com", 20.0, "ACTIVE", "2024-03-20"),
            EmployeeEntity("emp_05", "branch_01", "user_05", "EMP-1005", "Chef Antoine Khalil", "Executive Sous Chef", "Kitchen", "+966 50 555 6666", "antoine.k@alhargroup.com", 38.0, "ACTIVE", "2023-09-01"),
            EmployeeEntity("emp_06", "branch_01", "user_06", "EMP-1006", "Tariq Ali", "Fleet Courier", "Delivery", "+966 50 666 7777", "tariq.a@alhargroup.com", 18.0, "ACTIVE", "2024-05-10")
        )
        database.employeeDao().insertEmployees(employees)

        // Seed Phase 3 Menu & Product Engine Data
        _categories.value = DefaultSeedData.categories
        _modifierGroups.value = DefaultSeedData.modifierGroups
        _addons.value = DefaultSeedData.addons
        _ingredients.value = DefaultSeedData.ingredients
        _recipes.value = DefaultSeedData.recipes
        _richProducts.value = DefaultSeedData.products
        _combos.value = DefaultSeedData.combos
        _priceRules.value = DefaultSeedData.priceRules
        _taxProfiles.value = DefaultSeedData.taxProfiles

        // Seed Products to Room for offline POS
        val productEntities = DefaultSeedData.products.map {
            ProductEntity(it.id, it.categoryId, it.sku, it.nameEn, it.nameAr, it.price, it.costPrice, it.isAvailable, it.isTaxable, it.currentStock, it.reorderPoint)
        }
        database.productDao().insertProducts(productEntities)

        // Seed Tables with Phase 5 Floor, Zone, Secure QR Token & Waiter metadata
        val tableEntities = listOf(
            TableEntity(
                id = "t1",
                branchId = "branch_01",
                tableNumber = "T-01",
                capacity = 2,
                section = "Indoor Main",
                status = TableStatus.AVAILABLE.name,
                currentOrderId = null,
                floorId = "floor_01",
                floorName = "Ground Floor Dining",
                zoneId = "zone_indoor",
                combinedCapacity = 2,
                assignedWaiterId = "user_04",
                assignedWaiterName = "Sarah Nader",
                qrToken = TableOperationsEngine.generateSecureQrToken("comp_01", "branch_01", "t1", "T-01"),
                shape = "ROUND",
                posX = 1,
                posY = 1
            ),
            TableEntity(
                id = "t2",
                branchId = "branch_01",
                tableNumber = "T-02",
                capacity = 4,
                section = "Indoor Main",
                status = TableStatus.OCCUPIED.name,
                currentOrderId = "ord_demo_1",
                floorId = "floor_01",
                floorName = "Ground Floor Dining",
                zoneId = "zone_indoor",
                combinedCapacity = 4,
                assignedWaiterId = "user_04",
                assignedWaiterName = "Sarah Nader",
                qrToken = TableOperationsEngine.generateSecureQrToken("comp_01", "branch_01", "t2", "T-02"),
                activeReservationId = "res_03",
                guestCount = 3,
                shape = "RECTANGLE",
                posX = 2,
                posY = 1
            ),
            TableEntity(
                id = "t3",
                branchId = "branch_01",
                tableNumber = "T-03",
                capacity = 4,
                section = "Indoor Main",
                status = TableStatus.AVAILABLE.name,
                currentOrderId = null,
                floorId = "floor_01",
                floorName = "Ground Floor Dining",
                zoneId = "zone_indoor",
                combinedCapacity = 4,
                assignedWaiterId = "user_04",
                assignedWaiterName = "Sarah Nader",
                qrToken = TableOperationsEngine.generateSecureQrToken("comp_01", "branch_01", "t3", "T-03"),
                shape = "RECTANGLE",
                posX = 3,
                posY = 1
            ),
            TableEntity(
                id = "t4",
                branchId = "branch_01",
                tableNumber = "T-04",
                capacity = 6,
                section = "Indoor Main",
                status = TableStatus.WAITING_PAYMENT.name,
                currentOrderId = "ord_demo_2",
                floorId = "floor_01",
                floorName = "Ground Floor Dining",
                zoneId = "zone_indoor",
                combinedCapacity = 6,
                assignedWaiterId = "user_04",
                assignedWaiterName = "Sarah Nader",
                qrToken = TableOperationsEngine.generateSecureQrToken("comp_01", "branch_01", "t4", "T-04"),
                guestCount = 5,
                shape = "BOOTH",
                posX = 1,
                posY = 2
            ),
            TableEntity(
                id = "t5",
                branchId = "branch_01",
                tableNumber = "T-05",
                capacity = 8,
                section = "VIP Suite",
                status = TableStatus.RESERVED.name,
                currentOrderId = null,
                floorId = "floor_02",
                floorName = "Upper Mezzanine & VIP",
                zoneId = "zone_vip",
                combinedCapacity = 8,
                assignedWaiterId = "user_04",
                assignedWaiterName = "Sarah Nader",
                qrToken = TableOperationsEngine.generateSecureQrToken("comp_01", "branch_01", "t5", "T-05"),
                activeReservationId = "res_01",
                guestCount = 6,
                shape = "BOOTH",
                posX = 2,
                posY = 2
            ),
            TableEntity(
                id = "t6",
                branchId = "branch_01",
                tableNumber = "T-06",
                capacity = 4,
                section = "Terrace Garden",
                status = TableStatus.AVAILABLE.name,
                currentOrderId = null,
                floorId = "floor_03",
                floorName = "Outdoor Garden & Patio",
                zoneId = "zone_terrace",
                combinedCapacity = 4,
                assignedWaiterId = "user_04",
                assignedWaiterName = "Sarah Nader",
                qrToken = TableOperationsEngine.generateSecureQrToken("comp_01", "branch_01", "t6", "T-06"),
                shape = "ROUND",
                posX = 1,
                posY = 3
            ),
            TableEntity(
                id = "t7",
                branchId = "branch_01",
                tableNumber = "T-07",
                capacity = 2,
                section = "Terrace Garden",
                status = TableStatus.OCCUPIED.name,
                currentOrderId = "ord_demo_3",
                floorId = "floor_03",
                floorName = "Outdoor Garden & Patio",
                zoneId = "zone_terrace",
                combinedCapacity = 2,
                assignedWaiterId = "user_04",
                assignedWaiterName = "Sarah Nader",
                qrToken = TableOperationsEngine.generateSecureQrToken("comp_01", "branch_01", "t7", "T-07"),
                guestCount = 2,
                shape = "ROUND",
                posX = 2,
                posY = 3
            ),
            TableEntity(
                id = "t8",
                branchId = "branch_01",
                tableNumber = "T-08",
                capacity = 4,
                section = "Terrace Garden",
                status = TableStatus.CLEANING.name,
                currentOrderId = null,
                floorId = "floor_03",
                floorName = "Outdoor Garden & Patio",
                zoneId = "zone_terrace",
                combinedCapacity = 4,
                assignedWaiterId = "user_04",
                assignedWaiterName = "Sarah Nader",
                qrToken = TableOperationsEngine.generateSecureQrToken("comp_01", "branch_01", "t8", "T-08"),
                shape = "RECTANGLE",
                posX = 3,
                posY = 3
            )
        )
        database.tableDao().insertTables(tableEntities)

        // Seed Phase 5 Reservations
        val reservationEntities = DefaultSeedData.reservations.map { mapReservationDomainToEntity(it) }
        database.reservationDao().insertReservations(reservationEntities)

        // Seed Demo Orders & Items
        val now = System.currentTimeMillis()
        val demoOrders = listOf(
            OrderEntity(
                id = "ord_demo_1",
                branchId = "branch_01",
                orderNumber = "ORD-1041",
                orderType = "DINE_IN",
                tableId = "t2",
                customerId = "cust_1",
                status = "PREPARING",
                subtotal = 26.00,
                taxAmount = 2.60,
                discountAmount = 0.0,
                totalAmount = 28.60,
                paymentStatus = "UNPAID",
                createdAt = now - 900_000,
                terminalId = "POS-01",
                cashierId = "user_03",
                cashierName = "Youssef Zaid",
                customerName = "Faisal Al-Rashid",
                customerPhone = "+966 55 101 2020",
                notes = "VIP guest at Table T-02"
            ),
            OrderEntity(
                id = "ord_demo_2",
                branchId = "branch_01",
                orderNumber = "ORD-1042",
                orderType = "DINE_IN",
                tableId = "t4",
                customerId = "cust_2",
                status = "COMPLETED",
                subtotal = 40.00,
                taxAmount = 3.50,
                discountAmount = 5.0,
                totalAmount = 38.50,
                paymentStatus = "PAID",
                createdAt = now - 1800_000,
                terminalId = "POS-01",
                cashierId = "user_03",
                cashierName = "Youssef Zaid",
                customerName = "Layla Al-Otaibi",
                customerPhone = "+966 50 303 4040",
                couponCode = "BURGER5",
                completedAt = now - 1200_000
            ),
            OrderEntity(
                id = "ord_demo_3",
                branchId = "branch_01",
                orderNumber = "ORD-1043",
                orderType = "DINE_IN",
                tableId = "t7",
                customerId = null,
                status = "PENDING",
                subtotal = 21.50,
                taxAmount = 2.15,
                discountAmount = 0.0,
                totalAmount = 23.65,
                paymentStatus = "UNPAID",
                createdAt = now - 300_000,
                terminalId = "POS-01",
                cashierId = "user_03",
                cashierName = "Youssef Zaid"
            ),
            OrderEntity(
                id = "ord_demo_4",
                branchId = "branch_01",
                orderNumber = "ORD-1044",
                orderType = "TAKEOUT",
                tableId = null,
                customerId = "cust_3",
                status = "HELD",
                subtotal = 18.50,
                taxAmount = 1.85,
                discountAmount = 0.0,
                totalAmount = 20.35,
                paymentStatus = "UNPAID",
                createdAt = now - 450_000,
                terminalId = "POS-01",
                cashierId = "user_03",
                cashierName = "Youssef Zaid",
                customerName = "Omar Zaki",
                customerPhone = "+966 54 505 6060",
                holdLabel = "Omar Z. - Waiting for colleague"
            )
        )
        demoOrders.forEach { database.orderDao().insertOrder(it) }

        val demoItems = listOf(
            OrderItemEntity("oi_1", "ord_demo_1", "p1", "Truffle Angus Burger", "برجر أنجوس بالكمأة", 18.50, 1, 18.50, "Medium, Brioche", "COOKING", "v_p1_s", "Single (150g)", "Medium, Golden Butter Brioche"),
            OrderItemEntity("oi_2", "ord_demo_1", "p4", "Loaded Truffle Fries", "بطاطس محمرة بالكمأة والبارميزان", 7.50, 1, 7.50, "Extra crispy", "COOKING", "v_p4_reg", "Regular Basket"),
            OrderItemEntity("oi_3", "ord_demo_2", "p3", "Crispy Nashville Hot Chicken", "برجر دجاج ناشفيل المقرمش", 14.50, 2, 29.00, "", "READY", "v_p3_hot", "Nashville Hot", "Classic Nashville Hot"),
            OrderItemEntity("oi_4", "ord_demo_2", "p6", "Craft Passionfruit Lemonade", "ليمونادا باشن فروت طبيعية", 5.50, 2, 11.00, "Less ice", "READY", "v_p6_reg", "Regular (400ml)"),
            OrderItemEntity("oi_5", "ord_demo_3", "p2", "Smoked BBQ Bacon Burger", "برجر باربيكيو باللحم المقدد", 16.00, 1, 16.00, "", "PENDING", "v_p2_s", "Single (150g)"),
            OrderItemEntity("oi_6", "ord_demo_3", "p6", "Craft Passionfruit Lemonade", "ليمونادا باشن فروت طبيعية", 5.50, 1, 5.50, "", "PENDING", "v_p6_reg", "Regular (400ml)"),
            OrderItemEntity("oi_7", "ord_demo_4", "p1", "Truffle Angus Burger", "برجر أنجوس بالكمأة", 18.50, 1, 18.50, "Parked order", "PENDING", "v_p1_s", "Single (150g)")
        )
        database.orderDao().insertOrderItems(demoItems)
        database.shiftDao().insertShift(mapShiftDomainToEntity(DefaultSeedData.initialShift))

        // Initial Kitchen Tickets (Phase 6 Multi-Station Seeded Tickets)
        val initialTickets = DefaultSeedData.initialKitchenTickets
        _kitchenTickets.value = initialTickets
        database.kitchenTicketDao().insertTickets(initialTickets.map { mapKitchenTicketDomainToEntity(it) })

        // Seed Initial Real-Time Operational Events
        _realTimeEvents.value = listOf(
            RealTimeOperationalEvent(
                id = "rt_seed_1",
                type = RealTimeEventType.KITCHEN_TICKET_UPDATED,
                branchId = "branch_01",
                entityId = "kt_ord_demo_1_grill",
                referenceCode = "ORD-1041",
                titleEn = "ORD-1041 [Grill Station] -> Preparing",
                titleAr = "ORD-1041 [محطة الشواء والجريل] -> قيد التحضير",
                detailsEn = "1x Truffle Angus Burger (Single 150g) [Medium, Golden Butter Brioche]",
                detailsAr = "1x برجر أنجوس بالكمأة",
                sourceChannel = "KDS_GRILL",
                timestamp = now - 300_000
            ),
            RealTimeOperationalEvent(
                id = "rt_seed_2",
                type = RealTimeEventType.NEW_ORDER,
                branchId = "branch_01",
                entityId = "ord_demo_3",
                referenceCode = "ORD-1043",
                titleEn = "New Order ORD-1043 (Waiter Handheld)",
                titleAr = "طلب جديد ORD-1043 (تطبيق النادل)",
                detailsEn = "Routed to Grill Station & Drinks Station • Table T-07",
                detailsAr = "تم التوجيه إلى محطة الشواء ومحطة المشروبات • طاولة T-07",
                sourceChannel = "WAITER_APP",
                timestamp = now - 240_000
            ),
            RealTimeOperationalEvent(
                id = "rt_seed_3",
                type = RealTimeEventType.TABLE_CHANGED,
                branchId = "branch_01",
                entityId = "t4",
                referenceCode = "T-04",
                titleEn = "Table T-04 Requested Bill",
                titleAr = "طاولة T-04 طلبت الفاتورة",
                detailsEn = "Waiting Payment ($38.50)",
                detailsAr = "بانتظار الدفع ($38.50)",
                sourceChannel = "QR_TABLE",
                timestamp = now - 120_000
            )
        )

        // Initial Notifications
        _notifications.value = listOf(
            AppNotification("n1", "Table 4 Requested Bill", "طاولة 4 طلبت الفاتورة", "Server assigned: Sarah N.", "الخادم المسؤول: سارة ن.", NotificationType.ALERT, now - 120_000),
            AppNotification("n2", "Low Stock Alert: Onion Rings", "تنبيه انخفاض مخزون: حلقات البصل", "Only 4.0 portions remaining in Downtown Warehouse", "متبقي 4.0 حصص فقط في مستودع الفرع", NotificationType.WARNING, now - 600_000),
            AppNotification("n3", "Kitchen Dispatch ORD-1042", "جاهزية طلب المطبخ ORD-1042", "Hot line finished preparation for Table T-04", "انتهى المطبخ من تجهيز طلب طاولة T-04", NotificationType.ORDER, now - 900_000)
        )

        // Reset Phase 7-12 Enterprise StateFlows on seed
        _activePortalCustomer.value = DefaultSeedData.customers.firstOrNull()
        _customerAddresses.value = EnterprisePhase7To12SeedData.customerAddresses
        _loyaltyRewards.value = EnterprisePhase7To12SeedData.loyaltyRewards
        _customerReviews.value = EnterprisePhase7To12SeedData.customerReviews
        _customerComplaints.value = EnterprisePhase7To12SeedData.customerComplaints
        _deliveryZones.value = EnterprisePhase7To12SeedData.deliveryZones
        _drivers.value = EnterprisePhase7To12SeedData.drivers
        _deliveryAssignments.value = EnterprisePhase7To12SeedData.initialDeliveryAssignments
        _driverCashSettlements.value = emptyList()
        _warehouses.value = EnterprisePhase7To12SeedData.warehouses
        _unitConversionRules.value = EnterprisePhase7To12SeedData.unitConversionRules
        _warehouseStockItems.value = EnterprisePhase7To12SeedData.warehouseStockItems
        _suppliers.value = EnterprisePhase7To12SeedData.suppliers
        _inventoryTransactions.value = EnterprisePhase7To12SeedData.initialInventoryTransactions
        _warehouseTransfers.value = EnterprisePhase7To12SeedData.initialWarehouseTransfers
        _purchaseOrders.value = EnterprisePhase7To12SeedData.initialPurchaseOrders
        _wasteRecords.value = EnterprisePhase7To12SeedData.initialWasteRecords
        _stockCountSessions.value = emptyList()
        _chartOfAccounts.value = EnterprisePhase7To12SeedData.chartOfAccounts
        _costCenters.value = EnterprisePhase7To12SeedData.costCenters
        _financialPeriods.value = EnterprisePhase7To12SeedData.financialPeriods
        _expenses.value = EnterprisePhase7To12SeedData.initialExpenses
        _journalEntries.value = EnterprisePhase7To12SeedData.initialJournalEntries
        _bankAccounts.value = EnterprisePhase7To12SeedData.bankAccounts
        _bankTransactions.value = emptyList()
        _subledgerInvoices.value = EnterprisePhase7To12SeedData.subledgerInvoices
        _shiftTemplates.value = EnterprisePhase7To12SeedData.shiftTemplates
        _attendanceRecords.value = EnterprisePhase7To12SeedData.attendanceRecords
        _leaveRequests.value = EnterprisePhase7To12SeedData.leaveRequests
        _payrollRecords.value = EnterprisePhase7To12SeedData.payrollRecords
        _centralNotificationEvents.value = EnterprisePhase7To12SeedData.centralNotificationEvents

        recordAudit("SYSTEM_INIT", "Core", "boot", "RestoOps Enterprise ERP platform initialized with Phases 1-12 full production modules")
    }

    private fun mapKitchenTicketDomainToEntity(it: KitchenTicket): KitchenTicketEntity {
        val itemsPayload = it.items.joinToString(";;") { item ->
            listOf(
                item.id,
                item.orderItemId,
                item.productId,
                item.productNameEn.replace("|", "/"),
                item.productNameAr.replace("|", "/"),
                (item.variantName ?: "").replace("|", "/"),
                item.quantity.toString(),
                item.modifiersSummary.replace("|", "/"),
                item.addonsSummary.replace("|", "/"),
                item.notes.replace("|", "/"),
                item.stationType.name,
                item.status.name
            ).joinToString("|")
        }
        return KitchenTicketEntity(
            id = it.id,
            branchId = it.branchId,
            orderId = it.orderId,
            orderNumber = it.orderNumber,
            stationName = it.stationName,
            itemsSummary = it.itemsSummary,
            status = it.status,
            elapsedMinutes = it.elapsedMinutes,
            isPriority = it.isPriority,
            kitchenId = it.kitchenId,
            prepAreaId = it.prepAreaId,
            stationId = it.stationId,
            stationType = it.stationType.name,
            orderType = it.orderType.name,
            channel = it.channel.name,
            tableId = it.tableId,
            tableNumber = it.tableNumber,
            serverOrCashierName = it.serverOrCashierName,
            customerName = it.customerName,
            priority = it.priority.name,
            ticketStatus = it.ticketStatus.name,
            itemsPayload = itemsPayload,
            orderNotes = it.orderNotes,
            warningThresholdMinutes = it.warningThresholdMinutes,
            overdueThresholdMinutes = it.overdueThresholdMinutes,
            workflowStage = it.workflowStage,
            createdAt = it.createdAt,
            acceptedAt = it.acceptedAt,
            preparingAt = it.preparingAt,
            readyAt = it.readyAt,
            completedAt = it.completedAt,
            assignedChefName = it.assignedChefName
        )
    }

    private fun mapKitchenTicketEntityToDomain(it: KitchenTicketEntity): KitchenTicket {
        val parsedItems = if (it.itemsPayload.isBlank()) {
            emptyList()
        } else {
            it.itemsPayload.split(";;").mapNotNull { raw ->
                val parts = raw.split("|")
                if (parts.size >= 12) {
                    KitchenTicketItem(
                        id = parts[0],
                        orderItemId = parts[1],
                        productId = parts[2],
                        productNameEn = parts[3],
                        productNameAr = parts[4],
                        variantName = parts[5].ifBlank { null },
                        quantity = parts[6].toIntOrNull() ?: 1,
                        modifiersSummary = parts[7],
                        addonsSummary = parts[8],
                        notes = parts[9],
                        stationType = runCatching { KitchenStationType.valueOf(parts[10]) }.getOrDefault(KitchenStationType.GRILL),
                        status = runCatching { KdsTicketStatus.valueOf(parts[11]) }.getOrDefault(KdsTicketStatus.NEW)
                    )
                } else null
            }
        }
        return KitchenTicket(
            id = it.id,
            branchId = it.branchId,
            orderId = it.orderId,
            orderNumber = it.orderNumber,
            stationName = it.stationName,
            itemsSummary = it.itemsSummary,
            status = it.status,
            elapsedMinutes = it.elapsedMinutes,
            isPriority = it.isPriority,
            kitchenId = it.kitchenId,
            prepAreaId = it.prepAreaId,
            stationId = it.stationId,
            stationType = runCatching { KitchenStationType.valueOf(it.stationType) }.getOrDefault(KitchenStationType.GRILL),
            orderType = runCatching { OrderType.valueOf(it.orderType) }.getOrDefault(OrderType.DINE_IN),
            channel = runCatching { SalesChannel.valueOf(it.channel) }.getOrDefault(SalesChannel.POS),
            tableId = it.tableId,
            tableNumber = it.tableNumber,
            serverOrCashierName = it.serverOrCashierName,
            customerName = it.customerName,
            priority = runCatching { TicketPriority.valueOf(it.priority) }.getOrDefault(TicketPriority.NORMAL),
            ticketStatus = runCatching { KdsTicketStatus.valueOf(it.ticketStatus) }.getOrDefault(KdsTicketStatus.fromRawStatus(it.status)),
            items = parsedItems,
            orderNotes = it.orderNotes,
            warningThresholdMinutes = it.warningThresholdMinutes,
            overdueThresholdMinutes = it.overdueThresholdMinutes,
            workflowStage = it.workflowStage,
            createdAt = it.createdAt,
            acceptedAt = it.acceptedAt,
            preparingAt = it.preparingAt,
            readyAt = it.readyAt,
            completedAt = it.completedAt,
            assignedChefName = it.assignedChefName
        )
    }
}

enum class NotificationType {
    ORDER,
    ALERT,
    WARNING,
    SYSTEM
}

data class AppNotification(
    val id: String,
    val titleEn: String,
    val titleAr: String,
    val bodyEn: String,
    val bodyAr: String,
    val type: NotificationType,
    val timestamp: Long,
    val isRead: Boolean = false
)
