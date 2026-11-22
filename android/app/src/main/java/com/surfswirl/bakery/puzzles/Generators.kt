package com.surfswirl.bakery.puzzles

import kotlin.random.Random

data class MathQuestion(val text: String, val answer: Int)
data class Pattern(val shown: List<Int>, val answer: Int, val explain: String)
data class Sudoku(val n: Int, val boxRows: Int, val boxCols: Int, val givens: List<Int>, val solution: List<Int>)

/** Puzzle generators. Level 1 is gentle, level 10 is a real workout. All are deterministic for a seed. */
object Generators {
    const val MATH_QUESTIONS = 5
    const val PATTERN_QUESTIONS = 3

    // ---------- Word Scramble ----------
    fun scrambleLength(level: Int) = intArrayOf(4, 4, 5, 5, 6, 6, 7, 7, 8, 8)[level.coerceIn(1, 10) - 1]
    fun scrambleCount(level: Int) = if (level >= 9) 3 else 2

    fun scramble(level: Int, rnd: Random): List<String> =
        WordBank.words(scrambleLength(level)).shuffled(rnd).take(scrambleCount(level))

    /** Letters in a shuffled order that never spells the word itself. */
    fun shuffleLetters(word: String, rnd: Random): List<Char> {
        var s = word.toList()
        var guard = 0
        while (guard++ < 20) {
            s = word.toList().shuffled(rnd)
            if (String(s.toCharArray()) !in WordBank.anagrams(word) + word) break
        }
        return s
    }

    // ---------- Secret Word ----------
    fun secretLength(level: Int) = intArrayOf(4, 4, 5, 5, 5, 5, 6, 6, 6, 6)[level.coerceIn(1, 10) - 1]
    fun secretTries(level: Int) = intArrayOf(8, 7, 7, 6, 6, 6, 7, 6, 6, 5)[level.coerceIn(1, 10) - 1]
    fun secret(level: Int, rnd: Random) = WordBank.words(secretLength(level)).random(rnd)

    enum class Mark { HIT, NEAR, MISS }

    /** Green / yellow / grey marks for a guess, counting repeated letters correctly. */
    fun mark(guess: String, answer: String): List<Mark> {
        val res = MutableList(guess.length) { Mark.MISS }
        val left = mutableMapOf<Char, Int>()
        for (i in guess.indices) {
            if (guess[i] == answer[i]) res[i] = Mark.HIT else left[answer[i]] = (left[answer[i]] ?: 0) + 1
        }
        for (i in guess.indices) {
            if (res[i] == Mark.HIT) continue
            val c = guess[i]
            val n = left[c] ?: 0
            if (n > 0) { res[i] = Mark.NEAR; left[c] = n - 1 }
        }
        return res
    }

    // ---------- Quick Math ----------
    fun math(level: Int, rnd: Random): List<MathQuestion> = List(MATH_QUESTIONS) { mathQuestion(level, rnd) }

    private fun r(rnd: Random, a: Int, b: Int) = rnd.nextInt(a, b + 1)

