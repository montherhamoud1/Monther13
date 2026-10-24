package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyDao {
    @Query("SELECT * FROM companies LIMIT 1")
    fun getCompany(): Flow<CompanyEntity?>

    @Query("SELECT * FROM companies WHERE id = :id")
    suspend fun getCompanyById(id: String): CompanyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompany(company: CompanyEntity)

    @Update
    suspend fun updateCompany(company: CompanyEntity)
}

@Dao
interface BrandDao {
    @Query("SELECT * FROM brands")
    fun getAllBrands(): Flow<List<BrandEntity>>

    @Query("SELECT * FROM brands WHERE id = :id")
    suspend fun getBrandById(id: String): BrandEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrands(brands: List<BrandEntity>)

    @Update
    suspend fun updateBrand(brand: BrandEntity)
}

@Dao
interface BranchDao {
    @Query("SELECT * FROM branches")
    fun getAllBranches(): Flow<List<BranchEntity>>

    @Query("SELECT * FROM branches WHERE isActive = 1")
    fun getAllActiveBranches(): Flow<List<BranchEntity>>

    @Query("SELECT * FROM branches WHERE id = :id")
    suspend fun getBranchById(id: String): BranchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranches(branches: List<BranchEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranch(branch: BranchEntity)

    @Update
    suspend fun updateBranch(branch: BranchEntity)

    @Query("UPDATE branches SET isActive = :isActive WHERE id = :id")
    suspend fun updateBranchStatus(id: String, isActive: Boolean)
}

@Dao
interface PosTerminalDao {
    @Query("SELECT * FROM pos_terminals WHERE branchId = :branchId")
    fun getTerminalsForBranch(branchId: String): Flow<List<PosTerminalEntity>>

    @Query("SELECT * FROM pos_terminals")
    fun getAllTerminals(): Flow<List<PosTerminalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerminals(terminals: List<PosTerminalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerminal(terminal: PosTerminalEntity)

    @Update
    suspend fun updateTerminal(terminal: PosTerminalEntity)

    @Query("DELETE FROM pos_terminals WHERE id = :id")
    suspend fun deleteTerminal(id: String)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET failedAttempts = :attempts, isLocked = :isLocked WHERE id = :userId")
    suspend fun updateFailedAttempts(userId: String, attempts: Int, isLocked: Boolean)

    @Query("UPDATE users SET passwordHash = :newHash, failedAttempts = 0, isLocked = 0 WHERE id = :userId")
    suspend fun resetPassword(userId: String, newHash: String)

    @Query("UPDATE users SET status = :status WHERE id = :userId")
    suspend fun updateUserStatus(userId: String, status: String)
}

@Dao
interface RoleDao {
    @Query("SELECT * FROM roles")
    fun getAllRoles(): Flow<List<RoleEntity>>

    @Query("SELECT * FROM roles WHERE id = :id")
    suspend fun getRoleById(id: String): RoleEntity?

    @Query("SELECT * FROM roles WHERE code = :code LIMIT 1")
    suspend fun getRoleByCode(code: String): RoleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoles(roles: List<RoleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRole(role: RoleEntity)

    @Update
    suspend fun updateRole(role: RoleEntity)

    @Query("DELETE FROM roles WHERE id = :id AND isSystem = 0")
    suspend fun deleteCustomRole(id: String)
}

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees")
    fun getAllEmployees(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE branchId = :branchId")
    fun getEmployeesByBranch(branchId: String): Flow<List<EmployeeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployees(employees: List<EmployeeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: EmployeeEntity)

    @Update
    suspend fun updateEmployee(employee: EmployeeEntity)

    @Query("UPDATE employees SET status = :status WHERE id = :id")
    suspend fun updateEmployeeStatus(id: String, status: String)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isAvailable = 1")
    fun getAllAvailableProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE categoryId = :catId AND isAvailable = 1")
    fun getProductsByCategory(catId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE currentStock <= reorderPoint")
    fun getLowStockProducts(): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)
}

@Dao
interface TableDao {
    @Query("SELECT * FROM restaurant_tables WHERE branchId = :branchId")
    fun getTablesByBranch(branchId: String): Flow<List<TableEntity>>

    @Query("SELECT * FROM restaurant_tables")
    fun getAllTables(): Flow<List<TableEntity>>

    @Query("SELECT * FROM restaurant_tables WHERE id = :tableId LIMIT 1")
    suspend fun getTableById(tableId: String): TableEntity?

