package com.example.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.constants.NavigationSection
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.Translations
import com.example.ui.theme.RestoAmber500
import com.example.ui.theme.RestoNavy900

data class NavigationItem(
    val section: NavigationSection,
    val translationKey: String,
    val icon: ImageVector,
    val badgeCount: Int? = null
)

val NAV_ITEMS = listOf(
    NavigationItem(NavigationSection.DASHBOARD, "nav_dashboard", Icons.Default.Dashboard),
    NavigationItem(NavigationSection.POS, "nav_pos", Icons.Default.PointOfSale),
    NavigationItem(NavigationSection.TABLES, "nav_tables", Icons.Default.TableRestaurant),
    NavigationItem(NavigationSection.KITCHEN, "nav_kitchen", Icons.Default.Kitchen),
    NavigationItem(NavigationSection.CUSTOMER_PORTAL, "nav_customer_portal", Icons.Default.PhoneIphone),
    NavigationItem(NavigationSection.DELIVERY_DISPATCH, "nav_delivery_dispatch", Icons.Default.DeliveryDining),
    NavigationItem(NavigationSection.INVENTORY, "nav_inventory", Icons.Default.Inventory2),
    NavigationItem(NavigationSection.ACCOUNTING, "nav_accounting", Icons.Default.AccountBalance),
    NavigationItem(NavigationSection.HR_PAYROLL, "nav_hr_payroll", Icons.Default.Badge),
    NavigationItem(NavigationSection.REPORTS_ANALYTICS, "nav_reports_analytics", Icons.Default.Analytics),
    NavigationItem(NavigationSection.MENU_CATALOG, "nav_menu_catalog", Icons.Default.RestaurantMenu),
    NavigationItem(NavigationSection.ADMIN_PORTAL, "nav_admin", Icons.Default.AdminPanelSettings),
    NavigationItem(NavigationSection.DOMAIN_MODELS, "nav_domain_models", Icons.Default.Schema),
    NavigationItem(NavigationSection.AUDIT_LOGS, "nav_audit_logs", Icons.Default.Security)
)

@Composable
fun RestoNavigationRail(
    currentSection: NavigationSection,
    language: AppLanguage,
    onSectionSelected: (NavigationSection) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .testTag("resto_navigation_rail"),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        NAV_ITEMS.forEach { item ->
            val isSelected = item.section == currentSection
            NavigationRailItem(
                selected = isSelected,
                onClick = { onSectionSelected(item.section) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (item.badgeCount != null) {
                                Badge { Text(item.badgeCount.toString()) }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = Translations.get(item.translationKey, language)
                        )
                    }
                },
                label = {
                    Text(
                        text = Translations.get(item.translationKey, language),
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                },
                modifier = Modifier.testTag("nav_item_${item.section.id}")
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun RestoDrawerContent(
    currentSection: NavigationSection,
    language: AppLanguage,
    companyName: String,
    brandName: String,
    onSectionSelected: (NavigationSection) -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier.widthIn(max = 300.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = companyName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = brandName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            NAV_ITEMS.forEach { item ->
                val isSelected = item.section == currentSection
                NavigationDrawerItem(
                    icon = { Icon(item.icon, contentDescription = null) },
                    label = {
                        Text(
                            text = Translations.get(item.translationKey, language),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    selected = isSelected,
                    onClick = { onSectionSelected(item.section) },
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .testTag("drawer_item_${item.section.id}")
                )
            }
        }
    }
}

@Composable
fun RestoBottomBar(
    currentSection: NavigationSection,
    language: AppLanguage,
    onSectionSelected: (NavigationSection) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.testTag("resto_bottom_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        // Show primary 5 items for compact mobile bottom bar
        NAV_ITEMS.take(5).forEach { item ->
            val isSelected = item.section == currentSection
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSectionSelected(item.section) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = Translations.get(item.translationKey, language)
                    )
                },
                label = {
                    Text(
                        text = Translations.get(item.translationKey, language),
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                },
                modifier = Modifier.testTag("bottom_nav_${item.section.id}")
            )
        }
    }
}
