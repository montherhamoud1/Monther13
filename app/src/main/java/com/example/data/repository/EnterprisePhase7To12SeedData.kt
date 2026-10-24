package com.example.data.repository

import com.example.domain.model.*

object EnterprisePhase7To12SeedData {

    // --- Phase 7: Customer Addresses, Loyalty Rewards, Reviews & Complaints ---
    val customerAddresses = listOf(
        CustomerAddress(
            id = "addr_1",
            customerId = "cust_1",
            label = "Home Villa (Al-Olaya)",
            streetAddress = "7422 Olaya Main St, District 4",
            buildingAndFloor = "Villa 14, Gate B",
            city = "Riyadh",
            deliveryZoneId = "dzone_01",
            deliveryNotes = "Ring intercom 14B, guard has gate access",
            isDefault = true
        ),
        CustomerAddress(
            id = "addr_2",
            customerId = "cust_1",
            label = "Executive Office (KAFD)",
            streetAddress = "King Abdullah Financial District, Tower 3",
            buildingAndFloor = "Floor 18, Suite 1804",
            city = "Riyadh",
            deliveryZoneId = "dzone_02",
            deliveryNotes = "Leave at 18th floor reception desk",
            isDefault = false
        ),
        CustomerAddress(
            id = "addr_3",
            customerId = "cust_2",
            label = "Residence (Al-Malqa)",
            streetAddress = "3190 Prince Muhammad Bin Saad Rd",
            buildingAndFloor = "Apt 9, 3rd Floor",
            city = "Riyadh",
            deliveryZoneId = "dzone_01",
            deliveryNotes = "Call upon arrival",
            isDefault = true
        )
    )

    val loyaltyRewards = listOf(
        LoyaltyReward(
            id = "rew_1",
            titleEn = "$5 Off Any Order",
            titleAr = "خصم 5 دولار على أي طلب",
            pointsCost = 250,
            discountValue = 5.00,
            minTierRequired = "BRONZE"
        ),
        LoyaltyReward(
            id = "rew_2",
            titleEn = "Free Loaded Truffle Fries",
            titleAr = "بطاطس بالكمأة مجانية",
            pointsCost = 400,
            discountValue = 7.50,
            freeProductId = "p4",
            minTierRequired = "SILVER"
        ),
        LoyaltyReward(
            id = "rew_3",
            titleEn = "$15 VIP Dining Voucher",
            titleAr = "قسيمة عشاء VIP بقيمة 15 دولار",
            pointsCost = 750,
            discountValue = 15.00,
            minTierRequired = "GOLD"
        ),
        LoyaltyReward(
            id = "rew_4",
            titleEn = "Chef's Signature Burger & Dessert Experience",
            titleAr = "وجبة برجر الشيف مع الحلوى مجاناً",
            pointsCost = 1200,
            discountValue = 28.00,
            freeProductId = "p1",
            minTierRequired = "PLATINUM"
        )
    )

    val customerReviews = listOf(
        CustomerReview(
            id = "rev_01",
            orderId = "ord_demo_1",
            orderNumber = "ORD-1041",
            productId = "p1",
            productNameEn = "Truffle Angus Burger",
            customerId = "cust_1",
            customerName = "Faisal Al-Rashid",
            rating = 5,
            comment = "Incredible sear on the Angus patty and the truffle aioli was spot on. Arrived piping hot!",
            moderationStatus = ReviewModerationStatus.APPROVED,
            managerReply = "Thank you Faisal! Our Grill Chef Marco is thrilled to hear this."
        ),
        CustomerReview(
            id = "rev_02",
            orderId = "ord_demo_2",
            orderNumber = "ORD-1042",
            productId = "p3",
            productNameEn = "Crispy Nashville Hot Chicken",
            customerId = "cust_2",
            customerName = "Layla Al-Otaibi",
            rating = 4,
            comment = "Great crunch and spice level, passionfruit lemonade balanced it nicely.",
            moderationStatus = ReviewModerationStatus.APPROVED
        )
    )

    val customerComplaints = listOf(
        CustomerComplaint(
            id = "cmp_01",
            ticketNumber = "TKT-901",
            orderId = "ord_demo_2",
            orderNumber = "ORD-1042",
            branchId = "branch_01",
            customerId = "cust_2",
            customerName = "Layla Al-Otaibi",
            category = "Missing Side Sauce",
            priority = ComplaintPriority.MEDIUM,
            status = ComplaintStatus.RESOLVED,
            subject = "Extra ranch cup was missing from takeaway bag",
            description = "Ordered extra herb ranch cup on the side but it wasn't inside the sealed bag.",
            managementResponse = "Apologized to guest and credited 150 loyalty points to account.",
            compensationOffered = "150 Loyalty Points ($3 value)"
        )
    )

