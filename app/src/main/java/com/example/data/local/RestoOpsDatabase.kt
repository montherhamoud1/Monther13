package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CompanyEntity::class,
        BrandEntity::class,
        BranchEntity::class,
        PosTerminalEntity::class,
        UserEntity::class,
        RoleEntity::class,
        EmployeeEntity::class,
        ProductEntity::class,
        TableEntity::class,
        ReservationEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        PaymentEntity::class,
        CashierShiftEntity::class,
        OfflineSyncQueueEntity::class,
        AuditLogEntity::class,
        KitchenTicketEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class RestoOpsDatabase : RoomDatabase() {
    abstract fun companyDao(): CompanyDao
    abstract fun brandDao(): BrandDao
    abstract fun branchDao(): BranchDao
    abstract fun posTerminalDao(): PosTerminalDao
    abstract fun userDao(): UserDao
    abstract fun roleDao(): RoleDao
    abstract fun employeeDao(): EmployeeDao
    abstract fun productDao(): ProductDao
    abstract fun tableDao(): TableDao
    abstract fun reservationDao(): ReservationDao
    abstract fun orderDao(): OrderDao
    abstract fun paymentDao(): PaymentDao
    abstract fun shiftDao(): ShiftDao
    abstract fun offlineSyncDao(): OfflineSyncDao
    abstract fun auditDao(): AuditDao
    abstract fun kitchenTicketDao(): KitchenTicketDao

    companion object {
        @Volatile
        private var INSTANCE: RestoOpsDatabase? = null

        fun getInstance(context: Context): RestoOpsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RestoOpsDatabase::class.java,
                    "restoops_erp.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
