package com.example.data.repository

import com.example.domain.model.*

object DefaultSeedData {

    val categories = listOf(
        Category("cat_burgers", "brand_01", "Craft Burgers", "برجر حرفي فاخر", 1, "#F59E0B", parentId = null),
        Category("cat_burgers_angus", "brand_01", "Angus Reserve Burgers", "برجر أنجوس ريزيرف", 1, "#F59E0B", parentId = "cat_burgers"),
        Category("cat_burgers_chicken", "brand_01", "Crispy Chicken Burgers", "برجر دجاج مقرمش", 2, "#F59E0B", parentId = "cat_burgers"),
        Category("cat_sides", "brand_01", "Artisan Sides", "أطباق جانبية حرفية", 2, "#10B981", parentId = null),
        Category("cat_sides_fries", "brand_01", "Gourmet Fries & Loaded", "بطاطس مقلية مميزة", 1, "#10B981", parentId = "cat_sides"),
        Category("cat_beverages", "brand_01", "Craft Beverages", "مشروبات مميزة", 3, "#3B82F6", parentId = null),
        Category("cat_bev_coldbrew", "brand_01", "Nitro Cold Brews", "قهوة كولد برو نيترو", 1, "#3B82F6", parentId = "cat_beverages"),
        Category("cat_bev_refreshers", "brand_01", "Fresh Lemonades", "عصائر وليمونادا", 2, "#3B82F6", parentId = "cat_beverages"),
        Category("cat_combos", "brand_01", "Combo Meals & Bundles", "وجبات كومبو وباقات", 4, "#8B5CF6", parentId = null),
        Category("cat_dessert", "brand_01", "Artisan Desserts", "حلويات فاخرة", 5, "#EC4899", parentId = null),
        Category("cat_pizza", "brand_01", "Woodfire Artisan Pizza", "بيتزا حطبية حرفية", 6, "#DC2626", parentId = null)
    )

    val modifierGroups = listOf(
        ModifierGroup(
            id = "mg_doneness",
            nameEn = "Meat Temperature",
            nameAr = "درجة استواء اللحم",
            isRequired = true,
            minSelections = 1,
            maxSelections = 1,
            options = listOf(
                ModifierOption("opt_don_mr", "mg_doneness", "Medium Rare", "نصف استواء أحمر", 0.0, isDefault = false, calories = 0),
                ModifierOption("opt_don_med", "mg_doneness", "Medium (Chef Recommendation)", "متوسط الاستواء (توصية الشيف)", 0.0, isDefault = true, calories = 0),
                ModifierOption("opt_don_mw", "mg_doneness", "Medium Well", "مطبوخ جيداً", 0.0, isDefault = false, calories = 0),
                ModifierOption("opt_don_wd", "mg_doneness", "Well Done", "كامل الاستواء", 0.0, isDefault = false, calories = 0)
            )
        ),
        ModifierGroup(
            id = "mg_bun",
            nameEn = "Artisan Bun Choice",
            nameAr = "نوع الخبز الحرفي",
            isRequired = true,
            minSelections = 1,
            maxSelections = 1,
            options = listOf(
                ModifierOption("opt_bun_brioche", "mg_bun", "Golden Butter Brioche", "خبز بريوش زبدي ذهبي", 0.0, isDefault = true, calories = 180),
                ModifierOption("opt_bun_gf", "mg_bun", "Gluten-Free Artisan Bun", "خبز خالي من الغلوتين", 1.50, isDefault = false, calories = 140),
                ModifierOption("opt_bun_lettuce", "mg_bun", "Crispy Iceberg Lettuce Wrap", "لفافة خس آيسبيرغ مقرمشة (كيتو)", 0.0, isDefault = false, calories = 25)
            )
        ),
        ModifierGroup(
            id = "mg_cheese",
            nameEn = "Cheese Selection",
            nameAr = "خيارات الجبن المعتق",
            isRequired = false,
            minSelections = 0,
            maxSelections = 1,
            options = listOf(
                ModifierOption("opt_ch_cheddar", "mg_cheese", "Vintage English Cheddar", "جبن شيدر إنجليزي معتق", 0.0, isDefault = true, calories = 110),
                ModifierOption("opt_ch_emmental", "mg_cheese", "Swiss Emmental Melt", "جبن إيمنتال سويسري ذائب", 1.00, isDefault = false, calories = 120),
                ModifierOption("opt_ch_stilton", "mg_cheese", "Crumbled Blue Stilton", "جبن أزرق ستيلتون فاخر", 1.50, isDefault = false, calories = 130),
                ModifierOption("opt_ch_none", "mg_cheese", "No Cheese (Dairy-Free)", "بدون جبن (خالي من المشتقات)", 0.0, isDefault = false, calories = 0)
            )
        ),
        ModifierGroup(
            id = "mg_spice_level",
            nameEn = "Nashville Spice Level",
            nameAr = "مستوى حرارة ناشفيل",
            isRequired = true,
            minSelections = 1,
            maxSelections = 1,
            options = listOf(
                ModifierOption("opt_sp_mild", "mg_spice_level", "Southern Mild", "خفيف النكهة الجنوبية", 0.0, isDefault = false, calories = 0),
                ModifierOption("opt_sp_hot", "mg_spice_level", "Classic Nashville Hot", "حار ناشفيل الأصلي", 0.0, isDefault = true, calories = 0),
                ModifierOption("opt_sp_atomic", "mg_spice_level", "Atomic Ghost Pepper Heat", "حار شبح أتوميك ناري", 0.75, isDefault = false, calories = 10)
            )
        ),
        ModifierGroup(
            id = "mg_sauce",
            nameEn = "Signature Dip Sauces",
            nameAr = "صلصات التغميس الخاصة",
            isRequired = false,
            minSelections = 0,
            maxSelections = 3,
            options = listOf(
                ModifierOption("opt_sc_truffle", "mg_sauce", "Black Truffle Garlic Aioli", "مايونيز الكمأة السوداء بالثوم", 0.75, isDefault = true, calories = 140),
                ModifierOption("opt_sc_ranch", "mg_sauce", "Buttermilk Herb Ranch", "رانش بالأعشاب الطبيعية", 0.50, isDefault = false, calories = 110),
                ModifierOption("opt_sc_bbq", "mg_sauce", "Applewood Bourbon BBQ", "باربيكيو مدخن بخشب التفاح", 0.75, isDefault = false, calories = 90)
            )
        )
    )

    val addons = listOf(
        ProductAddon("add_bacon", "p1", "Crispy Beef Bacon Strips (2pcs)", "شرائح لحم بقري مقدد مقرمش", 2.50, 3),
        ProductAddon("add_truffle_aioli", "p1", "Extra Truffle Aioli Dip", "صوص مايونيز كمأة إضافي", 1.50, 4),
        ProductAddon("add_egg", "p1", "Fried Cage-Free Sunny Egg", "بيض مقلي بلدي طازج", 1.75, 2),
        ProductAddon("add_cheese", "p1", "Extra Melted Cheddar Slice", "شريحة جبن شيدر ذائبة إضافية", 1.50, 3),
        ProductAddon("add_patty", "p1", "Extra 150g Angus Beef Patty", "شريحة لحم أنجوس إضافية 150 جم", 4.50, 2)
    )

