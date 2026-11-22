package com.surfswirl.bakery.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.surfswirl.bakery.MainActivity
import com.surfswirl.bakery.game.Economy
import com.surfswirl.bakery.puzzles.Generators
import com.surfswirl.bakery.puzzles.PuzzleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.random.Random

/** Solves one of every puzzle type through the real UI, and runs the VIP Brain Gauntlet. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class PuzzleFlowTest {
    companion object {
        @JvmStatic @BeforeClass fun clock() = TestClock.install(7)
    }

    @get:org.junit.Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun start(type: PuzzleType) {
        rule.vm().buy(Economy.energyRescue)
        rule.waitDesc(type.title)
        rule.tapDesc(type.title)
        rule.waitDesc("Quit puzzle")
    }

    private fun settle() { rule.mainClock.advanceTimeBy(2000); rule.waitForIdle() }

    private fun collect() {
        rule.waitText("Collect", 10000)
        rule.tapDesc("Collect")
    }

    private fun solveCurrent(type: PuzzleType, shotName: String) {
        val p = rule.vm().ui.value.puzzle!!
        val r = Random(p.seed)
        rule.shot(shotName)
        when (type) {
            PuzzleType.SCRAMBLE -> {
                val words = Generators.scramble(p.level, r)
                for (w in words) {
                    repeat(w.length - 1) { rule.tapDesc("Hint") }
                    // the one remaining letter
                    val nodes = rule.onAllNodesWithContentDescription("Tile ${w.last()}")
                    for (k in 0 until nodes.fetchSemanticsNodes().size) runCatching { nodes[k].performClick() }
                    settle()
                }
            }
            PuzzleType.SECRET_WORD -> {
                val answer = Generators.secret(p.level, r)
                for (c in "ZZZZZZZZ".take(answer.length)) rule.tapDesc("Key $c")
                rule.tapDesc("Key ⏎")
                rule.shot("$shotName-guess")
                for (c in answer) rule.tapDesc("Key $c")
                rule.tapDesc("Key ⏎")
            }
            PuzzleType.QUICK_MATH -> for (q in Generators.math(p.level, r)) {
                for (ch in q.answer.toString()) rule.tapDesc("Key $ch"); rule.tapDesc("Key ✓"); settle()
            }
            PuzzleType.NUMBER_PATTERN -> for (q in Generators.patterns(p.level, r)) {
                for (ch in q.answer.toString()) rule.tapDesc("Key $ch"); rule.tapDesc("Key ✓"); settle()
            }
            PuzzleType.MEMORY -> {
                val cards = Generators.memory(p.level, r)
                rule.tapDesc("Card 1"); rule.waitForIdle(); rule.shot("$shotName-flip")
                val j = (1 until cards.size).first { cards[it] == cards[0] }
                rule.tapDesc("Card ${j + 1}"); settle()
                val done = mutableSetOf(0, j)
                for (a in cards.indices) {
                    if (a in done) continue
                    val b = cards.indices.first { it != a && it !in done && cards[it] == cards[a] }
                    rule.tapDesc("Card ${a + 1}"); rule.tapDesc("Card ${b + 1}"); done += a; done += b; settle()
                }
            }
            PuzzleType.SUDOKU -> {
                val s = Generators.sudoku(p.level, r)
                val empties = s.givens.indices.filter { s.givens[it] == 0 }
                for ((k, i) in empties.withIndex()) {
                    rule.tapDesc("Cell ${i / s.n + 1}-${i % s.n + 1}")
                    if (k == 0) { rule.tapDesc("Number ${s.solution[i] % s.n + 1}"); rule.shot("$shotName-mistake") }
                    rule.tapDesc("Number ${s.solution[i]}")
                }
            }
        }
        settle()
    }

    @Test fun solveEveryPuzzleType() {
        rule.waitDesc("Dialogue"); rule.finishDialogue()
        var n = 0
        for (t in PuzzleType.entries) {
            start(t)
            solveCurrent(t, "p%d-%s".format(++n, t.name.lowercase()))
            collect()
            val last = rule.vm().state.value.puzzleLog.last()
            assertEquals(t.name, last.type)
            assertTrue("${t.name} solved", last.solved)
        }
        assertEquals(PuzzleType.entries.size, rule.vm().state.value.puzzleLog.size)
        rule.vm().go(Screen.STATS)
        rule.waitText("Hardest first")
        rule.shot("p9-stats-after")
    }

    @Test fun vipGauntletNeedsThreeCategories() {
        rule.waitDesc("Dialogue"); rule.finishDialogue()
        rule.vm().buy(Economy.vipOffer)
        rule.waitDesc("Quick Math")
        rule.shot("v1-gauntlet-pick")
        rule.tapDesc("Quick Math"); rule.waitDesc("Quit puzzle")
        solveCurrent(PuzzleType.QUICK_MATH, "v2-gauntlet-1")
        // Math is used up now; the next pick can't be math.
        rule.waitDesc("Word Scramble")
        assertTrue(!rule.exists("Number Patterns"))
        rule.shot("v3-gauntlet-pick-2")
        rule.tapDesc("Memory Match"); rule.waitDesc("Quit puzzle")
        solveCurrent(PuzzleType.MEMORY, "v4-gauntlet-2")
        rule.waitDesc("Mini Sudoku")
        rule.tapDesc("Mini Sudoku"); rule.waitDesc("Quit puzzle")
        solveCurrent(PuzzleType.SUDOKU, "v5-gauntlet-3")
        rule.waitText("VIP unlocked", 10000)
        rule.shot("v6-vip-unlocked")
        rule.tapDesc("Collect")
        assertTrue(com.surfswirl.bakery.game.Engine.vip(rule.vm().state.value, TestClock.day))
    }
}
