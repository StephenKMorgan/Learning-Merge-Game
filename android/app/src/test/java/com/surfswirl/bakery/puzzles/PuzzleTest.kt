package com.surfswirl.bakery.puzzles

import com.surfswirl.bakery.game.Engine
import com.surfswirl.bakery.game.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PuzzleTest {
    @Test fun wordBankHasEnoughWords() {
        for (len in 4..8) assertTrue("length $len has ${WordBank.words(len).size}", WordBank.words(len).size >= 40)
        assertTrue(WordBank.all.all { w -> w.all { it in 'A'..'Z' } })
    }

    @Test fun scrambleNeverShowsTheAnswer() {
        val r = Random(4)
        for (lv in 1..10) for (w in Generators.scramble(lv, r)) {
            assertEquals(Generators.scrambleLength(lv), w.length)
            val s = String(Generators.shuffleLetters(w, r).toCharArray())
            assertEquals(w.toList().sorted(), s.toList().sorted())
            assertNotEquals(w, s)
        }
    }

    @Test fun secretWordMarks() {
        val m = Generators.mark("SPEED", "ABIDE")
        assertEquals(listOf(Generators.Mark.MISS, Generators.Mark.MISS, Generators.Mark.NEAR, Generators.Mark.MISS, Generators.Mark.NEAR), m)
        assertTrue(Generators.mark("BREAD", "BREAD").all { it == Generators.Mark.HIT })
        // repeated letters: CREPT has one E, so only the first E in the guess is marked
        assertEquals(listOf(Generators.Mark.NEAR, Generators.Mark.MISS, Generators.Mark.NEAR, Generators.Mark.MISS, Generators.Mark.MISS),
            Generators.mark("EERIE", "CREPT"))
    }

    @Test fun mathAnswersAreRight() {
        val r = Random(9)
        for (lv in 1..10) repeat(200) {
            val q = Generators.mathQuestion(lv, r)
            assertEquals(q.text, eval(q.text), q.answer)
            assertTrue(q.answer >= 0)
        }
    }

    /** Tiny evaluator for the generated question formats. */
    private fun eval(t: String): Int {
        val e = t.replace("−", "-").replace("×", "*").replace("÷", "/").replace(" ", "")
        Regex("^(\\d+)%of(\\d+)$").find(e)?.let { return it.groupValues[2].toInt() * it.groupValues[1].toInt() / 100 }
        val sq = e.replace(Regex("(\\d+)²")) { val n = it.groupValues[1].toInt(); "(${n * n})" }
        return Expr(sq).parse()
    }

    private class Expr(val s: String) {
        var i = 0
        fun parse(): Int = sum()
        fun sum(): Int { var v = prod(); while (i < s.length && (s[i] == '+' || s[i] == '-')) { val op = s[i++]; val r = prod(); v = if (op == '+') v + r else v - r }; return v }
        fun prod(): Int { var v = atom(); while (i < s.length && (s[i] == '*' || s[i] == '/')) { val op = s[i++]; val r = atom(); if (op == '/') { require(v % r == 0); v /= r } else v *= r }; return v }
        fun atom(): Int { if (s[i] == '(') { i++; val v = sum(); i++; return v }; val st = i; while (i < s.length && s[i].isDigit()) i++; return s.substring(st, i).toInt() }
    }

    @Test fun patternsAreConsistent() {
        val r = Random(3)
        for (lv in 1..10) repeat(100) {
            val p = Generators.pattern(lv, r)
            assertTrue(p.shown.size >= 5)
            assertTrue(p.explain.isNotBlank())
        }
        val p = Generators.pattern(1, Random(1))
        val d = p.shown[1] - p.shown[0]
        assertEquals(p.shown.last() + d, p.answer)
    }

    @Test fun memoryHasPairs() {
        for (lv in 1..10) {
            val cards = Generators.memory(lv, Random(lv))
            assertEquals(Generators.memoryPairs(lv) * 2, cards.size)
            assertTrue(cards.groupingBy { it }.eachCount().values.all { it == 2 })
        }
    }

    @Test fun sudokuIsValidAndUnique() {
        for (lv in 1..10) {
            val s = Generators.sudoku(lv, Random(lv * 7))
            val n = s.n
            val sol = s.solution.toIntArray()
            for (i in sol.indices) assertTrue(Generators.canPlace(sol, n, s.boxRows, s.boxCols, i, sol[i]))
            for (i in s.givens.indices) if (s.givens[i] != 0) assertEquals(sol[i], s.givens[i])
            assertEquals(1, Generators.countSolutions(s.givens.toIntArray(), n, s.boxRows, s.boxCols, 2))
            assertTrue(s.givens.count { it != 0 } >= Generators.sudokuGivens(lv))
        }
    }

    @Test fun wordsStartEasierThanMath() {
        val s = GameState()
        // Same price: a word puzzle comes at a higher level than a math puzzle.
        assertTrue(Brain.level(s, PuzzleType.SCRAMBLE, 4.0) > Brain.level(s, PuzzleType.QUICK_MATH, 4.0))
        assertEquals(5, Brain.level(s, PuzzleType.SCRAMBLE, 4.0))
        assertEquals(3, Brain.level(s, PuzzleType.QUICK_MATH, 4.0))
    }

    @Test fun factorLearnsFromResults() {
        var s = GameState()
        val start = Brain.factor(s, PuzzleType.QUICK_MATH)
        // breezing through math lowers its difficulty score (so it costs higher levels)
        repeat(5) { s = Engine.recordPuzzle(s, PuzzleType.QUICK_MATH, 3, true, 0, 0, 10, 0L).first }
        assertTrue(Brain.factor(s, PuzzleType.QUICK_MATH) < start)
        // struggling with word scramble raises it
        var w = GameState()
        repeat(5) { w = Engine.recordPuzzle(w, PuzzleType.SCRAMBLE, 5, false, 4, 2, 300, 0L).first }
        assertTrue(Brain.factor(w, PuzzleType.SCRAMBLE) > PuzzleType.SCRAMBLE.prior)
        assertEquals(5, w.puzzleLog.size)
        assertTrue(Brain.factor(w, PuzzleType.SCRAMBLE) <= 2.5)
    }

    @Test fun hiddenSpeedBonus() {
        val s = GameState()
        val fast = Engine.recordPuzzle(s, PuzzleType.MEMORY, 3, true, 0, 0, 5, 0L)
        assertTrue(fast.second)
        val slow = Engine.recordPuzzle(s, PuzzleType.MEMORY, 3, true, 0, 0, 500, 0L)
        assertFalse(slow.second)
        val hinted = Engine.recordPuzzle(s, PuzzleType.MEMORY, 3, true, 0, 1, 5, 0L)
        assertFalse(hinted.second)
    }

    @Test fun expectedTimeUsesHerHistory() {
        var s = GameState()
        repeat(5) { s = Engine.recordPuzzle(s, PuzzleType.SUDOKU, 2, true, 0, 0, 30 + it, 0L).first }
        assertEquals(32.0, Brain.expectedSeconds(s.puzzleLog, PuzzleType.SUDOKU, 2), 0.01)
    }
}
