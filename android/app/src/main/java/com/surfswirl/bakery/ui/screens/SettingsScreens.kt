package com.surfswirl.bakery.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.surfswirl.bakery.game.Events
import com.surfswirl.bakery.game.GameState
import com.surfswirl.bakery.game.Settings
import com.surfswirl.bakery.puzzles.Brain
import com.surfswirl.bakery.ui.GameViewModel
import com.surfswirl.bakery.ui.Screen
import com.surfswirl.bakery.ui.components.CandyButton
import com.surfswirl.bakery.ui.components.ItemArt
import com.surfswirl.bakery.ui.components.OutlinedText
import com.surfswirl.bakery.ui.components.Panel
import com.surfswirl.bakery.ui.components.ProgressBar
import com.surfswirl.bakery.ui.theme.Palette
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(vm: GameViewModel, state: GameState) {
    val ctx = LocalContext.current
    val s = state.settings
    var confirmReset by remember { mutableStateOf(false) }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        vm.updateSettings { it.copy(notifications = ok) }
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching {
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(vm.exportSave().toByteArray()) }
            vm.toast("Backup saved!")
        }.onFailure { vm.toast("Couldn't save the backup.") }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = runCatching { ctx.contentResolver.openInputStream(uri)?.use { String(it.readBytes()) } }.getOrNull()
            if (text == null || !vm.importSave(text)) vm.toast("That file isn't a Surf & Swirl backup.")
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        ScreenHeader(vm, state, "Settings")
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Panel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Sound & feel", style = MaterialTheme.typography.titleMedium)
                        Toggle("Sound effects", "Pops, chimes and the odd woof", s.sound) { v -> vm.updateSettings { it.copy(sound = v) } }
                        Toggle("Vibration", "Little taps when you merge and serve", s.haptics) { v -> vm.updateSettings { it.copy(haptics = v) } }
                        Text("Background music is coming in a future update.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Panel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Popups & reminders", style = MaterialTheme.typography.titleMedium)
                        Toggle("Daily reward popup", "Off: just a red dot on the Daily button", s.dailyPopup) { v -> vm.updateSettings { it.copy(dailyPopup = v) } }
                        Toggle("Gentle nudges", "Event announcements and helpful offers", s.nudges) { v -> vm.updateSettings { it.copy(nudges = v) } }
                        Toggle("Energy full notification", "One reminder when your energy refills", s.notifications) { v ->
                            if (v && Build.VERSION.SDK_INT >= 33) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else vm.updateSettings { it.copy(notifications = v) }
                        }
                    }
                }
            }
            item {
                Panel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Theme", style = MaterialTheme.typography.titleMedium)
                        Text("Auto follows the season's event. Or pick your favorite.", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { ThemeChip("Auto", "sparkles", Brush.verticalGradient(listOf(Color(0xFFFFE6CC), Color(0xFFB9DCFF))), s.theme == "auto") {
                                vm.updateSettings { it.copy(theme = "auto") } } }
                            items(Events.themes) { t ->
                                ThemeChip(t.name, t.icon, Brush.verticalGradient(listOf(Color(t.bgTop), Color(t.bgBottom))), s.theme == t.id) {
                                    vm.updateSettings { it.copy(theme = t.id) }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Panel(Modifier.fillMaxWidth().clickable { vm.go(Screen.STATS) }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ItemArt("bar_chart", Modifier.size(44.dp)); Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Brain stats", style = MaterialTheme.typography.titleMedium)
                            Text("Which puzzles are hardest, and how prices adapt", style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedText("›", 26)
                    }
                }
            }
            item {
                Panel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Your progress", style = MaterialTheme.typography.titleMedium)
                        Text("Progress is saved on this phone after every move and kept through app updates. It is also included in your phone's Google backup. " +
                            "You can make your own backup file too (handy for a new phone).", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CandyButton("Back up", Modifier.weight(1f), small = true, color = Palette.Blue, dark = Palette.BlueDark) {
                                exporter.launch("surf-and-swirl-backup.json")
                            }
                            CandyButton("Restore", Modifier.weight(1f), small = true, color = Palette.Purple, dark = Palette.PurpleDark) {
                                importer.launch(arrayOf("application/json", "text/plain", "*/*"))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (!confirmReset) CandyButton("Start over…", Modifier.fillMaxWidth(), small = true, color = Palette.Gray, dark = Palette.GrayDark) { confirmReset = true }
                        else Column {
                            Text("This erases all progress. Are you sure?", style = MaterialTheme.typography.bodyMedium, color = Palette.Ruby)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CandyButton("Keep playing", Modifier.weight(1f), small = true) { confirmReset = false }
                                CandyButton("Erase", Modifier.weight(1f), small = true, color = Palette.Ruby, dark = Palette.PinkDark) { confirmReset = false; vm.resetGame() }
                            }
                        }
                    }
                }
            }
            item {
                Panel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Credits", style = MaterialTheme.typography.titleMedium)
                        Text("Made with love for Mandy (and Surf).\n\n" +
                            "Item art: Microsoft Fluent Emoji (MIT License).\n" +
                            "Fonts: Fredoka and Nunito (SIL Open Font License).\n" +
                            "Cinnamon rolls, sounds and everything else: made for this game.",
                            style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        val version = remember {
                            runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "?"
                        }
                        Text("Version $version", style = MaterialTheme.typography.bodySmall)
                        Text("Updates: stephenkmorgan.github.io/bakery", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun Toggle(title: String, sub: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).semantics { contentDescription = title }.clickable { onChange(!value) },
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(sub, style = MaterialTheme.typography.bodySmall)
        }
        Switch(value, onChange, colors = SwitchDefaults.colors(checkedTrackColor = Palette.Green))
    }
}

@Composable
private fun ThemeChip(name: String, icon: String, bg: Brush, selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.width(84.dp).semantics { contentDescription = "Theme $name" }.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(bg)
            .border(if (selected) 4.dp else 2.dp, if (selected) Palette.Green else Color.White, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center) { ItemArt(icon, Modifier.size(40.dp)) }
        Text(name, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
fun StatsScreen(vm: GameViewModel, state: GameState) {
    val stats = Brain.stats(state)
    val fmt = DateTimeFormatter.ofPattern("MMM d, h:mm a")
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        ScreenHeader(vm, state, "Brain Stats", back = Screen.SETTINGS)
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Panel(Modifier.fillMaxWidth(), color = Color(0xFFFFF3C4)) {
                    Text("Each puzzle type has a difficulty score for you (1.0 = average). It starts with words easier and numbers harder, " +
                        "then learns from every puzzle: how often it's solved, mistakes, hints, and how long it takes. " +
                        "A higher score means a lower puzzle level for the same reward, so prices stay fair.",
                        Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
            item { Text("Hardest first", style = MaterialTheme.typography.titleMedium) }
            items(stats) { st ->
                Panel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ItemArt(st.type.art, Modifier.size(36.dp)); Spacer(Modifier.width(8.dp))
                            Text(st.type.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            Text("%.2f".format(st.factor), style = MaterialTheme.typography.titleLarge,
                                color = if (st.factor > 1.1) Palette.Ruby else if (st.factor < 0.9) Palette.GreenDark else Palette.Cocoa)
                        }
                        ProgressBar((st.factor / 2.5).toFloat(), Modifier.fillMaxWidth(), if (st.factor > 1.1) Palette.Ruby else Palette.Green, 10.dp)
                        Spacer(Modifier.height(4.dp))
                        Text(if (st.played == 0) "Not played yet" else
                            "Played ${st.played} · solved ${100 * st.solved / st.played}% · typical time ${st.medianSeconds.toInt()}s · " +
                                "avg mistakes %.1f · avg level %.1f".format(st.avgMistakes, st.avgLevel), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item { Text("Recent puzzles", style = MaterialTheme.typography.titleMedium) }
            items(state.puzzleLog.takeLast(30).reversed()) { r ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(com.surfswirl.bakery.puzzles.PuzzleType.of(r.type)?.title ?: r.type, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text("Lv ${r.level} · ${if (r.solved) "✓" else "✗"} · ${r.seconds}s", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(8.dp))
                    Text(Instant.ofEpochMilli(r.at).atZone(ZoneId.systemDefault()).format(fmt), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
