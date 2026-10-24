package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "companies")
data class CompanyEntity(
    @PrimaryKey val id: String,
    val code: String,
    val nameEn: String,
    val nameAr: String,
    val taxNumber: String,
    val defaultCurrency: String,
    val country: String,
    val fiscalYear: String,
    val autoLockMinutes: Int,
    val isActive: Boolean,
    val createdAt: Long
)

@Entity(tableName = "brands")
data class BrandEntity(
    @PrimaryKey val id: String,
    val companyId: String,
    val code: String,
    val nameEn: String,
    val nameAr: String,
    val status: String,
    val logoUrl: String,
    val primaryColor: String,
    val secondaryColor: String,
    val menuIdentity: String,
    val isActive: Boolean
)

@Entity(tableName = "branches")
data class BranchEntity(
    @PrimaryKey val id: String,
    val brandId: String,
    val code: String,
    val nameEn: String,
    val nameAr: String,
    val phone: String,
    val address: String,
    val seatingCapacity: Int,
    val isActive: Boolean,
    val openingTime: String = "10:00",
    val closingTime: String = "01:00",
    val timezone: String = "Asia/Riyadh (GMT+3)",
    val currency: String = "USD",
    val taxRate: Double = 0.10,
    val taxInclusive: Boolean = false,
    val assignedWarehouseId: String = "wh_01"
)

@Entity(tableName = "pos_terminals")
data class PosTerminalEntity(
    @PrimaryKey val id: String,
    val branchId: String,
    val terminalCode: String,
    val name: String,
    val status: String,
    val ipAddress: String
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val companyId: String,
    val username: String,
    val email: String,
    val fullName: String,
    val roleId: String,
    val activeBranchId: String,
    val status: String,
    val passwordHash: String,
    val allowedBranchIds: String, // comma separated
    val failedAttempts: Int,
    val isLocked: Boolean,
    val lastLoginAt: Long
)

@Entity(tableName = "roles")
data class RoleEntity(
    @PrimaryKey val id: String,
    val code: String,
    val nameEn: String,
    val nameAr: String,
    val permissionsJson: String, // comma separated permission codes
    val isSystem: Boolean,
    val branchRestricted: Boolean,
    val description: String
)

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey val id: String,
    val branchId: String,
    val userId: String,
    val employeeNumber: String,
    val fullName: String,
    val jobTitle: String,
    val department: String,
    val phone: String,
    val email: String,
    val hourlyRate: Double,
    val status: String,
    val hireDate: String
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val sku: String,
    val nameEn: String,
    val nameAr: String,
    val price: Double,
    val costPrice: Double,
    val isAvailable: Boolean,
    val isTaxable: Boolean,
    val currentStock: Double,
    val reorderPoint: Double
)

@Entity(tableName = "restaurant_tables")
data class TableEntity(
    @PrimaryKey val id: String,
    val branchId: String,
    val tableNumber: String,
    val capacity: Int,
    val section: String,
    val status: String,
    val currentOrderId: String?,
    val floorId: String = "floor_01",
    val floorName: String = "Ground Floor",
    val zoneId: String = "zone_indoor",
    val combinedWithTableIdsCsv: String = "", // comma-separated table IDs
    val combinedCapacity: Int = capacity,
    val assignedWaiterId: String? = null,
    val assignedWaiterName: String? = null,
    val qrToken: String = "QR-SIG-DEFAULT",
    val activeReservationId: String? = null,
    val guestCount: Int = 0,
    val shape: String = "ROUND",
    val posX: Int = 0,
    val posY: Int = 0
)

