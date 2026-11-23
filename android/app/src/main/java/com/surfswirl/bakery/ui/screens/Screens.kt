package com.surfswirl.bakery.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.surfswirl.bakery.game.Economy
import com.surfswirl.bakery.game.Engine
import com.surfswirl.bakery.game.Events
import com.surfswirl.bakery.game.GameState
import com.surfswirl.bakery.game.Offer
import com.surfswirl.bakery.game.Reward
import com.surfswirl.bakery.game.Story
import com.surfswirl.bakery.ui.Dialog
import com.surfswirl.bakery.ui.GameViewModel
import com.surfswirl.bakery.ui.Screen
import com.surfswirl.bakery.ui.components.Badge
import com.surfswirl.bakery.ui.components.CandyButton
import com.surfswirl.bakery.ui.components.CurrencyPill
import com.surfswirl.bakery.ui.components.ItemArt
import com.surfswirl.bakery.ui.components.OutlinedText
import com.surfswirl.bakery.ui.components.Panel
import com.surfswirl.bakery.ui.components.ProgressBar
import com.surfswirl.bakery.ui.components.RewardRow
import com.surfswirl.bakery.ui.components.bob
import com.surfswirl.bakery.ui.components.darken
import com.surfswirl.bakery.ui.components.pulse
import com.surfswirl.bakery.ui.theme.LocalGameColors
import com.surfswirl.bakery.ui.theme.Palette
import java.time.temporal.ChronoUnit

/** Shared header for the secondary screens: back arrow, title, currencies. */
@Composable
fun ScreenHeader(vm: GameViewModel, state: GameState, title: String, back: Screen = Screen.BOARD) {
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White).border(2.dp, LocalGameColors.current.accent, CircleShape)
            .semantics { contentDescription = "Back" }.clickable { vm.go(back) }, contentAlignment = Alignment.Center) {
            OutlinedText("‹", 26, color = LocalGameColors.current.accent, outline = Color.White)
        }
        Spacer(Modifier.width(8.dp))
        OutlinedText(title, 24, modifier = Modifier.weight(1f), align = TextAlign.Start, outline = Palette.Cocoa)
        CurrencyPill("coin", "${state.coins}", Palette.Coin, Modifier.width(92.dp))
        CurrencyPill("gem_stone", "${state.rubies}", Palette.Ruby, Modifier.width(80.dp))
    }
}

// =================================================================== Shop

