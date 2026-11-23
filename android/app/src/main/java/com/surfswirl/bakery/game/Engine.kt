package com.surfswirl.bakery.game

import com.surfswirl.bakery.puzzles.Brain
import com.surfswirl.bakery.puzzles.PuzzleType
import java.time.LocalDate
import kotlin.math.abs
import kotlin.random.Random

/** Things the UI should animate or play after an action. */
sealed interface Fx {
    data class Spawn(val from: Int, val to: Int, val uid: Long) : Fx
    data class Merge(val cell: Int, val item: String) : Fx
    data class Collect(val cell: Int, val currency: Currency, val amount: Int) : Fx
    data class Unbox(val cell: Int) : Fx
    data class LevelUp(val level: Int, val reward: Reward) : Fx
    data class Discover(val item: String) : Fx
    data class OrderDone(val orderId: Long, val coins: Int) : Fx
    data class Message(val text: String) : Fx
    data class Granted(val reward: Reward) : Fx
    data object Error : Fx
    data object Sold : Fx
    data object Pop : Fx
}

data class Outcome(val state: GameState, val fx: List<Fx> = emptyList())

/** Pure game rules: every function takes a state and returns a new one. No Android here. */
object Engine {
    fun idx(row: Int, col: Int) = row * BOARD_COLS + col
    fun season(d: LocalDate) = "%04d-%02d".format(d.year, d.monthValue)
    fun vip(s: GameState, d: LocalDate) = s.vipSeason == season(d)
    fun maxEnergy(s: GameState, d: LocalDate) = if (vip(s, d)) Economy.ENERGY_MAX_VIP else Economy.ENERGY_MAX
    fun regenMs(s: GameState, d: LocalDate) = if (vip(s, d)) Economy.REGEN_MS_VIP else Economy.REGEN_MS
    fun orderSlots(s: GameState, d: LocalDate) = Economy.ORDER_SLOTS + if (vip(s, d)) 1 else 0

    // ---------------------------------------------------------------- new game

    fun newGame(now: Long, today: LocalDate, rnd: Random): GameState {
        val board = MutableList(BOARD_SIZE) { Cell() }
        var uid = 1L
        fun put(r: Int, c: Int, item: String, boxed: Boolean = false) { board[idx(r, c)] = Cell(item, boxed, uid++) }
        put(4, 1, "gen:mixing_bowl")
        put(4, 5, "gen:fruit_basket")
        put(6, 3, "gen:surf")
        put(3, 3, "bread:1"); put(5, 3, "bread:1")
        put(4, 3, "fruit:1")
        // Packing boxes along the top and bottom, hiding goodies.
        val boxPool = listOf("bread:1", "bread:2", "fruit:1", "fruit:2", "cinnamon:1", "toys:1", "coins:1", "bread:3",
            "fruit:3", "cinnamon:2", "energy:1", "rubies:1", "breakfast:1", "sweets:1", "chest:1")
        for (r in listOf(0, 1, 8)) for (c in 0 until BOARD_COLS) put(r, c, boxPool.random(rnd), boxed = true)
        for (c in listOf(0, 6)) for (r in 2..7) put(r, c, boxPool.random(rnd), boxed = true)
        val orders = listOf(
            Order(1, "june", listOf("bread:2"), 13, 3, 1, "A warm flatbread, dear? Just merge two wheat together!"),
            Order(2, "walt", listOf("fruit:2"), 13, 3, 1, "Could I get a strawberry? June says I need more fruit."),
            Order(3, "lily", listOf("cinnamon:2"), 13, 3, 1, "Is that cinnamon I smell? Can I have some dough?"),
        )
        return GameState(
            createdAt = now, board = board, nextUid = uid, energyAt = now, orders = orders, lastSeen = now,
            generators = Content.generators.associate { it.id to GenState(it.charges) },
            discovered = board.mapNotNull { if (it.boxed) null else it.item }.toSet(),
            passSeason = season(today),
            obstacleChapter = 0,   // the packing boxes are chapter 1's obstacles
        )
    }

    // ---------------------------------------------------------------- time passing