    @Query("SELECT * FROM restaurant_tables WHERE qrToken = :qrToken LIMIT 1")
    suspend fun getTableByQrToken(qrToken: String): TableEntity?

    @Query("UPDATE restaurant_tables SET status = :status, currentOrderId = :orderId WHERE id = :tableId")
    suspend fun updateTableStatus(tableId: String, status: String, orderId: String?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTable(table: TableEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTables(tables: List<TableEntity>)
}

@Dao
interface ReservationDao {
    @Query("SELECT * FROM reservations WHERE branchId = :branchId ORDER BY reservationDate ASC, reservationTime ASC")
    fun getReservationsByBranch(branchId: String): Flow<List<ReservationEntity>>

    @Query("SELECT * FROM reservations WHERE id = :id LIMIT 1")
    suspend fun getReservationById(id: String): ReservationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReservation(reservation: ReservationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReservations(reservations: List<ReservationEntity>)

    @Query("DELETE FROM reservations WHERE id = :id")
    suspend fun deleteReservation(id: String)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders WHERE branchId = :branchId ORDER BY createdAt DESC")
    fun getOrdersByBranch(branchId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE idempotencyKey = :idempotencyKey LIMIT 1")
    suspend fun getOrderByIdempotencyKey(idempotencyKey: String): OrderEntity?

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getItemsForOrder(orderId: String): List<OrderItemEntity>

    @Query("SELECT * FROM order_items")
    fun getAllOrderItems(): Flow<List<OrderItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteItemsForOrder(orderId: String)

    @Query("DELETE FROM orders WHERE id = :orderId")
    suspend fun deleteOrder(orderId: String)

    @Query("UPDATE orders SET status = :status, updatedAt = :updatedAt WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE orders SET paymentStatus = :paymentStatus, status = 'COMPLETED', completedAt = :completedAt WHERE id = :orderId")
    suspend fun markOrderPaid(orderId: String, paymentStatus: String, completedAt: Long = System.currentTimeMillis())

    @Query("UPDATE order_items SET kitchenStatus = :kitchenStatus WHERE id = :orderItemId")
    suspend fun updateOrderItemKitchenStatus(orderItemId: String, kitchenStatus: String)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE orderId = :orderId")
    suspend fun getPaymentsForOrder(orderId: String): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE idempotencyKey = :idempotencyKey LIMIT 1")
    suspend fun getPaymentByIdempotencyKey(idempotencyKey: String): PaymentEntity?

    @Query("SELECT * FROM payments WHERE branchId = :branchId ORDER BY timestamp DESC")
    fun getPaymentsByBranch(branchId: String): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<PaymentEntity>)
}

@Dao
interface ShiftDao {
    @Query("SELECT * FROM cashier_shifts WHERE branchId = :branchId ORDER BY openedAt DESC")
    fun getShiftsByBranch(branchId: String): Flow<List<CashierShiftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: CashierShiftEntity)
}

@Dao
interface OfflineSyncDao {
    @Query("SELECT * FROM offline_sync_queue ORDER BY createdAt DESC")
    fun getAllQueueItems(): Flow<List<OfflineSyncQueueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncItem(item: OfflineSyncQueueEntity)

    @Query("UPDATE offline_sync_queue SET status = 'SYNCED', syncedAt = :syncedAt WHERE status != 'SYNCED'")
    suspend fun markAllSynced(syncedAt: Long = System.currentTimeMillis())
}

@Dao
interface AuditDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 150")
    fun getRecentAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity)
}

@Dao
interface KitchenTicketDao {
    @Query("SELECT * FROM kitchen_tickets WHERE branchId = :branchId ORDER BY createdAt DESC")
    fun getTicketsByBranch(branchId: String): Flow<List<KitchenTicketEntity>>

    @Query("SELECT * FROM kitchen_tickets ORDER BY createdAt DESC")
    fun getAllTickets(): Flow<List<KitchenTicketEntity>>

    @Query("SELECT * FROM kitchen_tickets WHERE id = :ticketId LIMIT 1")
    suspend fun getTicketById(ticketId: String): KitchenTicketEntity?

    @Query("SELECT * FROM kitchen_tickets WHERE orderId = :orderId")
    suspend fun getTicketsForOrder(orderId: String): List<KitchenTicketEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: KitchenTicketEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTickets(tickets: List<KitchenTicketEntity>)

    @Query("DELETE FROM kitchen_tickets WHERE id = :ticketId")
    suspend fun deleteTicket(ticketId: String)
}

