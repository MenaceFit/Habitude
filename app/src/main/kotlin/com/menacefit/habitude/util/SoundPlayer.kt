package com.menacefit.habitude.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.menacefit.habitude.R

/** Short, subtle UI sound effects (spec section 35 — "Sounds: ON/OFF"), loaded once and played fire-and-forget via [SoundPool]. */
class SoundPlayer(context: Context) {
    private val appContext = context.applicationContext

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val completeSoundId = pool.load(appContext, R.raw.sound_complete, 1)
    private val levelUpSoundId = pool.load(appContext, R.raw.sound_levelup, 1)

    fun playComplete(enabled: Boolean) {
        if (enabled) pool.play(completeSoundId, 0.7f, 0.7f, 1, 0, 1f)
    }

    fun playLevelUp(enabled: Boolean) {
        if (enabled) pool.play(levelUpSoundId, 0.8f, 0.8f, 1, 0, 1f)
    }

    fun release() {
        pool.release()
    }
}