    /** Applies everything that happens with time: energy, recharges, new day, new season, events. */
    fun refresh(s0: GameState, now: Long, today: LocalDate, rnd: Random): Outcome {
        var s = s0
        val fx = mutableListOf<Fx>()
        // Energy regen
        val max = maxEnergy(s, today)
        val regen = regenMs(s, today)
        s = if (s.energy >= max) s.copy(energyAt = now) else {
            val gained = ((now - s.energyAt) / regen).toInt().coerceAtLeast(0)
            val e = minOf(max, s.energy + gained)
            s.copy(energy = e, energyAt = if (e >= max) now else s.energyAt + gained * regen)
        }
        // Generator recharges
        val gens = s.generators.toMutableMap()
        for ((id, g) in s.generators) {
            if (g.charges <= 0 && g.cooldownUntil in 1..now) {
                gens[id] = GenState(Content.generator(id)?.charges ?: 20, 0)
            }
        }
        s = s.copy(generators = gens)
        // Shop stock resets each day
        val day = today.toString()
        if (s.shopDay != day) s = s.copy(shopDay = day, shopBought = emptyMap())
        // Daily specials and the weekly mini-event roll over
        if (s.goalsDay != day) s = s.copy(goalsDay = day, dayStart = s.counters, dailyGoalsClaimed = emptySet())
        val week = Goals.weekId(today)
        if (s.weekId != week) s = s.copy(weekId = week, weekPts = 0, weekClaimed = emptySet())
        // Pass season
        val season = season(today)
        if (s.passSeason != season) s = s.copy(passSeason = season, passPts = 0, passClaimedFree = emptySet(), passClaimedVip = emptySet())
        // Seasonal event
        val ev = Events.active(today)
        val evId = ev?.id ?: ""
        if (s.eventId != evId) {
            val o = switchEvent(s, ev, rnd)
            s = o.state; fx += o.fx
        }
        // Generators unlocked by level that aren't on the board yet (e.g. after an update adds one)
        s = giveMissingGenerators(s)
        s = Obstacles.lay(s, today, rnd)
        s = fillOrders(s, today, rnd)
        return Outcome(s.copy(lastSeen = now), fx)
    }

    private fun switchEvent(s0: GameState, ev: EventDef?, rnd: Random): Outcome {
        var s = s0
        val fx = mutableListOf<Fx>()
        // Old event items turn into coins.
        var coins = 0
        val board = s.board.map { c ->
            val item = c.item ?: return@map c
            val ch = Content.chainOf(item)
            val isOldGen = Content.isGenerator(item) && Content.genId(item).startsWith("event_")
            if ((ch?.event != null) || isOldGen) {
                coins += if (isOldGen) 0 else Content.value(item)
                c.copy(item = null, boxed = false)
            } else c
        }
        val storage = s.storage.map { if (it != null && Content.chainOf(it)?.event != null) { coins += Content.value(it); null } else it }
        val pending = s.pending.filterNot { Content.chainOf(it)?.event != null || it.startsWith("gen:event_") }
        s = s.copy(board = board, storage = storage, pending = pending, coins = s.coins + coins,
            eventId = ev?.id ?: "", eventPts = 0, eventClaimed = emptySet())
        if (coins > 0) fx += Fx.Message("Last event's items were traded for $coins coins.")
        if (ev != null) {
            val gid = "event_${ev.id}"
            s = s.copy(pending = listOf("gen:$gid") + s.pending,
                generators = s.generators + (gid to GenState(ev.generator.charges)))
            fx += Fx.Message("${ev.name} has started! Your event basket is in the gift bubble.")
        }
        return Outcome(s, fx)
    }

    private fun giveMissingGenerators(s: GameState): GameState {
        val have = (s.board.mapNotNull { it.item } + s.storage.filterNotNull() + s.pending).filter { Content.isGenerator(it) }.toSet()
        val add = Content.generators.filter { it.unlockLevel <= s.level && "gen:${it.id}" !in have }.map { "gen:${it.id}" }
        if (add.isEmpty()) return s
        return s.copy(pending = s.pending + add,
            generators = s.generators + add.map { Content.genId(it) }.filter { it !in s.generators }.associateWith { GenState(Content.generator(it)!!.charges) })
    }

