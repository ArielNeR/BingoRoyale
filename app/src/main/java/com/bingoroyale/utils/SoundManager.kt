package com.bingoroyale.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class SoundManager(private val context: Context) {

    private var soundPool: SoundPool? = null
    private var vibrator: Vibrator? = null

    var isSoundEnabled = true
    var isVibrationEnabled = true

    private var ballSoundId: Int = 0
    private var bingoSoundId: Int = 0

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        // Obtener Vibrator
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Nota: En producción, cargarías los sonidos desde res/raw
        // ballSoundId = soundPool?.load(context, R.raw.ball_sound, 1) ?: 0
        // bingoSoundId = soundPool?.load(context, R.raw.bingo_sound, 1) ?: 0
    }

    fun playBallSound() {
        if (isSoundEnabled && ballSoundId != 0) {
            soundPool?.play(ballSoundId, 1f, 1f, 1, 0, 1f)
        }

        if (isVibrationEnabled) {
            vibrate(30)
        }
    }

    fun playBingoSound() {
        if (isSoundEnabled && bingoSoundId != 0) {
            soundPool?.play(bingoSoundId, 1f, 1f, 1, 0, 1f)
        }

        if (isVibrationEnabled) {
            vibrate(longArrayOf(0, 100, 50, 100, 50, 200))
        }
    }

    fun vibrate(duration: Long) {
        if (!isVibrationEnabled) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(duration)
        }
    }

    fun vibrate(pattern: LongArray) {
        if (!isVibrationEnabled) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, -1)
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
    }
}