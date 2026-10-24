package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.Translations
import com.example.designsystem.*
import com.example.domain.model.AuditLog
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AuditScreen(
    auditLogs: List<AuditLog>,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("audit_screen")
    ) {
        Column {
            Text(
                text = Translations.get("nav_audit_logs", language),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Immutable compliance log tracking operations, branch switches, logins, and orders",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val columns = listOf(
            TableColumn<AuditLog>(
                title = "Timestamp",
                width = 120.dp
            ) { log ->
                Text(
                    text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            TableColumn<AuditLog>(
                title = "Action",
                width = 130.dp
            ) { log ->
                val variant = when (log.action) {
                    "CREATE_ORDER" -> BadgeVariant.SUCCESS
                    "PAY_ORDER" -> BadgeVariant.INFO
                    "SWITCH_BRANCH" -> BadgeVariant.WARNING
                    "SYSTEM_INIT" -> BadgeVariant.NEUTRAL
                    else -> BadgeVariant.INFO
                }
                RestoBadge(text = log.action, variant = variant)
            },
            TableColumn<AuditLog>(
                title = "Entity",
                width = 90.dp
            ) { log ->
                Text(log.entityName, fontWeight = FontWeight.Bold)
            },
            TableColumn<AuditLog>(
                title = "User",
                width = 130.dp
            ) { log ->
                Text(log.userName, style = MaterialTheme.typography.bodySmall)
            },
            TableColumn<AuditLog>(
                title = "Operational Details",
                width = 240.dp
            ) { log ->
                Text(
                    text = log.details,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 16.sp
                )
            }
        )

        RestoDataTable(
            items = auditLogs,
            columns = columns,
            minTableWidth = 710.dp,
            testTag = "audit_data_table"
        )
    }
}