    // --- Phase 8: Delivery Zones, Drivers, Active Delivery Assignments & Cash Settlements ---
    val deliveryZones = listOf(
        DeliveryZone(
            id = "dzone_01",
            branchId = "branch_01",
            code = "ZONE-A",
            nameEn = "Zone A • Olaya & Sulaimaniyah Core",
            nameAr = "المنطقة أ • العليا والسليمانية",
            boundaryDescription = "0.0 km – 4.5 km radius around Downtown Flagship",
            radiusKm = 4.5,
            deliveryFee = 3.50,
            minimumOrderAmount = 15.00,
            estimatedMinutes = 28
        ),
        DeliveryZone(
            id = "dzone_02",
            branchId = "branch_01",
            code = "ZONE-B",
            nameEn = "Zone B • KAFD, Al-Malqa & North Ring",
            nameAr = "المنطقة ب • المركز المالي والملقا",
            boundaryDescription = "4.5 km – 10.0 km radius (Express Highway corridor)",
            radiusKm = 10.0,
            deliveryFee = 5.50,
            minimumOrderAmount = 25.00,
            estimatedMinutes = 40
        ),
        DeliveryZone(
            id = "dzone_03",
            branchId = "branch_01",
            code = "ZONE-C",
            nameEn = "Zone C • Diplomatic Quarter & West",
            nameAr = "المنطقة ج • الحي الدبلوماسي والغرب",
            boundaryDescription = "10.0 km – 15.0 km extended perimeter",
            radiusKm = 15.0,
            deliveryFee = 7.50,
            minimumOrderAmount = 40.00,
            estimatedMinutes = 50
        )
    )

    val drivers = listOf(
        DeliveryDriver(
            id = "drv_01",
            branchId = "branch_01",
            fullName = "Tariq Ali",
            phone = "+966 50 666 7777",
            vehicleType = "MOTORCYCLE",
            isAvailable = false,
            activeDeliveries = 1,
            employeeId = "emp_06",
            vehiclePlate = "RYD-4821",
            shiftStatus = "ONLINE",
            driverStatus = "ON_DELIVERY",
            locationSharingConsented = true,
            currentLat = 24.7165,
            currentLng = 46.6812,
            cashCollectedUnsettled = 42.50,
            completedDeliveriesToday = 6,
            rating = 4.95
        ),
        DeliveryDriver(
            id = "drv_02",
            branchId = "branch_01",
            fullName = "Sami Al-Dosari",
            phone = "+966 55 888 9911",
            vehicleType = "CAR",
            isAvailable = true,
            activeDeliveries = 0,
            employeeId = "emp_06",
            vehiclePlate = "KSA-9034",
            shiftStatus = "ONLINE",
            driverStatus = "AVAILABLE",
            locationSharingConsented = true,
            currentLat = 24.7110,
            currentLng = 46.6740,
            cashCollectedUnsettled = 18.00,
            completedDeliveriesToday = 4,
            rating = 4.85
        ),
        DeliveryDriver(
            id = "drv_03",
            branchId = "branch_01",
            fullName = "Mazen Al-Qahtani",
            phone = "+966 54 712 3344",
            vehicleType = "MOTORCYCLE",
            isAvailable = false,
            activeDeliveries = 0,
            employeeId = "emp_06",
            vehiclePlate = "RYD-1129",
            shiftStatus = "ON_BREAK",
            driverStatus = "BREAK",
            locationSharingConsented = false,
            cashCollectedUnsettled = 0.0,
            completedDeliveriesToday = 5,
            rating = 4.78
        )
    )

    val initialDeliveryAssignments = listOf(
        DeliveryAssignment(
            id = "deliv_01",
            orderId = "ord_demo_1",
            orderNumber = "ORD-1041",
            branchId = "branch_01",
            deliveryZoneId = "dzone_01",
            deliveryZoneName = "Zone A • Olaya & Sulaimaniyah Core",
            driverId = "drv_01",
            driverName = "Tariq Ali",
            driverPhone = "+966 50 666 7777",
            vehicleInfo = "Motorcycle • RYD-4821",
            customerName = "Faisal Al-Rashid",
            customerPhone = "+966 55 101 2020",
            deliveryAddress = "7422 Olaya Main St, Villa 14, Gate B",
            deliveryNotes = "Ring intercom 14B",
            status = DeliveryWorkflowStatus.ON_ROUTE,
            orderTotalAmount = 28.60,
            paymentMethod = PaymentMethod.CASH,
            isCashOnDelivery = true,
            customerOtpCode = "4829",
            estimatedDeliveryMinutes = 28,
            dispatchedAt = System.currentTimeMillis() - (18 * 60_000L)
        ),
        DeliveryAssignment(
            id = "deliv_02",
            orderId = "ord_demo_3",
            orderNumber = "ORD-1043",
            branchId = "branch_01",
            deliveryZoneId = "dzone_02",
            deliveryZoneName = "Zone B • KAFD & Al-Malqa",
            driverId = null,
            driverName = null,
            driverPhone = null,
            customerName = "Layla Al-Otaibi",
            customerPhone = "+966 50 303 4040",
            deliveryAddress = "3190 Prince Muhammad Bin Saad Rd, Apt 9",
            deliveryNotes = "Call upon arrival",
            status = DeliveryWorkflowStatus.AWAITING_DRIVER,
            orderTotalAmount = 23.65,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            isCashOnDelivery = false,
            customerOtpCode = "7194",
            estimatedDeliveryMinutes = 35,
            dispatchedAt = System.currentTimeMillis() - (42 * 60_000L) // overdue highlight test
        )
    )

