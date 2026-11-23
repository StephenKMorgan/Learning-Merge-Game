package com.surfswirl.bakery.game

import kotlinx.serialization.Serializable

/**
 * The whole saved game. Stored as JSON (see data/SaveStore.kt).
 *
 * Save compatibility rules (so updates never lose progress):
 *  - Every field needs a default value, so an older save without it still loads.
 *  - Never rename or remove a field; add a new one instead (unknown fields are ignored on load).
 *  - For changes that need data rewritten, bump [CURRENT_SAVE_VERSION] and add a step in Migrations.kt.
 */
const val CURRENT_SAVE_VERSION = 1

const val BOARD_COLS = 7
const val BOARD_ROWS = 9
const val BOARD_SIZE = BOARD_COLS * BOARD_ROWS

@Serializable
data class Cell(
    val item: String? = null,
    /** A packing box covers the item; it opens when something next to it is merged. */
    val boxed: Boolean = false,
    /** Unique per item instance, so the UI can animate it. */
    val uid: Long = 0,
    /** Items left in a gift box. */
    val left: Int = 0,
    /** A chapter obstacle over the item: "web", "sack", "leaves", "drift" (clear by merging next to it) or "crate". */
    val cover: String = "",
    /** Merges next to it still needed to clear the cover. */
    val hp: Int = 0,
    /** For a crate: the item that opens it. */
    val need: String = "",
) {
    /** Can't be moved, merged or served: under a packing box or an obstacle. */
    val locked get() = boxed || cover.isNotEmpty()
}

@Serializable
data class GenState(val charges: Int = 0, val cooldownUntil: Long = 0)

@Serializable
data class Order(
    val id: Long,
    val customer: String,
    val wants: List<String>,
    val coins: Int,
    val xp: Int,
    val pts: Int,
    val line: String = "",
)

@Serializable
data class Reward(
    val coins: Int = 0,
    val rubies: Int = 0,
    val energy: Int = 0,
    val items: List<String> = emptyList(),
    /** Unlocks the VIP pass for the current season. */
    val vip: Boolean = false,
) {
    operator fun plus(o: Reward) = Reward(coins + o.coins, rubies + o.rubies, energy + o.energy, items + o.items, vip || o.vip)
    fun scaled(f: Double) = Reward((coins * f).toInt(), (rubies * f).toInt(), (energy * f).toInt(), items, vip)
    val isEmpty get() = coins == 0 && rubies == 0 && energy == 0 && items.isEmpty() && !vip
}

@Serializable
data class PuzzleRecord(
    val type: String,
    val level: Int,
    val solved: Boolean,
    val mistakes: Int,
    val hints: Int,
    val seconds: Int,
    val at: Long,
    /** The hidden "fast solve" bonus was given. */
    val bonus: Boolean = false,
    /** The difficulty factor for this type just before this puzzle. */
    val factor: Double = 1.0,
)

@Serializable
data class Settings(
    val sound: Boolean = true,
    val haptics: Boolean = true,
    /** Show the daily reward popup on launch (otherwise just a dot on the calendar button). */
    val dailyPopup: Boolean = true,
    /** Gentle prompts: out-of-energy offers, event reminders. */
    val nudges: Boolean = true,
    /** "Energy is full" notification. */
    val notifications: Boolean = true,
    /** "auto" follows the current seasonal event, or a theme id. */
    val theme: String = "auto",
    val music: Boolean = true,
    /** The baker's look (hair, skin, outfit, glasses). */
    val avatar: Avatar = Avatar(),
)

@Serializable
data class GameState(
    val saveVersion: Int = CURRENT_SAVE_VERSION,
    val createdAt: Long = 0,
    val board: List<Cell> = List(BOARD_SIZE) { Cell() },
    val storage: List<String?> = List(4) { null },
    /** Reward items waiting to be placed on the board (the gift bubble). */
    val pending: List<String> = emptyList(),
    val coins: Int = 60,
    val rubies: Int = 15,
    val energy: Int = 100,
    val energyAt: Long = 0,
    val level: Int = 1,
    val xp: Int = 0,
    val generators: Map<String, GenState> = emptyMap(),
    val orders: List<Order> = emptyList(),
    val nextUid: Long = 1,
    val discovered: Set<String> = emptySet(),
    val tasksDone: Set<String> = emptySet(),
    val seenStory: Set<String> = emptySet(),
    val tutorial: Int = 0,
    val dailyClaims: Int = 0,
    val lastDaily: String = "",
    val shopDay: String = "",
    val shopBought: Map<String, Int> = emptyMap(),
    val freeTreatDay: String = "",
    /** Season id ("2026-11") the VIP pass is active for. */
    val vipSeason: String = "",
    val passSeason: String = "",
    val passPts: Int = 0,
    val passClaimedFree: Set<Int> = emptySet(),
    val passClaimedVip: Set<Int> = emptySet(),
    val eventId: String = "",
    val eventPts: Int = 0,
    val eventClaimed: Set<Int> = emptySet(),
    /** Event-story decorations ever unlocked ("frosty_3"), kept after the event ends so the place can be visited. */
    val eventDecor: Set<String> = emptySet(),
    /** Puzzle type -> how hard that type is for the player (1.0 = average). Learned over time. */
    val brain: Map<String, Double> = emptyMap(),
    val puzzleLog: List<PuzzleRecord> = emptyList(),
    val counters: Map<String, Long> = emptyMap(),
    /** Daily specials: the day they're for, the counters at the start of that day, and which were claimed. */
    val goalsDay: String = "",
    val dayStart: Map<String, Long> = emptyMap(),
    val dailyGoalsClaimed: Set<String> = emptySet(),
    /** Weekly mini-event: ISO week id, points earned this week, milestones claimed. */
    val weekId: String = "",
    val weekPts: Int = 0,
    val weekClaimed: Set<Int> = emptySet(),
    /** Board obstacles: the chapter whose obstacles were last laid out (-1 = none yet). */
    val obstacleChapter: Int = -1,
    val settings: Settings = Settings(),
    val lastSeen: Long = 0,
) {
    fun count(key: String) = counters[key] ?: 0L
}