@Composable
fun ShopScreen(vm: GameViewModel, state: GameState) {
    val today = vm.today()
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        ScreenHeader(vm, state, "Brain Shop")
        LazyVerticalGrid(GridCells.Fixed(2), contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item(span = { GridItemSpan(2) }) {
                Panel(Modifier.fillMaxWidth(), color = Color(0xFFFFF3C4)) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        ItemArt("brain", Modifier.size(46.dp).bob(3f))
                        Spacer(Modifier.width(8.dp))
                        Text("No money here! Everything costs brain power. Bigger rewards ask for harder puzzles.",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item(span = { GridItemSpan(2) }) {
                val claimed = state.freeTreatDay == today.toString()
                Panel(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        ItemArt("bone", Modifier.size(52.dp).pulse(!claimed))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Surf's Daily Treat", style = MaterialTheme.typography.titleMedium)
                            RewardRow(Economy.freeTreat, size = 26.dp)
                        }
                        CandyButton(if (claimed) "Tomorrow" else "Free!", Modifier.width(118.dp), enabled = !claimed, color = Palette.Orange,
                            dark = Palette.OrangeDark) { vm.claimFreeTreat() }
                    }
                }
            }
            item(span = { GridItemSpan(2) }) { VipBanner(vm, state) }
            items(Economy.offers, key = { it.id }) { o -> OfferCard(vm, state, o) }
            item(span = { GridItemSpan(2) }) {
                Text("Stock refills every day. The puzzle level adjusts to how hard each puzzle type is for you.",
                    style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun VipBanner(vm: GameViewModel, state: GameState) {
    val active = Engine.vip(state, vm.today())
    Panel(Modifier.fillMaxWidth().clickable { vm.pushDialog(Dialog.Vip) },
        color = Color(0xFFFFE9A6)) {
        Box(Modifier.background(Brush.horizontalGradient(listOf(Color(0xFFFFE07A), Color(0xFFFFB86B))))) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ItemArt("ticket", Modifier.size(56.dp).rotate(-12f).pulse(!active))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    OutlinedText("Golden Whisk VIP", 20, outline = Palette.OrangeDark, align = TextAlign.Start)
                    Text(if (active) "Active this month! Enjoy the perks." else "More energy, a 4th order, golden pass rewards. Unlock with a 3-puzzle Brain Gauntlet!",
                        style = MaterialTheme.typography.bodySmall, color = Palette.Cocoa)
                }
                if (!active) Badge("3 🧠", color = Palette.PurpleDark)
            }
        }
    }
}

@Composable
private fun OfferCard(vm: GameViewModel, state: GameState, o: Offer) {
    val left = Engine.stockLeft(state, o)
    val effortLabel = when { o.effort < 3 -> "Easy"; o.effort < 5 -> "Medium"; o.effort < 7 -> "Hard"; else -> "Expert" }
    val color = when { o.effort < 3 -> Palette.Green; o.effort < 5 -> Palette.Blue; o.effort < 7 -> Palette.Orange; else -> Palette.Ruby }
    Panel(Modifier.fillMaxWidth().semantics { contentDescription = o.title }) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth()) {
                if (o.badge.isNotEmpty()) Badge(o.badge, Modifier.align(Alignment.TopStart), color = Palette.Pink)
                Badge("$left left", Modifier.align(Alignment.TopEnd), color = if (left > 0) Palette.CocoaSoft else Palette.Gray)
            }
            ItemArt(o.art, Modifier.size(64.dp).bob(3f, 1500 + o.id.length * 50))
            Text(o.title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            RewardRow(o.reward, size = 28.dp)
            Spacer(Modifier.height(6.dp))
            CandyButton(effortLabel, Modifier.fillMaxWidth(), color = color, dark = color.darken(0.25f),
                icon = "brain", enabled = left > 0, small = true) { vm.buy(o) }
        }
    }
}

// =================================================================== Pass

@Composable
fun PassScreen(vm: GameViewModel, state: GameState) {
    val today = vm.today()
    val vip = Engine.vip(state, today)
    val tier = Engine.passTier(state)
    val daysLeft = ChronoUnit.DAYS.between(today, today.withDayOfMonth(today.lengthOfMonth())) + 1
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        ScreenHeader(vm, state, "Season Pass")
        Panel(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            Column(Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${today.month.name.lowercase().replaceFirstChar { it.uppercase() }} Season", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    Badge("$daysLeft days left", color = Palette.CocoaSoft)
                }
                Text("Every order you serve earns pass points.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                val inTier = state.passPts % Economy.PASS_PTS_PER_TIER
                ProgressBar(if (tier >= Economy.PASS_TIERS) 1f else inTier.toFloat() / Economy.PASS_PTS_PER_TIER, Modifier.fillMaxWidth(), Palette.Gold, 18.dp,
                    if (tier >= Economy.PASS_TIERS) "Complete!" else "Tier ${tier + 1}: $inTier / ${Economy.PASS_PTS_PER_TIER}")
                if (!vip) {
                    Spacer(Modifier.height(6.dp))
                    CandyButton("Unlock golden track", Modifier.fillMaxWidth(), icon = "ticket", color = Palette.Gold, dark = Palette.GoldDark) { vm.pushDialog(Dialog.Vip) }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
            Text("Free", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.width(48.dp))
            Text("Golden VIP", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, color = Palette.GoldDark)
        }
        LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items((0 until Economy.PASS_TIERS).toList()) { t ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PassCell(Economy.passFree(t), reached = t < tier, claimed = t in state.passClaimedFree, locked = false,
                        Modifier.weight(1f)) { vm.claimPass(t, false) }
                    Box(Modifier.width(48.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(if (t < tier) Palette.Gold else Palette.Gray)
                            .border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) { OutlinedText("${t + 1}", 13) }
                    }
                    PassCell(Economy.passVip(t), reached = t < tier, claimed = t in state.passClaimedVip, locked = !vip,
                        Modifier.weight(1f), gold = true) { vm.claimPass(t, true) }
                }
            }
        }
    }
}

