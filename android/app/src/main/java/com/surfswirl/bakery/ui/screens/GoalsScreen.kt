package com.surfswirl.bakery.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.surfswirl.bakery.game.Economy
import com.surfswirl.bakery.game.Engine
import com.surfswirl.bakery.game.GameState
import com.surfswirl.bakery.game.Goals
import com.surfswirl.bakery.ui.Dialog
import com.surfswirl.bakery.ui.GameViewModel
import com.surfswirl.bakery.ui.components.Badge
import com.surfswirl.bakery.ui.components.CandyButton
import com.surfswirl.bakery.ui.components.ItemArt
import com.surfswirl.bakery.ui.components.OutlinedText
import com.surfswirl.bakery.ui.components.Panel
import com.surfswirl.bakery.ui.components.ProgressBar
import com.surfswirl.bakery.ui.components.RewardRow
import com.surfswirl.bakery.ui.components.bob
import com.surfswirl.bakery.ui.components.pulse
import com.surfswirl.bakery.ui.theme.Palette
import java.time.DayOfWeek
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Everything to come back for each day: the login reward, daily specials and the weekly mini-event. */
@Composable
fun GoalsScreen(vm: GameViewModel, state: GameState) {
    val today = vm.today()
    val specials = Goals.today(today)
    val week = Goals.week(today)
    val weekEnds = ChronoUnit.DAYS.between(today, today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)))
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        ScreenHeader(vm, state, "Goals")
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Login reward
            item {
                val avail = Engine.dailyAvailable(state, today)
                Panel(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ItemArt("calendar", Modifier.size(44.dp).pulse(avail)); Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Daily reward", style = MaterialTheme.typography.titleMedium)
                            Text("Day ${Engine.dailyIndex(state) + if (avail) 1 else 0} of 7", style = MaterialTheme.typography.bodySmall)
                        }
                        CandyButton(if (avail) "Open" else "View", small = true, color = if (avail) Palette.Green else Palette.Gray,
                            dark = if (avail) Palette.GreenDark else Palette.GrayDark) { vm.pushDialog(Dialog.Daily) }
                    }
                }
            }
            // Daily specials
            item {
                Panel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Today's specials", style = MaterialTheme.typography.titleLarge)
                        Text("New ones every day", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(6.dp))
                        for (g in specials) {
                            val p = Goals.progress(state, g)
                            val done = g.id in state.dailyGoalsClaimed
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).semantics { contentDescription = "Special ${g.title}" },
                                verticalAlignment = Alignment.CenterVertically) {
                                ItemArt(g.art, Modifier.size(38.dp)); Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(g.title, style = MaterialTheme.typography.titleSmall)
                                    ProgressBar(p.toFloat() / g.target, Modifier.fillMaxWidth(), Palette.Pink, 14.dp, "$p / ${g.target}")
                                }
                                Spacer(Modifier.width(8.dp))
                                when {
                                    done -> OutlinedText("✓", 24, color = Palette.Green, outline = Color.White)
                                    p >= g.target -> CandyButton("Claim", Modifier.pulse(), small = true) { vm.claimDailyGoal(g.id) }
                                    else -> RewardRow(g.reward, size = 26.dp)
                                }
                            }
                        }
                        val all = specials.all { it.id in state.dailyGoalsClaimed }
                        val bonusDone = "bonus" in state.dailyGoalsClaimed
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFF3C4)).padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("Finish all three:", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                            when {
                                bonusDone -> OutlinedText("✓", 22, color = Palette.Green, outline = Color.White)
                                all -> CandyButton("Bonus!", Modifier.pulse(), small = true, color = Palette.Gold, dark = Palette.GoldDark) { vm.claimDailyBonus() }
                                else -> RewardRow(Goals.dailyBonus, size = 26.dp)
                            }
                        }
                    }
                }
            }
            // Weekly mini-event
            item {
                Panel(Modifier.fillMaxWidth(), color = Color(0xFFEAF5FF)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ItemArt(week.art, Modifier.size(52.dp).bob(3f)); Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(week.name, style = MaterialTheme.typography.titleLarge)
                                Text(week.blurb, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${state.weekPts} points", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            Badge(if (weekEnds <= 1) "Ends tonight" else "$weekEnds days left", color = Palette.BlueDark)
                        }
                        Text("Every order you serve is a point; ${week.name} orders count double.", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(6.dp))
                        Goals.weekMilestones.forEachIndexed { i, (need, r) ->
                            val reached = state.weekPts >= need
                            val claimed = i in state.weekClaimed
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(38.dp).clip(CircleShape).background(if (reached) Palette.Blue else Palette.Gray),
                                    contentAlignment = Alignment.Center) { OutlinedText("$need", 12) }
                                Spacer(Modifier.width(8.dp))
                                RewardRow(r, Modifier.weight(1f), size = 28.dp)
                                when {
                                    claimed -> OutlinedText("✓", 22, color = Palette.Green, outline = Color.White)
                                    reached -> CandyButton("Claim", Modifier.pulse(), small = true, color = Palette.Blue, dark = Palette.BlueDark) { vm.claimWeek(i) }
                                    else -> ProgressBar(state.weekPts.toFloat() / need, Modifier.width(80.dp), Palette.Blue, 12.dp)
                                }
                            }
                        }
                    }
                }
            }
            item {
                Text("Specials refresh every day and the weekly event every Monday, so there's always something to work on " +
                    "between story chapters.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
