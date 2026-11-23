package com.surfswirl.bakery.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.surfswirl.bakery.MainActivity
import com.surfswirl.bakery.data.SaveCodec
import com.surfswirl.bakery.game.Cell
import com.surfswirl.bakery.game.Engine
import com.surfswirl.bakery.game.GameState
import com.surfswirl.bakery.game.Order
import com.surfswirl.bakery.game.Story
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.random.Random

/**
 * Screenshots of a game in progress, for the download page (build/screenshots/store-*.png).
 * A brand-new game looks empty, so this sets up a bakery a few days in.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class StoreScreenshotsTest {
    companion object {
        @JvmStatic @BeforeClass fun clock() = TestClock.install(11)
    }

    @get:org.junit.Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun midGame(tasks: Int): GameState {
        val day = TestClock.day
        val s = Engine.newGame(System.currentTimeMillis(), day, Random(3))
        val board = s.board.toMutableList()
        var uid = 1000L
        fun put(r: Int, c: Int, item: String) { board[Engine.idx(r, c)] = Cell(item, uid = uid++) }
        // open up most of the boxes, keep a few along the edges
        for (i in board.indices) if (board[i].boxed && (i / 7 in 1..7 || i % 7 in 1..5)) board[i] = Cell()
        put(4, 1, "gen:mixing_bowl"); put(4, 5, "gen:fruit_basket"); put(6, 3, "gen:surf")
        put(2, 1, "gen:hen_house"); put(2, 5, "gen:cookie_jar"); put(7, 1, "gen:teapot")
        val items = listOf(
            3 to 2 to "cinnamon:5", 3 to 3 to "cinnamon:4", 3 to 4 to "bread:4", 5 to 2 to "fruit:3", 5 to 4 to "fruit:3",
            2 to 3 to "sweets:5", 1 to 2 to "breakfast:3", 1 to 4 to "breakfast:2", 4 to 2 to "bread:2", 4 to 4 to "drinks:3",
            5 to 3 to "toys:3", 6 to 2 to "coins:2", 6 to 4 to "sweets:3", 7 to 3 to "cinnamon:2", 7 to 5 to "fruit:5",
            5 to 1 to "drinks:2", 6 to 5 to "harvest:3", 3 to 5 to "harvest:2", 2 to 2 to "chest:2", 7 to 2 to "toys:1",
            1 to 3 to "rubies:1", 6 to 1 to "bread:3",
        )
        for ((rc, item) in items) put(rc.first, rc.second, item)
        val orders = listOf(
            Order(1, "june", listOf("cinnamon:5"), 70, 6, 3, "An iced cinnamon roll, please! It smells like cinnamon in here again…"),
            Order(2, "kai", listOf("fruit:3", "drinks:3"), 55, 7, 3, "Blueberries and a green tea, dude!"),
            Order(3, "lily", listOf("sweets:6"), 135, 7, 3, "Can I have a cupcake? Please please please?"),
            Order(4, "dot", listOf("bread:5"), 69, 6, 3, "A pretzel. For… investigative purposes."),
        )
        return s.copy(
            board = board, nextUid = uid, orders = orders, level = 6, xp = 40, coins = 1240, rubies = 86, energy = 87,
            tasksDone = Story.allTasks().take(tasks).map { it.id }.toSet(),
            seenStory = setOf("intro"), tutorial = 5, lastDaily = day.toString(), dailyClaims = 3,
            eventId = "harvest", eventPts = 64, passSeason = Engine.season(day), passPts = 95,
            discovered = board.mapNotNull { it.item }.toSet(),
            obstacleChapter = -1,   // lay this chapter's obstacles, so the screenshot shows them
        )
    }

    private fun load(s: GameState) {
        assertTrue(rule.vm().importSave(SaveCodec.encode(s)))
        rule.runOnUiThread { while (rule.vm().ui.value.dialogs.isNotEmpty()) rule.vm().closeDialog() }
        rule.mainClock.advanceTimeBy(4000)
        rule.waitForIdle()
    }

    @Test fun storeShots() {
        rule.waitDesc("Dialogue")
        load(midGame(9))
        rule.mainClock.advanceTimeBy(4000)
        rule.shot("store-1-board")
        rule.vm().go(Screen.GOALS)
        rule.waitText("Today's specials")
        rule.shot("store-6-goals")
        rule.vm().go(Screen.BOARD)
        rule.vm().openPlace("bakery")
        rule.waitText("Next task")
        rule.mainClock.advanceTimeBy(2000)
        rule.shot("store-2-bakery")
        rule.vm().go(Screen.BOARD)
        load(midGame(Story.chapters[0].tasks.size + 6))
        rule.vm().openPlace("patio")
        rule.waitText("Next task")
        rule.mainClock.advanceTimeBy(2000)
        rule.shot("store-3-patio")
        rule.vm().go(Screen.BAKERY)
        rule.waitDesc("Map of Maple Lane")
        rule.mainClock.advanceTimeBy(1500)
        rule.shot("store-5-map")
        rule.vm().go(Screen.SHOP)
        rule.waitText("Brain Shop")
        rule.mainClock.advanceTimeBy(2000)
        rule.shot("store-4-shop")
    }
}
