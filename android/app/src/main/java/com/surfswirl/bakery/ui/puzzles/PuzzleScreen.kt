package com.surfswirl.bakery.ui.puzzles

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.surfswirl.bakery.puzzles.Generators
import com.surfswirl.bakery.puzzles.PuzzleType
import com.surfswirl.bakery.puzzles.WordBank
import com.surfswirl.bakery.ui.GameViewModel
import com.surfswirl.bakery.ui.Ui
import com.surfswirl.bakery.ui.components.CandyButton
import com.surfswirl.bakery.ui.components.ItemArt
import com.surfswirl.bakery.ui.components.OutlinedText
import com.surfswirl.bakery.ui.components.Panel
import com.surfswirl.bakery.ui.components.Particles
import com.surfswirl.bakery.ui.components.bob
import com.surfswirl.bakery.ui.components.pulse
import com.surfswirl.bakery.ui.theme.Fredoka
import com.surfswirl.bakery.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/** What a puzzle reports back. */
class PuzzleHost(
    val solved: (mistakes: Int, hints: Int) -> Unit,
    val failed: (mistakes: Int, hints: Int) -> Unit,
    val sfxGood: () -> Unit,
    val sfxBad: () -> Unit,
)

@Composable
fun PuzzleScreen(vm: GameViewModel, ui: Ui, particles: Particles) {
    val p = ui.puzzle ?: return
    val type = p.type ?: return
    // Active time only: counts while the screen is resumed. Never shown to the player.
    var seconds by remember(p.seed) { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(p.seed) {
        while (true) { delay(1000); if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) seconds++ }
    }
    var done by remember(p.seed) { mutableStateOf<Boolean?>(null) }
    val width = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val scope = rememberCoroutineScope()
    val host = remember(p.seed) {
        PuzzleHost(
            solved = { m, h ->
                if (done == null) {
                    done = true
                    particles.confetti(width)
                    scope.launch { delay(1400); vm.puzzleDone(true, m, h, seconds) }
                }
            },
            failed = { m, h -> if (done == null) { done = false; scope.launch { delay(1600); vm.puzzleDone(false, m, h, seconds) } } },
            sfxGood = { vm.playGood() },
            sfxBad = { vm.playBad() },
        )
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(Color.White).border(2.dp, Palette.Ruby, CircleShape)
                .semantics { contentDescription = "Quit puzzle" }.clickable { vm.quitPuzzle() }, contentAlignment = Alignment.Center) {
                OutlinedText("✕", 18, color = Palette.Ruby, outline = Color.White)
            }
            Spacer(Modifier.width(10.dp))
            ItemArt(type.art, Modifier.size(40.dp).bob(2f))
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                OutlinedText(type.title, 22, align = TextAlign.Start)
                Text("Level ${p.level}" + if (p.total > 1) " · Puzzle ${p.solved + 1} of ${p.total}" else "", style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val rnd = remember(p.seed) { Random(p.seed) }
            when (type) {
                PuzzleType.SCRAMBLE -> Scramble(p.level, rnd, host)
                PuzzleType.SECRET_WORD -> SecretWord(p.level, rnd, host)
                PuzzleType.QUICK_MATH -> QuickMath(p.level, rnd, host)
                PuzzleType.NUMBER_PATTERN -> Patterns(p.level, rnd, host)
                PuzzleType.MEMORY -> Memory(p.level, rnd, host)
                PuzzleType.SUDOKU -> SudokuGame(p.level, rnd, host)
            }
            when (done) {
                true -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val s = remember { Animatable(0.2f) }
                    LaunchedEffect(Unit) { s.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 300f)) }
                    Box(Modifier.graphicsLayer { scaleX = s.value; scaleY = s.value }) { OutlinedText("Solved!", 52, outline = Palette.GreenDark) }
                }
                false -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { OutlinedText("Out of tries", 40, outline = Palette.PinkDark) }
                null -> {}
            }
        }
    }
}

// ---------------------------------------------------------------- shared pieces

@Composable
fun Shake(trigger: Int, content: @Composable () -> Unit) {
    val x = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger > 0) for (v in listOf(18f, -14f, 10f, -6f, 0f)) x.animateTo(v, tween(45))
    }
    Box(Modifier.graphicsLayer { translationX = x.value }) { content() }
}

