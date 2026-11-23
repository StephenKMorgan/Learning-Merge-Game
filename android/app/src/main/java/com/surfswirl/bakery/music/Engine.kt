package com.surfswirl.bakery.music

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.tanh

/** A note the composer wants played. [beat] is from the start of the bar. */
class NoteEvent(val beat: Double, val part: Int, val midi: Int, val beats: Double, val vel: Float)

private val QUAL = mapOf(
    "maj7" to intArrayOf(0, 4, 7, 11, 14), "m7b5" to intArrayOf(0, 3, 6, 10, 14), "m7" to intArrayOf(0, 3, 7, 10, 14),
    "7sus" to intArrayOf(0, 5, 7, 10, 14), "7" to intArrayOf(0, 4, 7, 10, 14), "m6" to intArrayOf(0, 3, 7, 9, 14),
    "6" to intArrayOf(0, 4, 7, 9, 14), "maj" to intArrayOf(0, 4, 7, 12, 14), "m" to intArrayOf(0, 3, 7, 12, 14),
)
private val NOTES = mapOf("C" to 0, "Db" to 1, "D" to 2, "Eb" to 3, "E" to 4, "F" to 5, "Gb" to 6, "G" to 7, "Ab" to 8, "A" to 9, "Bb" to 10, "B" to 11)

class Chord(val root: Int, val iv: IntArray) {
    fun tones() = iv.take(4).map { (root + it) % 12 }.toSet()
    companion object {
        fun parse(sym: String): Chord {
            for (q in QUAL.keys.sortedByDescending { it.length }) if (sym.endsWith(q)) {
                val r = sym.removeSuffix(q)
                if (r in NOTES) return Chord(NOTES[r]!!, QUAL[q]!!)
            }
            return Chord(NOTES[sym] ?: 0, QUAL["maj"]!!)
        }
    }
}

/**
 * Writes the music one bar at a time: chords from the style, and fresh melodies built from short
 * motifs that repeat and vary, so it sounds composed rather than random, and never exactly repeats.
 */
class Composer(val style: Style, seed: Long) {
    private val rng = Rng(seed)
    private val chords: List<Chord>
    init {
        val all = mutableListOf<Chord>()
        for (sec in style.form) all += style.sections[sec]!!.map { Chord.parse(it) }
        chords = all
    }
    val barsPerLoop get() = chords.size
    private val scale = (0 until 128).filter { m ->
        val d = ((m - style.key) % 12 + 12) % 12
        d in (if (style.minor) setOf(0, 2, 3, 5, 7, 8, 10) else setOf(0, 2, 4, 5, 7, 9, 11))
    }

    private var motif: List<Triple<Double, Double, Int>> = emptyList()   // (beat in 2-bar span, length, step delta)
    private var phraseNotes = mutableMapOf<Int, List<NoteEvent>>()
    private var melodyPitch = (style.melodyLow + style.melodyHigh) / 2
    private var phraseCount = 0

    fun chordAt(bar: Int) = chords[bar % chords.size]

    private fun sw(beat: Double): Double {
        val f = beat - kotlin.math.floor(beat)
        return if (abs(f - 0.5) < 1e-6) kotlin.math.floor(beat) + style.swing else beat
    }

    private fun voicing(c: Chord, low: Int = 53): List<Int> {
        val base = low + ((c.root - low) % 12 + 12) % 12
        return c.iv.drop(1).map { iv ->
            var m = base + iv
            while (m < low) m += 12
            while (m > low + 17) m -= 12
            m
        }.sorted()
    }

    private fun bassRoot(c: Chord) = 36 + ((c.root - 36) % 12 + 12) % 12

    private fun nearestScale(m: Int) = scale.minByOrNull { abs(it - m) } ?: m

