package com.surfswirl.bakery.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.min
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.surfswirl.bakery.game.BOARD_COLS
import com.surfswirl.bakery.game.BOARD_ROWS
import com.surfswirl.bakery.game.Cell
import com.surfswirl.bakery.game.Content
import com.surfswirl.bakery.game.Economy
import com.surfswirl.bakery.game.Engine
import com.surfswirl.bakery.game.Events
import com.surfswirl.bakery.game.GameState
import com.surfswirl.bakery.game.GenState
import com.surfswirl.bakery.game.Order
import com.surfswirl.bakery.ui.GameViewModel
import com.surfswirl.bakery.ui.Geometry
import com.surfswirl.bakery.ui.Screen
import com.surfswirl.bakery.ui.Ui
import com.surfswirl.bakery.ui.Dialog
import com.surfswirl.bakery.ui.components.Badge
import com.surfswirl.bakery.ui.components.CandyButton
import com.surfswirl.bakery.ui.components.CurrencyPill
import com.surfswirl.bakery.ui.components.ItemArt
import com.surfswirl.bakery.ui.components.NavButton
import com.surfswirl.bakery.ui.components.OutlinedText
import com.surfswirl.bakery.ui.components.Panel
import com.surfswirl.bakery.ui.components.ProgressBar
import com.surfswirl.bakery.ui.components.bob
import com.surfswirl.bakery.ui.components.pulse
import com.surfswirl.bakery.ui.components.wiggle
import com.surfswirl.bakery.ui.theme.LocalGameColors
import com.surfswirl.bakery.ui.theme.Palette
import kotlin.math.PI
import kotlin.math.sin

fun mmss(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
}

@Composable
fun BoardScreen(vm: GameViewModel, state: GameState, ui: Ui, geo: Geometry) {
    LaunchedEffect(ui.dialogs.isEmpty()) { if (ui.dialogs.isEmpty()) vm.startTutorialIfNeeded() }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Hud(vm, state, geo)
        val tip = tutorialText(state.tutorial)
        if (tip != null) TutorialBanner(tip)
        Orders(vm, state)
        Box(Modifier.weight(1f).fillMaxWidth()) { Board(vm, state, ui, geo) }
        InfoBar(vm, state, ui)
        BottomBar(vm, state)
    }
}

private fun tutorialText(step: Int) = when (step) {
    1 -> "Drag one Wheat onto the other Wheat to merge them!"
    2 -> "Grandma June wants a Flatbread. Tap Serve on her order!"
    3 -> "Tap the Mixing Bowl to bake more. Each tap uses 1 energy."
    4 -> "Fill orders for coins, then tap Bakery to fix the place up!"
    else -> null
}

