package com.example.ui.shell

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.Translations
import com.example.core.validation.Validators
import com.example.designsystem.*
import com.example.domain.model.Company
import com.example.domain.model.User
import com.example.domain.model.UserSession
import com.example.ui.theme.RestoAmber500
import com.example.ui.theme.RestoNavy900
import com.example.ui.theme.RestoRose600
import java.text.SimpleDateFormat
import java.util.*

data class DemoRole(
    val id: String,
    val title: String,
    val description: String
)

val DEMO_ROLES = listOf(
    DemoRole("ROLE_ADMIN", "System Administrator", "Full tenant & security access"),
    DemoRole("ROLE_MANAGER", "Branch General Manager", "Operations, refunds & approvals"),
    DemoRole("ROLE_CASHIER", "Lead Cashier (POS)", "Order taking & cash drawer reconciliation"),
    DemoRole("ROLE_WAITER", "Floor Waiter / Server", "Table ordering, guest service & billing"),
    DemoRole("ROLE_CHEF", "Executive Chef", "Kitchen KDS ticketing & recipe management"),
    DemoRole("ROLE_DRIVER", "Delivery Driver", "Dispatch routing & customer drop-off")
)

@Composable
fun UserMenuDialog(
    visible: Boolean,
    user: User,
    company: Company,
    session: UserSession?,
    language: AppLanguage,
    onRoleSwitch: (roleId: String, title: String) -> Unit,
    onOpenLogin: () -> Unit,
    onLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    RestoModalDialog(
        onDismissRequest = onDismiss,
        title = Translations.get("user_profile", language)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = RestoAmber500,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = user.fullName.take(1).uppercase(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = RestoNavy900
                        )
                    }
                }
                Column {
                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RestoBadge(text = user.roleId, variant = BadgeVariant.INFO)
                        if (session != null) {
                            RestoBadge(
                                text = "Expires ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(session.expiresAt))}",
                                variant = BadgeVariant.SUCCESS
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = Translations.get("quick_role_switch", language),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
        ) {
            items(DEMO_ROLES) { role ->
                val isCurrent = role.id == user.roleId
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRoleSwitch(role.id, role.title) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = role.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = role.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isCurrent) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            RestoButton(
                onClick = onOpenLogin,
                variant = RestoButtonVariant.OUTLINE,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Switch Tenant")
            }

            RestoButton(
                onClick = onLogout,
                variant = RestoButtonVariant.DANGER,
                modifier = Modifier.weight(1f),
                testTag = "user_logout_button"
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(Translations.get("logout", language))
            }
        }
    }
}

@Composable
fun LoginDialog(
    visible: Boolean,
    currentCompanyCode: String,
    loginError: String?,
    language: AppLanguage,
    onLoginSubmit: (companyCode: String, username: String, passwordAttempt: String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    var companyCode by remember(currentCompanyCode) { mutableStateOf(currentCompanyCode) }
    var username by remember { mutableStateOf("admin_mira") }
    var password by remember { mutableStateOf("password123") }
    var validationError by remember { mutableStateOf<String?>(null) }

    RestoModalDialog(
        onDismissRequest = onDismiss,
        title = Translations.get("login", language)
    ) {
        Text(
            text = Translations.get("login_subtitle", language),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        RestoTextInput(
            value = companyCode,
            onValueChange = {
                companyCode = it
                validationError = null
            },
            label = Translations.get("company_code", language),
            placeholder = "e.g., AL-HAYA",
            leadingIcon = Icons.Default.Business,
            testTag = "login_company_input"
        )

        Spacer(modifier = Modifier.height(10.dp))

        RestoTextInput(
            value = username,
            onValueChange = {
                username = it
                validationError = null
            },
            label = Translations.get("username_or_email", language),
            leadingIcon = Icons.Default.Person,
            testTag = "login_username_input"
        )

        Spacer(modifier = Modifier.height(10.dp))

        RestoTextInput(
            value = password,
            onValueChange = {
                password = it
                validationError = null
            },
            label = Translations.get("password", language),
            leadingIcon = Icons.Default.Lock,
            testTag = "login_password_input"
        )

        val activeError = loginError ?: validationError
        if (activeError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = RestoRose600, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = activeError,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        RestoButton(
            onClick = {
                val compValidation = Validators.validateCompanyCode(companyCode)
                val userValidation = Validators.validateUsername(username)
                if (!compValidation.isValid) {
                    validationError = if (language == AppLanguage.ARABIC) "رمز المؤسسة غير صالح" else "Invalid company code format"
                } else if (!userValidation.isValid) {
                    validationError = if (language == AppLanguage.ARABIC) "اسم المستخدم مطلوب" else "Username is required"
                } else {
                    onLoginSubmit(companyCode, username, password)
                }
            },
            variant = RestoButtonVariant.PRIMARY,
            modifier = Modifier.fillMaxWidth(),
            testTag = "submit_login_button"
        ) {
            Text(Translations.get("sign_in_button", language))
        }
    }
}