    /** Notes for [bar] for the parts that are switched on. */
    fun bar(bar: Int, active: BooleanArray): List<NoteEvent> {
        val out = mutableListOf<NoteEvent>()
        val c = chordAt(bar)
        val next = chordAt(bar + 1)
        if (bar % 4 == 0) makePhrase(bar)
        for ((pi, p) in style.parts.withIndex()) {
            if (!active[pi]) continue
            fun add(beat: Double, midi: Int, beats: Double, vel: Float) {
                val v = vel * (0.88f + rng.float() * 0.24f)
                out += NoteEvent(beat, pi, midi + p.shift, beats, v)
            }
            when (p.role) {
                Role.PAD -> for (m in voicing(c, 55)) add(0.0, m, 4.1, 0.8f)
                Role.COMP -> comp(p.pattern, c, bar, ::add)
                Role.BASS -> bass(p.pattern, c, next, bar, ::add)
                Role.DRUMS -> drums(p.pattern, bar, pi, out)
                Role.ARP -> {
                    val v = voicing(c, 62)
                    val seq = listOf(0, 1, 2, 3, 2, 1, 0, 1)
                    for (i in 0 until 8) add(sw(i * 0.5), v[seq[i] % v.size] + 12, 0.9, if (i % 2 == 0) 0.8f else 0.6f)
                }
                Role.MELODY -> phraseNotes[bar]?.forEach { n ->
                    if (p.pattern == "melodySparse" && n.beat % 1.0 != 0.0 && rng.chance(0.5)) return@forEach
                    add(sw(n.beat), n.midi, n.beats, n.vel)
                    if (p.double) add(sw(n.beat) + 0.01, n.midi - 12, n.beats, n.vel * 0.4f)
                }
                Role.COUNTER -> if (bar % 2 == 0) {
                    val tones = voicing(c, 64)
                    add(0.0, tones[0] + 12, 1.9, 0.7f); add(2.0, tones[2 % tones.size] + 12, 1.9, 0.6f)
                }
                Role.EXTRA -> if (p.pattern == "offbeats") for (i in 0 until 4) add(sw(i + 0.5), 80, 0.2, 0.8f)
            }
        }
        return out
    }

    private fun comp(pattern: String, c: Chord, bar: Int, add: (Double, Int, Double, Float) -> Unit) {
        val v = voicing(c)
        fun chord(beat: Double, len: Double, vel: Float, stagger: Double = 0.02) = v.forEachIndexed { k, m -> add(beat + k * stagger, m, len, vel) }
        when (pattern) {
            "swing" -> {
                chord(0.0, 1.4, 1f); chord(sw(1.5), 1.2, 0.9f)
                if (bar % 2 == 1 || rng.chance(0.3)) chord(3.0, 0.8, 0.75f)
                if (rng.chance(0.2)) chord(sw(2.5), 0.4, 0.6f)
            }
            "bossa" -> {
                val pat = if (bar % 2 == 0) listOf(0.0, 1.0, 2.5, 3.5) else listOf(0.5, 1.5, 2.0, 3.0)
                for (b in pat) chord(b, 0.8, 0.9f, 0.025)
            }
            "sparse" -> { chord(0.0, 3.6, 0.9f, 0.05); if (rng.chance(0.4)) chord(sw(2.5), 1.4, 0.6f, 0.04) }
            "fingerpick" -> {
                val r = bassRoot(c) + 12
                val seq = listOf(r, v[0], v[1], v[0], r + 7, v[1], v[2 % v.size], v[1])
                for (i in 0 until 8) add(sw(i * 0.5), seq[i], 1.2, if (i % 4 == 0) 1f else 0.7f)
            }
            "strum" -> {
                val accents = listOf(1f, 0f, 0.8f, 0.6f, 0f, 0.6f, 0.9f, 0.6f)
                for (i in 0 until 8) if (accents[i] > 0) chord(sw(i * 0.5), 0.45, accents[i], if (i % 2 == 0) 0.012 else -0.0)
            }
            "staccato" -> for (b in listOf(0.0, 1.5, 2.0, 3.5)) chord(sw(b), 0.3, 0.9f, 0.0)
        }
    }