@Composable
private fun PassCell(r: Reward, reached: Boolean, claimed: Boolean, locked: Boolean, modifier: Modifier, gold: Boolean = false, onClaim: () -> Unit) {
    val can = reached && !claimed && !locked
    Panel(modifier.height(78.dp).clickable(enabled = can) { onClaim() }, color = if (gold) Color(0xFFFFF0C2) else Palette.Cream) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            RewardRow(r, Modifier.graphicsLayer { alpha = if (claimed) 0.35f else 1f }.then(if (can) Modifier.pulse() else Modifier), size = 34.dp)
            if (claimed) OutlinedText("✓", 28, color = Palette.Green, outline = Color.White)
            if (locked) ItemArt("locked", Modifier.size(26.dp).align(Alignment.TopEnd).padding(2.dp))
            if (can) Badge("Claim!", Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp), color = Palette.Green)
        }
    }
}

// =================================================================== Event

@Composable
fun EventScreen(vm: GameViewModel, state: GameState) {
    val today = vm.today()
    val ev = Events.active(today)
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        ScreenHeader(vm, state, ev?.name ?: "Events")
        if (ev == null) {
            Text("No event right now. The next one is coming soon!", Modifier.padding(24.dp), style = MaterialTheme.typography.bodyLarge)
            return
        }
        val daysLeft = ChronoUnit.DAYS.between(today, ev.endDate(today)) + 1
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Panel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            com.surfswirl.bakery.game.Content.chain(ev.chain)!!.items.forEachIndexed { i, it ->
                                ItemArt(it.second, Modifier.size(44.dp).bob(3f, 1200 + i * 150))
                            }
                        }
                        Text(ev.blurb, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(4.dp))
                        Badge("Ends in $daysLeft days", color = Palette.CocoaSoft)
                        Spacer(Modifier.height(6.dp))
                        Text("Tap the ${ev.name} Basket on your board for event items. Merge them, then select one and tap Turn in.",
                            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(6.dp))
                        Text("${state.eventPts} points", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
            if (ev.scene != null) item {
                Panel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text(ev.place, style = MaterialTheme.typography.titleLarge)
                        Text("Every milestone you claim decorates a little more of the cottage.", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(6.dp))
                        val next = ev.milestones.indices.firstOrNull { it !in state.eventClaimed }
                        SceneView(ev.scene, state.tasksDone + state.eventDecor, next?.let { ev.decorId(it) },
                            onTapLayer = { vm.sceneTap(it) }, onExpand = { vm.pushDialog(Dialog.Visit(ev.scene)) })
                    }
                }
            }
            itemsIndexed(ev.milestones) { i, (need, r) ->
                val reached = state.eventPts >= need
                val claimed = i in state.eventClaimed
                Panel(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(if (reached) LocalGameColors.current.accent else Palette.Gray),
                            contentAlignment = Alignment.Center) { OutlinedText("$need", 13) }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            ev.decor.getOrNull(i)?.let { Text(it.first, style = MaterialTheme.typography.titleSmall) }
                            RewardRow(r, size = 30.dp)
                        }
                        when {
                            claimed -> OutlinedText("✓", 26, color = Palette.Green, outline = Color.White)
                            reached -> CandyButton("Claim", Modifier.width(96.dp).pulse(), small = true) { vm.claimEvent(i) }
                            else -> ProgressBar(state.eventPts.toFloat() / need, Modifier.width(96.dp), LocalGameColors.current.accent, 14.dp)
                        }
                    }
                }
            }
        }
    }
}
