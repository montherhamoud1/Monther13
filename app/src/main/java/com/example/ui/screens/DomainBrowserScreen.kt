package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Schema
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.Translations
import com.example.designsystem.*

data class DomainModelMeta(
    val number: Int,
    val name: String,
    val descriptionEn: String,
    val descriptionAr: String,
    val primaryFields: List<String>,
    val relationships: String,
    val phaseScope: String
)

val DOMAIN_MODELS_LIST = listOf(
    DomainModelMeta(1, "Company", "Multi-tenant top level enterprise entity", "الكيان المؤسسي الأعلى متعدد المستأجرين", listOf("id", "code", "nameEn", "nameAr", "taxNumber", "defaultCurrency"), "1:N Brands, 1:N Users, 1:N Suppliers", "Core Phase 1-12"),
    DomainModelMeta(2, "Brand", "Commercial brand concept operating under company", "العلامة التجارية المشغلة تحت الشركة", listOf("id", "companyId", "code", "nameEn", "nameAr", "status"), "N:1 Company, 1:N Branches, 1:N Categories", "Core Phase 1-12"),
    DomainModelMeta(3, "Branch", "Physical restaurant unit or cloud kitchen hub", "الفرع التشغيلي أو المطبخ السحابي", listOf("id", "brandId", "code", "nameEn", "nameAr", "phone", "address", "seatingCapacity"), "N:1 Brand, 1:N Tables, 1:N Orders, 1:N Employees", "Core Phase 1-12"),
    DomainModelMeta(4, "User", "System user with multi-branch RBAC credentials", "مستخدم النظام وصلاحيات الوصول متعدد الفروع", listOf("id", "companyId", "username", "email", "fullName", "roleId", "activeBranchId"), "N:1 Role, N:1 Company, 1:N AuditLogs", "Security & Auth"),
    DomainModelMeta(5, "Role", "Enterprise RBAC role definition", "تعريف الدور الوظيفي بنظام الصلاحيات", listOf("id", "code", "nameEn", "nameAr", "permissions[]"), "1:N Users, M:N Permissions", "Security & Auth"),
    DomainModelMeta(6, "Permission", "Granular functional operational grant", "إذن صلاحية تشغيلية دقيقة", listOf("id", "code", "module", "descriptionEn", "descriptionAr"), "M:N Roles", "Security & Auth"),
    DomainModelMeta(7, "Employee", "Staff HR record linked to branch payroll", "سجل الموظف والموارد البشرية والرواتب", listOf("id", "branchId", "userId", "employeeNumber", "jobTitle", "hourlyRate"), "N:1 Branch, 1:1 User", "HR & Attendance"),
    DomainModelMeta(8, "Customer", "Guest profile, contact, and loyalty wallet", "ملف العميل وسجل نقاط الولاء", listOf("id", "companyId", "name", "phone", "email", "loyaltyPoints", "tier"), "1:N Orders, 1:N LoyaltyTransactions", "CRM & Loyalty"),
    DomainModelMeta(9, "Category", "Menu taxonomy grouping dishes & beverages", "تصنيف قائمة الطعام والمشروبات", listOf("id", "brandId", "nameEn", "nameAr", "sortOrder", "colorHex"), "1:N Products, N:1 Brand", "Menu & Catalog"),
    DomainModelMeta(10, "Product", "Sellable dish, ingredient bundle, or beverage", "المنتج الغذائي أو الصنف القابل للبيع", listOf("id", "categoryId", "sku", "nameEn", "nameAr", "price", "costPrice", "currentStock"), "N:1 Category, 1:N Recipes, 1:N OrderItems", "Catalog & Inventory"),
    DomainModelMeta(11, "Order", "Sales transaction across POS, online, or tables", "معاملة البيع من الكاشير أو الطاولات أو التوصيل", listOf("id", "branchId", "orderNumber", "orderType", "tableId", "status", "totalAmount"), "1:N OrderItems, 1:N Payments, N:1 Table", "POS & Ordering"),
    DomainModelMeta(12, "OrderItem", "Line item entry with modifiers and kitchen notes", "الصنف المضاف في الفاتورة مع الملاحظات", listOf("id", "orderId", "productId", "quantity", "unitPrice", "totalPrice", "kitchenStatus"), "N:1 Order, N:1 Product", "POS & KDS"),
    DomainModelMeta(13, "Table", "Dining room physical seating station", "طاولة الصالة المادية والموقع والمقاعد", listOf("id", "branchId", "tableNumber", "capacity", "section", "status", "currentOrderId"), "N:1 Branch, 1:1 ActiveOrder", "Table & Floor"),
    DomainModelMeta(14, "Kitchen", "KDS station routing and prep tickets", "شاشة المطبخ وتوزيع التذاكر ومحطات الطهي", listOf("id", "branchId", "orderId", "stationName", "itemsSummary", "status", "elapsedMinutes"), "N:1 Order, N:1 Branch", "Kitchen Display"),
    DomainModelMeta(15, "Driver", "Delivery fleet driver personnel and vehicle", "سائق التوصيل ونوع المركبة وحالة الطلبات", listOf("id", "branchId", "fullName", "phone", "vehicleType", "activeDeliveries"), "N:1 Branch, 1:N Deliveries", "Delivery Fleet"),
    DomainModelMeta(16, "Warehouse", "Storage depot holding raw par inventory", "مستودع تخزين المواد الخام والمخزون", listOf("id", "branchId", "code", "nameEn", "nameAr", "location", "isCentral"), "N:1 Branch, 1:N InventoryStock", "Inventory"),
    DomainModelMeta(17, "Supplier", "Raw food & beverage procurement partner", "المورد المعتمد للمواد الغذائية والتوريد", listOf("id", "companyId", "code", "name", "contactPerson", "phone", "paymentTerms"), "1:N PurchaseOrders, N:1 Company", "Purchasing"),
    DomainModelMeta(18, "Payment", "Financial settlement transaction", "معاملة السداد المالي وطريقة الدفع", listOf("id", "orderId", "branchId", "amount", "paymentMethod", "referenceNumber"), "N:1 Order, N:1 Branch", "Accounting & Cash"),
    DomainModelMeta(19, "Expense", "Branch operating petty cash / operational costs", "المصروفات التشغيلية والنثريات اليومية", listOf("id", "branchId", "category", "description", "amount", "approvedBy"), "N:1 Branch", "Accounting"),
    DomainModelMeta(20, "AuditLog", "Immutable security and operational event trail", "سجل التدقيق والأمان غير القابل للتعديل", listOf("id", "companyId", "branchId", "userId", "action", "entityName", "details", "timestamp"), "System-wide tracking", "Audit & Security")
)

@Composable
fun DomainBrowserScreen(
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    var selectedModel by remember { mutableStateOf<DomainModelMeta?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("domain_models_screen")
    ) {
        Column {
            Text(
                text = Translations.get("domain_explorer_title", language),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = Translations.get("domain_explorer_desc", language),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(DOMAIN_MODELS_LIST) { model ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedModel = model }
                        .testTag("domain_card_${model.name}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${model.number}",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = model.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    RestoBadge(text = model.phaseScope, variant = BadgeVariant.INFO)
                                }
                                Text(
                                    text = if (language == AppLanguage.ARABIC) model.descriptionAr else model.descriptionEn,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Details",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Model Schema Inspector Modal
    if (selectedModel != null) {
        val model = selectedModel!!
        RestoModalDialog(
            onDismissRequest = { selectedModel = null },
            title = "Domain Entity: ${model.name}"
        ) {
            Text(
                text = if (language == AppLanguage.ARABIC) model.descriptionAr else model.descriptionEn,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Key Entity Schema Properties:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                model.primaryFields.forEach { field ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "• $field",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Relational Graph:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = model.relationships,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            RestoButton(
                onClick = { selectedModel = null },
                variant = RestoButtonVariant.OUTLINE,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close Inspector")
            }
        }
    }
}
