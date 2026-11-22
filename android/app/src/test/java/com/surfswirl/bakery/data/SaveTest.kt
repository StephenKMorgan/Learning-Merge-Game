package com.surfswirl.bakery.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.surfswirl.bakery.game.Engine
import com.surfswirl.bakery.game.GameState
import com.surfswirl.bakery.game.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate
import kotlin.random.Random

/** Updates must never lose progress: these check that saves survive schema changes and bad files. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SaveTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun game() = Engine.newGame(1_800_000_000_000L, LocalDate.of(2026, 11, 22), Random(5))
        .copy(coins = 1234, level = 7, tasksDone = setOf("c1_sweep"), brain = mapOf("QUICK_MATH" to 1.4))

    @Test fun roundTrip() {
        val s = game()
        assertEquals(s, SaveCodec.decode(SaveCodec.encode(s)))
        val store = SaveStore(tmp.root)
        store.save(s)
        assertEquals(s, SaveStore(tmp.root).load())
    }

    @Test fun oldSaveWithMissingFieldsLoads() {
        // A save from an early version: only a few fields, no settings, no event data.
        val old = """{"saveVersion":1,"coins":500,"level":3,"tasksDone":["c1_sweep"]}"""
        val s = SaveCodec.decode(old)
        assertEquals(500, s.coins)
        assertEquals(3, s.level)
        assertEquals(Settings(), s.settings)
        assertEquals(GameState().board.size, s.board.size)
    }

    @Test fun newerSaveWithUnknownFieldsLoads() {
        val text = SaveCodec.encode(game()).replaceFirst("{", """{"someFutureField":{"x":1},"anotherOne":[1,2],""")
        assertEquals(1234, SaveCodec.decode(text).coins)
    }

    @Test fun badValuesFallBackToDefaults() {
        val s = SaveCodec.decode("""{"coins":null,"level":4,"settings":{"theme":null}}""")
        assertEquals(GameState().coins, s.coins)
        assertEquals("auto", s.settings.theme)
    }

    @Test fun corruptMainFileFallsBackToPrevious() {
        val store = SaveStore(tmp.root)
        store.save(game())
        store.save(game().copy(coins = 99))   // previous good save is kept in save.prev.json
        File(tmp.root, "save.json").writeText("{ this is not json")
        val loaded = SaveStore(tmp.root).load()
        assertNotNull(loaded)
        assertEquals(1234, loaded!!.coins)
    }

    @Test fun noSaveYet() {
        assertNull(SaveStore(tmp.root).load())
    }
}