@Entity(tableName = "reservations")
data class ReservationEntity(
    @PrimaryKey val id: String,
    val branchId: String,
    val customerId: String?,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String,
    val tableId: String,
    val tableNumber: String,
    val reservationDate: String,
    val reservationTime: String,
    val partySize: Int,
    val status: String,
    val notes: String,
    val confirmationCode: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val branchId: String,
    val orderNumber: String,
    val orderType: String,
    val tableId: String?,
    val customerId: String?,
    val status: String,
    val subtotal: Double,
    val taxAmount: Double,
    val discountAmount: Double,
    val totalAmount: Double,
    val paymentStatus: String,
    val createdAt: Long,
    val terminalId: String = "POS-01",
    val cashierId: String = "user_01",
    val cashierName: String = "Mira Hamoud",
    val customerName: String? = null,
    val customerPhone: String? = null,
    val channel: String = "POS",
    val couponCode: String? = null,
    val discountReason: String = "",
    val deliveryFee: Double = 0.0,
    val deliveryAddress: String = "",
    val serviceFee: Double = 0.0,
    val notes: String = "",
    val holdLabel: String? = null,
    val idempotencyKey: String = "idem_${id}_${createdAt}",
    val syncStatus: String = "SYNCED",
    val updatedAt: Long = createdAt,
    val completedAt: Long? = null,
    val cancelledReason: String? = null,
    val refundedAmount: Double = 0.0,
    val refundReason: String? = null,
    val approvedByManager: String? = null
)

@Entity(tableName = "order_items")
data class OrderItemEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val productId: String,
    val productNameEn: String,
    val productNameAr: String,
    val unitPrice: Double,
    val quantity: Int,
    val totalPrice: Double,
    val notes: String,
    val kitchenStatus: String,
    val variantId: String? = null,
    val variantName: String? = null,
    val modifiersSummary: String = "",
    val modifiersDelta: Double = 0.0,
    val addonsSummary: String = "",
    val addonsDelta: Double = 0.0,
    val discountAmount: Double = 0.0,
    val returnedQuantity: Int = 0
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val branchId: String,
    val amount: Double,
    val paymentMethod: String,
    val referenceNumber: String,
    val status: String,
    val timestamp: Long,
    val idempotencyKey: String,
    val cashierId: String,
    val shiftId: String?,
    val tenderedAmount: Double,
    val changeAmount: Double
)

@Entity(tableName = "cashier_shifts")
data class CashierShiftEntity(
    @PrimaryKey val id: String,
    val branchId: String,
    val terminalId: String,
    val cashierId: String,
    val cashierName: String,
    val status: String,
    val openedAt: Long,
    val closedAt: Long?,
    val openingCash: Double,
    val cashSales: Double,
    val cardSales: Double,
    val bankTransferSales: Double,
    val walletSales: Double,
    val cashIn: Double,
    val cashOut: Double,
    val expenses: Double,
    val cashRefunds: Double,
    val actualClosingCash: Double?,
    val closingNotes: String
)

@Entity(tableName = "offline_sync_queue")
data class OfflineSyncQueueEntity(
    @PrimaryKey val id: String,
    val idempotencyKey: String,
    val branchId: String,
    val operationType: String,
    val entityId: String,
    val summary: String,
    val status: String,
    val retryCount: Int,
    val createdAt: Long,
    val syncedAt: Long?
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val companyId: String,
    val branchId: String,
    val userId: String,
    val userName: String,
    val action: String,
    val entityName: String,
    val entityId: String,
    val details: String,
    val timestamp: Long
)

@Entity(tableName = "kitchen_tickets")
data class KitchenTicketEntity(
    @PrimaryKey val id: String,
    val branchId: String,
    val orderId: String,
    val orderNumber: String,
    val stationName: String,
    val itemsSummary: String,
    val status: String,
    val elapsedMinutes: Int,
    val isPriority: Boolean,
    val kitchenId: String = "kit_main",
    val prepAreaId: String = "prep_hot",
    val stationId: String = "st_grill",
    val stationType: String = "GRILL",
    val orderType: String = "DINE_IN",
    val channel: String = "POS",
    val tableId: String? = null,
    val tableNumber: String? = null,
    val serverOrCashierName: String = "",
    val customerName: String? = null,
    val priority: String = "NORMAL",
    val ticketStatus: String = "NEW",
    val itemsPayload: String = "", // Serialized ticket items
    val orderNotes: String = "",
    val warningThresholdMinutes: Int = 8,
    val overdueThresholdMinutes: Int = 15,
    val workflowStage: String = "STATION_PREP",
    val createdAt: Long = System.currentTimeMillis(),
    val acceptedAt: Long? = null,
    val preparingAt: Long? = null,
    val readyAt: Long? = null,
    val completedAt: Long? = null,
    val assignedChefName: String? = null
)