    // --- Phase 9: Warehouses, Unit Conversions, Stock Items, Ledger, Transfers, Suppliers & Purchasing ---
    val warehouses = listOf(
        Warehouse(
            id = "wh_central",
            branchId = "branch_01",
            code = "WH-CENTRAL-01",
            nameEn = "Central Commissary & Cold Hub",
            nameAr = "المستودع المركزي والتبريد",
            location = "Riyadh Industrial Logistics Park",
            isCentral = true,
            warehouseType = "CENTRAL",
            managerName = "Mira Hamoud"
        ),
        Warehouse(
            id = "wh_branch_01",
            branchId = "branch_01",
            code = "WH-BR01-MAIN",
            nameEn = "Downtown Branch Dry & Cold Store",
            nameAr = "مستودع فرع وسط المدينة",
            location = "Downtown Flagship Basement B1",
            isCentral = false,
            warehouseType = "BRANCH",
            managerName = "Karim Mansour"
        ),
        Warehouse(
            id = "wh_kitchen_01",
            branchId = "branch_01",
            code = "WH-KIT01-LINE",
            nameEn = "Hot Line Prep & Walk-In Chiller",
            nameAr = "مخزن خط المطبخ والثلاجة اليومية",
            location = "Main Kitchen Zone A",
            isCentral = false,
            warehouseType = "KITCHEN",
            managerName = "Chef Antoine Khalil"
        )
    )

    val unitConversionRules = listOf(
        UnitConversionRule("ucr_1", "ing_angus_patty", "Certified Angus Beef Patty", "شريحة لحم أنجوس", "Case (40 pcs)", "pcs", "pcs", 40.0, 1.0),
        UnitConversionRule("ucr_2", "ing_brioche_bun", "Artisan Brioche Bun", "خبز بريوش طازج", "Crate (24 pcs)", "pcs", "pcs", 24.0, 1.0),
        UnitConversionRule("ucr_3", "ing_truffle_aioli", "Black Truffle Aioli", "صوص أيولي بالكمأة", "Tub (2 kg)", "kg", "g", 2.0, 1000.0),
        UnitConversionRule("ucr_4", "ing_fries", "Skin-On Potato Fries", "بطاطس مقلية", "Carton (10 kg)", "kg", "g", 10.0, 1000.0)
    )

    val warehouseStockItems = listOf(
        WarehouseStockItem(
            id = "wsi_1",
            warehouseId = "wh_branch_01",
            warehouseName = "Downtown Branch Dry & Cold Store",
            branchId = "branch_01",
            itemCode = "ING-BEEF-150",
            itemId = "ing_angus_patty",
            itemNameEn = "Certified Angus Beef Patty (150g)",
            itemNameAr = "شريحة لحم أنجوس معتمد (150 جم)",
            stockUnit = "pcs",
            recipeUnit = "pcs",
            currentQuantity = 140.0,
            reservedQuantity = 12.0,
            minimumQuantity = 40.0,
            maximumQuantity = 300.0,
            reorderPoint = 50.0,
            unitCost = 3.20
        ),
        WarehouseStockItem(
            id = "wsi_2",
            warehouseId = "wh_branch_01",
            warehouseName = "Downtown Branch Dry & Cold Store",
            branchId = "branch_01",
            itemCode = "ING-BUN-01",
            itemId = "ing_brioche_bun",
            itemNameEn = "Artisan Brioche Bun",
            itemNameAr = "خبز بريوش طازج",
            stockUnit = "pcs",
            recipeUnit = "pcs",
            currentQuantity = 160.0,
            reservedQuantity = 10.0,
            minimumQuantity = 50.0,
            maximumQuantity = 300.0,
            reorderPoint = 60.0,
            unitCost = 0.65
        ),
        WarehouseStockItem(
            id = "wsi_3",
            warehouseId = "wh_branch_01",
            warehouseName = "Downtown Branch Dry & Cold Store",
            branchId = "branch_01",
            itemCode = "ING-TRUF-01",
            itemId = "ing_truffle_aioli",
            itemNameEn = "Black Truffle Aioli",
            itemNameAr = "صوص أيولي بالكمأة السوداء",
            stockUnit = "g",
            recipeUnit = "g",
            currentQuantity = 1800.0,
            reservedQuantity = 150.0,
            minimumQuantity = 1500.0,
            maximumQuantity = 10000.0,
            reorderPoint = 2000.0, // Low stock alert active!
            unitCost = 0.04
        ),
        WarehouseStockItem(
            id = "wsi_4",
            warehouseId = "wh_branch_01",
            warehouseName = "Downtown Branch Dry & Cold Store",
            branchId = "branch_01",
            itemCode = "ING-FRIES-01",
            itemId = "ing_fries",
            itemNameEn = "Skin-On Potato Fries",
            itemNameAr = "بطاطس مقلية بالقشر",
            stockUnit = "g",
            recipeUnit = "g",
            currentQuantity = 24000.0,
            reservedQuantity = 1000.0,
            minimumQuantity = 5000.0,
            maximumQuantity = 50000.0,
            reorderPoint = 8000.0,
            unitCost = 0.004
        ),
        WarehouseStockItem(
            id = "wsi_5",
            warehouseId = "wh_central",
            warehouseName = "Central Commissary & Cold Hub",
            branchId = "branch_01",
            itemCode = "ING-BEEF-150",
            itemId = "ing_angus_patty",
            itemNameEn = "Certified Angus Beef Patty (150g)",
            itemNameAr = "شريحة لحم أنجوس معتمد (150 جم)",
            stockUnit = "pcs",
            recipeUnit = "pcs",
            currentQuantity = 600.0,
            reservedQuantity = 40.0,
            minimumQuantity = 150.0,
            maximumQuantity = 1200.0,
            reorderPoint = 200.0,
            unitCost = 3.15
        )
    )