    // ---------------------------------------------------------------- board actions

    fun nearestEmpty(board: List<Cell>, from: Int): Int? {
        val fr = from / BOARD_COLS; val fc = from % BOARD_COLS
        return board.indices.filter { board[it].item == null && !board[it].locked }
            .minWithOrNull(compareBy<Int>({ val r = it / BOARD_COLS; val c = it % BOARD_COLS; maxOf(abs(r - fr), abs(c - fc)) },
                { val r = it / BOARD_COLS; val c = it % BOARD_COLS; abs(r - fr) + abs(c - fc) }, { it }))
    }

    /** Tapping a cell: generators make items, collectibles are collected, gift boxes open. */
    fun tap(s: GameState, i: Int, now: Long, today: LocalDate, rnd: Random): Outcome {
        val cell = s.board[i]
        val item = cell.item ?: return Outcome(s)
        if (cell.boxed) return Outcome(s, listOf(Fx.Message("Merge something next to this box to open it.")))
        if (cell.cover.isNotEmpty()) return Outcome(s, listOf(Fx.Message(Obstacles.hint(cell))))
        if (Content.isGenerator(item)) return useGenerator(s, i, now, rnd)
        val ch = Content.chainOf(item) ?: return Outcome(s)
        if (ch.collect != null) {
            val amt = Content.collectAmount(item)
            val board = s.board.toMutableList().also { it[i] = Cell() }
            val ns = when (ch.collect) {
                Currency.COINS -> s.copy(coins = s.coins + amt)
                Currency.RUBIES -> s.copy(rubies = s.rubies + amt)
                Currency.ENERGY -> s.copy(energy = s.energy + amt)
            }.copy(board = board)
            return Outcome(ns, listOf(Fx.Collect(i, ch.collect, amt)))
        }
        if (ch.id == "chest") return openChest(s, i, rnd)
        return Outcome(s)
    }

    fun useGenerator(s: GameState, i: Int, now: Long, rnd: Random): Outcome {
        val gid = Content.genId(s.board[i].item!!)
        val def = Content.generator(gid) ?: return Outcome(s)
        val g = s.generators[gid] ?: GenState(def.charges)
        if (g.charges <= 0) return Outcome(s, listOf(Fx.Error))
        if (s.energy < 1) return Outcome(s, listOf(Fx.Error, Fx.Message("NO_ENERGY")))
        val to = nearestEmpty(s.board, i) ?: return Outcome(s, listOf(Fx.Error, Fx.Message("The board is full! Merge, serve or sell something.")))
        val chain = weighted(def.outputs, rnd)
        val lv = if (rnd.nextInt(100) < bonusChance(s, gid)) 2 else 1
        val item = "$chain:$lv"
        val board = s.board.toMutableList()
        board[to] = Cell(item, uid = s.nextUid)
        val charges = g.charges - 1
        val ng = if (charges <= 0) GenState(0, now + def.cooldownMinutes * 60_000L) else g.copy(charges = charges)
        var ns = s.copy(board = board, nextUid = s.nextUid + 1, energy = s.energy - 1,
            generators = s.generators + (gid to ng), counters = s.counters + ("taps" to s.count("taps") + 1))
        val fx = mutableListOf<Fx>(Fx.Spawn(i, to, s.nextUid))
        if (item !in ns.discovered) { ns = ns.copy(discovered = ns.discovered + item) }
        return Outcome(ns, fx)
    }

    /** Finished renovation tasks make some generators a bit better (chance of a level-2 item). */
    fun bonusChance(s: GameState, gid: String): Int {
        var c = 8
        if ("c1_display" in s.tasksDone && gid == "mixing_bowl") c += 10
        if ("c1_party" in s.tasksDone) c += 5
        if ("c2_party" in s.tasksDone) c += 5
        return c
    }

    private fun weighted(opts: List<Pair<String, Int>>, rnd: Random): String {
        var x = rnd.nextInt(opts.sumOf { it.second })
        for ((k, w) in opts) { if (x < w) return k; x -= w }
        return opts.last().first
    }