@Composable
private fun TutorialBanner(text: String) {
    Panel(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp), color = Color(0xFFFFF3C4)) {
        Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            ItemArt("surf_face", Modifier.size(34.dp).bob(3f))
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ------------------------------------------------------------------ HUD

@Composable
private fun Hud(vm: GameViewModel, state: GameState, geo: Geometry) {
    val now by vm.clock.collectAsStateWithLifecycle()
    val max = Engine.maxEnergy(state, vm.today())
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        // Level badge
        Box(Modifier.semantics { contentDescription = "Level ${state.level}" }.onGloballyPositioned {
            val p = it.positionInWindow(); geo.xp = Offset(p.x + it.size.width / 2f, p.y + it.size.height / 2f)
        }.clickable { vm.toast("Level ${state.level} · ${state.xp}/${Economy.xpToNext(state.level)} XP to the next level") }) {
            Box(Modifier.size(46.dp).shadow(3.dp, CircleShape).clip(CircleShape).background(Color.White).border(2.5.dp, Palette.Gold, CircleShape),
                contentAlignment = Alignment.Center) { ItemArt("mandy_face", Modifier.size(42.dp)) }
            Box(Modifier.align(Alignment.BottomEnd).offset(4.dp, 4.dp).size(22.dp).clip(CircleShape).background(Palette.Blue)
                .border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                OutlinedText("${state.level}", 12, outline = Palette.BlueDark)
            }
        }
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                CurrencyPill("high_voltage", "${state.energy}", Palette.Energy, Modifier.weight(1f).onGloballyPositioned {
                    val p = it.positionInWindow(); geo.energy = Offset(p.x + 30f, p.y + it.size.height / 2f)
                }, desc = "Energy ${state.energy}") { vm.pushDialog(Dialog.OutOfEnergy) }
                CurrencyPill("coin", "${state.coins}", Palette.Coin, Modifier.weight(1f).onGloballyPositioned {
                    val p = it.positionInWindow(); geo.coins = Offset(p.x + 30f, p.y + it.size.height / 2f)
                }, desc = "Coins ${state.coins}")
                CurrencyPill("gem_stone", "${state.rubies}", Palette.Ruby, Modifier.weight(1f).onGloballyPositioned {
                    val p = it.positionInWindow(); geo.rubies = Offset(p.x + 30f, p.y + it.size.height / 2f)
                }, desc = "Rubies ${state.rubies}") { vm.go(Screen.SHOP) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressBar(state.xp.toFloat() / Economy.xpToNext(state.level), Modifier.width(90.dp), Palette.Blue, 10.dp)
                Spacer(Modifier.width(6.dp))
                val next = Engine.regenMs(state, vm.today()) - (now - state.energyAt)
                if (state.energy < max) Text("+1 ⚡ in ${mmss(next)}", style = MaterialTheme.typography.bodySmall)
                else Text("Energy full", style = MaterialTheme.typography.bodySmall)
                if (Engine.vip(state, vm.today())) { Spacer(Modifier.width(6.dp)); Badge("VIP", color = Palette.GoldDark) }
            }
        }
    }
}


// ------------------------------------------------------------------ orders

@Composable
private fun Orders(vm: GameViewModel, state: GameState) {
    LazyRow(Modifier.fillMaxWidth().height(112.dp), contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.orders, key = { it.id }) { o -> OrderCard(vm, state, o) }
    }
}