    val suppliers = listOf(
        Supplier(
            id = "sup_01",
            companyId = "comp_01",
            code = "SUP-PRIME-01",
            name = "Arabian Prime Meats & Cold Chain Co.",
            contactPerson = "khalid.s@arabianprime.sa",
            phone = "+966 11 400 8899",
            email = "orders@arabianprime.sa",
            paymentTerms = "NET_30",
            suppliedIngredientIds = listOf("ing_angus_patty", "ing_chicken_breast"),
            currentPayableBalance = 1280.00,
            rating = 4.9
        ),
        Supplier(
            id = "sup_02",
            companyId = "comp_01",
            code = "SUP-BAKERY-02",
            name = "Golden Harvest Artisan Bakery & Dairy",
            contactPerson = "nadia@goldenharvest.sa",
            phone = "+966 11 455 2211",
            email = "supply@goldenharvest.sa",
            paymentTerms = "NET_15",
            suppliedIngredientIds = listOf("ing_brioche_bun", "ing_swiss_cheese", "ing_truffle_aioli"),
            currentPayableBalance = 460.00,
            rating = 4.8
        )
    )

    val initialInventoryTransactions = listOf(
        InventoryTransaction(
            id = "itx_seed_1",
            referenceNumber = "OB-2026-1001",
            branchId = "branch_01",
            warehouseId = "wh_branch_01",
            warehouseName = "Downtown Branch Dry & Cold Store",
            itemId = "ing_angus_patty",
            itemNameEn = "Certified Angus Beef Patty (150g)",
            itemNameAr = "شريحة لحم أنجوس معتمد (150 جم)",
            transactionType = InventoryTransactionType.OPENING_BALANCE,
            quantityDelta = +150.0,
            unit = "pcs",
            unitCost = 3.20,
            balanceBefore = 0.0,
            balanceAfter = 150.0,
            sourceDocumentType = "OPENING_BALANCE",
            sourceDocumentId = "ob_01",
            performedBy = "Karim Mansour",
            notes = "Monthly opening balance verification"
        ),
        InventoryTransaction(
            id = "itx_seed_2",
            referenceNumber = "CONS-ORD-1041",
            branchId = "branch_01",
            warehouseId = "wh_branch_01",
            warehouseName = "Downtown Branch Dry & Cold Store",
            itemId = "ing_angus_patty",
            itemNameEn = "Certified Angus Beef Patty (150g)",
            itemNameAr = "شريحة لحم أنجوس معتمد (150 جم)",
            transactionType = InventoryTransactionType.SALE_CONSUMPTION,
            quantityDelta = -10.0,
            unit = "pcs",
            unitCost = 3.20,
            balanceBefore = 150.0,
            balanceAfter = 140.0,
            sourceDocumentType = "ORDER",
            sourceDocumentId = "ord_demo_1",
            performedBy = "Recipe Consumption Engine",
            notes = "Automatic recipe deduction for burger orders"
        )
    )

