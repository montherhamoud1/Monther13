package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.engine.MenuEngine
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.Translations
import com.example.designsystem.*
import com.example.domain.model.*
import java.util.UUID

enum class MenuCatalogTab {
    PRODUCTS,
    CATEGORIES,
    VARIANTS_MODIFIERS,
    COMBOS,
    CHANNEL_PRICING,
    RECIPES_COSTING,
    UNIFIED_PREVIEW
}

@Composable
fun MenuCatalogScreen(
    branch: Branch,
    products: List<Product>,
    categories: List<Category>,
    ingredients: List<Ingredient>,
    recipes: List<Recipe>,
    combos: List<ComboProduct>,
    modifierGroups: List<ModifierGroup>,
    addons: List<ProductAddon>,
    priceRules: List<ChannelPriceRule>,
    taxProfiles: List<TaxProfile>,
    language: AppLanguage,
    onSaveProduct: (Product) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onToggleProductAvailability: (String, Boolean) -> Unit,
    onSaveCategory: (Category) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onSaveIngredient: (Ingredient) -> Unit,
    onDeleteIngredient: (String) -> Unit,
    onSaveRecipe: (Recipe) -> Unit,
    onSaveCombo: (ComboProduct) -> Unit,
    onSaveModifierGroup: (ModifierGroup) -> Unit,
    onSaveAddon: (ProductAddon) -> Unit,
    onSavePriceRule: (ChannelPriceRule) -> Unit,
    onDeletePriceRule: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(MenuCatalogTab.PRODUCTS) }

    // Dialog States
    var showProductDialog by remember { mutableStateOf<Product?>(null) }
    var isCreatingProduct by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf<Category?>(null) }
    var isCreatingCategory by remember { mutableStateOf(false) }
    var showIngredientDialog by remember { mutableStateOf<Ingredient?>(null) }
    var isCreatingIngredient by remember { mutableStateOf(false) }
    var showPriceRuleDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("menu_catalog_screen")
    ) {
        // Workspace Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = Translations.get("nav_menu_catalog", language),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            RestoBadge(
                                text = "Phase 3 Engine",
                                variant = BadgeVariant.INFO
                            )
                        }
                        Text(
                            text = if (language == AppLanguage.ARABIC)
                                "إدارة المنتجات، التصنيفات الشجرية، المتغيرات، المحددات، باقات الكومبو، تسعير القنوات ومحرك تكاليف الوصفات"
                            else
                                "Products, Category Hierarchy, Modifiers, Combos, Multi-Channel Price Lists & Recipe Costing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (selectedTab) {
                            MenuCatalogTab.PRODUCTS -> {
                                RestoButton(
                                    onClick = { isCreatingProduct = true },
                                    modifier = Modifier.testTag("btn_create_product")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(Translations.get("btn_new_product", language))
                                }
                            }
                            MenuCatalogTab.CATEGORIES -> {
                                RestoButton(
                                    onClick = { isCreatingCategory = true },
                                    modifier = Modifier.testTag("btn_create_category")
                                ) {
                                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(Translations.get("btn_new_category", language))
                                }
                            }
                            MenuCatalogTab.RECIPES_COSTING -> {
                                RestoButton(
                                    onClick = { isCreatingIngredient = true },
                                    modifier = Modifier.testTag("btn_create_ingredient")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(Translations.get("btn_new_ingredient", language))
                                }
                            }
                            MenuCatalogTab.CHANNEL_PRICING -> {
                                RestoButton(
                                    onClick = { showPriceRuleDialog = true },
                                    modifier = Modifier.testTag("btn_create_price_rule")
                                ) {
                                    Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(Translations.get("btn_new_price_rule", language))
                                }
                            }
                            else -> {}
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Tab Row
                ScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    edgePadding = 0.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("menu_catalog_tabs")
                ) {
                    Tab(
                        selected = selectedTab == MenuCatalogTab.PRODUCTS,
                        onClick = { selectedTab = MenuCatalogTab.PRODUCTS },
                        text = { Text(Translations.get("menu_tab_products", language)) },
                        icon = { Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == MenuCatalogTab.CATEGORIES,
                        onClick = { selectedTab = MenuCatalogTab.CATEGORIES },
                        text = { Text(Translations.get("menu_tab_categories", language)) },
                        icon = { Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == MenuCatalogTab.VARIANTS_MODIFIERS,
                        onClick = { selectedTab = MenuCatalogTab.VARIANTS_MODIFIERS },
                        text = { Text(Translations.get("menu_tab_variants_modifiers", language)) },
                        icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == MenuCatalogTab.COMBOS,
                        onClick = { selectedTab = MenuCatalogTab.COMBOS },
                        text = { Text(Translations.get("menu_tab_combos", language)) },
                        icon = { Icon(Icons.Default.Fastfood, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == MenuCatalogTab.CHANNEL_PRICING,
                        onClick = { selectedTab = MenuCatalogTab.CHANNEL_PRICING },
                        text = { Text(Translations.get("menu_tab_pricing_channels", language)) },
                        icon = { Icon(Icons.Default.PriceChange, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == MenuCatalogTab.RECIPES_COSTING,
                        onClick = { selectedTab = MenuCatalogTab.RECIPES_COSTING },
                        text = { Text(Translations.get("menu_tab_recipes_costing", language)) },
                        icon = { Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == MenuCatalogTab.UNIFIED_PREVIEW,
                        onClick = { selectedTab = MenuCatalogTab.UNIFIED_PREVIEW },
                        text = { Text(Translations.get("menu_tab_unified_preview", language)) },
                        icon = { Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (selectedTab) {
                MenuCatalogTab.PRODUCTS -> ProductsCatalogTab(
                    products = products,
                    categories = categories,
                    language = language,
                    onEditProduct = { showProductDialog = it },
                    onDeleteProduct = onDeleteProduct,
                    onToggleAvailability = onToggleProductAvailability
                )
                MenuCatalogTab.CATEGORIES -> CategoriesHierarchyTab(
                    categories = categories,
                    products = products,
                    language = language,
                    onEditCategory = { showCategoryDialog = it },
                    onDeleteCategory = onDeleteCategory
                )
                MenuCatalogTab.VARIANTS_MODIFIERS -> VariantsModifiersTab(
                    products = products,
                    modifierGroups = modifierGroups,
                    addons = addons,
                    language = language,
                    onSaveModifierGroup = onSaveModifierGroup,
                    onSaveAddon = onSaveAddon
                )
                MenuCatalogTab.COMBOS -> CombosTab(
                    combos = combos,
                    products = products,
                    categories = categories,
                    priceRules = priceRules,
                    language = language,
                    onSaveCombo = onSaveCombo
                )
                MenuCatalogTab.CHANNEL_PRICING -> ChannelPricingTab(
                    products = products,
                    priceRules = priceRules,
                    taxProfiles = taxProfiles,
                    language = language,
                    onDeletePriceRule = onDeletePriceRule
                )
                MenuCatalogTab.RECIPES_COSTING -> RecipesCostingTab(
                    products = products,
                    ingredients = ingredients,
                    recipes = recipes,
                    language = language,
                    onEditIngredient = { showIngredientDialog = it },
                    onDeleteIngredient = onDeleteIngredient,
                    onSaveRecipe = onSaveRecipe
                )
                MenuCatalogTab.UNIFIED_PREVIEW -> UnifiedMenuPreviewTab(
                    branch = branch,
                    categories = categories,
                    products = products,
                    priceRules = priceRules,
                    language = language
                )
            }
        }
    }

    // Product Add/Edit Dialog
    if (isCreatingProduct || showProductDialog != null) {
        val prodToEdit = showProductDialog
        ProductEditDialog(
            product = prodToEdit,
            categories = categories,
            taxProfiles = taxProfiles,
            language = language,
            onDismiss = {
                isCreatingProduct = false
                showProductDialog = null
            },
            onSave = { updated ->
                onSaveProduct(updated)
                isCreatingProduct = false
                showProductDialog = null
            }
        )
    }

    // Category Add/Edit Dialog
    if (isCreatingCategory || showCategoryDialog != null) {
        val catToEdit = showCategoryDialog
        CategoryEditDialog(
            category = catToEdit,
            categories = categories,
            language = language,
            onDismiss = {
                isCreatingCategory = false
                showCategoryDialog = null
            },
            onSave = { updated ->
                onSaveCategory(updated)
                isCreatingCategory = false
                showCategoryDialog = null
            }
        )
    }

    // Ingredient Add/Edit Dialog
    if (isCreatingIngredient || showIngredientDialog != null) {
        val ingToEdit = showIngredientDialog
        IngredientEditDialog(
            ingredient = ingToEdit,
            language = language,
            onDismiss = {
                isCreatingIngredient = false
                showIngredientDialog = null
            },
            onSave = { updated ->
                onSaveIngredient(updated)
                isCreatingIngredient = false
                showIngredientDialog = null
            }
        )
    }

    // Price Rule Dialog
    if (showPriceRuleDialog) {
        PriceRuleCreateDialog(
            products = products,
            branches = listOf(branch),
            language = language,
            onDismiss = { showPriceRuleDialog = false },
            onSave = { rule ->
                onSavePriceRule(rule)
                showPriceRuleDialog = false
            }
        )
    }
}

// ==========================================
// 1. PRODUCTS TAB
// ==========================================

@Composable
fun ProductsCatalogTab(
    products: List<Product>,
    categories: List<Category>,
    language: AppLanguage,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onToggleAvailability: (String, Boolean) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }

    val filteredProducts = remember(products, searchQuery, selectedCategoryId) {
        products.filter { prod ->
            val matchCategory = selectedCategoryId == null || prod.categoryId == selectedCategoryId
            val matchQuery = searchQuery.isBlank() ||
                prod.nameEn.contains(searchQuery, ignoreCase = true) ||
                prod.nameAr.contains(searchQuery, ignoreCase = true) ||
                prod.sku.contains(searchQuery, ignoreCase = true) ||
                prod.barcode.contains(searchQuery, ignoreCase = true)
            matchCategory && matchQuery
        }
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Summary KPIs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val totalCount = products.size
            val sellableCount = products.count { it.isSellable }
            val prepCount = products.count { it.isPreparationItem }
            val avgMargin = if (products.isNotEmpty()) {
                products.map {
                    if (it.price > 0) ((it.price - it.costPrice) / it.price) * 100.0 else 0.0
                }.average()
            } else 0.0

            MetricBox("Total Items", "$totalCount", Icons.Default.Fastfood, BadgeVariant.INFO, Modifier.weight(1f))
            MetricBox("Sellable", "$sellableCount", Icons.Default.PointOfSale, BadgeVariant.SUCCESS, Modifier.weight(1f))
            MetricBox("Kitchen Prep", "$prepCount", Icons.Default.Kitchen, BadgeVariant.WARNING, Modifier.weight(1f))
            MetricBox("Avg Gross Margin", String.format("%.1f%%", avgMargin), Icons.Default.TrendingUp, BadgeVariant.INFO, Modifier.weight(1f))
        }

        // Search & Category Filters
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RestoTextInput(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = Translations.get("search_placeholder", language),
                    placeholder = "Search by SKU, barcode, name...",
                    leadingIcon = Icons.Default.Search,
                    modifier = Modifier.fillMaxWidth()
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = { selectedCategoryId = null },
                            label = { Text("All Categories (${products.size})") }
                        )
                    }
                    items(categories) { cat ->
                        val count = products.count { it.categoryId == cat.id }
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { selectedCategoryId = cat.id },
                            label = { Text("${if (language == AppLanguage.ARABIC) cat.nameAr else cat.nameEn} ($count)") }
                        )
                    }
                }
            }
        }

        // Products List
        if (filteredProducts.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                RestoEmptyState(
                    title = Translations.get("empty_data_title", language),
                    description = Translations.get("empty_data_desc", language),
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredProducts, key = { it.id }) { prod ->
                    ProductRowCard(
                        product = prod,
                        category = categories.find { it.id == prod.categoryId },
                        language = language,
                        onEdit = { onEditProduct(prod) },
                        onDelete = { onDeleteProduct(prod.id) },
                        onToggleAvailability = { onToggleAvailability(prod.id, !prod.isAvailable) }
                    )
                }
            }
        }
    }
}

