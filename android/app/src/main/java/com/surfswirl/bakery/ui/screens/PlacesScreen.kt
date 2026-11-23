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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

/** Which story places show up in the row at the top: unlocked chapters, the next locked one, and event places. */
fun visiblePlaces(state: GameState, vm: GameViewModel): List<Pair<Story.Place, Boolean>> {
    val chapter = Story.chapterIndex(state.tasksDone)
    val active = Events.active(vm.today())?.id
    return Story.places.mapNotNull { p ->
        when {
            p.event != null -> {
                val played = state.eventDecor.any { it.startsWith(p.event + "_") }
                if (p.event == active || played) p to true else null
            }
            p.chapter <= chapter -> p to true
            p.chapter == chapter + 1 -> p to false   // shown locked, as a teaser
            else -> null
        }
    }
}

/**
 * The story: a row of places along Maple Lane. The current chapter's place opens by default; any place
 * you've already fixed up can be visited, explored full screen, and tapped to relive its moments.
 */
@Composable
fun BakeryScreen(vm: GameViewModel, state: GameState) {
    val chIdx = Story.chapterIndex(state.tasksDone)
    val places = visiblePlaces(state, vm)
    val defaultPlace = Story.places.firstOrNull { it.chapter == chIdx.coerceAtMost(Story.chapters.size - 1) }?.id ?: "bakery"
    var selected by rememberSaveable { mutableStateOf(defaultPlace) }
    val place = places.firstOrNull { it.first.id == selected && it.second }?.first ?: Story.places.first { it.id == defaultPlace }
    val done = state.tasksDone + state.eventDecor

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        ScreenHeader(vm, state, "Maple Lane")
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(places, key = { it.first.id }) { (p, open) ->
                PlaceChip(p, selected = p.id == place.id, open = open) {
                    if (open) selected = p.id else vm.toast("Finish Chapter ${p.chapter} to open the ${p.name}!")
                }
            }
        }
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (place.event != null) eventPlace(vm, state, place, done) else chapterPlace(vm, state, place, done, chIdx)
        }
    }
}

@Composable
private fun PlaceChip(p: Story.Place, selected: Boolean, open: Boolean, onClick: () -> Unit) {
    Column(Modifier.width(76.dp).semantics { contentDescription = "Place ${p.name}" }.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(60.dp).clip(RoundedCornerShape(18.dp))
            .background(if (selected) Color(0xFFFFE08A) else Color.White)
            .border(if (selected) 3.dp else 2.dp, if (selected) Palette.Orange else Color.White, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center) {
            ItemArt(p.icon, Modifier.size(40.dp).graphicsLayer { alpha = if (open) 1f else 0.35f }, gray = !open)
            if (!open) ItemArt("locked", Modifier.size(24.dp).align(Alignment.BottomEnd))
        }
        Text(p.name, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.chapterPlace(vm: GameViewModel, state: GameState, place: Story.Place, done: Set<String>, chIdx: Int) {
    val chapter = Story.chapters[place.chapter]
    val count = chapter.tasks.count { it.id in state.tasksDone }
    val current = place.chapter == chIdx
    val next = if (current) Story.nextTask(state.tasksDone) else null
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