    private fun bass(pattern: String, c: Chord, next: Chord, bar: Int, add: (Double, Int, Double, Float) -> Unit) {
        val r = bassRoot(c)
        when (pattern) {
            "walk" -> {
                val nr = bassRoot(next)
                val third = r + if (c.iv[1] == 3) 3 else 4
                val walk = listOf(r, if (rng.chance(0.5)) r + 7 else third, if (rng.chance(0.5)) r + 12 else r + 7, nr + if (rng.chance(0.5)) 1 else -1)
                walk.forEachIndexed { i, m -> add(i.toDouble(), m, 0.9, if (i == 0) 1f else 0.85f) }
            }
            "bossa" -> listOf(0.0 to r, 1.5 to r + 7, 2.0 to r + 7, 3.5 to r).forEach { (b, m) -> add(b, m, 0.7, 0.9f) }
            "roots" -> { add(0.0, r, 1.9, 1f); add(2.0, if (rng.chance(0.6)) r + 7 else r, 1.9, 0.85f) }
            "pedal" -> add(0.0, r, 3.9, 0.9f)
            "folk" -> { add(0.0, r, 0.9, 1f); add(1.0, r + 7, 0.9, 0.8f); add(2.0, r, 0.9, 0.9f); add(3.0, r + 7, 0.9, 0.8f) }
        }
    }

    private fun drums(kit: String, bar: Int, pi: Int, out: MutableList<NoteEvent>) {
        fun hit(beat: Double, inst: Inst, vel: Float) {
            out += NoteEvent(beat, pi, -inst.ordinal - 1, 0.2, vel * (0.85f + rng.float() * 0.3f))
        }
        when (kit) {
            "brush" -> for (i in 0 until 4) {
                if (i % 2 == 0) hit(i.toDouble(), Inst.KICK, 0.38f)
                if (i % 2 == 1) hit(i.toDouble(), Inst.BRUSH, 0.07f)
                hit(i.toDouble(), Inst.TICK, 0.035f); hit(sw(i + 0.5), Inst.TICK, 0.025f)
            }
            "brushLight" -> for (i in 0 until 4) {
                if (i % 2 == 1) hit(i.toDouble(), Inst.BRUSH, 0.05f)
                hit(i.toDouble(), Inst.TICK, 0.02f)
            }
            "bossa" -> {
                hit(0.0, Inst.KICK, 0.25f); hit(2.0, Inst.KICK, 0.2f)
                for (i in 0 until 8) hit(i * 0.5, Inst.SHAKER, if (i % 2 == 1) 0.045f else 0.03f)
                for (b in if (bar % 2 == 0) listOf(0.5, 2.0, 3.5) else listOf(1.0, 2.5)) hit(b, Inst.RIM, 0.06f)
            }
            "folk" -> {
                hit(0.0, Inst.KICK, 0.35f); hit(2.0, Inst.KICK, 0.3f)
                hit(1.0, Inst.SNAP, 0.08f); hit(3.0, Inst.SNAP, 0.08f)
                for (i in 0 until 8) hit(i * 0.5, Inst.SHAKER, if (i % 2 == 1) 0.04f else 0.025f)
            }
            "island" -> {
                hit(0.0, Inst.KICK, 0.3f)
                for (i in 0 until 8) hit(sw(i * 0.5), Inst.SHAKER, if (i % 2 == 1) 0.045f else 0.03f)
                hit(sw(1.5), Inst.RIM, 0.06f); hit(sw(3.5), Inst.RIM, 0.06f)
            }
            "light" -> for (i in 0 until 8) hit(i * 0.5, Inst.SHAKER, if (i % 2 == 1) 0.035f else 0.02f)
        }
    }

