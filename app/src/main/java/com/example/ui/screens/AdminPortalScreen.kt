package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.Translations
import com.example.designsystem.*
import com.example.domain.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminPortalScreen(
    company: Company,
    brands: List<Brand>,
    branches: List<Branch>,
    users: List<User>,
    roles: List<Role>,
    employees: List<Employee>,
    auditLogs: List<AuditLog>,
    terminals: List<PosTerminal>,
    language: AppLanguage,
    onUpdateCompany: (Company) -> Unit,
    onSaveBrand: (Brand) -> Unit,
    onSaveBranch: (Branch) -> Unit,
    onToggleBranchStatus: (String, Boolean) -> Unit,
    onAddTerminal: (PosTerminal) -> Unit,
    onDeleteTerminal: (String) -> Unit,
    onSaveUser: (User) -> Unit,
    onToggleUserStatus: (String, String) -> Unit,
    onResetPassword: (String, String) -> Unit,
    onSaveRole: (Role) -> Unit,
    onDeleteRole: (String) -> Unit,
    onSaveEmployee: (Employee) -> Unit,
    onUpdateEmployeeStatus: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        TabItem(Translations.get("admin_tab_overview", language)),
        TabItem(Translations.get("admin_tab_company", language)),
        TabItem(Translations.get("admin_tab_brands", language), brands.size),
        TabItem(Translations.get("admin_tab_branches", language), branches.size),
        TabItem(Translations.get("admin_tab_users", language), users.size),
        TabItem(Translations.get("admin_tab_roles", language), roles.size),
        TabItem(Translations.get("admin_tab_employees", language), employees.size)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_portal_screen")
    ) {
        // Module Title and Identity Security Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = Translations.get("nav_admin", language),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Enterprise tenant security, identity management & multi-branch governance",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = RestoEmerald100,
                modifier = Modifier.padding(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = RestoEmerald600, modifier = Modifier.size(16.dp))
                    Text(
                        text = "RBAC Enforced",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = RestoEmerald600
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tabs
        RestoTabs(
            tabs = tabs,
            selectedIndex = selectedTabIndex,
            onTabSelected = { selectedTabIndex = it },
            isScrollable = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Content
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTabIndex) {
                0 -> SecurityOverviewTab(
                    company = company,
                    users = users,
                    roles = roles,
                    branches = branches,
                    auditLogs = auditLogs,
                    language = language
                )
                1 -> CompanySettingsTab(
                    company = company,
                    language = language,
                    onUpdateCompany = onUpdateCompany
                )
                2 -> BrandsTab(
                    brands = brands,
                    language = language,
                    onSaveBrand = onSaveBrand
                )
                3 -> BranchesTab(
                    branches = branches,
                    terminals = terminals,
                    language = language,
                    onSaveBranch = onSaveBranch,
                    onToggleStatus = onToggleBranchStatus,
                    onAddTerminal = onAddTerminal,
                    onDeleteTerminal = onDeleteTerminal
                )
                4 -> UsersTab(
                    users = users,
                    roles = roles,
                    branches = branches,
                    language = language,
                    onSaveUser = onSaveUser,
                    onToggleStatus = onToggleUserStatus,
                    onResetPassword = onResetPassword
                )
                5 -> RolesTab(
                    roles = roles,
                    language = language,
                    onSaveRole = onSaveRole,
                    onDeleteRole = onDeleteRole
                )
                6 -> EmployeesTab(
                    employees = employees,
                    branches = branches,
                    language = language,
                    onSaveEmployee = onSaveEmployee,
                    onUpdateStatus = onUpdateEmployeeStatus
                )
            }
        }
    }
}

