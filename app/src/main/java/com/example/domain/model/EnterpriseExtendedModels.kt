package com.example.domain.model

// =========================================================================================
// PHASE 7: CUSTOMER ORDERING PLATFORM, CRM, LOYALTY, COUPONS, REVIEWS & COMPLAINTS
// =========================================================================================

data class CustomerAddress(
    val id: String,
    val customerId: String,
    val label: String, // Home, Office, Villa
    val streetAddress: String,
    val buildingAndFloor: String = "",
    val city: String = "Riyadh",
    val deliveryZoneId: String = "dzone_01",
    val deliveryNotes: String = "",
    val isDefault: Boolean = false
)

enum class LoyaltyTierLevel(
    val code: String,
    val titleEn: String,
    val titleAr: String,
    val minPoints: Int,
    val earnMultiplier: Double,
    val perksEn: String,
    val perksAr: String
) {
    BRONZE("BRONZE", "Bronze Member", "عضوية برونزية", 0, 1.0, "1 pt per $1 spent", "نقطة لكل دولار"),
    SILVER("SILVER", "Silver Member", "عضوية فضية", 300, 1.25, "1.25x points + Priority support", "1.25x نقاط + دعم أولوية"),
    GOLD("GOLD", "Gold Member", "عضوية ذهبية", 800, 1.5, "1.5x points + Free delivery on $25+", "1.5x نقاط + توصيل مجاني"),
    PLATINUM("PLATINUM", "Platinum VIP", "عضوية بلاتينية VIP", 1800, 2.0, "2x points + Exclusive chef tastings & VIP table priority", "2x نقاط + أولوية حجز VIP")
}

data class LoyaltyReward(
    val id: String,
    val titleEn: String,
    val titleAr: String,
    val pointsCost: Int,
    val discountValue: Double,
    val freeProductId: String? = null,
    val minTierRequired: String = "BRONZE",
    val expiryDaysAfterRedeem: Int = 30,
    val isActive: Boolean = true
)

data class CustomerCrmMetrics(
    val customerId: String,
    val customerName: String,
    val phone: String,
    val tier: String,
    val orderCount: Int,
    val totalSpend: Double,
    val averageOrderValue: Double,
    val favoriteProductNames: List<String>,
    val lastOrderNumber: String?,
    val lastOrderTimestamp: Long?,
    val loyaltyPointsBalance: Int,
    val pointsExpiringSoon: Int = 0,
    val expiryRuleDescription: String = "Points valid for 12 months from last qualifying purchase"
)

enum class ReviewModerationStatus(val titleEn: String, val titleAr: String) {
    PENDING("Pending Moderation", "بانتظار المراجعة"),
    APPROVED("Published", "منشور"),
    HIDDEN("Hidden / Flagged", "مخفي")
}

