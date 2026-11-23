package com.surfswirl.bakery.game

import java.time.LocalDate
import java.time.temporal.IsoFields

/**
 * Repeatable goals between story releases.
 *  - Daily specials: 3 small goals a day (picked by date, the same all day), plus a bonus for finishing all 3.
 *    Progress is measured from the day's starting counters (GameState.dayStart), so nothing else needs to know.
 *  - Weekly mini-event: a themed week (Bread Week, Fruit Fiesta…). Every order served earns points, orders with
 *    the week's theme earn double, and a 5-step reward ladder resets each Monday.
 */
object Goals {
    data class Daily(val id: String, val title: String, val counter: String, val target: Int, val reward: Reward, val art: String)

    val pool = listOf(
        Daily("merge25", "Merge 25 items", "merges", 25, Reward(coins = 60), "sparkles"),
        Daily("merge50", "Merge 50 items", "merges", 50, Reward(rubies = 4), "sparkles"),
        Daily("serve6", "Serve 6 orders", "orders", 6, Reward(energy = 25), "bellhop_bell"),
        Daily("serve12", "Serve 12 orders", "orders", 12, Reward(items = listOf("chest:1")), "bellhop_bell"),
        Daily("puzzle1", "Solve a brain puzzle", "solved", 1, Reward(rubies = 3), "brain"),
        Daily("puzzle2", "Solve 2 brain puzzles", "solved", 2, Reward(energy = 40), "brain"),
        Daily("tap40", "Bake 40 times", "taps", 40, Reward(coins = 50), "bowl_with_spoon"),
        Daily("tap80", "Bake 80 times", "taps", 80, Reward(rubies = 4), "bowl_with_spoon"),
        Daily("clear3", "Clear 3 board obstacles", "cleared", 3, Reward(coins = 70), "package"),
    )
    val dailyBonus = Reward(rubies = 5, items = listOf("chest:1"))

    /** Today's three specials: one about merging/baking, one about serving, one about puzzles/obstacles. */
    fun today(d: LocalDate): List<Daily> {
        val n = d.toEpochDay().toInt()
        val a = listOf("merge25", "merge50", "tap40", "tap80")
        val b = listOf("serve6", "serve12")
        val c = listOf("puzzle1", "puzzle2", "clear3")
        return listOf(a[n % a.size], b[(n / 2) % b.size], c[(n / 3) % c.size]).map { id -> pool.first { it.id == id } }
    }

    fun progress(s: GameState, g: Daily) = ((s.counters[g.counter] ?: 0L) - (s.dayStart[g.counter] ?: 0L)).toInt().coerceIn(0, g.target)

    data class Week(val name: String, val chain: String, val art: String, val blurb: String)

    val weeks = listOf(
        Week("Bread Week", "bread", "baguette_bread", "Every loaf counts double! Fresh bread is all the rage on Maple Lane."),
        Week("Fruit Fiesta", "fruit", "watermelon", "Fruit orders count double. Bea says it's good for you."),
        Week("Sweet Tooth Week", "sweets", "cupcake", "Sweets orders count double. Max has a list."),
        Week("Breakfast Club", "breakfast", "pancakes", "Breakfast orders count double. Pancakes for everyone!"),
        Week("Surf's Toy Week", "toys", "teddy_bear", "Orders with Surf's toys count double. He approves."),
        Week("Cinnamon Week", "cinnamon", "iced_roll", "Cinnamon orders count double. The whole town smells amazing."),
        Week("Café Week", "drinks", "bubble_tea", "Drink orders count double. Coffee, tea, bubble tea!"),
        Week("Flower Week", "flowers", "bouquet", "Flower orders count double. Every table gets a bouquet."),
    )

    fun weekId(d: LocalDate) = "%d-W%02d".format(d.get(IsoFields.WEEK_BASED_YEAR), d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))
    fun week(d: LocalDate) = weeks[(d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) + d.get(IsoFields.WEEK_BASED_YEAR)) % weeks.size]

    val weekMilestones = listOf(
        6 to Reward(coins = 80), 14 to Reward(energy = 40), 24 to Reward(rubies = 8),
        36 to Reward(items = listOf("chest:2")), 50 to Reward(rubies = 15, coins = 200),
    )

    /** Points an order is worth this week. */
    fun orderPoints(o: Order, d: LocalDate): Int {
        val chain = week(d).chain
        return if (o.wants.any { Content.parse(it)?.first == chain }) 2 else 1
    }
}
