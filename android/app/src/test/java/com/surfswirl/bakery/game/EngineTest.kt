package com.surfswirl.bakery.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

class EngineTest {
    private val day = LocalDate.of(2026, 11, 22)
    private val t0 = 1_800_000_000_000L
    private fun game(seed: Int = 1) = Engine.newGame(t0, day, Random(seed))
    private fun i(r: Int, c: Int) = Engine.idx(r, c)

    @Test fun contentIsConsistent() {
        // Every chain/generator item has art that the asset folder will provide, and ids are unique.
        assertEquals(Content.chains.size, Content.chains.map { it.id }.toSet().size)
        for (g in Content.generators) for ((c, _) in g.outputs) assertNotNull("chain $c", Content.chain(c))
        for (e in Events.events) { assertNotNull(Content.chain(e.chain)); assertEquals(e.id, Content.chain(e.chain)!!.event) }
        assertEquals("bread:2", Content.next("bread:1"))
        assertNull(Content.next("bread:8"))
        assertEquals("Golden Cinnamon Roll", Content.name("cinnamon:8"))
        assertTrue(Story.allTasks().map { it.id }.toSet().size == Story.allTasks().size)
    }

    @Test fun newGameLayout() {
        val s = game()
        assertEquals(BOARD_SIZE, s.board.size)
        assertEquals("gen:mixing_bowl", s.board[i(4, 1)].item)
        assertEquals("bread:1", s.board[i(3, 3)].item)
        assertTrue(s.board[i(0, 0)].boxed)
        assertEquals(3, s.orders.size)
        assertEquals(s.board.mapNotNull { if (it.item != null) it.uid else null }.size, s.board.mapNotNull { if (it.item != null) it.uid else null }.toSet().size)
    }

    @Test fun mergeOpensNeighbouringBoxes() {
        var s = game()
        // put a matching item next to a box
        val b = s.board.toMutableList(); b[i(2, 2)] = Cell("fruit:1", uid = 900); b[i(2, 3)] = Cell("fruit:1", uid = 901)
        s = s.copy(board = b)
        val o = Engine.move(s, i(2, 3), i(2, 2), Random(1))
        assertEquals("fruit:2", o.state.board[i(2, 2)].item)
        assertNull(o.state.board[i(2, 3)].item)
        assertFalse(o.state.board[i(1, 2)].boxed)   // box above opened
        assertTrue(o.fx.any { it is Fx.Unbox })
        assertTrue(o.fx.any { it is Fx.Merge })
    }

    @Test fun moveAndSwapAndBoxedBlocks() {
        val s = game()
        val moved = Engine.move(s, i(3, 3), i(3, 2), Random(1)).state
        assertEquals("bread:1", moved.board[i(3, 2)].item); assertNull(moved.board[i(3, 3)].item)
        val swapped = Engine.move(s, i(3, 3), i(4, 3), Random(1)).state
        assertEquals("fruit:1", swapped.board[i(3, 3)].item); assertEquals("bread:1", swapped.board[i(4, 3)].item)
        val blocked = Engine.move(s, i(3, 3), i(0, 3), Random(1))
        assertEquals(s.board, blocked.state.board)
    }

    @Test fun generatorUsesEnergyAndRecharges() {
        var s = game()
        val o = Engine.tap(s, i(4, 1), t0, day, Random(2))
        assertEquals(99, o.state.energy)
        val spawn = o.fx.filterIsInstance<Fx.Spawn>().single()
        val item = o.state.board[spawn.to].item!!
        assertTrue(item.startsWith("bread:") || item.startsWith("cinnamon:"))
        // drain charges
        s = o.state
        repeat(40) { s = Engine.move(s, 0, 0, Random(1)).state; s = Engine.tap(s, i(4, 1), t0, day, Random(it)).state.let { st ->
            // keep the board from filling: clear everything but generators
            st.copy(board = st.board.map { c -> if (c.item != null && !Content.isGenerator(c.item)) Cell() else c }) } }
        val g = s.generators["mixing_bowl"]!!
        assertEquals(0, g.charges)
        assertTrue(g.cooldownUntil > t0)
        assertTrue(Engine.tap(s, i(4, 1), t0, day, Random(1)).fx.contains(Fx.Error))
        val later = Engine.refresh(s, g.cooldownUntil + 1, day, Random(1)).state
        assertEquals(30, later.generators["mixing_bowl"]!!.charges)
    }

    @Test fun noEnergyAsksForHelp() {
        val s = game().copy(energy = 0)
        val o = Engine.tap(s, i(4, 1), t0, day, Random(1))
        assertTrue(o.fx.contains(Fx.Message("NO_ENERGY")))
    }

