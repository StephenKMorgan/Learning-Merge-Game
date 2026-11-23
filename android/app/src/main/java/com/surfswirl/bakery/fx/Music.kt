package com.surfswirl.bakery.fx

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.surfswirl.bakery.R

/**
 * Background music. Plays the café tracks one after another (made by tools/make_music.py),
 * switches to the holiday version during the Frosty theme, gets quieter during puzzles so they're
 * easier to concentrate on, and pauses whenever the app is in the background.
 */
class Music(private val context: Context) {
    private var player: MediaPlayer? = null
    private var playlist: List<Int> = listOf(R.raw.music_cinnamon, R.raw.music_patio)
    private var index = 0
    private var foreground = false
    private var ducked = false

    var enabled = true
        set(v) { field = v; update() }

    fun setTheme(themeId: String) {
        val list = when (themeId) {
            "frosty" -> listOf(R.raw.music_cinnamon_frosty, R.raw.music_patio)
            "summer", "spring" -> listOf(R.raw.music_patio, R.raw.music_cinnamon)
            else -> listOf(R.raw.music_cinnamon, R.raw.music_patio)
        }
        if (list != playlist) {
            playlist = list; index = 0
            release()
            update()
        }
    }

    fun setDucked(d: Boolean) {
        ducked = d
        runCatching { player?.setVolume(volume(), volume()) }
    }

    fun onForeground() { foreground = true; update() }
    fun onBackground() { foreground = false; update() }

    private fun volume() = if (ducked) 0.12f else 0.32f

    private fun update() {
        val shouldPlay = enabled && foreground
        if (!shouldPlay) { runCatching { if (player?.isPlaying == true) player?.pause() }; return }
        if (player == null) startTrack() else runCatching { if (player?.isPlaying == false) player?.start() }
    }

    private fun startTrack() {
        release()
        val mp = runCatching { MediaPlayer.create(context, playlist[index % playlist.size]) }.getOrNull() ?: return
        runCatching {
            mp.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            mp.setVolume(volume(), volume())
            mp.setOnCompletionListener {
                index++
                startTrack()
            }
            mp.start()
        }
        player = mp
    }

    private fun release() {
        runCatching { player?.release() }
        player = null
    }
}
