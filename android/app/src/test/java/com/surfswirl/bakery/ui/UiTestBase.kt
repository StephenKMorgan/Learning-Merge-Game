package com.surfswirl.bakery.ui

import android.graphics.Bitmap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.surfswirl.bakery.BakeryApp
import com.surfswirl.bakery.MainActivity
import java.io.File
import java.time.LocalDate

object TestClock {
    val day: LocalDate = LocalDate.of(2026, 11, 22)
    fun install(seed: Long = 42) {
        GameViewModel.testToday = { day }
        GameViewModel.testSeed = seed
    }
}

typealias Rule = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>

private val shots = File(System.getProperty("user.dir"), "build/screenshots").apply { mkdirs() }

fun Rule.shot(name: String) {
    waitForIdle()
    val view = activity.window.decorView
    val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    runOnUiThread { view.draw(android.graphics.Canvas(bmp)) }
    File(shots, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
}

fun Rule.vm(): GameViewModel =
    ViewModelProvider(activity, GameViewModel.factory(activity.application as BakeryApp))[GameViewModel::class.java]

fun Rule.waitDesc(desc: String, timeout: Long = 8000) = try {
    waitUntil(timeout) { onAllNodesWithContentDescription(desc, substring = true).fetchSemanticsNodes().isNotEmpty() }
} catch (e: Throwable) { shot("fail-" + desc.filter { it.isLetterOrDigit() }); throw e }

fun Rule.waitText(text: String, timeout: Long = 8000) = try {
    waitUntil(timeout) { onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
} catch (e: Throwable) { shot("fail-" + text.filter { it.isLetterOrDigit() }); throw e }

fun Rule.tapDesc(desc: String): SemanticsNodeInteraction {
    waitDesc(desc)
    return onAllNodesWithContentDescription(desc).onFirst().also { it.performClick() }
}

fun Rule.exists(desc: String) = onAllNodesWithContentDescription(desc, substring = true).fetchSemanticsNodes().isNotEmpty()

/** Taps through a story dialogue until it closes. */
fun Rule.finishDialogue() {
    var guard = 0
    while (exists("Dialogue") && guard++ < 30) { onAllNodesWithContentDescription("Dialogue").onFirst().performClick(); waitForIdle() }
}

fun Rule.waitUntilTrue(timeout: Long = 8000, cond: () -> Boolean) = waitUntil(timeout) { cond() }