// ==========================================
// TAB 0: SECURITY & OVERVIEW
// ==========================================
@Composable
fun SecurityOverviewTab(
    company: Company,
    users: List<User>,
    roles: List<Role>,
    branches: List<Branch>,
    auditLogs: List<AuditLog>,
    language: AppLanguage
) {
    val activeUsersCount = users.count { it.status == "ACTIVE" }
    val lockedAccountsCount = users.count { it.isLocked }
    val securityAudits = auditLogs.filter {
        it.action.startsWith("LOGIN") || it.action.startsWith("USER") || it.action.startsWith("ROLE") || it.action.startsWith("PASSWORD")
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RestoKpiCard(
                    title = "Active User Accounts",
                    value = "$activeUsersCount / ${users.size}",
                    subtitle = "${users.size - activeUsersCount} suspended/inactive",
                    icon = Icons.Default.People,
                    modifier = Modifier.weight(1f)
                )
                RestoKpiCard(
                    title = "Configured Roles",
                    value = "${roles.size}",
                    subtitle = "${roles.count { !it.isSystem }} custom roles",
                    icon = Icons.Default.AdminPanelSettings,
                    modifier = Modifier.weight(1f)
                )
                RestoKpiCard(
                    title = "Locked Accounts",
                    value = "$lockedAccountsCount",
                    subtitle = "threshold: 5 failures",
                    icon = Icons.Default.LockClock,
                    trendPositive = lockedAccountsCount == 0,
                    trendLabel = if (lockedAccountsCount == 0) "Secure" else "Alert",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                text = "Recent Identity & Security Audit Trail",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            val columns = listOf(
                TableColumn<AuditLog>(title = "Timestamp", width = 110.dp) { log ->
                    Text(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp)))
                },
                TableColumn<AuditLog>(title = "Security Event", width = 140.dp) { log ->
                    val variant = when {
                        log.action.contains("FAILED") || log.action.contains("BLOCKED") -> BadgeVariant.DANGER
                        log.action.contains("RESET") -> BadgeVariant.WARNING
                        log.action.contains("SUCCESS") -> BadgeVariant.SUCCESS
                        else -> BadgeVariant.INFO
                    }
                    RestoBadge(text = log.action, variant = variant)
                },
                TableColumn<AuditLog>(title = "Initiating User", width = 130.dp) { log ->
                    Text(log.userName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                },
                TableColumn<AuditLog>(title = "Audit Telemetry", width = 280.dp) { log ->
                    Text(log.details, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            )

            RestoDataTable(
                items = securityAudits.take(20),
                columns = columns,
                minTableWidth = 660.dp
            )
        }
    }
}

// ==========================================
// TAB 1: COMPANY SETTINGS
// ==========================================
@Composable
fun CompanySettingsTab(
    company: Company,
    language: AppLanguage,
    onUpdateCompany: (Company) -> Unit
) {
    var nameEn by remember(company) { mutableStateOf(company.nameEn) }
    var nameAr by remember(company) { mutableStateOf(company.nameAr) }
    var taxNumber by remember(company) { mutableStateOf(company.taxNumber) }
    var currency by remember(company) { mutableStateOf(company.defaultCurrency) }
    var country by remember(company) { mutableStateOf(company.country) }
    var fiscalYear by remember(company) { mutableStateOf(company.fiscalYear) }
    var autoLock by remember(company) { mutableIntStateOf(company.autoLockMinutes) }
    var isSavedToast by remember { mutableStateOf(false) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Enterprise Profile & Fiscal Governance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RestoTextInput(
                            value = nameEn,
                            onValueChange = { nameEn = it },
                            label = "Company Legal Name (English)",
                            modifier = Modifier.weight(1f)
                        )
                        RestoTextInput(
                            value = nameAr,
                            onValueChange = { nameAr = it },
                            label = "اسم المؤسسة الرسمي (العربية)",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RestoTextInput(
                            value = taxNumber,
                            onValueChange = { taxNumber = it },
                            label = "Tax / VAT ID Registration",
                            modifier = Modifier.weight(1f)
                        )
                        RestoTextInput(
                            value = currency,
                            onValueChange = { currency = it },
                            label = "Default System Currency",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RestoTextInput(
                            value = country,
                            onValueChange = { country = it },
                            label = "Country of Incorporation",
                            modifier = Modifier.weight(1f)
                        )
                        RestoTextInput(
                            value = fiscalYear,
                            onValueChange = { fiscalYear = it },
                            label = Translations.get("lbl_fiscal_year", language),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(Translations.get("lbl_auto_lock", language), fontWeight = FontWeight.Bold)
                            Text("Automatic session timeout in minutes", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        RestoNumberStepper(
                            value = autoLock,
                            onValueChange = { autoLock = it },
                            minValue = 5,
                            maxValue = 120
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    RestoButton(
                        onClick = {
                            val updated = company.copy(
                                nameEn = nameEn,
                                nameAr = nameAr,
                                taxNumber = taxNumber,
                                defaultCurrency = currency,
                                country = country,
                                fiscalYear = fiscalYear,
                                autoLockMinutes = autoLock
                            )
                            onUpdateCompany(updated)
                            isSavedToast = true
                        },
                        variant = RestoButtonVariant.PRIMARY,
                        modifier = Modifier.align(Alignment.End),
                        testTag = "save_company_settings_button"
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Translations.get("action_save", language))
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 2: BRANDS MANAGEMENT
// ==========================================
@Composable
fun BrandsTab(
    brands: List<Brand>,
    language: AppLanguage,
    onSaveBrand: (Brand) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingBrand by remember { mutableStateOf<Brand?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Commercial Concepts & Brand Identifiers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            RestoButton(
                onClick = {
                    editingBrand = null
                    showAddDialog = true
                },
                variant = RestoButtonVariant.PRIMARY,
                testTag = "add_brand_button"
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(Translations.get("btn_create_brand", language))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val columns = listOf(
            TableColumn<Brand>(title = "Code", width = 120.dp) { b ->
                Text(b.code, fontWeight = FontWeight.Bold)
            },
            TableColumn<Brand>(title = "Brand Name", width = 200.dp) { b ->
                Column {
                    Text(if (language == AppLanguage.ARABIC) b.nameAr else b.nameEn, fontWeight = FontWeight.Bold)
                    Text(b.menuIdentity, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            TableColumn<Brand>(title = "Brand Palette", width = 120.dp) { b ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(b.primaryColor))))
                    Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(b.secondaryColor))))
                }
            },
            TableColumn<Brand>(title = "Status", width = 100.dp) { b ->
                RestoBadge(
                    text = if (b.isActive) "ACTIVE" else "INACTIVE",
                    variant = if (b.isActive) BadgeVariant.SUCCESS else BadgeVariant.NEUTRAL
                )
            },
            TableColumn<Brand>(title = "Actions", width = 110.dp) { b ->
                IconButton(
                    onClick = {
                        editingBrand = b
                        showAddDialog = true
                    }
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
            }
        )

        RestoDataTable(
            items = brands,
            columns = columns,
            minTableWidth = 650.dp
        )
    }

    if (showAddDialog) {
        BrandDialog(
            brand = editingBrand,
            language = language,
            onSave = { brand ->
                onSaveBrand(brand)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
fun BrandDialog(
    brand: Brand?,
    language: AppLanguage,
    onSave: (Brand) -> Unit,
    onDismiss: () -> Unit
) {
    var code by remember { mutableStateOf(brand?.code ?: "") }
    var nameEn by remember { mutableStateOf(brand?.nameEn ?: "") }
    var nameAr by remember { mutableStateOf(brand?.nameAr ?: "") }
    var menuIdentity by remember { mutableStateOf(brand?.menuIdentity ?: "Gourmet Dining") }
    var primaryColor by remember { mutableStateOf(brand?.primaryColor ?: "#F59E0B") }
    var isActive by remember { mutableStateOf(brand?.isActive ?: true) }

    RestoModalDialog(
        onDismissRequest = onDismiss,
        title = if (brand == null) Translations.get("btn_create_brand", language) else "Edit Brand: ${brand.code}"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RestoTextInput(value = code, onValueChange = { code = it }, label = "Brand Code (e.g. BURGER_CRAFT)")
            RestoTextInput(value = nameEn, onValueChange = { nameEn = it }, label = "Brand Name (English)")
            RestoTextInput(value = nameAr, onValueChange = { nameAr = it }, label = "اسم العلامة التجارية (العربية)")
            RestoTextInput(value = menuIdentity, onValueChange = { menuIdentity = it }, label = "Menu Concept / Identity")
            RestoTextInput(value = primaryColor, onValueChange = { primaryColor = it }, label = "Primary Color Hex (e.g. #F59E0B)")

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Brand Active Status", fontWeight = FontWeight.Bold)
                Switch(checked = isActive, onCheckedChange = { isActive = it })
            }

            Spacer(modifier = Modifier.height(10.dp))

            RestoButton(
                onClick = {
                    val b = Brand(
                        id = brand?.id ?: "brand_${UUID.randomUUID().toString().take(6)}",
                        companyId = "comp_01",
                        code = code.ifBlank { "BRAND" },
                        nameEn = nameEn.ifBlank { "New Brand" },
                        nameAr = nameAr.ifBlank { "علامة تجارية جديدة" },
                        menuIdentity = menuIdentity,
                        primaryColor = primaryColor,
                        isActive = isActive
                    )
                    onSave(b)
                },
                variant = RestoButtonVariant.PRIMARY,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(Translations.get("action_save", language))
            }
        }
    }
}

// ==========================================
// TAB 3: BRANCHES & POS TERMINALS
// ==========================================
@Composable
fun BranchesTab(
    branches: List<Branch>,
    terminals: List<PosTerminal>,
    language: AppLanguage,
    onSaveBranch: (Branch) -> Unit,
    onToggleStatus: (String, Boolean) -> Unit,
    onAddTerminal: (PosTerminal) -> Unit,
    onDeleteTerminal: (String) -> Unit
) {
    var showBranchDialog by remember { mutableStateOf(false) }
    var editingBranch by remember { mutableStateOf<Branch?>(null) }
    var showTerminalDialog by remember { mutableStateOf<Branch?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Operational Branches & Hardware Tills",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            RestoButton(
                onClick = {
                    editingBranch = null
                    showBranchDialog = true
                },
                variant = RestoButtonVariant.PRIMARY,
                testTag = "add_branch_button"
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(Translations.get("btn_create_branch", language))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val columns = listOf(
            TableColumn<Branch>(title = "Branch Code", width = 130.dp) { b ->
                Column {
                    Text(b.code, fontWeight = FontWeight.Bold)
                    Text(b.timezone, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            TableColumn<Branch>(title = "Name & Address", width = 200.dp) { b ->
                Column {
                    Text(if (language == AppLanguage.ARABIC) b.nameAr else b.nameEn, fontWeight = FontWeight.Bold)
                    Text(b.address, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            TableColumn<Branch>(title = "Operating Hours", width = 130.dp) { b ->
                Text("${b.openingTime} - ${b.closingTime}", fontSize = 12.sp)
            },
            TableColumn<Branch>(title = "Tax / VAT", width = 100.dp) { b ->
                Text("${(b.taxRate * 100).toInt()}% ${if (b.taxInclusive) "(Inc)" else "(Ex)"}", fontSize = 12.sp)
            },
            TableColumn<Branch>(title = "Status", width = 100.dp) { b ->
                RestoBadge(
                    text = if (b.isActive) "ACTIVE" else "INACTIVE",
                    variant = if (b.isActive) BadgeVariant.SUCCESS else BadgeVariant.NEUTRAL
                )
            },
            TableColumn<Branch>(title = "Actions", width = 140.dp) { b ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { editingBranch = b; showBranchDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showTerminalDialog = b }) {
                        Icon(Icons.Default.PointOfSale, contentDescription = "Terminals", tint = RestoAmber600)
                    }
                }
            }
        )

        RestoDataTable(
            items = branches,
            columns = columns,
            minTableWidth = 800.dp
        )
    }

    if (showBranchDialog) {
        BranchDialog(
            branch = editingBranch,
            language = language,
            onSave = {
                onSaveBranch(it)
                showBranchDialog = false
            },
            onDismiss = { showBranchDialog = false }
        )
    }

    if (showTerminalDialog != null) {
        val branch = showTerminalDialog!!
        val branchTerminals = terminals.filter { it.branchId == branch.id }
        BranchTerminalsDialog(
            branch = branch,
            terminals = branchTerminals,
            language = language,
            onAddTerminal = onAddTerminal,
            onDeleteTerminal = onDeleteTerminal,
            onDismiss = { showTerminalDialog = null }
        )
    }
}

@Composable
fun BranchDialog(
    branch: Branch?,
    language: AppLanguage,
    onSave: (Branch) -> Unit,
    onDismiss: () -> Unit
) {
    var code by remember { mutableStateOf(branch?.code ?: "") }
    var nameEn by remember { mutableStateOf(branch?.nameEn ?: "") }
    var nameAr by remember { mutableStateOf(branch?.nameAr ?: "") }
    var address by remember { mutableStateOf(branch?.address ?: "") }
    var phone by remember { mutableStateOf(branch?.phone ?: "+966 11 000 0000") }
    var openingTime by remember { mutableStateOf(branch?.openingTime ?: "10:00") }
    var closingTime by remember { mutableStateOf(branch?.closingTime ?: "01:00") }
    var timezone by remember { mutableStateOf(branch?.timezone ?: "Asia/Riyadh (GMT+3)") }
    var taxRate by remember { mutableDoubleStateOf(branch?.taxRate ?: 0.10) }
    var taxInclusive by remember { mutableStateOf(branch?.taxInclusive ?: false) }
    var capacity by remember { mutableIntStateOf(branch?.seatingCapacity ?: 80) }
    var isActive by remember { mutableStateOf(branch?.isActive ?: true) }

    RestoModalDialog(
        onDismissRequest = onDismiss,
        title = if (branch == null) Translations.get("btn_create_branch", language) else "Edit Branch: ${branch.code}"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RestoTextInput(value = code, onValueChange = { code = it }, label = "Code", modifier = Modifier.weight(1f))
                RestoTextInput(value = phone, onValueChange = { phone = it }, label = "Phone", modifier = Modifier.weight(1f))
            }
            RestoTextInput(value = nameEn, onValueChange = { nameEn = it }, label = "Branch Name (English)")
            RestoTextInput(value = nameAr, onValueChange = { nameAr = it }, label = "اسم الفرع (العربية)")
            RestoTextInput(value = address, onValueChange = { address = it }, label = "Street & Building Address")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RestoTextInput(value = openingTime, onValueChange = { openingTime = it }, label = "Opening Time", modifier = Modifier.weight(1f))
                RestoTextInput(value = closingTime, onValueChange = { closingTime = it }, label = "Closing Time", modifier = Modifier.weight(1f))
            }

            RestoTextInput(value = timezone, onValueChange = { timezone = it }, label = Translations.get("lbl_timezone", language))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Tax Inclusive In Menu Prices", fontSize = 13.sp)
                Switch(checked = taxInclusive, onCheckedChange = { taxInclusive = it })
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Branch Active Status", fontSize = 13.sp)
                Switch(checked = isActive, onCheckedChange = { isActive = it })
            }

            RestoButton(
                onClick = {
                    val b = Branch(
                        id = branch?.id ?: "branch_${UUID.randomUUID().toString().take(6)}",
                        brandId = "brand_01",
                        code = code.ifBlank { "BR-NEW" },
                        nameEn = nameEn.ifBlank { "New Branch" },
                        nameAr = nameAr.ifBlank { "فرع جديد" },
                        phone = phone,
                        address = address.ifBlank { "Main Street" },
                        seatingCapacity = capacity,
                        isActive = isActive,
                        openingTime = openingTime,
                        closingTime = closingTime,
                        timezone = timezone,
                        taxRate = taxRate,
                        taxInclusive = taxInclusive
                    )
                    onSave(b)
                },
                variant = RestoButtonVariant.PRIMARY,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(Translations.get("action_save", language))
            }
        }
    }
}

@Composable
fun BranchTerminalsDialog(
    branch: Branch,
    terminals: List<PosTerminal>,
    language: AppLanguage,
    onAddTerminal: (PosTerminal) -> Unit,
    onDeleteTerminal: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newTermCode by remember { mutableStateOf("") }
    var newTermName by remember { mutableStateOf("") }
    var newTermIp by remember { mutableStateOf("192.168.1.105") }

    RestoModalDialog(
        onDismissRequest = onDismiss,
        title = "POS Terminals for ${branch.code}"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "${terminals.size} hardware till devices registered for this branch",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(terminals) { t ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(t.terminalCode, fontWeight = FontWeight.Bold)
                                    RestoBadge(text = t.status, variant = if (t.status == "ONLINE") BadgeVariant.SUCCESS else BadgeVariant.NEUTRAL)
                                }
                                Text("${t.name} • IP: ${t.ipAddress}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { onDeleteTerminal(t.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RestoRose600)
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            Text("Register New Till:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RestoTextInput(value = newTermCode, onValueChange = { newTermCode = it }, label = "Code (e.g. POS-04)", modifier = Modifier.weight(1f))
                RestoTextInput(value = newTermName, onValueChange = { newTermName = it }, label = "Register Name", modifier = Modifier.weight(1f))
            }
            RestoTextInput(value = newTermIp, onValueChange = { newTermIp = it }, label = "Static IP Address")

            RestoButton(
                onClick = {
                    if (newTermCode.isNotBlank()) {
                        val term = PosTerminal(
                            id = "term_${UUID.randomUUID().toString().take(6)}",
                            branchId = branch.id,
                            terminalCode = newTermCode,
                            name = newTermName.ifBlank { "Counter Till" },
                            status = "ONLINE",
                            ipAddress = newTermIp
                        )
                        onAddTerminal(term)
                        newTermCode = ""
                        newTermName = ""
                    }
                },
                variant = RestoButtonVariant.SECONDARY,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(Translations.get("btn_add_terminal", language))
            }
        }
    }
}

// ==========================================
// TAB 4: USERS & CREDENTIALS
// ==========================================
@Composable
fun UsersTab(
    users: List<User>,
    roles: List<Role>,
    branches: List<Branch>,
    language: AppLanguage,
    onSaveUser: (User) -> Unit,
    onToggleStatus: (String, String) -> Unit,
    onResetPassword: (String, String) -> Unit
) {
    var showUserDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<User?>(null) }
    var resetPwdUser by remember { mutableStateOf<User?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "System Operator Accounts & Credentials",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            RestoButton(
                onClick = {
                    editingUser = null
                    showUserDialog = true
                },
                variant = RestoButtonVariant.PRIMARY,
                testTag = "add_user_button"
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(Translations.get("btn_create_user", language))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val columns = listOf(
            TableColumn<User>(title = "User", width = 160.dp) { u ->
                Column {
                    Text(u.fullName, fontWeight = FontWeight.Bold)
                    Text("@${u.username}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            TableColumn<User>(title = "Assigned Role", width = 140.dp) { u ->
                val r = roles.find { it.code == u.roleId }
                RestoBadge(text = r?.nameEn ?: u.roleId, variant = BadgeVariant.INFO)
            },
            TableColumn<User>(title = "Branches Scope", width = 140.dp) { u ->
                Text("${u.allowedBranchIds.size} Branches", fontSize = 12.sp)
            },
            TableColumn<User>(title = "Status & Lock", width = 130.dp) { u ->
                Column {
                    val statusVariant = when (u.status) {
                        "ACTIVE" -> BadgeVariant.SUCCESS
                        "SUSPENDED" -> BadgeVariant.DANGER
                        else -> BadgeVariant.WARNING
                    }
                    RestoBadge(text = u.status, variant = statusVariant)
                    if (u.isLocked) {
                        Text("LOCKED (${u.failedAttempts} fails)", color = RestoRose600, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            TableColumn<User>(title = "Actions", width = 150.dp) { u ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { editingUser = u; showUserDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { resetPwdUser = u }) {
                        Icon(Icons.Default.Key, contentDescription = "Reset Pwd", tint = RestoAmber600)
                    }
                    IconButton(
                        onClick = {
                            val next = if (u.status == "ACTIVE") "SUSPENDED" else "ACTIVE"
                            onToggleStatus(u.id, next)
                        }
                    ) {
                        Icon(
                            if (u.status == "ACTIVE") Icons.Default.Block else Icons.Default.CheckCircle,
                            contentDescription = "Toggle",
                            tint = if (u.status == "ACTIVE") RestoRose600 else RestoEmerald600
                        )
                    }
                }
            }
        )

        RestoDataTable(
            items = users,
            columns = columns,
            minTableWidth = 720.dp
        )
    }

    if (showUserDialog) {
        UserDialog(
            user = editingUser,
            roles = roles,
            branches = branches,
            language = language,
            onSave = {
                onSaveUser(it)
                showUserDialog = false
            },
            onDismiss = { showUserDialog = false }
        )
    }

    if (resetPwdUser != null) {
        val u = resetPwdUser!!
        ResetPasswordDialog(
            user = u,
            language = language,
            onConfirmReset = { newPwd ->
                onResetPassword(u.id, newPwd)
                resetPwdUser = null
            },
            onDismiss = { resetPwdUser = null }
        )
    }
}

@Composable
fun UserDialog(
    user: User?,
    roles: List<Role>,
    branches: List<Branch>,
    language: AppLanguage,
    onSave: (User) -> Unit,
    onDismiss: () -> Unit
) {
    var username by remember { mutableStateOf(user?.username ?: "") }
    var fullName by remember { mutableStateOf(user?.fullName ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var selectedRole by remember { mutableStateOf(roles.find { it.code == user?.roleId } ?: roles.first()) }
    var selectedBranchIds by remember { mutableStateOf(user?.allowedBranchIds?.toSet() ?: branches.map { it.id }.toSet()) }
    var status by remember { mutableStateOf(user?.status ?: "ACTIVE") }

    RestoModalDialog(
        onDismissRequest = onDismiss,
        title = if (user == null) Translations.get("btn_create_user", language) else "Edit User: ${user.username}"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RestoTextInput(value = username, onValueChange = { username = it }, label = "Username", modifier = Modifier.weight(1f))
                RestoTextInput(value = fullName, onValueChange = { fullName = it }, label = "Full Name", modifier = Modifier.weight(1f))
            }
            RestoTextInput(value = email, onValueChange = { email = it }, label = "Work Email")

            RestoDropdown(
                items = roles,
                selectedItem = selectedRole,
                onItemSelected = { selectedRole = it },
                label = Translations.get("role_select", language),
                itemLabel = { it.nameEn }
            )

            Text("Authorized Branch Access Scope:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            branches.forEach { b ->
                val isChecked = selectedBranchIds.contains(b.id)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable {
                        selectedBranchIds = if (isChecked) selectedBranchIds - b.id else selectedBranchIds + b.id
                    }
                ) {
                    Checkbox(checked = isChecked, onCheckedChange = { chk ->
                        selectedBranchIds = if (chk) selectedBranchIds + b.id else selectedBranchIds - b.id
                    })
                    Text(b.nameEn, fontSize = 12.sp)
                }
            }

            RestoButton(
                onClick = {
                    val u = User(
                        id = user?.id ?: "user_${UUID.randomUUID().toString().take(6)}",
                        companyId = "comp_01",
                        username = username.ifBlank { "user_new" },
                        fullName = fullName.ifBlank { "Operator" },
                        email = email.ifBlank { "ops@alhargroup.com" },
                        roleId = selectedRole.code,
                        activeBranchId = selectedBranchIds.firstOrNull() ?: "branch_01",
                        allowedBranchIds = selectedBranchIds.toList(),
                        status = status
                    )
                    onSave(u)
                },
                variant = RestoButtonVariant.PRIMARY,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(Translations.get("action_save", language))
            }
        }
    }
}

@Composable
fun ResetPasswordDialog(
    user: User,
    language: AppLanguage,
    onConfirmReset: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newPassword by remember { mutableStateOf("Resto2026!#") }

    RestoModalDialog(
        onDismissRequest = onDismiss,
        title = "Reset Credentials for ${user.fullName}"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Generate a new secure temporary password for ${user.username}. This will automatically clear any security lockouts and reset failed login attempts to 0.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            RestoTextInput(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = "New Password",
                leadingIcon = Icons.Default.VpnKey
            )

            RestoButton(
                onClick = { onConfirmReset(newPassword) },
                variant = RestoButtonVariant.PRIMARY,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Confirm Password Reset")
            }
        }
    }
}

// ==========================================
// TAB 5: ROLES & PERMISSIONS
// ==========================================
@Composable
fun RolesTab(
    roles: List<Role>,
    language: AppLanguage,
    onSaveRole: (Role) -> Unit,
    onDeleteRole: (String) -> Unit
) {
    var showRoleDialog by remember { mutableStateOf(false) }
    var editingRole by remember { mutableStateOf<Role?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Role Based Access Control (RBAC) Matrix",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            RestoButton(
                onClick = {
                    editingRole = null
                    showRoleDialog = true
                },
                variant = RestoButtonVariant.PRIMARY,
                testTag = "add_role_button"
            ) {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(Translations.get("btn_create_role", language))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val columns = listOf(
            TableColumn<Role>(title = "Role Code", width = 150.dp) { r ->
                Column {
                    Text(r.code, fontWeight = FontWeight.Bold)
                    Text(if (r.isSystem) "System Default" else "Custom Defined", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                }
            },
            TableColumn<Role>(title = "Title & Description", width = 220.dp) { r ->
                Column {
                    Text(if (language == AppLanguage.ARABIC) r.nameAr else r.nameEn, fontWeight = FontWeight.Bold)
                    Text(r.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            TableColumn<Role>(title = "Permissions", width = 120.dp) { r ->
                Text(if (r.permissions.contains("*")) "Full (*)" else "${r.permissions.size} Grants", fontWeight = FontWeight.Bold)
            },
            TableColumn<Role>(title = "Branch Scope", width = 120.dp) { r ->
                RestoBadge(
                    text = if (r.branchRestricted) "BRANCH RESTRICTED" else "GLOBAL MULTI-BRANCH",
                    variant = if (r.branchRestricted) BadgeVariant.WARNING else BadgeVariant.INFO
                )
            },
            TableColumn<Role>(title = "Actions", width = 110.dp) { r ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { editingRole = r; showRoleDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    if (!r.isSystem) {
                        IconButton(onClick = { onDeleteRole(r.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RestoRose600)
                        }
                    }
                }
            }
        )

        RestoDataTable(
            items = roles,
            columns = columns,
            minTableWidth = 720.dp
        )
    }

    if (showRoleDialog) {
        RoleDialog(
            role = editingRole,
            language = language,
            onSave = {
                onSaveRole(it)
                showRoleDialog = false
            },
            onDismiss = { showRoleDialog = false }
        )
    }
}

val ALL_SAMPLE_PERMISSIONS = listOf(
    "ADMINISTRATION:COMPANY_SETTINGS:VIEW",
    "ADMINISTRATION:COMPANY_SETTINGS:EDIT",
    "ADMINISTRATION:BRANCH_MANAGEMENT:VIEW",
    "ADMINISTRATION:BRANCH_MANAGEMENT:CREATE",
    "ADMINISTRATION:BRANCH_MANAGEMENT:EDIT",
    "ADMINISTRATION:USER_MANAGEMENT:VIEW",
    "ADMINISTRATION:USER_MANAGEMENT:CREATE",
    "ADMINISTRATION:USER_MANAGEMENT:EDIT",
    "ADMINISTRATION:ROLE_MANAGEMENT:VIEW",
    "ADMINISTRATION:ROLE_MANAGEMENT:CREATE",
    "POS:POS_TERMINAL:VIEW",
    "POS:POS_TERMINAL:CREATE",
    "POS:POS_TERMINAL:APPROVE",
    "POS:POS_TERMINAL:REFUND",
    "POS:POS_TERMINAL:DISCOUNT",
    "TABLES:FLOOR_TABLES:VIEW",
    "TABLES:FLOOR_TABLES:EDIT",
    "KITCHEN:KDS_SCREEN:VIEW",
    "KITCHEN:KDS_SCREEN:EDIT",
    "INVENTORY:WAREHOUSE_STOCK:VIEW",
    "INVENTORY:WAREHOUSE_STOCK:EDIT",
    "REPORTS:AUDIT_TRAIL:VIEW",
    "REPORTS:AUDIT_TRAIL:EXPORT"
)

@Composable
fun RoleDialog(
    role: Role?,
    language: AppLanguage,
    onSave: (Role) -> Unit,
    onDismiss: () -> Unit
) {
    var code by remember { mutableStateOf(role?.code ?: "ROLE_") }
    var nameEn by remember { mutableStateOf(role?.nameEn ?: "") }
    var nameAr by remember { mutableStateOf(role?.nameAr ?: "") }
    var description by remember { mutableStateOf(role?.description ?: "") }
    var branchRestricted by remember { mutableStateOf(role?.branchRestricted ?: true) }
    var selectedPermissions by remember {
        mutableStateOf(role?.permissions?.toSet() ?: setOf("POS:POS_TERMINAL:VIEW", "TABLES:FLOOR_TABLES:VIEW"))
    }

    RestoModalDialog(
        onDismissRequest = onDismiss,
        title = if (role == null) Translations.get("btn_create_role", language) else "Edit Role: ${role.code}"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RestoTextInput(value = code, onValueChange = { code = it }, label = "Role Identifier Code")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RestoTextInput(value = nameEn, onValueChange = { nameEn = it }, label = "Title (English)", modifier = Modifier.weight(1f))
                RestoTextInput(value = nameAr, onValueChange = { nameAr = it }, label = "المسمى (العربية)", modifier = Modifier.weight(1f))
            }
            RestoTextInput(value = description, onValueChange = { description = it }, label = "Role Scope & Description")

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Enforce Branch Restriction", fontSize = 13.sp)
                Switch(checked = branchRestricted, onCheckedChange = { branchRestricted = it })
            }

            Text("Select Permissions Matrix:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(ALL_SAMPLE_PERMISSIONS) { perm ->
                    val isChecked = selectedPermissions.contains(perm) || selectedPermissions.contains("*")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectedPermissions = if (isChecked) selectedPermissions - perm else selectedPermissions + perm
                        }
                    ) {
                        Checkbox(checked = isChecked, onCheckedChange = { chk ->
                            selectedPermissions = if (chk) selectedPermissions + perm else selectedPermissions - perm
                        })
                        Text(perm, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                }
            }

            RestoButton(
                onClick = {
                    val r = Role(
                        id = role?.id ?: "role_${UUID.randomUUID().toString().take(6)}",
                        code = code.ifBlank { "ROLE_CUSTOM" },
                        nameEn = nameEn.ifBlank { "Custom Role" },
                        nameAr = nameAr.ifBlank { "دور مخصص" },
                        permissions = selectedPermissions.toList(),
                        isSystem = role?.isSystem ?: false,
                        branchRestricted = branchRestricted,
                        description = description
                    )
                    onSave(r)
                },
                variant = RestoButtonVariant.PRIMARY,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(Translations.get("action_save", language))
            }
        }
    }
}

// ==========================================
// TAB 6: EMPLOYEES & HR
// ==========================================
@Composable
fun EmployeesTab(
    employees: List<Employee>,
    branches: List<Branch>,
    language: AppLanguage,
    onSaveEmployee: (Employee) -> Unit,
    onUpdateStatus: (String, String) -> Unit
) {
    var showEmpDialog by remember { mutableStateOf(false) }
    var editingEmp by remember { mutableStateOf<Employee?>(null) }
    var departmentFilter by remember { mutableStateOf("ALL") }

    val departments = listOf("ALL", "Management", "Operations", "Kitchen", "Front of House", "Delivery")
    val filteredEmployees = if (departmentFilter == "ALL") employees else employees.filter { it.department == departmentFilter }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Staff Personnel & Operational Roster",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            RestoButton(
                onClick = {
                    editingEmp = null
                    showEmpDialog = true
                },
                variant = RestoButtonVariant.PRIMARY,
                testTag = "add_employee_button"
            ) {
                Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(Translations.get("btn_create_employee", language))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Department Filter Chips
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            departments.forEach { dept ->
                RestoFilterChip(
                    selected = departmentFilter == dept,
                    onClick = { departmentFilter = dept },
                    label = dept
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val columns = listOf(
            TableColumn<Employee>(title = "Emp #", width = 100.dp) { e ->
                Text(e.employeeNumber, fontWeight = FontWeight.Bold)
            },
            TableColumn<Employee>(title = "Full Name", width = 180.dp) { e ->
                Column {
                    Text(e.fullName, fontWeight = FontWeight.Bold)
                    Text(e.email, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            TableColumn<Employee>(title = "Department & Title", width = 180.dp) { e ->
                Column {
                    Text(e.jobTitle, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    RestoBadge(text = e.department, variant = BadgeVariant.NEUTRAL)
                }
            },
            TableColumn<Employee>(title = "Status", width = 110.dp) { e ->
                val variant = when (e.status) {
                    "ACTIVE" -> BadgeVariant.SUCCESS
                    "ON_LEAVE" -> BadgeVariant.WARNING
                    else -> BadgeVariant.DANGER
                }
                RestoBadge(text = e.status, variant = variant)
            },
            TableColumn<Employee>(title = "Actions", width = 110.dp) { e ->
                IconButton(onClick = { editingEmp = e; showEmpDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
            }
        )

        RestoDataTable(
            items = filteredEmployees,
            columns = columns,
            minTableWidth = 680.dp
        )
    }

    if (showEmpDialog) {
        EmployeeDialog(
            employee = editingEmp,
            branches = branches,
            language = language,
            onSave = {
                onSaveEmployee(it)
                showEmpDialog = false
            },
            onDismiss = { showEmpDialog = false }
        )
    }
}

@Composable
fun EmployeeDialog(
    employee: Employee?,
    branches: List<Branch>,
    language: AppLanguage,
    onSave: (Employee) -> Unit,
    onDismiss: () -> Unit
) {
    var empNumber by remember { mutableStateOf(employee?.employeeNumber ?: "EMP-${(1000..9999).random()}") }
    var fullName by remember { mutableStateOf(employee?.fullName ?: "") }
    var jobTitle by remember { mutableStateOf(employee?.jobTitle ?: "") }
    var department by remember { mutableStateOf(employee?.department ?: "Operations") }
    var phone by remember { mutableStateOf(employee?.phone ?: "+966 50 000 0000") }
    var email by remember { mutableStateOf(employee?.email ?: "staff@alhargroup.com") }
    var hourlyRate by remember { mutableDoubleStateOf(employee?.hourlyRate ?: 25.0) }
    var selectedBranch by remember { mutableStateOf(branches.find { it.id == employee?.branchId } ?: branches.first()) }
    var status by remember { mutableStateOf(employee?.status ?: "ACTIVE") }

    RestoModalDialog(
        onDismissRequest = onDismiss,
        title = if (employee == null) Translations.get("btn_create_employee", language) else "Edit Employee: ${employee.fullName}"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RestoTextInput(value = empNumber, onValueChange = { empNumber = it }, label = "Employee #", modifier = Modifier.weight(1f))
                RestoTextInput(value = fullName, onValueChange = { fullName = it }, label = "Full Name", modifier = Modifier.weight(1f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RestoTextInput(value = jobTitle, onValueChange = { jobTitle = it }, label = "Job Title", modifier = Modifier.weight(1f))
                RestoTextInput(value = department, onValueChange = { department = it }, label = "Department", modifier = Modifier.weight(1f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RestoTextInput(value = phone, onValueChange = { phone = it }, label = "Phone", modifier = Modifier.weight(1f))
                RestoTextInput(value = email, onValueChange = { email = it }, label = "Email", modifier = Modifier.weight(1f))
            }

            RestoDropdown(
                items = branches,
                selectedItem = selectedBranch,
                onItemSelected = { selectedBranch = it },
                label = "Assigned Branch",
                itemLabel = { it.nameEn }
            )

            RestoButton(
                onClick = {
                    val emp = Employee(
                        id = employee?.id ?: "emp_${UUID.randomUUID().toString().take(6)}",
                        branchId = selectedBranch.id,
                        userId = employee?.userId ?: "user_01",
                        employeeNumber = empNumber,
                        fullName = fullName.ifBlank { "Staff Member" },
                        jobTitle = jobTitle.ifBlank { "Crew Member" },
                        department = department,
                        phone = phone,
                        email = email,
                        hourlyRate = hourlyRate,
                        status = status
                    )
                    onSave(emp)
                },
                variant = RestoButtonVariant.PRIMARY,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(Translations.get("action_save", language))
            }
        }
    }
}
