package com.surfswirl.bakery.game

import com.surfswirl.bakery.puzzles.Category

/**
 * Prices and rewards. In the shop nothing costs money: every offer costs "effort", paid by solving
 * brain puzzles. Bigger rewards ask for harder puzzles (see puzzles/Brain.kt for how effort turns
 * into a puzzle level that fits the player).
 */
data class Offer(
    val id: String,
    val title: String,
    val art: String,
    val reward: Reward,
    /** Effort per puzzle, roughly the puzzle level for an average player (1..10). */
    val effort: Double,
    val puzzles: Int = 1,
    val perDay: Int = 99,
    val badge: String = "",
)

object Economy {
    const val ENERGY_MAX = 100
    const val ENERGY_MAX_VIP = 150
    const val REGEN_MS = 120_000L
    const val REGEN_MS_VIP = 90_000L
    const val ORDER_SLOTS = 3
    const val PASS_TIERS = 30
    const val PASS_PTS_PER_TIER = 30

    fun xpToNext(level: Int) = 20 + 15 * (level - 1)
    fun levelUpReward(level: Int) = Reward(rubies = 4 + level, energy = 40)

    /** Ruby price to finish a generator's recharge now: 1 ruby per 3 minutes left. */
    fun skipCost(msLeft: Long) = maxOf(1, ((msLeft + 179_999) / 180_000).toInt())

    fun storageSlotCost(slots: Int) = 10 + (slots - 4) * 5
    const val MAX_STORAGE = 12

    val offers = listOf(
        Offer("energy_snack", "Energy Snack", "high_voltage", Reward(energy = 30), effort = 2.0, perDay = 6),
        Offer("coin_pouch", "Coin Pouch", "money_bag", Reward(coins = 120), effort = 3.0, perDay = 4),
        Offer("ruby_sprinkle", "Ruby Sprinkle", "gem_stone", Reward(rubies = 10), effort = 3.5, perDay = 3),
        Offer("energy_feast", "Energy Feast", "high_voltage", Reward(energy = 100), effort = 5.0, perDay = 2, badge = "Best value"),
        Offer("baker_chest", "Baker's Gift Box", "wrapped_gift", Reward(items = listOf("chest:2")), effort = 5.5, perDay = 2),
        Offer("ruby_jar", "Ruby Treasure", "crown", Reward(rubies = 40, coins = 100), effort = 7.5, perDay = 1, badge = "Big!"),
    )

    /** The VIP pass: a "brain gauntlet" of 3 puzzles from 3 different categories. */
    val vipOffer = Offer("vip", "Golden Whisk VIP", "ticket", Reward(vip = true, rubies = 20), effort = 6.0, puzzles = 3)

    /** Out of energy: a quick puzzle for a top-up. */
    val energyRescue = Offer("energy_rescue", "Energy Top-Up", "high_voltage", Reward(energy = 25), effort = 2.0, perDay = 99)

    /** Generator recharging: a puzzle finishes it now. */
    fun rechargeOffer(genName: String) = Offer("recharge", "Recharge $genName", "hourglass_done", Reward(), effort = 3.0, perDay = 99)

    fun offer(id: String) = (offers + vipOffer + energyRescue).firstOrNull { it.id == id }

    val freeTreat = Reward(energy = 10, coins = 20)

    val daily = listOf(
        Reward(energy = 20), Reward(coins = 80), Reward(rubies = 5), Reward(items = listOf("chest:1")),
        Reward(energy = 50), Reward(coins = 200), Reward(rubies = 15, items = listOf("chest:2")),
    )

    val vipPerks = listOf(
        "Max energy 150 (instead of 100)",
        "Energy refills faster (every 90 s)",
        "A 4th customer order slot",
        "The golden reward track on the pass",
        "+20 rubies right away",
    )

    fun passFree(tier: Int): Reward = when (tier % 5) {
        0 -> Reward(coins = 40 + tier * 5)
        1 -> Reward(energy = 15)
        2 -> Reward(rubies = 2)
        3 -> Reward(coins = 60 + tier * 5)
        else -> Reward(items = listOf("chest:1"))
    }

    fun passVip(tier: Int): Reward = when {
        tier == PASS_TIERS - 1 -> Reward(rubies = 50, items = listOf("cinnamon:7", "chest:3"))
        tier % 5 == 4 -> Reward(items = listOf("chest:2"), rubies = 5)
        tier % 3 == 0 -> Reward(energy = 40)
        tier % 3 == 1 -> Reward(rubies = 6)
        else -> Reward(coins = 150, items = listOf("energy:2"))
    }

    val categoriesForVip = listOf(Category.WORD, Category.MATH, Category.LOGIC, Category.MEMORY)
}