    val initialWarehouseTransfers = listOf(
        WarehouseTransfer(
            id = "trf_01",
            transferNumber = "TRF-2026-014",
            sourceWarehouseId = "wh_central",
            sourceWarehouseName = "Central Commissary & Cold Hub",
            destinationWarehouseId = "wh_branch_01",
            destinationWarehouseName = "Downtown Branch Dry & Cold Store",
            itemId = "ing_angus_patty",
            itemNameEn = "Certified Angus Beef Patty (150g)",
            itemNameAr = "شريحة لحم أنجوس معتمد (150 جم)",
            quantity = 80.0,
            unit = "pcs",
            unitCost = 3.15,
            status = WarehouseTransferStatus.SHIPPED,
            requestedBy = "Chef Antoine Khalil",
            approvedBy = "Mira Hamoud"
        )
    )

    val initialPurchaseOrders = listOf(
        PurchaseOrderDocument(
            id = "po_01",
            poNumber = "PO-2026-801",
            branchId = "branch_01",
            warehouseId = "wh_branch_01",
            warehouseName = "Downtown Branch Dry & Cold Store",
            supplierId = "sup_01",
            supplierName = "Arabian Prime Meats & Cold Chain Co.",
            stage = ProcurementStage.PURCHASE_ORDER,
            lines = listOf(
                PurchaseOrderLine(
                    itemId = "ing_angus_patty",
                    itemNameEn = "Certified Angus Beef Patty (150g)",
                    itemNameAr = "شريحة لحم أنجوس معتمد (150 جم)",
                    quantityOrdered = 100.0,
                    quantityReceived = 100.0,
                    purchaseUnit = "pcs",
                    stockConversionFactor = 1.0,
                    unitPrice = 3.20
                )
            ),
            subtotal = 320.00,
            taxAmount = 32.00,
            totalAmount = 352.00,
            notes = "Weekly cold-chain replenishment"
        )
    )

    val initialWasteRecords = listOf(
        WasteRecord(
            id = "wst_01",
            wasteNumber = "WST-101",
            branchId = "branch_01",
            warehouseId = "wh_kitchen_01",
            itemId = "ing_brioche_bun",
            itemNameEn = "Artisan Brioche Bun",
            itemNameAr = "خبز بريوش طازج",
            quantity = 4.0,
            unit = "pcs",
            unitCost = 0.65,
            reason = "Over-toasted on flat grill during lunch rush",
            reportedByEmployee = "Chef Marco Rossi"
        )
    )

    // --- Phase 10: Chart of Accounts, Cost Centers, Financial Periods, Journals, Bank & AR/AP ---
    val chartOfAccounts = listOf(
        ChartOfAccount("coa_1010", "1010", "Main Cash Drawer & Safe", "النقدية بالصندوق والخزينة", AccountType.ASSETS, balance = 4850.00),
        ChartOfAccount("coa_1020", "1020", "Corporate Operating Bank & Card Clearing", "الحساب البنكي التشغيلي وتسويات البطاقات", AccountType.ASSETS, balance = 42600.00),
        ChartOfAccount("coa_1100", "1100", "Accounts Receivable (Customers & Corporate)", "ذمم العملاء المدينة", AccountType.ASSETS, balance = 840.00),
        ChartOfAccount("coa_1200", "1200", "Food & Beverage Inventory Asset", "مخزون المواد الغذائية والمشروبات", AccountType.ASSETS, balance = 8420.00),
        ChartOfAccount("coa_2010", "2010", "Accounts Payable (Suppliers)", "ذمم الموردين الدائنة", AccountType.LIABILITIES, balance = 1740.00),
        ChartOfAccount("coa_2100", "2100", "VAT Output Tax Payable", "ضريبة القيمة المضافة المستحقة", AccountType.LIABILITIES, balance = 1420.00),
        ChartOfAccount("coa_3010", "3010", "Paid-In Capital & Retained Earnings", "رأس المال والأرباح المبقاة", AccountType.EQUITY, balance = 46000.00),
        ChartOfAccount("coa_4010", "4010", "Food & Beverage Sales Revenue", "إيرادات مبيعات الأطعمة والمشروبات", AccountType.REVENUE, balance = 14850.00),
        ChartOfAccount("coa_5010", "5010", "Cost of Goods Sold - Food & Packaging", "تكلفة البضاعة المباعة والمواد الخام", AccountType.COST_OF_SALES, balance = 4450.00),
        ChartOfAccount("coa_6010", "6010", "Branch Operating & Utility Expenses", "المصروفات التشغيلية والمرافق للفرع", AccountType.EXPENSES, balance = 2850.00)
    )

    val costCenters = listOf(
        CostCenter("cc_01", "CC-BRANCH-01", "Downtown Flagship Operations", "عمليات فرع وسط المدينة", "BRANCH", "branch_01", 30000.0),
        CostCenter("cc_02", "CC-KITCHEN-01", "Central Culinary & Hot Line", "المطبخ الرئيسي وخط التحضير", "KITCHEN", "branch_01", 18000.0),
        CostCenter("cc_03", "CC-DELIV-01", "Fleet & Delivery Dispatch", "أسطول التوصيل والتشغيل", "DELIVERY", "branch_01", 8000.0),
        CostCenter("cc_04", "CC-SUPPLY-01", "Warehousing & Cold Chain", "المستودعات وسلسلة الإمداد", "DEPARTMENT", "branch_01", 12000.0)
    )