    @Test fun energyRegenerates() {
        val s = game().copy(energy = 10, energyAt = t0)
        val r = Engine.refresh(s, t0 + Economy.REGEN_MS * 5 + 1000, day, Random(1)).state
        assertEquals(15, r.energy)
        val full = Engine.refresh(s, t0 + Economy.REGEN_MS * 500, day, Random(1)).state
        assertEquals(Economy.ENERGY_MAX, full.energy)
        // energy above max (from rewards) is kept
        val over = Engine.refresh(s.copy(energy = 180), t0 + Economy.REGEN_MS * 5, day, Random(1)).state
        assertEquals(180, over.energy)
    }

    @Test fun serveOrderPaysAndLevelsUp() {
        var s = game()
        s = Engine.move(s, i(3, 3), i(5, 3), Random(1)).state  // two wheat -> flatbread
        assertEquals("bread:2", s.board[i(5, 3)].item)
        val june = s.orders.first { it.customer == "june" }
        assertNotNull(Engine.orderCells(s, june))
        val before = s.coins
        val o = Engine.serve(s.copy(xp = Economy.xpToNext(1) - 1), june.id, day, Random(1))
        assertEquals(before + june.coins, o.state.coins)
        assertNull(o.state.board[i(5, 3)].item)
        assertEquals(3, o.state.orders.size)   // refilled
        assertEquals(2, o.state.level)
        assertTrue(o.fx.any { it is Fx.LevelUp })
        // level 2 unlocks the Hen House as a gift
        assertTrue("gen:hen_house" in o.state.pending)
    }

    @Test fun cannotServeWithoutItems() {
        val s = game()
        val o = Engine.serve(s, s.orders.first().id, day, Random(1))
        assertEquals(s.coins, o.state.coins)
        assertTrue(o.fx.contains(Fx.Error))
    }

    @Test fun collectiblesAndChests() {
        val b = game().board.toMutableList()
        b[i(2, 2)] = Cell("coins:2", uid = 500); b[i(2, 4)] = Cell("chest:1", uid = 501)
        var s = game().copy(board = b)
        val c = Engine.tap(s, i(2, 2), t0, day, Random(1))
        assertEquals(s.coins + 15, c.state.coins)
        s = c.state
        val g = Engine.tap(s, i(2, 4), t0, day, Random(1))
        assertEquals(3, g.fx.filterIsInstance<Fx.Spawn>().size)
        assertNull(g.state.board[i(2, 4)].item)
    }

    @Test fun storageAndPending() {
        var s = game()
        s = Engine.store(s, i(3, 3)).state
        assertEquals("bread:1", s.storage[0]); assertNull(s.board[i(3, 3)].item)
        s = Engine.unstore(s, 0).state
        assertNull(s.storage[0])
        s = s.copy(pending = listOf("chest:1"))
        s = Engine.placePending(s).state
        assertTrue(s.pending.isEmpty()); assertTrue(s.board.any { it.item == "chest:1" })
        val rich = s.copy(rubies = 100)
        assertEquals(5, Engine.buyStorageSlot(rich).state.storage.size)
    }

    @Test fun renovationTasksInOrder() {
        var s = game().copy(coins = 10_000)
        assertTrue(Engine.completeTask(s, "c1_door").state.tasksDone.isEmpty())  // must do sweep first
        s = Engine.completeTask(s, "c1_sweep").state
        assertEquals(10_000 - 40, s.coins)
        for (t in Story.chapters[0].tasks.drop(1)) s = Engine.completeTask(s, t.id).state
        assertEquals(1, Story.chapterIndex(s.tasksDone))
        assertTrue(s.pending.contains("chest:2"))   // chapter reward
        val poor = game().copy(coins = 5)
        assertTrue(Engine.completeTask(poor, "c1_sweep").fx.contains(Fx.Error))
    }

    @Test fun dailyRewardsCycle() {
        var s = game()
        assertTrue(Engine.dailyAvailable(s, day))
        s = Engine.claimDaily(s, day).state
        assertEquals(120, s.energy)
        assertFalse(Engine.dailyAvailable(s, day))
        assertEquals(s, Engine.claimDaily(s, day).state)
        s = Engine.claimDaily(s, day.plusDays(1)).state
        assertEquals(60 + 80, s.coins)
    }

