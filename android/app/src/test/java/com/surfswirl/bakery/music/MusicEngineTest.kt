package com.surfswirl.bakery.music

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Renders listening samples of the live music to build/music-samples (WAV files) and checks the engine
 * is healthy (no silence, no clipping, no NaN) and fast enough for a phone.
 */
class MusicEngineTest {
    private val out = File(System.getProperty("user.dir"), "build/music-samples").apply { mkdirs() }

    private fun render(name: String, styles: List<Style>, seconds: Int, intensity: (Double) -> Float): Pair<Double, Double> {
        val e = MusicEngine(seed = name.hashCode().toLong())
        e.setStyles(styles)
        val total = seconds * Dsp.SR
        val l = FloatArray(total); val r = FloatArray(total)
        val chunk = 1024
        val bl = FloatArray(chunk); val br = FloatArray(chunk)
        val t0 = System.nanoTime()
        var i = 0
        while (i < total) {
            e.intensity = intensity(i.toDouble() / total)
            val n = minOf(chunk, total - i)
            e.render(bl, br, n)
            System.arraycopy(bl, 0, l, i, n); System.arraycopy(br, 0, r, i, n)
            i += n
        }
        val secs = (System.nanoTime() - t0) / 1e9
        var peak = 0.0; var sum = 0.0
        for (k in 0 until total) {
            assertTrue("NaN in $name", !l[k].isNaN() && !r[k].isNaN())
            peak = maxOf(peak, abs(l[k].toDouble()), abs(r[k].toDouble())); sum += l[k] * l[k]
        }
        writeWav(File(out, "$name.wav"), l, r)
        val rms = sqrt(sum / total)
        println("%-22s realtime x%.1f  peak %.2f  rms %.3f".format(name, seconds / secs, peak, rms))
        assertTrue("$name is silent", rms > 0.02)
        assertTrue("$name clips", peak < 1.0)
        return seconds / secs to rms
    }

    private fun writeWav(f: File, l: FloatArray, r: FloatArray) {
        DataOutputStream(FileOutputStream(f).buffered()).use { o ->
            val n = l.size
            fun i32(v: Int) { o.write(v and 255); o.write(v shr 8 and 255); o.write(v shr 16 and 255); o.write(v shr 24 and 255) }
            fun i16(v: Int) { o.write(v and 255); o.write(v shr 8 and 255) }
            o.writeBytes("RIFF"); i32(36 + n * 4); o.writeBytes("WAVEfmt "); i32(16); i16(1); i16(2); i32(Dsp.SR); i32(Dsp.SR * 4); i16(4); i16(16)
            o.writeBytes("data"); i32(n * 4)
            for (k in 0 until n) { i16((l[k] * 32767).toInt()); i16((r[k] * 32767).toInt()) }
        }
    }

    @Test fun renderSamples() {
        val speeds = mutableListOf<Double>()
        // The band builds up as the board gets busier: piano alone, then bass, melody, drums, counter-melody.
        speeds += render("1-day-quiet-to-busy", listOf(Styles.cafe), 100) { t -> (0.05 + t * 1.1).toFloat().coerceAtMost(1f) }.first
        speeds += render("2-day-patio", listOf(Styles.patio), 70) { 0.7f }.first
        speeds += render("3-night", listOf(Styles.night), 80) { 0.6f }.first
        speeds += render("4-puzzle", listOf(Styles.puzzle), 75) { 0.5f }.first
        speeds += render("5-event-harvest", listOf(Styles.harvest), 70) { 0.8f }.first
        speeds += render("6-event-frosty", listOf(Styles.frosty), 60) { 0.8f }.first
        speeds += render("7-event-summer", listOf(Styles.summer), 45) { 0.8f }.first
        speeds += render("8-event-spooky", listOf(Styles.spooky), 45) { 0.8f }.first
        speeds += render("9-event-sweetheart", listOf(Styles.sweetheart), 45) { 0.8f }.first
        speeds += render("10-event-spring", listOf(Styles.spring), 45) { 0.8f }.first
        // A phone is slower than this computer, so ask for plenty of headroom.
        assertTrue("too slow: ${speeds.min()}", speeds.min() > 8)
    }
}
