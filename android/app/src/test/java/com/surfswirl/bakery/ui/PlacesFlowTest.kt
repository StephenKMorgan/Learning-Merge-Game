package com.surfswirl.bakery.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.surfswirl.bakery.MainActivity
import com.surfswirl.bakery.data.SaveCodec
import com.surfswirl.bakery.game.Engine
import com.surfswirl.bakery.game.Story
import com.surfswirl.bakery.ui.screens.Scenes
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import kotlin.random.Random

/**
 * Story places: Chapter 3's kitchen, visiting an earlier place, exploring full screen and tapping a
 * decoration to replay its story, and decorating Grandma June's cottage during Frosty Holidays.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class PlacesFlowTest {
    companion object {
        val day: LocalDate = LocalDate.of(2026, 12, 10)
        @JvmStatic @BeforeClass fun clock() {
            GameViewModel.testToday = { day }
            GameViewModel.testSeed = 5
        }
    }

    @get:org.junit.Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun tapScene(scene: String, layerId: String) {
        val def = Scenes.def(rule.activity, scene)!!
        val l = def.layers.first { it.id == layerId }
        // the explore view is drawn last, on top
        rule.onAllNodesWithContentDescription("Scene $scene").onLast().performTouchInput {
            click(Offset((l.x + l.w / 2f) / def.w * width, (l.y + l.h / 2f) / def.h * height))
        }
        rule.waitForIdle()
    }

    @Test fun visitPlacesAndDecorateTheCottage() {
        rule.waitDesc("Dialogue"); rule.finishDialogue()
        val base = Engine.newGame(System.currentTimeMillis(), day, Random(2))
        val done = (Story.chapters[0].tasks + Story.chapters[1].tasks + Story.chapters[2].tasks.take(6)).map { it.id }.toSet()
        val s = base.copy(tasksDone = done, coins = 2000, level = 9, seenStory = setOf("intro"), tutorial = 5,
            lastDaily = day.toString(), eventId = "frosty", eventPts = 120)
        assertTrue(rule.vm().importSave(SaveCodec.encode(s)))
        rule.runOnUiThread { while (rule.vm().ui.value.dialogs.isNotEmpty()) rule.vm().closeDialog() }
        rule.mainClock.advanceTimeBy(3000)

        // Chapter 3 opens on the kitchen
        rule.vm().go(Screen.BAKERY)
        rule.waitText("Cinnamon Famous")
        rule.shot("places-1-kitchen")

        // Visit the finished bakery, explore it full screen, and tap the bell
        rule.tapDesc("Place Bakery")
        rule.waitText("The Grand Reopening")
        rule.shot("places-2-bakery-visit")
        rule.tapDesc("Explore")
        rule.waitText("Pinch to zoom")
        rule.shot("places-3-explore")
        tapScene("inside", "bell")
        rule.waitText("ding")
        rule.shot("places-4-tapped-bell")
        rule.finishDialogue()
        rule.tapDesc("Done")

        // Frosty Holidays: claim milestones to decorate Grandma June's cottage
        rule.vm().go(Screen.EVENT)
        rule.waitText("Grandma June's Cottage")
        rule.shot("places-5-event-cottage")
        rule.tapDesc("Claim")
        rule.waitDesc("Dialogue")
        rule.shot("places-6-decoration-story")
        rule.finishDialogue()
        repeat(2) { rule.tapDesc("Claim"); rule.finishDialogue() }
        assertTrue("frosty_3" in rule.vm().state.value.eventDecor)
        rule.vm().go(Screen.BAKERY)
        rule.tapDesc("Place Cottage")
        rule.waitText("decorations")
        rule.mainClock.advanceTimeBy(2000)
        rule.shot("places-7-cottage")
    }
}
