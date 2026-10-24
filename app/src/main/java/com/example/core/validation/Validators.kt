package com.example.core.validation

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val messageEn: String, val messageAr: String) : ValidationResult()

    val isValid: Boolean get() = this is Valid
}

object Validators {
    fun validateCompanyCode(code: String): ValidationResult {
        val trimmed = code.trim()
        return when {
            trimmed.isEmpty() -> ValidationResult.Invalid("Company code cannot be empty", "رمز المؤسسة مطلوب")
            trimmed.length < 3 -> ValidationResult.Invalid("Company code must be at least 3 characters", "يجب أن يتكون رمز المؤسسة من 3 أحرف على الأقل")
            !trimmed.matches(Regex("^[a-zA-Z0-9_-]+$")) -> ValidationResult.Invalid("Company code contains invalid characters", "رمز المؤسسة يحتوي على رموز غير صالحة")
            else -> ValidationResult.Valid
        }
    }

    fun validateUsername(username: String): ValidationResult {
        val trimmed = username.trim()
        return when {
            trimmed.isEmpty() -> ValidationResult.Invalid("Username or email is required", "اسم المستخدم أو البريد الإلكتروني مطلوب")
            trimmed.length < 3 -> ValidationResult.Invalid("Username must be at least 3 characters", "اسم المستخدم يجب أن يكون 3 أحرف على الأقل")
            else -> ValidationResult.Valid
        }
    }

    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isEmpty() -> ValidationResult.Invalid("Password is required", "كلمة المرور مطلوبة")
            password.length < 4 -> ValidationResult.Invalid("Password must be at least 4 characters", "كلمة المرور يجب أن تكون 4 أحرف على الأقل")
            else -> ValidationResult.Valid
        }
    }

    fun validatePositiveAmount(amount: Double, fieldName: String = "Amount"): ValidationResult {
        return if (amount >= 0.0) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid("$fieldName must be non-negative", "يجب أن تكون القيمة موجبة")
        }
    }

    fun validateQuantity(qty: Int): ValidationResult {
        return if (qty > 0) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid("Quantity must be greater than 0", "الكمية يجب أن تكون أكبر من صفر")
        }
    }

    fun validateSku(sku: String): ValidationResult {
        val trimmed = sku.trim()
        return if (trimmed.isNotEmpty() && trimmed.length in 3..30) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid("Invalid SKU format", "رمز الصنف (SKU) غير صالح")
        }
    }
}
