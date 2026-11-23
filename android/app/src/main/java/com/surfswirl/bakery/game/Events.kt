package com.surfswirl.bakery.game

import java.time.LocalDate
import java.time.MonthDay

/**
 * Seasonal themes and events. To add an event: add a chain in Content.kt (with event = id), a Theme
 * here, and an EventDef with its dates. Events run on the phone's date, so they work offline and a
 * new APK can ship next season's events ahead of time.
 */
data class Theme(
    val id: String,
    val name: String,
    val bgTop: Long,
    val bgBottom: Long,
    val accent: Long,
    val tileA: Long,
    val tileB: Long,
    val boardFrame: Long,
    /** Art for the floating background particles. */
    val particles: List<String>,
    val icon: String,
)

data class EventDef(
    val id: String,
    val name: String,
    val start: MonthDay,
    /** Inclusive. May be earlier than start (wraps over New Year). */
    val end: MonthDay,
    val theme: String,
    val chain: String,
    val icon: String,
    val blurb: String,
    val milestones: List<Pair<Int, Reward>>,
    /** Optional event story: a place that gets one more decoration for each milestone claimed. */
    val scene: String? = null,
    val place: String = "",
    /** (title, story lines) per milestone, in order. Decoration ids are "<event>_<n>" (1-based). */
    val decor: List<Pair<String, List<Line>>> = emptyList(),
    val intro: List<Line> = emptyList(),
) {
    fun decorId(milestone: Int) = "${id}_${milestone + 1}"

    val generator get() = GeneratorDef("event_$id", "$name Basket", "shopping_bags", listOf(chain to 100), 20, 30, 1,
        "Tap for $name items. Merge them up and turn them in for event points!", event = id)

    fun activeOn(d: LocalDate): Boolean {
        val md = MonthDay.from(d)
        return if (!end.isBefore(start)) !md.isBefore(start) && !md.isAfter(end)
        else !md.isBefore(start) || !md.isAfter(end)
    }

    /** The last day of this run of the event, for the countdown. */
    fun endDate(d: LocalDate): LocalDate {
        var e = end.atYear(d.year)
        if (e.isBefore(d)) e = e.plusYears(1)
        return e
    }
}

object Events {
    val themes = listOf(
        Theme("classic", "Cinnamon Classic", 0xFFFFE6CC, 0xFFFFC6D3, 0xFFE8743B, 0xFFFFF4E6, 0xFFFFE9D2, 0xFFC97B4A,
            listOf("sparkles", "cinnamon_roll", "cookie"), "cinnamon_roll"),
        Theme("harvest", "Harvest Spice", 0xFFFFE0B2, 0xFFFFAB76, 0xFFD9601A, 0xFFFFF3E0, 0xFFFFE3C2, 0xFFA0522D,
            listOf("fallen_leaf", "maple_leaf", "chestnut"), "maple_leaf"),
        Theme("frosty", "Frosty Holidays", 0xFFE3F4FF, 0xFFB9DCFF, 0xFF2F80ED, 0xFFF4FBFF, 0xFFE2F1FF, 0xFF5B8FC7,
            listOf("snowflake", "snowflake", "sparkles"), "snowman"),
        Theme("sweetheart", "Sweetheart Pink", 0xFFFFE4EE, 0xFFFFB8D2, 0xFFE83E8C, 0xFFFFF1F6, 0xFFFFE0EC, 0xFFC9567F,
            listOf("red_heart", "sparkles", "love_letter"), "heart_with_ribbon"),
        Theme("spring", "Spring Bloom", 0xFFE8FBE5, 0xFFC9F0FF, 0xFF34A853, 0xFFF4FFF2, 0xFFE4F8E0, 0xFF5FA86A,
            listOf("cherry_blossom", "four_leaf_clover", "tulip"), "cherry_blossom"),
        Theme("summer", "Surf's Summer", 0xFFD9F7FF, 0xFFFFE9A8, 0xFF0098C7, 0xFFF0FCFF, 0xFFDDF5FC, 0xFF3A9CC0,
            listOf("water_wave", "sun", "sparkles"), "water_wave"),
        Theme("spooky", "Spooky Sweets", 0xFFEDE1FF, 0xFFFFC9A3, 0xFF8E44AD, 0xFFF8F2FF, 0xFFEDE3FA, 0xFF7D5BA6,
            listOf("crystal_ball", "candy", "sparkles"), "crystal_ball"),
    )

    private fun ms(m: Int, d: Int) = MonthDay.of(m, d)