    fun openChest(s: GameState, i: Int, rnd: Random): Outcome {
        val cell = s.board[i]
        val lv = Content.level(cell.item!!)
        var left = if (cell.left > 0) cell.left else Content.chestSize(lv)
        val board = s.board.toMutableList()
        var uid = s.nextUid
        val fx = mutableListOf<Fx>()
        val pool = chestPool(s, lv)
        var disc = s.discovered
        while (left > 0) {
            val to = nearestEmpty(board, i) ?: break
            val item = pool.random(rnd)
            board[to] = Cell(item, uid = uid)
            fx += Fx.Spawn(i, to, uid)
            uid++; left--
            disc = disc + item
        }
        if (fx.isEmpty()) return Outcome(s, listOf(Fx.Error, Fx.Message("No room to open the gift. Make some space!")))
        board[i] = if (left > 0) cell.copy(left = left) else Cell()
        return Outcome(s.copy(board = board, nextUid = uid, discovered = disc), fx)
    }

    private fun chestPool(s: GameState, lv: Int): List<String> {
        val chains = ownedChains(s)
        val items = chains.flatMap { c -> (1..minOf(lv + 1, 3)).map { "$c:$it" } }
        val extras = when (lv) { 1 -> listOf("coins:1", "energy:1"); 2 -> listOf("coins:2", "energy:2", "rubies:1"); else -> listOf("coins:3", "energy:2", "rubies:2") }
        return items + extras + extras
    }

    /** Item chains the player can make with the generators they own. */
    fun ownedChains(s: GameState): List<String> {
        val gens = (s.board.mapNotNull { it.item } + s.storage.filterNotNull() + s.pending).filter { Content.isGenerator(it) }
        return gens.flatMap { Content.generator(Content.genId(it))?.outputs?.map { o -> o.first } ?: emptyList() }
            .filter { Content.chain(it)?.collect == null && Content.chain(it)?.event == null }.distinct()
    }

    /** Dragging an item from one cell to another: merge, move or swap. */
    fun move(s: GameState, from: Int, to: Int, rnd: Random): Outcome {
        if (from == to) return Outcome(s)
        val a = s.board[from]
        val b = s.board[to]
        val item = a.item ?: return Outcome(s)
        if (a.locked) return Outcome(s)
        val board = s.board.toMutableList()
        // Deliver the item a crate is asking for
        if (b.cover == "crate") {
            if (item != b.need) return Outcome(s, listOf(Fx.Error, Fx.Message(Obstacles.hint(b))))
            board[from] = Cell()
            board[to] = b.copy(cover = "", need = "", hp = 0, uid = s.nextUid)
            return Outcome(s.copy(board = board, nextUid = s.nextUid + 1, counters = s.counters + ("cleared" to s.count("cleared") + 1)),
                listOf(Fx.Unbox(to)))
        }
        // Merge (you can merge into a packing box, but not into an obstacle)
        if (b.item == item && b.cover.isEmpty() && Content.next(item) != null && !Content.isGenerator(item)) {
            val result = Content.next(item)!!
            board[from] = Cell()
            board[to] = Cell(result, uid = s.nextUid)
            var uid = s.nextUid + 1
            val fx = mutableListOf<Fx>(Fx.Merge(to, result))
            // Open neighbouring boxes
            for (n in neighbours(to)) if (board[n].boxed) { board[n] = board[n].copy(boxed = false, uid = uid++); fx += Fx.Unbox(n) }
            // Wear down obstacles next to the merge
            var cleared = 0
            for (n in neighbours(to)) {
                val c = board[n]
                if (c.cover.isEmpty() || c.cover == "crate") continue
                if (c.hp <= 1) { board[n] = c.copy(cover = "", hp = 0, uid = uid++); fx += Fx.Unbox(n); cleared++ }
                else board[n] = c.copy(hp = c.hp - 1)
            }
            var ns = s.copy(board = board, nextUid = uid, counters = s.counters + ("merges" to s.count("merges") + 1) +
                if (cleared > 0) mapOf("cleared" to s.count("cleared") + cleared) else emptyMap())
            if (result !in ns.discovered) {
                ns = ns.copy(discovered = ns.discovered + result)
                fx += Fx.Discover(result)
                if (Content.level(result) >= 4 && Content.chainOf(result)?.collect == null) ns = ns.copy(rubies = ns.rubies + 1)
            }
            return Outcome(ns, fx)
        }
        if (b.locked) return Outcome(s, listOf(Fx.Error))
        if (b.item == null) {
            board[to] = a; board[from] = Cell()
        } else {
            board[to] = a; board[from] = b
        }
        return Outcome(s.copy(board = board), listOf(Fx.Pop))
    }

