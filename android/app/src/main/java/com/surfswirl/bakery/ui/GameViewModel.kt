package com.surfswirl.bakery.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.surfswirl.bakery.BakeryApp
import com.surfswirl.bakery.data.SaveCodec
import com.surfswirl.bakery.data.SaveStore
import com.surfswirl.bakery.fx.Haptics
import com.surfswirl.bakery.fx.Sfx
import com.surfswirl.bakery.fx.Sound
import com.surfswirl.bakery.game.Content
import com.surfswirl.bakery.game.Currency
import com.surfswirl.bakery.game.Economy
import com.surfswirl.bakery.game.Engine
import com.surfswirl.bakery.game.Events
import com.surfswirl.bakery.game.Fx
import com.surfswirl.bakery.game.GameState
import com.surfswirl.bakery.game.Line
import com.surfswirl.bakery.game.Offer
import com.surfswirl.bakery.game.Outcome
import com.surfswirl.bakery.game.Reward
import com.surfswirl.bakery.game.Story
import com.surfswirl.bakery.notify.EnergyReminder
import com.surfswirl.bakery.puzzles.Brain
import com.surfswirl.bakery.puzzles.Category
import com.surfswirl.bakery.puzzles.PuzzleType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.random.Random

enum class Screen { BOARD, BAKERY, SHOP, PASS, EVENT, SETTINGS, STATS, PUZZLE }

/** Why the player is solving puzzles, and what they get at the end. */
sealed interface Purpose {
    data class Buy(val offer: Offer) : Purpose
    data class Recharge(val genId: String) : Purpose
}

data class PuzzleSession(
    val purpose: Purpose,
    val effort: Double,
    val total: Int,
    val solved: Int = 0,
    val usedCategories: Set<Category> = emptySet(),
    val type: PuzzleType? = null,
    val level: Int = 1,
    val seed: Long = 0,
    val bonuses: Int = 0,
    val startedAt: Long = 0,
)

sealed interface Dialog {
    data class Talk(val lines: List<Line>, val title: String = "") : Dialog
    data class Rewarded(val reward: Reward, val title: String, val lucky: Boolean = false) : Dialog
    data class LevelUp(val level: Int, val reward: Reward, val unlocked: List<String>) : Dialog
    data object Daily : Dialog
    data object OutOfEnergy : Dialog
    data class Recharge(val genId: String) : Dialog
    data class ChainInfo(val chainId: String) : Dialog
    data object PickPuzzle : Dialog
    data class PuzzleFailed(val type: PuzzleType) : Dialog
    data object Vip : Dialog
    data class Discovery(val item: String) : Dialog
    data object Storage : Dialog
    /** Full-screen look around a story place: pinch to zoom, tap things to hear their story. */
    data class Visit(val scene: String) : Dialog
    data object Customize : Dialog
}

data class Ui(
    val screen: Screen = Screen.BOARD,
    val selected: Int? = null,
    val dialogs: List<Dialog> = emptyList(),
    val puzzle: PuzzleSession? = null,
    val toast: String? = null,
    val toastId: Long = 0,
)