@Composable
private fun Tile(text: String, size: Dp, color: Color, modifier: Modifier = Modifier, textColor: Color = Palette.Cocoa, onClick: (() -> Unit)? = null) {
    val pressed = remember { mutableStateOf(false) }
    Box(modifier.size(size).clip(RoundedCornerShape(size * 0.22f))
        .background(Brush.verticalGradient(listOf(color, color.copy(red = color.red * 0.92f, green = color.green * 0.92f, blue = color.blue * 0.92f))))
        .border(2.dp, Color.White, RoundedCornerShape(size * 0.22f))
        .then(if (onClick != null) Modifier.semantics { contentDescription = "Tile $text" }.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center) {
        Text(text, fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.5f).sp, color = textColor)
    }
}

@Composable
private fun Keypad(onKey: (String) -> Unit, extra: String = "") {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf(if (extra.isEmpty()) "⌫" else extra, "0", "✓"))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        for (r in rows) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (k in r) {
                val color = when (k) { "✓" -> Palette.Green; "⌫", "−" -> Palette.Orange; else -> Color.White }
                Box(Modifier.width(88.dp).height(56.dp).clip(RoundedCornerShape(16.dp)).background(color)
                    .border(2.dp, Color(0x33000000), RoundedCornerShape(16.dp)).semantics { contentDescription = "Key $k" }
                    .clickable { onKey(k) }, contentAlignment = Alignment.Center) {
                    Text(k, fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = if (color == Color.White) Palette.Cocoa else Color.White)
                }
            }
        }
    }
}

@Composable
private fun HintButton(modifier: Modifier = Modifier, label: String = "Hint", onClick: () -> Unit) =
    CandyButton(label, modifier, small = true, color = Palette.Gold, dark = Palette.GoldDark, icon = "light_bulb", onClick = onClick)

@Composable
private fun Dots(total: Int, done: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            Box(Modifier.size(14.dp).clip(CircleShape).background(if (i < done) Palette.Green else Color(0x33000000)).border(1.5.dp, Color.White, CircleShape))
        }
    }
}

// ---------------------------------------------------------------- Word Scramble

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Scramble(level: Int, rnd: Random, host: PuzzleHost) {
    val words = remember { Generators.scramble(level, rnd) }
    var wi by remember { mutableIntStateOf(0) }
    var mistakes by remember { mutableIntStateOf(0) }
    var hints by remember { mutableIntStateOf(0) }
    var shake by remember { mutableIntStateOf(0) }
    val word = words.getOrNull(wi) ?: return
    val letters = remember(wi) { mutableStateListOf<Char>().apply { addAll(Generators.shuffleLetters(word, rnd)) } }
    val slots = remember(wi) { mutableStateListOf<Int?>().apply { repeat(word.length) { add(null) } } }
    var locked by remember(wi) { mutableIntStateOf(0) }  // slots fixed by hints
    var solvedWord by remember(wi) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun check() {
        if (slots.any { it == null }) return
        val guess = String(slots.map { letters[it!!] }.toCharArray())
        if (guess == word || guess in WordBank.anagrams(word)) {
            solvedWord = true
            host.sfxGood()
            scope.launch {
                delay(700)
                if (wi + 1 >= words.size) host.solved(mistakes, hints) else wi++
            }
        } else {
            mistakes++; shake++; host.sfxBad()
            for (i in locked until slots.size) slots[i] = null
        }
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Dots(words.size, wi + if (solvedWord) 1 else 0)
        Spacer(Modifier.height(16.dp))
        Text("Unscramble the word", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        val tile = if (word.length >= 8) 40.dp else 46.dp
        Shake(shake) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                slots.forEachIndexed { i, li ->
                    Tile(li?.let { letters[it].toString() } ?: "", tile,
                        if (solvedWord) Color(0xFFB8F5B0) else if (i < locked) Color(0xFFFFE08A) else Color(0xFFFFF6EA),
                        onClick = { if (i >= locked && li != null && !solvedWord) slots[i] = null })
                }
            }
        }
        Spacer(Modifier.height(30.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            letters.forEachIndexed { i, c ->
                val used = i in slots
                Tile(c.toString(), 54.dp, if (used) Color(0x33FFFFFF) else Color(0xFFFFC98A), Modifier.graphicsLayer { alpha = if (used) 0.3f else 1f },
                    onClick = {
                        if (!used && !solvedWord) {
                            val e = slots.indexOfFirst { it == null }
                            if (e >= 0) { slots[e] = i; host.sfxTap(); check() }
                        }
                    })
            }
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CandyButton("Shuffle", Modifier.weight(1f), small = true, color = Palette.Blue, dark = Palette.BlueDark) {
                val keep = slots.toList().map { it?.let { idx -> letters[idx] } }
                val free = letters.indices.filter { it !in slots }.map { letters[it] }.shuffled(rnd)
                // rebuild letters keeping slot letters in front
                val newLetters = keep.filterNotNull() + free
                letters.clear(); letters.addAll(newLetters)
                var k = 0
                for (i in slots.indices) slots[i] = if (keep[i] != null) k++ else null
            }
            CandyButton("Clear", Modifier.weight(1f), small = true, color = Palette.Orange, dark = Palette.OrangeDark) {
                for (i in locked until slots.size) slots[i] = null
            }
            HintButton(Modifier.weight(1f)) {
                if (locked < word.length - 1 && !solvedWord) {
                    hints++
                    for (i in locked until slots.size) slots[i] = null
                    val need = word[locked]
                    val li = letters.indices.first { it !in slots && letters[it] == need }
                    slots[locked] = li
                    locked++
                }
            }
        }
    }
}