    val ingredients = listOf(
        Ingredient("ing_beef", "ING-001", "Black Angus Ground Beef 80/20", "لحم عجل أنجوس أسود مفروم", "kg", 12.00, 180.0, 0.88, 0.04),
        Ingredient("ing_brioche", "ING-002", "Artisan Brioche Burger Buns", "خبز بريوش زبدي حرفي", "pcs", 0.65, 350.0, 1.0, 0.02),
        Ingredient("ing_truffle_aioli", "ING-003", "House Black Truffle Aioli", "مايونيز الكمأة السوداء المحضر", "kg", 16.50, 40.0, 0.95, 0.03),
        Ingredient("ing_cheddar", "ING-004", "Vintage English Cheddar Slices", "شرائح جبن شيدر إنجليزي معتق", "kg", 14.00, 50.0, 0.98, 0.02),
        Ingredient("ing_bacon", "ING-005", "Applewood Smoked Beef Bacon", "لحم بقري مقدد مدخن بخشب التفاح", "kg", 18.00, 45.0, 0.82, 0.06),
        Ingredient("ing_chicken", "ING-006", "Fresh Halal Chicken Breast", "صدور دجاج طازجة حلال", "kg", 7.50, 140.0, 0.90, 0.05),
        Ingredient("ing_potatoes", "ING-007", "Russet Potatoes Grade A", "بطاطس راست منتقاة درجة أولى", "kg", 1.80, 450.0, 0.85, 0.08),
        Ingredient("ing_parm", "ING-008", "Shaved Parmigiano-Reggiano", "جبن بارميزان إيطالي مبشور", "kg", 24.00, 30.0, 0.98, 0.01),
        Ingredient("ing_passionfruit", "ING-009", "Organic Passionfruit Purée", "مهروس فاكهة الباشن فروت العضوي", "liter", 9.00, 75.0, 0.98, 0.02),
        Ingredient("ing_coffee", "ING-010", "Ethiopian Yirgacheffe Single Origin", "حبوب بن إثيوبي ييرغاتشيفي", "kg", 22.00, 60.0, 0.92, 0.03),
        Ingredient("ing_box", "ING-011", "Biodegradable Gourmet Burger Box", "علبة برجر ورقية قابلة للتحلل", "pcs", 0.35, 1200.0, 1.0, 0.0),
        Ingredient("ing_cup", "ING-012", "Cold Drink PLA Cup & Sip Lid", "كوب مشروبات باردة مع غطاء", "pcs", 0.25, 1500.0, 1.0, 0.0)
    )

    val recipes = listOf(
        Recipe(
            id = "rec_p1",
            productId = "p1",
            name = "Truffle Angus Burger Standard Portion Recipe",
            yieldCount = 1.0,
            packagingCost = 0.35,
            laborCost = 0.85,
            items = listOf(
                RecipeItem("ing_beef", "Black Angus Beef", 0.180, "kg"),
                RecipeItem("ing_brioche", "Brioche Bun", 1.0, "pcs"),
                RecipeItem("ing_truffle_aioli", "Truffle Aioli", 0.035, "kg"),
                RecipeItem("ing_cheddar", "Aged Cheddar", 0.040, "kg"),
                RecipeItem("ing_box", "Burger Box", 1.0, "pcs", isPackaging = true)
            )
        ),
        Recipe(
            id = "rec_p2",
            productId = "p2",
            name = "Smoked BBQ Bacon Burger Recipe",
            yieldCount = 1.0,
            packagingCost = 0.35,
            laborCost = 0.85,
            items = listOf(
                RecipeItem("ing_beef", "Black Angus Beef", 0.180, "kg"),
                RecipeItem("ing_brioche", "Brioche Bun", 1.0, "pcs"),
                RecipeItem("ing_bacon", "Smoked Bacon", 0.045, "kg"),
                RecipeItem("ing_box", "Burger Box", 1.0, "pcs", isPackaging = true)
            )
        ),
        Recipe(
            id = "rec_p3",
            productId = "p3",
            name = "Nashville Hot Chicken Recipe",
            yieldCount = 1.0,
            packagingCost = 0.35,
            laborCost = 0.75,
            items = listOf(
                RecipeItem("ing_chicken", "Chicken Breast", 0.220, "kg"),
                RecipeItem("ing_brioche", "Brioche Bun", 1.0, "pcs"),
                RecipeItem("ing_box", "Burger Box", 1.0, "pcs", isPackaging = true)
            )
        ),
        Recipe(
            id = "rec_p4",
            productId = "p4",
            name = "Loaded Truffle Fries Recipe",
            yieldCount = 1.0,
            packagingCost = 0.25,
            laborCost = 0.45,
            items = listOf(
                RecipeItem("ing_potatoes", "Russet Potatoes", 0.250, "kg"),
                RecipeItem("ing_truffle_aioli", "Truffle Oil/Aioli", 0.020, "kg"),
                RecipeItem("ing_parm", "Parmigiano", 0.025, "kg")
            )
        ),
        Recipe(
            id = "rec_p6",
            productId = "p6",
            name = "Passionfruit Lemonade Recipe",
            yieldCount = 1.0,
            packagingCost = 0.25,
            laborCost = 0.30,
            items = listOf(
                RecipeItem("ing_passionfruit", "Passionfruit Purée", 0.080, "liter"),
                RecipeItem("ing_cup", "Cold Cup & Lid", 1.0, "pcs", isPackaging = true)
            )
        ),
        Recipe(
            id = "rec_p7",
            productId = "p7",
            name = "Nitro Cold Brew Coffee Recipe",
            yieldCount = 1.0,
            packagingCost = 0.25,
            laborCost = 0.30,
            items = listOf(
                RecipeItem("ing_coffee", "Ethiopian Beans", 0.045, "kg"),
                RecipeItem("ing_cup", "Cold Cup & Lid", 1.0, "pcs", isPackaging = true)
            )
        )
    )