    fun neighbours(i: Int): List<Int> {
        val r = i / BOARD_COLS; val c = i % BOARD_COLS
        return listOfNotNull(
            if (r > 0) i - BOARD_COLS else null, if (r < BOARD_ROWS - 1) i + BOARD_COLS else null,
            if (c > 0) i - 1 else null, if (c < BOARD_COLS - 1) i + 1 else null,
        )
    }

    fun sell(s: GameState, i: Int): Outcome {
        val item = s.board[i].item ?: return Outcome(s)
        if (s.board[i].locked) return Outcome(s)
        val price = Content.sellPrice(item)
        if (price <= 0) return Outcome(s, listOf(Fx.Error))
        val board = s.board.toMutableList().also { it[i] = Cell() }
        return Outcome(s.copy(board = board, coins = s.coins + price), listOf(Fx.Sold, Fx.Collect(i, Currency.COINS, price)))
    }

    fun store(s: GameState, i: Int): Outcome {
        val item = s.board[i].item ?: return Outcome(s)
        if (s.board[i].locked) return Outcome(s)
        val slot = s.storage.indexOfFirst { it == null }
        if (slot < 0) return Outcome(s, listOf(Fx.Error, Fx.Message("The pantry shelf is full.")))
        val board = s.board.toMutableList().also { it[i] = Cell() }
        val st = s.storage.toMutableList().also { it[slot] = item }
        return Outcome(s.copy(board = board, storage = st), listOf(Fx.Pop))
    }

    fun unstore(s: GameState, slot: Int): Outcome {
        val item = s.storage.getOrNull(slot) ?: return Outcome(s)
        val to = nearestEmpty(s.board, idx(BOARD_ROWS / 2, BOARD_COLS / 2)) ?: return Outcome(s, listOf(Fx.Error, Fx.Message("The board is full!")))
        val board = s.board.toMutableList().also { it[to] = Cell(item, uid = s.nextUid) }
        val st = s.storage.toMutableList().also { it[slot] = null }
        return Outcome(s.copy(board = board, storage = st, nextUid = s.nextUid + 1), listOf(Fx.Pop))
    }

    fun buyStorageSlot(s: GameState): Outcome {
        if (s.storage.size >= Economy.MAX_STORAGE) return Outcome(s)
        val cost = Economy.storageSlotCost(s.storage.size)
        if (s.rubies < cost) return Outcome(s, listOf(Fx.Error, Fx.Message("Not enough rubies. Earn some in the shop with a puzzle!")))
        return Outcome(s.copy(rubies = s.rubies - cost, storage = s.storage + null), listOf(Fx.Pop))
    }

    /** Puts the next waiting reward item on the board. */
    fun placePending(s: GameState): Outcome {
        val item = s.pending.firstOrNull() ?: return Outcome(s)
        val to = nearestEmpty(s.board, idx(BOARD_ROWS / 2, BOARD_COLS / 2)) ?: return Outcome(s, listOf(Fx.Error, Fx.Message("The board is full!")))
        val board = s.board.toMutableList().also { it[to] = Cell(item, uid = s.nextUid) }
        return Outcome(s.copy(board = board, pending = s.pending.drop(1), nextUid = s.nextUid + 1, discovered = s.discovered + item),
            listOf(Fx.Spawn(-1, to, s.nextUid)))
    }

