package com.surfswirl.bakery.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.surfswirl.bakery.game.BOARD_COLS
import com.surfswirl.bakery.game.Content
import com.surfswirl.bakery.game.Currency
import com.surfswirl.bakery.game.Fx
import com.surfswirl.bakery.ui.components.OutlinedText
import com.surfswirl.bakery.ui.components.Panel
import com.surfswirl.bakery.ui.components.ParticleLayer
import com.surfswirl.bakery.ui.components.Particles
import com.surfswirl.bakery.ui.components.FloatingBackground
import com.surfswirl.bakery.ui.components.Shape
import com.surfswirl.bakery.ui.screens.BakeryScreen
import com.surfswirl.bakery.ui.screens.BoardScreen
import com.surfswirl.bakery.ui.screens.DialogHost
import com.surfswirl.bakery.ui.screens.EventScreen
import com.surfswirl.bakery.ui.screens.PassScreen
import com.surfswirl.bakery.ui.screens.SettingsScreen
import com.surfswirl.bakery.ui.screens.ShopScreen
import com.surfswirl.bakery.ui.screens.StatsScreen
import com.surfswirl.bakery.ui.puzzles.PuzzleScreen
import com.surfswirl.bakery.ui.theme.BakeryTheme
import com.surfswirl.bakery.ui.theme.LocalGameColors
import kotlinx.coroutines.delay

/** Where things are on screen (window pixels), so coins can fly from a cell to its counter. */
class Geometry {
    var boardOrigin = Offset.Zero
    var cellPx = 0f
    var coins = Offset.Zero
    var rubies = Offset.Zero
    var energy = Offset.Zero
    var xp = Offset.Zero
    fun cellCenter(i: Int) = Offset(boardOrigin.x + (i % BOARD_COLS + 0.5f) * cellPx, boardOrigin.y + (i / BOARD_COLS + 0.5f) * cellPx)
}

@Composable
fun App(vm: GameViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val theme = remember(state.settings.theme, state.eventId) { vm.theme() }
    val particles = remember { Particles() }
    val geo = remember { Geometry() }
    val widthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }

    BackHandler(enabled = ui.screen != Screen.BOARD || ui.dialogs.isNotEmpty()) { vm.back() }

    LaunchedEffect(vm) {
        vm.fx.collect { f ->
            when (f) {
                is Fx.Merge -> {
                    val c = geo.cellCenter(f.cell)
                    val color = Content.chainOf(f.item)?.color?.let { Color(it) } ?: Color.Yellow
                    particles.burst(c.x, c.y, color, 16 + Content.level(f.item) * 2, 420f + Content.level(f.item) * 40f)
                    if (Content.level(f.item) >= (Content.chainOf(f.item)?.max ?: 99)) particles.confetti(widthPx, 60)
                }
                is Fx.Unbox -> { val c = geo.cellCenter(f.cell); particles.burst(c.x, c.y, Color(0xFFC89060), 12, 300f, Shape.CIRCLE) }
                is Fx.Collect -> {
                    val c = geo.cellCenter(f.cell)
                    val (art, to) = when (f.currency) { Currency.COINS -> "coin" to geo.coins; Currency.RUBIES -> "gem_stone" to geo.rubies; Currency.ENERGY -> "high_voltage" to geo.energy }
                    particles.flyTo(art, c.x, c.y, to.x, to.y, count = (f.amount / 5).coerceIn(3, 8))
                }
                is Fx.OrderDone -> {
                    particles.flyTo("coin", widthPx / 2, geo.boardOrigin.y - 60f, geo.coins.x, geo.coins.y, 8)
                    particles.flyTo("glowing_star", widthPx / 2, geo.boardOrigin.y - 60f, geo.xp.x, geo.xp.y, 3)
                }
                is Fx.LevelUp, is Fx.Granted -> particles.confetti(widthPx)
                else -> {}
            }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(com.surfswirl.bakery.ui.theme.LocalAvatar provides state.settings.avatar) {
    BakeryTheme(theme) {
        val c = LocalGameColors.current
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom)))) {
            FloatingBackground(theme.particles)
            AnimatedContent(ui.screen, label = "screen", transitionSpec = {
                (fadeIn() + slideInVertically(spring(stiffness = 500f)) { it / 12 }) togetherWith fadeOut()
            }) { screen ->
                when (screen) {
                    Screen.BOARD -> BoardScreen(vm, state, ui, geo)
                    Screen.BAKERY -> BakeryScreen(vm, state)
                    Screen.SHOP -> ShopScreen(vm, state)
                    Screen.PASS -> PassScreen(vm, state)
                    Screen.EVENT -> EventScreen(vm, state)
                    Screen.SETTINGS -> SettingsScreen(vm, state)
                    Screen.STATS -> StatsScreen(vm, state)
                    Screen.PUZZLE -> PuzzleScreen(vm, ui, particles)
                }
            }
            DialogHost(vm, state, ui, particles)
            ParticleLayer(particles)
            Toast(ui.toast, ui.toastId)
        }
    }
    }
}

@Composable
private fun Toast(text: String?, id: Long) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(id) {
        if (text != null && id > 0) { visible = true; delay(2600); visible = false }
    }
    Box(Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = 170.dp), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(visible && text != null, enter = scaleIn(spring(dampingRatio = 0.5f)) + fadeIn(),
            exit = fadeOut() + slideOutVertically { it }) {
            Panel(Modifier.padding(horizontal = 24.dp), color = Color(0xF25B3220)) {
                OutlinedText(text ?: "", 15, outline = Color(0xFF3A1E12), modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
            }
        }
    }
}