    fun mathQuestion(level: Int, rnd: Random): MathQuestion = when (level.coerceIn(1, 10)) {
        1 -> { val a = r(rnd, 1, 10); val b = r(rnd, 1, 10); MathQuestion("$a + $b", a + b) }
        2 -> if (rnd.nextBoolean()) { val a = r(rnd, 5, 25); val b = r(rnd, 2, 20); MathQuestion("$a + $b", a + b) }
             else { val a = r(rnd, 10, 30); val b = r(rnd, 1, a); MathQuestion("$a − $b", a - b) }
        3 -> if (rnd.nextBoolean()) { val a = r(rnd, 15, 70); val b = r(rnd, 11, 29); MathQuestion("$a + $b", a + b) }
             else { val a = r(rnd, 40, 99); val b = r(rnd, 11, a - 5); MathQuestion("$a − $b", a - b) }
        4 -> { val a = r(rnd, 2, 9); val b = r(rnd, 2, 10); MathQuestion("$a × $b", a * b) }
        5 -> when (rnd.nextInt(3)) {
            0 -> { val a = r(rnd, 3, 12); val b = r(rnd, 3, 12); MathQuestion("$a × $b", a * b) }
            1 -> { val a = r(rnd, 10, 40); val b = r(rnd, 10, 40); val c = r(rnd, 5, 30); MathQuestion("$a + $b + $c", a + b + c) }
            else -> { val a = r(rnd, 100, 200); val b = r(rnd, 20, 90); MathQuestion("$a − $b", a - b) }
        }
        6 -> if (rnd.nextBoolean()) { val b = r(rnd, 3, 12); val q = r(rnd, 3, 12); MathQuestion("${b * q} ÷ $b", q) }
             else { val a = r(rnd, 12, 49); val b = r(rnd, 3, 9); MathQuestion("$a × $b", a * b) }
        7 -> if (rnd.nextBoolean()) { val a = r(rnd, 2, 20); val b = r(rnd, 2, 9); val c = r(rnd, 2, 9); MathQuestion("$a + $b × $c", a + b * c) }
             else { val a = r(rnd, 2, 9); val b = r(rnd, 2, 9); val c = r(rnd, 2, 6); MathQuestion("($a + $b) × $c", (a + b) * c) }
        8 -> when (rnd.nextInt(3)) {
            0 -> { val p = listOf(10, 20, 25, 50, 75).random(rnd); val base = if (p == 25 || p == 75) 4 * r(rnd, 3, 30) else 10 * r(rnd, 2, 30); MathQuestion("$p% of $base", base * p / 100) }
            1 -> { val a = r(rnd, 11, 19); MathQuestion("$a²", a * a) }
            else -> { val b = r(rnd, 6, 15); val q = r(rnd, 6, 15); MathQuestion("${b * q} ÷ $b", q) }
        }
        9 -> if (rnd.nextBoolean()) { val a = r(rnd, 12, 35); val b = r(rnd, 11, 19); MathQuestion("$a × $b", a * b) }
             else { val a = r(rnd, 300, 999); val b = r(rnd, 120, a - 50); MathQuestion("$a − $b", a - b) }
        else -> when (rnd.nextInt(3)) {
            0 -> { val d = r(rnd, 2, 9); val q = r(rnd, 3, 15); val c = r(rnd, 2, 20); val ab = d * q + c
                   // find a × b = ab if possible, else fall back to a single product
                   val a = (2..12).firstOrNull { ab % it == 0 && ab / it in 2..30 }
                   if (a != null) MathQuestion("($a × ${ab / a} − $c) ÷ $d", q) else MathQuestion("($ab − $c) ÷ $d", q) }
            1 -> { val a = r(rnd, 3, 12); val b = r(rnd, 3, 12); MathQuestion("$a² + $b²", a * a + b * b) }
            else -> { val p = listOf(15, 35, 45, 60).random(rnd); val base = 20 * r(rnd, 2, 25); MathQuestion("$p% of $base", base * p / 100) }
        }
    }

    // ---------- Number Patterns ----------
    fun patterns(level: Int, rnd: Random): List<Pattern> = List(PATTERN_QUESTIONS) { pattern(level, rnd) }

    private fun seq(n: Int, f: (Int) -> Int) = List(n) { f(it) }

    fun pattern(level: Int, rnd: Random): Pattern {
        val lv = level.coerceIn(1, 10)
        val kind = when (lv) {
            1 -> 0; 2 -> listOf(0, 1).random(rnd); 3 -> listOf(1, 2).random(rnd); 4 -> listOf(2, 3).random(rnd)
            5 -> listOf(3, 4).random(rnd); 6 -> listOf(4, 5).random(rnd); 7 -> listOf(5, 6).random(rnd)
            8 -> listOf(6, 7).random(rnd); 9 -> listOf(7, 8).random(rnd); else -> listOf(8, 9, 10).random(rnd)
        }
        val all: List<Int>
        val why: String
        when (kind) {
            0 -> { val s = r(rnd, 1, 10); val k = r(rnd, 1, 5); all = seq(6) { s + k * it }; why = "Add $k each time." }
            1 -> if (rnd.nextBoolean()) { val s = r(rnd, 2, 20); val k = r(rnd, 3, 11); all = seq(6) { s + k * it }; why = "Add $k each time." }
                 else { val k = r(rnd, 2, 9); val s = k * 6 + r(rnd, 1, 20); all = seq(6) { s - k * it }; why = "Take away $k each time." }
            2 -> { val s = r(rnd, 1, 5); val m = if (lv >= 4 && rnd.nextBoolean()) 3 else 2; all = seq(6) { s * pow(m, it) }; why = "Multiply by $m each time." }
            3 -> { val s = r(rnd, 1, 10); val a = r(rnd, 1, 6); val b = r(rnd, 7, 12); all = seq(7) { s + (it / 2) * (a + b) + if (it % 2 == 1) a else 0 }; why = "Alternate: add $a, then add $b." }
            4 -> { val o = r(rnd, 1, 4); all = seq(6) { (it + o) * (it + o) }; why = "Square numbers: ${o}², ${o + 1}², ${o + 2}²…" }
            5 -> { val s = r(rnd, 1, 10); val d = r(rnd, 1, 3); val inc = r(rnd, 1, 3); var x = s; all = seq(6) { i -> if (i > 0) x += d + inc * (i - 1); x }; why = "The gap grows by $inc each time." }
            6 -> { val a = r(rnd, 1, 5); val b = r(rnd, 1, 6); val l = mutableListOf(a, b); while (l.size < 7) l += l[l.size - 1] + l[l.size - 2]; all = l; why = "Each number is the sum of the two before it." }
            7 -> { val s1 = r(rnd, 1, 9); val k1 = r(rnd, 2, 5); val s2 = r(rnd, 20, 40); val k2 = r(rnd, 1, 4)
                   all = seq(8) { if (it % 2 == 0) s1 + k1 * (it / 2) else s2 - k2 * (it / 2) }; why = "Two patterns take turns: one goes up by $k1, the other down by $k2." }
            8 -> { val s = r(rnd, 1, 4); var x = s; all = seq(6) { i -> if (i > 0) x = x * 2 + 1; x }; why = "Double it, then add 1." }
            9 -> { val o = r(rnd, 1, 4); all = seq(6) { val n = it + o; n * (n + 1) / 2 }; why = "Triangle numbers: add one more each time (+${o + 1}, +${o + 2}…)." }
            else -> { val s = r(rnd, 1, 5); val a = r(rnd, 2, 3); val b = r(rnd, 1, 5); var x = s; all = seq(7) { i -> if (i > 0) x = if (i % 2 == 1) x * a else x + b; x }; why = "Alternate: multiply by $a, then add $b." }
        }
        return Pattern(all.dropLast(1), all.last(), why)
    }

