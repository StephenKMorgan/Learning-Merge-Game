package com.surfswirl.bakery.fx

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class Haptics(context: Context) {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        else @Suppress("DEPRECATION") context.getSystemService(Vibrator::class.java)

    @Volatile var enabled = true

    private fun predefined(id: Int, ms: Long) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.createPredefined(id)
        else VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)

    private fun play(e: () -> VibrationEffect) {
        val v = vibrator ?: return
        if (!enabled || !v.hasVibrator()) return
        runCatching { v.vibrate(e()) }
    }

    fun tick() = play { predefined(VibrationEffect.EFFECT_TICK, 8) }
    fun click() = play { predefined(VibrationEffect.EFFECT_CLICK, 18) }
    fun heavy() = play { predefined(VibrationEffect.EFFECT_HEAVY_CLICK, 30) }
    fun error() = play { VibrationEffect.createWaveform(longArrayOf(0, 40, 60, 40), intArrayOf(0, 180, 0, 180), -1) }
    fun celebrate() = play { VibrationEffect.createWaveform(longArrayOf(0, 30, 50, 30, 50, 60), intArrayOf(0, 150, 0, 200, 0, 255), -1) }
}