data class CustomerReview(
    val id: String,
    val orderId: String,
    val orderNumber: String,
    val productId: String? = null,
    val productNameEn: String = "Overall Order Experience",
    val customerId: String,
    val customerName: String,
    val rating: Int, // 1..5
    val comment: String,
    val moderationStatus: ReviewModerationStatus = ReviewModerationStatus.APPROVED,
    val managerReply: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ComplaintPriority(val titleEn: String, val titleAr: String) {
    LOW("Low", "منخفضة"),
    MEDIUM("Medium", "متوسطة"),
    HIGH("High", "عالية"),
    CRITICAL("Critical / Food Safety", "حرجة / سلامة غذائية")
}

enum class ComplaintStatus(val titleEn: String, val titleAr: String) {
    OPEN("Open", "مفتوحة"),
    IN_REVIEW("In Review", "قيد المعالجة"),
    RESOLVED("Resolved", "تم الحل"),
    CLOSED("Closed", "مغلقة")
}

data class CustomerComplaint(
    val id: String,
    val ticketNumber: String,
    val orderId: String?,
    val orderNumber: String?,
    val branchId: String,
    val customerId: String,
    val customerName: String,
    val category: String, // Late Delivery, Missing Item, Food Quality, Billing, Service
    val priority: ComplaintPriority = ComplaintPriority.MEDIUM,
    val status: ComplaintStatus = ComplaintStatus.OPEN,
    val subject: String,
    val description: String,
    val managementResponse: String = "",
    val compensationOffered: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null
)

// =========================================================================================
// PHASE 8: DELIVERY MANAGEMENT, ZONES, DRIVER APP, DISPATCH & CASH SETTLEMENT
// =========================================================================================

data class DeliveryZone(
    val id: String,
    val branchId: String,
    val code: String,
    val nameEn: String,
    val nameAr: String,
    val boundaryDescription: String, // e.g. "Olaya & Sulaimaniyah (0 - 5 km radius)"
    val radiusKm: Double,
    val deliveryFee: Double,
    val minimumOrderAmount: Double,
    val estimatedMinutes: Int,
    val isActive: Boolean = true
)

enum class DeliveryWorkflowStatus(
    val code: String,
    val titleEn: String,
    val titleAr: String,
    val stepOrder: Int
) {
    READY("READY", "Kitchen Ready", "جاهز في المطبخ", 1),
    AWAITING_DRIVER("AWAITING_DRIVER", "Awaiting Driver", "بانتظار تعيين سائق", 2),
    ASSIGNED("ASSIGNED", "Driver Assigned", "تم تعيين السائق", 3),
    ACCEPTED("ACCEPTED", "Accepted by Driver", "قبل السائق المهمة", 4),
    ARRIVED_RESTAURANT("ARRIVED_RESTAURANT", "Arrived at Branch", "وصل السائق للفرع", 5),
    PICKED_UP("PICKED_UP", "Picked Up", "تم استلام الطلب", 6),
    ON_ROUTE("ON_ROUTE", "On Route to Customer", "في الطريق للعميل", 7),
    DELIVERED("DELIVERED", "Delivered", "تم التوصيل بنجاح", 8),
    FAILED_DELIVERY("FAILED_DELIVERY", "Failed Delivery", "تعذر التوصيل", 9),
    RETURNED_TO_BRANCH("RETURNED_TO_BRANCH", "Returned to Branch", "تم الإرجاع للفرع", 10)
}

data class DeliveryAssignment(
    val id: String,
    val orderId: String,
    val orderNumber: String,
    val branchId: String,
    val deliveryZoneId: String,
    val deliveryZoneName: String,
    val driverId: String?,
    val driverName: String?,
    val driverPhone: String?,
    val vehicleInfo: String = "Motorcycle • RYD-4821",
    val customerName: String,
    val customerPhone: String,
    val deliveryAddress: String,
    val deliveryNotes: String = "",
    val status: DeliveryWorkflowStatus = DeliveryWorkflowStatus.AWAITING_DRIVER,
    val orderTotalAmount: Double,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val isCashOnDelivery: Boolean = true,
    val cashCollectedAmount: Double = 0.0,
    val cashSettledWithBranch: Boolean = false,
    val customerOtpCode: String = "4829",
    val otpVerified: Boolean = false,
    val proofOfDeliveryNote: String = "",
    val failureReason: String? = null,
    val retryCount: Int = 0,
    val estimatedDeliveryMinutes: Int = 35,
    val dispatchedAt: Long = System.currentTimeMillis(),
    val acceptedAt: Long? = null,
    val pickedUpAt: Long? = null,
    val deliveredAt: Long? = null,
    val consentedLat: Double? = 24.7136,
    val consentedLng: Double? = 46.6753
) {
    fun elapsedMinutes(now: Long = System.currentTimeMillis()): Int {
        val end = deliveredAt ?: now
        return ((end - dispatchedAt) / 60_000L).toInt().coerceAtLeast(0)
    }

    fun isOverdue(now: Long = System.currentTimeMillis()): Boolean {
        if (status in setOf(DeliveryWorkflowStatus.DELIVERED, DeliveryWorkflowStatus.RETURNED_TO_BRANCH)) return false
        return elapsedMinutes(now) > estimatedDeliveryMinutes
    }
}

data class DriverCashSettlement(
    val id: String,
    val branchId: String,
    val driverId: String,
    val driverName: String,
    val assignmentIds: List<String>,
    val expectedCashAmount: Double,
    val handedOverCashAmount: Double,
    val varianceAmount: Double = handedOverCashAmount - expectedCashAmount,
    val receivedByCashierName: String,
    val settledAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)

// =========================================================================================
// PHASE 9: INVENTORY, WAREHOUSES, TRACEABLE MOVEMENTS, TRANSFERS & PROCUREMENT
// =========================================================================================

enum class CostingMethod(val titleEn: String, val titleAr: String) {
    WEIGHTED_AVERAGE("Weighted Average Cost (WAC)", "المتوسط المرجح للتكلفة"),
    FIFO("First-In, First-Out (FIFO)", "الوارد أولاً يصرف أولاً"),
    STANDARD_COST("Standard Recipe Cost", "التكلفة المعيارية")
}

data class UnitConversionRule(
    val id: String,
    val ingredientId: String,
    val ingredientNameEn: String,
    val ingredientNameAr: String,
    val purchaseUnit: String, // e.g. "Case (10 kg)"
    val stockUnit: String, // e.g. "kg"
    val recipeUnit: String, // e.g. "g"
    val purchaseToStockFactor: Double, // e.g. 10.0 (1 Case = 10 kg)
    val stockToRecipeFactor: Double // e.g. 1000.0 (1 kg = 1000 g)
)

data class WarehouseStockItem(
    val id: String,
    val warehouseId: String,
    val warehouseName: String,
    val branchId: String,
    val itemCode: String,
    val itemId: String, // links to Ingredient.id or Product.id
    val itemNameEn: String,
    val itemNameAr: String,
    val stockUnit: String,
    val recipeUnit: String = stockUnit,
    val currentQuantity: Double,
    val reservedQuantity: Double = 0.0,
    val minimumQuantity: Double = 10.0,
    val maximumQuantity: Double = 200.0,
    val reorderPoint: Double = 20.0,
    val unitCost: Double,
    val costingMethod: CostingMethod = CostingMethod.WEIGHTED_AVERAGE,
    val lastUpdatedAt: Long = System.currentTimeMillis()
) {
    val availableQuantity: Double
        get() = (currentQuantity - reservedQuantity).coerceAtLeast(0.0)

    val totalValuation: Double
        get() = currentQuantity * unitCost

    val isLowStock: Boolean
        get() = availableQuantity <= reorderPoint
}

enum class InventoryTransactionType(val titleEn: String, val titleAr: String, val sign: Int) {
    OPENING_BALANCE("Opening Balance", "رصيد افتتاحي", +1),
    PURCHASE_RECEIPT("Purchase Receipt (GRN)", "استلام مشتريات", +1),
    SALE_CONSUMPTION("Recipe Sale Consumption", "استهلاك مبيعات الوصفات", -1),
    TRANSFER_OUT("Warehouse Transfer Out", "صرف تحويل مخزني", -1),
    TRANSFER_IN("Warehouse Transfer In", "استلام تحويل مخزني", +1),
    ADJUSTMENT("Stock Adjustment", "تسوية جردية", 0),
    WASTE("Kitchen Waste", "هدر مطبخ", -1),
    DAMAGE("Damaged Stock", "تالف مخزني", -1),
    RETURN_TO_SUPPLIER("Return to Supplier", "مرتجع للمورد", -1),
    STOCK_COUNT("Physical Stock Count Variance", "فروقات الجرد الفعلي", 0)
}

data class InventoryTransaction(
    val id: String,
    val referenceNumber: String,
    val branchId: String,
    val warehouseId: String,
    val warehouseName: String,
    val itemId: String,
    val itemNameEn: String,
    val itemNameAr: String,
    val transactionType: InventoryTransactionType,
    val quantityDelta: Double, // positive for inflow, negative for outflow
    val unit: String,
    val unitCost: Double,
    val totalCostImpact: Double = kotlin.math.abs(quantityDelta) * unitCost,
    val balanceBefore: Double,
    val balanceAfter: Double,
    val sourceDocumentType: String, // ORDER, PURCHASE_ORDER, TRANSFER, WASTE_LOG, COUNT_SESSION
    val sourceDocumentId: String,
    val performedBy: String,
    val approvedBy: String? = null,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class StockCountLine(
    val itemId: String,
    val itemNameEn: String,
    val itemNameAr: String,
    val unit: String,
    val systemQuantity: Double,
    val countedQuantity: Double,
    val unitCost: Double
) {
    val varianceQuantity: Double get() = countedQuantity - systemQuantity
    val varianceValue: Double get() = varianceQuantity * unitCost
}

data class StockCountSession(
    val id: String,
    val sessionNumber: String,
    val branchId: String,
    val warehouseId: String,
    val warehouseName: String,
    val status: String = "APPROVED", // DRAFT, PENDING_APPROVAL, APPROVED
    val lines: List<StockCountLine>,
    val countedBy: String,
    val approvedBy: String? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class WasteRecord(
    val id: String,
    val wasteNumber: String,
    val branchId: String,
    val warehouseId: String,
    val itemId: String,
    val itemNameEn: String,
    val itemNameAr: String,
    val quantity: Double,
    val unit: String,
    val unitCost: Double,
    val totalLossAmount: Double = quantity * unitCost,
    val reason: String, // Expired, Overcooked, Dropped, Contamination, Prep Trim
    val reportedByEmployee: String,
    val approvalStatus: String = "APPROVED", // PENDING, APPROVED, REJECTED
    val approvedBy: String? = "Karim Mansour",
    val timestamp: Long = System.currentTimeMillis()
)

enum class WarehouseTransferStatus(val titleEn: String, val titleAr: String) {
    REQUESTED("Requested", "تم الطلب"),
    APPROVED("Approved", "معتمد"),
    SHIPPED("In Transit / Shipped", "تم الشحن"),
    RECEIVED("Received", "تم الاستلام"),
    COMPLETED("Completed", "مكتمل")
}

data class WarehouseTransfer(
    val id: String,
    val transferNumber: String,
    val sourceWarehouseId: String,
    val sourceWarehouseName: String,
    val destinationWarehouseId: String,
    val destinationWarehouseName: String,
    val itemId: String,
    val itemNameEn: String,
    val itemNameAr: String,
    val quantity: Double,
    val unit: String,
    val unitCost: Double,
    val status: WarehouseTransferStatus = WarehouseTransferStatus.REQUESTED,
    val requestedBy: String,
    val approvedBy: String? = null,
    val receivedBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)

enum class ProcurementStage(val titleEn: String, val titleAr: String) {
    PURCHASE_REQUEST("Purchase Request (PR)", "طلب شراء داخلي"),
    APPROVED("PR Approved", "طلب شراء معتمد"),
    QUOTATION("Supplier Quotation (RFQ)", "عرض سعر مورد"),
    PURCHASE_ORDER("Purchase Order (PO)", "أمر شراء رسمي"),
    RECEIVED("Goods Received (GRN)", "تم استلام البضاعة"),
    SUPPLIER_INVOICE("Supplier Invoice Posted", "فاتورة مورد مسجلة"),
    SUPPLIER_PAYMENT("Paid & Settled", "تم سداد المورد"),
    SUPPLIER_RETURN("Supplier Return", "مرتجع مشتريات للمورد")
}

data class PurchaseOrderLine(
    val itemId: String,
    val itemNameEn: String,
    val itemNameAr: String,
    val quantityOrdered: Double,
    val quantityReceived: Double = quantityOrdered,
    val purchaseUnit: String,
    val stockConversionFactor: Double = 1.0,
    val unitPrice: Double,
    val lineTotal: Double = quantityOrdered * unitPrice
)

data class PurchaseOrderDocument(
    val id: String,
    val poNumber: String,
    val branchId: String,
    val warehouseId: String,
    val warehouseName: String,
    val supplierId: String,
    val supplierName: String,
    val stage: ProcurementStage = ProcurementStage.PURCHASE_ORDER,
    val lines: List<PurchaseOrderLine>,
    val subtotal: Double,
    val taxAmount: Double,
    val totalAmount: Double,
    val paidAmount: Double = 0.0,
    val supplierInvoiceNumber: String? = null,
    val requestedBy: String = "Chef Antoine Khalil",
    val approvedBy: String? = "Karim Mansour",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)

// =========================================================================================
// PHASE 10: FINANCIAL & ACCOUNTING SUBSYSTEM (COA, DOUBLE-ENTRY JOURNAL, AR/AP, REPORTS)
// =========================================================================================

enum class AccountType(
    val code: String,
    val titleEn: String,
    val titleAr: String,
    val normalBalance: String // DEBIT or CREDIT
) {
    ASSETS("ASSETS", "Assets", "الأصول", "DEBIT"),
    LIABILITIES("LIABILITIES", "Liabilities", "الالتزامات والخصوم", "CREDIT"),
    EQUITY("EQUITY", "Owner's Equity", "حقوق الملكية", "CREDIT"),
    REVENUE("REVENUE", "Revenue", "الإيرادات والمبيعات", "CREDIT"),
    COST_OF_SALES("COST_OF_SALES", "Cost of Goods Sold (COGS)", "تكلفة المبيعات", "DEBIT"),
    EXPENSES("EXPENSES", "Operating Expenses", "المصروفات التشغيلية", "DEBIT")
}

data class ChartOfAccount(
    val id: String,
    val accountCode: String,
    val nameEn: String,
    val nameAr: String,
    val accountType: AccountType,
    val parentAccountCode: String? = null,
    val isActive: Boolean = true,
    val balance: Double = 0.0
)

data class CostCenter(
    val id: String,
    val code: String,
    val nameEn: String,
    val nameAr: String,
    val centerType: String, // BRANCH, DEPARTMENT, KITCHEN, DELIVERY, CUSTOM
    val branchId: String = "branch_01",
    val budgetMonthly: Double = 25000.0
)

enum class FinancialPeriodStatus(val titleEn: String, val titleAr: String) {
    OPEN("Open for Posting", "مفتوحة للترحيل"),
    CLOSED("Soft Closed", "مغلقة مبدئياً"),
    LOCKED("Audited & Locked", "مقفلة نهائياً")
}

data class FinancialPeriod(
    val id: String,
    val periodCode: String, // e.g. "2026-10"
    val nameEn: String,
    val nameAr: String,
    val startDate: String,
    val endDate: String,
    val status: FinancialPeriodStatus = FinancialPeriodStatus.OPEN,
    val closedBy: String? = null
)

data class JournalLine(
    val id: String,
    val accountCode: String,
    val accountNameEn: String,
    val accountNameAr: String,
    val accountType: AccountType,
    val debit: Double = 0.0,
    val credit: Double = 0.0,
    val costCenterCode: String = "CC-BRANCH-01",
    val memo: String = ""
)

data class JournalEntry(
    val id: String,
    val entryNumber: String,
    val date: String, // YYYY-MM-DD
    val periodCode: String = "2026-10",
    val branchId: String,
    val costCenterCode: String = "CC-BRANCH-01",
    val sourceDocumentType: String, // POS_SALE, REFUND, PURCHASE_INVOICE, EXPENSE, SUPPLIER_PAYMENT, CUSTOMER_RECEIPT, INVENTORY_COGS, BANK_TRANSFER
    val sourceDocumentId: String,
    val referenceNumber: String,
    val descriptionEn: String,
    val descriptionAr: String,
    val lines: List<JournalLine>,
    val postedBy: String = "Auto-Accounting Engine",
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalDebit: Double get() = lines.sumOf { it.debit }
    val totalCredit: Double get() = lines.sumOf { it.credit }
    val isBalanced: Boolean get() = kotlin.math.abs(totalDebit - totalCredit) < 0.01
}

data class BankAccount(
    val id: String,
    val accountNameEn: String,
    val accountNameAr: String,
    val bankName: String,
    val ibanNumber: String,
    val glAccountCode: String = "1020",
    val currency: String = "USD",
    val currentBalance: Double,
    val branchId: String = "branch_01"
)

data class BankTransaction(
    val id: String,
    val referenceNumber: String,
    val bankAccountId: String,
    val bankAccountName: String,
    val transactionType: String, // DEPOSIT, WITHDRAWAL, TRANSFER,CASH_SWEEP
    val amount: Double,
    val counterparty: String,
    val notes: String,
    val journalEntryId: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class SubledgerInvoice(
    val id: String,
    val invoiceNumber: String,
    val ledgerType: String, // RECEIVABLE (Customer) or PAYABLE (Supplier)
    val partyId: String,
    val partyName: String,
    val sourceDocumentId: String,
    val issueDate: String,
    val dueDate: String,
    val daysPastDue: Int,
    val totalAmount: Double,
    val paidAmount: Double,
    val status: String = if (paidAmount >= totalAmount) "PAID" else if (paidAmount > 0) "PARTIAL" else "OPEN"
) {
    val remainingBalance: Double get() = (totalAmount - paidAmount).coerceAtLeast(0.0)
    val agingBucket: String
        get() = when {
            remainingBalance <= 0.0 -> "CURRENT"
            daysPastDue <= 30 -> "0-30 DAYS"
            daysPastDue <= 60 -> "31-60 DAYS"
            daysPastDue <= 90 -> "61-90 DAYS"
            else -> "90+ DAYS"
        }
}

// =========================================================================================
// PHASE 11: HR, EMPLOYEE MANAGEMENT, ATTENDANCE, SHIFTS, LEAVE & PAYROLL
// =========================================================================================

enum class AttendanceVerificationSource(
    val code: String,
    val titleEn: String,
    val titleAr: String,
    val integrationNoteEn: String
) {
    FINGERPRINT_DEVICE_API(
        "FINGERPRINT_DEVICE_API",
        "Biometric Terminal Gateway (ZKTeco / HID API)",
        "بوابة جهاز البصمة (تكامل شبكي)",
        "Integration-ready webhook/TCP listener for external biometric terminals"
    ),
    MOBILE_CAMERA_VERIFICATION(
        "MOBILE_CAMERA_VERIFICATION",
        "Mobile Geofence & Selfie Check-In",
        "تحقق الجوال والموقع الجغرافي",
        "Integration-ready mobile camera & branch GPS boundary verification"
    ),
    TERMINAL_CAMERA(
        "TERMINAL_CAMERA",
        "POS Terminal Badge + Camera Snapshot",
        "كاميرا نقطة البيع وبطاقة الموظف",
        "Integration-ready POS terminal camera verification"
    ),
    MANUAL_SUPERVISOR_OVERRIDE(
        "MANUAL_SUPERVISOR_OVERRIDE",
        "Manual Entry (Requires Manager Permission)",
        "إدخال يدوي (يتطلب صلاحية المدير)",
        "Audited manual attendance entry with supervisor approval"
    )
}

data class AttendanceRecord(
    val id: String,
    val employeeId: String,
    val employeeNumber: String,
    val employeeName: String,
    val department: String,
    val branchId: String,
    val date: String, // YYYY-MM-DD
    val scheduledShiftName: String = "Morning Operations (08:00 - 16:00)",
    val checkInTime: String, // HH:mm
    val checkOutTime: String? = null,
    val breakMinutes: Int = 30,
    val lateMinutes: Int = 0,
    val earlyLeaveMinutes: Int = 0,
    val overtimeMinutes: Int = 0,
    val status: String = "PRESENT", // PRESENT, LATE, ABSENT, ON_LEAVE, OVERTIME
    val source: AttendanceVerificationSource = AttendanceVerificationSource.TERMINAL_CAMERA,
    val isApproved: Boolean = true,
    val approvedBy: String? = "Karim Mansour",
    val correctionNote: String = ""
)

data class ShiftTemplate(
    val id: String,
    val branchId: String,
    val code: String,
    val nameEn: String,
    val nameAr: String,
    val startTime: String, // e.g. "08:00"
    val endTime: String, // e.g. "16:00"
    val breakDurationMinutes: Int = 30,
    val department: String = "All Departments"
)

data class EmployeeShiftSchedule(
    val id: String,
    val branchId: String,
    val employeeId: String,
    val employeeName: String,
    val department: String,
    val shiftTemplateId: String,
    val shiftName: String,
    val date: String,
    val linkedRoleModule: String // POS, WAITER, KITCHEN, DRIVER, INVENTORY
)

enum class LeaveType(val titleEn: String, val titleAr: String, val annualAllowanceDays: Int) {
    ANNUAL("Annual Paid Leave", "إجازة سنوية مدفوعة", 21),
    SICK("Medical / Sick Leave", "إجازة مرضية", 14),
    EMERGENCY("Emergency Family Leave", "إجازة اضطرارية", 5),
    UNPAID("Unpaid Authorized Leave", "إجازة بدون راتب", 30)
}

data class LeaveRequest(
    val id: String,
    val employeeId: String,
    val employeeName: String,
    val department: String,
    val branchId: String,
    val leaveType: LeaveType,
    val startDate: String,
    val endDate: String,
    val daysCount: Int,
    val reason: String,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val approvedBy: String? = null,
    val remainingBalanceDays: Int = 18,
    val requestedAt: Long = System.currentTimeMillis()
)

data class PayrollRecord(
    val id: String,
    val payrollPeriod: String, // e.g. "2026-10"
    val branchId: String,
    val employeeId: String,
    val employeeNumber: String,
    val employeeName: String,
    val department: String,
    val jobTitle: String,
    val baseSalary: Double,
    val housingAllowance: Double,
    val transportAllowance: Double,
    val overtimePay: Double = 0.0,
    val deductions: Double = 0.0,
    val salaryAdvanceDeduction: Double = 0.0,
    val status: String = "APPROVED", // DRAFT, PENDING_APPROVAL, APPROVED, PAID
    val approvedBy: String? = "Mira Hamoud",
    val paidAt: Long? = null
) {
    val netSalary: Double
        get() = (baseSalary + housingAllowance + transportAllowance + overtimePay - deductions - salaryAdvanceDeduction).coerceAtLeast(0.0)
}

// =========================================================================================
// PHASE 12: ANALYTICS, REPORT ENGINE, DRILL-DOWN, MULTI-CHANNEL NOTIFICATIONS & AI BOUNDARY
// =========================================================================================

enum class EnterpriseReportType(
    val code: String,
    val titleEn: String,
    val titleAr: String,
    val categoryGroup: String
) {
    SALES("SALES", "Sales Summary Report", "تقرير ملخص المبيعات", "Commercial"),
    ORDERS("ORDERS", "Orders Audit Report", "تقرير الطلبات التفصيلي", "Commercial"),
    PRODUCTS("PRODUCTS", "Product Performance Report", "تقرير أداء الأصناف", "Commercial"),
    CATEGORIES("CATEGORIES", "Category Mix Report", "تقرير مبيعات التصنيفات", "Commercial"),
    CASHIER("CASHIER", "Cashier & Shift Variance Report", "تقرير الكاشير وعهدة الورديات", "Commercial"),
    BRANCHES("BRANCHES", "Multi-Branch Comparison", "تقرير مقارنة الفروع", "Commercial"),
    CUSTOMERS("CUSTOMERS", "Customer CRM & Loyalty Report", "تقرير العملاء والولاء", "Commercial"),
    KITCHEN("KITCHEN", "Kitchen SLA & Station Speed", "تقرير سرعة محطات المطبخ", "Operations"),
    DELIVERY("DELIVERY", "Delivery SLA & Zone Performance", "تقرير التوصيل والمناطق", "Operations"),
    DRIVERS("DRIVERS", "Driver Productivity & Cash COD", "تقرير السائقين والتحصيل النقدي", "Operations"),
    INVENTORY("INVENTORY", "Warehouse Stock & Valuation", "تقرير المخزون وتقييم المستودعات", "Supply Chain"),
    PURCHASING("PURCHASING", "Purchasing & GRN Report", "تقرير أوامر الشراء والاستلام", "Supply Chain"),
    SUPPLIERS("SUPPLIERS", "Supplier Spend & Payables", "تقرير الموردين والمستحقات", "Supply Chain"),
    EXPENSES("EXPENSES", "Operating Expenses Breakdown", "تقرير المصروفات التشغيلية", "Finance"),
    ACCOUNTING("ACCOUNTING", "General Ledger & Trial Balance", "تقرير الأستاذ العام وميزان المراجعة", "Finance"),
    EMPLOYEES("EMPLOYEES", "Employee Roster & Cost Report", "تقرير الموظفين والرواتب", "HR"),
    ATTENDANCE("ATTENDANCE", "Attendance, Late & Overtime", "تقرير الحضور والانصراف والإضافي", "HR"),
    PROFITABILITY("PROFITABILITY", "Net Profitability & Margin P&L", "تقرير الربحية الصافية وهوامش الربح", "Finance")
}

data class ReportFilterCriteria(
    val dateRangeLabel: String = "Last 30 Days",
    val branchId: String? = null, // null = All Allowed Branches
    val brandId: String? = null,
    val channel: SalesChannel? = null,
    val employeeId: String? = null,
    val productId: String? = null,
    val categoryId: String? = null,
    val customerId: String? = null,
    val paymentMethod: PaymentMethod? = null
)

data class ReportRowData(
    val primaryKey: String,
    val labelEn: String,
    val labelAr: String,
    val secondaryLabel: String,
    val metricOneLabel: String,
    val metricOneValue: String,
    val metricTwoLabel: String,
    val metricTwoValue: String,
    val statusBadge: String,
    val sourceDocumentType: String,
    val sourceDocumentId: String,
    val auditActionFilter: String
)

data class DrillDownTrace(
    val metricTitle: String,
    val reportType: EnterpriseReportType,
    val sourceDocumentType: String,
    val sourceDocumentId: String,
    val sourceSummary: String,
    val accountingReference: String,
    val relatedAuditLogs: List<AuditLog>
)

enum class NotificationChannelType(val titleEn: String, val titleAr: String) {
    IN_APP("In-App Live Banner", "إشعار داخلي فوري"),
    PUSH_READY("Push Notification (FCM-Ready)", "إشعار جوال (مهيأ لـ FCM)"),
    EMAIL_READY("Email Dispatch (SMTP-Ready)", "بريد إلكتروني (مهيأ)"),
    SMS_READY("SMS Gateway (OTP/Alert-Ready)", "رسائل نصية SMS (مهيأ)"),
    WHATSAPP_READY("WhatsApp Business API-Ready", "واتساب للأعمال (مهيأ)")
}

data class CentralNotificationEvent(
    val id: String,
    val eventTrigger: String, // ORDER_CREATED, ORDER_CONFIRMED, KITCHEN_READY, DRIVER_ASSIGNED, DELIVERY_COMPLETED, LOW_STOCK, PAYMENT_FAILURE, SHIFT_CLOSING, APPROVAL_REQUEST
    val titleEn: String,
    val titleAr: String,
    val messageEn: String,
    val messageAr: String,
    val channelsDispatched: List<NotificationChannelType>,
    val referenceId: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class AuthorizedAiQueryResponse(
    val queryText: String,
    val isAuthorized: Boolean,
    val requiredPermission: String,
    val answerEn: String,
    val answerAr: String,
    val supportingFacts: List<String>,
    val dataTimestamp: Long = System.currentTimeMillis()
)
