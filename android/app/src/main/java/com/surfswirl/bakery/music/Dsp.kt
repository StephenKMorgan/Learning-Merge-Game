package com.surfswirl.bakery.music

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/** Shared sound-making helpers. Pure Kotlin (no Android), so the music can be rendered in tests too. */
object Dsp {
    const val SR = 32000
    private const val TABLE = 4096
    private val sinTable = FloatArray(TABLE + 1) { sin(2 * PI * it / TABLE).toFloat() }

    /** sin(2π·phase) for phase in cycles (any value). */
    fun sin1(phase: Double): Float {
        val p = phase - kotlin.math.floor(phase)
        val x = p * TABLE
        val i = x.toInt()
        val f = (x - i).toFloat()
        return sinTable[i] + (sinTable[i + 1] - sinTable[i]) * f
    }

    fun mtof(m: Int): Double = 440.0 * 2.0.pow((m - 69) / 12.0)

    /** Per-sample multiplier for an exponential decay with time constant [seconds]. */
    fun decay(seconds: Double): Float = exp(-1.0 / (seconds * SR)).toFloat()

    fun onePoleK(cutoff: Double): Float = exp(-2 * PI * cutoff / SR).toFloat()
}

/** Small fast random numbers for noise and choices (deterministic for a seed). */
class Rng(seed: Long) {
    private var s = seed xor 0x5DEECE66DL
    fun next(): Long { s = s xor (s shl 13); s = s xor (s ushr 7); s = s xor (s shl 17); return s }
    fun float(): Float = ((next() ushr 40).toFloat() / (1L shl 24).toFloat())
    fun noise(): Float = float() * 2f - 1f
    fun int(n: Int): Int = ((next() ushr 33) % n).toInt()
    fun chance(p: Double) = float() < p
    fun <T> pick(list: List<T>): T = list[int(list.size)]
}

/** A light stereo room reverb (Freeverb style: parallel combs into series allpasses). */
class Reverb(roomSize: Float = 0.78f, private val damp: Float = 0.35f, var wet: Float = 0.22f) {
    private class Comb(size: Int) { val buf = FloatArray(size); var i = 0; var store = 0f }
    private class AllPass(size: Int) { val buf = FloatArray(size); var i = 0 }
    private val scale = Dsp.SR / 44100f
    private val combL = intArrayOf(1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617).map { Comb((it * scale).toInt()) }
    private val combR = intArrayOf(1139, 1211, 1300, 1379, 1445, 1514, 1580, 1640).map { Comb((it * scale).toInt()) }
    private val apL = intArrayOf(556, 441, 341, 225).map { AllPass((it * scale).toInt()) }
    private val apR = intArrayOf(579, 464, 364, 248).map { AllPass((it * scale).toInt()) }
    private val feedback = roomSize

    private fun comb(c: Comb, x: Float): Float {
        val y = c.buf[c.i]
        c.store = y * (1 - damp) + c.store * damp
        c.buf[c.i] = x + c.store * feedback
        if (++c.i >= c.buf.size) c.i = 0
        return y
    }

    private fun allpass(a: AllPass, x: Float): Float {
        val b = a.buf[a.i]
        val y = -x + b
        a.buf[a.i] = x + b * 0.5f
        if (++a.i >= a.buf.size) a.i = 0
        return y
    }

    fun process(l: FloatArray, r: FloatArray, n: Int) {
        for (k in 0 until n) {
            val input = (l[k] + r[k]) * 0.015f
            var ol = 0f; var or = 0f
            for (c in combL) ol += comb(c, input)
            for (c in combR) or += comb(c, input)
            for (a in apL) ol = allpass(a, ol)
            for (a in apR) or = allpass(a, or)
            l[k] += ol * wet
            r[k] += or * wet
        }
    }
}