@Composable
private fun OrderCard(vm: GameViewModel, state: GameState, o: Order) {
    val ready = Engine.orderCells(state, o) != null
    val who = Content.character(o.customer)
    val enter = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f)) }
    Panel(Modifier.width(if (o.wants.size > 1) 176.dp else 138.dp).height(104.dp)
        .graphicsLayer { scaleX = enter.value; scaleY = enter.value }
        .semantics { contentDescription = "Order from ${who.name}" }
        .clickable { vm.toast("${who.name}: “${o.line}”") },
        color = if (ready) Color(0xFFE9FFE4) else Palette.Cream) {
        Column(Modifier.padding(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ItemArt(who.art, Modifier.size(40.dp).bob(2f, 1800 + (o.id % 500).toInt()))
                Spacer(Modifier.width(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (w in o.wants) {
                        val have = state.board.any { it.item == w && !it.locked }
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x22FFB070))) {
                            ItemArt(Content.art(w), Modifier.fillMaxSize().padding(3.dp).wiggle(have && ready, 4f))
                            if (have) Box(Modifier.size(16.dp).align(Alignment.BottomEnd).clip(CircleShape).background(Palette.Green)
                                .border(1.5.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) { OutlinedText("✓", 10, outline = Palette.GreenDark) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            if (ready) CandyButton("Serve", Modifier.fillMaxWidth().pulse(), small = true) { vm.serve(o.id) }
            else Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
                ItemArt("coin", Modifier.size(20.dp)); Text(" ${o.coins}", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.width(8.dp))
                ItemArt("glowing_star", Modifier.size(18.dp)); Text(" ${o.xp}", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

// ------------------------------------------------------------------ board

private data class DragState(val from: Int, val pos: Offset)

@Composable
private fun Board(vm: GameViewModel, state: GameState, ui: Ui, geo: Geometry) {
    val colors = LocalGameColors.current
    // The board takes on the colours of the current chapter's place.
    val palette = boardPalette(com.surfswirl.bakery.game.Story.chapterIndex(state.tasksDone), colors)
    val now by vm.clock.collectAsStateWithLifecycle()
    val lastTouch by vm.lastTouch.collectAsStateWithLifecycle()
    val board by rememberUpdatedState(state.board)
    var drag by remember { mutableStateOf<DragState?>(null) }
    val slop = LocalViewConfiguration.current.touchSlop
    val hintPair = remember(state.board) { findPair(state.board) }
    val showHint = now - lastTouch > 7000 && ui.dialogs.isEmpty()
    val wanted = remember(state.orders) { state.orders.flatMap { it.wants }.toSet() }

    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
        val cell: Dp = min(maxWidth / BOARD_COLS, maxHeight / BOARD_ROWS)
        Box(
            Modifier.size(cell * BOARD_COLS, cell * BOARD_ROWS)
                .onGloballyPositioned { geo.boardOrigin = it.positionInWindow(); geo.cellPx = it.size.width / BOARD_COLS.toFloat() }
                .semantics { contentDescription = "Board" }
                .pointerInput(Unit) {
                    val cpx = size.width / BOARD_COLS.toFloat()
                    fun cellAt(p: Offset): Int? {
                        val c = (p.x / cpx).toInt(); val r = (p.y / cpx).toInt()
                        return if (p.x < 0 || p.y < 0 || c !in 0 until BOARD_COLS || r !in 0 until BOARD_ROWS) null else r * BOARD_COLS + c
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val start = cellAt(down.position) ?: return@awaitEachGesture
                        val movable = board[start].item != null && !board[start].locked
                        var dragging = false
                        var pos = down.position
                        while (true) {
                            val ev = awaitPointerEvent()
                            val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                            if (!ch.pressed) break
                            pos = ch.position
                            if (!dragging && movable && (pos - down.position).getDistance() > slop) dragging = true
                            if (dragging) { drag = DragState(start, pos); ch.consume() }
                        }
                        drag = null
                        if (dragging) cellAt(pos)?.let { vm.drag(start, it) } else vm.tapCell(start)
                    }
                },
        ) {
            // Board frame and checker tiles
            Canvas(Modifier.fillMaxSize()) {
                val cp = size.width / BOARD_COLS
                drawRoundRect(palette.third, Offset(-8f, -8f), Size(size.width + 16f, size.height + 16f), CornerRadius(36f))
                drawRoundRect(Color.White.copy(alpha = 0.5f), Offset(-4f, -4f), Size(size.width + 8f, size.height + 8f), CornerRadius(30f), style = Stroke(4f))
                for (i in 0 until BOARD_COLS * BOARD_ROWS) {
                    val r = i / BOARD_COLS; val c = i % BOARD_COLS
                    drawRoundRect(if ((r + c) % 2 == 0) palette.first else palette.second, Offset(c * cp + 2f, r * cp + 2f),
                        Size(cp - 4f, cp - 4f), CornerRadius(cp * 0.22f))
                }
            }
            // Selection ring
            ui.selected?.let { s ->
                if (state.board.getOrNull(s)?.item != null) {
                    Box(Modifier.offset(cell * (s % BOARD_COLS), cell * (s / BOARD_COLS)).size(cell).pulse(amount = 0.05f, period = 600)
                        .border(3.dp, Color.White, RoundedCornerShape(cell * 0.24f))
                        .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(cell * 0.24f)))
                }
            }
            // Drop target highlight
            drag?.let { d ->
                val cpx = geo.cellPx
                val c = (d.pos.x / cpx).toInt(); val r = (d.pos.y / cpx).toInt()
                if (c in 0 until BOARD_COLS && r in 0 until BOARD_ROWS) {
                    val t = r * BOARD_COLS + c
                    val from = state.board[d.from].item
                    val canMerge = t != d.from && state.board[t].item == from && from != null && Content.next(from) != null && !Content.isGenerator(from)
                    Box(Modifier.offset(cell * c, cell * r).size(cell)
                        .background(if (canMerge) Palette.Green.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.35f), RoundedCornerShape(cell * 0.24f))
                        .border(3.dp, if (canMerge) Palette.Green else Color.White, RoundedCornerShape(cell * 0.24f)))
                }
            }
            // Items
            state.board.forEachIndexed { i, c ->
                if (c.item != null) key(c.uid) {
                    BoardItem(vm, i, c, cell, state.generators, now,
                        dragging = drag?.from == i,
                        wanted = c.item in wanted && !c.locked,
                        hint = showHint && hintPair != null && i in hintPair)
                }
            }
            // The dragged item follows the finger
            drag?.let { d ->
                val item = state.board[d.from].item
                if (item != null) {
                    val half = geo.cellPx * 0.6f
                    Box(Modifier.offset { IntOffset((d.pos.x - half).toInt(), (d.pos.y - half * 1.5f).toInt()) }.size(cell * 1.2f)) {
                        ItemArt(Content.art(item), Modifier.fillMaxSize().padding(2.dp))
                    }
                }
            }
        }
    }
}

/** Two matching mergeable items, for the idle hint wiggle. */
private fun findPair(board: List<Cell>): Set<Int>? {
    val seen = mutableMapOf<String, Int>()
    for ((i, c) in board.withIndex()) {
        val it = c.item ?: continue
        if (c.locked || Content.isGenerator(it) || Content.next(it) == null) continue
        val j = seen[it]
        if (j != null) return setOf(i, j)
        seen[it] = i
    }
    return null
}

@Composable
private fun BoardItem(vm: GameViewModel, i: Int, c: Cell, cell: Dp, gens: Map<String, GenState>, now: Long,
                      dragging: Boolean, wanted: Boolean, hint: Boolean) {
    val item = c.item ?: return
    val origin = remember { vm.spawnFrom.remove(c.uid) }
    val popped = remember { vm.popUids.remove(c.uid) }
    val fly = remember { Animatable(if (origin != null) 0f else 1f) }
    val pop = remember { Animatable(if (popped) 0.2f else 1f) }
    LaunchedEffect(Unit) {
        if (origin != null) fly.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        if (popped) pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMediumLow))
    }
    // Small bounce whenever the item lands in a new cell.
    val land = remember { Animatable(1f) }
    var lastCell by remember { mutableStateOf(i) }
    LaunchedEffect(i) {
        if (lastCell != i) { lastCell = i; land.snapTo(0.75f); land.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 600f)) }
    }
    val col = i % BOARD_COLS; val row = i / BOARD_COLS
    val gen = Content.isGenerator(item)
    val gstate = if (gen) gens[Content.genId(item)] else null
    val cooling = gstate != null && gstate.charges <= 0
    val ch = Content.chainOf(item)
    Box(
        Modifier
            .offset {
                val tx = col * cell.toPx(); val ty = row * cell.toPx()
                if (origin != null && fly.value < 1f) {
                    val (ox, oy) = if (origin < 0) (3 * cell.toPx() to 9 * cell.toPx()) else ((origin % BOARD_COLS) * cell.toPx() to (origin / BOARD_COLS) * cell.toPx())
                    val t = fly.value
                    val arc = sin(PI * t).toFloat() * cell.toPx() * 0.8f
                    IntOffset((ox + (tx - ox) * t).toInt(), (oy + (ty - oy) * t - arc).toInt())
                } else IntOffset(tx.toInt(), ty.toInt())
            }
            .size(cell)
            .graphicsLayer {
                val s = (if (origin != null) 0.4f + 0.6f * fly.value else 1f) * pop.value * land.value
                scaleX = s; scaleY = s
                alpha = if (dragging) 0.3f else 1f
            }
            .semantics { contentDescription = (if (c.boxed) "Boxed " else if (c.cover.isNotEmpty()) "${com.surfswirl.bakery.game.Obstacles.name(c.cover)} " else "") + Content.name(item) },
    ) {
        if (c.boxed) {
            Box(Modifier.fillMaxSize().padding(2.dp).clip(RoundedCornerShape(cell * 0.22f))
                .background(Brush.verticalGradient(listOf(Color(0xFFE2B37E), Color(0xFFC48A55))))
                .border(2.dp, Color(0xFFF5D7AE), RoundedCornerShape(cell * 0.22f))) {
                ItemArt(Content.art(item), Modifier.fillMaxSize().padding(cell * 0.18f), alpha = 0.55f, gray = true)
                // tape
                Box(Modifier.align(Alignment.Center).fillMaxWidth().height(cell * 0.16f).background(Color(0x99F7E2B8)))
            }
            return@Box
        }
        if (c.cover.isNotEmpty()) { ObstacleTile(c, cell); return@Box }
        val mod = Modifier.fillMaxSize().padding(cell * 0.06f)
            .then(if (hint) Modifier.wiggle(true, 9f) else Modifier)
            .then(if (gen && !cooling) Modifier.pulse(amount = 0.05f, period = 1100) else Modifier)
            .then(if (wanted) Modifier.bob(2.5f, 900) else Modifier)
        ItemArt(Content.art(item), mod, gray = cooling)
        if (cooling) {
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp).clip(RoundedCornerShape(50)).background(Color(0xCC5B3220))
                .padding(horizontal = 4.dp)) { OutlinedText(mmss(gstate!!.cooldownUntil - now), 10, outline = Color.Black) }
            ItemArt("hourglass_done", Modifier.size(cell * 0.38f).align(Alignment.TopEnd))
        }
        if (gen && !cooling && gstate != null) {
            Box(Modifier.align(Alignment.TopEnd).padding(1.dp)) { ItemArt("high_voltage", Modifier.size(cell * 0.3f)) }
        }
        if (ch?.collect != null) Box(Modifier.align(Alignment.BottomEnd)) { OutlinedText("+${Content.collectAmount(item)}", 11) }
        if (ch?.id == "chest" && c.left > 0) Badge("${c.left}", Modifier.align(Alignment.TopEnd))
        if (ch?.event != null) ItemArt("sparkles", Modifier.size(cell * 0.3f).align(Alignment.TopStart))
        if (wanted) Box(Modifier.align(Alignment.TopEnd).size(cell * 0.3f).clip(CircleShape).background(Palette.Green)
            .border(1.5.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) { OutlinedText("✓", 10, outline = Palette.GreenDark) }
        if (!gen && ch != null && ch.collect == null && ch.id != "chest" && Content.level(item) == ch.max)
            ItemArt("star", Modifier.size(cell * 0.3f).align(Alignment.BottomEnd))
    }
}

