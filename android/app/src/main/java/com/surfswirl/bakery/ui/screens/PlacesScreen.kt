package com.surfswirl.bakery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.surfswirl.bakery.game.Events
import com.surfswirl.bakery.game.GameState
import com.surfswirl.bakery.game.Story
import com.surfswirl.bakery.ui.Dialog
import com.surfswirl.bakery.ui.GameViewModel
import com.surfswirl.bakery.ui.Screen
import com.surfswirl.bakery.ui.components.Badge
import com.surfswirl.bakery.ui.components.CandyButton
import com.surfswirl.bakery.ui.components.ItemArt
import com.surfswirl.bakery.ui.components.Panel
import com.surfswirl.bakery.ui.components.ProgressBar
import com.surfswirl.bakery.ui.components.bob
import com.surfswirl.bakery.ui.components.pulse
import com.surfswirl.bakery.ui.theme.Palette

/** Whether a story place can be visited, and why not. */
fun placeOpen(p: Story.Place, state: GameState, today: java.time.LocalDate): Boolean {
    val chapter = Story.chapterIndex(state.tasksDone)
    return when {
        p.event != null -> Events.active(today)?.id == p.event || state.eventDecor.any { it.startsWith(p.event + "_") }
        else -> p.chapter <= chapter && p.chapter < Story.chapters.size
    }
}

/** Places in this location that can be visited. */
fun openPlaces(loc: Story.Location, state: GameState, today: java.time.LocalDate) =
    loc.places.mapNotNull { id -> Story.places.firstOrNull { it.id == id } }.filter { placeOpen(it, state, today) }

/** Whether a place has something to do right now (affordable task, claimable decoration). */
fun placeHasNews(p: Story.Place, state: GameState, today: java.time.LocalDate): Boolean {
    if (p.event != null) {
        val ev = Events.byId(p.event) ?: return false
        return Events.active(today)?.id == ev.id && ev.milestones.withIndex().any { (i, m) -> state.eventPts >= m.first && i !in state.eventClaimed }
    }
    if (p.chapter != Story.chapterIndex(state.tasksDone)) return false
    val t = Story.nextTask(state.tasksDone, today) ?: return false
    return state.coins >= t.cost
}

object MapData {
    @kotlinx.serialization.Serializable
    data class Map(val w: Int, val h: Int, val spots: kotlin.collections.Map<String, List<Int>>,
                   val reveal: kotlin.collections.Map<String, Int> = emptyMap(), val anims: MapAnims = MapAnims())
    private var map: Map? = null
    fun get(ctx: android.content.Context): Map? = map ?: runCatching {
        kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString(Map.serializer(),
            ctx.assets.open("scenes/map.json").bufferedReader().readText())
    }.getOrNull().also { map = it }
}

/**
 * Maple Lane: a map of every story location. Open locations have a pin; tap one for its stories. Places that
 * aren't open yet wait under clouds with a hint of when they're coming. New locations just add a pin.
 */