    val financialPeriods = listOf(
        FinancialPeriod("fp_2026_09", "2026-09", "September 2026", "سبتمبر 2026", "2026-09-01", "2026-09-30", FinancialPeriodStatus.LOCKED, "Mira Hamoud"),
        FinancialPeriod("fp_2026_10", "2026-10", "October 2026 (Current)", "أكتوبر 2026 (الحالية)", "2026-10-01", "2026-10-31", FinancialPeriodStatus.OPEN, null)
    )

    val initialExpenses = listOf(
        Expense(
            id = "exp_01",
            branchId = "branch_01",
            category = "Utilities",
            description = "Commercial Kitchen Electricity & Chiller HVAC - Oct",
            amount = 640.00,
            approvedBy = "Karim Mansour",
            documentNumber = "EXP-1001",
            costCenterCode = "CC-KITCHEN-01",
            paymentMethod = PaymentMethod.BANK_TRANSFER
        ),
        Expense(
            id = "exp_02",
            branchId = "branch_01",
            category = "Maintenance",
            description = "Woodfire Pizza Oven Stone Calibration & Hood Filter Service",
            amount = 280.00,
            approvedBy = "Mira Hamoud",
            documentNumber = "EXP-1002",
            costCenterCode = "CC-KITCHEN-01",
            paymentMethod = PaymentMethod.CASH
        )
    )

    val initialJournalEntries = listOf(
        JournalEntry(
            id = "je_seed_sale_1042",
            entryNumber = "JE-ORD-1042",
            date = "2026-10-23",
            branchId = "branch_01",
            costCenterCode = "CC-BRANCH-01",
            sourceDocumentType = "ORDER",
            sourceDocumentId = "ord_demo_2",
            referenceNumber = "ORD-1042",
            descriptionEn = "POS Dine-In Sale & Recipe COGS for ORD-1042",
            descriptionAr = "قيد مبيعات وتكلفة مخزون للطلب ORD-1042",
            lines = listOf(
                JournalLine("jl_1", "1010", "Main Cash Drawer & Safe", "النقدية بالصندوق والخزينة", AccountType.ASSETS, debit = 38.50, credit = 0.0),
                JournalLine("jl_2", "4010", "Food & Beverage Sales Revenue", "إيرادات مبيعات الأطعمة والمشروبات", AccountType.REVENUE, debit = 0.0, credit = 35.00),
                JournalLine("jl_3", "2100", "VAT Output Tax Payable", "ضريبة القيمة المضافة المستحقة", AccountType.LIABILITIES, debit = 0.0, credit = 3.50),
                JournalLine("jl_4", "5010", "Cost of Goods Sold - Food & Packaging", "تكلفة البضاعة المباعة", AccountType.COST_OF_SALES, debit = 11.20, credit = 0.0),
                JournalLine("jl_5", "1200", "Food & Beverage Inventory Asset", "مخزون المواد الغذائية", AccountType.ASSETS, debit = 0.0, credit = 11.20)
            )
        ),
        JournalEntry(
            id = "je_seed_exp_1001",
            entryNumber = "JE-EXP-1001",
            date = "2026-10-23",
            branchId = "branch_01",
            costCenterCode = "CC-KITCHEN-01",
            sourceDocumentType = "EXPENSE",
            sourceDocumentId = "exp_01",
            referenceNumber = "EXP-1001",
            descriptionEn = "Commercial Kitchen Electricity & Chiller HVAC",
            descriptionAr = "كهرباء المطبخ والتبريد المركزي",
            lines = listOf(
                JournalLine("jl_6", "6010", "Branch Operating & Utility Expenses", "المصروفات التشغيلية", AccountType.EXPENSES, debit = 640.00, credit = 0.0),
                JournalLine("jl_7", "1020", "Corporate Operating Bank & Card Clearing", "الحساب البنكي التشغيلي", AccountType.ASSETS, debit = 0.0, credit = 640.00)
            )
        )
    )

    val bankAccounts = listOf(
        BankAccount(
            id = "bank_01",
            accountNameEn = "Al-Haramain Corporate Operating Account",
            accountNameAr = "الحساب الجاري التشغيلي لشركة الحرمين",
            bankName = "Saudi National Bank (SNB)",
            ibanNumber = "SA44 1000 0001 2345 6789 0001",
            glAccountCode = "1020",
            currentBalance = 42600.00
        ),
        BankAccount(
            id = "bank_02",
            accountNameEn = "POS Card & Mada Settlement Escrow",
            accountNameAr = "حساب تسويات مدى والبطاقات الائتمانية",
            bankName = "Al Rajhi Corporate Bank",
            ibanNumber = "SA88 8000 0009 8765 4321 0002",
            glAccountCode = "1020",
            currentBalance = 15840.00
        )
    )