// ------------------------------------------------------------------ info bar

@Composable
private fun InfoBar(vm: GameViewModel, state: GameState, ui: Ui) {
    val now by vm.clock.collectAsStateWithLifecycle()
    val sel = ui.selected?.let { state.board.getOrNull(it) }?.takeIf { it.item != null }
    Panel(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).height(74.dp)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (sel == null) {
                NavButtonSmall("basket", "Pantry") { vm.pushDialog(Dialog.Storage) }
                Spacer(Modifier.width(8.dp))
                Text(idleTip(state, now), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            } else {
                val item = sel.item!!
                ItemArt(Content.art(item), Modifier.size(52.dp).bob(2f))
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Text(Content.name(item), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(itemSubtitle(state, sel, now), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                ItemActions(vm, state, sel)
            }
            if (state.pending.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                Box(Modifier.semantics { contentDescription = "Gifts" }.clickable { vm.placePending() }) {
                    ItemArt(Content.art(state.pending.first()).let { if (Content.isGenerator(state.pending.first())) it else "wrapped_gift" },
                        Modifier.size(50.dp).wiggle(true, 5f))
                    Badge("${state.pending.size}", Modifier.align(Alignment.TopEnd))
                }
            }
        }
    }
}

@Composable
private fun NavButtonSmall(art: String, label: String, onClick: () -> Unit) {
    Column(Modifier.semantics { contentDescription = label }.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        ItemArt(art, Modifier.size(38.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun idleTip(s: GameState, now: Long): String {
    val tips = listOf(
        "Drag matching items together to merge them into something better.",
        "Tap a generator to bake. Each tap uses 1 energy.",
        "Out of energy? Solve a quick brain puzzle in the Shop!",
        "Merge next to a packing box to open it.",
        "Fill orders to earn coins, then fix up the bakery.",
        "Surf sometimes finds coins. Good boy!",
        "Tap an item, then the ⓘ button to see what it becomes.",
        "Put items on the Pantry shelf to save board space.",
    )
    return tips[((now / 9000) % tips.size).toInt()]
}

private fun itemSubtitle(s: GameState, c: Cell, now: Long): String {
    val item = c.item!!
    if (c.boxed) return "Merge something next to this box to open it."
    if (c.cover.isNotEmpty()) return com.surfswirl.bakery.game.Obstacles.hint(c)
    if (Content.isGenerator(item)) {
        val gid = Content.genId(item)
        val g = s.generators[gid] ?: return ""
        val def = Content.generator(gid)
        return if (g.charges <= 0) "Recharging… ready in ${mmss(g.cooldownUntil - now)}. Tap for options."
        else "Tap to make items (1 ⚡ each). ${g.charges}/${def?.charges} left before it recharges."
    }
    val ch = Content.chainOf(item) ?: return ""
    if (ch.collect != null) return "Tap to collect +${Content.collectAmount(item)}. Merge two for more!"
    if (ch.id == "chest") return "Tap again to open! It holds ${if (c.left > 0) c.left else Content.chestSize(Content.level(item))} items."
    val next = Content.next(item)
    val lv = "Level ${Content.level(item)} of ${ch.max}"
    if (ch.event != null) return "$lv · Turn in for ${Events.points(Content.level(item))} event points, or merge for more."
    return if (next != null) "$lv · Merge two to make ${Content.name(next)}" else "$lv · Max level! A customer will love this."
}

@Composable
private fun ItemActions(vm: GameViewModel, state: GameState, c: Cell) {
    val item = c.item!!
    if (c.locked) return
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        val ch = Content.chainOf(item)
        if (ch != null && ch.collect == null) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(Palette.Blue).border(2.dp, Color.White, CircleShape)
                .semantics { contentDescription = "Item info" }.clickable { vm.pushDialog(Dialog.ChainInfo(ch.id)) },
                contentAlignment = Alignment.Center) { OutlinedText("i", 16, outline = Palette.BlueDark) }
        }
        if (ch?.event != null && ch.event == state.eventId) {
            CandyButton("Turn in", small = true, color = Palette.Purple, dark = Palette.PurpleDark) { vm.turnInSelected() }
        } else if (Content.sellPrice(item) > 0) {
            CandyButton("+${Content.sellPrice(item)}", small = true, icon = "coin", color = Palette.Orange, dark = Palette.OrangeDark) { vm.sellSelected() }
        }
        if (ch?.collect == null) CandyButton("Store", small = true, color = Palette.Purple, dark = Palette.PurpleDark) { vm.storeSelected() }
    }
}

// ------------------------------------------------------------------ bottom bar

@Composable
private fun BottomBar(vm: GameViewModel, state: GameState) {
    val today = vm.today()
    val ev = Events.active(today)
    val passDot = (0 until Engine.passTier(state)).any { it !in state.passClaimedFree || (Engine.vip(state, today) && it !in state.passClaimedVip) }
    val eventDot = ev != null && ev.milestones.withIndex().any { (i, m) -> state.eventPts >= m.first && i !in state.eventClaimed }
    val bakeryDot = Story.nextTaskAffordable(state, today)
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        NavButton("house", "Map", dot = bakeryDot) { vm.go(Screen.BAKERY) }
        NavButton("shopping_bags", "Shop", dot = state.freeTreatDay != today.toString()) { vm.go(Screen.SHOP) }
        NavButton("ticket", "Pass", dot = passDot) { vm.go(Screen.PASS) }
        if (ev != null) NavButton(ev.icon, "Event", dot = eventDot) { vm.go(Screen.EVENT) }
        NavButton("calendar", "Goals", dot = Engine.goalsReady(state, today)) { vm.go(Screen.GOALS) }
        NavButton("gear", "Settings") { vm.go(Screen.SETTINGS) }
    }
}

private object Story {
    fun nextTaskAffordable(s: GameState, today: java.time.LocalDate): Boolean {
        val t = com.surfswirl.bakery.game.Story.nextTask(s.tasksDone, today) ?: return false
        return s.coins >= t.cost
    }
}


/** Board tile colours (light, dark, frame) for each chapter's place. */
private fun boardPalette(chapter: Int, colors: com.surfswirl.bakery.ui.theme.GameColors) = when (chapter) {
    1 -> Triple(Color(0xFFF1FBE9), Color(0xFFE0F2D3), Color(0xFF6FA35E))      // patio: garden greens
    2 -> Triple(Color(0xFFF3F9FE), Color(0xFFE0EDF8), Color(0xFF5D8FB5))      // kitchen: blue and white tiles
    else -> Triple(colors.tileA, colors.tileB, colors.frame)                  // bakery: the theme's colours
}

/** How an obstacle looks: themed tile, a peek of the reward, and what it needs. */
@Composable
private fun ObstacleTile(c: Cell, cell: Dp) {
    val (top, bottom, icon) = when (c.cover) {
        "web" -> Triple(Color(0xFFE9E4EE), Color(0xFFCFC6D8), "")
        "sack" -> Triple(Color(0xFFF1DDB5), Color(0xFFD9B987), "")
        "leaves" -> Triple(Color(0xFFFFD9A8), Color(0xFFF0A868), "fallen_leaf")
        "drift" -> Triple(Color(0xFFFFFFFF), Color(0xFFDDEEFF), "snowflake")
        else -> Triple(Color(0xFFD9A066), Color(0xFFB07A45), "")
    }
    Box(Modifier.fillMaxSize().padding(2.dp).clip(RoundedCornerShape(cell * 0.22f))
        .background(Brush.verticalGradient(listOf(top, bottom)))
        .border(2.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(cell * 0.22f))) {
        when (c.cover) {
            "crate" -> {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width
                    drawLine(Color(0xFF8A5A34), Offset(w * 0.12f, w * 0.12f), Offset(w * 0.88f, w * 0.88f), w * 0.07f)
                    drawLine(Color(0xFF8A5A34), Offset(w * 0.88f, w * 0.12f), Offset(w * 0.12f, w * 0.88f), w * 0.07f)
                }
                Box(Modifier.align(Alignment.Center).size(cell * 0.58f).clip(CircleShape).background(Color.White)
                    .border(2.dp, Palette.Orange, CircleShape), contentAlignment = Alignment.Center) {
                    ItemArt(Content.art(c.need), Modifier.size(cell * 0.44f))
                }
            }
            "web" -> {
                ItemArt(Content.art(c.item!!), Modifier.fillMaxSize().padding(cell * 0.2f), alpha = 0.4f, gray = true)
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width; val col = Color.White
                    for (a in 0 until 6) {
                        val ang = a * Math.PI / 3
                        drawLine(col, Offset(w / 2, w / 2), Offset(w / 2 + (w * 0.55f * kotlin.math.cos(ang)).toFloat(), w / 2 + (w * 0.55f * kotlin.math.sin(ang)).toFloat()), 2.5f)
                    }
                    for (r in listOf(0.15f, 0.28f, 0.4f)) drawCircle(col, w * r, Offset(w / 2, w / 2), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                }
            }
            "sack" -> {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width
                    drawRoundRect(Color(0xFFFFF6E6), Offset(w * 0.2f, w * 0.28f), Size(w * 0.6f, w * 0.6f), CornerRadius(w * 0.12f))
                    drawRect(Color(0xFFC98B62), Offset(w * 0.3f, w * 0.2f), Size(w * 0.4f, w * 0.1f))
                }
                Text("FLOUR", Modifier.align(Alignment.Center).padding(top = cell * 0.18f), fontSize = (cell.value * 0.15f).sp,
                    color = Palette.CocoaSoft, style = MaterialTheme.typography.labelSmall)
            }
            else -> {
                ItemArt(Content.art(c.item!!), Modifier.fillMaxSize().padding(cell * 0.22f), alpha = 0.35f, gray = true)
                if (icon.isNotEmpty()) for ((dx, dy) in listOf(0.1f to 0.1f, 0.55f to 0.2f, 0.3f to 0.55f))
                    ItemArt(icon, Modifier.offset(cell * dx, cell * dy).size(cell * 0.38f))
            }
        }
        if (c.hp > 1) Badge("${c.hp}", Modifier.align(Alignment.TopEnd), color = Palette.CocoaSoft)
    }
}
