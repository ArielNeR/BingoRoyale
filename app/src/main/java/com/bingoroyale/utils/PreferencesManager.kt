package com.bingoroyale.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PreferencesManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "bingo_royale_prefs"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_AUTO_MARK = "auto_mark"
        private const val KEY_DEFAULT_SPEED = "default_speed"
        private const val KEY_DEFAULT_MODE = "default_mode"
        private const val KEY_PLAYER_NAME = "player_name"
        private const val KEY_FIRST_RUN = "first_run"
        private const val KEY_TOTAL_GAMES = "total_games"
        private const val KEY_TOTAL_WINS = "total_wins"
        private const val KEY_TOTAL_LINES = "total_lines"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_SOUND_ENABLED, value) }

    var isVibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_VIBRATION_ENABLED, value) }

    var isAutoMarkEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_MARK, false)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_MARK, value) }

    var defaultSpeed: Float
        get() = prefs.getFloat(KEY_DEFAULT_SPEED, 1f)
        set(value) = prefs.edit { putFloat(KEY_DEFAULT_SPEED, value) }

    var defaultMode: Int
        get() = prefs.getInt(KEY_DEFAULT_MODE, 75)
        set(value) = prefs.edit { putInt(KEY_DEFAULT_MODE, value) }

    var playerName: String
        get() = prefs.getString(KEY_PLAYER_NAME, "Jugador") ?: "Jugador"
        set(value) = prefs.edit { putString(KEY_PLAYER_NAME, value) }

    var isFirstRun: Boolean
        get() = prefs.getBoolean(KEY_FIRST_RUN, true)
        set(value) = prefs.edit { putBoolean(KEY_FIRST_RUN, value) }

    // Estadísticas
    var totalGames: Int
        get() = prefs.getInt(KEY_TOTAL_GAMES, 0)
        set(value) = prefs.edit { putInt(KEY_TOTAL_GAMES, value) }

    var totalWins: Int
        get() = prefs.getInt(KEY_TOTAL_WINS, 0)
        set(value) = prefs.edit { putInt(KEY_TOTAL_WINS, value) }

    var totalLines: Int
        get() = prefs.getInt(KEY_TOTAL_LINES, 0)
        set(value) = prefs.edit { putInt(KEY_TOTAL_LINES, value) }

    fun incrementGames() {
        totalGames++
    }

    fun incrementWins() {
        totalWins++
    }

    fun incrementLines(count: Int = 1) {
        totalLines += count
    }

    fun resetStats() {
        prefs.edit {
            putInt(KEY_TOTAL_GAMES, 0)
            putInt(KEY_TOTAL_WINS, 0)
            putInt(KEY_TOTAL_LINES, 0)
        }
    }
}