package com.example.core.constants

object AppConstants {
    const val PLATFORM_NAME = "RestoOps ERP"
    const val VERSION = "12.0.0"
    const val PHASE = "Phase 12 - Full Enterprise ERP & Production Readiness"
    const val DEFAULT_CURRENCY = "USD"
    const val DEFAULT_CURRENCY_SYMBOL = "$"
    const val DEFAULT_TAX_RATE = 0.10 // 10% VAT / Sales tax
    const val DEFAULT_SERVICE_FEE_RATE = 0.05 // 5% Dine-In service fee when enabled
    const val DEFAULT_DELIVERY_FEE = 3.50 // Default delivery fee

    val SUPPORTED_LANGUAGES = listOf(
        LanguageOption("en", "English", "EN", isRtl = false),
        LanguageOption("ar", "العربية", "AR", isRtl = true)
    )

    const val TIMEOUT_API_MS = 10_000L
    const val LOW_STOCK_THRESHOLD = 5.0
}

data class LanguageOption(
    val code: String,
    val nativeName: String,
    val shortBadge: String,
    val isRtl: Boolean
)

enum class AppThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

enum class NavigationSection(val id: String) {
    DASHBOARD("dashboard"),
    MENU_CATALOG("menu_catalog"),
    ADMIN_PORTAL("admin"),
    POS("pos"),
    TABLES("tables"),
    KITCHEN("kitchen"),
    CUSTOMER_PORTAL("customer_portal"),
    DELIVERY_DISPATCH("delivery_dispatch"),
    INVENTORY("inventory"),
    ACCOUNTING("accounting"),
    HR_PAYROLL("hr_payroll"),
    REPORTS_ANALYTICS("reports_analytics"),
    DOMAIN_MODELS("domain_models"),
    AUDIT_LOGS("audit_logs"),
    UNAUTHORIZED("unauthorized")
}