private fun PuzzleHost.sfxTap() {}

// ---------------------------------------------------------------- Secret Word

@Composable
fun SecretWord(level: Int, rnd: Random, host: PuzzleHost) {
    val answer = remember { Generators.secret(level, rnd) }
    val tries = Generators.secretTries(level)
    val guesses = remember { mutableStateListOf<String>() }
    var current by remember { mutableStateOf("") }
    var hints by remember { mutableIntStateOf(0) }
    val revealed = remember { mutableStateListOf<Int>() }
    var shake by remember { mutableIntStateOf(0) }
    var over by remember { mutableStateOf(false) }

    val keyMarks = remember(guesses.size) {
        val m = mutableMapOf<Char, Generators.Mark>()
        for (g in guesses) Generators.mark(g, answer).forEachIndexed { i, mk ->
            val c = g[i]; val old = m[c]
            if (old == null || mk.ordinal < old.ordinal) m[c] = mk
        }
        m
    }

    fun submit() {
        if (over) return
        if (current.length < answer.length) { shake++; host.sfxBad(); return }
        guesses += current
        val g = current
        current = ""
        if (g == answer) { over = true; host.sfxGood(); host.solved(maxOf(0, guesses.size - 3), hints) }
        else if (guesses.size >= tries) { over = true; host.failed(guesses.size, hints) }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cell = minOf((maxWidth - 40.dp) / answer.length, 54.dp, (maxHeight - 260.dp) / tries)
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Guess the ${answer.length}-letter word · ${tries - guesses.size} tries left", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Legend(Color(0xFF6CCB5F), "right spot"); Legend(Color(0xFFF2C14E), "in the word"); Legend(Color(0xFFB9AFA8), "not in it")
            }
            Spacer(Modifier.height(8.dp))
            for (r in 0 until tries) {
                val g = guesses.getOrNull(r)
                val marks = g?.let { Generators.mark(it, answer) }
                val row = @Composable {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(vertical = 2.dp)) {
                        for (c in answer.indices) {
                            val ch = when { g != null -> g[c]; r == guesses.size -> current.getOrNull(c); else -> null }
                            val color = when (marks?.get(c)) {
                                Generators.Mark.HIT -> Color(0xFF6CCB5F); Generators.Mark.NEAR -> Color(0xFFF2C14E); Generators.Mark.MISS -> Color(0xFFB9AFA8)
                                null -> if (r == guesses.size && c in revealed && ch == null) Color(0xFFFFF0B8) else Color(0xFFFFF6EA)
                            }
                            val flip by animateFloatAsState(if (marks != null) 360f else 0f, tween(500, delayMillis = c * 90), label = "flip")
                            Tile(ch?.toString() ?: if (r == guesses.size && c in revealed) answer[c].lowercase() else "", cell, color,
                                Modifier.graphicsLayer { rotationX = flip }, textColor = if (marks != null) Color.White else Palette.Cocoa)
                        }
                    }
                }
                if (r == guesses.size) Shake(shake) { row() } else row()
            }
            if (over && guesses.lastOrNull() != answer) Text("The word was $answer", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            HintButton(label = "Hint: reveal a letter") {
                val free = answer.indices.filter { it !in revealed }
                if (free.size > 1 && !over) { revealed += free.random(rnd); hints++ }
            }
            Spacer(Modifier.height(6.dp))
            Keyboard(keyMarks, onLetter = { if (current.length < answer.length && !over) current += it },
                onDelete = { current = current.dropLast(1) }, onEnter = { submit() })
        }
    }
}