class GameViewModel(
    private val store: SaveStore?,
    private val sfx: Sfx?,
    private val haptics: Haptics?,
    private val music: com.surfswirl.bakery.fx.Music? = null,
    private val now: () -> Long = { System.currentTimeMillis() },
    private val today: () -> LocalDate = { LocalDate.now() },
    private val rnd: Random = Random.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(load())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _ui = MutableStateFlow(Ui())
    val ui: StateFlow<Ui> = _ui.asStateFlow()

    private val _fx = MutableSharedFlow<Fx>(extraBufferCapacity = 64)
    val fx = _fx.asSharedFlow()

    /** Ticks once a second so timers and energy update on screen. */
    private val _clock = MutableStateFlow(now())
    val clock: StateFlow<Long> = _clock.asStateFlow()

    fun today(): LocalDate = today.invoke()
    fun now(): Long = now.invoke()

    init {
        applySettings()
        viewModelScope.launch {
            _ui.collect { u -> music?.setDucked(u.screen == Screen.PUZZLE); updateMusic() }
        }
        viewModelScope.launch {
            _state.collect { updateMusic() }
        }
        refresh()
        if ("intro" !in _state.value.seenStory) {
            pushDialog(Dialog.Talk(Story.intro, "Welcome to Maple Lane"))
            _state.update { it.copy(seenStory = it.seenStory + "intro") }
        } else if (Engine.dailyAvailable(_state.value, today()) && _state.value.settings.dailyPopup) {
            pushDialog(Dialog.Daily)
        }
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _clock.value = now()
                if (_clock.value / 1000 % 15 == 0L) { refresh(); updateMusic() }
            }
        }
        @OptIn(FlowPreview::class)
        viewModelScope.launch(Dispatchers.IO) {
            _state.drop(1).debounce(400).collect { s -> store?.save(s) }
        }
    }

    private fun load(): GameState {
        val saved = runCatching { store?.load() }.getOrNull()
        return saved ?: Engine.newGame(now(), today(), rnd)
    }

    // ---------------------------------------------------------------- plumbing

    /** Animation hints, written before the state changes so the board can animate new items. */
    val spawnFrom = java.util.concurrent.ConcurrentHashMap<Long, Int>()
    val popUids: MutableSet<Long> = java.util.concurrent.ConcurrentHashMap.newKeySet()
    private val _lastTouch = MutableStateFlow(now())
    val lastTouch: StateFlow<Long> = _lastTouch.asStateFlow()

    private fun apply(o: Outcome) {
        for (f in o.fx) when (f) {
            is Fx.Spawn -> spawnFrom[f.uid] = f.from
            is Fx.Merge -> popUids += o.state.board[f.cell].uid
            is Fx.Unbox -> popUids += o.state.board[f.cell].uid
            else -> {}
        }
        _state.value = o.state
        o.fx.forEach { handleFx(it) }
    }

    private fun handleFx(f: Fx) {
        _fx.tryEmit(f)
        when (f) {
            is Fx.Spawn -> { sfx?.play(Sound.POP, 0.9f + rnd.nextFloat() * 0.3f); haptics?.tick() }
            is Fx.Merge -> { sfx?.play(Sound.MERGE, 0.8f + Content.level(f.item) * 0.07f); haptics?.click() }
            is Fx.Collect -> { sfx?.play(if (f.currency == Currency.ENERGY) Sound.SPARKLE else Sound.COIN); haptics?.tick() }
            is Fx.Unbox -> sfx?.play(Sound.UNLOCK)
            is Fx.LevelUp -> {
                sfx?.play(Sound.LEVEL_UP); haptics?.celebrate()
                val unlocked = Content.generators.filter { it.unlockLevel == f.level }.map { it.name }
                pushDialog(Dialog.LevelUp(f.level, f.reward, unlocked))
            }
            is Fx.Discover -> { if (Content.level(f.item) >= 5) pushDialog(Dialog.Discovery(f.item)) }
            is Fx.OrderDone -> { sfx?.play(Sound.ORDER); haptics?.heavy() }
            is Fx.Message -> if (f.text == "NO_ENERGY") pushDialog(Dialog.OutOfEnergy) else toast(f.text)
            is Fx.Granted -> { sfx?.play(Sound.SPARKLE); haptics?.celebrate() }
            Fx.Error -> { sfx?.play(Sound.ERROR); haptics?.error() }
            Fx.Sold -> sfx?.play(Sound.COIN)
            Fx.Pop -> sfx?.play(Sound.TAP)
        }
    }

    fun woof() { sfx?.play(Sound.WOOF); haptics?.tick() }

    fun playGood() { sfx?.play(Sound.CORRECT); haptics?.click() }
    fun playBad() { sfx?.play(Sound.WRONG, 1f, 0.7f); haptics?.error() }

    fun toast(text: String) = _ui.update { it.copy(toast = text, toastId = it.toastId + 1) }

    fun pushDialog(d: Dialog) = _ui.update { it.copy(dialogs = it.dialogs + d) }
    /** Shows a dialog on top of the current one (e.g. a story line while exploring a scene). */
    fun pushDialogFront(d: Dialog) = _ui.update { it.copy(dialogs = listOf(d) + it.dialogs) }
    fun closeDialog() = _ui.update { it.copy(dialogs = it.dialogs.drop(1)) }

    fun refresh() {
        val before = _state.value.eventId
        apply(Engine.refresh(_state.value, now(), today(), rnd))
        val ev = Events.byId(_state.value.eventId)
        val key = "event_intro_${_state.value.eventId}_${today().year}"
        if (ev != null && ev.intro.isNotEmpty() && key !in _state.value.seenStory && "intro" in _state.value.seenStory) {
            _state.update { it.copy(seenStory = it.seenStory + key) }
            pushDialog(Dialog.Talk(ev.intro, ev.place))
        }
    }

    /**
     * Picks the music for what's happening: the puzzle style during puzzles, night music in the
     * evening, the event's style during an event, otherwise café and patio taking turns. The busier
     * the board (orders ready to serve, items on the board), the more instruments join in.
     */
    private fun updateMusic() {
        val m = music ?: return
        val s = _state.value
        val hour = java.time.Instant.ofEpochMilli(now()).atZone(java.time.ZoneId.systemDefault()).hour
        val theme = Events.themeFor(s.settings, today()).id
        val styles = when {
            _ui.value.screen == Screen.PUZZLE -> listOf(com.surfswirl.bakery.music.Styles.puzzle)
            hour >= 20 || hour < 6 -> listOf(com.surfswirl.bakery.music.Styles.night)
            else -> com.surfswirl.bakery.music.Styles.forEvent(theme)?.let { listOf(it, com.surfswirl.bakery.music.Styles.cafe) }
                ?: listOf(com.surfswirl.bakery.music.Styles.cafe, com.surfswirl.bakery.music.Styles.patio)
        }
        m.setStyles(styles)
        val ready = s.orders.count { Engine.orderCells(s, it) != null }
        val filled = s.board.count { it.item != null && !it.boxed }.toFloat() / s.board.size
        m.setIntensity(0.3f + 0.15f * ready + 0.45f * filled)
    }

    fun onForeground() {
        refresh()
        music?.onForeground()
    }

    fun onBackground(context: Context) {
        music?.onBackground()
        val s = _state.value
        store?.save(s)
        if (s.settings.notifications) {
            val max = Engine.maxEnergy(s, today())
            if (s.energy < max) {
                val ms = (max - s.energy - 1).toLong() * Engine.regenMs(s, today()) + (Engine.regenMs(s, today()) - (now() - s.energyAt))
                EnergyReminder.schedule(context, ms)
            } else EnergyReminder.cancel(context)
        } else EnergyReminder.cancel(context)
    }

    private fun applySettings() {
        sfx?.enabled = _state.value.settings.sound
        haptics?.enabled = _state.value.settings.haptics
        music?.enabled = _state.value.settings.music
    }

    fun go(screen: Screen) {
        sfx?.play(Sound.TAP)
        _ui.update { it.copy(screen = screen, selected = null) }
    }

    fun back(): Boolean {
        val u = _ui.value
        if (u.dialogs.isNotEmpty()) { closeDialog(); return true }
        return when (u.screen) {
            Screen.BOARD -> false
            Screen.PUZZLE -> { quitPuzzle(); true }
            Screen.STATS -> { go(Screen.SETTINGS); true }
            else -> { go(Screen.BOARD); true }
        }
    }

    // ---------------------------------------------------------------- board

    fun tapCell(i: Int) {
        _lastTouch.value = now()
        val s = _state.value
        val cell = s.board[i]
        val item = cell.item
        if (item == null) { _ui.update { it.copy(selected = null) }; return }
        val wasSelected = _ui.value.selected == i
        _ui.update { it.copy(selected = i) }
        if (cell.boxed) { apply(Engine.tap(s, i, now(), today(), rnd)); return }
        if (Content.isGenerator(item)) {
            val gid = Content.genId(item)
            val g = s.generators[gid]
            if (g != null && g.charges <= 0) { pushDialog(Dialog.Recharge(gid)); sfx?.play(Sound.TAP); return }
            if (gid == "surf" && rnd.nextInt(6) == 0) sfx?.play(Sound.WOOF)
            val o = Engine.tap(s, i, now(), today(), rnd)
            apply(o)
            if (o.fx.any { it is Fx.Spawn } && s.tutorial == 3) advanceTutorial(4)
            return
        }
        val ch = Content.chainOf(item)
        if (ch?.collect != null || ch?.id == "chest") {
            // Collectibles and gifts act on the second tap, so dragging them is still possible.
            if (wasSelected || ch.collect != null) { apply(Engine.tap(s, i, now(), today(), rnd)); _ui.update { it.copy(selected = null) } }
            return
        }
        sfx?.play(Sound.TAP); haptics?.tick()
    }

    fun drag(from: Int, to: Int) {
        _lastTouch.value = now()
        val before = _state.value
        val o = Engine.move(before, from, to, rnd)
        apply(o)
        val merged = o.fx.any { it is Fx.Merge }
        _ui.update { it.copy(selected = if (o.state.board[to].item != null && (merged || o.state !== before)) to else from) }
        if (merged && before.tutorial == 1) advanceTutorial(2)
    }

    fun sellSelected() {
        val i = _ui.value.selected ?: return
        apply(Engine.sell(_state.value, i))
        _ui.update { it.copy(selected = null) }
    }

    fun storeSelected() {
        val i = _ui.value.selected ?: return
        apply(Engine.store(_state.value, i))
        _ui.update { it.copy(selected = null) }
    }

    fun unstore(slot: Int) = apply(Engine.unstore(_state.value, slot))
    fun buySlot() = apply(Engine.buyStorageSlot(_state.value))
    fun placePending() = apply(Engine.placePending(_state.value))
    fun turnInSelected() {
        val i = _ui.value.selected ?: return
        apply(Engine.turnInEvent(_state.value, i))
        _ui.update { it.copy(selected = null) }
    }

    fun serve(orderId: Long) {
        val before = _state.value
        val o = Engine.serve(before, orderId, today(), rnd)
        apply(o)
        if (o.fx.any { it is Fx.OrderDone }) {
            _ui.update { it.copy(selected = null) }
            if (before.tutorial <= 2) advanceTutorial(3)
        }
    }

    fun skipRecharge(gid: String) {
        apply(Engine.skipCooldown(_state.value, gid, now()))
        closeDialog()
    }

    // ---------------------------------------------------------------- tutorial

    fun advanceTutorial(to: Int) = _state.update { if (it.tutorial < to) it.copy(tutorial = to) else it }

    fun startTutorialIfNeeded() { if (_state.value.tutorial == 0) advanceTutorial(1) }

    // ---------------------------------------------------------------- bakery

    fun doTask(id: String) {
        val before = _state.value
        val o = Engine.completeTask(before, id)
        apply(o)
        if (id in o.state.tasksDone && id !in before.tasksDone) {
            sfx?.play(Sound.SPARKLE); haptics?.celebrate()
            val t = Story.task(id)!!
            val lines = t.after.toMutableList()
            val ch = Story.chapterOf(id)
            val next = Story.chapters.getOrNull(Story.chapters.indexOf(ch) + 1)
            if (ch.tasks.all { it.id in o.state.tasksDone } && next != null) lines += next.intro
            pushDialog(Dialog.Talk(lines, t.title))
            if (before.tutorial == 4) advanceTutorial(5)
        }
    }

    // ---------------------------------------------------------------- daily, shop, pass, event

    fun claimDaily() {
        apply(Engine.claimDaily(_state.value, today()))
        closeDialog()
    }

    fun claimFreeTreat() = apply(Engine.claimFreeTreat(_state.value, today()))
    fun claimPass(tier: Int, vip: Boolean) = apply(Engine.claimPass(_state.value, tier, vip, today()))
    fun claimEvent(m: Int) {
        val before = _state.value
        apply(Engine.claimEvent(before, m))
        val ev = Events.byId(before.eventId) ?: return
        val id = ev.decorId(m)
        if (id in _state.value.eventDecor && id !in before.eventDecor) {
            ev.decor.getOrNull(m)?.let { (title, lines) -> pushDialog(Dialog.Talk(lines, title)) }
        }
    }

    private val mandyLines = listOf(
        "Does it smell like cinnamon rolls in here, or is it just me?", "Every corner of this place has a story now.",
        "Surf, that is NOT your roll.", "I could stand here all day. Well, until the oven timer goes off.",
    )
    private val surfLines = listOf("Woof!", "*tail wagging intensifies*", "*rolls over for belly rubs*", "*sniffs your hand for crumbs*")

    /** Tapped something in a scene: replay that decoration's story moment. */
    fun sceneTap(layer: com.surfswirl.bakery.ui.screens.SceneLayer) {
        sfx?.play(Sound.TAP)
        when {
            layer.id.startsWith("surf") -> { woof(); pushDialogFront(Dialog.Talk(listOf(com.surfswirl.bakery.game.Line("surf", surfLines.random(rnd))))) }
            layer.id.startsWith("mandy") -> pushDialogFront(Dialog.Talk(listOf(com.surfswirl.bakery.game.Line("mandy", mandyLines.random(rnd)))))
            layer.show.startsWith("done:") -> {
                val id = layer.show.removePrefix("done:")
                Story.task(id)?.let { pushDialogFront(Dialog.Talk(it.after, it.title)); return }
                for (ev in Events.events) ev.decor.forEachIndexed { i, (title, lines) ->
                    if (ev.decorId(i) == id) { pushDialogFront(Dialog.Talk(lines, title)); return }
                }
            }
            layer.show.startsWith("undone:") -> toast("Fix this up with a renovation task!")
        }
    }

    fun updateAvatar(f: (com.surfswirl.bakery.game.Avatar) -> com.surfswirl.bakery.game.Avatar) {
        sfx?.play(Sound.TAP)
        updateSettings { it.copy(avatar = f(it.avatar)) }
    }

    fun updateSettings(f: (com.surfswirl.bakery.game.Settings) -> com.surfswirl.bakery.game.Settings) {
        _state.update { it.copy(settings = f(it.settings)) }
        applySettings()
    }

    fun theme() = Events.themeFor(_state.value.settings, today())

    fun exportSave(): String = SaveCodec.encode(_state.value)

    fun importSave(text: String): Boolean {
        val s = runCatching { SaveCodec.decode(text) }.getOrNull() ?: return false
        if (s.board.size != _state.value.board.size) return false
        _state.value = s
        applySettings()
        refresh()
        toast("Save restored!")
        return true
    }

    fun resetGame() {
        _state.value = Engine.newGame(now(), today(), rnd).copy(settings = _state.value.settings)
        _ui.value = Ui()
        pushDialog(Dialog.Talk(Story.intro, "Welcome to Maple Lane"))
        _state.update { it.copy(seenStory = it.seenStory + "intro") }
    }

    // ---------------------------------------------------------------- puzzles

    fun buy(offer: Offer) {
        val s = _state.value
        if (offer.id == "vip" && Engine.vip(s, today())) { toast("VIP is already active this month!"); return }
        if (Engine.stockLeft(s, offer) <= 0) { toast("Sold out for today. Fresh stock tomorrow!"); return }
        startPuzzles(Purpose.Buy(offer), offer.effort, offer.puzzles)
    }

    fun rechargeWithPuzzle(gid: String) {
        closeDialog()
        startPuzzles(Purpose.Recharge(gid), Economy.rechargeOffer("").effort, 1)
    }

    fun energyRescue() {
        closeDialog()
        buy(Economy.energyRescue)
    }

    private fun startPuzzles(p: Purpose, effort: Double, total: Int) {
        _ui.update { it.copy(puzzle = PuzzleSession(p, effort, total)) }
        pushDialog(Dialog.PickPuzzle)
    }

    /** Puzzle types the player may choose for the current step (VIP needs different categories). */
    fun puzzleChoices(): List<PuzzleType> {
        val p = _ui.value.puzzle ?: return emptyList()
        return PuzzleType.entries.filter { it.category !in p.usedCategories }
    }

    fun puzzleLevel(t: PuzzleType): Int = Brain.level(_state.value, t, _ui.value.puzzle?.effort ?: 3.0)

    fun pickPuzzle(t: PuzzleType?) {
        val choices = puzzleChoices()
        val type = t ?: choices.random(rnd)
        closeDialog()
        _ui.update {
            val p = it.puzzle ?: return@update it
            it.copy(screen = Screen.PUZZLE, puzzle = p.copy(type = type, level = puzzleLevel(type), seed = rnd.nextLong(),
                startedAt = now()))
        }
        sfx?.play(Sound.WHOOSH)
    }

    fun cancelPuzzleChoice() {
        closeDialog()
        _ui.update { it.copy(puzzle = null) }
    }

    /** Called by a puzzle screen when it ends. [seconds] is active time (the screen pauses it in the background). */
    fun puzzleDone(solved: Boolean, mistakes: Int, hints: Int, seconds: Int) {
        val p = _ui.value.puzzle ?: return
        val type = p.type ?: return
        val (ns, bonus) = Engine.recordPuzzle(_state.value, type, p.level, solved, mistakes, hints, seconds, now())
        _state.value = ns
        if (!solved) {
            sfx?.play(Sound.WRONG)
            pushDialog(Dialog.PuzzleFailed(type))
            return
        }
        sfx?.play(Sound.CORRECT); haptics?.celebrate()
        val np = p.copy(solved = p.solved + 1, usedCategories = p.usedCategories + type.category, bonuses = p.bonuses + if (bonus) 1 else 0)
        if (np.solved < np.total) {
            _ui.update { it.copy(puzzle = np) }
            toast("Great! ${np.total - np.solved} to go.")
            pushDialog(Dialog.PickPuzzle)
            return
        }
        finishPurpose(np)
    }

    private fun finishPurpose(p: PuzzleSession) {
        val lucky = p.bonuses > 0
        val mult = if (lucky) 1.25 else 1.0
        val back = when (p.purpose) { is Purpose.Buy -> if (p.purpose.offer.id == "energy_rescue") Screen.BOARD else Screen.SHOP; is Purpose.Recharge -> Screen.BOARD }
        _ui.update { it.copy(puzzle = null, screen = back) }
        when (val pp = p.purpose) {
            is Purpose.Buy -> {
                val o = Engine.buyOffer(_state.value, pp.offer, mult, today())
                _state.value = o.state
                val r = (o.fx.firstOrNull { it is Fx.Granted } as? Fx.Granted)?.reward ?: pp.offer.reward
                handleFx(Fx.Granted(r))
                pushDialog(Dialog.Rewarded(r, if (pp.offer.id == "vip") "VIP unlocked!" else pp.offer.title, lucky))
            }
            is Purpose.Recharge -> {
                var s = Engine.rechargeNow(_state.value, pp.genId)
                val extra = if (lucky) Reward(energy = 10) else Reward()
                s = Engine.grant(s, extra)
                _state.value = s
                handleFx(Fx.Granted(extra))
                pushDialog(Dialog.Rewarded(extra, "${Content.generator(pp.genId)?.name ?: "Generator"} recharged!", lucky))
            }
        }
    }

    fun retryPuzzle() {
        closeDialog()
        _ui.update { u -> u.copy(puzzle = u.puzzle?.copy(type = null)) }
        pushDialog(Dialog.PickPuzzle)
    }

    fun quitPuzzle() {
        _ui.update { it.copy(puzzle = null, screen = Screen.BOARD, dialogs = it.dialogs.filter { d -> d !is Dialog.PuzzleFailed && d !is Dialog.PickPuzzle }) }
    }

    fun leaveFailedPuzzle() {
        closeDialog()
        quitPuzzle()
    }

    companion object {
        /** Tests can replace the clock and random source. */
        var testNow: (() -> Long)? = null
        var testToday: (() -> LocalDate)? = null
        var testSeed: Long? = null

        fun factory(app: BakeryApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = GameViewModel(
                app.store, app.sfx, app.haptics, app.music,
                testNow ?: { System.currentTimeMillis() },
                testToday ?: { LocalDate.now() },
                testSeed?.let { Random(it) } ?: Random.Default,
            ) as T
        }
    }
}
