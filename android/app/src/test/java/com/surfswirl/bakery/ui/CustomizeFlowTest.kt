package com.surfswirl.bakery.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.surfswirl.bakery.MainActivity
import com.surfswirl.bakery.game.Looks
import com.surfswirl.bakery.game.Story
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Customizes the baker in Settings and checks the new look is saved and used in the scenes and dialogue. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class CustomizeFlowTest {
    companion object {
        @JvmStatic @BeforeClass fun clock() = TestClock.install(21)
    }

    @get:org.junit.Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun customizeTheBaker() {
        rule.waitDesc("Dialogue"); rule.finishDialogue()
        rule.tapDesc("Settings")
        rule.waitText("Your baker")
        rule.tapDesc("Customize")
        rule.waitText("Hair style")
        rule.shot("look-1-original")
        rule.tapDesc("Hair Long")
        rule.tapDesc("Hair color 6")
        rule.tapDesc("Skin tone 4")
        rule.tapDesc("Apron 2")
        rule.tapDesc("Glasses: off")
        rule.onAllNodesWithContentDescription("Baker name").onFirst().performTextReplacement("Kay")
        rule.shot("look-2-customized")
        val a = rule.vm().state.value.settings.avatar
        assertEquals("long", a.hairStyle)
        assertEquals(Looks.hairColors[5], a.hair)
        assertEquals(Looks.skinTones[3], a.skin)
        assertEquals(Looks.outfits[1], a.outfit)
        assertTrue(a.glasses)
        assertEquals("Kay", a.name)
        rule.tapDesc("Done")

        // The new look in a scene and in story dialogue
        rule.vm().go(Screen.BAKERY)
        rule.waitText("Next task")
        rule.mainClock.advanceTimeBy(1500)
        rule.shot("look-3-scene")
        rule.vm().pushDialog(Dialog.Talk(listOf(com.surfswirl.bakery.game.Line("june", "Welcome, Mandy! Mandy's bakery is the talk of the town."),
            com.surfswirl.bakery.game.Line("mandy", "Thank you, June!"))))
        rule.waitDesc("Dialogue")
        rule.waitText("Welcome, Kay! Kay's bakery")
        rule.shot("look-4-dialogue")
        rule.onAllNodesWithContentDescription("Dialogue").onFirst().performClick()
        rule.waitText("Kay")
    }
}