    val products = listOf(
        Product(
            id = "p1",
            categoryId = "cat_burgers_angus",
            sku = "BUR-001",
            nameEn = "Truffle Angus Burger",
            nameAr = "برجر أنجوس بالكمأة",
            price = 18.50,
            costPrice = 6.42,
            isAvailable = true,
            isTaxable = true,
            currentStock = 42.0,
            reorderPoint = 10.0,
            barcode = "6281001001",
            descriptionEn = "Prime black angus beef, black truffle aioli, aged cheddar on toasted brioche bun",
            descriptionAr = "لحم أنجوس أسود فاخر، مايونيز الكمأة السوداء، جبن شيدر معتق وخبز بريوش محمص",
            imageUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd",
            isSellable = true,
            isPreparationItem = true,
            isInventoryItem = true,
            variants = listOf(
                ProductVariant("v_p1_s", "p1", "Single (150g)", "شريحة واحدة (150 جم)", "BUR-001-S", 0.0, "6281001001-S", isDefault = true),
                ProductVariant("v_p1_d", "p1", "Double (300g)", "شريحتين (300 جم)", "BUR-001-D", 4.50, "6281001001-D"),
                ProductVariant("v_p1_t", "p1", "Triple Beast (450g)", "ثلاث شرائح (450 جم)", "BUR-001-T", 8.00, "6281001001-T")
            ),
            modifierGroupIds = listOf("mg_doneness", "mg_bun", "mg_cheese"),
            addonIds = listOf("add_bacon", "add_truffle_aioli", "add_egg", "add_patty"),
            taxProfileId = "tax_standard_10"
        ),
        Product(
            id = "p2",
            categoryId = "cat_burgers_angus",
            sku = "BUR-002",
            nameEn = "Smoked BBQ Bacon Burger",
            nameAr = "برجر باربيكيو باللحم المقدد",
            price = 16.00,
            costPrice = 5.65,
            isAvailable = true,
            isTaxable = true,
            currentStock = 35.0,
            reorderPoint = 8.0,
            barcode = "6281001002",
            descriptionEn = "Smoked angus patty, applewood bacon, house smoked BBQ sauce, crispy onions",
            descriptionAr = "شريحة أنجوس مدخنة، لحم مقدد بخشب التفاح، صلصة باربيكيو مدخنة وبصل مقرمش",
            imageUrl = "https://images.unsplash.com/photo-1553979459-d2229ba7433b",
            isSellable = true,
            isPreparationItem = true,
            isInventoryItem = true,
            variants = listOf(
                ProductVariant("v_p2_s", "p2", "Single (150g)", "شريحة واحدة (150 جم)", "BUR-002-S", 0.0, isDefault = true),
                ProductVariant("v_p2_d", "p2", "Double (300g)", "شريحتين (300 جم)", "BUR-002-D", 4.50)
            ),
            modifierGroupIds = listOf("mg_doneness", "mg_bun"),
            addonIds = listOf("add_bacon", "add_cheese"),
            taxProfileId = "tax_standard_10"
        ),
        Product(
            id = "p3",
            categoryId = "cat_burgers_chicken",
            sku = "BUR-003",
            nameEn = "Crispy Nashville Hot Chicken",
            nameAr = "برجر دجاج ناشفيل المقرمش",
            price = 14.50,
            costPrice = 4.90,
            isAvailable = true,
            isTaxable = true,
            currentStock = 28.0,
            reorderPoint = 6.0,
            barcode = "6281001003",
            descriptionEn = "Double-dredged buttermilk fried chicken breast, habanero-cayenne glaze, creamy slaw",
            descriptionAr = "صدر دجاج مقلي مقرمش منقوع بالحليب، صوص هابانيرو حار، وسلطة كولسلو كريمية",
            imageUrl = "https://images.unsplash.com/photo-1625813506062-0aeb1d7a094b",
            isSellable = true,
            isPreparationItem = true,
            isInventoryItem = true,
            variants = listOf(
                ProductVariant("v_p3_mild", "p3", "Classic Mild", "كلاسيكي هادئ", "BUR-003-M", 0.0, isDefault = true),
                ProductVariant("v_p3_hot", "p3", "Nashville Hot", "ناشفيل حار", "BUR-003-H", 0.50),
                ProductVariant("v_p3_fiery", "p3", "Extra Fiery Atomic", "ناري حارق إضافي", "BUR-003-X", 1.00)
            ),
            modifierGroupIds = listOf("mg_spice_level", "mg_sauce"),
            addonIds = listOf("add_cheese"),
            taxProfileId = "tax_standard_10"
        ),
        Product(
            id = "p4",
            categoryId = "cat_sides_fries",
            sku = "SID-001",
            nameEn = "Loaded Truffle Fries",
            nameAr = "بطاطس محمرة بالكمأة والبارميزان",
            price = 7.50,
            costPrice = 2.15,
            isAvailable = true,
            isTaxable = true,
            currentStock = 55.0,
            reorderPoint = 12.0,
            barcode = "6281002001",
            descriptionEn = "Hand-cut russet fries tossed in white truffle oil, rosemary, shaved parmigiano",
            descriptionAr = "بطاطس مقطعة يدوياً متبلة بزيت الكمأة البيضاء، إكليل الجبل وجبن بارميزان مبشور",
            imageUrl = "https://images.unsplash.com/photo-1573080496219-bb080dd4f877",
            isSellable = true,
            isPreparationItem = true,
            isInventoryItem = true,
            variants = listOf(
                ProductVariant("v_p4_reg", "p4", "Regular Basket", "سلة عادية", "SID-001-R", 0.0, isDefault = true),
                ProductVariant("v_p4_lrg", "p4", "Jumbo Share Basket", "سلة مشاركة عائلية", "SID-001-L", 2.50)
            ),
            taxProfileId = "tax_standard_10"
        ),
        Product(
            id = "p5",
            categoryId = "cat_sides",
            sku = "SID-002",
            nameEn = "Artisan Onion Rings",
            nameAr = "حلقات البصل المكرملة المقرمشة",
            price = 6.00,
            costPrice = 1.85,
            isAvailable = true,
            isTaxable = true,
            currentStock = 4.0,
            reorderPoint = 8.0,
            barcode = "6281002002",
            descriptionEn = "Beer-battered jumbo sweet yellow onions with smoked garlic dip",
            descriptionAr = "حلقات بصل حلوة مقرمشة مع صلصة الثوم المدخن",
            imageUrl = "https://images.unsplash.com/photo-1639024471287-032f667712d5",
            isSellable = true,
            isPreparationItem = true,
            isInventoryItem = true,
            taxProfileId = "tax_standard_10"
        ),
        Product(
            id = "p6",
            categoryId = "cat_bev_refreshers",
            sku = "BEV-001",
            nameEn = "Craft Passionfruit Lemonade",
            nameAr = "ليمونادا باشن فروت طبيعية",
            price = 5.50,
            costPrice = 1.25,
            isAvailable = true,
            isTaxable = true,
            currentStock = 80.0,
            reorderPoint = 15.0,
            barcode = "6281003001",
            descriptionEn = "Cold-pressed lemon juice, Ecuadorian passionfruit purée, mint leaves, sparkling soda",
            descriptionAr = "عصير ليمون معصور على البارد، مهروس الباشن فروت الإكوادوري، نعناع وصودا فوارة",
            imageUrl = "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd",
            isSellable = true,
            isPreparationItem = true,
            isInventoryItem = true,
            variants = listOf(
                ProductVariant("v_p6_reg", "p6", "Regular (400ml)", "حجم عادي (400 مل)", "BEV-001-R", 0.0, isDefault = true),
                ProductVariant("v_p6_lrg", "p6", "Large (600ml)", "حجم كبير (600 مل)", "BEV-001-L", 1.50)
            ),
            taxProfileId = "tax_standard_10"
        ),
        Product(
            id = "p7",
            categoryId = "cat_bev_coldbrew",
            sku = "BEV-002",
            nameEn = "Cold Brew Nitro Espresso",
            nameAr = "قهوة كولد برو نيترو مثلجة",
            price = 6.00,
            costPrice = 1.40,
            isAvailable = true,
            isTaxable = true,
            currentStock = 60.0,
            reorderPoint = 10.0,
            barcode = "6281003002",
            descriptionEn = "18-hour steeped Ethiopian single-origin beans infused with micro-nitrogen velvet foam",
            descriptionAr = "قهوة مقطرة على البارد لمدة 18 ساعة بحبوب إثيوبية ومحقونة بنيتروجين مخملي رغوي",
            imageUrl = "https://images.unsplash.com/photo-1517701550927-30cf4ba1dba5",
            isSellable = true,
            isPreparationItem = true,
            isInventoryItem = true,
            variants = listOf(
                ProductVariant("v_p7_reg", "p7", "Regular", "عادي", "BEV-002-R", 0.0, isDefault = true),
                ProductVariant("v_p7_lrg", "p7", "Large", "كبير", "BEV-002-L", 1.50)
            ),
            taxProfileId = "tax_standard_10"
        ),
        Product(
            id = "p8",
            categoryId = "cat_dessert",
            sku = "DES-001",
            nameEn = "Salted Caramel Brioche Pudding",
            nameAr = "بودينغ بريوش بالكراميل المملح",
            price = 9.00,
            costPrice = 2.80,
            isAvailable = true,
            isTaxable = true,
            currentStock = 18.0,
            reorderPoint = 5.0,
            barcode = "6281004001",
            descriptionEn = "Warm butter brioche soaked in Madagascan vanilla custard, Fleur de Sel caramel, gelato",
            descriptionAr = "بريوش زبدي دافئ مشرب بكاسترد فانيليا مدغشقر، كراميل بملح البحر وجيلاتو",
            imageUrl = "https://images.unsplash.com/photo-1551024601-bec78aea704b",
            isSellable = true,
            isPreparationItem = true,
            isInventoryItem = true,
            taxProfileId = "tax_standard_10"
        ),
        Product(
            id = "p9",
            categoryId = "cat_combos",
            sku = "CMB-001",
            nameEn = "Craft Trio Combo Meal",
            nameAr = "وجبة كومبو ثلاثية كرافت",
            price = 24.50,
            costPrice = 8.80,
            isAvailable = true,
            isTaxable = true,
            currentStock = 100.0,
            reorderPoint = 10.0,
            barcode = "6281005001",
            descriptionEn = "Complete signature meal: Choose 1 Craft Burger + 1 Gourmet Side + 1 Cold Beverage",
            descriptionAr = "وجبة متكاملة: اختر برجر حرفي + طبق جانبي مميز + مشروب بارد منعش",
            imageUrl = "https://images.unsplash.com/photo-1594212699903-ec8a3eca50f5",
            isSellable = true,
            isPreparationItem = true,
            isInventoryItem = false,
            isCombo = true,
            taxProfileId = "tax_standard_10"
        ),
        Product(
            id = "p10",
            categoryId = "cat_pizza",
            sku = "PIZ-001",
            nameEn = "Woodfire Truffle Burrata Pizza",
            nameAr = "بيتزا بوراتا بالكمأة الحطبية",
            price = 19.50,
            costPrice = 6.10,
            isAvailable = true,
            isTaxable = true,
            currentStock = 30.0,
            reorderPoint = 8.0,
            barcode = "6281006001",
            descriptionEn = "72-hour fermented sourdough crust, fresh Puglia burrata, black truffle cream, wild basil",
            descriptionAr = "عجينة ساوردو مخمرة 72 ساعة، جبن بوراتا طازج، كريمة الكمأة السوداء وريحان بري",
            imageUrl = "https://images.unsplash.com/photo-1513104890138-7c749659a591",
            isSellable = true,
            isPreparationItem = true,
            isInventoryItem = true,
            taxProfileId = "tax_standard_10"
        )
    )