    /** A 4-bar phrase: motif, motif, motif varied, then a cadence. Every 4th phrase rests. */
    private fun makePhrase(startBar: Int) {
        phraseCount++
        phraseNotes.keys.removeAll { it < startBar - 8 }
        if (phraseCount % 4 == 0) { for (b in 0 until 4) phraseNotes[startBar + b] = emptyList(); return }
        val rhythms = listOf(
            listOf(0.0 to 1.5, 1.5 to 0.5, 2.0 to 1.0, 3.0 to 1.0),
            listOf(0.0 to 1.0, 1.0 to 0.5, 1.5 to 1.5, 3.0 to 1.0),
            listOf(0.5 to 0.5, 1.0 to 1.0, 2.0 to 2.0),
            listOf(0.0 to 2.0, 2.0 to 1.0, 3.0 to 0.5, 3.5 to 0.5),
            listOf(0.0 to 0.5, 0.5 to 0.5, 1.0 to 1.0, 2.5 to 1.5),
        )
        // keep the motif half the time, so a theme comes back before it moves on
        if (motif.isEmpty() || phraseCount % 2 == 1) {
            val r1 = rng.pick(rhythms); val r2 = rng.pick(rhythms)
            motif = r1.map { Triple(it.first, it.second, rng.pick(listOf(-2, -1, -1, 1, 1, 2, 0, 3, -3))) } +
                r2.map { Triple(it.first + 4, it.second, rng.pick(listOf(-2, -1, 1, 1, 2, -1))) }
        }
        val bars = Array(4) { mutableListOf<NoteEvent>() }
        fun place(bar: Int, beat: Double, len: Double, step: Int, strong: Boolean, vel: Float) {
            val c = chordAt(startBar + bar)
            val idx = scale.indexOf(nearestScale(melodyPitch))
            var m = scale[(idx + step).coerceIn(0, scale.size - 1)]
            if (m > style.melodyHigh) m = scale[(scale.indexOf(nearestScale(m)) - 4).coerceAtLeast(0)]
            if (m < style.melodyLow) m = scale[(scale.indexOf(nearestScale(m)) + 4).coerceAtMost(scale.size - 1)]
            if (strong) {
                val tones = c.tones()
                m = (m - 3..m + 3).filter { it % 12 in tones && it in scale }.minByOrNull { abs(it - m) } ?: m
            }
            melodyPitch = m
            bars[bar] += NoteEvent(beat, 0, m, len, vel)
        }
        val variation = rng.int(3)
        for ((beat, len, step) in motif) {
            val bar = (beat / 4).toInt(); val b = beat % 4
            place(bar, b, len, step, b == 0.0 || b == 2.0, if (b == 0.0) 1f else 0.85f)
        }
        // bar 3: motif bar 1 again (maybe shifted), for the "same tune" feeling
        for ((beat, len, step) in motif.filter { it.first < 4 }) {
            place(2, beat, len, step + if (variation == 0) 0 else if (variation == 1) 1 else -1, beat == 0.0 || beat == 2.0, 0.9f)
        }
        // bar 4: a cadence on a chord tone
        place(3, 0.0, 1.0, rng.pick(listOf(-1, 1)), true, 0.9f)
        place(3, 1.0, 2.5, rng.pick(listOf(-1, 0, 1)), true, 0.95f)
        for (b in 0 until 4) phraseNotes[startBar + b] = bars[b]
    }
}

/**
 * Plays a [Style] live: schedules bars ahead of time, starts voices on time, fades parts in and out
 * as the intensity changes, and crossfades between styles. Call [render] from the audio thread.
 */
class MusicEngine(seed: Long = System.nanoTime()) {
    private val seedRng = Rng(seed)
    private var style: Style = Styles.cafe
    private var composer = Composer(style, seedRng.next())
    private var pendingStyles: List<Style>? = null
    private var styleGroup: List<Style> = listOf(Styles.cafe)
    private var groupIndex = 0
    private var barsInStyle = 0

    private val reverb = Reverb()
    private var sample = 0L
    private var nextBar = 0L
    private var bar = 0
    private val queue = ArrayList<Pair<Long, NoteEvent>>()
    private val voices = ArrayList<Pair<Int, Voice>>()

    private var partGain = FloatArray(8)
    private var partTarget = FloatArray(8)
    private var master = 0f
    private var masterTarget = 1f
    private val bufL = Array(8) { FloatArray(BLOCK) }
    private val bufR = Array(8) { FloatArray(BLOCK) }
    private val mixL = FloatArray(BLOCK)
    private val mixR = FloatArray(BLOCK)
    private val voiceRng = Rng(seed + 1)

    /** 0 = calm (just the comp), 1 = full band. Set from any thread. */
    @Volatile var intensity = 0.5f

    /** Styles to play in turn (a new one every couple of minutes). Changes crossfade in. */
    fun setStyles(styles: List<Style>) {
        if (styles.map { it.id } == styleGroup.map { it.id }) return
        pendingStyles = styles
        masterTarget = 0f
    }

    val currentStyle get() = style.id
    private val beatSamples get() = 60.0 / style.bpm * Dsp.SR

    private fun startStyle(s: Style) {
        style = s
        composer = Composer(s, seedRng.next())
        reverb.wet = s.reverb
        styleLevel = Styles.level[s.id] ?: 1f
        queue.clear(); voices.clear()
        nextBar = sample; bar = 0; barsInStyle = 0
        partGain = FloatArray(8) { 0f }
        updateTargets(snap = true)
        masterTarget = 1f
    }

