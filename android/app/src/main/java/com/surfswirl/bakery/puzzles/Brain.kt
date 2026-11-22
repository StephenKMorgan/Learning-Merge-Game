package com.surfswirl.bakery.puzzles

import com.surfswirl.bakery.game.GameState
import com.surfswirl.bakery.game.PuzzleRecord
import kotlin.math.roundToInt

enum class Category(val label: String) { WORD("Words"), MATH("Numbers"), MEMORY("Memory"), LOGIC("Logic") }

/**
 * The puzzle types. [prior] is the starting guess at how hard this type feels to the player
 * (1.0 = average). Mandy is stronger at words than numbers, so words start easier (< 1) and math
 * starts harder (> 1). The app then learns the real value from how she actually does.
 */
enum class PuzzleType(val title: String, val category: Category, val art: String, val blurb: String, val prior: Double) {
    SCRAMBLE("Word Scramble", Category.WORD, "input_latin_letters", "Unscramble the letters into words", 0.8),
    SECRET_WORD("Secret Word", Category.WORD, "thought_balloon", "Guess the hidden word from color clues", 0.85),
    QUICK_MATH("Quick Math", Category.MATH, "abacus", "Answer five sums", 1.3),
    NUMBER_PATTERN("Number Patterns", Category.MATH, "input_numbers", "Find what comes next", 1.25),
    MEMORY("Memory Match", Category.MEMORY, "brain", "Flip cards and find the pairs", 1.0),
    SUDOKU("Mini Sudoku", Category.LOGIC, "puzzle_piece", "Fill the grid, no repeats", 1.1);

    companion object {
        fun of(name: String) = entries.firstOrNull { it.name == name }
    }
}

/**
 * Turns a reward's price ("effort") into a puzzle level for each type, and learns from results.
 *
 * effort ≈ level × factor. A type that is hard for the player has a high factor, so the same price
 * asks for a lower level of it. After every puzzle the factor moves toward what her performance
 * shows, so the prices keep matching how hard each type really is for her.
 */
object Brain {
    const val MIN_LEVEL = 1
    const val MAX_LEVEL = 10

    fun factor(s: GameState, t: PuzzleType) = s.brain[t.name] ?: t.prior

    fun levelFor(effort: Double, factor: Double) = (effort / factor).roundToInt().coerceIn(MIN_LEVEL, MAX_LEVEL)

    fun level(s: GameState, t: PuzzleType, effort: Double) = levelFor(effort, factor(s, t))

    /** A rough "typical" solve time, used until there is enough of the player's own history. */
    fun baselineSeconds(t: PuzzleType, level: Int): Double = when (t) {
        PuzzleType.SCRAMBLE -> Generators.scrambleCount(level) * (12.0 + 5 * Generators.scrambleLength(level))
        PuzzleType.SECRET_WORD -> 45.0 + 10 * level
        PuzzleType.QUICK_MATH -> Generators.MATH_QUESTIONS * (5.0 + 3 * level)
        PuzzleType.NUMBER_PATTERN -> Generators.PATTERN_QUESTIONS * (8.0 + 4 * level)
        PuzzleType.MEMORY -> 12.0 + Generators.memoryPairs(level) * 5.0
        PuzzleType.SUDOKU -> when (Generators.sudokuSize(level)) { 4 -> 45.0; 6 -> 150.0 + 20 * (level - 4); else -> 420.0 + 60 * (level - 7) }
    }

    /** What a solve of this type and level usually takes this player (median of her recent solves). */
    fun expectedSeconds(log: List<PuzzleRecord>, t: PuzzleType, level: Int): Double {
        val mine = log.asReversed().asSequence()
            .filter { it.type == t.name && it.solved && it.level == level }
            .take(15).map { it.seconds.toDouble() }.toList()
        if (mine.size < 3) return baselineSeconds(t, level)
        val sorted = mine.sorted()
        return sorted[sorted.size / 2]
    }

    /** 0 (struggled / gave up) .. 1 (fast and flawless). Around 0.65 is "about right". */
    fun performance(solved: Boolean, mistakes: Int, hints: Int, seconds: Int, expected: Double): Double {
        if (!solved) return 0.0
        val accuracy = (1.0 - 0.12 * mistakes - 0.15 * hints).coerceIn(0.0, 1.0)
        val ratio = seconds / expected.coerceAtLeast(1.0)
        val speed = (1.4 - 0.6 * ratio).coerceIn(0.0, 1.0)
        return 0.65 * accuracy + 0.35 * speed
    }

    /** The new factor after a result. Small steps, so one bad day doesn't swing prices around. */
    fun updatedFactor(old: Double, performance: Double): Double {
        val target = old * (1 + 0.6 * (0.65 - performance))
        return (old * 0.8 + target * 0.2).coerceIn(0.4, 2.5)
    }

    /**
     * The hidden quick-solve bonus. The player never sees a clock; a fast, clean solve just
     * sometimes comes with a "lucky bonus".
     */
    fun speedBonus(solved: Boolean, mistakes: Int, hints: Int, seconds: Int, expected: Double) =
        solved && mistakes == 0 && hints == 0 && seconds < expected * 0.7

    data class TypeStats(
        val type: PuzzleType, val played: Int, val solved: Int, val avgMistakes: Double,
        val medianSeconds: Double, val factor: Double, val avgLevel: Double, val bonuses: Int,
    )

    fun stats(s: GameState): List<TypeStats> = PuzzleType.entries.map { t ->
        val rs = s.puzzleLog.filter { it.type == t.name }
        val times = rs.filter { it.solved }.map { it.seconds.toDouble() }.sorted()
        TypeStats(
            t, rs.size, rs.count { it.solved },
            if (rs.isEmpty()) 0.0 else rs.sumOf { it.mistakes }.toDouble() / rs.size,
            if (times.isEmpty()) 0.0 else times[times.size / 2],
            factor(s, t),
            if (rs.isEmpty()) 0.0 else rs.sumOf { it.level }.toDouble() / rs.size,
            rs.count { it.bonus },
        )
    }.sortedByDescending { it.factor }
}