    fun skipCooldown(s: GameState, gid: String, now: Long): Outcome {
        val g = s.generators[gid] ?: return Outcome(s)
        if (g.charges > 0) return Outcome(s)
        val cost = Economy.skipCost(g.cooldownUntil - now)
        if (s.rubies < cost) return Outcome(s, listOf(Fx.Error, Fx.Message("Not enough rubies.")))
        return Outcome(rechargeNow(s.copy(rubies = s.rubies - cost), gid), listOf(Fx.Pop))
    }

    fun rechargeNow(s: GameState, gid: String) =
        s.copy(generators = s.generators + (gid to GenState(Content.generator(gid)?.charges ?: 20, 0)))

    // ---------------------------------------------------------------- orders

    /** Board cells (unboxed) that can fill this order, or null if it can't be filled yet. */
    fun orderCells(s: GameState, o: Order): List<Int>? {
        val used = mutableSetOf<Int>()
        for (w in o.wants) {
            val i = s.board.indices.firstOrNull { it !in used && s.board[it].item == w && !s.board[it].locked } ?: return null
            used += i
        }
        return used.toList()
    }

    fun serve(s: GameState, orderId: Long, today: LocalDate, rnd: Random): Outcome {
        val o = s.orders.firstOrNull { it.id == orderId } ?: return Outcome(s)
        val cells = orderCells(s, o) ?: return Outcome(s, listOf(Fx.Error))
        val board = s.board.toMutableList()
        cells.forEach { board[it] = Cell() }
        var ns = s.copy(board = board, orders = s.orders.filter { it.id != orderId }, coins = s.coins + o.coins,
            passPts = s.passPts + o.pts, counters = s.counters + ("orders" to s.count("orders") + 1),
            weekPts = s.weekPts + Goals.orderPoints(o, today))
        val fx = mutableListOf<Fx>(Fx.OrderDone(orderId, o.coins))
        val xo = addXp(ns, o.xp)
        ns = xo.state; fx += xo.fx
        ns = fillOrders(ns, today, rnd)
        return Outcome(ns, fx)
    }

    fun addXp(s: GameState, xp: Int): Outcome {
        var ns = s.copy(xp = s.xp + xp)
        val fx = mutableListOf<Fx>()
        while (ns.xp >= Economy.xpToNext(ns.level)) {
            ns = ns.copy(xp = ns.xp - Economy.xpToNext(ns.level), level = ns.level + 1)
            val r = Economy.levelUpReward(ns.level)
            ns = grant(ns, r)
            ns = giveMissingGenerators(ns)
            fx += Fx.LevelUp(ns.level, r)
        }
        return Outcome(ns, fx)
    }

    private val orderLines = listOf(
        "One %s, please! It smells like cinnamon rolls in here again…",
        "I'd love a %s. Is that Surf behind the counter?",
        "Could I get a %s? I've been thinking about it all day.",
        "A %s for me, please! Your bakery is the coziest place in town.",
        "Do you have a %s? Surf told me you did. Well, he barked.",
        "%s, please! And a pat for Surf, if he's allowed.",
        "I'll take a %s. My whole house smells like your bakery now!",
    )

    fun fillOrders(s0: GameState, today: LocalDate, rnd: Random): GameState {
        var s = s0
        val slots = orderSlots(s, today)
        while (s.orders.size < slots) {
            val o = newOrder(s, rnd) ?: break
            s = s.copy(orders = s.orders + o)
        }
        return s
    }

    fun newOrder(s: GameState, rnd: Random): Order? {
        val chains = ownedChains(s).ifEmpty { return null }
        val busy = s.orders.map { it.customer }.toSet()
        val who = Content.customers.filter { it.id !in busy }.randomOrNull(rnd) ?: Content.customers.random(rnd)
        val count = if (s.level >= 3 && rnd.nextInt(100) < 35) 2 else 1
        val wants = mutableListOf<String>()
        repeat(count) {
            val c = Content.chain(chains.random(rnd))!!
            val cap = minOf(c.max, 2 + s.level / 2 + rnd.nextInt(2))
            val lv = rnd.nextInt(maxOf(2, cap - 2), cap + 1)
            wants += "${c.id}:$lv"
        }
        val coins = wants.sumOf { Content.value(it) } + 5
        val xp = wants.sumOf { Content.level(it) } + 1
        val pts = maxOf(1, wants.sumOf { Content.level(it) } / 2)
        val line = orderLines.random(rnd).format(Content.name(wants[0]))
        return Order(s.nextUid + rnd.nextLong(1_000_000), who.id, wants, coins, xp, pts, line.replaceFirstChar { it.uppercase() })
    }

