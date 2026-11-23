package com.surfswirl.bakery.game

/**
 * Everything the board can hold. Items are identified by a string id "chain:level" (for example
 * "bread:3") or "gen:<id>" for generators. These ids are stored in the save file, so NEVER rename or
 * reorder existing ones; only append new levels/chains (see CLAUDE.md "Save compatibility").
 */
data class ChainDef(
    val id: String,
    val name: String,
    /** (item name, art file in assets/items without extension), level 1 first. */
    val items: List<Pair<String, String>>,
    /** Accent color for sparkles and the chain sheet (ARGB). */
    val color: Long,
    /** Collectible chains (coins, rubies, energy) are tapped to collect instead of served. */
    val collect: Currency? = null,
    /** Set for seasonal event chains: items are turned in for event points. */
    val event: String? = null,
) {
    val max get() = items.size
}

enum class Currency { COINS, RUBIES, ENERGY }

data class GeneratorDef(
    val id: String,
    val name: String,
    val art: String,
    /** chain id -> weight. */
    val outputs: List<Pair<String, Int>>,
    val charges: Int,
    val cooldownMinutes: Int,
    val unlockLevel: Int,
    val blurb: String,
    val event: String? = null,
)

data class Character(val id: String, val name: String, val art: String)

object Content {
    val chains: List<ChainDef> = listOf(
        ChainDef("bread", "Bread Basket", listOf(
            "Wheat" to "sheaf_of_rice", "Flatbread" to "flatbread", "Fresh Loaf" to "bread", "Bagel" to "bagel",
            "Pretzel" to "pretzel", "Baguette" to "baguette_bread", "Butter Croissant" to "croissant",
            "Deluxe Sandwich" to "sandwich"), 0xFFE0A050),
        ChainDef("cinnamon", "Cinnamon Rolls", listOf(
            "Cinnamon Sticks" to "cinnamon_sticks", "Dough Ball" to "dough_ball", "Cinnamon Swirl" to "cinnamon_swirl",
            "Cinnamon Roll" to "cinnamon_roll", "Iced Cinnamon Roll" to "iced_roll", "Plate of Rolls" to "roll_plate",
            "Tray of Rolls" to "roll_tray", "Golden Cinnamon Roll" to "golden_roll"), 0xFFC8782D),
        ChainDef("sweets", "Sweet Treats", listOf(
            "Candy" to "candy", "Lollipop" to "lollipop", "Chocolate Bar" to "chocolate_bar", "Cookie" to "cookie",
            "Sprinkle Donut" to "doughnut", "Cupcake" to "cupcake", "Strawberry Shortcake" to "shortcake",
            "Celebration Cake" to "birthday_cake"), 0xFFFF6FA8),
        ChainDef("breakfast", "Breakfast", listOf(
            "Egg" to "egg", "Milk" to "glass_of_milk", "Butter" to "butter", "Cheese" to "cheese_wedge",
            "Honey Pot" to "honey_pot", "Custard" to "custard", "Pancake Stack" to "pancakes",
            "Golden Waffle" to "waffle"), 0xFFFFC93C),
        ChainDef("fruit", "Fruit Stand", listOf(
            "Cherries" to "cherries", "Strawberry" to "strawberry", "Blueberries" to "blueberries", "Apple" to "red_apple",
            "Peach" to "peach", "Mango" to "mango", "Pineapple" to "pineapple", "Watermelon" to "watermelon"), 0xFFFF5A5A),
        ChainDef("drinks", "Café Drinks", listOf(
            "Coffee Beans" to "beans", "Hot Coffee" to "hot_beverage", "Green Tea" to "teacup_without_handle",
            "Spiced Cider" to "mate", "Iced Latte" to "cup_with_straw", "Bubble Tea" to "bubble_tea",
            "Affogato" to "ice_cream"), 0xFF8B5A3C),
        ChainDef("flowers", "Flower Shop", listOf(
            "Seedling" to "seedling", "Sprig" to "herb", "Tulip" to "tulip", "Rose" to "rose", "Hibiscus" to "hibiscus",
            "Sunflower" to "sunflower", "Cherry Blossom" to "cherry_blossom", "Grand Bouquet" to "bouquet"), 0xFFB070FF),
        ChainDef("toys", "Surf's Toys", listOf(
            "Bone" to "bone", "Tennis Ball" to "softball", "Frisbee" to "flying_disc", "Teddy Bear" to "teddy_bear",
            "Bandana Bow" to "ribbon", "Good Boy Crown" to "crown", "Best Boy Trophy" to "trophy"), 0xFF3FB8FF),

        // Collectibles: tap to collect.
        ChainDef("coins", "Coins", listOf("Coin" to "coin", "Coin Stack" to "coin", "Coin Pouch" to "money_bag",
            "Treasure Bag" to "money_bag"), 0xFFFFC21A, collect = Currency.COINS),
        ChainDef("rubies", "Rubies", listOf("Ruby" to "gem_stone", "Ruby Pair" to "gem_stone", "Ruby Cluster" to "gem_stone"),
            0xFFFF3D7F, collect = Currency.RUBIES),
        ChainDef("energy", "Energy", listOf("Spark" to "high_voltage", "Bolt" to "high_voltage", "Power Surge" to "high_voltage"),
            0xFF4CD964, collect = Currency.ENERGY),
        ChainDef("chest", "Gift Boxes", listOf("Gift Box" to "wrapped_gift", "Big Gift" to "wrapped_gift",
            "Mega Gift" to "wrapped_gift"), 0xFFFF7A45),

        // Seasonal event chains (see Events.kt).
        ChainDef("harvest", "Harvest Festival", listOf("Fallen Leaf" to "fallen_leaf", "Maple Leaf" to "maple_leaf",
            "Chestnut" to "chestnut", "Forest Mushroom" to "mushroom", "Pumpkin Pie" to "pie"), 0xFFE8742A, event = "harvest"),
        ChainDef("frosty", "Frosty Holidays", listOf("Snowflake" to "snowflake", "Candle" to "candle", "Snowman" to "snowman",
            "Holiday Tree" to "christmas_tree", "Present" to "wrapped_gift"), 0xFF5AB4FF, event = "frosty"),
        ChainDef("sweetheart", "Sweetheart Days", listOf("Heart" to "red_heart", "Love Letter" to "love_letter",
            "Rose" to "rose", "Heart Box" to "heart_with_ribbon", "Bouquet of Love" to "bouquet"), 0xFFFF4F8B, event = "sweetheart"),
        ChainDef("spring", "Spring Bloom", listOf("Clover" to "four_leaf_clover", "Tulip" to "tulip",
            "Cherry Blossom" to "cherry_blossom", "Dove" to "dove", "Rainbow" to "rainbow"), 0xFF6FD66F, event = "spring"),
        ChainDef("summer", "Surf's Summer", listOf("Wave" to "water_wave", "Sunshine" to "sun", "Shaved Ice" to "shaved_ice",
            "Beach Umbrella" to "umbrella_on_ground", "Surf Champion" to "person_surfing"), 0xFF22C3E6, event = "summer"),
        ChainDef("spooky", "Spooky Season", listOf("Mushroom" to "mushroom", "Candy" to "candy", "Crystal Ball" to "crystal_ball",
            "Magic Wand" to "magic_wand", "Moon Cake" to "moon_cake"), 0xFF9B59FF, event = "spooky"),
    )