@Composable
private fun Legend(c: Color, t: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(c)); Spacer(Modifier.width(3.dp))
        Text(t, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Keyboard(marks: Map<Char, Generators.Mark>, onLetter: (Char) -> Unit, onDelete: () -> Unit, onEnter: () -> Unit) {
    val rows = listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val kw = (maxWidth - 4.dp * 9) / 10
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            rows.forEachIndexed { ri, r ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (ri == 2) KeyBox("⏎", kw * 1.5f, Palette.Green, Color.White) { onEnter() }
                    for (c in r) {
                        val bg = when (marks[c]) { Generators.Mark.HIT -> Color(0xFF6CCB5F); Generators.Mark.NEAR -> Color(0xFFF2C14E); Generators.Mark.MISS -> Color(0xFF9D938D); null -> Color.White }
                        KeyBox(c.toString(), kw, bg, if (bg == Color.White) Palette.Cocoa else Color.White) { onLetter(c) }
                    }
                    if (ri == 2) KeyBox("⌫", kw * 1.5f, Palette.Orange, Color.White) { onDelete() }
                }
            }
        }
    }
}

@Composable
private fun KeyBox(t: String, w: Dp, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(Modifier.width(w).height(50.dp).clip(RoundedCornerShape(10.dp)).background(bg).border(1.5.dp, Color(0x33000000), RoundedCornerShape(10.dp))
        .semantics { contentDescription = "Key $t" }.clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(t, fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = fg)
    }
}

// ---------------------------------------------------------------- Quick Math