    @Test fun eventsStartAndEnd() {
        assertEquals("harvest", Events.active(LocalDate.of(2026, 11, 22))?.id)
        assertEquals("frosty", Events.active(LocalDate.of(2027, 1, 3))?.id)
        assertEquals("frosty", Events.active(LocalDate.of(2026, 12, 31))?.id)
        assertNull(Events.active(LocalDate.of(2026, 9, 1)))
        var s = Engine.refresh(game(), t0, day, Random(1)).state
        assertEquals("harvest", s.eventId)
        assertTrue("gen:event_harvest" in s.pending)
        s = Engine.placePending(s).state
        // an event item on the board turns into coins when the event changes
        val b = s.board.toMutableList(); b[i(2, 3)] = Cell("harvest:3", uid = 999); s = s.copy(board = b)
        val pts = Engine.turnInEvent(s, i(2, 3)).state.eventPts
        assertEquals(Events.points(3), pts)
        val dec = Engine.refresh(s, t0, LocalDate.of(2026, 12, 2), Random(1)).state
        assertEquals("frosty", dec.eventId)
        assertTrue(dec.board.none { it.item == "harvest:3" || it.item == "gen:event_harvest" })
        assertEquals(s.coins + Content.value("harvest:3"), dec.coins)
        assertTrue("gen:event_frosty" in dec.pending)
    }

    @Test fun vipAndPass() {
        var s = game().copy(passSeason = Engine.season(day), passPts = Economy.PASS_PTS_PER_TIER * 2 + 5)
        assertEquals(2, Engine.passTier(s))
        s = Engine.claimPass(s, 0, false, day).state
        assertTrue(0 in s.passClaimedFree)
        assertEquals(s, Engine.claimPass(s, 0, true, day).state)   // not VIP yet
        s = Engine.grant(s, Reward(vip = true), day)
        assertTrue(Engine.vip(s, day))
        assertEquals(Economy.ENERGY_MAX_VIP, Engine.maxEnergy(s, day))
        assertEquals(4, Engine.fillOrders(s, day, Random(1)).orders.size)
        s = Engine.claimPass(s, 1, true, day).state
        assertTrue(1 in s.passClaimedVip)
        // new month: VIP and pass reset
        val next = Engine.refresh(s, t0, day.plusMonths(1), Random(1)).state
        assertFalse(Engine.vip(next, day.plusMonths(1)))
        assertEquals(0, next.passPts)
    }

    @Test fun shopStockPerDay() {
        val o = Economy.offers.first { it.id == "ruby_jar" }
        var s = Engine.refresh(game(), t0, day, Random(1)).state
        assertEquals(1, Engine.stockLeft(s, o))
        s = Engine.buyOffer(s, o, 1.0, day).state
        assertEquals(0, Engine.stockLeft(s, o))
        s = Engine.refresh(s, t0, day.plusDays(1), Random(1)).state
        assertEquals(1, Engine.stockLeft(s, o))
        val lucky = Engine.buyOffer(s, o, 1.25, day).state
        assertEquals(s.rubies + 50, lucky.rubies)
    }

    @Test fun ordersOnlyAskForOwnedChains() {
        var s = game()
        repeat(50) { s = s.copy(orders = emptyList()); s = Engine.fillOrders(s, day, Random(it)) }
        val owned = Engine.ownedChains(s).toSet()
        assertTrue(s.orders.all { o -> o.wants.all { Content.parse(it)!!.first in owned } })
    }

    @Test fun chapterThreeFollowsChapterTwo() {
        var s = game().copy(coins = 100_000)
        for (t in Story.chapters[0].tasks + Story.chapters[1].tasks) s = Engine.completeTask(s, t.id).state
        assertEquals(2, Story.chapterIndex(s.tasksDone))
        assertEquals("c3_clear", Story.nextTask(s.tasksDone)?.id)
        for (t in Story.chapters[2].tasks) s = Engine.completeTask(s, t.id).state
        assertEquals(3, Story.chapterIndex(s.tasksDone))
        assertTrue("cinnamon:6" in s.pending)
        assertNull(Story.nextTask(s.tasksDone))
    }

    @Test fun eventMilestonesDecorateTheCottageAndItStays() {
        val dec = LocalDate.of(2026, 12, 10)
        var s = Engine.refresh(game(), t0, dec, Random(1)).state
        assertEquals("frosty", s.eventId)
        s = s.copy(eventPts = 100)
        s = Engine.claimEvent(s, 0).state
        s = Engine.claimEvent(s, 1).state
        assertEquals(setOf("frosty_1", "frosty_2"), s.eventDecor)
        assertTrue(Engine.claimEvent(s, 5).state.eventDecor.size == 2)   // not enough points yet
        // the event ends, but the cottage keeps its decorations
        val after = Engine.refresh(s, t0, LocalDate.of(2027, 1, 20), Random(1)).state
        assertEquals("", after.eventId)
        assertEquals(setOf("frosty_1", "frosty_2"), after.eventDecor)
        // every decoration has a layer in the cottage scene
        val ev = Events.byId("frosty")!!
        assertEquals(ev.milestones.size, ev.decor.size)
    }
}