    private val chainMap = chains.associateBy { it.id }
    fun chain(id: String): ChainDef? = chainMap[id]

    val generators: List<GeneratorDef> = listOf(
        GeneratorDef("mixing_bowl", "Mixing Bowl", "bowl_with_spoon", listOf("bread" to 60, "cinnamon" to 40), 30, 15, 1,
            "Bread and cinnamon dough. The heart of the bakery."),
        GeneratorDef("fruit_basket", "Fruit Basket", "basket", listOf("fruit" to 100), 24, 20, 1,
            "Fresh fruit from the market."),
        GeneratorDef("surf", "Surf", "surf_face", listOf("toys" to 85, "coins" to 15), 12, 30, 1,
            "Surf the golden retriever. Tap for toys (and the odd coin he found)."),
        GeneratorDef("hen_house", "Hen House", "hut", listOf("breakfast" to 100), 24, 20, 2,
            "Eggs, milk and breakfast goodies."),
        GeneratorDef("cookie_jar", "Cookie Jar", "jar", listOf("sweets" to 100), 24, 20, 3,
            "Sweets and treats."),
        GeneratorDef("teapot", "Café Teapot", "teapot", listOf("drinks" to 100), 24, 20, 5,
            "Coffee, tea and café drinks."),
        GeneratorDef("flower_pot", "Flower Pot", "potted_plant", listOf("flowers" to 100), 20, 25, 7,
            "Flowers to make the tables pretty."),
    )
    private val genMap = generators.associateBy { it.id }
    fun generator(id: String): GeneratorDef? = genMap[id] ?: Events.byId(id.removePrefix("event_"))?.generator

