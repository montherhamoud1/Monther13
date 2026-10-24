package com.example.domain.model

// 1. Company
data class Company(
    val id: String,
    val code: String,
    val nameEn: String,
    val nameAr: String,
    val taxNumber: String,
    val defaultCurrency: String = "USD",
    val country: String = "Saudi Arabia",
    val fiscalYear: String = "2026-2027",
    val autoLockMinutes: Int = 15,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

// 2. Brand
data class Brand(
    val id: String,
    val companyId: String,
    val code: String,
    val nameEn: String,
    val nameAr: String,
    val status: String = "ACTIVE",
    val logoUrl: String = "",
    val primaryColor: String = "#F59E0B",
    val secondaryColor: String = "#0F172A",
    val menuIdentity: String = "Gourmet Burgers & Smokehouse",
    val isActive: Boolean = true
)

// 3. Branch
data class Branch(
    val id: String,
    val brandId: String,
    val code: String,
    val nameEn: String,
    val nameAr: String,
    val phone: String,
    val address: String,
    val seatingCapacity: Int = 80,
    val isActive: Boolean = true,
    val openingTime: String = "10:00",
    val closingTime: String = "01:00",
    val timezone: String = "Asia/Riyadh (GMT+3)",
    val currency: String = "USD",
    val taxRate: Double = 0.10,
    val taxInclusive: Boolean = false,
    val assignedWarehouseId: String = "wh_01",
    val posTerminals: List<PosTerminal> = emptyList()
)

data class PosTerminal(
    val id: String,
    val branchId: String,
    val terminalCode: String,
    val name: String,
    val status: String = "ONLINE", // ONLINE, OFFLINE, MAINTENANCE
    val ipAddress: String = "192.168.1.101"
)

// 4. User
data class User(
    val id: String,
    val companyId: String,
    val username: String,
    val email: String,
    val fullName: String,
    val roleId: String,
    val activeBranchId: String,
    val status: String = "ACTIVE", // ACTIVE, SUSPENDED, PENDING_ACTIVATION
    val passwordHash: String = "password123",
    val allowedBranchIds: List<String> = listOf("branch_01", "branch_02", "branch_03"),
    val failedAttempts: Int = 0,
    val isLocked: Boolean = false,
    val lastLoginAt: Long = System.currentTimeMillis()
)

// 5. Role
data class Role(
    val id: String,
    val code: String,
    val nameEn: String,
    val nameAr: String,
    val permissions: List<String> = emptyList(),
    val isSystem: Boolean = true,
    val branchRestricted: Boolean = false,
    val description: String = ""
)

// 6. Permission & Granular Rules
enum class ModuleType(val code: String) {
    ADMINISTRATION("ADMINISTRATION"),
    POS("POS"),
    TABLES("TABLES"),
    KITCHEN("KITCHEN"),
    INVENTORY("INVENTORY"),
    REPORTS("REPORTS"),
    AUDIT("AUDIT")
}

enum class ScreenType(val code: String) {
    COMPANY_SETTINGS("COMPANY_SETTINGS"),
    BRAND_MANAGEMENT("BRAND_MANAGEMENT"),
    BRANCH_MANAGEMENT("BRANCH_MANAGEMENT"),
    USER_MANAGEMENT("USER_MANAGEMENT"),
    ROLE_MANAGEMENT("ROLE_MANAGEMENT"),
    EMPLOYEE_MANAGEMENT("EMPLOYEE_MANAGEMENT"),
    POS_TERMINAL("POS_TERMINAL"),
    FLOOR_TABLES("FLOOR_TABLES"),
    KDS_SCREEN("KDS_SCREEN"),
    WAREHOUSE_STOCK("WAREHOUSE_STOCK"),
    AUDIT_TRAIL("AUDIT_TRAIL")
}

enum class ActionType(val code: String) {
    VIEW("VIEW"),
    CREATE("CREATE"),
    EDIT("EDIT"),
    DELETE("DELETE"),
    APPROVE("APPROVE"),
    CANCEL("CANCEL"),
    REFUND("REFUND"),
    DISCOUNT("DISCOUNT"),
    EXPORT("EXPORT"),
    PRINT("PRINT"),
    MANAGE_SETTINGS("MANAGE_SETTINGS")
}

data class Permission(
    val id: String,
    val code: String,
    val module: String,
    val descriptionEn: String,
    val descriptionAr: String
)

data class PermissionRule(
    val module: ModuleType,
    val screen: ScreenType,
    val action: ActionType,
    val isBranchRestricted: Boolean = false
) {
    val code: String get() = "${module.code}:${screen.code}:${action.code}"
}

// 7. Employee
data class Employee(
    val id: String,
    val branchId: String,
    val userId: String,
    val employeeNumber: String,
    val jobTitle: String,
    val hourlyRate: Double = 0.0,
    val status: String = "ACTIVE", // ACTIVE, ON_LEAVE, TERMINATED
    val fullName: String = "",
    val department: String = "Operations", // Operations, Kitchen, Front of House, Delivery, Management
    val phone: String = "",
    val email: String = "",
    val hireDate: String = "2025-01-15"
)

// 8. Customer (Extended for Phase 7 Customer Platform & CRM)
data class Customer(
    val id: String,
    val companyId: String,
    val name: String,
    val phone: String,
    val email: String,
    val loyaltyPoints: Int = 0,
    val tier: String = "SILVER", // BRONZE, SILVER, GOLD, PLATINUM
    val preferredBranchId: String = "branch_01",
    val favoriteProductIds: List<String> = emptyList(),
    val addresses: List<CustomerAddress> = emptyList(),
    val orderCount: Int = 0,
    val totalSpend: Double = 0.0,
    val averageOrderValue: Double = 0.0,
    val lastOrderAt: Long? = null,
    val pointsExpiryDate: String = "2027-12-31",
    val otpVerified: Boolean = true
)

// 9. Category
data class Category(
    val id: String,
    val brandId: String,
    val nameEn: String,
    val nameAr: String,
    val sortOrder: Int = 0,
    val colorHex: String = "#F59E0B",
    val parentId: String? = null,
    val imageUrl: String = "",
    val branchAvailability: List<String> = emptyList(), // if empty, available in all branches
    val isActive: Boolean = true
)

// 10. Product
data class Product(
    val id: String,
    val categoryId: String,
    val sku: String,
    val nameEn: String,
    val nameAr: String,
    val price: Double,
    val costPrice: Double = 0.0,
    val isAvailable: Boolean = true,
    val isTaxable: Boolean = true,
    val currentStock: Double = 100.0,
    val reorderPoint: Double = 10.0,
    val barcode: String = "",
    val descriptionEn: String = "",
    val descriptionAr: String = "",
    val imageUrl: String = "",
    val isSellable: Boolean = true,
    val isPreparationItem: Boolean = true, // Kitchen prep required
    val isInventoryItem: Boolean = true,
    val isCombo: Boolean = false,
    val variants: List<ProductVariant> = emptyList(),
    val modifierGroupIds: List<String> = emptyList(),
    val addonIds: List<String> = emptyList(),
    val branchAvailability: List<String> = emptyList(),
    val taxProfileId: String = "tax_standard_10"
)

// Variants
data class ProductVariant(
    val id: String,
    val productId: String,
    val nameEn: String,
    val nameAr: String,
    val sku: String,
    val priceAdjustment: Double,
    val barcode: String = "",
    val isDefault: Boolean = false
)

// Modifiers
data class ModifierGroup(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val isRequired: Boolean = false,
    val minSelections: Int = 0,
    val maxSelections: Int = 1,
    val options: List<ModifierOption> = emptyList()
)

data class ModifierOption(
    val id: String,
    val groupId: String,
    val nameEn: String,
    val nameAr: String,
    val priceAdjustment: Double = 0.0,
    val isDefault: Boolean = false,
    val calories: Int = 0
)

// Add-ons
data class ProductAddon(
    val id: String,
    val productId: String,
    val nameEn: String,
    val nameAr: String,
    val price: Double,
    val maxQuantity: Int = 5
)

// Combos
data class ComboComponent(
    val id: String,
    val comboId: String,
    val titleEn: String,
    val titleAr: String,
    val allowedCategoryIds: List<String> = emptyList(),
    val defaultProductId: String? = null,
    val allowedProductIds: List<String> = emptyList(),
    val extraCharge: Double = 0.0
)

data class ComboProduct(
    val id: String,
    val productId: String,
    val nameEn: String,
    val nameAr: String,
    val sku: String,
    val comboBasePrice: Double,
    val components: List<ComboComponent> = emptyList()
)

// Sales Channels & Pricing Engine
enum class SalesChannel(val titleEn: String, val titleAr: String) {
    POS("POS Terminal", "نقطة البيع الكاشير"),
    WAITER_APP("Waiter Handheld", "تطبيق النادل"),
    QR_TABLE("QR Table Ordering", "الطلب عبر رمز QR"),
    CUSTOMER_WEB("Customer Web Store", "الموقع الإلكتروني للعميل"),
    CUSTOMER_APP("Customer Mobile App", "تطبيق الجوال للعميل"),
    DELIVERY_PARTNERS("Delivery Aggregators", "منصات التوصيل الشريكة")
}

data class ChannelPriceRule(
    val id: String,
    val productId: String,
    val variantId: String? = null,
    val branchId: String? = null,
    val channel: SalesChannel? = null,
    val customerTier: String? = null,
    val customPrice: Double
)

// Tax Configuration
data class TaxProfile(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val rate: Double = 0.10,
    val isInclusive: Boolean = false,
    val taxCode: String = "VAT_STD"
)

// Availability & Schedule
data class ProductSchedule(
    val productId: String,
    val branchId: String? = null,
    val isAvailable: Boolean = true,
    val isOutOfStock: Boolean = false,
    val daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    val startTime: String = "00:00",
    val endTime: String = "23:59"
)

// Recipe Engine & Product Costing
data class Ingredient(
    val id: String,
    val code: String,
    val nameEn: String,
    val nameAr: String,
    val unit: String,
    val costPerUnit: Double,
    val currentStock: Double,
    val yieldPercentage: Double = 1.0,
    val wastePercentage: Double = 0.05,
    val isSubRecipe: Boolean = false
)

data class RecipeItem(
    val ingredientId: String,
    val ingredientNameEn: String,
    val quantity: Double,
    val unit: String,
    val isPackaging: Boolean = false
)

data class Recipe(
    val id: String,
    val productId: String,
    val variantId: String? = null,
    val name: String,
    val yieldCount: Double = 1.0,
    val items: List<RecipeItem> = emptyList(),
    val packagingCost: Double = 0.35,
    val laborCost: Double = 0.75
)

data class ProductCostSummary(
    val productId: String,
    val sellingPrice: Double,
    val rawIngredientsCost: Double,
    val packagingCost: Double,
    val laborCost: Double,
    val totalCost: Double,
    val grossProfit: Double,
    val grossMarginPercentage: Double,
    val cogsPercentage: Double
)

// 11. Order & Central Order Engine (Phase 4)
enum class OrderType(val code: String, val titleEn: String, val titleAr: String) {
    DINE_IN("DINE_IN", "Dine-In", "محلي (طاولات)"),
    TAKEOUT("TAKEOUT", "Takeaway", "سفري"),
    PICKUP("PICKUP", "Curbside Pickup", "استلام من الفرع"),
    DELIVERY("DELIVERY", "Home Delivery", "توصيل للمنازل"),
    PHONE_ORDER("PHONE_ORDER", "Phone Order", "طلب هاتفي"),
    QR_ONLINE("QR_ONLINE", "QR / Online", "طلب رقمي QR")
}

enum class OrderStatus(val code: String, val titleEn: String, val titleAr: String) {
    DRAFT("DRAFT", "Draft", "مسودة"),
    HELD("HELD", "On Hold", "معلق"),
    PENDING("PENDING", "Pending", "قيد الانتظار"),
    PREPARING("PREPARING", "Preparing", "قيد التحضير"),
    READY("READY", "Ready", "جاهز للتسليم"),
    SERVED("SERVED", "Served", "تم التقديم"),
    OUT_FOR_DELIVERY("OUT_FOR_DELIVERY", "Out for Delivery", "جاري التوصيل"),
    COMPLETED("COMPLETED", "Completed", "مكتمل"),
    CANCELLED("CANCELLED", "Cancelled", "ملغي"),
    REFUNDED("REFUNDED", "Refunded", "مسترجع مالياً"),
    RETURNED("RETURNED", "Returned", "مرتجع أصناف")
}

enum class SyncStatus(val titleEn: String, val titleAr: String) {
    SYNCED("Synced", "متزامن"),
    QUEUED_OFFLINE("Queued Offline", "في طابور عدم الاتصال"),
    SYNCING("Syncing", "جاري المزامنة"),
    FAILED("Sync Failed", "فشل المزامنة")
}

data class Order(
    val id: String,
    val branchId: String,
    val orderNumber: String,
    val orderType: OrderType,
    val tableId: String? = null,
    val customerId: String? = null,
    val status: OrderStatus = OrderStatus.PENDING,
    val subtotal: Double,
    val taxAmount: Double,
    val discountAmount: Double = 0.0,
    val totalAmount: Double,
    val paymentStatus: String = "UNPAID", // UNPAID, PARTIAL, PAID, REFUNDED
    val createdAt: Long = System.currentTimeMillis(),
    // Phase 4 Central Order Engine Extensions
    val terminalId: String = "POS-01",
    val cashierId: String = "user_01",
    val cashierName: String = "Mira Hamoud",
    val customerName: String? = null,
    val customerPhone: String? = null,
    val channel: SalesChannel = SalesChannel.POS,
    val couponCode: String? = null,
    val discountReason: String = "",
    val deliveryFee: Double = 0.0,
    val deliveryAddress: String = "",
    val serviceFee: Double = 0.0,
    val notes: String = "",
    val holdLabel: String? = null,
    val idempotencyKey: String = "idem_${id}_${createdAt}",
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val updatedAt: Long = createdAt,
    val completedAt: Long? = null,
    val cancelledReason: String? = null,
    val refundedAmount: Double = 0.0,
    val refundReason: String? = null,
    val approvedByManager: String? = null,
    val items: List<OrderItem> = emptyList(),
    val payments: List<Payment> = emptyList()
)

// 12. OrderItem
data class OrderItem(
    val id: String,
    val orderId: String,
    val productId: String,
    val productNameEn: String,
    val productNameAr: String,
    val unitPrice: Double,
    val quantity: Int,
    val totalPrice: Double,
    val notes: String = "",
    val kitchenStatus: String = "PENDING", // PENDING, COOKING, READY, SERVED, CANCELLED
    // Phase 4 Item Customization & Return Extensions
    val variantId: String? = null,
    val variantName: String? = null,
    val modifiersSummary: String = "",
    val modifiersDelta: Double = 0.0,
    val addonsSummary: String = "",
    val addonsDelta: Double = 0.0,
    val discountAmount: Double = 0.0,
    val returnedQuantity: Int = 0
)

// 13. Smart Table Management, Floors, Zones, QR & Reservations (Phase 5)
enum class TableStatus(val titleEn: String, val titleAr: String) {
    AVAILABLE("Available", "متاحة"),
    OCCUPIED("Occupied", "مشغولة"),
    RESERVED("Reserved", "محجوزة"),
    WAITING_PAYMENT("Waiting Payment", "بانتظار الدفع"),
    BILLED("Waiting Payment", "تم إصدار الفاتورة"),
    CLEANING("Cleaning", "قيد التنظيف"),
    UNAVAILABLE("Unavailable", "غير متاحة")
}

data class RestaurantFloor(
    val id: String,
    val branchId: String,
    val nameEn: String,
    val nameAr: String,
    val levelOrder: Int = 1
)

data class TableZone(
    val id: String,
    val floorId: String,
    val branchId: String,
    val nameEn: String,
    val nameAr: String,
    val colorHex: String = "#3B82F6"
)

data class RestaurantTable(
    val id: String,
    val branchId: String,
    val tableNumber: String,
    val capacity: Int,
    val section: String, // Zone name: Indoor Main, Terrace Garden, VIP Suite
    val status: TableStatus = TableStatus.AVAILABLE,
    val currentOrderId: String? = null,
    // Phase 5 Smart Table Extensions
    val floorId: String = "floor_01",
    val floorName: String = "Ground Floor",
    val zoneId: String = "zone_indoor",
    val combinedWithTableIds: List<String> = emptyList(),
    val combinedCapacity: Int = capacity,
    val assignedWaiterId: String? = null,
    val assignedWaiterName: String? = null,
    val qrToken: String = "qtok_${id}_secure",
    val activeReservationId: String? = null,
    val guestCount: Int = 0,
    val shape: String = "ROUND", // ROUND, RECTANGLE, BOOTH
    val posX: Int = 0,
    val posY: Int = 0
)

enum class ReservationStatus(val titleEn: String, val titleAr: String) {
    PENDING("Pending", "قيد الانتظار"),
    CONFIRMED("Confirmed", "مؤكد"),
    SEATED("Seated", "تم إجلاس الضيوف"),
    COMPLETED("Completed", "مكتمل"),
    CANCELLED("Cancelled", "ملغي"),
    NO_SHOW("No-Show", "لم يحضر")
}

data class Reservation(
    val id: String,
    val branchId: String,
    val customerId: String? = null,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String = "",
    val tableId: String,
    val tableNumber: String,
    val reservationDate: String, // YYYY-MM-DD
    val reservationTime: String, // HH:mm
    val partySize: Int,
    val status: ReservationStatus = ReservationStatus.PENDING,
    val notes: String = "",
    val confirmationCode: String = "RES-${id.takeLast(4).uppercase()}",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)

data class CustomerTableSession(
    val sessionId: String,
    val qrToken: String,
    val companyId: String,
    val companyName: String,
    val branchId: String,
    val branchName: String,
    val tableId: String,
    val tableNumber: String,
    val zoneName: String,
    val guestName: String = "Table Guest",
    val partySize: Int = 2,
    val activeOrderId: String? = null,
    val billRequested: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

// 14. Kitchen Display System (KDS), Stations, Routing & Real-Time Operations (Phase 6)
enum class KitchenStationType(
    val code: String,
    val titleEn: String,
    val titleAr: String,
    val colorHex: String
) {
    GRILL("GRILL", "Grill Station", "محطة الشواء والجريل", "#EA580C"),
    PIZZA("PIZZA", "Pizza & Oven", "محطة البيتزا والفرن", "#DC2626"),
    FRYER("FRYER", "Fryer Station", "محطة القلي المقرمش", "#D97706"),
    DRINKS("DRINKS", "Drinks & Bar", "محطة المشروبات والبار", "#2563EB"),
    DESSERT("DESSERT", "Dessert & Pastry", "محطة الحلويات", "#DB2777"),
    PACKING("PACKING", "Packing Station", "محطة التغليف والتعبئة", "#7C3AED"),
    EXPEDITER("EXPEDITER", "Expediter / Pass", "محطة التجميع والفحص النهائي", "#059669")
}

enum class KdsTicketStatus(
    val code: String,
    val titleEn: String,
    val titleAr: String
) {
    NEW("NEW", "New", "جديد"),
    ACCEPTED("ACCEPTED", "Accepted", "مقبول"),
    PREPARING("PREPARING", "Preparing", "قيد التحضير"),
    READY("READY", "Ready", "جاهز"),
    COMPLETED("COMPLETED", "Completed", "مكتمل"),
    CANCELLED("CANCELLED", "Cancelled", "ملغي");

    companion object {
        fun fromRawStatus(raw: String): KdsTicketStatus {
            return when (raw.uppercase()) {
                "NEW", "QUEUED", "PENDING" -> NEW
                "ACCEPTED" -> ACCEPTED
                "PREPARING", "COOKING" -> PREPARING
                "READY" -> READY
                "COMPLETED", "SERVED", "BUMPED" -> COMPLETED
                "CANCELLED" -> CANCELLED
                else -> NEW
            }
        }
    }
}

enum class TicketPriority(
    val code: String,
    val titleEn: String,
    val titleAr: String
) {
    NORMAL("NORMAL", "Normal", "عادي"),
    HIGH("HIGH", "High Priority", "أولوية عالية"),
    VIP_RUSH("VIP_RUSH", "VIP / Rush", "عاجل / كبار الشخصيات")
}

data class Kitchen(
    val id: String,
    val branchId: String,
    val code: String,
    val nameEn: String,
    val nameAr: String,
    val description: String = "",
    val isActive: Boolean = true,
    val defaultWarningMinutes: Int = 8,
    val defaultOverdueMinutes: Int = 15
)

data class PreparationArea(
    val id: String,
    val kitchenId: String,
    val branchId: String,
    val areaCode: String,
    val nameEn: String,
    val nameAr: String
)

data class KitchenStation(
    val id: String,
    val kitchenId: String,
    val prepAreaId: String,
    val prepAreaName: String,
    val branchId: String,
    val code: String,
    val stationType: KitchenStationType,
    val nameEn: String,
    val nameAr: String,
    val warningThresholdMinutes: Int = 8,
    val overdueThresholdMinutes: Int = 15,
    val isActive: Boolean = true,
    val assignedEmployeeIds: List<String> = emptyList(),
    val colorHex: String = stationType.colorHex
)

data class KitchenEmployeeAssignment(
    val id: String,
    val branchId: String,
    val employeeId: String,
    val employeeName: String,
    val roleTitle: String,
    val kitchenId: String,
    val stationId: String,
    val stationName: String,
    val stationType: KitchenStationType,
    val isOnDuty: Boolean = true,
    val ticketsCompletedCount: Int = 0
)

data class StationRoutingRule(
    val id: String,
    val branchId: String,
    val nameEn: String,
    val nameAr: String,
    val categoryId: String? = null,
    val productId: String? = null,
    val orderType: OrderType? = null,
    val targetStationId: String,
    val targetStationType: KitchenStationType,
    val targetStationName: String,
    val routeToPackingWhenReady: Boolean = true,
    val routeToExpediterWhenReady: Boolean = true,
    val isActive: Boolean = true
)

data class KitchenTicketItem(
    val id: String,
    val orderItemId: String,
    val productId: String,
    val productNameEn: String,
    val productNameAr: String,
    val variantName: String? = null,
    val quantity: Int,
    val modifiersSummary: String = "",
    val addonsSummary: String = "",
    val notes: String = "",
    val stationType: KitchenStationType = KitchenStationType.GRILL,
    val status: KdsTicketStatus = KdsTicketStatus.NEW
)

data class KitchenTicket(
    val id: String,
    val branchId: String,
    val orderId: String,
    val orderNumber: String,
    val stationName: String, // Grill, Pizza, Fryer, Drinks, Dessert, Packing, Expediter
    val itemsSummary: String,
    val status: String = "NEW", // NEW, ACCEPTED, PREPARING, READY, COMPLETED, CANCELLED
    val elapsedMinutes: Int = 0,
    val isPriority: Boolean = false,
    // Phase 6 KDS & Workflow Extensions
    val kitchenId: String = "kit_main",
    val prepAreaId: String = "prep_hot",
    val stationId: String = "st_grill",
    val stationType: KitchenStationType = KitchenStationType.GRILL,
    val orderType: OrderType = OrderType.DINE_IN,
    val channel: SalesChannel = SalesChannel.POS,
    val tableId: String? = null,
    val tableNumber: String? = null,
    val serverOrCashierName: String = "",
    val customerName: String? = null,
    val priority: TicketPriority = if (isPriority) TicketPriority.VIP_RUSH else TicketPriority.NORMAL,
    val ticketStatus: KdsTicketStatus = KdsTicketStatus.fromRawStatus(status),
    val items: List<KitchenTicketItem> = emptyList(),
    val orderNotes: String = "",
    val warningThresholdMinutes: Int = 8,
    val overdueThresholdMinutes: Int = 15,
    val workflowStage: String = "STATION_PREP", // STATION_PREP, PACKING, EXPEDITER, COMPLETED
    val createdAt: Long = System.currentTimeMillis() - (elapsedMinutes * 60_000L),
    val acceptedAt: Long? = null,
    val preparingAt: Long? = null,
    val readyAt: Long? = null,
    val completedAt: Long? = null,
    val assignedChefName: String? = null
) {
    fun computeElapsedSeconds(nowMillis: Long = System.currentTimeMillis()): Long {
        val endRef = completedAt ?: nowMillis
        val diffSeconds = ((endRef - createdAt) / 1000L).coerceAtLeast(0L)
        return maxOf(diffSeconds, elapsedMinutes * 60L)
    }

    fun computeElapsedMinutes(nowMillis: Long = System.currentTimeMillis()): Int {
        return (computeElapsedSeconds(nowMillis) / 60L).toInt()
    }

    fun isOverdue(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (ticketStatus in listOf(KdsTicketStatus.COMPLETED, KdsTicketStatus.CANCELLED)) return false
        return computeElapsedMinutes(nowMillis) >= overdueThresholdMinutes
    }

    fun isWarning(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (ticketStatus in listOf(KdsTicketStatus.COMPLETED, KdsTicketStatus.CANCELLED)) return false
        val mins = computeElapsedMinutes(nowMillis)
        return mins in warningThresholdMinutes until overdueThresholdMinutes
    }
}

enum class RealTimeEventType(val titleEn: String, val titleAr: String) {
    NEW_ORDER("New Order", "طلب جديد"),
    ORDER_STATUS_CHANGED("Order Status Changed", "تحديث حالة الطلب"),
    TABLE_CHANGED("Table State Changed", "تحديث حالة الطاولة"),
    KITCHEN_TICKET_UPDATED("Kitchen Ticket Updated", "تحديث تذكرة المطبخ"),
    NOTIFICATION("Operational Notification", "إشعار تشغيلي")
}

data class RealTimeOperationalEvent(
    val id: String,
    val type: RealTimeEventType,
    val branchId: String,
    val entityId: String,
    val referenceCode: String,
    val titleEn: String,
    val titleAr: String,
    val detailsEn: String,
    val detailsAr: String,
    val sourceChannel: String = "SYSTEM",
    val timestamp: Long = System.currentTimeMillis()
)

// 15. Driver (Extended for Phase 8 Delivery Dispatch & Driver App)
data class DeliveryDriver(
    val id: String,
    val branchId: String,
    val fullName: String,
    val phone: String,
    val vehicleType: String = "MOTORCYCLE", // MOTORCYCLE, CAR, BICYCLE
    val isAvailable: Boolean = true,
    val activeDeliveries: Int = 0,
    val employeeId: String = "emp_06",
    val vehiclePlate: String = "RYD-4821",
    val shiftStatus: String = "ONLINE", // ONLINE, OFFLINE, ON_BREAK
    val driverStatus: String = if (isAvailable) "AVAILABLE" else "ON_DELIVERY", // AVAILABLE, ON_DELIVERY, BREAK, OFFLINE
    val locationSharingConsented: Boolean = true,
    val currentLat: Double = 24.7136,
    val currentLng: Double = 46.6753,
    val lastLocationUpdateAt: Long = System.currentTimeMillis(),
    val cashCollectedUnsettled: Double = 0.0,
    val completedDeliveriesToday: Int = 4,
    val rating: Double = 4.9
)

// 16. Warehouse (Extended for Phase 9 Multi-Warehouse Inventory)
data class Warehouse(
    val id: String,
    val branchId: String,
    val code: String,
    val nameEn: String,
    val nameAr: String,
    val location: String,
    val isCentral: Boolean = false,
    val warehouseType: String = if (isCentral) "CENTRAL" else "BRANCH", // CENTRAL, BRANCH, KITCHEN, COLD_STORAGE
    val managerName: String = "Karim Mansour",
    val isActive: Boolean = true
)

// 17. Supplier (Extended for Phase 9 Procurement & Phase 10 Payables)
data class Supplier(
    val id: String,
    val companyId: String,
    val code: String,
    val name: String,
    val contactPerson: String,
    val phone: String,
    val email: String,
    val paymentTerms: String = "NET_30",
    val suppliedIngredientIds: List<String> = emptyList(),
    val currentPayableBalance: Double = 0.0,
    val rating: Double = 4.8,
    val isActive: Boolean = true
)

// 18. Payment
enum class PaymentMethod(val title: String, val titleAr: String = title) {
    CASH("Cash", "نقدي"),
    CREDIT_CARD("Credit Card", "بطاقة ائتمانية"),
    DEBIT_CARD("Debit Card", "بطاقة مدى / خصم"),
    BANK_TRANSFER("Bank Transfer", "تحويل بنكي"),
    DIGITAL_WALLET("Digital Wallet", "محفظة رقمية"),
    GIFT_CARD("Gift Card", "بطاقة هدايا"),
    MOBILE_PAY("Mobile Pay", "دفع عبر الجوال")
}

data class Payment(
    val id: String,
    val orderId: String,
    val branchId: String,
    val amount: Double,
    val paymentMethod: PaymentMethod,
    val referenceNumber: String,
    val status: String = "SUCCESS", // SUCCESS, REFUNDED, VOIDED
    val timestamp: Long = System.currentTimeMillis(),
    val idempotencyKey: String = "pay_idem_${id}",
    val cashierId: String = "user_01",
    val shiftId: String? = null,
    val tenderedAmount: Double = amount,
    val changeAmount: Double = 0.0
)

// Phase 4: Discounts & Coupons
enum class DiscountType {
    PERCENTAGE,
    FIXED_AMOUNT
}

data class Coupon(
    val code: String,
    val titleEn: String,
    val titleAr: String,
    val discountType: DiscountType,
    val value: Double,
    val minOrderAmount: Double = 0.0,
    val maxDiscountAmount: Double? = null,
    val isActive: Boolean = true,
    // Phase 7 Coupon Targeting & Restrictions
    val validFromDate: String = "2026-01-01",
    val validUntilDate: String = "2027-12-31",
    val allowedBranchIds: List<String> = emptyList(), // empty = all branches
    val allowedCategoryIds: List<String> = emptyList(), // empty = all categories
    val allowedProductIds: List<String> = emptyList(), // empty = all products
    val allowedCustomerIds: List<String> = emptyList(), // empty = all customers
    val allowedTiers: List<String> = emptyList() // empty = all loyalty tiers
)

// Phase 4: Cashier Shifts & Drawer Management
enum class ShiftStatus {
    OPEN,
    CLOSED
}

enum class CashMovementType(val titleEn: String, val titleAr: String) {
    CASH_IN("Pay In (Cash Added)", "إيداع نقدي بالصندوق"),
    CASH_OUT("Pay Out (Safe Drop)", "سحب نقدي من الصندوق"),
    EXPENSE("Petty Cash Expense", "مصروفات نثرية"),
    REFUND("Cash Refund", "استرداد نقدي لعميل")
}

data class CashDrawerMovement(
    val id: String,
    val shiftId: String,
    val branchId: String,
    val type: CashMovementType,
    val amount: Double,
    val reason: String,
    val performedBy: String,
    val approvedBy: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class CashierShift(
    val id: String,
    val branchId: String,
    val terminalId: String,
    val cashierId: String,
    val cashierName: String,
    val status: ShiftStatus = ShiftStatus.OPEN,
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val openingCash: Double = 200.0,
    val cashSales: Double = 0.0,
    val cardSales: Double = 0.0,
    val bankTransferSales: Double = 0.0,
    val walletSales: Double = 0.0,
    val cashIn: Double = 0.0,
    val cashOut: Double = 0.0,
    val expenses: Double = 0.0,
    val cashRefunds: Double = 0.0,
    val actualClosingCash: Double? = null,
    val closingNotes: String = "",
    val movements: List<CashDrawerMovement> = emptyList()
) {
    val totalGrossSales: Double
        get() = cashSales + cardSales + bankTransferSales + walletSales

    val expectedCash: Double
        get() = openingCash + cashSales + cashIn - cashOut - expenses - cashRefunds

    val cashDifference: Double
        get() = (actualClosingCash ?: expectedCash) - expectedCash
}

// Phase 4: Offline Sync Queue
data class OfflineSyncEvent(
    val id: String,
    val idempotencyKey: String,
    val branchId: String,
    val operationType: String, // PLACE_ORDER, UPDATE_ORDER, PAYMENT, REFUND, SHIFT_CLOSE
    val entityId: String,
    val summary: String,
    val status: SyncStatus = SyncStatus.QUEUED_OFFLINE,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null
)

// Phase 4: Split Bill Model
data class SplitSeatShare(
    val seatNumber: Int,
    val label: String,
    val items: List<OrderItem> = emptyList(),
    val subtotal: Double,
    val taxAmount: Double,
    val serviceFeeShare: Double,
    val discountShare: Double,
    val totalDue: Double,
    val isPaid: Boolean = false,
    val paymentMethod: PaymentMethod? = null
)

// 19. Expense (Extended for Phase 10 Financial & Accounting Subsystem)
data class Expense(
    val id: String,
    val branchId: String,
    val category: String, // Utilities, Supplies, Maintenance, Food, Marketing, Rent
    val description: String,
    val amount: Double,
    val approvedBy: String,
    val timestamp: Long = System.currentTimeMillis(),
    val documentNumber: String = "EXP-${id.takeLast(4).uppercase()}",
    val costCenterCode: String = "CC-BRANCH-01",
    val paymentMethod: PaymentMethod = PaymentMethod.BANK_TRANSFER,
    val attachmentName: String = "receipt_${id}.pdf",
    val approvalStatus: String = "APPROVED", // PENDING_APPROVAL, APPROVED, REJECTED
    val journalEntryId: String? = null
)

// 20. AuditLog
data class AuditLog(
    val id: String,
    val companyId: String,
    val branchId: String,
    val userId: String,
    val userName: String,
    val action: String, // LOGIN, SWITCH_BRANCH, CREATE_ORDER, VOID_ITEM, PAY_ORDER, USER_CREATE, etc.
    val entityName: String,
    val entityId: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

// Session Management State
data class UserSession(
    val user: User,
    val role: Role,
    val token: String,
    val loginTimestamp: Long,
    val expiresAt: Long,
    val activeBranchId: String,
    val allowedBranchIds: List<String>,
    val effectivePermissions: Set<String>
) {
    val isValid: Boolean get() = System.currentTimeMillis() < expiresAt
    fun hasPermission(permissionCode: String): Boolean {
        if (role.code == "ROLE_ADMIN" || effectivePermissions.contains("*")) return true
        return effectivePermissions.contains(permissionCode)
    }
}