@Composable
fun BakeryScreen(vm: GameViewModel, state: GameState) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val today = vm.today()
    val map = androidx.compose.runtime.remember { MapData.get(ctx) }
    val img = androidx.compose.runtime.remember { Scenes.image(ctx, "map.webp") }
    val scroll = androidx.compose.foundation.rememberScrollState()
    val current = Story.places.firstOrNull { it.chapter == Story.chapterIndex(state.tasksDone).coerceAtMost(Story.chapters.size - 1) }
        ?.let { Story.locationOf(it.id) }?.id ?: "bakery"
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        ScreenHeader(vm, state, "Maple Lane")
        if (map == null || img == null) return
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
            val k = constraints.maxWidth / map.w.toFloat()
            val viewport = constraints.maxHeight
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.LaunchedEffect(Unit) {
                val y = (map.spots[current]?.get(1) ?: 0) * k
                scroll.scrollTo((y - viewport / 2f).toInt().coerceAtLeast(0))
            }
            // Past the bottom of the map (tall screens) the cloud bank just carries on.
            Box(Modifier.fillMaxSize().background(Color(0xFFE6EDF2)).verticalScroll(scroll)) {
              Box(Modifier.fillMaxWidth().aspectRatio(map.w / map.h.toFloat()).clipToBounds()) {
                androidx.compose.foundation.Image(img, contentDescription = "Map of Maple Lane",
                    modifier = Modifier.fillMaxWidth().aspectRatio(map.w / map.h.toFloat()))
                MapLife(map.anims, k, Modifier.matchParentSize())
                // Fog of war: one cloud layer over the whole town, with soft clearings cut around open places.
                val openIds = Story.locations.filter { openPlaces(it, state, today).isNotEmpty() }.map { it.id }
                val fog = androidx.compose.runtime.remember(openIds) { fogWithClearings(ctx, map, openIds) }
                val drift = androidx.compose.animation.core.rememberInfiniteTransition(label = "fog")
                val dx by drift.animateFloat(initialValue = -12f, targetValue = 12f, animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    androidx.compose.animation.core.tween(16000), androidx.compose.animation.core.RepeatMode.Reverse), label = "dx")
                if (fog != null) androidx.compose.foundation.Image(fog, contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.FillBounds, alpha = 0.9f,
                    modifier = Modifier.matchParentSize().graphicsLayer { scaleX = 1.05f; scaleY = 1.02f; translationX = dx * k })
                MapBirds(map.w * k, map.h * k, k * 1.25f, Modifier.matchParentSize())
                for (loc in Story.locations) {
                    val spot = map.spots[loc.id] ?: continue
                    val open = openPlaces(loc, state, today)
                    val news = open.any { placeHasNews(it, state, today) }
                    val cx = with(density) { (spot[0] * k).toDp() }
                    val cy = with(density) { (spot[1] * k).toDp() }
                    if (open.isEmpty()) LockedSpot(loc, cx, cy) { vm.toast("${loc.name}: ${loc.teaser}") }
                    else MapPin(loc, cx, cy, news, loc.id == current) { vm.pushDialog(Dialog.Location(loc.id)) }
                }
              }
            }
        }
    }
}

/** The fog image with a soft hole cut around each open location (done once per set of open places). */
private fun fogWithClearings(ctx: android.content.Context, map: MapData.Map, open: List<String>): androidx.compose.ui.graphics.ImageBitmap? {
    val src = Scenes.image(ctx, "fog.webp") ?: return null
    val bmp = src.asAndroidBitmap().copy(android.graphics.Bitmap.Config.ARGB_8888, true)
    val c = android.graphics.Canvas(bmp)
    val f = bmp.width / map.w.toFloat()
    for (id in open) {
        val spot = map.spots[id] ?: continue
        val x = spot[0] * f; val y = spot[1] * f; val r = (map.reveal[id] ?: 250) * f
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.RadialGradient(x, y, r, intArrayOf(0xFF000000.toInt(), 0xFF000000.toInt(), 0x00000000),
                floatArrayOf(0f, 0.5f, 1f), android.graphics.Shader.TileMode.CLAMP)
            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_OUT)
        }
        c.drawCircle(x, y, r, paint)
    }
    return bmp.asImageBitmap()
}