    // ---------------------------------------------------------------- rewards, story, daily

    fun grant(s: GameState, r: Reward, today: LocalDate? = null): GameState = s.copy(
        coins = s.coins + r.coins, rubies = s.rubies + r.rubies, energy = s.energy + r.energy,
        pending = s.pending + r.items,
        vipSeason = if (r.vip && today != null) season(today) else s.vipSeason,
    )

    fun completeTask(s: GameState, taskId: String, today: LocalDate? = null): Outcome {
        val t = Story.task(taskId) ?: return Outcome(s)
        if (taskId in s.tasksDone || Story.nextTask(s.tasksDone, today)?.id != taskId) return Outcome(s)
        if (s.coins < t.cost) return Outcome(s, listOf(Fx.Error, Fx.Message("You need ${t.cost - s.coins} more coins. Fill some orders!")))
        var ns = s.copy(coins = s.coins - t.cost, tasksDone = s.tasksDone + taskId)
        val fx = mutableListOf<Fx>()
        val ch = Story.chapterOf(taskId)
        if (ch.tasks.all { it.id in ns.tasksDone }) { ns = grant(ns, ch.reward); fx += Fx.Granted(ch.reward) }
        val xo = addXp(ns, 5)
        return Outcome(xo.state, fx + xo.fx)
    }

    fun dailyAvailable(s: GameState, today: LocalDate) = s.lastDaily != today.toString()
    fun dailyIndex(s: GameState) = s.dailyClaims % 7

    fun claimDaily(s: GameState, today: LocalDate): Outcome {
        if (!dailyAvailable(s, today)) return Outcome(s)
        val r = Economy.daily[dailyIndex(s)]
        return Outcome(grant(s, r).copy(lastDaily = today.toString(), dailyClaims = s.dailyClaims + 1), listOf(Fx.Granted(r)))
    }

    fun claimFreeTreat(s: GameState, today: LocalDate): Outcome {
        if (s.freeTreatDay == today.toString()) return Outcome(s)
        return Outcome(grant(s, Economy.freeTreat).copy(freeTreatDay = today.toString()), listOf(Fx.Granted(Economy.freeTreat)))
    }

    fun stockLeft(s: GameState, o: Offer) = o.perDay - (s.shopBought[o.id] ?: 0)

    /** After the puzzles for an offer are solved. [bonus] is the hidden quick-solve multiplier. */
    fun buyOffer(s: GameState, o: Offer, bonus: Double, today: LocalDate): Outcome {
        val r = if (bonus > 1.0) o.reward.scaled(bonus) else o.reward
        val ns = grant(s, r, today).copy(shopBought = s.shopBought + (o.id to (s.shopBought[o.id] ?: 0) + 1))
        return Outcome(ns, listOf(Fx.Granted(r)))
    }

    // ---------------------------------------------------------------- pass & events

    fun passTier(s: GameState) = minOf(Economy.PASS_TIERS, s.passPts / Economy.PASS_PTS_PER_TIER)

    fun claimPass(s: GameState, tier: Int, vipTrack: Boolean, today: LocalDate): Outcome {
        if (tier >= passTier(s)) return Outcome(s)
        if (vipTrack) {
            if (!vip(s, today) || tier in s.passClaimedVip) return Outcome(s)
            val r = Economy.passVip(tier)
            return Outcome(grant(s, r).copy(passClaimedVip = s.passClaimedVip + tier), listOf(Fx.Granted(r)))
        }
        if (tier in s.passClaimedFree) return Outcome(s)
        val r = Economy.passFree(tier)
        return Outcome(grant(s, r).copy(passClaimedFree = s.passClaimedFree + tier), listOf(Fx.Granted(r)))
    }

