package com.surfswirl.bakery.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.surfswirl.bakery.game.Content
import com.surfswirl.bakery.game.Economy
import com.surfswirl.bakery.game.Engine
import com.surfswirl.bakery.game.GameState
import com.surfswirl.bakery.game.Line
import com.surfswirl.bakery.ui.Dialog
import com.surfswirl.bakery.ui.GameViewModel
import com.surfswirl.bakery.ui.Ui
import com.surfswirl.bakery.ui.components.Badge
import com.surfswirl.bakery.ui.components.CandyButton
import com.surfswirl.bakery.ui.components.ItemArt
import com.surfswirl.bakery.ui.components.OutlinedText
import com.surfswirl.bakery.ui.components.Panel
import com.surfswirl.bakery.ui.components.Particles
import com.surfswirl.bakery.ui.components.RewardRow
import com.surfswirl.bakery.ui.components.Sunburst
import com.surfswirl.bakery.ui.components.bob
import com.surfswirl.bakery.ui.components.pulse
import com.surfswirl.bakery.ui.components.wiggle
import com.surfswirl.bakery.ui.theme.Palette

@Composable
fun DialogHost(vm: GameViewModel, state: GameState, ui: Ui, particles: Particles) {
    val d = ui.dialogs.firstOrNull() ?: return
    // Scrim
    Box(Modifier.fillMaxSize().background(Color(0x99301A10))
        .clickable(remember { MutableInteractionSource() }, null) { if (d is Dialog.Talk) {} else if (dismissable(d)) vm.closeDialog() })
    AnimatedContent(d, label = "dialog", transitionSpec = { scaleIn(spring(dampingRatio = 0.55f, stiffness = 400f), 0.7f) + fadeIn() togetherWith fadeOut() }) { dlg ->
        Box(Modifier.fillMaxSize().navigationBarsPadding(), contentAlignment = if (dlg is Dialog.Talk) Alignment.BottomCenter else Alignment.Center) {
            when (dlg) {
                is Dialog.Talk -> Talk(dlg.lines, dlg.title) { vm.closeDialog() }
                is Dialog.Rewarded -> RewardDialog(dlg.title, dlg.lucky, { RewardRow(dlg.reward, size = 56.dp) }) { vm.closeDialog() }
                is Dialog.LevelUp -> RewardDialog("Level ${dlg.level}!", false, {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        RewardRow(dlg.reward, size = 56.dp)
                        if (dlg.unlocked.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("New: ${dlg.unlocked.joinToString()}! It's waiting in your gift bubble.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                        }
                    }
                }) { vm.closeDialog() }
                Dialog.Daily -> DailyDialog(vm, state)
                Dialog.OutOfEnergy -> EnergyDialog(vm, state)
                is Dialog.Recharge -> RechargeDialog(vm, state, dlg.genId)
                is Dialog.ChainInfo -> ChainDialog(vm, state, dlg.chainId)
                Dialog.PickPuzzle -> PickPuzzleDialog(vm, ui)
                is Dialog.PuzzleFailed -> Card(vm, "So close!") {
                    ItemArt("surf_face", Modifier.size(80.dp).bob())
                    Text("That one was tricky. Surf believes in you! Want to try a different puzzle?", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    CandyButton("Try another", Modifier.fillMaxWidth()) { vm.retryPuzzle() }
                    Spacer(Modifier.height(6.dp))
                    CandyButton("Maybe later", Modifier.fillMaxWidth(), color = Palette.Gray, dark = Palette.GrayDark) { vm.leaveFailedPuzzle() }
                }
                Dialog.Vip -> VipDialog(vm, state)
                is Dialog.Discovery -> RewardDialog("New discovery!", false, {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ItemArt(Content.art(dlg.item), Modifier.size(110.dp).bob())
                        OutlinedText(Content.name(dlg.item), 22)
                        Text("+1 ruby for discovering it", style = MaterialTheme.typography.bodyMedium)
                    }
                }) { vm.closeDialog() }
                Dialog.Storage -> StorageDialog(vm, state)
            }
        }
    }
}

private fun dismissable(d: Dialog) = d !is Dialog.PickPuzzle && d !is Dialog.PuzzleFailed && d !is Dialog.Rewarded && d !is Dialog.LevelUp

@Composable
fun Card(vm: GameViewModel, title: String, closable: Boolean = true, content: @Composable () -> Unit) {
    Box(Modifier.padding(20.dp).widthIn(max = 420.dp)) {
        Panel(Modifier.fillMaxWidth().padding(top = 22.dp)) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 30.dp, bottom = 16.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally) { content() }
        }
        Box(Modifier.align(Alignment.TopCenter).clip(RoundedCornerShape(50)).background(Palette.Pink)
            .border(3.dp, Color.White, RoundedCornerShape(50)).padding(horizontal = 22.dp, vertical = 6.dp)) {
            OutlinedText(title, 20, outline = Palette.PinkDark)
        }
        if (closable) Box(Modifier.align(Alignment.TopEnd).padding(top = 14.dp).size(36.dp).clip(CircleShape).background(Palette.Ruby)
            .border(2.dp, Color.White, CircleShape).semantics { contentDescription = "Close" }.clickable { vm.closeDialog() },
            contentAlignment = Alignment.Center) { OutlinedText("✕", 16, outline = Palette.PinkDark) }
    }
}