    val combos = listOf(
        ComboProduct(
            id = "cmb_01",
            productId = "p9",
            nameEn = "Craft Trio Combo Meal",
            nameAr = "وجبة كومبو ثلاثية كرافت",
            sku = "CMB-001",
            comboBasePrice = 24.50,
            components = listOf(
                ComboComponent("comp_c1", "cmb_01", "Signature Burger Choice", "اختيار البرجر الرئيسي", allowedCategoryIds = listOf("cat_burgers_angus", "cat_burgers_chicken"), defaultProductId = "p1", extraCharge = 0.0),
                ComboComponent("comp_c2", "cmb_01", "Gourmet Side Choice", "اختيار الطبق الجانبي", allowedCategoryIds = listOf("cat_sides", "cat_sides_fries"), defaultProductId = "p4", extraCharge = 0.0),
                ComboComponent("comp_c3", "cmb_01", "Cold Craft Beverage", "اختيار المشروب البارد", allowedCategoryIds = listOf("cat_bev_coldbrew", "cat_bev_refreshers"), defaultProductId = "p6", extraCharge = 0.0)
            )
        )
    )

    val priceRules = listOf(
        // Delivery platforms markup (+15% to 20%)
        ChannelPriceRule("pr_del_p1", "p1", channel = SalesChannel.DELIVERY_PARTNERS, customPrice = 21.50),
        ChannelPriceRule("pr_del_p2", "p2", channel = SalesChannel.DELIVERY_PARTNERS, customPrice = 18.50),
        ChannelPriceRule("pr_del_p3", "p3", channel = SalesChannel.DELIVERY_PARTNERS, customPrice = 16.90),
        ChannelPriceRule("pr_del_p9", "p9", channel = SalesChannel.DELIVERY_PARTNERS, customPrice = 28.50),
        // Airport Express branch markup
        ChannelPriceRule("pr_air_p1", "p1", branchId = "branch_03", customPrice = 20.00),
        ChannelPriceRule("pr_air_p6", "p6", branchId = "branch_03", customPrice = 6.50),
        // QR table ordering special discount
        ChannelPriceRule("pr_qr_p1", "p1", channel = SalesChannel.QR_TABLE, customPrice = 17.50),
        ChannelPriceRule("pr_qr_p9", "p9", channel = SalesChannel.QR_TABLE, customPrice = 22.95)
    )