    val characters: List<Character> = listOf(
        Character("mandy", "Mandy", "mandy_face"),
        Character("surf", "Surf", "surf_face"),
        Character("june", "Grandma June", "old_woman"),
        Character("walt", "Grandpa Walt", "old_man"),
        Character("kai", "Kai the Surfer", "person_surfing"),
        Character("bea", "Bea the Gardener", "woman_farmer"),
        Character("dot", "Detective Dot", "detective"),
        Character("officer", "Officer Lou", "police_officer"),
        Character("ivy", "Ivy the Painter", "woman_artist"),
        Character("chef", "Chef Marco", "man_cook"),
        Character("lily", "Little Lily", "girl"),
        Character("max", "Max", "boy"),
        Character("ms_park", "Ms. Park", "woman_teacher"),
        Character("hank", "Hank the Builder", "construction_worker"),
        Character("rosa", "Rosa from the Office", "woman_office_worker"),
        Character("farmer", "Farmer Gus", "farmer"),
        Character("fire", "Captain Flo", "firefighter"),
        Character("penny", "Penny Pepper", "woman_in_tuxedo"),
    )
    private val charMap = characters.associateBy { it.id }
    fun character(id: String) = charMap[id] ?: characters[0]
    /** Characters who place orders (not Mandy or Surf). */
    val customers get() = characters.drop(2).filter { it.id != "penny" }

    // ---- item helpers ----

    fun parse(id: String): Pair<String, Int>? {
        val i = id.indexOf(':')
        if (i < 0) return null
        val lv = id.substring(i + 1).toIntOrNull() ?: return null
        return id.substring(0, i) to lv
    }

    fun isGenerator(id: String) = id.startsWith("gen:")
    fun genId(item: String) = item.removePrefix("gen:")

    fun art(id: String): String {
        if (isGenerator(id)) return generator(genId(id))?.art ?: "package"
        val (c, lv) = parse(id) ?: return "package"
        return chain(c)?.items?.getOrNull(lv - 1)?.second ?: "package"
    }

    fun name(id: String): String {
        if (isGenerator(id)) return generator(genId(id))?.name ?: "Generator"
        val (c, lv) = parse(id) ?: return "?"
        return chain(c)?.items?.getOrNull(lv - 1)?.first ?: "?"
    }

    fun next(id: String): String? {
        if (isGenerator(id)) return null
        val (c, lv) = parse(id) ?: return null
        val ch = chain(c) ?: return null
        return if (lv < ch.max) "$c:${lv + 1}" else null
    }

    fun level(id: String) = parse(id)?.second ?: 0
    fun chainOf(id: String) = parse(id)?.let { chain(it.first) }

    /** Coins an item is worth when it fills an order (sell price is a fifth of this). */
    fun value(id: String): Int {
        val lv = level(id)
        if (lv <= 0) return 0
        return 4 shl (lv - 1)  // 4, 8, 16, 32 ...
    }

    fun sellPrice(id: String): Int {
        if (isGenerator(id)) return 0
        val ch = chainOf(id) ?: return 0
        if (ch.collect != null || ch.id == "chest") return 0
        return maxOf(1, value(id) / 5)
    }

    /** How much a collectible gives when tapped. */
    fun collectAmount(id: String): Int {
        val ch = chainOf(id) ?: return 0
        val lv = level(id)
        return when (ch.collect) {
            Currency.COINS -> intArrayOf(5, 15, 40, 100)[lv - 1]
            Currency.RUBIES -> intArrayOf(1, 3, 8)[lv - 1]
            Currency.ENERGY -> intArrayOf(5, 15, 40)[lv - 1]
            null -> 0
        }
    }

    /** Items that come out of a gift box of the given level. */
    fun chestSize(level: Int) = intArrayOf(3, 5, 8)[(level - 1).coerceIn(0, 2)]
}