@Composable
fun QuickMath(level: Int, rnd: Random, host: PuzzleHost) {
    val qs = remember { Generators.math(level, rnd) }
    var qi by remember { mutableIntStateOf(0) }
    var input by remember { mutableStateOf("") }
    var mistakes by remember { mutableIntStateOf(0) }
    var wrongHere by remember { mutableIntStateOf(0) }
    var hints by remember { mutableIntStateOf(0) }
    var shake by remember { mutableIntStateOf(0) }
    var flash by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val q = qs.getOrNull(qi) ?: return

    fun next() {
        input = ""; wrongHere = 0
        if (qi + 1 >= qs.size) host.solved(mistakes, hints) else qi++
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Dots(qs.size, qi)
        Spacer(Modifier.height(24.dp))
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedText(q.text + " = ?", 40, outline = Palette.Cocoa, color = Color.White)
                Spacer(Modifier.height(12.dp))
                Shake(shake) {
                    Box(Modifier.width(180.dp).height(64.dp).clip(RoundedCornerShape(16.dp)).background(Color.White)
                        .border(3.dp, Palette.Blue, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        Text(flash ?: input.ifEmpty { " " }, fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 34.sp,
                            color = if (flash != null) Palette.GreenDark else Palette.Cocoa)
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        HintButton(label = "Hint: first digit") {
            val a = q.answer.toString()
            if (input.isEmpty() && a.length > 1) { input = a.take(1); hints++ }
        }
        Spacer(Modifier.height(8.dp))
        Keypad({ k ->
            if (flash != null) return@Keypad
            when (k) {
                "⌫" -> input = input.dropLast(1)
                "✓" -> if (input.isNotEmpty()) {
                    if (input.toIntOrNull() == q.answer) {
                        host.sfxGood(); flash = "✓ ${q.answer}"
                        scope.launch { delay(450); flash = null; next() }
                    } else {
                        mistakes++; wrongHere++; shake++; host.sfxBad(); input = ""
                        if (wrongHere >= 3) { flash = "${q.answer}"; scope.launch { delay(1200); flash = null; next() } }
                    }
                }
                else -> if (input.length < 6) input += k
            }
        })
    }
}

// ---------------------------------------------------------------- Number Patterns

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Patterns(level: Int, rnd: Random, host: PuzzleHost) {
    val ps = remember { Generators.patterns(level, rnd) }
    var pi by remember { mutableIntStateOf(0) }
    var input by remember { mutableStateOf("") }
    var mistakes by remember { mutableIntStateOf(0) }
    var wrongHere by remember { mutableIntStateOf(0) }
    var hints by remember { mutableIntStateOf(0) }
    var shake by remember { mutableIntStateOf(0) }
    var showWhy by remember { mutableStateOf(false) }
    var flash by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val p = ps.getOrNull(pi) ?: return

    fun next() {
        input = ""; wrongHere = 0; showWhy = false
        if (pi + 1 >= ps.size) host.solved(mistakes, hints) else pi++
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Dots(ps.size, pi)
        Spacer(Modifier.height(16.dp))
        Text("What comes next?", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            p.shown.forEachIndexed { i, n ->
                Box(Modifier.height(54.dp).width(if (n.toString().length > 3) 74.dp else 56.dp).clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFFF6EA)).border(2.dp, Color.White, RoundedCornerShape(14.dp)).bob(2f, 1300 + i * 120),
                    contentAlignment = Alignment.Center) { Text("$n", fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Palette.Cocoa) }
            }
            Shake(shake) {
                Box(Modifier.height(54.dp).width(80.dp).clip(RoundedCornerShape(14.dp)).background(Color.White)
                    .border(3.dp, Palette.Purple, RoundedCornerShape(14.dp)).pulse(input.isEmpty()), contentAlignment = Alignment.Center) {
                    Text(flash ?: input.ifEmpty { "?" }, fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 24.sp,
                        color = if (flash != null) Palette.GreenDark else Palette.Purple)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        if (showWhy) Text(p.explain, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        HintButton(label = "Hint: show the rule") { if (!showWhy) { showWhy = true; hints++ } }
        Spacer(Modifier.height(8.dp))
        Keypad({ k ->
            if (flash != null) return@Keypad
            when (k) {
                "⌫" -> input = input.dropLast(1)
                "✓" -> if (input.isNotEmpty()) {
                    if (input.toIntOrNull() == p.answer) {
                        host.sfxGood(); flash = "${p.answer}"; showWhy = true
                        scope.launch { delay(1100); flash = null; next() }
                    } else {
                        mistakes++; wrongHere++; shake++; host.sfxBad(); input = ""
                        if (wrongHere >= 3) { flash = "${p.answer}"; showWhy = true; scope.launch { delay(2000); flash = null; next() } }
                    }
                }
                else -> if (input.length < 6) input += k
            }
        })
    }
}

// ---------------------------------------------------------------- Memory Match

@Composable
fun Memory(level: Int, rnd: Random, host: PuzzleHost) {
    val cards = remember { Generators.memory(level, rnd) }
    val cols = Generators.memoryColumns(level)
    val matched = remember { mutableStateListOf<Int>() }
    val open = remember { mutableStateListOf<Int>() }
    var mismatches by remember { mutableIntStateOf(0) }
    var hints by remember { mutableIntStateOf(0) }
    var peek by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pairs = cards.size / 2

    fun tap(i: Int) {
        if (peek || i in matched || i in open || open.size >= 2) return
        open += i
        if (open.size == 2) {
            val (a, b) = open[0] to open[1]
            if (cards[a] == cards[b]) {
                matched += a; matched += b; open.clear(); host.sfxGood()
                if (matched.size == cards.size) host.solved(maxOf(0, mismatches - pairs), hints)
            } else {
                mismatches++
                scope.launch { delay(750); open.clear() }
            }
        }
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Find all $pairs pairs", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val rows = (cards.size + cols - 1) / cols
            val size = minOf((maxWidth - 8.dp * (cols - 1)) / cols, (maxHeight - 8.dp * (rows - 1)) / rows, 110.dp)
            Column(Modifier.align(Alignment.Center), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (r in 0 until rows) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (c in 0 until cols) {
                        val i = r * cols + c
                        if (i >= cards.size) { Spacer(Modifier.size(size)); continue }
                        val faceUp = peek || i in open || i in matched
                        val rot by animateFloatAsState(if (faceUp) 180f else 0f, tween(300), label = "card")
                        Box(Modifier.size(size).graphicsLayer { rotationY = rot; cameraDistance = 12f * density }
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (rot > 90f) (if (i in matched) Color(0xFFDFF7D8) else Color.White) else Palette.Pink)
                            .border(3.dp, Color.White, RoundedCornerShape(16.dp))
                            .semantics { contentDescription = "Card ${i + 1}" }
                            .clickable { tap(i) }, contentAlignment = Alignment.Center) {
                            if (rot > 90f) ItemArt(cards[i], Modifier.fillMaxSize().padding(10.dp).graphicsLayer { rotationY = 180f })
                            else ItemArt("cinnamon_roll", Modifier.fillMaxSize().padding(size * 0.25f), alpha = 0.5f)
                        }
                    }
                }
            }
        }
        HintButton(label = "Hint: peek") {
            if (!peek) { hints++; peek = true; scope.launch { delay(900); peek = false } }
        }
    }
}

// ---------------------------------------------------------------- Mini Sudoku

@Composable
fun SudokuGame(level: Int, rnd: Random, host: PuzzleHost) {
    val puz = remember { Generators.sudoku(level, rnd) }
    val n = puz.n
    val grid = remember { mutableStateListOf<Int>().apply { addAll(puz.givens) } }
    val wrong = remember { mutableStateListOf<Int>() }
    var sel by remember { mutableStateOf(grid.indexOfFirst { it == 0 }) }
    var mistakes by remember { mutableIntStateOf(0) }
    var hints by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    fun place(v: Int) {
        if (finished || sel < 0 || puz.givens[sel] != 0) return
        grid[sel] = v
        if (v != 0 && v != puz.solution[sel]) { if (sel !in wrong) wrong += sel; mistakes++; host.sfxBad() } else wrong.remove(sel)
        if (grid.indices.all { grid[it] == puz.solution[it] }) { finished = true; host.solved(mistakes, hints) }
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Each row, column and box has 1–$n once", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1f).padding(4.dp)) {
            val cell = maxWidth / n
            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Palette.Cocoa).padding(2.dp)) {
                Column {
                    for (r in 0 until n) Row {
                        for (c in 0 until n) {
                            val i = r * n + c
                            val given = puz.givens[i] != 0
                            val boxShade = ((r / puz.boxRows) + (c / puz.boxCols)) % 2 == 0
                            val selRow = sel >= 0 && (sel / n == r || sel % n == c)
                            val bg = when {
                                i == sel -> Color(0xFFFFE08A)
                                grid[i] != 0 && sel >= 0 && grid[i] == grid[sel] -> Color(0xFFFFF0B8)
                                selRow -> Color(0xFFFFF4DE)
                                boxShade -> Color(0xFFFFFBF5) else -> Color(0xFFFFEEDC)
                            }
                            Box(Modifier.size(cell - 2.dp / n).padding(
                                start = if (c % puz.boxCols == 0 && c > 0) 1.5.dp else 0.5.dp,
                                top = if (r % puz.boxRows == 0 && r > 0) 1.5.dp else 0.5.dp, end = 0.5.dp, bottom = 0.5.dp)
                                .background(bg).semantics { contentDescription = "Cell ${r + 1}-${c + 1}" }.clickable { sel = i },
                                contentAlignment = Alignment.Center) {
                                if (grid[i] != 0) Text("${grid[i]}", fontFamily = Fredoka, fontWeight = FontWeight.Bold,
                                    fontSize = (cell.value * 0.55f).sp,
                                    color = when { given -> Palette.Cocoa; i in wrong -> Palette.Ruby; else -> Palette.BlueDark })
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (v in 1..n) {
                Box(Modifier.size(if (n == 9) 36.dp else 52.dp).clip(RoundedCornerShape(12.dp)).background(Color.White)
                    .border(2.dp, Palette.Blue, RoundedCornerShape(12.dp)).semantics { contentDescription = "Number $v" }.clickable { place(v) },
                    contentAlignment = Alignment.Center) { Text("$v", fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Palette.BlueDark) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CandyButton("Erase", Modifier.weight(1f), small = true, color = Palette.Orange, dark = Palette.OrangeDark) { place(0) }
            HintButton(Modifier.weight(1f), label = "Hint: fill cell") {
                if (finished) return@HintButton
                val target = if (sel >= 0 && grid[sel] != puz.solution[sel]) sel else grid.indices.firstOrNull { grid[it] != puz.solution[it] } ?: return@HintButton
                sel = target; hints++
                grid[target] = puz.solution[target]; wrong.remove(target)
                if (grid.indices.all { grid[it] == puz.solution[it] }) { finished = true; host.solved(mistakes, hints) }
            }
        }
    }
}