    private fun updateTargets(snap: Boolean = false) {
        val level = min(intensity, style.maxIntensity)
        for ((i, p) in style.parts.withIndex()) {
            partTarget[i] = if (level >= p.from) 1f else 0f
            if (snap) partGain[i] = partTarget[i]
        }
    }

    fun render(outL: FloatArray, outR: FloatArray, frames: Int) {
        var done = 0
        while (done < frames) {
            val n = min(BLOCK, frames - done)
            renderBlock(n)
            System.arraycopy(mixL, 0, outL, done, n)
            System.arraycopy(mixR, 0, outR, done, n)
            done += n
        }
    }

    private fun renderBlock(n: Int) {
        // style changes once the old one has faded out
        if (masterTarget == 0f && master < 0.01f) {
            pendingStyles?.let { styleGroup = it; groupIndex = 0; pendingStyles = null; startStyle(it[0]) }
        }
        updateTargets()
        val blockEnd = sample + n
        // write bars ahead
        while (nextBar < blockEnd + BLOCK) {
            if (barsInStyle >= BARS_PER_STYLE && styleGroup.size > 1 && bar % 8 == 0 && pendingStyles == null && !nextStyleInGroup) {
                groupIndex = (groupIndex + 1) % styleGroup.size
                masterTarget = 0f
                nextStyleInGroup = true
            }
            val active = BooleanArray(style.parts.size) { partTarget[it] > 0f || partGain[it] > 0.01f }
            for (e in composer.bar(bar, active)) {
                val at = nextBar + (e.beat * beatSamples).toLong() + voiceRng.int(80)
                queue += at to e
            }
            nextBar += (4 * beatSamples).toLong()
            bar++; barsInStyle++
        }
        if (nextStyleInGroup && master < 0.01f) {
            nextStyleInGroup = false
            startStyle(styleGroup[groupIndex])
        }
        // start voices that begin in this block
        val it = queue.iterator()
        while (it.hasNext()) {
            val (at, e) = it.next()
            if (at < blockEnd) {
                it.remove()
                val p = style.parts.getOrNull(e.part) ?: continue
                val delay = (at - sample).coerceAtLeast(0).toInt()
                val inst = if (e.midi < 0) Inst.entries[-e.midi - 1] else p.inst
                val dur = e.beats * 60.0 / style.bpm
                voices += e.part to Voice.create(inst, if (e.midi < 0) 60 else e.midi, dur, e.vel * p.gain, p.pan, delay, voiceRng)
            }
        }
        for (k in 0 until 8) { bufL[k].fill(0f, 0, n); bufR[k].fill(0f, 0, n) }
        val vi = voices.iterator()
        while (vi.hasNext()) {
            val (part, v) = vi.next()
            if (!v.render(bufL[part], bufR[part], n)) vi.remove()
        }
        mixL.fill(0f, 0, n); mixR.fill(0f, 0, n)
        val fade = n / (1.6f * Dsp.SR)
        for (k in style.parts.indices) {
            val g0 = partGain[k]
            val g1 = if (partTarget[k] > g0) min(partTarget[k], g0 + fade) else maxOf(partTarget[k], g0 - fade)
            partGain[k] = g1
            if (g0 < 0.0005f && g1 < 0.0005f) continue
            for (j in 0 until n) {
                val g = g0 + (g1 - g0) * j / n
                mixL[j] += bufL[k][j] * g; mixR[j] += bufR[k][j] * g
            }
        }
        reverb.process(mixL, mixR, n)
        val m0 = master
        val mfade = n / (1.5f * Dsp.SR)
        master = if (masterTarget > master) min(masterTarget, master + mfade) else maxOf(masterTarget, master - mfade)
        for (j in 0 until n) {
            val g = (m0 + (master - m0) * j / n) * 0.9f * styleLevel
            mixL[j] = tanh(mixL[j] * g)
            mixR[j] = tanh(mixR[j] * g)
        }
        sample = blockEnd
    }

    private var nextStyleInGroup = false
    private var styleLevel = Styles.level["cafe"] ?: 1f

    companion object {
        const val BLOCK = 256
        /** Play each style of a group for about this many bars before moving to the next. */
        const val BARS_PER_STYLE = 64
    }
}
