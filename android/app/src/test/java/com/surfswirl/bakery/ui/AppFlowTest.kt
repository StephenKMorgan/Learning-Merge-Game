package com.surfswirl.bakery.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.surfswirl.bakery.MainActivity
import com.surfswirl.bakery.game.BOARD_COLS
import com.surfswirl.bakery.game.Engine
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

/**
 * Plays the real app like a person: the intro story, the first merge, serving the first order,
 * baking, fixing up the bakery, buying energy with a math puzzle, and visiting every screen.
 * Screenshots go to build/screenshots.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class AppFlowTest {
    companion object {
        @JvmStatic @BeforeClass fun clock() = TestClock.install()
    }

    @get:org.junit.Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun dragCell(from: Int, to: Int) {
        rule.onNodeWithContentDescription("Board").performTouchInput {
            val cell = width / BOARD_COLS.toFloat()
            fun c(i: Int) = Offset((i % BOARD_COLS + 0.5f) * cell, (i / BOARD_COLS + 0.5f) * cell)
            down(c(from))
            for (k in 1..10) moveTo(c(from) + (c(to) - c(from)) * (k / 10f))
            up()
        }
        rule.waitForIdle()
    }

    private fun tapCell(i: Int) {
        rule.onNodeWithContentDescription("Board").performTouchInput {
            val cell = width / BOARD_COLS.toFloat()
            click(Offset((i % BOARD_COLS + 0.5f) * cell, (i / BOARD_COLS + 0.5f) * cell))
        }
        rule.waitForIdle()
    }

    @Test fun playTheFirstFewMinutes() {
        rule.waitDesc("Dialogue")
        rule.shot("01-intro-story")
        rule.finishDialogue()
        rule.waitDesc("Board")
        rule.shot("02-board")

        // Tutorial: merge the two wheat
        dragCell(Engine.idx(3, 3), Engine.idx(5, 3))
        assertEquals("bread:2", rule.vm().state.value.board[Engine.idx(5, 3)].item)
        rule.shot("03-first-merge")

        // Serve Grandma June
        val coins = rule.vm().state.value.coins
        rule.tapDesc("Serve")
        rule.waitUntilTrue { rule.vm().state.value.coins > coins }
        rule.shot("04-served")

        // Bake with the mixing bowl
        val energy = rule.vm().state.value.energy
        tapCell(Engine.idx(4, 1)); tapCell(Engine.idx(4, 1)); tapCell(Engine.idx(4, 1))
        assertEquals(energy - 3, rule.vm().state.value.energy)
        rule.shot("05-baked")

        // Item info sheet for the cherries
        tapCell(Engine.idx(4, 3))
        rule.tapDesc("Item info")
        rule.waitDesc("Close")
        rule.shot("06-chain-info")
        rule.tapDesc("Close")

        // Fix up the bakery
        rule.tapDesc("Bakery")
        rule.waitText("Next task")
        rule.shot("07-bakery")
        rule.tapDesc("Do it!")
        rule.waitDesc("Dialogue")
        rule.shot("08-task-story")
        rule.finishDialogue()
        assertTrue("c1_sweep" in rule.vm().state.value.tasksDone)
        rule.shot("09-bakery-after")
        rule.tapDesc("Back")

        // Shop: buy an Energy Snack with a Quick Math puzzle
        rule.tapDesc("Shop")
        rule.waitText("Brain Shop")
        rule.shot("10-shop")
        rule.vm().buy(com.surfswirl.bakery.game.Economy.offers.first { it.id == "energy_snack" })
        rule.waitDesc("Quick Math")
        rule.shot("11-pick-puzzle")
        rule.tapDesc("Quick Math")
        rule.waitDesc("Key 1")
        rule.shot("12-quick-math")
        val p = rule.vm().ui.value.puzzle!!
        val qs = Generators.math(p.level, Random(p.seed))
        val before = rule.vm().state.value.energy
        for (q in qs) {
            for (ch in q.answer.toString()) rule.tapDesc("Key $ch")
            rule.tapDesc("Key ✓")
            rule.mainClock.advanceTimeBy(600)
            rule.waitForIdle()
        }
        rule.waitText("Collect", 10000)
        rule.shot("13-reward")
        rule.tapDesc("Collect")
        assertTrue(rule.vm().state.value.energy >= before + 30)
        assertEquals(1, rule.vm().state.value.puzzleLog.size)
        assertEquals(PuzzleType.QUICK_MATH.name, rule.vm().state.value.puzzleLog[0].type)

        // VIP offer
        rule.vm().pushDialog(Dialog.Vip)
        rule.waitText("Start the Gauntlet")
        rule.shot("14-vip")
        rule.tapDesc("Close")
        rule.tapDesc("Back")

        // Pass, event, daily, settings, stats
        rule.tapDesc("Pass")
        rule.waitText("Season")
        rule.shot("15-pass")
        rule.tapDesc("Back")
        rule.tapDesc("Event")
        rule.waitText("points")
        rule.shot("16-event")
        rule.tapDesc("Back")
        rule.tapDesc("Daily")
        rule.waitText("Day 1")
        rule.shot("17-daily")
        rule.tapDesc("Claim")
        rule.tapDesc("Settings")
        rule.waitText("Popups")
        rule.shot("18-settings")
        rule.tapDesc("Daily reward popup")
        assertTrue(!rule.vm().state.value.settings.dailyPopup)
        rule.vm().go(Screen.STATS)
        rule.waitText("Hardest first")
        rule.shot("19-stats")
        rule.vm().go(Screen.BOARD)
        rule.vm().pushDialog(Dialog.Storage)
        rule.waitText("Pantry Shelf")
        rule.shot("20-pantry")
    }
}