@Composable
private fun MapPin(loc: Story.Location, cx: androidx.compose.ui.unit.Dp, cy: androidx.compose.ui.unit.Dp, news: Boolean, current: Boolean, onClick: () -> Unit) {
    Column(Modifier.offset(cx - 60.dp, cy - 92.dp).width(120.dp).semantics { contentDescription = "Location ${loc.name}" }
        .clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Box(Modifier.size(56.dp).then(if (current) Modifier.bob(4f, 900) else Modifier).clip(androidx.compose.foundation.shape.CircleShape)
                .background(Color.White).border(3.dp, if (current) Palette.Orange else Palette.Pink, androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center) { ItemArt(loc.icon, Modifier.size(36.dp)) }
            if (news) Badge("!", Modifier.align(Alignment.TopEnd), color = Palette.Ruby)
        }
        Box(Modifier.padding(top = 2.dp).clip(RoundedCornerShape(50)).background(Color(0xE65B3220)).padding(horizontal = 8.dp, vertical = 2.dp)) {
            Text(loc.name, style = MaterialTheme.typography.labelSmall, color = Color.White, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

@Composable
private fun LockedSpot(loc: Story.Location, cx: androidx.compose.ui.unit.Dp, cy: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    // A small label floating on the fog: you can't see the place yet, only a hint of what's coming.
    Row(Modifier.offset(cx - 80.dp, cy - 18.dp).width(160.dp).semantics { contentDescription = "Locked ${loc.name}" }
        .clickable(onClick = onClick).clip(RoundedCornerShape(50)).background(Color(0xCCFFFFFF))
        .border(1.5.dp, Color(0x66A0AEC0), RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        ItemArt("locked", Modifier.size(16.dp)); Spacer(Modifier.width(4.dp))
        Text(loc.teaser, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = Palette.CocoaSoft, maxLines = 2)
    }
}

/** A location's card on the map: its stories, each with progress, tap to go there. */
@Composable
fun LocationCard(vm: GameViewModel, state: GameState, id: String) {
    val loc = Story.locations.firstOrNull { it.id == id } ?: return
    val today = vm.today()
    Card(vm, loc.name) {
        if (loc.blurb.isNotEmpty()) Text(loc.blurb, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        for (pid in loc.places) {
            val p = Story.places.firstOrNull { it.id == pid } ?: continue
            val open = placeOpen(p, state, today)
            val (title, progress) = placeSummary(p, state)
            Panel(Modifier.fillMaxWidth().padding(vertical = 4.dp).semantics { contentDescription = "Story ${p.name}" }
                .clickable { if (open) vm.openPlace(p.id) else vm.toast(lockedReason(p)) }, color = Color.White) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    ItemArt(p.icon, Modifier.size(42.dp), gray = !open); Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.name, style = MaterialTheme.typography.titleMedium)
                        Text(if (open) title else lockedReason(p), style = MaterialTheme.typography.bodySmall)
                        if (open) ProgressBar(progress, Modifier.fillMaxWidth().padding(top = 4.dp), if (p.event != null) Palette.Blue else Palette.Pink, 10.dp)
                    }
                    if (open && placeHasNews(p, state, today)) Badge("!", color = Palette.Ruby)
                    if (!open) ItemArt("locked", Modifier.size(24.dp))
                }
            }
        }
    }
}

private fun lockedReason(p: Story.Place) =
    if (p.event != null) "Opens with ${Events.byId(p.event)?.name ?: "its event"}"
    else Story.chapters.getOrNull(p.chapter)?.opens?.let { "Opens ${it.month.name.lowercase().replaceFirstChar { c -> c.uppercase() }} ${it.dayOfMonth}" }
        ?: "Finish Chapter ${p.chapter} to open"

private fun placeSummary(p: Story.Place, state: GameState): Pair<String, Float> {
    if (p.event != null) {
        val ev = Events.byId(p.event) ?: return "" to 0f
        val n = ev.decor.indices.count { ev.decorId(it) in state.eventDecor }
        return "${ev.name} · $n / ${ev.decor.size} decorations" to n / ev.decor.size.toFloat()
    }
    val ch = Story.chapters.getOrNull(p.chapter) ?: return "" to 0f
    val n = ch.tasks.count { it.id in state.tasksDone }
    return "Chapter ${p.chapter + 1}: ${ch.title} · $n / ${ch.tasks.size}" to n / ch.tasks.size.toFloat()
}

/** One story place: its scene, next task or decoration, and finished moments to replay. */
@Composable
fun PlaceScreen(vm: GameViewModel, state: GameState, placeId: String) {
    val place = Story.places.firstOrNull { it.id == placeId } ?: Story.places.first()
    val done = state.tasksDone + state.eventDecor
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        ScreenHeader(vm, state, place.name, back = Screen.BAKERY)
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (place.event != null) eventPlace(vm, state, place, done) else chapterPlace(vm, state, place, done, Story.chapterIndex(state.tasksDone))
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.chapterPlace(vm: GameViewModel, state: GameState, place: Story.Place, done: Set<String>, chIdx: Int) {
    val chapter = Story.chapters[place.chapter]
    val count = chapter.tasks.count { it.id in state.tasksDone }
    val current = place.chapter == chIdx
    val next = if (current) Story.nextTask(state.tasksDone, vm.today()) else null
    item {
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(10.dp)) {
                Text("Chapter ${place.chapter + 1}: ${chapter.title}", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                ProgressBar(count.toFloat() / chapter.tasks.size, Modifier.fillMaxWidth(), Palette.Pink, 18.dp, "$count / ${chapter.tasks.size} tasks")
                Spacer(Modifier.height(8.dp))
                SceneView(place.scene, done, next?.id, onTapLayer = { vm.sceneTap(it) }, onExpand = { vm.pushDialog(Dialog.Visit(place.scene)) })
                Text("Tap anything in the picture to relive its story · ⤢ to explore", style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.Center)
            }
        }
    }
    item {
        when {
            next != null -> Panel(Modifier.fillMaxWidth(), color = Color(0xFFFFF6D6)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    ItemArt(next.art, Modifier.size(64.dp).bob(3f))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Next task", style = MaterialTheme.typography.bodySmall)
                        Text(next.title, style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ItemArt("coin", Modifier.size(20.dp)); Text(" ${next.cost}", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    val can = state.coins >= next.cost
                    CandyButton(if (can) "Do it!" else "Need ${next.cost - state.coins}", Modifier.width(132.dp).pulse(can),
                        enabled = can, color = Palette.Pink, dark = Palette.PinkDark) { vm.doTask(next.id) }
                }
            }
            count == chapter.tasks.size -> Panel(Modifier.fillMaxWidth()) {
                Text(if (place.chapter == Story.chapters.size - 1) "Chapter complete! The next chapter arrives in a future update."
                    else "All fixed up! Come back any time to visit.",
                    Modifier.padding(16.dp).fillMaxWidth(), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            }
            else -> {}
        }
    }
    val finished = chapter.tasks.filter { it.id in state.tasksDone }.reversed()
    if (finished.isNotEmpty()) item { Text("Finished here (tap to replay the story)", style = MaterialTheme.typography.titleSmall) }
    items(finished, key = { it.id }) { t ->
        Panel(Modifier.fillMaxWidth().clickable { vm.pushDialog(Dialog.Talk(t.after, t.title)) }) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ItemArt(t.art, Modifier.size(36.dp)); Spacer(Modifier.width(8.dp))
                Text(t.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Text("✓", color = Palette.Green, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.eventPlace(vm: GameViewModel, state: GameState, place: Story.Place, done: Set<String>) {
    val ev = Events.byId(place.event!!) ?: return
    val active = Events.active(vm.today())?.id == ev.id
    val decorated = ev.decor.indices.count { ev.decorId(it) in state.eventDecor }
    val nextMilestone = if (active) ev.milestones.indices.firstOrNull { it !in state.eventClaimed } else null
    item {
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ev.place, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    Badge(ev.name, color = Palette.BlueDark)
                }
                Spacer(Modifier.height(4.dp))
                ProgressBar(decorated.toFloat() / ev.decor.size, Modifier.fillMaxWidth(), Palette.Blue, 18.dp, "$decorated / ${ev.decor.size} decorations")
                Spacer(Modifier.height(8.dp))
                SceneView(place.scene, done, nextMilestone?.let { ev.decorId(it) }, onTapLayer = { vm.sceneTap(it) },
                    onExpand = { vm.pushDialog(Dialog.Visit(place.scene)) })
            }
        }
    }
    item {
        Panel(Modifier.fillMaxWidth(), color = Color(0xFFEAF5FF)) {
            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (active) {
                    val title = nextMilestone?.let { ev.decor.getOrNull(it)?.first }
                    Text(if (title != null) "Next: $title" else "The cottage is all decorated!", style = MaterialTheme.typography.titleMedium)
                    Text("Earn event points by turning in ${ev.name} items. Each milestone decorates something new.",
                        style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    CandyButton("Go to the event", Modifier.fillMaxWidth(), icon = ev.icon, color = Palette.Blue, dark = Palette.BlueDark) { vm.go(Screen.EVENT) }
                } else {
                    Text("The cottage waits for you until ${ev.name} comes back.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                }
            }
        }
    }
    val unlocked = ev.decor.withIndex().filter { ev.decorId(it.index) in state.eventDecor }.reversed()
    items(unlocked, key = { it.index }) { (i, d) ->
        Panel(Modifier.fillMaxWidth().clickable { vm.pushDialog(Dialog.Talk(d.second, d.first)) }) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Badge("${i + 1}", color = Palette.BlueDark); Spacer(Modifier.width(8.dp))
                Text(d.first, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Text("✓", color = Palette.Green, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