    val subledgerInvoices = listOf(
        SubledgerInvoice(
            id = "inv_ap_01",
            invoiceNumber = "SINV-2026-881",
            ledgerType = "PAYABLE",
            partyId = "sup_01",
            partyName = "Arabian Prime Meats & Cold Chain Co.",
            sourceDocumentId = "po_01",
            issueDate = "2026-10-01",
            dueDate = "2026-10-31",
            daysPastDue = 18,
            totalAmount = 1280.00,
            paidAmount = 400.00
        ),
        SubledgerInvoice(
            id = "inv_ap_02",
            invoiceNumber = "SINV-2026-740",
            ledgerType = "PAYABLE",
            partyId = "sup_02",
            partyName = "Golden Harvest Artisan Bakery & Dairy",
            sourceDocumentId = "po_01",
            issueDate = "2026-08-25",
            dueDate = "2026-09-10",
            daysPastDue = 43,
            totalAmount = 460.00,
            paidAmount = 0.0
        ),
        SubledgerInvoice(
            id = "inv_ar_01",
            invoiceNumber = "CINV-2026-309",
            ledgerType = "RECEIVABLE",
            partyId = "cust_1",
            partyName = "Faisal Al-Rashid (Corporate Catering)",
            sourceDocumentId = "ord_demo_1",
            issueDate = "2026-10-10",
            dueDate = "2026-10-25",
            daysPastDue = 12,
            totalAmount = 840.00,
            paidAmount = 300.00
        )
    )

    // --- Phase 11: HR, Attendance, Shift Templates, Schedules, Leave & Payroll ---
    val shiftTemplates = listOf(
        ShiftTemplate("stpl_morning", "branch_01", "SHIFT-AM", "Morning Prep & Lunch (08:00 - 16:00)", "وردية الصباح والغداء", "08:00", "16:00", 30, "Kitchen & FOH"),
        ShiftTemplate("stpl_evening", "branch_01", "SHIFT-PM", "Prime Dinner Service (16:00 - 00:00)", "وردية المساء والعشاء", "16:00", "00:00", 30, "All Departments"),
        ShiftTemplate("stpl_fleet", "branch_01", "SHIFT-DRV", "Peak Delivery Wave (12:00 - 22:00)", "وردية الذروة للتوصيل", "12:00", "22:00", 45, "Delivery Fleet")
    )

    val attendanceRecords = listOf(
        AttendanceRecord(
            id = "att_01",
            employeeId = "emp_03",
            employeeNumber = "EMP-1003",
            employeeName = "Youssef Zaid",
            department = "Front of House (POS)",
            branchId = "branch_01",
            date = "2026-10-23",
            scheduledShiftName = "Morning Prep & Lunch (08:00 - 16:00)",
            checkInTime = "07:56",
            checkOutTime = "16:15",
            breakMinutes = 30,
            lateMinutes = 0,
            overtimeMinutes = 15,
            status = "PRESENT",
            source = AttendanceVerificationSource.TERMINAL_CAMERA
        ),
        AttendanceRecord(
            id = "att_02",
            employeeId = "emp_04",
            employeeNumber = "EMP-1004",
            employeeName = "Sarah Nader",
            department = "Front of House (Waiter)",
            branchId = "branch_01",
            date = "2026-10-23",
            scheduledShiftName = "Morning Prep & Lunch (08:00 - 16:00)",
            checkInTime = "08:14",
            checkOutTime = null,
            breakMinutes = 30,
            lateMinutes = 14,
            overtimeMinutes = 0,
            status = "LATE",
            source = AttendanceVerificationSource.FINGERPRINT_DEVICE_API
        ),
        AttendanceRecord(
            id = "att_03",
            employeeId = "emp_05",
            employeeNumber = "EMP-1005",
            employeeName = "Chef Antoine Khalil",
            department = "Kitchen",
            branchId = "branch_01",
            date = "2026-10-23",
            scheduledShiftName = "Morning Prep & Lunch (08:00 - 16:00)",
            checkInTime = "07:45",
            checkOutTime = null,
            breakMinutes = 30,
            lateMinutes = 0,
            overtimeMinutes = 45,
            status = "OVERTIME",
            source = AttendanceVerificationSource.TERMINAL_CAMERA
        ),
        AttendanceRecord(
            id = "att_04",
            employeeId = "emp_06",
            employeeNumber = "EMP-1006",
            employeeName = "Tariq Ali",
            department = "Delivery",
            branchId = "branch_01",
            date = "2026-10-23",
            scheduledShiftName = "Peak Delivery Wave (12:00 - 22:00)",
            checkInTime = "11:55",
            checkOutTime = null,
            breakMinutes = 45,
            lateMinutes = 0,
            overtimeMinutes = 0,
            status = "PRESENT",
            source = AttendanceVerificationSource.MOBILE_CAMERA_VERIFICATION
        )
    )

