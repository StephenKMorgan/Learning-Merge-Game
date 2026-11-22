package com.surfswirl.bakery.fx

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.surfswirl.bakery.R

enum class Sound(val res: Int) {
    POP(R.raw.sfx_pop), TAP(R.raw.sfx_tap), MERGE(R.raw.sfx_merge), COIN(R.raw.sfx_coin), ERROR(R.raw.sfx_error),
    LEVEL_UP(R.raw.sfx_levelup), ORDER(R.raw.sfx_order), CORRECT(R.raw.sfx_correct), WRONG(R.raw.sfx_wrong),
    WHOOSH(R.raw.sfx_whoosh), SPARKLE(R.raw.sfx_sparkle), UNLOCK(R.raw.sfx_unlock), WOOF(R.raw.sfx_woof),
}

/** Short sound effects through SoundPool. Music is planned for a later update (see CLAUDE.md). */
class Sfx(context: Context) {
    private val pool = SoundPool.Builder().setMaxStreams(6).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    ).build()
    private val ids = Sound.entries.associateWith { runCatching { pool.load(context, it.res, 1) }.getOrDefault(0) }

    @Volatile var enabled = true

    /** [pitch] 0.5..2.0, used to make higher-level merges sound brighter. */
    fun play(s: Sound, pitch: Float = 1f, volume: Float = 1f) {
        if (!enabled) return
        val id = ids[s] ?: return
        if (id != 0) pool.play(id, volume, volume, 1, 0, pitch.coerceIn(0.5f, 2f))
    }
}