@Composable
fun MetricBox(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    variant: BadgeVariant,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProductRowCard(
    product: Product,
    category: Category?,
    language: AppLanguage,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleAvailability: () -> Unit
) {
    val grossProfit = product.price - product.costPrice
    val marginPercent = if (product.price > 0) (grossProfit / product.price) * 100.0 else 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_row_${product.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Product Info
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Color Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (product.isCombo) Icons.Default.Fastfood else Icons.Default.LunchDining,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (language == AppLanguage.ARABIC) product.nameAr else product.nameEn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (product.isCombo) {
                            RestoBadge("COMBO", variant = BadgeVariant.INFO)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        if (product.variants.isNotEmpty()) {
                            RestoBadge("${product.variants.size} sizes", variant = BadgeVariant.INFO)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        if (product.modifierGroupIds.isNotEmpty()) {
                            RestoBadge("${product.modifierGroupIds.size} mods", variant = BadgeVariant.NEUTRAL)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "SKU: ${product.sku}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (product.barcode.isNotBlank()) {
                            Text(
                                text = "• Barcode: ${product.barcode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        category?.let { cat ->
                            Text(
                                text = "• ${if (language == AppLanguage.ARABIC) cat.nameAr else cat.nameEn}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Operational flags
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (product.isPreparationItem) {
                            RestoBadge("Kitchen Prep", variant = BadgeVariant.WARNING)
                        }
                        if (product.isInventoryItem) {
                            RestoBadge("Stock: ${product.currentStock}", variant = BadgeVariant.NEUTRAL)
                        }
                    }
                }
            }

            // Price & Costing Section
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "$${String.format("%.2f", product.price)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Cost: $${String.format("%.2f", product.costPrice)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                RestoBadge(
                    text = String.format("%.0f%% Margin", marginPercent),
                    variant = when {
                        marginPercent >= 60.0 -> BadgeVariant.SUCCESS
                        marginPercent >= 40.0 -> BadgeVariant.INFO
                        else -> BadgeVariant.WARNING
                    }
                )
            }

            // Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Availability Switch
                Switch(
                    checked = product.isAvailable,
                    onCheckedChange = { onToggleAvailability() },
                    modifier = Modifier.testTag("switch_avail_${product.id}")
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Product", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Product", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// ==========================================
// 2. CATEGORIES HIERARCHY TAB
// ==========================================

@Composable
fun CategoriesHierarchyTab(
    categories: List<Category>,
    products: List<Product>,
    language: AppLanguage,
    onEditCategory: (Category) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    val parentCategories = categories.filter { it.parentId == null }.sortedBy { it.sortOrder }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(parentCategories, key = { it.id }) { parent ->
            val children = categories.filter { it.parentId == parent.id }.sortedBy { it.sortOrder }
            val parentItemCount = products.count { it.categoryId == parent.id }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Parent Category Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(parent.colorHex.ifBlank { "#F59E0B" })))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (language == AppLanguage.ARABIC) parent.nameAr else parent.nameEn,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            RestoBadge("Top Level (Order: ${parent.sortOrder})", variant = BadgeVariant.INFO)
                            Spacer(modifier = Modifier.width(6.dp))
                            RestoBadge("$parentItemCount direct items", variant = BadgeVariant.NEUTRAL)
                        }

                        Row {
                            IconButton(onClick = { onEditCategory(parent) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onDeleteCategory(parent.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    // Sub-categories list
                    if (children.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (language == AppLanguage.ARABIC) "التصنيفات الفرعية:" else "Sub-Categories:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        children.forEach { child ->
                            val childCount = products.count { it.categoryId == child.id }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (language == AppLanguage.ARABIC) child.nameAr else child.nameEn,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        RestoBadge("Order: ${child.sortOrder}", variant = BadgeVariant.NEUTRAL)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        RestoBadge("$childCount items", variant = BadgeVariant.INFO)
                                    }

                                    Row {
                                        IconButton(onClick = { onEditCategory(child) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(onClick = { onDeleteCategory(child.id) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. VARIANTS & MODIFIERS TAB
// ==========================================

@Composable
fun VariantsModifiersTab(
    products: List<Product>,
    modifierGroups: List<ModifierGroup>,
    addons: List<ProductAddon>,
    language: AppLanguage,
    onSaveModifierGroup: (ModifierGroup) -> Unit,
    onSaveAddon: (ProductAddon) -> Unit
) {
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var showNewAddonDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Modifier Groups
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (language == AppLanguage.ARABIC) "مجموعات الخيارات والمحددات" else "Modifier Groups & Option Rules",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (language == AppLanguage.ARABIC) "قواعد الاختيار الإلزامي والحد الأدنى والأقصى مع فروقات الأسعار" else "Required/optional rules, min/max selection bounds, and price adjustments",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                RestoButton(
                    onClick = { showNewGroupDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Translations.get("btn_new_modifier", language))
                }
            }
        }

        items(modifierGroups, key = { it.id }) { group ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (language == AppLanguage.ARABIC) group.nameAr else group.nameEn,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                if (group.isRequired) {
                                    RestoBadge("Required (${group.minSelections} - ${group.maxSelections})", variant = BadgeVariant.WARNING)
                                } else {
                                    RestoBadge("Optional (Max: ${group.maxSelections})", variant = BadgeVariant.INFO)
                                }
                            }
                            Text(
                                text = "Group ID: ${group.id} • ${group.options.size} options available",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Options Grid
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        group.options.forEach { opt ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (language == AppLanguage.ARABIC) opt.nameAr else opt.nameEn,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (opt.isDefault) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            RestoBadge("Default", variant = BadgeVariant.SUCCESS)
                                        }
                                        if (opt.calories > 0) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${opt.calories} kcal",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Text(
                                        text = if (opt.priceAdjustment > 0) "+$${String.format("%.2f", opt.priceAdjustment)}" else "Free",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (opt.priceAdjustment > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Global Add-ons
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (language == AppLanguage.ARABIC) "الإضافات الاختيارية (Add-ons)" else "Optional Add-ons",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (language == AppLanguage.ARABIC) "إضافات مدفوعة بأسعار محددة والحد الأقصى للطلب" else "Extra additions with custom pricing and maximum quantities",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                RestoButton(
                    onClick = { showNewAddonDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Translations.get("btn_new_addon", language))
                }
            }
        }

        items(addons, key = { it.id }) { addon ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (language == AppLanguage.ARABIC) addon.nameAr else addon.nameEn,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Max Quantity: ${addon.maxQuantity}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "+$${String.format("%.2f", addon.price)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (showNewGroupDialog) {
        NewModifierGroupDialog(
            language = language,
            onDismiss = { showNewGroupDialog = false },
            onSave = {
                onSaveModifierGroup(it)
                showNewGroupDialog = false
            }
        )
    }

    if (showNewAddonDialog) {
        NewAddonDialog(
            language = language,
            onDismiss = { showNewAddonDialog = false },
            onSave = {
                onSaveAddon(it)
                showNewAddonDialog = false
            }
        )
    }
}

// ==========================================
// 4. COMBOS TAB
// ==========================================

@Composable
fun CombosTab(
    combos: List<ComboProduct>,
    products: List<Product>,
    categories: List<Category>,
    priceRules: List<ChannelPriceRule>,
    language: AppLanguage,
    onSaveCombo: (ComboProduct) -> Unit
) {
    var showCreateComboDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (language == AppLanguage.ARABIC) "باقات ووجبات الكومبو" else "Combo Meals & Bundles Engine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (language == AppLanguage.ARABIC) "تحديد خطوات الاختيار (برجر + طبق جانبي + مشروب) ورسوم الترقية" else "Configure multi-step bundled selections with component categories and upgrade surcharges",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                RestoButton(
                    onClick = { showCreateComboDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Translations.get("btn_new_combo", language))
                }
            }
        }

        items(combos, key = { it.id }) { combo ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (language == AppLanguage.ARABIC) combo.nameAr else combo.nameEn,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                RestoBadge("SKU: ${combo.sku}", variant = BadgeVariant.NEUTRAL)
                            }
                            Text(
                                text = "${combo.components.size} Selection Steps Required",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$${String.format("%.2f", combo.comboBasePrice)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Base Bundle Price",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (language == AppLanguage.ARABIC) "خطوات تكوين الوجبة:" else "Bundle Component Steps:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    combo.components.forEachIndexed { idx, comp ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${idx + 1}", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (language == AppLanguage.ARABIC) comp.titleAr else comp.titleEn,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val defaultProduct = products.find { it.id == comp.defaultProductId }
                                        Text(
                                            text = "Default: ${defaultProduct?.nameEn ?: "None"} • Allowed Categories: ${comp.allowedCategoryIds.size}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (comp.extraCharge > 0) {
                                    RestoBadge("+$${String.format("%.2f", comp.extraCharge)} extra", variant = BadgeVariant.WARNING)
                                } else {
                                    RestoBadge("Included", variant = BadgeVariant.SUCCESS)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateComboDialog) {
        CreateComboDialog(
            products = products,
            categories = categories,
            language = language,
            onDismiss = { showCreateComboDialog = false },
            onSave = {
                onSaveCombo(it)
                showCreateComboDialog = false
            }
        )
    }
}

// ==========================================
// 5. CHANNEL PRICING & TAXES TAB
// ==========================================

@Composable
fun ChannelPricingTab(
    products: List<Product>,
    priceRules: List<ChannelPriceRule>,
    taxProfiles: List<TaxProfile>,
    language: AppLanguage,
    onDeletePriceRule: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Multi-Channel Price Comparison
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (language == AppLanguage.ARABIC) "مقارنة أسعار القنوات البيعية" else "Multi-Channel Price Resolution Matrix",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (language == AppLanguage.ARABIC) "مقارنة الأسعار المحسوبة بين الكاشير وتطبيقات التوصيل ورمز QR" else "Resolved prices across POS, Delivery Aggregators, and QR Ordering",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Table
                    products.take(6).forEach { prod ->
                        val posPrice = MenuEngine.resolveProductPrice(prod, channel = SalesChannel.POS, rules = priceRules)
                        val delPrice = MenuEngine.resolveProductPrice(prod, channel = SalesChannel.DELIVERY_PARTNERS, rules = priceRules)
                        val qrPrice = MenuEngine.resolveProductPrice(prod, channel = SalesChannel.QR_TABLE, rules = priceRules)

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (language == AppLanguage.ARABIC) prod.nameAr else prod.nameEn,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Base Price: $${String.format("%.2f", prod.price)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("POS Register", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("$${String.format("%.2f", posPrice)}", fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Delivery Apps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            "$${String.format("%.2f", delPrice)}",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("QR Table Promo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            "$${String.format("%.2f", qrPrice)}",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Active Price Rules
        item {
            Text(
                text = if (language == AppLanguage.ARABIC) "قواعد التسعير النشطة (${priceRules.size})" else "Active Channel & Branch Price Rules (${priceRules.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(priceRules, key = { it.id }) { rule ->
            val product = products.find { it.id == rule.productId }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = product?.nameEn ?: rule.productId,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            rule.channel?.let { ch ->
                                RestoBadge(ch.name, variant = BadgeVariant.INFO)
                            }
                            rule.branchId?.let { b ->
                                Spacer(modifier = Modifier.width(4.dp))
                                RestoBadge("Branch: $b", variant = BadgeVariant.NEUTRAL)
                            }
                        }
                        Text(
                            text = "Rule ID: ${rule.id}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$${String.format("%.2f", rule.customPrice)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { onDeletePriceRule(rule.id) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Section: Tax Profiles
        item {
            Text(
                text = if (language == AppLanguage.ARABIC) "الملفات الضريبية للعلامة التجارية" else "Tax Profiles & Tax Rates",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(taxProfiles, key = { it.id }) { tax ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (language == AppLanguage.ARABIC) tax.nameAr else tax.nameEn,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            RestoBadge(tax.taxCode, variant = BadgeVariant.NEUTRAL)
                        }
                        Text(
                            text = if (tax.isInclusive) "Inclusive (Included in Display Price)" else "Exclusive (Added at checkout)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "${(tax.rate * 100).toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ==========================================
// 6. RECIPES & COSTING TAB
// ==========================================

@Composable
fun RecipesCostingTab(
    products: List<Product>,
    ingredients: List<Ingredient>,
    recipes: List<Recipe>,
    language: AppLanguage,
    onEditIngredient: (Ingredient) -> Unit,
    onDeleteIngredient: (String) -> Unit,
    onSaveRecipe: (Recipe) -> Unit
) {
    var selectedProductId by remember { mutableStateOf(products.firstOrNull()?.id ?: "") }
    val ingredientsMap = remember(ingredients) { ingredients.associateBy { it.id } }

    val activeProduct = products.find { it.id == selectedProductId }
    val activeRecipe = recipes.find { it.productId == selectedProductId }
    val costSummary = remember(activeRecipe, ingredientsMap, activeProduct) {
        if (activeRecipe != null && activeProduct != null) {
            MenuEngine.calculateRecipeCost(activeRecipe, ingredientsMap, activeProduct.price)
        } else null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Product Selector for Cost Simulator
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (language == AppLanguage.ARABIC) "محاكي تكاليف الإنتاج وهامش الربح الحقيقي" else "Portion Recipe Cost & Gross Margin Simulator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (language == AppLanguage.ARABIC) "حساب تكلفة المكونات بنسب الهدر والإنتاجية مع العمالة والتغليف" else "Calculates yield %, trim waste %, packaging overhead, and direct labor overhead",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(products) { prod ->
                            FilterChip(
                                selected = selectedProductId == prod.id,
                                onClick = { selectedProductId = prod.id },
                                label = { Text(if (language == AppLanguage.ARABIC) prod.nameAr else prod.nameEn) }
                            )
                        }
                    }

                    costSummary?.let { summary ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CostKpiBox("Raw Ingredients", "$${String.format("%.2f", summary.rawIngredientsCost)}", Modifier.weight(1f))
                            CostKpiBox("Packaging", "$${String.format("%.2f", summary.packagingCost)}", Modifier.weight(1f))
                            CostKpiBox("Labor Overhead", "$${String.format("%.2f", summary.laborCost)}", Modifier.weight(1f))
                            CostKpiBox("Total Portion COGS", "$${String.format("%.2f", summary.totalCost)}", Modifier.weight(1f))
                            CostKpiBox(
                                "Gross Profit",
                                "$${String.format("%.2f", summary.grossProfit)} (${String.format("%.1f%%", summary.grossMarginPercentage)})",
                                Modifier.weight(1.2f),
                                isHighlight = true
                            )
                        }
                    }
                }
            }
        }

        // Section: Recipe Bill of Materials (BOM)
        if (activeRecipe != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Bill of Materials: ${activeRecipe.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        activeRecipe.items.forEach { item ->
                            val ing = ingredientsMap[item.ingredientId]
                            val effYield = (ing?.yieldPercentage ?: 1.0) * (1.0 - (ing?.wastePercentage ?: 0.0))
                            val lineCost = if (ing != null) (item.quantity * ing.costPerUnit) / effYield.coerceAtLeast(0.05) else 0.0

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
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
                                            text = "${item.quantity} ${item.unit} • ${ing?.nameEn ?: item.ingredientNameEn}",
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Unit Cost: $${ing?.costPerUnit ?: 0.0}/${item.unit} | Yield: ${((ing?.yieldPercentage ?: 1.0) * 100).toInt()}% | Waste: ${((ing?.wastePercentage ?: 0.0) * 100).toInt()}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(
                                        text = "$${String.format("%.2f", lineCost)}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Raw Ingredients Inventory Par
        item {
            Text(
                text = if (language == AppLanguage.ARABIC) "المواد الخام وسجلات التكلفة والإنتاجية (${ingredients.size})" else "Master Ingredients Catalog & Yield Rates (${ingredients.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(ingredients, key = { it.id }) { ing ->
            val effectiveFactor = ing.yieldPercentage * (1.0 - ing.wastePercentage)
            val effectiveUnitCost = ing.costPerUnit / effectiveFactor.coerceAtLeast(0.05)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (language == AppLanguage.ARABIC) ing.nameAr else ing.nameEn,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            RestoBadge("Code: ${ing.code}", variant = BadgeVariant.NEUTRAL)
                        }
                        Text(
                            text = "Stock: ${ing.currentStock} ${ing.unit} • Purchase Cost: $${ing.costPerUnit}/${ing.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Yield: ${(ing.yieldPercentage * 100).toInt()}% • Waste: ${(ing.wastePercentage * 100).toInt()}% • Effective Cost: $${String.format("%.2f", effectiveUnitCost)}/${ing.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row {
                        IconButton(onClick = { onEditIngredient(ing) }) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onDeleteIngredient(ing.id) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CostKpiBox(label: String, value: String, modifier: Modifier = Modifier, isHighlight: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isHighlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isHighlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isHighlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

// ==========================================
// 7. UNIFIED CHANNEL MENU PREVIEW TAB
// ==========================================

@Composable
fun UnifiedMenuPreviewTab(
    branch: Branch,
    categories: List<Category>,
    products: List<Product>,
    priceRules: List<ChannelPriceRule>,
    language: AppLanguage
) {
    var selectedChannel by remember { mutableStateOf(SalesChannel.POS) }

    val resolvedCategories = remember(categories, products, selectedChannel, branch.id, priceRules) {
        MenuEngine.getUnifiedMenuForChannel(
            categories = categories,
            products = products,
            channel = selectedChannel,
            branchId = branch.id,
            priceRules = priceRules
        )
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Channel Switcher
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (language == AppLanguage.ARABIC) "محاكي قائمة القنوات الموحدة" else "Unified Omni-Channel Menu Consumer Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (language == AppLanguage.ARABIC) "جميع القنوات (POS، النادل، رمز QR، الويب، الجوال، التوصيل) تستهلك نفس المحرك دون تكرار للبيانات" else "Zero duplication: POS, Waiter App, QR Ordering, Web, Mobile App & Delivery platforms all consume this live menu",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SalesChannel.values()) { channel ->
                        FilterChip(
                            selected = selectedChannel == channel,
                            onClick = { selectedChannel = channel },
                            label = { Text(if (language == AppLanguage.ARABIC) channel.titleAr else channel.titleEn) },
                            leadingIcon = {
                                Icon(
                                    when (channel) {
                                        SalesChannel.POS -> Icons.Default.PointOfSale
                                        SalesChannel.WAITER_APP -> Icons.Default.RoomService
                                        SalesChannel.QR_TABLE -> Icons.Default.QrCode
                                        SalesChannel.CUSTOMER_WEB -> Icons.Default.Language
                                        SalesChannel.CUSTOMER_APP -> Icons.Default.PhoneAndroid
                                        SalesChannel.DELIVERY_PARTNERS -> Icons.Default.DeliveryDining
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

        // Rendered Categories & Items
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(resolvedCategories, key = { it.category.id }) { catMenu ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (language == AppLanguage.ARABIC) catMenu.category.nameAr else catMenu.category.nameEn,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            RestoBadge("${catMenu.items.size} Available", variant = BadgeVariant.SUCCESS)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        catMenu.items.forEach { menuItem ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (language == AppLanguage.ARABIC) menuItem.product.nameAr else menuItem.product.nameEn,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = if (language == AppLanguage.ARABIC) menuItem.product.descriptionAr else menuItem.product.descriptionEn,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "$${String.format("%.2f", menuItem.resolvedPrice)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (menuItem.resolvedPrice != menuItem.product.price) {
                                            Text(
                                                text = "Base: $${String.format("%.2f", menuItem.product.price)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// DIALOGS
// ==========================================

@Composable
fun ProductEditDialog(
    product: Product?,
    categories: List<Category>,
    taxProfiles: List<TaxProfile>,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    val isEdit = product != null
    var sku by remember { mutableStateOf(product?.sku ?: "BUR-00${(10..99).random()}") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "628100${(1000..9999).random()}") }
    var nameEn by remember { mutableStateOf(product?.nameEn ?: "") }
    var nameAr by remember { mutableStateOf(product?.nameAr ?: "") }
    var priceStr by remember { mutableStateOf(product?.price?.toString() ?: "15.00") }
    var costPriceStr by remember { mutableStateOf(product?.costPrice?.toString() ?: "5.00") }
    var selectedCategoryId by remember { mutableStateOf(product?.categoryId ?: categories.firstOrNull()?.id ?: "") }
    var isSellable by remember { mutableStateOf(product?.isSellable ?: true) }
    var isPrepItem by remember { mutableStateOf(product?.isPreparationItem ?: true) }
    var isInventoryItem by remember { mutableStateOf(product?.isInventoryItem ?: true) }

    RestoModalDialog(
        title = if (isEdit) "Edit Product: ${product?.nameEn}" else Translations.get("btn_new_product", language),
        onDismissRequest = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RestoTextInput(value = sku, onValueChange = { sku = it }, label = "SKU", modifier = Modifier.weight(1f))
                RestoTextInput(value = barcode, onValueChange = { barcode = it }, label = "Barcode", modifier = Modifier.weight(1f))
            }
            RestoTextInput(value = nameEn, onValueChange = { nameEn = it }, label = "Name (English)", modifier = Modifier.fillMaxWidth())
            RestoTextInput(value = nameAr, onValueChange = { nameAr = it }, label = "Name (Arabic)", modifier = Modifier.fillMaxWidth())

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RestoTextInput(value = priceStr, onValueChange = { priceStr = it }, label = "Selling Price ($)", modifier = Modifier.weight(1f))
                RestoTextInput(value = costPriceStr, onValueChange = { costPriceStr = it }, label = "Cost Price ($)", modifier = Modifier.weight(1f))
            }

            // Category Selector
            Text("Category:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick = { selectedCategoryId = cat.id },
                        label = { Text(if (language == AppLanguage.ARABIC) cat.nameAr else cat.nameEn) }
                    )
                }
            }

            // Toggles
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Sellable Item", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = isSellable, onCheckedChange = { isSellable = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Kitchen Preparation Required", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = isPrepItem, onCheckedChange = { isPrepItem = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Track in Inventory", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = isInventoryItem, onCheckedChange = { isInventoryItem = it })
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(Translations.get("action_cancel", language))
                }
                Spacer(modifier = Modifier.width(8.dp))
                RestoButton(
                    onClick = {
                        if (nameEn.isNotBlank()) {
                            val newProd = Product(
                                id = product?.id ?: "prod_${UUID.randomUUID().toString().take(6)}",
                                categoryId = selectedCategoryId,
                                sku = sku,
                                barcode = barcode,
                                nameEn = nameEn,
                                nameAr = nameAr.ifBlank { nameEn },
                                price = priceStr.toDoubleOrNull() ?: 15.00,
                                costPrice = costPriceStr.toDoubleOrNull() ?: 5.00,
                                isSellable = isSellable,
                                isPreparationItem = isPrepItem,
                                isInventoryItem = isInventoryItem,
                                variants = product?.variants ?: emptyList(),
                                modifierGroupIds = product?.modifierGroupIds ?: emptyList(),
                                addonIds = product?.addonIds ?: emptyList()
                            )
                            onSave(newProd)
                        }
                    }
                ) {
                    Text(Translations.get("action_save", language))
                }
            }
        }
    }
}

@Composable
fun CategoryEditDialog(
    category: Category?,
    categories: List<Category>,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit
) {
    var nameEn by remember { mutableStateOf(category?.nameEn ?: "") }
    var nameAr by remember { mutableStateOf(category?.nameAr ?: "") }
    var sortOrderStr by remember { mutableStateOf(category?.sortOrder?.toString() ?: "1") }
    var colorHex by remember { mutableStateOf(category?.colorHex ?: "#F59E0B") }
    var selectedParentId by remember { mutableStateOf(category?.parentId) }

    val potentialParents = categories.filter { it.parentId == null && it.id != category?.id }

    RestoModalDialog(
        title = if (category != null) "Edit Category" else Translations.get("btn_new_category", language),
        onDismissRequest = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RestoTextInput(value = nameEn, onValueChange = { nameEn = it }, label = "Category Name (EN)", modifier = Modifier.fillMaxWidth())
            RestoTextInput(value = nameAr, onValueChange = { nameAr = it }, label = "Category Name (AR)", modifier = Modifier.fillMaxWidth())
            RestoTextInput(value = sortOrderStr, onValueChange = { sortOrderStr = it }, label = "Sort Order", modifier = Modifier.fillMaxWidth())

            Text("Parent Category (Hierarchical Tree):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = selectedParentId == null,
                        onClick = { selectedParentId = null },
                        label = { Text("None (Top Level)") }
                    )
                }
                items(potentialParents) { p ->
                    FilterChip(
                        selected = selectedParentId == p.id,
                        onClick = { selectedParentId = p.id },
                        label = { Text(if (language == AppLanguage.ARABIC) p.nameAr else p.nameEn) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(Translations.get("action_cancel", language))
                }
                Spacer(modifier = Modifier.width(8.dp))
                RestoButton(
                    onClick = {
                        if (nameEn.isNotBlank()) {
                            val newCat = Category(
                                id = category?.id ?: "cat_${UUID.randomUUID().toString().take(6)}",
                                brandId = "brand_01",
                                nameEn = nameEn,
                                nameAr = nameAr.ifBlank { nameEn },
                                sortOrder = sortOrderStr.toIntOrNull() ?: 1,
                                colorHex = colorHex,
                                parentId = selectedParentId
                            )
                            onSave(newCat)
                        }
                    }
                ) {
                    Text(Translations.get("action_save", language))
                }
            }
        }
    }
}

@Composable
fun IngredientEditDialog(
    ingredient: Ingredient?,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (Ingredient) -> Unit
) {
    var code by remember { mutableStateOf(ingredient?.code ?: "ING-0${(10..99).random()}") }
    var nameEn by remember { mutableStateOf(ingredient?.nameEn ?: "") }
    var nameAr by remember { mutableStateOf(ingredient?.nameAr ?: "") }
    var unit by remember { mutableStateOf(ingredient?.unit ?: "kg") }
    var costStr by remember { mutableStateOf(ingredient?.costPerUnit?.toString() ?: "10.00") }
    var stockStr by remember { mutableStateOf(ingredient?.currentStock?.toString() ?: "50.0") }
    var yieldStr by remember { mutableStateOf(ingredient?.yieldPercentage?.let { (it * 100).toInt().toString() } ?: "90") }
    var wasteStr by remember { mutableStateOf(ingredient?.wastePercentage?.let { (it * 100).toInt().toString() } ?: "5") }

    RestoModalDialog(
        title = if (ingredient != null) "Edit Raw Ingredient" else Translations.get("btn_new_ingredient", language),
        onDismissRequest = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RestoTextInput(value = code, onValueChange = { code = it }, label = "Ingredient Code", modifier = Modifier.fillMaxWidth())
            RestoTextInput(value = nameEn, onValueChange = { nameEn = it }, label = "Name (EN)", modifier = Modifier.fillMaxWidth())
            RestoTextInput(value = nameAr, onValueChange = { nameAr = it }, label = "Name (AR)", modifier = Modifier.fillMaxWidth())

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RestoTextInput(value = unit, onValueChange = { unit = it }, label = "Unit (kg/liter/pcs)", modifier = Modifier.weight(1f))
                RestoTextInput(value = costStr, onValueChange = { costStr = it }, label = "Cost / Unit ($)", modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RestoTextInput(value = yieldStr, onValueChange = { yieldStr = it }, label = "Yield %", modifier = Modifier.weight(1f))
                RestoTextInput(value = wasteStr, onValueChange = { wasteStr = it }, label = "Trim Waste %", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(Translations.get("action_cancel", language))
                }
                Spacer(modifier = Modifier.width(8.dp))
                RestoButton(
                    onClick = {
                        if (nameEn.isNotBlank()) {
                            val newIng = Ingredient(
                                id = ingredient?.id ?: "ing_${UUID.randomUUID().toString().take(6)}",
                                code = code,
                                nameEn = nameEn,
                                nameAr = nameAr.ifBlank { nameEn },
                                unit = unit,
                                costPerUnit = costStr.toDoubleOrNull() ?: 10.0,
                                currentStock = stockStr.toDoubleOrNull() ?: 50.0,
                                yieldPercentage = (yieldStr.toDoubleOrNull() ?: 90.0) / 100.0,
                                wastePercentage = (wasteStr.toDoubleOrNull() ?: 5.0) / 100.0
                            )
                            onSave(newIng)
                        }
                    }
                ) {
                    Text(Translations.get("action_save", language))
                }
            }
        }
    }
}

@Composable
fun PriceRuleCreateDialog(
    products: List<Product>,
    branches: List<Branch>,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (ChannelPriceRule) -> Unit
) {
    var selectedProductId by remember { mutableStateOf(products.firstOrNull()?.id ?: "") }
    var selectedChannel by remember { mutableStateOf<SalesChannel?>(SalesChannel.DELIVERY_PARTNERS) }
    var customPriceStr by remember { mutableStateOf("19.50") }

    RestoModalDialog(
        title = "Create Channel Price Rule",
        onDismissRequest = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Select Product:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(products) { p ->
                    FilterChip(
                        selected = selectedProductId == p.id,
                        onClick = { selectedProductId = p.id },
                        label = { Text(if (language == AppLanguage.ARABIC) p.nameAr else p.nameEn) }
                    )
                }
            }

            Text("Select Sales Channel:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(SalesChannel.values()) { ch ->
                    FilterChip(
                        selected = selectedChannel == ch,
                        onClick = { selectedChannel = ch },
                        label = { Text(if (language == AppLanguage.ARABIC) ch.titleAr else ch.titleEn) }
                    )
                }
            }

            RestoTextInput(
                value = customPriceStr,
                onValueChange = { customPriceStr = it },
                label = "Custom Price ($)",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(Translations.get("action_cancel", language))
                }
                Spacer(modifier = Modifier.width(8.dp))
                RestoButton(
                    onClick = {
                        val newRule = ChannelPriceRule(
                            id = "pr_${UUID.randomUUID().toString().take(6)}",
                            productId = selectedProductId,
                            channel = selectedChannel,
                            customPrice = customPriceStr.toDoubleOrNull() ?: 19.50
                        )
                        onSave(newRule)
                    }
                ) {
                    Text(Translations.get("action_save", language))
                }
            }
        }
    }
}

@Composable
fun NewModifierGroupDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (ModifierGroup) -> Unit
) {
    var nameEn by remember { mutableStateOf("") }
    var nameAr by remember { mutableStateOf("") }
    var isRequired by remember { mutableStateOf(false) }
    var minSel by remember { mutableStateOf("0") }
    var maxSel by remember { mutableStateOf("1") }

    RestoModalDialog(
        title = Translations.get("btn_new_modifier", language),
        onDismissRequest = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RestoTextInput(value = nameEn, onValueChange = { nameEn = it }, label = "Group Name (EN)", modifier = Modifier.fillMaxWidth())
            RestoTextInput(value = nameAr, onValueChange = { nameAr = it }, label = "Group Name (AR)", modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Required Selection", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = isRequired, onCheckedChange = { isRequired = it })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RestoTextInput(value = minSel, onValueChange = { minSel = it }, label = "Min Selections", modifier = Modifier.weight(1f))
                RestoTextInput(value = maxSel, onValueChange = { maxSel = it }, label = "Max Selections", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(Translations.get("action_cancel", language))
                }
                Spacer(modifier = Modifier.width(8.dp))
                RestoButton(
                    onClick = {
                        if (nameEn.isNotBlank()) {
                            val group = ModifierGroup(
                                id = "mg_${UUID.randomUUID().toString().take(6)}",
                                nameEn = nameEn,
                                nameAr = nameAr.ifBlank { nameEn },
                                isRequired = isRequired,
                                minSelections = minSel.toIntOrNull() ?: 0,
                                maxSelections = maxSel.toIntOrNull() ?: 1,
                                options = listOf(
                                    ModifierOption("opt_1", "mg_new", "Option 1", "خيار 1", 0.0, isDefault = true),
                                    ModifierOption("opt_2", "mg_new", "Option 2 (Extra)", "خيار 2 إضافي", 1.50)
                                )
                            )
                            onSave(group)
                        }
                    }
                ) {
                    Text(Translations.get("action_save", language))
                }
            }
        }
    }
}

@Composable
fun NewAddonDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (ProductAddon) -> Unit
) {
    var nameEn by remember { mutableStateOf("") }
    var nameAr by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("2.00") }
    var maxQtyStr by remember { mutableStateOf("3") }

    RestoModalDialog(
        title = Translations.get("btn_new_addon", language),
        onDismissRequest = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RestoTextInput(value = nameEn, onValueChange = { nameEn = it }, label = "Addon Name (EN)", modifier = Modifier.fillMaxWidth())
            RestoTextInput(value = nameAr, onValueChange = { nameAr = it }, label = "Addon Name (AR)", modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RestoTextInput(value = priceStr, onValueChange = { priceStr = it }, label = "Price ($)", modifier = Modifier.weight(1f))
                RestoTextInput(value = maxQtyStr, onValueChange = { maxQtyStr = it }, label = "Max Quantity", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(Translations.get("action_cancel", language))
                }
                Spacer(modifier = Modifier.width(8.dp))
                RestoButton(
                    onClick = {
                        if (nameEn.isNotBlank()) {
                            val addon = ProductAddon(
                                id = "add_${UUID.randomUUID().toString().take(6)}",
                                productId = "global",
                                nameEn = nameEn,
                                nameAr = nameAr.ifBlank { nameEn },
                                price = priceStr.toDoubleOrNull() ?: 2.00,
                                maxQuantity = maxQtyStr.toIntOrNull() ?: 3
                            )
                            onSave(addon)
                        }
                    }
                ) {
                    Text(Translations.get("action_save", language))
                }
            }
        }
    }
}

@Composable
fun CreateComboDialog(
    products: List<Product>,
    categories: List<Category>,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (ComboProduct) -> Unit
) {
    var nameEn by remember { mutableStateOf("") }
    var nameAr by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("CMB-00${(10..99).random()}") }
    var basePriceStr by remember { mutableStateOf("22.50") }

    RestoModalDialog(
        title = Translations.get("btn_new_combo", language),
        onDismissRequest = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RestoTextInput(value = nameEn, onValueChange = { nameEn = it }, label = "Combo Name (EN)", modifier = Modifier.fillMaxWidth())
            RestoTextInput(value = nameAr, onValueChange = { nameAr = it }, label = "Combo Name (AR)", modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RestoTextInput(value = sku, onValueChange = { sku = it }, label = "Combo SKU", modifier = Modifier.weight(1f))
                RestoTextInput(value = basePriceStr, onValueChange = { basePriceStr = it }, label = "Base Bundle Price ($)", modifier = Modifier.weight(1f))
            }
            Text("Includes default 3-step bundle: Entrée + Side + Drink", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(Translations.get("action_cancel", language))
                }
                Spacer(modifier = Modifier.width(8.dp))
                RestoButton(
                    onClick = {
                        if (nameEn.isNotBlank()) {
                            val combo = ComboProduct(
                                id = "cmb_${UUID.randomUUID().toString().take(6)}",
                                productId = "p_combo_custom",
                                nameEn = nameEn,
                                nameAr = nameAr.ifBlank { nameEn },
                                sku = sku,
                                comboBasePrice = basePriceStr.toDoubleOrNull() ?: 22.50,
                                components = listOf(
                                    ComboComponent("c1", "cmb_new", "Entrée Choice", "اختيار الوجبة الرئيسية", allowedCategoryIds = listOf("cat_burgers")),
                                    ComboComponent("c2", "cmb_new", "Side Dish Choice", "اختيار الطبق الجانبي", allowedCategoryIds = listOf("cat_sides")),
                                    ComboComponent("c3", "cmb_new", "Drink Choice", "اختيار المشروب", allowedCategoryIds = listOf("cat_beverages"))
                                )
                            )
                            onSave(combo)
                        }
                    }
                ) {
                    Text(Translations.get("action_save", language))
                }
            }
        }
    }
}
