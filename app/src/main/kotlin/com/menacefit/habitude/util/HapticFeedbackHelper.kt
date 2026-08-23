package com.menacefit.habitude.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Thin, settings-aware wrapper over the system vibrator (spec section 34 — "Haptic Feedback: ON/OFF"). */
class HapticFeedbackHelper(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private fun vibrate(durationMs: Long, amplitude: Int) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        v.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
    }

    /** A quick, light tap — habit completion. */
    fun tick(enabled: Boolean) {
        if (enabled) vibrate(20, 80)
    }

    /** A slightly firmer double-pulse — level up / achievement unlock / streak record. */
    fun celebrate(enabled: Boolean) {
        if (!enabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 30, 60, 40), intArrayOf(0, 200, 0, 255), -1))
    }
}
