package com.bingoroyale.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.bingoroyale.R

class SoundManager(private val context: Context) {

    private var soundPool: SoundPool? = null
    private var soundBall: Int = 0
    private var soundMark: Int = 0
    private var soundLine: Int = 0
    private var soundBingo: Int = 0
    private var soundClick: Int = 0

    private var vibrator: Vibrator? = null

    var isSoundEnabled = true
    var isVibrationEnabled = true

    init {
        initSoundPool()
        initVibrator()
    }

    private fun initSoundPool() {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(attributes)
            .build()

        soundPool?.let { pool ->
            soundBall = pool.load(context, R.raw.ball_draw, 1)
            soundMark = pool.load(context, R.raw.cell_mark, 1)
            soundLine = pool.load(context, R.raw.line_complete, 1)
            soundBingo = pool.load(context, R.raw.bingo_win, 1)
            soundClick = pool.load(context, R.raw.button_click, 1)
        }
    }

    private fun initVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun playBallSound() {
        if (isSoundEnabled) {
            soundPool?.play(soundBall, 1f, 1f, 1, 0, 1f)
        }
        vibrateShort()
    }

    fun playMarkSound() {
        if (isSoundEnabled) {
            soundPool?.play(soundMark, 0.7f, 0.7f, 1, 0, 1f)
        }
        vibrateShort()
    }

    fun playLineSound() {
        if (isSoundEnabled) {
            soundPool?.play(soundLine, 1f, 1f, 1, 0, 1f)
        }
        vibrateMedium()
    }

    fun playBingoSound() {
        if (isSoundEnabled) {
            soundPool?.play(soundBingo, 1f, 1f, 1, 0, 1f)
        }
        vibrateLong()
    }

    fun playClickSound() {
        if (isSoundEnabled) {
            soundPool?.play(soundClick, 0.5f, 0.5f, 1, 0, 1f)
        }
    }

    private fun vibrateShort() {
        if (!isVibrationEnabled) return
        vibrate(30)
    }

    private fun vibrateMedium() {
        if (!isVibrationEnabled) return
        vibrate(100)
    }

    private fun vibrateLong() {
        if (!isVibrationEnabled) return
        vibratePattern(longArrayOf(0, 100, 50, 100, 50, 200))
    }

    private fun vibrate(duration: Long) {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(duration)
            }
        }
    }

    private fun vibratePattern(pattern: LongArray) {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(pattern, -1)
            }
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
    }
}