    private fun ladder(chainItem: String) = listOf(
        20 to Reward(coins = 50), 50 to Reward(energy = 30), 90 to Reward(rubies = 5),
        140 to Reward(items = listOf("chest:1")), 200 to Reward(coins = 150), 280 to Reward(energy = 60),
        380 to Reward(rubies = 15), 500 to Reward(items = listOf("chest:2")), 650 to Reward(coins = 400, rubies = 10),
        850 to Reward(rubies = 30, items = listOf(chainItem, "chest:3")),
    )

    val events = listOf(
        EventDef("harvest", "Harvest Festival", ms(11, 1), ms(11, 30), "harvest", "harvest", "maple_leaf",
            "Crisp leaves, warm pies, and the whole bakery smells like cinnamon (as usual).", ladder("cinnamon:6")),
        EventDef("frosty", "Frosty Holidays", ms(12, 1), ms(1, 6), "frosty", "frosty", "snowman",
            "Snow on Maple Lane! Grandma June has lent you her cottage on the hill for the holidays.", ladder("sweets:7"),
            scene = "cottage", place = "Grandma June's Cottage",
            intro = listOf(
                Line("june", "Mandy, dear, my old cottage up the hill sits empty every winter. Why don't you and Surf make it cozy for the holidays?"),
                Line("mandy", "Oh June, really? We'll make it the coziest cottage on the whole hill!"),
                Line("surf", "*bounds through the snow and immediately gets stuck in a drift*"),
                Line("mandy", "Every reward milestone you reach this event decorates a little more of the cottage."),
            ),
            decor = listOf(
                "Light the fireplace" to listOf(Line("mandy", "There we go. Crackling fire, warm toes."), Line("surf", "*claims the spot closest to the fire*")),
                "Hang the stockings" to listOf(Line("lily", "There's one for Surf! Does Santa bring dogs presents?"), Line("mandy", "Only very good boys. So… probably.")),
                "Bring in a tree" to listOf(Line("farmer", "Freshest tree on the hill, just for you."), Line("mandy", "It smells like pine… and somehow also cinnamon.")),
                "Decorate the tree" to listOf(Line("ivy", "A little gold star on top, and it's perfect!"), Line("surf", "*very gently sniffs every single ornament*")),
                "String the lights" to listOf(Line("walt", "Up the ladder I go! June always loved lights along the beams."), Line("june", "Careful, Walter!")),
                "Hang a wreath" to listOf(Line("bea", "Holly, pine and red berries. Every cozy door needs a wreath.")),
                "Cocoa and cookies" to listOf(Line("mandy", "Hot cocoa with marshmallows, and cinnamon cookies, of course."), Line("max", "Can I have three? It's the holidays!")),
                "Presents under the tree" to listOf(Line("mandy", "One for June, one for Walter… and a squeaky toy for someone."), Line("surf", "*has already found it*")),
                "Build a snowman" to listOf(Line("kai", "Snow surfing is basically the same thing, right? Anyway, I made you a snowman."), Line("mandy", "He's perfect! Look Surf, a new friend.")),
                "Surf's Santa hat" to listOf(Line("mandy", "One last thing… a Santa hat for the best boy."), Line("surf", "WOOF! (The cottage is complete! Happy holidays!)")),
            ),
        ),
        EventDef("sweetheart", "Sweetheart Days", ms(2, 1), ms(2, 15), "sweetheart", "sweetheart", "heart_with_ribbon",
            "Love is in the air, and so is the smell of cinnamon rolls.", ladder("flowers:7")),
        EventDef("spring", "Spring Bloom", ms(3, 20), ms(4, 30), "spring", "spring", "cherry_blossom",
            "Fresh flowers for the patio. Surf keeps sniffing the tulips.", ladder("flowers:8")),
        EventDef("summer", "Surf's Summer", ms(6, 20), ms(8, 15), "summer", "summer", "water_wave",
            "Beach days! Kai is teaching Surf to ride a wave.", ladder("drinks:6")),
        EventDef("spooky", "Spooky Season", ms(10, 1), ms(10, 31), "spooky", "spooky", "crystal_ball",
            "Costumes, candy and a mysterious cinnamon roll thief.", ladder("cinnamon:7")),
    )

    fun byId(id: String) = events.firstOrNull { it.id == id }
    fun active(d: LocalDate) = events.firstOrNull { it.activeOn(d) }
    fun theme(id: String) = themes.firstOrNull { it.id == id } ?: themes[0]

    fun themeFor(settings: Settings, d: LocalDate): Theme =
        if (settings.theme == "auto") theme(active(d)?.theme ?: "classic") else theme(settings.theme)

    /** Event points for turning in an event item. */
    fun points(level: Int) = intArrayOf(1, 3, 8, 20, 50)[(level - 1).coerceIn(0, 4)]
}