    val taxProfiles = listOf(
        TaxProfile("tax_standard_10", "VAT 10% Standard (Exclusive)", "ضريبة القيمة المضافة 10% (غير شاملة)", 0.10, isInclusive = false, taxCode = "VAT_10_EXCL"),
        TaxProfile("tax_inclusive_15", "VAT 15% Standard (Inclusive)", "ضريبة القيمة المضافة 15% (شاملة)", 0.15, isInclusive = true, taxCode = "VAT_15_INCL"),
        TaxProfile("tax_exempt_0", "Zero-Rated / Tax Exempt", "معفى من الضريبة (صفر بالمائة)", 0.0, isInclusive = false, taxCode = "VAT_ZERO")
    )

    val customers = listOf(
        Customer("cust_1", "comp_01", "Faisal Al-Rashid", "+966 55 101 2020", "faisal.r@gmail.com", loyaltyPoints = 1480, tier = "VIP_GOLD"),
        Customer("cust_2", "comp_01", "Layla Al-Otaibi", "+966 50 303 4040", "layla.o@outlook.com", loyaltyPoints = 2950, tier = "PLATINUM"),
        Customer("cust_3", "comp_01", "Omar Zaki", "+966 54 505 6060", "omar.zaki@corp.sa", loyaltyPoints = 420, tier = "SILVER"),
        Customer("cust_4", "comp_01", "Noura Mansour", "+966 56 707 8080", "noura.m@yahoo.com", loyaltyPoints = 890, tier = "GOLD")
    )

    val coupons = listOf(
        Coupon("WELCOME10", "10% Welcome Discount", "خصم الترحيب 10%", DiscountType.PERCENTAGE, 10.0, minOrderAmount = 10.0, maxDiscountAmount = 15.0),
        Coupon("VIP20", "20% VIP Loyalty Reward", "مكافأة ولاء كبار العملاء 20%", DiscountType.PERCENTAGE, 20.0, minOrderAmount = 25.0, maxDiscountAmount = 30.0),
        Coupon("BURGER5", "$5 Off Burger Order", "خصم 5 دولار على طلبات البرجر", DiscountType.FIXED_AMOUNT, 5.0, minOrderAmount = 18.0),
        Coupon("LUNCH15", "15% Business Lunch Promo", "عرض غداء العمل 15%", DiscountType.PERCENTAGE, 15.0, minOrderAmount = 15.0, maxDiscountAmount = 20.0)
    )

    val initialShift = CashierShift(
        id = "shift_morning_01",
        branchId = "branch_01",
        terminalId = "POS-01",
        cashierId = "user_03",
        cashierName = "Youssef Zaid",
        status = ShiftStatus.OPEN,
        openedAt = System.currentTimeMillis() - (4 * 3600 * 1000L),
        openingCash = 250.00,
        cashSales = 142.50,
        cardSales = 318.40,
        bankTransferSales = 45.00,
        walletSales = 89.20,
        cashIn = 50.00,
        cashOut = 0.00,
        expenses = 15.00,
        cashRefunds = 0.00,
        movements = listOf(
            CashDrawerMovement(
                id = "mov_01",
                shiftId = "shift_morning_01",
                branchId = "branch_01",
                type = CashMovementType.CASH_IN,
                amount = 50.00,
                reason = "Small denomination coin & bill change replenish",
                performedBy = "Youssef Zaid",
                timestamp = System.currentTimeMillis() - (3 * 3600 * 1000L)
            ),
            CashDrawerMovement(
                id = "mov_02",
                shiftId = "shift_morning_01",
                branchId = "branch_01",
                type = CashMovementType.EXPENSE,
                amount = 15.00,
                reason = "Express ice bags & receipt thermal paper rolls",
                performedBy = "Youssef Zaid",
                approvedBy = "Karim Mansour",
                timestamp = System.currentTimeMillis() - (90 * 60 * 1000L)
            )
        )
    )

    val floors = listOf(
        RestaurantFloor("floor_01", "branch_01", "Ground Floor Dining", "الطابق الأرضي الرئيسي", 1),
        RestaurantFloor("floor_02", "branch_01", "Upper Mezzanine & VIP", "الميزانين العلوي وكبار الشخصيات", 2),
        RestaurantFloor("floor_03", "branch_01", "Outdoor Garden & Patio", "الحديقة الخارجية والتراس", 3)
    )

    val zones = listOf(
        TableZone("zone_indoor", "floor_01", "branch_01", "Indoor Main", "الصالة الداخلية الرئيسية", "#3B82F6"),
        TableZone("zone_vip", "floor_02", "branch_01", "VIP Suite", "جناح كبار الشخصيات VIP", "#8B5CF6"),
        TableZone("zone_terrace", "floor_03", "branch_01", "Terrace Garden", "حديقة التراس", "#10B981")
    )

    val reservations = listOf(
        Reservation(
            id = "res_01",
            branchId = "branch_01",
            customerId = "cust_1",
            customerName = "Faisal Al-Rashid",
            customerPhone = "+966 55 101 2020",
            customerEmail = "faisal.r@gmail.com",
            tableId = "t5",
            tableNumber = "T-05",
            reservationDate = "2026-10-24",
            reservationTime = "20:00",
            partySize = 6,
            status = ReservationStatus.CONFIRMED,
            notes = "VIP executive dinner, quiet booth requested, sparkling water on arrival",
            confirmationCode = "RES-8841"
        ),
        Reservation(
            id = "res_02",
            branchId = "branch_01",
            customerId = "cust_4",
            customerName = "Noura Mansour",
            customerPhone = "+966 56 707 8080",
            customerEmail = "noura.m@yahoo.com",
            tableId = "t6",
            tableNumber = "T-06",
            reservationDate = "2026-10-24",
            reservationTime = "21:30",
            partySize = 4,
            status = ReservationStatus.PENDING,
            notes = "Anniversary celebration, terrace garden table",
            confirmationCode = "RES-9102"
        ),
        Reservation(
            id = "res_03",
            branchId = "branch_01",
            customerId = "cust_3",
            customerName = "Omar Zaki",
            customerPhone = "+966 54 505 6060",
            customerEmail = "omar.zaki@corp.sa",
            tableId = "t2",
            tableNumber = "T-02",
            reservationDate = "2026-10-24",
            reservationTime = "19:00",
            partySize = 3,
            status = ReservationStatus.SEATED,
            notes = "Business casual dinner",
            confirmationCode = "RES-7720"
        )
    )