    val leaveRequests = listOf(
        LeaveRequest(
            id = "lv_01",
            employeeId = "emp_04",
            employeeName = "Sarah Nader",
            department = "Front of House",
            branchId = "branch_01",
            leaveType = LeaveType.ANNUAL,
            startDate = "2026-11-10",
            endDate = "2026-11-14",
            daysCount = 5,
            reason = "Family wedding travel",
            status = "PENDING",
            remainingBalanceDays = 16
        ),
        LeaveRequest(
            id = "lv_02",
            employeeId = "emp_03",
            employeeName = "Youssef Zaid",
            department = "Front of House",
            branchId = "branch_01",
            leaveType = LeaveType.SICK,
            startDate = "2026-10-05",
            endDate = "2026-10-06",
            daysCount = 2,
            reason = "Medical dental procedure",
            status = "APPROVED",
            approvedBy = "Karim Mansour",
            remainingBalanceDays = 12
        )
    )

    val payrollRecords = listOf(
        PayrollRecord("pay_01", "2026-10", "branch_01", "emp_01", "EMP-1001", "Mira Hamoud", "Management", "Chief Operating Officer", 8500.0, 2125.0, 600.0, 0.0, 0.0, 0.0, "PAID", "Mira Hamoud"),
        PayrollRecord("pay_02", "2026-10", "branch_01", "emp_02", "EMP-1002", "Karim Mansour", "Management", "Branch General Manager", 5800.0, 1450.0, 450.0, 120.0, 0.0, 0.0, "APPROVED", "Mira Hamoud"),
        PayrollRecord("pay_03", "2026-10", "branch_01", "emp_03", "EMP-1003", "Youssef Zaid", "Front of House", "Senior Cashier & Till Lead", 3200.0, 800.0, 300.0, 85.0, 0.0, 150.0, "APPROVED", "Karim Mansour"),
        PayrollRecord("pay_04", "2026-10", "branch_01", "emp_04", "EMP-1004", "Sarah Nader", "Front of House", "Dining Room Captain", 2900.0, 725.0, 280.0, 40.0, 25.0, 0.0, "PENDING_APPROVAL", null),
        PayrollRecord("pay_05", "2026-10", "branch_01", "emp_05", "EMP-1005", "Chef Antoine Khalil", "Kitchen", "Executive Sous Chef", 5200.0, 1300.0, 400.0, 240.0, 0.0, 0.0, "APPROVED", "Karim Mansour"),
        PayrollRecord("pay_06", "2026-10", "branch_01", "emp_06", "EMP-1006", "Tariq Ali", "Delivery", "Fleet Courier", 2600.0, 650.0, 350.0, 110.0, 0.0, 0.0, "APPROVED", "Karim Mansour")
    )

    val centralNotificationEvents = listOf(
        CentralNotificationEvent(
            id = "cne_01",
            eventTrigger = "ORDER_CREATED",
            titleEn = "Order ORD-1041 Created & Routed",
            titleAr = "تم إنشاء وتوجيه الطلب ORD-1041",
            messageEn = "Dispatched to Grill & Fryer stations; customer tracking link active.",
            messageAr = "تم التوجيه لمحطة الشواء والقلي؛ رابط تتبع العميل نشط.",
            channelsDispatched = listOf(NotificationChannelType.IN_APP, NotificationChannelType.PUSH_READY, NotificationChannelType.WHATSAPP_READY),
            referenceId = "ORD-1041"
        ),
        CentralNotificationEvent(
            id = "cne_02",
            eventTrigger = "LOW_STOCK",
            titleEn = "Low Stock Alert: Black Truffle Aioli",
            titleAr = "تنبيه انخفاض المخزون: صوص أيولي بالكمأة",
            messageEn = "Available stock (1,650g) is below reorder point (2,000g) in Downtown Branch Store.",
            messageAr = "الكمية المتاحة أقل من نقطة إعادة الطلب في مستودع الفرع.",
            channelsDispatched = listOf(NotificationChannelType.IN_APP, NotificationChannelType.EMAIL_READY),
            referenceId = "ING-TRUF-01"
        ),
        CentralNotificationEvent(
            id = "cne_03",
            eventTrigger = "DRIVER_ASSIGNED",
            titleEn = "Driver Tariq Ali Assigned to ORD-1041",
            titleAr = "تم تعيين السائق طارق علي للطلب ORD-1041",
            messageEn = "Customer OTP 4829 generated and dispatched via SMS-Ready gateway.",
            messageAr = "تم توليد رمز التحقق 4829 وإرساله عبر بوابة الرسائل.",
            channelsDispatched = listOf(NotificationChannelType.IN_APP, NotificationChannelType.SMS_READY, NotificationChannelType.PUSH_READY),
            referenceId = "deliv_01"
        )
    )
}