    fun turnInEvent(s: GameState, i: Int): Outcome {
        val item = s.board[i].item ?: return Outcome(s)
        val ch = Content.chainOf(item) ?: return Outcome(s)
        if (ch.event == null || ch.event != s.eventId || s.board[i].locked) return Outcome(s)
        val pts = Events.points(Content.level(item))
        val board = s.board.toMutableList().also { it[i] = Cell() }
        return Outcome(s.copy(board = board, eventPts = s.eventPts + pts), listOf(Fx.Message("+$pts event points!"), Fx.Sold))
    }

    fun claimEvent(s: GameState, milestone: Int): Outcome {
        val ev = Events.byId(s.eventId) ?: return Outcome(s)
        val (need, r) = ev.milestones.getOrNull(milestone) ?: return Outcome(s)
        if (s.eventPts < need || milestone in s.eventClaimed) return Outcome(s)
        val decor = if (ev.scene != null && milestone < ev.decor.size) s.eventDecor + ev.decorId(milestone) else s.eventDecor
        return Outcome(grant(s, r).copy(eventClaimed = s.eventClaimed + milestone, eventDecor = decor), listOf(Fx.Granted(r)))
    }

    // ---------------------------------------------------------------- goals

    fun claimDailyGoal(s: GameState, id: String, today: LocalDate): Outcome {
        val g = Goals.today(today).firstOrNull { it.id == id } ?: return Outcome(s)
        if (id in s.dailyGoalsClaimed || Goals.progress(s, g) < g.target) return Outcome(s)
        return Outcome(grant(s, g.reward).copy(dailyGoalsClaimed = s.dailyGoalsClaimed + id), listOf(Fx.Granted(g.reward)))
    }

    fun claimDailyBonus(s: GameState, today: LocalDate): Outcome {
        if ("bonus" in s.dailyGoalsClaimed || !Goals.today(today).all { it.id in s.dailyGoalsClaimed }) return Outcome(s)
        return Outcome(grant(s, Goals.dailyBonus).copy(dailyGoalsClaimed = s.dailyGoalsClaimed + "bonus"), listOf(Fx.Granted(Goals.dailyBonus)))
    }

    fun claimWeek(s: GameState, i: Int): Outcome {
        val (need, r) = Goals.weekMilestones.getOrNull(i) ?: return Outcome(s)
        if (s.weekPts < need || i in s.weekClaimed) return Outcome(s)
        return Outcome(grant(s, r).copy(weekClaimed = s.weekClaimed + i), listOf(Fx.Granted(r)))
    }

    /** Something on the Goals screen can be claimed. */
    fun goalsReady(s: GameState, today: LocalDate): Boolean =
        dailyAvailable(s, today) ||
            Goals.today(today).any { it.id !in s.dailyGoalsClaimed && Goals.progress(s, it) >= it.target } ||
            (Goals.today(today).all { it.id in s.dailyGoalsClaimed } && "bonus" !in s.dailyGoalsClaimed) ||
            Goals.weekMilestones.withIndex().any { (i, m) -> s.weekPts >= m.first && i !in s.weekClaimed }

    // ---------------------------------------------------------------- puzzles

    /** Logs a puzzle result and lets the difficulty model learn from it. Returns the state and bonus flag. */
    fun recordPuzzle(s: GameState, type: PuzzleType, level: Int, solved: Boolean, mistakes: Int, hints: Int,
                     seconds: Int, now: Long): Pair<GameState, Boolean> {
        val expected = Brain.expectedSeconds(s.puzzleLog, type, level)
        val perf = Brain.performance(solved, mistakes, hints, seconds, expected)
        val bonus = Brain.speedBonus(solved, mistakes, hints, seconds, expected)
        val old = Brain.factor(s, type)
        val rec = PuzzleRecord(type.name, level, solved, mistakes, hints, seconds, now, bonus, old)
        val log = (s.puzzleLog + rec).takeLast(1000)
        val counters = s.counters + ("puzzles" to s.count("puzzles") + 1) + if (solved) mapOf("solved" to s.count("solved") + 1) else emptyMap()
        return s.copy(brain = s.brain + (type.name to Brain.updatedFactor(old, perf)), puzzleLog = log, counters = counters) to bonus
    }
}