@Composable
private fun Talk(lines: List<Line>, title: String, onDone: () -> Unit) {
    var i by remember(lines) { mutableIntStateOf(0) }
    val line = lines.getOrNull(i) ?: return
    val who = Content.character(line.who)
    val pop = remember(i) { Animatable(0.85f) }
    LaunchedEffect(i) { pop.animateTo(1f, spring(dampingRatio = 0.4f)) }
    Column(Modifier.fillMaxWidth().semantics { contentDescription = "Dialogue" }
        .clickable(remember { MutableInteractionSource() }, null) { if (i < lines.size - 1) i++ else onDone() }
        .padding(12.dp)) {
        if (title.isNotEmpty() && i == 0) OutlinedText(title, 22, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            ItemArt(who.art, Modifier.size(110.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }.bob(3f))
            Spacer(Modifier.width(4.dp))
            Box(Modifier.clip(RoundedCornerShape(50)).background(Palette.Pink).border(2.dp, Color.White, RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 4.dp)) { OutlinedText(who.name, 16, outline = Palette.PinkDark) }
        }
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(line.text, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(6.dp))
                Text(if (i < lines.size - 1) "Tap to continue ▸" else "Tap to close", Modifier.align(Alignment.End),
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun RewardDialog(title: String, lucky: Boolean, content: @Composable () -> Unit, onCollect: () -> Unit) {
    Box(contentAlignment = Alignment.Center) {
        Sunburst(Modifier.size(460.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            OutlinedText(title, 32, outline = Palette.OrangeDark)
            if (lucky) {
                Spacer(Modifier.height(4.dp))
                Box(Modifier.clip(RoundedCornerShape(50)).background(Palette.Purple).border(2.dp, Color.White, RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 4.dp).wiggle(true, 3f)) { OutlinedText("✨ Lucky bonus! Surf found extra ✨", 15, outline = Palette.PurpleDark) }
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.bob(4f)) { content() }
            Spacer(Modifier.height(24.dp))
            CandyButton("Collect", Modifier.width(200.dp).pulse(), color = Palette.Green, dark = Palette.GreenDark) { onCollect() }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DailyDialog(vm: GameViewModel, state: GameState) {
    val today = vm.today()
    val avail = Engine.dailyAvailable(state, today)
    val idx = Engine.dailyIndex(state)
    Card(vm, "Daily Rewards") {
        Text(if (avail) "Welcome back! Here's today's treat." else "Come back tomorrow for the next reward!", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Economy.daily.forEachIndexed { i, r ->
                val past = i < idx
                val current = i == idx && avail
                Box(Modifier.width(if (i == 6) 150.dp else 72.dp).height(92.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (current) Color(0xFFFFE08A) else if (past) Color(0xFFE6F8E0) else Color(0xFFFFF1E2))
                    .border(if (current) 3.dp else 1.5.dp, if (current) Palette.Orange else Color.White, RoundedCornerShape(16.dp))
                    .then(if (current) Modifier.pulse(amount = 0.04f) else Modifier), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Day ${i + 1}", style = MaterialTheme.typography.labelMedium)
                        RewardRow(r, size = 30.dp)
                    }
                    if (past) OutlinedText("✓", 30, color = Palette.Green, outline = Color.White)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        CandyButton(if (avail) "Claim" else "OK", Modifier.fillMaxWidth()) { if (avail) vm.claimDaily() else vm.closeDialog() }
    }
}

@Composable
private fun EnergyDialog(vm: GameViewModel, state: GameState) {
    val now by vm.clock.collectAsStateWithLifecycle()
    val max = Engine.maxEnergy(state, vm.today())
    Card(vm, "Energy") {
        ItemArt("high_voltage", Modifier.size(90.dp).bob())
        Text(if (state.energy <= 0) "You're out of energy!" else "Energy: ${state.energy} / $max", style = MaterialTheme.typography.titleLarge)
        if (state.energy < max) Text("+1 every ${Engine.regenMs(state, vm.today()) / 1000} seconds · next in ${mmss(Engine.regenMs(state, vm.today()) - (now - state.energyAt))}",
            style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(10.dp))
        Text("Give your brain a quick workout for a top-up:", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        CandyButton("Quick puzzle: +${Economy.energyRescue.reward.energy}", Modifier.fillMaxWidth(), icon = "brain") { vm.energyRescue() }
        Spacer(Modifier.height(6.dp))
        CandyButton("More in the Shop", Modifier.fillMaxWidth(), color = Palette.Blue, dark = Palette.BlueDark) {
            vm.closeDialog(); vm.go(com.surfswirl.bakery.ui.Screen.SHOP)
        }
    }
}

@Composable
private fun RechargeDialog(vm: GameViewModel, state: GameState, gid: String) {
    val now by vm.clock.collectAsStateWithLifecycle()
    val def = Content.generator(gid) ?: return
    val g = state.generators[gid]
    if (g == null || g.charges > 0) { LaunchedEffect(Unit) { vm.closeDialog() }; return }
    val left = g.cooldownUntil - now
    val cost = Economy.skipCost(left)
    Card(vm, def.name) {
        ItemArt(def.art, Modifier.size(90.dp).bob())
        Text("Recharging…", style = MaterialTheme.typography.titleLarge)
        OutlinedText(mmss(left), 30, outline = Palette.Cocoa)
        Text("Ready in a little while, or speed it up:", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        CandyButton("Brain puzzle", Modifier.fillMaxWidth(), icon = "brain") { vm.rechargeWithPuzzle(gid) }
        Spacer(Modifier.height(6.dp))
        CandyButton("Recharge for $cost", Modifier.fillMaxWidth(), icon = "gem_stone", color = Palette.Pink, dark = Palette.PinkDark,
            enabled = state.rubies >= cost) { vm.skipRecharge(gid) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChainDialog(vm: GameViewModel, state: GameState, chainId: String) {
    val ch = Content.chain(chainId) ?: return
    Card(vm, ch.name) {
        Text("Merge two of the same to make the next one.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ch.items.forEachIndexed { i, (name, art) ->
                val id = "${ch.id}:${i + 1}"
                val known = id in state.discovered
                Column(Modifier.width(76.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(Color(0x22FF9F2E)), contentAlignment = Alignment.Center) {
                        ItemArt(art, Modifier.size(54.dp), silhouette = !known)
                        Badge("${i + 1}", Modifier.align(Alignment.TopStart), color = Palette.CocoaSoft)
                    }
                    Text(if (known) name else "???", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2)
                }
            }
        }
    }
}

@Composable
private fun PickPuzzleDialog(vm: GameViewModel, ui: Ui) {
    val p = ui.puzzle ?: return
    val title = when (val pp = p.purpose) {
        is com.surfswirl.bakery.ui.Purpose.Buy -> pp.offer.title
        is com.surfswirl.bakery.ui.Purpose.Recharge -> "Recharge"
    }
    Box(Modifier.padding(20.dp).widthIn(max = 420.dp)) {
        Panel(Modifier.fillMaxWidth().padding(top = 22.dp)) {
            Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 30.dp, bottom = 14.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally) {
                if (p.total > 1) Text("Brain Gauntlet: puzzle ${p.solved + 1} of ${p.total} (each from a different category)",
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Text("Pick a puzzle", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                for (t in vm.puzzleChoices()) {
                    val lv = vm.puzzleLevel(t)
                    Panel(Modifier.fillMaxWidth().padding(vertical = 4.dp).semantics { contentDescription = t.title }.clickable { vm.pickPuzzle(t) },
                        color = Color.White) {
                        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            ItemArt(t.art, Modifier.size(44.dp)); Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(t.title, style = MaterialTheme.typography.titleMedium)
                                Text(t.blurb, style = MaterialTheme.typography.bodySmall)
                            }
                            Badge("Lv $lv", color = when { lv <= 3 -> Palette.GreenDark; lv <= 6 -> Palette.BlueDark; else -> Palette.PurpleDark })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CandyButton("Surprise me", Modifier.weight(1f), small = true, color = Palette.Purple, dark = Palette.PurpleDark, icon = "game_die") { vm.pickPuzzle(null) }
                    CandyButton("Not now", Modifier.weight(1f), small = true, color = Palette.Gray, dark = Palette.GrayDark) { vm.cancelPuzzleChoice() }
                }
            }
        }
        Box(Modifier.align(Alignment.TopCenter).clip(RoundedCornerShape(50)).background(Palette.Purple)
            .border(3.dp, Color.White, RoundedCornerShape(50)).padding(horizontal = 22.dp, vertical = 6.dp)) {
            OutlinedText(title, 20, outline = Palette.PurpleDark)
        }
    }
}

@Composable
private fun VipDialog(vm: GameViewModel, state: GameState) {
    val active = Engine.vip(state, vm.today())
    Card(vm, "Golden Whisk VIP") {
        ItemArt("ticket", Modifier.size(96.dp).wiggle(true, 4f))
        Text(if (active) "You're VIP for the rest of this month!" else "Become VIP for the rest of this month:", style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        for (perk in Economy.vipPerks) Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            ItemArt("glowing_star", Modifier.size(22.dp)); Spacer(Modifier.width(6.dp))
            Text(perk, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(10.dp))
        if (!active) {
            Text("Price: the Brain Gauntlet. Three puzzles, each from a different category.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            CandyButton("Start the Gauntlet", Modifier.fillMaxWidth().pulse(), icon = "brain", color = Palette.Gold, dark = Palette.GoldDark) {
                vm.closeDialog(); vm.buy(Economy.vipOffer)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StorageDialog(vm: GameViewModel, state: GameState) {
    Card(vm, "Pantry Shelf") {
        Text("Tap an item to put it back on the board. To store something, select it on the board and tap Store.",
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.storage.forEachIndexed { i, item ->
                Box(Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFFFE9D2)).border(2.dp, Color.White, RoundedCornerShape(16.dp))
                    .clickable(enabled = item != null) { vm.unstore(i) }, contentAlignment = Alignment.Center) {
                    if (item != null) ItemArt(Content.art(item), Modifier.size(52.dp))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        if (state.storage.size < Economy.MAX_STORAGE) {
            val cost = Economy.storageSlotCost(state.storage.size)
            CandyButton("New shelf slot: $cost", Modifier.fillMaxWidth(), icon = "gem_stone", color = Palette.Pink, dark = Palette.PinkDark,
                enabled = state.rubies >= cost, small = true) { vm.buySlot() }
        }
    }
}