    // --- Phase 6: Kitchens, Preparation Areas, Stations, Kitchen Employees & Routing Rules ---
    val kitchens = listOf(
        Kitchen(
            id = "kit_main",
            branchId = "branch_01",
            code = "KIT-HOT-01",
            nameEn = "Main Culinary Hot Kitchen",
            nameAr = "المطبخ الساخن الرئيسي",
            description = "Primary Grill, Fryer & Assembly Pass Kitchen",
            defaultWarningMinutes = 8,
            defaultOverdueMinutes = 15
        ),
        Kitchen(
            id = "kit_woodfire",
            branchId = "branch_01",
            code = "KIT-OVEN-02",
            nameEn = "Woodfire & Pastry Kitchen",
            nameAr = "مطبخ الفرن الحطبي والحلويات",
            description = "Artisan Woodfire Pizza Oven & Warm Dessert Bakery",
            defaultWarningMinutes = 10,
            defaultOverdueMinutes = 18
        ),
        Kitchen(
            id = "kit_coldbar",
            branchId = "branch_01",
            code = "KIT-BAR-03",
            nameEn = "Cold Bar & Craft Beverage Lab",
            nameAr = "بار المشروبات الباردة والنيترو",
            description = "Nitro Cold Brew, Fresh Pressed Lemonades & Cold Prep",
            defaultWarningMinutes = 4,
            defaultOverdueMinutes = 8
        )
    )

    val preparationAreas = listOf(
        PreparationArea("prep_hot", "kit_main", "branch_01", "AREA-HOT", "Hot Line & Smokehouse Area", "منطقة الخط الساخن والشواء"),
        PreparationArea("prep_oven", "kit_woodfire", "branch_01", "AREA-OVEN", "Woodfire & Pastry Area", "منطقة الفرن الحطبي والمخبوزات"),
        PreparationArea("prep_cold", "kit_coldbar", "branch_01", "AREA-BAR", "Beverage & Cold Dispense Area", "منطقة تحضير المشروبات والبار"),
        PreparationArea("prep_pass", "kit_main", "branch_01", "AREA-PASS", "Packing & Expediter Pass Area", "منطقة التغليف والتجميع النهائي")
    )

    val kitchenStations = listOf(
        KitchenStation(
            id = "st_grill",
            kitchenId = "kit_main",
            prepAreaId = "prep_hot",
            prepAreaName = "Hot Line & Smokehouse Area",
            branchId = "branch_01",
            code = "ST-GRILL",
            stationType = KitchenStationType.GRILL,
            nameEn = "Grill Station",
            nameAr = "محطة الشواء والجريل",
            warningThresholdMinutes = 8,
            overdueThresholdMinutes = 15,
            assignedEmployeeIds = listOf("emp_05"),
            colorHex = "#EA580C"
        ),
        KitchenStation(
            id = "st_pizza",
            kitchenId = "kit_woodfire",
            prepAreaId = "prep_oven",
            prepAreaName = "Woodfire & Pastry Area",
            branchId = "branch_01",
            code = "ST-PIZZA",
            stationType = KitchenStationType.PIZZA,
            nameEn = "Pizza & Woodfire Oven",
            nameAr = "محطة البيتزا والفرن",
            warningThresholdMinutes = 10,
            overdueThresholdMinutes = 18,
            assignedEmployeeIds = listOf("emp_k2"),
            colorHex = "#DC2626"
        ),
        KitchenStation(
            id = "st_fryer",
            kitchenId = "kit_main",
            prepAreaId = "prep_hot",
            prepAreaName = "Hot Line & Smokehouse Area",
            branchId = "branch_01",
            code = "ST-FRYER",
            stationType = KitchenStationType.FRYER,
            nameEn = "Fryer Station",
            nameAr = "محطة القلي المقرمش",
            warningThresholdMinutes = 6,
            overdueThresholdMinutes = 12,
            assignedEmployeeIds = listOf("emp_k3"),
            colorHex = "#D97706"
        ),
        KitchenStation(
            id = "st_drinks",
            kitchenId = "kit_coldbar",
            prepAreaId = "prep_cold",
            prepAreaName = "Beverage & Cold Dispense Area",
            branchId = "branch_01",
            code = "ST-DRINKS",
            stationType = KitchenStationType.DRINKS,
            nameEn = "Drinks & Bar Station",
            nameAr = "محطة المشروبات والبار",
            warningThresholdMinutes = 4,
            overdueThresholdMinutes = 8,
            assignedEmployeeIds = listOf("emp_k4"),
            colorHex = "#2563EB"
        ),
        KitchenStation(
            id = "st_dessert",
            kitchenId = "kit_woodfire",
            prepAreaId = "prep_oven",
            prepAreaName = "Woodfire & Pastry Area",
            branchId = "branch_01",
            code = "ST-DESSERT",
            stationType = KitchenStationType.DESSERT,
            nameEn = "Dessert & Pastry Station",
            nameAr = "محطة الحلويات",
            warningThresholdMinutes = 6,
            overdueThresholdMinutes = 12,
            assignedEmployeeIds = listOf("emp_k5"),
            colorHex = "#DB2777"
        ),
        KitchenStation(
            id = "st_packing",
            kitchenId = "kit_main",
            prepAreaId = "prep_pass",
            prepAreaName = "Packing & Expediter Pass Area",
            branchId = "branch_01",
            code = "ST-PACKING",
            stationType = KitchenStationType.PACKING,
            nameEn = "Packing Station",
            nameAr = "محطة التغليف والتعبئة",
            warningThresholdMinutes = 5,
            overdueThresholdMinutes = 10,
            assignedEmployeeIds = listOf("emp_k6"),
            colorHex = "#7C3AED"
        ),
        KitchenStation(
            id = "st_expediter",
            kitchenId = "kit_main",
            prepAreaId = "prep_pass",
            prepAreaName = "Packing & Expediter Pass Area",
            branchId = "branch_01",
            code = "ST-EXPEDITER",
            stationType = KitchenStationType.EXPEDITER,
            nameEn = "Expediter / Pass Station",
            nameAr = "محطة التجميع والفحص النهائي",
            warningThresholdMinutes = 5,
            overdueThresholdMinutes = 10,
            assignedEmployeeIds = listOf("emp_05", "emp_k6"),
            colorHex = "#059669"
        )
    )

    val kitchenEmployees = listOf(
        KitchenEmployeeAssignment("ke_01", "branch_01", "emp_05", "Chef Antoine Khalil", "Executive Sous Chef", "kit_main", "st_grill", "Grill Station", KitchenStationType.GRILL, isOnDuty = true, ticketsCompletedCount = 28),
        KitchenEmployeeAssignment("ke_02", "branch_01", "emp_k2", "Chef Marco Rossi", "Master Pizzaiolo", "kit_woodfire", "st_pizza", "Pizza & Woodfire Oven", KitchenStationType.PIZZA, isOnDuty = true, ticketsCompletedCount = 19),
        KitchenEmployeeAssignment("ke_03", "branch_01", "emp_k3", "Chef Hassan Al-Harbi", "Hot Line Fry Chef", "kit_main", "st_fryer", "Fryer Station", KitchenStationType.FRYER, isOnDuty = true, ticketsCompletedCount = 34),
        KitchenEmployeeAssignment("ke_04", "branch_01", "emp_k4", "Barista Rami Mansour", "Beverage & Nitro Specialist", "kit_coldbar", "st_drinks", "Drinks & Bar Station", KitchenStationType.DRINKS, isOnDuty = true, ticketsCompletedCount = 41),
        KitchenEmployeeAssignment("ke_05", "branch_01", "emp_k5", "Pastry Chef Nadia Saleh", "Pastry & Gelato Chef", "kit_woodfire", "st_dessert", "Dessert & Pastry Station", KitchenStationType.DESSERT, isOnDuty = true, ticketsCompletedCount = 15),
        KitchenEmployeeAssignment("ke_06", "branch_01", "emp_k6", "Zaid Al-Qahtani", "Lead Expediter & Packing Pass", "kit_main", "st_expediter", "Expediter / Pass Station", KitchenStationType.EXPEDITER, isOnDuty = true, ticketsCompletedCount = 46)
    )

