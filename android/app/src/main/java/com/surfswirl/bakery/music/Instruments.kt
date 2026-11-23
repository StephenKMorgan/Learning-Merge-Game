package com.surfswirl.bakery.music

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Every instrument the composer can use. Add one here and in [Voice.create]. */
enum class Inst {
    RHODES, BASS, CELESTA, MARIMBA, MUSIC_BOX, VIBES, GUITAR, UKULELE, HARP, STEEL_DRUM, FLUTE, PAD, PIZZ, GLOCK,
    KICK, BRUSH, TICK, SHAKER, SLEIGH, RIM, SNAP,
}

/**
 * One playing note. Renders itself into a stereo bus, starting [delay] samples into the next block.
 * Returns false from [render] once it has faded out.
 */
abstract class Voice(val gain: Float, pan: Float, var delay: Int, val hold: Int) {
    private val gl = sqrt(0.5f * (1 - pan)) * gain
    private val gr = sqrt(0.5f * (1 + pan)) * gain
    protected var age = 0
    private var rel = 1f
    private val relK = Dsp.decay(0.08)
    open val maxLen: Int = Dsp.SR * 4

    /** Next mono sample, given the time since the note started. */
    protected abstract fun sample(): Float

    fun render(l: FloatArray, r: FloatArray, n: Int): Boolean {
        var k = 0
        if (delay >= n) { delay -= n; return true }
        k = delay; delay = 0
        while (k < n) {
            if (age > hold) { rel *= relK; if (rel < 0.0005f) return false }
            if (age > maxLen) return false
            val s = sample() * rel
            l[k] += s * gl; r[k] += s * gr
            age++; k++
        }
        return true
    }

    companion object {
        fun create(inst: Inst, midi: Int, durSec: Double, vel: Float, pan: Float, delay: Int, rng: Rng): Voice {
            val f = Dsp.mtof(midi)
            val hold = (durSec * Dsp.SR).toInt()
            return when (inst) {
                Inst.RHODES -> Rhodes(f, vel, pan, delay, hold)
                Inst.BASS -> Bass(f, vel, pan, delay, hold)
                Inst.CELESTA -> Partials(f, vel, pan, delay, hold, floatArrayOf(1f, 2f, 3.98f), floatArrayOf(1f, 0.4f, 0.2f), floatArrayOf(0.7f, 0.3f, 0.12f), 0.002)
                Inst.MARIMBA -> Partials(f, vel, pan, delay, hold, floatArrayOf(1f, 3.93f, 9.2f), floatArrayOf(1f, 0.25f, 0.1f), floatArrayOf(0.32f, 0.05f, 0.015f), 0.001)
                Inst.MUSIC_BOX -> Partials(f, vel, pan, delay, hold, floatArrayOf(1f, 2f, 5.4f), floatArrayOf(1f, 0.3f, 0.15f), floatArrayOf(0.9f, 0.25f, 0.06f), 0.001)
                Inst.GLOCK -> Partials(f, vel, pan, delay, hold, floatArrayOf(1f, 2.76f, 5.4f), floatArrayOf(1f, 0.35f, 0.15f), floatArrayOf(1.1f, 0.2f, 0.05f), 0.001)
                Inst.VIBES -> Vibes(f, vel, pan, delay, hold)
                Inst.GUITAR -> Pluck(f, vel, pan, delay, hold, 0.996f, 0.5f, rng)
                Inst.UKULELE -> Pluck(f, vel, pan, delay, hold, 0.992f, 0.75f, rng)
                Inst.HARP -> Pluck(f, vel, pan, delay, hold, 0.998f, 0.65f, rng)
                Inst.PIZZ -> Pluck(f, vel, pan, delay, hold, 0.985f, 0.4f, rng)
                Inst.STEEL_DRUM -> SteelDrum(f, vel, pan, delay, hold)
                Inst.FLUTE -> Flute(f, vel, pan, delay, hold, rng)
                Inst.PAD -> Pad(f, vel, pan, delay, hold)
                Inst.KICK -> Kick(vel, pan, delay)
                Inst.BRUSH -> Noise(vel, pan, delay, 0.3, 1200.0, 5000.0, 0.03, 0.13, rng)
                Inst.TICK -> Noise(vel, pan, delay, 0.05, 6000.0, 15000.0, 0.0005, 0.012, rng)
                Inst.SHAKER -> Noise(vel, pan, delay, 0.09, 5000.0, 14000.0, 0.015, 0.03, rng)
                Inst.RIM -> Rim(vel, pan, delay, rng)
                Inst.SNAP -> Noise(vel, pan, delay, 0.08, 1500.0, 8000.0, 0.001, 0.02, rng)
                Inst.SLEIGH -> Sleigh(vel, pan, delay, rng)
            }
        }
    }
}