    private fun pow(b: Int, e: Int): Int { var r = 1; repeat(e) { r *= b }; return r }

    // ---------- Memory Match ----------
    fun memoryPairs(level: Int) = intArrayOf(3, 4, 5, 6, 6, 8, 8, 10, 10, 12)[level.coerceIn(1, 10) - 1]
    fun memoryColumns(level: Int) = if (memoryPairs(level) <= 4) 3 else 4

    val memoryArt = listOf("croissant", "cupcake", "doughnut", "cookie", "strawberry", "cherries", "peach", "tulip",
        "sunflower", "teddy_bear", "bone", "honey_pot", "pretzel", "bagel", "bubble_tea", "iced_roll", "dog_face",
        "watermelon", "rose", "hot_beverage", "lollipop", "pancakes")

    fun memory(level: Int, rnd: Random): List<String> {
        val pick = memoryArt.shuffled(rnd).take(memoryPairs(level))
        return (pick + pick).shuffled(rnd)
    }

    // ---------- Sudoku ----------
    fun sudokuSize(level: Int) = when (level.coerceIn(1, 10)) { in 1..3 -> 4; in 4..6 -> 6; else -> 9 }
    fun sudokuGivens(level: Int) = intArrayOf(10, 8, 6, 22, 18, 15, 42, 36, 32, 28)[level.coerceIn(1, 10) - 1]

    fun sudoku(level: Int, rnd: Random): Sudoku {
        val n = sudokuSize(level)
        val (br, bc) = when (n) { 4 -> 2 to 2; 6 -> 2 to 3; else -> 3 to 3 }
        val grid = IntArray(n * n)
        fill(grid, n, br, bc, 0, rnd)
        val solution = grid.toList()
        val target = sudokuGivens(level)
        var filled = n * n
        for (i in (0 until n * n).shuffled(rnd)) {
            if (filled <= target) break
            val keep = grid[i]
            grid[i] = 0
            if (countSolutions(grid.copyOf(), n, br, bc, 2) != 1) grid[i] = keep else filled--
        }
        return Sudoku(n, br, bc, grid.toList(), solution)
    }

    fun canPlace(g: IntArray, n: Int, br: Int, bc: Int, idx: Int, v: Int): Boolean {
        val row = idx / n; val col = idx % n
        for (k in 0 until n) {
            if (g[row * n + k] == v && row * n + k != idx) return false
            if (g[k * n + col] == v && k * n + col != idx) return false
        }
        val r0 = row / br * br; val c0 = col / bc * bc
        for (rr in r0 until r0 + br) for (cc in c0 until c0 + bc) {
            val j = rr * n + cc
            if (j != idx && g[j] == v) return false
        }
        return true
    }

    private fun fill(g: IntArray, n: Int, br: Int, bc: Int, idx: Int, rnd: Random): Boolean {
        if (idx == n * n) return true
        for (v in (1..n).shuffled(rnd)) {
            if (canPlace(g, n, br, bc, idx, v)) {
                g[idx] = v
                if (fill(g, n, br, bc, idx + 1, rnd)) return true
                g[idx] = 0
            }
        }
        return false
    }

    fun countSolutions(g: IntArray, n: Int, br: Int, bc: Int, limit: Int): Int {
        // pick the empty cell with the fewest options
        var best = -1; var bestCount = n + 1
        for (i in g.indices) if (g[i] == 0) {
            var c = 0
            for (v in 1..n) if (canPlace(g, n, br, bc, i, v)) c++
            if (c == 0) return 0
            if (c < bestCount) { bestCount = c; best = i }
        }
        if (best < 0) return 1
        var total = 0
        for (v in 1..n) if (canPlace(g, n, br, bc, best, v)) {
            g[best] = v
            total += countSolutions(g, n, br, bc, limit - total)
            g[best] = 0
            if (total >= limit) break
        }
        return total
    }
}