    val stationRoutingRules = listOf(
        StationRoutingRule(
            id = "rr_grill_burgers",
            branchId = "branch_01",
            nameEn = "Angus Burgers & Combos -> Grill Station",
            nameAr = "برجر الأنجوس والكومبو -> محطة الشواء والجريل",
            categoryId = "cat_burgers_angus",
            targetStationId = "st_grill",
            targetStationType = KitchenStationType.GRILL,
            targetStationName = "Grill Station"
        ),
        StationRoutingRule(
            id = "rr_grill_parent",
            branchId = "branch_01",
            nameEn = "Craft Burgers & Combos -> Grill Station",
            nameAr = "البرجر الحرفي -> محطة الشواء والجريل",
            categoryId = "cat_combos",
            targetStationId = "st_grill",
            targetStationType = KitchenStationType.GRILL,
            targetStationName = "Grill Station"
        ),
        StationRoutingRule(
            id = "rr_pizza_oven",
            branchId = "branch_01",
            nameEn = "Woodfire Pizza -> Pizza & Oven Station",
            nameAr = "البيتزا الحطبية -> محطة البيتزا والفرن",
            categoryId = "cat_pizza",
            targetStationId = "st_pizza",
            targetStationType = KitchenStationType.PIZZA,
            targetStationName = "Pizza & Woodfire Oven"
        ),
        StationRoutingRule(
            id = "rr_fryer_chicken",
            branchId = "branch_01",
            nameEn = "Crispy Chicken Burgers -> Fryer Station",
            nameAr = "برجر الدجاج المقرمش -> محطة القلي المقرمش",
            categoryId = "cat_burgers_chicken",
            targetStationId = "st_fryer",
            targetStationType = KitchenStationType.FRYER,
            targetStationName = "Fryer Station"
        ),
        StationRoutingRule(
            id = "rr_fryer_sides",
            branchId = "branch_01",
            nameEn = "Gourmet Fries & Sides -> Fryer Station",
            nameAr = "البطاطس والأطباق الجانبية -> محطة القلي المقرمش",
            categoryId = "cat_sides",
            targetStationId = "st_fryer",
            targetStationType = KitchenStationType.FRYER,
            targetStationName = "Fryer Station"
        ),
        StationRoutingRule(
            id = "rr_drinks_bar",
            branchId = "branch_01",
            nameEn = "Cold Brews & Lemonades -> Drinks & Bar Station",
            nameAr = "المشروبات الباردة والليمونادا -> محطة المشروبات والبار",
            categoryId = "cat_beverages",
            targetStationId = "st_drinks",
            targetStationType = KitchenStationType.DRINKS,
            targetStationName = "Drinks & Bar Station"
        ),
        StationRoutingRule(
            id = "rr_dessert_pastry",
            branchId = "branch_01",
            nameEn = "Artisan Desserts -> Dessert & Pastry Station",
            nameAr = "الحلويات الفاخرة -> محطة الحلويات",
            categoryId = "cat_dessert",
            targetStationId = "st_dessert",
            targetStationType = KitchenStationType.DESSERT,
            targetStationName = "Dessert & Pastry Station"
        )
    )