private class Rhodes(f: Double, vel: Float, pan: Float, delay: Int, hold: Int) : Voice(vel, pan, delay, hold) {
    private val inc = f / Dsp.SR
    private var p = 0.0; private var pb = 0.0; private var pt = 0.0
    private var idx = 0.6f; private val idxK = Dsp.decay(0.35)
    private var env = 1f; private val envK = Dsp.decay(1.2)
    private var tine = 0.12f; private val tineK = Dsp.decay(0.05)
    override fun sample(): Float {
        val atk = min(1f, age / (0.003f * Dsp.SR))
        val m = Dsp.sin1(p)
        val s = Dsp.sin1(p + idx * m / 6.283f) + tine * Dsp.sin1(pt) + 0.3f * Dsp.sin1(pb)
        p += inc; pb += inc * 1.003; pt += inc * 7.1
        idx *= idxK; tine *= tineK; env *= envK
        return s * env * atk * 0.55f
    }
}

private class Bass(f: Double, vel: Float, pan: Float, delay: Int, hold: Int) : Voice(vel, pan, delay, hold) {
    private val inc = f / Dsp.SR
    private var p = 0.0
    private var env = 1f; private val envK = Dsp.decay(0.9)
    private var thump = 0.5f; private val thumpK = Dsp.decay(0.03)
    override fun sample(): Float {
        val atk = min(1f, age / (0.008f * Dsp.SR))
        val s = Dsp.sin1(p) * (1 + thump) + 0.35f * Dsp.sin1(2 * p) + 0.12f * Dsp.sin1(3 * p)
        p += inc; env *= envK; thump *= thumpK
        return s * env * atk * 0.6f
    }
}

/** A few sine partials, each with its own decay: bells, celesta, marimba, music box. */
private class Partials(f: Double, vel: Float, pan: Float, delay: Int, hold: Int,
                       private val ratio: FloatArray, amp: FloatArray, decays: FloatArray, private val attack: Double) : Voice(vel, pan, delay, hold) {
    private val inc = f / Dsp.SR
    private var p = 0.0
    private val a = amp.copyOf()
    private val k = FloatArray(decays.size) { Dsp.decay(decays[it].toDouble()) }
    override val maxLen = (Dsp.SR * (decays.max() * 7)).toInt()
    override fun sample(): Float {
        val atk = min(1f, age / (attack.toFloat() * Dsp.SR))
        var s = 0f
        for (i in ratio.indices) { s += a[i] * Dsp.sin1(p * ratio[i]); a[i] *= k[i] }
        p += inc
        return s * atk * 0.5f
    }
}

private class Vibes(f: Double, vel: Float, pan: Float, delay: Int, hold: Int) : Voice(vel, pan, delay, hold) {
    private val inc = f / Dsp.SR
    private var p = 0.0; private var trem = 0.0
    private var env = 1f; private val envK = Dsp.decay(1.4)
    private var hi = 0.3f; private val hiK = Dsp.decay(0.08)
    override fun sample(): Float {
        val s = Dsp.sin1(p) + hi * Dsp.sin1(p * 4)
        val t = 0.82f + 0.18f * Dsp.sin1(trem)
        p += inc; trem += 5.5 / Dsp.SR; env *= envK; hi *= hiK
        return s * env * t * 0.45f * min(1f, age / 40f)
    }
}

/** Karplus-Strong plucked string: guitar, ukulele, harp, pizzicato. */
private class Pluck(f: Double, vel: Float, pan: Float, delay: Int, hold: Int, private val fb: Float, bright: Float, rng: Rng) : Voice(vel, pan, delay, hold) {
    private val n = max(2, (Dsp.SR / f).toInt())
    private val buf = FloatArray(n) { rng.noise() }
    private var i = 0
    private val b = bright
    init {
        // soften the initial noise burst a little for a rounder tone
        for (k in 1 until n) buf[k] = buf[k] * b + buf[k - 1] * (1 - b)
    }
    override val maxLen = Dsp.SR * 3
    override fun sample(): Float {
        val j = if (i + 1 >= n) 0 else i + 1
        val out = buf[i]
        // the classic KS low-pass, blended with the raw sample for brighter strings
        buf[i] = fb * (b * 0.5f * (out + buf[j]) + (1 - b) * (0.25f * out + 0.5f * buf[j] + 0.25f * buf[if (j + 1 >= n) 0 else j + 1]))
        i = j
        return out * 0.55f
    }
}

