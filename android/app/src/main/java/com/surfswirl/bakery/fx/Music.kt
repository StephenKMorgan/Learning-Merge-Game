package com.surfswirl.bakery.fx

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Process
import com.surfswirl.bakery.music.Dsp
import com.surfswirl.bakery.music.MusicEngine
import com.surfswirl.bakery.music.Style

/**
 * Live background music: the [MusicEngine] composes and plays on its own audio thread. The game
 * tells it which styles fit (day, night, puzzle, event) and how busy things are (intensity).
 * It only runs while the app is in front and music is on.
 */
class Music {
    private val engine = MusicEngine()
    @Volatile private var running = false
    private var thread: Thread? = null
    private var track: AudioTrack? = null
    private var foreground = false
    private var ducked = false

    var enabled = true
        set(v) { field = v; update() }

    fun setStyles(styles: List<Style>) = engine.setStyles(styles)

    fun setIntensity(v: Float) { engine.intensity = v.coerceIn(0f, 1f) }

    fun setDucked(d: Boolean) {
        ducked = d
        runCatching { track?.setVolume(volume()) }
    }

    fun onForeground() { foreground = true; update() }
    fun onBackground() { foreground = false; update() }

    private fun volume() = if (ducked) 0.55f else 0.8f

    private fun update() {
        if (enabled && foreground) start() else stop()
    }

    private fun start() {
        if (running || Build.FINGERPRINT == "robolectric") return
        val min = AudioTrack.getMinBufferSize(Dsp.SR, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        val t = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(Dsp.SR).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
                .setBufferSizeInBytes(maxOf(min, FRAMES * 4 * 4))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }.getOrNull() ?: return
        track = t
        runCatching { t.setVolume(volume()); t.play() }
        running = true
        thread = Thread({
            runCatching { Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO) }
            val l = FloatArray(FRAMES); val r = FloatArray(FRAMES)
            val pcm = ShortArray(FRAMES * 2)
            while (running) {
                engine.render(l, r, FRAMES)
                for (i in 0 until FRAMES) {
                    pcm[2 * i] = (l[i] * 32767f).toInt().coerceIn(-32768, 32767).toShort()
                    pcm[2 * i + 1] = (r[i] * 32767f).toInt().coerceIn(-32768, 32767).toShort()
                }
                if (t.write(pcm, 0, pcm.size) < 0) break
            }
        }, "music").apply { isDaemon = true; start() }
    }

    private fun stop() {
        if (!running) return
        running = false
        runCatching { thread?.join(500) }
        thread = null
        runCatching { track?.pause(); track?.flush(); track?.release() }
        track = null
    }

    private companion object { const val FRAMES = 1024 }
}
