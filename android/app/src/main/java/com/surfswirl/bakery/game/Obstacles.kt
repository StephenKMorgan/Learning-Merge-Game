package com.surfswirl.bakery.game

import java.time.LocalDate
import kotlin.random.Random

/**
 * Board obstacles. Each new chapter lays a fresh set on empty squares (never over her items), themed to the
 * chapter's place, each hiding a reward:
 *  - web / sack / leaves / drift: merge next to it (hp times) to clear it
 *  - crate: drag the item it asks for onto it
 */
object Obstacles {
    /** Obstacle kinds per chapter (index), repeating the last one for later chapters. */
    private val byChapter = listOf(listOf("web"), listOf("leaves"), listOf("sack"), listOf("sack", "web"))
    fun hp(kind: String) = when (kind) { "web", "drift" -> 2; else -> 1 }

    fun name(kind: String) = when (kind) {
        "web" -> "Cobwebs"; "sack" -> "Flour sack"; "leaves" -> "Fallen leaves"; "drift" -> "Snow drift"; "crate" -> "Delivery crate"
        else -> "Obstacle"
    }

    fun hint(c: Cell): String = when (c.cover) {
        "crate" -> "This crate wants ${Content.name(c.need)}. Drag one onto it to open it!"
        "" -> ""
        else -> "${name(c.cover)}: merge next to it ${if (c.hp > 1) "${c.hp} more times" else "once"} to clear it. There's a surprise underneath!"
    }

    /** Lays this chapter's obstacles if it hasn't happened yet. Leaves at least 10 squares free. */
    fun lay(s: GameState, today: LocalDate, rnd: Random): GameState {
        val chapter = Story.chapterIndex(s.tasksDone)
        if (s.obstacleChapter >= chapter) return s
        val empty = s.board.indices.filter { s.board[it].item == null && !s.board[it].boxed }.shuffled(rnd)
        val count = minOf(8, empty.size - 10)
        if (count <= 0) return s
        val kinds = byChapter[chapter.coerceAtMost(byChapter.size - 1)] + if (Events.active(today)?.id == "frosty") listOf("drift") else emptyList()
        val chains = Engine.ownedChains(s).ifEmpty { listOf("bread") }
        val rewards = listOf("chest:1", "chest:1", "coins:2", "coins:3", "rubies:1", "rubies:2", "energy:2") +
            chains.flatMap { listOf("$it:2", "$it:3") }
        val board = s.board.toMutableList()
        var uid = s.nextUid
        for ((n, i) in empty.take(count).withIndex()) {
            val reward = rewards.random(rnd)
            board[i] = if (n < 2) {
                val ch = Content.chain(chains.random(rnd))!!
                Cell(reward, uid = uid++, cover = "crate", need = "${ch.id}:${rnd.nextInt(2, minOf(4, ch.max) + 1)}")
            } else {
                val k = kinds.random(rnd)
                Cell(reward, uid = uid++, cover = k, hp = hp(k))
            }
        }
        return s.copy(board = board, nextUid = uid, obstacleChapter = chapter)
    }
}