    val initialKitchenTickets: List<KitchenTicket>
        get() {
            val now = System.currentTimeMillis()
            return listOf(
                KitchenTicket(
                    id = "kt_ord_demo_1_grill",
                    branchId = "branch_01",
                    orderId = "ord_demo_1",
                    orderNumber = "ORD-1041",
                    stationName = "Grill Station",
                    itemsSummary = "1x Truffle Angus Burger (Single 150g) [Medium, Golden Butter Brioche]",
                    status = "PREPARING",
                    elapsedMinutes = 16, // Overdue example (>= 15m)
                    isPriority = true,
                    kitchenId = "kit_main",
                    prepAreaId = "prep_hot",
                    stationId = "st_grill",
                    stationType = KitchenStationType.GRILL,
                    orderType = OrderType.DINE_IN,
                    channel = SalesChannel.POS,
                    tableId = "t2",
                    tableNumber = "T-02",
                    serverOrCashierName = "Youssef Zaid",
                    customerName = "Faisal Al-Rashid",
                    priority = TicketPriority.VIP_RUSH,
                    ticketStatus = KdsTicketStatus.PREPARING,
                    items = listOf(
                        KitchenTicketItem(
                            id = "kti_1",
                            orderItemId = "oi_1",
                            productId = "p1",
                            productNameEn = "Truffle Angus Burger",
                            productNameAr = "برجر أنجوس بالكمأة",
                            variantName = "Single (150g)",
                            quantity = 1,
                            modifiersSummary = "Medium, Golden Butter Brioche",
                            notes = "Medium, Brioche",
                            stationType = KitchenStationType.GRILL,
                            status = KdsTicketStatus.PREPARING
                        )
                    ),
                    orderNotes = "VIP guest at Table T-02",
                    warningThresholdMinutes = 8,
                    overdueThresholdMinutes = 15,
                    workflowStage = "STATION_PREP",
                    createdAt = now - (16 * 60_000L),
                    acceptedAt = now - (15 * 60_000L),
                    preparingAt = now - (14 * 60_000L),
                    assignedChefName = "Chef Antoine Khalil"
                ),
                KitchenTicket(
                    id = "kt_ord_demo_1_fryer",
                    branchId = "branch_01",
                    orderId = "ord_demo_1",
                    orderNumber = "ORD-1041",
                    stationName = "Fryer Station",
                    itemsSummary = "1x Loaded Truffle Fries (Regular Basket) • Note: Extra crispy",
                    status = "READY",
                    elapsedMinutes = 10, // Warning threshold example
                    isPriority = true,
                    kitchenId = "kit_main",
                    prepAreaId = "prep_hot",
                    stationId = "st_fryer",
                    stationType = KitchenStationType.FRYER,
                    orderType = OrderType.DINE_IN,
                    channel = SalesChannel.POS,
                    tableId = "t2",
                    tableNumber = "T-02",
                    serverOrCashierName = "Youssef Zaid",
                    customerName = "Faisal Al-Rashid",
                    priority = TicketPriority.VIP_RUSH,
                    ticketStatus = KdsTicketStatus.READY,
                    items = listOf(
                        KitchenTicketItem(
                            id = "kti_2",
                            orderItemId = "oi_2",
                            productId = "p4",
                            productNameEn = "Loaded Truffle Fries",
                            productNameAr = "بطاطس محمرة بالكمأة والبارميزان",
                            variantName = "Regular Basket",
                            quantity = 1,
                            notes = "Extra crispy",
                            stationType = KitchenStationType.FRYER,
                            status = KdsTicketStatus.READY
                        )
                    ),
                    orderNotes = "VIP guest at Table T-02",
                    warningThresholdMinutes = 6,
                    overdueThresholdMinutes = 12,
                    workflowStage = "STATION_PREP",
                    createdAt = now - (10 * 60_000L),
                    acceptedAt = now - (9 * 60_000L),
                    preparingAt = now - (8 * 60_000L),
                    readyAt = now - (1 * 60_000L),
                    assignedChefName = "Chef Hassan Al-Harbi"
                ),
                KitchenTicket(
                    id = "kt_ord_demo_3_grill",
                    branchId = "branch_01",
                    orderId = "ord_demo_3",
                    orderNumber = "ORD-1043",
                    stationName = "Grill Station",
                    itemsSummary = "1x Smoked BBQ Bacon Burger (Single 150g)",
                    status = "NEW",
                    elapsedMinutes = 4,
                    isPriority = false,
                    kitchenId = "kit_main",
                    prepAreaId = "prep_hot",
                    stationId = "st_grill",
                    stationType = KitchenStationType.GRILL,
                    orderType = OrderType.DINE_IN,
                    channel = SalesChannel.WAITER_APP,
                    tableId = "t7",
                    tableNumber = "T-07",
                    serverOrCashierName = "Sarah Nader",
                    priority = TicketPriority.NORMAL,
                    ticketStatus = KdsTicketStatus.NEW,
                    items = listOf(
                        KitchenTicketItem(
                            id = "kti_5",
                            orderItemId = "oi_5",
                            productId = "p2",
                            productNameEn = "Smoked BBQ Bacon Burger",
                            productNameAr = "برجر باربيكيو باللحم المقدد",
                            variantName = "Single (150g)",
                            quantity = 1,
                            stationType = KitchenStationType.GRILL,
                            status = KdsTicketStatus.NEW
                        )
                    ),
                    warningThresholdMinutes = 8,
                    overdueThresholdMinutes = 15,
                    workflowStage = "STATION_PREP",
                    createdAt = now - (4 * 60_000L),
                    assignedChefName = "Chef Antoine Khalil"
                ),
                KitchenTicket(
                    id = "kt_ord_demo_3_drinks",
                    branchId = "branch_01",
                    orderId = "ord_demo_3",
                    orderNumber = "ORD-1043",
                    stationName = "Drinks & Bar Station",
                    itemsSummary = "1x Craft Passionfruit Lemonade (Regular 400ml)",
                    status = "ACCEPTED",
                    elapsedMinutes = 3,
                    isPriority = false,
                    kitchenId = "kit_coldbar",
                    prepAreaId = "prep_cold",
                    stationId = "st_drinks",
                    stationType = KitchenStationType.DRINKS,
                    orderType = OrderType.DINE_IN,
                    channel = SalesChannel.WAITER_APP,
                    tableId = "t7",
                    tableNumber = "T-07",
                    serverOrCashierName = "Sarah Nader",
                    priority = TicketPriority.NORMAL,
                    ticketStatus = KdsTicketStatus.ACCEPTED,
                    items = listOf(
                        KitchenTicketItem(
                            id = "kti_6",
                            orderItemId = "oi_6",
                            productId = "p6",
                            productNameEn = "Craft Passionfruit Lemonade",
                            productNameAr = "ليمونادا باشن فروت طبيعية",
                            variantName = "Regular (400ml)",
                            quantity = 1,
                            stationType = KitchenStationType.DRINKS,
                            status = KdsTicketStatus.ACCEPTED
                        )
                    ),
                    warningThresholdMinutes = 4,
                    overdueThresholdMinutes = 8,
                    workflowStage = "STATION_PREP",
                    createdAt = now - (3 * 60_000L),
                    acceptedAt = now - (2 * 60_000L),
                    assignedChefName = "Barista Rami Mansour"
                ),
                KitchenTicket(
                    id = "kt_ord_demo_2_expediter",
                    branchId = "branch_01",
                    orderId = "ord_demo_2",
                    orderNumber = "ORD-1042",
                    stationName = "Expediter / Pass Station",
                    itemsSummary = "2x Crispy Nashville Hot Chicken (Nashville Hot)\n2x Craft Passionfruit Lemonade • Note: Less ice",
                    status = "READY",
                    elapsedMinutes = 6,
                    isPriority = false,
                    kitchenId = "kit_main",
                    prepAreaId = "prep_pass",
                    stationId = "st_expediter",
                    stationType = KitchenStationType.EXPEDITER,
                    orderType = OrderType.DINE_IN,
                    channel = SalesChannel.POS,
                    tableId = "t4",
                    tableNumber = "T-04",
                    serverOrCashierName = "Youssef Zaid",
                    customerName = "Layla Al-Otaibi",
                    priority = TicketPriority.NORMAL,
                    ticketStatus = KdsTicketStatus.READY,
                    items = listOf(
                        KitchenTicketItem(
                            id = "kti_3",
                            orderItemId = "oi_3",
                            productId = "p3",
                            productNameEn = "Crispy Nashville Hot Chicken",
                            productNameAr = "برجر دجاج ناشفيل المقرمش",
                            variantName = "Nashville Hot",
                            quantity = 2,
                            modifiersSummary = "Classic Nashville Hot",
                            stationType = KitchenStationType.EXPEDITER,
                            status = KdsTicketStatus.READY
                        ),
                        KitchenTicketItem(
                            id = "kti_4",
                            orderItemId = "oi_4",
                            productId = "p6",
                            productNameEn = "Craft Passionfruit Lemonade",
                            productNameAr = "ليمونادا باشن فروت طبيعية",
                            variantName = "Regular (400ml)",
                            quantity = 2,
                            notes = "Less ice",
                            stationType = KitchenStationType.EXPEDITER,
                            status = KdsTicketStatus.READY
                        )
                    ),
                    warningThresholdMinutes = 5,
                    overdueThresholdMinutes = 10,
                    workflowStage = "EXPEDITER",
                    createdAt = now - (6 * 60_000L),
                    readyAt = now - (1 * 60_000L),
                    assignedChefName = "Zaid Al-Qahtani"
                )
            )
        }
}