private class SteelDrum(f: Double, vel: Float, pan: Float, delay: Int, hold: Int) : Voice(vel, pan, delay, hold) {
    private val inc = f / Dsp.SR
    private var p = 0.0
    private var env = 1f; private val envK = Dsp.decay(0.55)
    private var a2 = 0.5f; private val a2K = Dsp.decay(0.25)
    private var a3 = 0.35f; private val a3K = Dsp.decay(0.1)
    override fun sample(): Float {
        val bend = 1 + 0.02 * kotlin.math.exp(-age / (0.01 * Dsp.SR))
        val s = Dsp.sin1(p) + a2 * Dsp.sin1(p * 2.0) + a3 * Dsp.sin1(p * 3.01)
        p += inc * bend; env *= envK; a2 *= a2K; a3 *= a3K
        return s * env * min(1f, age / 30f) * 0.45f
    }
}

private class Flute(f: Double, vel: Float, pan: Float, delay: Int, hold: Int, private val rng: Rng) : Voice(vel, pan, delay, hold) {
    private val inc = f / Dsp.SR
    private var p = 0.0; private var vib = 0.0
    private var lp = 0f
    override fun sample(): Float {
        val atk = min(1f, age / (0.06f * Dsp.SR))
        val v = 1 + 0.005 * Dsp.sin1(vib) * min(1f, age / (0.3f * Dsp.SR))
        lp = lp * 0.9f + rng.noise() * 0.1f
        val s = Dsp.sin1(p) + 0.18f * Dsp.sin1(p * 2) + 0.05f * Dsp.sin1(p * 3) + lp * 0.25f
        p += inc * v; vib += 5.0 / Dsp.SR
        return s * atk * 0.4f
    }
}

/** Soft strings-like pad: detuned additive saws with a slow swell. */
private class Pad(f: Double, vel: Float, pan: Float, delay: Int, hold: Int) : Voice(vel, pan, delay, hold) {
    private val inc = f / Dsp.SR
    private var p1 = 0.0; private var p2 = 0.37
    override val maxLen = hold + Dsp.SR
    override fun sample(): Float {
        val atk = min(1f, age / (0.5f * Dsp.SR))
        var s = 0f
        for (h in 1..4) { val a = 1f / h; s += a * (Dsp.sin1(p1 * h) + Dsp.sin1(p2 * h)) }
        p1 += inc * 0.997; p2 += inc * 1.003
        return s * atk * 0.18f
    }
}

private class Kick(vel: Float, pan: Float, delay: Int) : Voice(vel, pan, delay, Dsp.SR / 3) {
    private var p = 0.0
    private var env = 1f; private val envK = Dsp.decay(0.12)
    override val maxLen = Dsp.SR / 2
    override fun sample(): Float {
        val t = age.toDouble() / Dsp.SR
        val f = 110 * kotlin.math.exp(-t / 0.05) + 45
        p += f / Dsp.SR; env *= envK
        return Dsp.sin1(p) * env
    }
}

/** Filtered noise bursts: brushes, hi-hat ticks, shakers, snaps. */
private class Noise(vel: Float, pan: Float, delay: Int, len: Double, hp: Double, lpf: Double, private val atk: Double, dec: Double, private val rng: Rng) : Voice(vel, pan, delay, (len * Dsp.SR).toInt()) {
    private val hk = Dsp.onePoleK(hp); private val lk = Dsp.onePoleK(lpf)
    private var h = 0f; private var l = 0f
    private var env = 1f; private val envK = Dsp.decay(dec)
    override val maxLen = (len * Dsp.SR).toInt() + Dsp.SR / 10
    override fun sample(): Float {
        val x = rng.noise()
        h = h * hk + x * (1 - hk)
        val band = x - h
        l = l * lk + band * (1 - lk)
        val a = min(1f, age / (atk.toFloat() * Dsp.SR + 1))
        env *= envK
        return l * a * env * 1.6f
    }
}

private class Rim(vel: Float, pan: Float, delay: Int, private val rng: Rng) : Voice(vel, pan, delay, Dsp.SR / 20) {
    private var p = 0.0
    private var env = 1f; private val envK = Dsp.decay(0.018)
    override val maxLen = Dsp.SR / 8
    override fun sample(): Float {
        p += 1700.0 / Dsp.SR; env *= envK
        return (Dsp.sin1(p) * 0.6f + rng.noise() * 0.4f) * env
    }
}

private class Sleigh(vel: Float, pan: Float, delay: Int, rng: Rng) : Voice(vel, pan, delay, Dsp.SR / 4) {
    private val fs = DoubleArray(6) { 5000.0 + rng.float() * 4000 }
    private val ps = DoubleArray(6) { rng.float().toDouble() }
    private var env = 1f; private val envK = Dsp.decay(0.06)
    private var am = 0.0
    override val maxLen = Dsp.SR / 3
    override fun sample(): Float {
        var s = 0f
        for (i in 0 until 6) { s += Dsp.sin1(ps[i]); ps[i] += fs[i] / Dsp.SR }
        am += 22.0 / Dsp.SR; env *= envK
        return s * env * (1 + 0.5f * Dsp.sin1(am)) * 0.18f
    }
}
